package com.drakeet.purewriter

import com.drakeet.purewriter.model.ArticleMessage
import com.drakeet.purewriter.model.ChannelActive
import com.drakeet.purewriter.model.EmptyArticleMessage
import com.drakeet.purewriter.model.WordCountMessage
import com.drakeet.purewriter.net.newJsonMessage
import com.drakeet.purewriter.protocol.PureWriterProtocol
import io.netty.buffer.ByteBuf
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.protobuf.ProtobufDecoder
import io.netty.handler.codec.protobuf.ProtobufEncoder
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterization and contract tests for the frozen mobile TCP protocol.
 * Verifies byte-level Protobuf framing, message envelopes, and JSON payloads.
 */
class ProtocolContractTest {

  @Test
  fun testEnvelopeSerializationRoundTrip() {
    val original = PureWriterProtocol.Message.newBuilder()
      .setId(101)
      .setVersion(2)
      .setContentType("JSON")
      .setMessageType("ArticleMessage")
      .setContent("{\"title\":\"Hello\",\"content\":\"World\",\"selectionStart\":0,\"selectionEnd\":5}")
      .setTime(1600000000000L)
      .build()

    val bytes = original.toByteArray()
    val parsed = PureWriterProtocol.Message.parseFrom(bytes)

    assertEquals(101, parsed.id)
    assertEquals(2, parsed.version)
    assertEquals("JSON", parsed.contentType)
    assertEquals("ArticleMessage", parsed.messageType)
    assertEquals("{\"title\":\"Hello\",\"content\":\"World\",\"selectionStart\":0,\"selectionEnd\":5}", parsed.content)
    assertEquals(1600000000000L, parsed.time)
  }

  @Test
  fun testNewJsonMessageHelperContract() {
    val article = ArticleMessage(
      title = "Draft",
      content = "Line 1\nLine 2",
      selectionStart = 3,
      selectionEnd = 7
    )
    val msg = newJsonMessage(id = 1, type = "ArticleMessage", any = article)

    assertEquals(1, msg.id)
    assertEquals(2, msg.version)
    assertEquals("JSON", msg.contentType)
    assertEquals("ArticleMessage", msg.messageType)
    assertTrue("Time should be populated", msg.time > 0L)

    val deserialized = msg.content.jsonTo(ArticleMessage::class)
    assertEquals("Draft", deserialized.title)
    assertEquals("Line 1\nLine 2", deserialized.content)
    assertEquals(3, deserialized.selectionStart)
    assertEquals(7, deserialized.selectionEnd)
  }

  @Test
  fun testArticleMessageJsonShape() {
    val json = """{"title":"Test","content":"Body","selectionStart":1,"selectionEnd":2}"""
    val article = json.jsonTo(ArticleMessage::class)
    assertEquals("Test", article.title)
    assertEquals("Body", article.content)
    assertEquals(1, article.selectionStart)
    assertEquals(2, article.selectionEnd)

    val reSerialized = article.toJson()
    assertTrue(reSerialized.contains("\"title\":\"Test\""))
    assertTrue(reSerialized.contains("\"content\":\"Body\""))
    assertTrue(reSerialized.contains("\"selectionStart\":1"))
    assertTrue(reSerialized.contains("\"selectionEnd\":2"))
  }

  @Test
  fun testWordCountMessageJsonShape() {
    val json = """{"text":"350 words"}"""
    val msg = json.jsonTo(WordCountMessage::class)
    assertEquals("350 words", msg.text)

    val reSerialized = msg.toJson()
    assertTrue(reSerialized.contains("\"text\":\"350 words\""))
  }

  @Test
  fun testPingMessageContract() {
    val pingMsg = newJsonMessage(id = 0, type = "Ping")
    assertEquals(0, pingMsg.id)
    assertEquals(2, pingMsg.version)
    assertEquals("JSON", pingMsg.contentType)
    assertEquals("Ping", pingMsg.messageType)
    assertEquals("\"\"", pingMsg.content)
  }

  @Test
  fun testEmptyArticleMessageEvent() {
    val empty = EmptyArticleMessage()
    assertNotNull(empty)
  }

  @Test
  fun testChannelActiveEvent() {
    val active = ChannelActive(true)
    assertTrue(active.active)
    val inactive = ChannelActive(false)
    assertEquals(false, inactive.active)
  }

  @Test
  fun testTraditionalChineseConversion() {
    // String extension function check
    val text = "纯纯写作"
    assertNotNull(text.toTraditionalIfNeeded())
  }

  @Test
  fun testNettyProtobufVarintFramingPipeline() {
    // 1. Outbound encoder pipeline matching Client.kt
    val outbound = EmbeddedChannel(
      ProtobufVarint32LengthFieldPrepender(),
      ProtobufEncoder()
    )

    val msg = PureWriterProtocol.Message.newBuilder()
      .setId(42)
      .setVersion(2)
      .setContentType("JSON")
      .setMessageType("WordCountMessage")
      .setContent("{\"text\":\"10 字\"}")
      .setTime(System.currentTimeMillis())
      .build()

    assertTrue(outbound.writeOutbound(msg))
    val frame: ByteBuf? = outbound.readOutbound()
    assertNotNull("Frame must be produced", frame)

    // 2. Inbound decoder pipeline matching Client.kt
    val inbound = EmbeddedChannel(
      ProtobufVarint32FrameDecoder(),
      ProtobufDecoder(PureWriterProtocol.Message.getDefaultInstance())
    )

    assertTrue(inbound.writeInbound(frame))
    val decoded: PureWriterProtocol.Message? = inbound.readInbound()
    assertNotNull("Decoded message must exist", decoded)
    assertEquals(42, decoded!!.id)
    assertEquals(2, decoded.version)
    assertEquals("WordCountMessage", decoded.messageType)
    assertEquals("{\"text\":\"10 字\"}", decoded.content)
  }
}
