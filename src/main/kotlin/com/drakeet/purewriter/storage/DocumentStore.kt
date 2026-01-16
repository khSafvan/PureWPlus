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

package com.drakeet.purewriter.storage

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

data class DocumentMetadata(
  var id: String = "active",
  var title: String = "Untitled",
  var baseHash: String = "",
  var lastSyncTime: Long = 0,
  var lastModifiedTime: Long = 0,
  var isDirty: Boolean = false
)

data class DocumentRecord(
  val metadata: DocumentMetadata,
  val content: String
)

class DocumentStore(
  private val documentsDirProvider: () -> File = { AppPaths.documentsDir }
) {

  private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
  private val lock = Any()

  fun saveDocument(
    id: String = "active",
    title: String,
    content: String,
    isDirty: Boolean = false,
    updateBaseHash: Boolean = false
  ): DocumentRecord {
    synchronized(lock) {
      val dir = documentsDirProvider()
      dir.mkdirs()

      val safeId = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
      val textFile = File(dir, "$safeId.txt")
      val metaFile = File(dir, "$safeId.meta.json")

      val existingMeta = loadMetadata(metaFile, safeId)
      val now = System.currentTimeMillis()

      val contentHash = sha256(content)
      val newBaseHash = if (updateBaseHash || existingMeta.baseHash.isEmpty()) contentHash else existingMeta.baseHash

      val meta = DocumentMetadata(
        id = safeId,
        title = title,
        baseHash = newBaseHash,
        lastSyncTime = if (updateBaseHash) now else existingMeta.lastSyncTime,
        lastModifiedTime = now,
        isDirty = isDirty
      )

      // Atomic write of text content
      val tempTextFile = File(dir, "$safeId.txt.tmp")
      Files.write(tempTextFile.toPath(), content.toByteArray(StandardCharsets.UTF_8))
      Files.move(tempTextFile.toPath(), textFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)

      // Write metadata
      FileWriter(metaFile).use { writer ->
        gson.toJson(meta, writer)
      }

      return DocumentRecord(meta, content)
    }
  }

  fun loadDocument(id: String = "active"): DocumentRecord? {
    synchronized(lock) {
      val dir = documentsDirProvider()
      val safeId = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
      val textFile = File(dir, "$safeId.txt")
      val metaFile = File(dir, "$safeId.meta.json")

      if (!textFile.exists()) return null

      val content = String(Files.readAllBytes(textFile.toPath()), StandardCharsets.UTF_8)
      val meta = loadMetadata(metaFile, safeId)
      return DocumentRecord(meta, content)
    }
  }

  private fun loadMetadata(metaFile: File, defaultId: String): DocumentMetadata {
    if (metaFile.exists() && metaFile.length() > 0) {
      try {
        FileReader(metaFile).use { reader ->
          val meta = gson.fromJson(reader, DocumentMetadata::class.java)
          if (meta != null) return meta
        }
      } catch (e: Exception) {
        // Fallback
      }
    }
    return DocumentMetadata(id = defaultId)
  }

  companion object {
    val default: DocumentStore by lazy { DocumentStore() }

    fun sha256(text: String): String {
      val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(StandardCharsets.UTF_8))
      return bytes.joinToString("") { "%02x".format(it) }
    }
  }
}
