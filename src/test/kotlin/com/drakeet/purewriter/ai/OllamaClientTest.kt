/*
 * Copyright (C) 2026 PureWPlus Project Contributors
 *
 * This file is part of Pure Writer Desktop / PureWPlus
 *
 * Pure Writer Desktop is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.drakeet.purewriter.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OllamaClientTest {

  @Test
  fun testDefaultHostConfiguration() {
    val client = OllamaClient()
    assertEquals("http://localhost:11434", client.host)

    val customClient = OllamaClient("http://192.168.1.100:11434/")
    assertEquals("http://192.168.1.100:11434/", customClient.host)
  }

  @Test
  fun testListModelsHandlesUnreachableGracefully() {
    val client = OllamaClient("http://127.0.0.1:54321") // Non-existent port
    val models = client.listModels()
    assertTrue(models.isEmpty())
  }
}
