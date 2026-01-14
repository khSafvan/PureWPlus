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

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.*

data class BackupEntry(
  val file: File,
  val documentId: String,
  val timestamp: Long,
  val reason: String
)

class BackupEngine(
  private val backupsDirProvider: () -> File = { AppPaths.backupsDir }
) {

  private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).apply {
    timeZone = TimeZone.getDefault()
  }

  fun createBackup(
    documentId: String = "active",
    content: String,
    title: String? = null,
    reason: String = "pre-overwrite"
  ): File? {
    if (content.isEmpty()) return null

    val safeDocId = documentId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    val safeReason = reason.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    val targetDir = File(backupsDirProvider(), safeDocId)
    targetDir.mkdirs()

    val timestamp = System.currentTimeMillis()
    val dateStr = dateFormat.format(Date(timestamp))
    val fileName = "${dateStr}_${safeReason}.bak"
    val targetFile = File(targetDir, fileName)
    val tempFile = File(targetDir, "$fileName.tmp.${UUID.randomUUID()}")

    try {
      OutputStreamWriter(FileOutputStream(tempFile), StandardCharsets.UTF_8).use { writer ->
        if (!title.isNullOrEmpty()) {
          writer.write("--- TITLE: $title ---\n")
        }
        writer.write(content)
      }
      Files.move(
        tempFile.toPath(),
        targetFile.toPath(),
        StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING
      )
      return targetFile
    } catch (e: Exception) {
      tempFile.delete()
      throw e
    }
  }

  fun listBackups(documentId: String = "active"): List<BackupEntry> {
    val safeDocId = documentId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    val dir = File(backupsDirProvider(), safeDocId)
    if (!dir.exists() || !dir.isDirectory) return emptyList()

    return dir.listFiles { file -> file.isFile && file.name.endsWith(".bak") }
      ?.mapNotNull { file ->
        val parts = file.name.removeSuffix(".bak").split("_", limit = 4)
        if (parts.size >= 3) {
          val datePart = "${parts[0]}_${parts[1]}_${parts[2]}"
          val reason = if (parts.size == 4) parts[3] else "unknown"
          val time = try {
            dateFormat.parse(datePart)?.time ?: file.lastModified()
          } catch (e: Exception) {
            file.lastModified()
          }
          BackupEntry(file, safeDocId, time, reason)
        } else {
          BackupEntry(file, safeDocId, file.lastModified(), "manual")
        }
      }
      ?.sortedByDescending { it.timestamp }
      ?: emptyList()
  }

  fun readBackupContent(file: File): String {
    val raw = String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
    return if (raw.startsWith("--- TITLE: ") && raw.contains(" ---\n")) {
      raw.substringAfter(" ---\n")
    } else {
      raw
    }
  }

  fun pruneOldBackups(retentionDays: Int = 30): Int {
    if (retentionDays <= 0) return 0
    val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 60 * 60 * 1000)
    var deletedCount = 0

    val root = backupsDirProvider()
    if (!root.exists()) return 0

    root.listFiles { dir -> dir.isDirectory }?.forEach { docDir ->
      docDir.listFiles { file -> file.isFile && file.name.endsWith(".bak") }?.forEach { file ->
        if (file.lastModified() < cutoff) {
          if (file.delete()) deletedCount++
        }
      }
    }
    return deletedCount
  }

  companion object {
    val default: BackupEngine by lazy { BackupEngine() }
  }
}
