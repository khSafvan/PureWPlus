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

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile

class DocumentExporterTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun testExportToPlainText() {
    val target = File(tempFolder.root, "export.txt")
    DocumentExporter.exportToPlainText("Title", "Plain content", target)
    assertTrue(target.exists())
    assertEquals("Plain content", target.readText())
  }

  @Test
  fun testExportToMarkdown() {
    val target = File(tempFolder.root, "export.md")
    DocumentExporter.exportToMarkdown("My Chapter", "Body paragraph", target)
    assertTrue(target.exists())
    assertEquals("# My Chapter\n\nBody paragraph", target.readText())
  }

  @Test
  fun testExportToHtml() {
    val target = File(tempFolder.root, "export.html")
    DocumentExporter.exportToHtml("My Story", "# Heading 1\n\nParagraph text with & and < symbols.", target)
    assertTrue(target.exists())
    val content = target.readText()
    assertTrue(content.contains("<h1>Heading 1</h1>"))
    assertTrue(content.contains("&amp; and &lt;"))
  }

  @Test
  fun testExportBackupsToZip() {
    val backupDir = File(tempFolder.root, "backups")
    val engine = BackupEngine(backupsDirProvider = { backupDir })
    engine.createBackup("doc1", "Version 1", reason = "r1")
    engine.createBackup("doc1", "Version 2", reason = "r2")

    val zipFile = File(tempFolder.root, "backups.zip")
    val count = DocumentExporter.exportBackupsToZip(engine, "doc1", zipFile)
    assertEquals(2, count)
    assertTrue(zipFile.exists())

    val zip = ZipFile(zipFile)
    assertEquals(2, zip.size())
    zip.close()
  }
}
