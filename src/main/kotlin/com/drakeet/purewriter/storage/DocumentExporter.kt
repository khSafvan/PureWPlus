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
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocumentExporter {

  fun exportToPlainText(title: String, content: String, targetFile: File) {
    targetFile.parentFile?.mkdirs()
    Files.write(targetFile.toPath(), content.toByteArray(StandardCharsets.UTF_8))
  }

  fun exportToMarkdown(title: String, content: String, targetFile: File) {
    targetFile.parentFile?.mkdirs()
    val mdText = if (title.isNotBlank() && !content.startsWith("# ")) {
      "# $title\n\n$content"
    } else {
      content
    }
    Files.write(targetFile.toPath(), mdText.toByteArray(StandardCharsets.UTF_8))
  }

  fun exportToHtml(title: String, content: String, targetFile: File) {
    targetFile.parentFile?.mkdirs()
    val escapedTitle = escapeHtml(title)
    val bodyHtml = content.split("\n\n").joinToString("\n") { paragraph ->
      val lines = paragraph.split("\n").joinToString("<br>\n") { line ->
        if (line.startsWith("# ")) "<h1>${escapeHtml(line.removePrefix("# ").trim())}</h1>"
        else if (line.startsWith("## ")) "<h2>${escapeHtml(line.removePrefix("## ").trim())}</h2>"
        else if (line.startsWith("### ")) "<h3>${escapeHtml(line.removePrefix("### ").trim())}</h3>"
        else escapeHtml(line)
      }
      "<p>$lines</p>"
    }

    val html = """
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8">
        <title>$escapedTitle</title>
        <style>
          body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; line-height: 1.8; max-width: 720px; margin: 40px auto; padding: 0 20px; color: #1f2937; }
          h1, h2, h3 { color: #111827; }
          p { margin-bottom: 1.2em; }
        </style>
      </head>
      <body>
        $bodyHtml
      </body>
      </html>
    """.trimIndent()

    Files.write(targetFile.toPath(), html.toByteArray(StandardCharsets.UTF_8))
  }

  fun exportBackupsToZip(backupEngine: BackupEngine, documentId: String, targetZipFile: File): Int {
    targetZipFile.parentFile?.mkdirs()
    val backups = backupEngine.listBackups(documentId)
    if (backups.isEmpty()) return 0

    ZipOutputStream(FileOutputStream(targetZipFile)).use { zos ->
      for (entry in backups) {
        if (!entry.file.exists()) continue
        val zipEntry = ZipEntry(entry.file.name)
        zos.putNextEntry(zipEntry)
        Files.copy(entry.file.toPath(), zos)
        zos.closeEntry()
      }
    }
    return backups.size
  }

  private fun escapeHtml(text: String): String {
    return text.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#x27;")
  }
}
