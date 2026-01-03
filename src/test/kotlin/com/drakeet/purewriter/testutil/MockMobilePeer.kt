/*
 * Copyright (C) 2019 Drakeet Xu <drakeet@drakeet.com>
 * Modifications Copyright (C) 2026 PureWPlus Project Contributors
 *
 * This file is part of Pure Writer Desktop / PureWPlus
 *
 * Pure Writer Desktop is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rebase-server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with rebase-server. If not, see <http://www.gnu.org/licenses/>.
 */

package com.drakeet.purewriter.testutil

import com.drakeet.purewriter.model.ArticleMessage
import com.drakeet.purewriter.model.EmptyArticleMessage
import com.drakeet.purewriter.model.WordCountMessage
import com.drakeet.purewriter.protocol.PureWriterProtocol
import com.google.gson.Gson
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.protobuf.ProtobufDecoder
import io.netty.handler.codec.protobuf.ProtobufEncoder
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MockMobilePeer {

  private val gson = Gson()
  private var bossGroup: NioEventLoopGroup? = null
  private var workerGroup: NioEventLoopGroup? = null
  private var serverChannel: Channel? = null

  var port: Int = 0
    private set

  val connectedChannels = CopyOnWriteArrayList<Channel>()
  val receivedMessages = CopyOnWriteArrayList<PureWriterProtocol.Message>()
  private val messageLatches = CopyOnWriteArrayList<Pair<(PureWriterProtocol.Message) -> Boolean, CountDownLatch>>()

  fun start(): Int {
    bossGroup = NioEventLoopGroup(1)
    workerGroup = NioEventLoopGroup()

    val bootstrap = ServerBootstrap()
      .group(bossGroup, workerGroup)
      .channel(NioServerSocketChannel::class.java)
      .childHandler(object : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel) {
          ch.pipeline().apply {
            addLast(ProtobufVarint32FrameDecoder())
            addLast(ProtobufDecoder(PureWriterProtocol.Message.getDefaultInstance()))
            addLast(ProtobufVarint32LengthFieldPrepender())
            addLast(ProtobufEncoder())
            addLast(object : SimpleChannelInboundHandler<PureWriterProtocol.Message>() {
              override fun channelActive(ctx: ChannelHandlerContext) {
                connectedChannels.add(ctx.channel())
                super.channelActive(ctx)
              }

              override fun channelInactive(ctx: ChannelHandlerContext) {
                connectedChannels.remove(ctx.channel())
                super.channelInactive(ctx)
              }

              override fun channelRead0(ctx: ChannelHandlerContext, msg: PureWriterProtocol.Message) {
                receivedMessages.add(msg)
                messageLatches.forEach { (predicate, latch) ->
                  if (predicate(msg)) {
                    latch.countDown()
                  }
                }
              }
            })
          }
        }
      })

    val future = bootstrap.bind(InetSocketAddress("127.0.0.1", 0)).sync()
    serverChannel = future.channel()
    port = (serverChannel?.localAddress() as InetSocketAddress).port
    return port
  }

  fun waitForConnection(timeoutMs: Long = 3000): Channel? {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      val ch = connectedChannels.firstOrNull { it.isActive }
      if (ch != null) return ch
      Thread.sleep(20)
    }
    return null
  }

  fun sendArticle(title: String, content: String, selectionStart: Int = 0, selectionEnd: Int = 0) {
    val payload = ArticleMessage(title = title, content = content, selectionStart = selectionStart, selectionEnd = selectionEnd)
    val msg = PureWriterProtocol.Message.newBuilder()
      .setContentType("JSON")
      .setVersion(2)
      .setTime(System.currentTimeMillis())
      .setMessageType("ArticleMessage")
      .setContent(gson.toJson(payload))
      .build()
    broadcast(msg)
  }

  fun sendWordCount(text: String) {
    val payload = WordCountMessage(text = text)
    val msg = PureWriterProtocol.Message.newBuilder()
      .setContentType("JSON")
      .setVersion(2)
      .setTime(System.currentTimeMillis())
      .setMessageType("WordCountMessage")
      .setContent(gson.toJson(payload))
      .build()
    broadcast(msg)
  }

  fun sendEmptyArticle() {
    val payload = EmptyArticleMessage()
    val msg = PureWriterProtocol.Message.newBuilder()
      .setContentType("JSON")
      .setVersion(2)
      .setTime(System.currentTimeMillis())
      .setMessageType("EmptyArticleMessage")
      .setContent(gson.toJson(payload))
      .build()
    broadcast(msg)
  }

  fun sendDisconnect() {
    val msg = PureWriterProtocol.Message.newBuilder()
      .setContentType("JSON")
      .setVersion(2)
      .setTime(System.currentTimeMillis())
      .setMessageType("DisconnectMessage")
      .setContent("\"\"")
      .build()
    broadcast(msg)
  }

  fun broadcast(msg: PureWriterProtocol.Message) {
    connectedChannels.forEach { channel ->
      if (channel.isActive) {
        channel.writeAndFlush(msg)
      }
    }
  }

  fun waitForMessage(
    timeoutMs: Long = 3000,
    predicate: (PureWriterProtocol.Message) -> Boolean
  ): PureWriterProtocol.Message? {
    // Check if already received
    val existing = receivedMessages.firstOrNull(predicate)
    if (existing != null) return existing

    val latch = CountDownLatch(1)
    val pair = Pair(predicate, latch)
    messageLatches.add(pair)
    try {
      latch.await(timeoutMs, TimeUnit.MILLISECONDS)
      return receivedMessages.firstOrNull(predicate)
    } finally {
      messageLatches.remove(pair)
    }
  }

  fun stop() {
    connectedChannels.forEach { it.close() }
    serverChannel?.close()?.syncUninterruptibly()
    bossGroup?.shutdownGracefully(0, 500, TimeUnit.MILLISECONDS)?.syncUninterruptibly()
    workerGroup?.shutdownGracefully(0, 500, TimeUnit.MILLISECONDS)?.syncUninterruptibly()
  }
}
