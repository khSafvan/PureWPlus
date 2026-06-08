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

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class OllamaGenerateRequest(
  val model: String,
  val prompt: String,
  val system: String? = null,
  val stream: Boolean = false
)

data class OllamaGenerateResponse(
  val model: String? = null,
  val response: String? = null,
  val done: Boolean = false
)

class OllamaClient(
  var host: String = "http://localhost:11434"
) {

  private val gson = Gson()

  fun listModels(): List<String> {
    val cleanHost = host.trimEnd('/')
    val url = URL("$cleanHost/api/tags")
    val conn = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 3000
      readTimeout = 5000
    }

    return try {
      if (conn.responseCode == 200) {
        val jsonStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val root = gson.fromJson(jsonStr, JsonObject::class.java)
        val modelsArray = root.getAsJsonArray("models")
        val result = mutableListOf<String>()
        if (modelsArray != null) {
          for (element in modelsArray) {
            val name = element.asJsonObject.get("name")?.asString
            if (!name.isNullOrBlank()) {
              result.add(name)
            }
          }
        }
        result
      } else {
        emptyList()
      }
    } catch (e: Exception) {
      emptyList()
    } finally {
      conn.disconnect()
    }
  }

  fun generate(
    model: String,
    prompt: String,
    systemPrompt: String? = null
  ): String {
    val cleanHost = host.trimEnd('/')
    val url = URL("$cleanHost/api/generate")
    val conn = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      setRequestProperty("Content-Type", "application/json; charset=utf-8")
      doOutput = true
      connectTimeout = 5000
      readTimeout = 60000
    }

    val req = OllamaGenerateRequest(
      model = model,
      prompt = prompt,
      system = systemPrompt,
      stream = false
    )

    try {
      OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
        writer.write(gson.toJson(req))
        writer.flush()
      }

      val code = conn.responseCode
      if (code == 200) {
        val respStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val respObj = gson.fromJson(respStr, OllamaGenerateResponse::class.java)
        return respObj.response ?: ""
      } else {
        val errStr = conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
        throw RuntimeException("Ollama HTTP $code: $errStr")
      }
    } finally {
      conn.disconnect()
    }
  }

  companion object {
    val default: OllamaClient by lazy { OllamaClient() }
  }
}
