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

package com.drakeet.purewriter.net

import com.drakeet.purewriter.RxBus
import com.drakeet.purewriter.model.ArticleMessage
import com.drakeet.purewriter.model.ChannelActive
import com.drakeet.purewriter.model.EmptyArticleMessage
import com.drakeet.purewriter.model.WordCountMessage
import com.drakeet.purewriter.testutil.MockMobilePeer
import com.google.gson.Gson
import io.reactivex.disposables.CompositeDisposable
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class SyncIntegrationTest {

  private lateinit var peer: MockMobilePeer
  private lateinit var client: Client
  private val disposables = CompositeDisposable()
  private val gson = Gson()

  companion object {
    @org.junit.BeforeClass
    @JvmStatic
    fun initJavaFx() {
      try {
        com.sun.javafx.application.PlatformImpl.startup {}
      } catch (t: Throwable) {
        // already initialized or headless
      }
    }
  }

  @Before
  fun setUp() {
    peer = MockMobilePeer()
    peer.start()
    client = Client()
  }

  @After
  fun tearDown() {
    disposables.clear()
    client.stop()
    peer.stop()
  }

  @Test
  fun testConnectAndReceiveArticleMessage() {
    val activeLatch = CountDownLatch(1)
    disposables.add(
      RxBus.event(ChannelActive::class.java)
        .subscribe { if (it.active) activeLatch.countDown() }
    )

    val articleLatch = CountDownLatch(1)
    var receivedArticle: ArticleMessage? = null
    disposables.add(
      RxBus.event(ArticleMessage::class.java)
        .subscribe {
          receivedArticle = it
          articleLatch.countDown()
        }
    )

    thread(isDaemon = true) {
      client.start("127.0.0.1", peer.port)
    }

    assertTrue("Client should connect within 3s", activeLatch.await(3, TimeUnit.SECONDS))
    val serverPeerChannel = peer.waitForConnection(3000)
    assertNotNull("MockMobilePeer should detect client connection", serverPeerChannel)

    peer.sendArticle(
      title = "Test Chapter 1",
      content = "The quick brown fox jumps over the lazy dog.",
      selectionStart = 4,
      selectionEnd = 9
    )

    assertTrue("Client should receive ArticleMessage within 3s", articleLatch.await(3, TimeUnit.SECONDS))
    assertNotNull(receivedArticle)
    assertEquals("Test Chapter 1", receivedArticle?.title)
    assertEquals("The quick brown fox jumps over the lazy dog.", receivedArticle?.content)
    assertEquals(4, receivedArticle?.selectionStart)
    assertEquals(9, receivedArticle?.selectionEnd)
  }

  @Test
  fun testClientSendsArticleToMobilePeer() {
    val activeLatch = CountDownLatch(1)
    disposables.add(
      RxBus.event(ChannelActive::class.java)
        .subscribe { if (it.active) activeLatch.countDown() }
    )

    thread(isDaemon = true) {
      client.start("127.0.0.1", peer.port)
    }

    assertTrue("Client should connect within 3s", activeLatch.await(3, TimeUnit.SECONDS))

    val channel = client.channel
    assertNotNull("Client channel should not be null", channel)

    channel!!.sendArticle(
      title = "Edited from Desktop",
      content = "Desktop additions to the text.",
      start = 10,
      end = 15
    )

    val received = peer.waitForMessage(3000) { it.messageType == "ArticleMessage" }
    assertNotNull("Mobile peer should receive ArticleMessage from desktop", received)
    val parsed = gson.fromJson(received?.content, ArticleMessage::class.java)
    assertEquals("Edited from Desktop", parsed.title)
    assertEquals("Desktop additions to the text.", parsed.content)
  }

  @Test
  fun testWordCountAndEmptyArticleMessages() {
    val activeLatch = CountDownLatch(1)
    disposables.add(
      RxBus.event(ChannelActive::class.java)
        .subscribe { if (it.active) activeLatch.countDown() }
    )

    val wordCountLatch = CountDownLatch(1)
    var receivedWordCount: WordCountMessage? = null
    disposables.add(
      RxBus.event(WordCountMessage::class.java)
        .subscribe {
          receivedWordCount = it
          wordCountLatch.countDown()
        }
    )

    val emptyLatch = CountDownLatch(1)
    disposables.add(
      RxBus.event(EmptyArticleMessage::class.java)
        .subscribe { emptyLatch.countDown() }
    )

    thread(isDaemon = true) {
      client.start("127.0.0.1", peer.port)
    }

    assertTrue("Client should connect within 3s", activeLatch.await(3, TimeUnit.SECONDS))

    peer.sendWordCount("42 words")
    assertTrue("Client should receive WordCountMessage", wordCountLatch.await(3, TimeUnit.SECONDS))
    assertEquals("42 words", receivedWordCount?.text)

    peer.sendEmptyArticle()
    assertTrue("Client should receive EmptyArticleMessage", emptyLatch.await(3, TimeUnit.SECONDS))
  }
}
