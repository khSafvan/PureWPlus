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

class BackupEngineTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun testCreateAndListBackup() {
    val backupsDir = File(tempFolder.root, "backups")
    val engine = BackupEngine(backupsDirProvider = { backupsDir })

    val createdFile = engine.createBackup(
      documentId = "chapter_1",
      content = "Line 1\nLine 2\nLine 3",
      title = "Chapter One",
      reason = "pre-sync"
    )

    assertNotNull(createdFile)
    assertTrue(createdFile!!.exists())

    val content = engine.readBackupContent(createdFile)
    assertEquals("Line 1\nLine 2\nLine 3", content)

    val list = engine.listBackups("chapter_1")
    assertEquals(1, list.size)
    assertEquals("chapter_1", list[0].documentId)
    assertEquals("pre-sync", list[0].reason)
  }

  @Test
  fun testEmptyContentProducesNoBackup() {
    val backupsDir = File(tempFolder.root, "backups")
    val engine = BackupEngine(backupsDirProvider = { backupsDir })

    val file = engine.createBackup(
      documentId = "empty_doc",
      content = "",
      reason = "test"
    )

    assertNull(file)
    assertEquals(0, engine.listBackups("empty_doc").size)
  }

  @Test
  fun testPruneOldBackups() {
    val backupsDir = File(tempFolder.root, "backups")
    val engine = BackupEngine(backupsDirProvider = { backupsDir })

    val f1 = engine.createBackup("doc1", "Recent text", reason = "recent")
    assertNotNull(f1)

    val doc1Dir = File(backupsDir, "doc1")
    val oldFile = File(doc1Dir, "20200101_000000_000_old.bak")
    oldFile.writeText("Ancient text")
    oldFile.setLastModified(System.currentTimeMillis() - 40L * 24 * 60 * 60 * 1000) // 40 days old

    assertEquals(2, engine.listBackups("doc1").size)

    val pruned = engine.pruneOldBackups(retentionDays = 30)
    assertEquals(1, pruned)
    assertFalse(oldFile.exists())
    assertTrue(f1!!.exists())
  }
}
