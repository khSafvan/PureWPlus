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

package com.drakeet.purewriter.sync

import com.drakeet.purewriter.model.ArticleMessage
import com.drakeet.purewriter.storage.BackupEngine
import com.drakeet.purewriter.storage.DocumentStore
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SyncCoordinatorTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun testInitialSyncSavesToDocumentStore() {
    val docDir = File(tempFolder.root, "docs")
    val backupDir = File(tempFolder.root, "backups")
    val docStore = DocumentStore(documentsDirProvider = { docDir })
    val backupEngine = BackupEngine(backupsDirProvider = { backupDir })
    val coordinator = SyncCoordinator(documentStore = docStore, backupEngine = backupEngine)

    var updatedTitle = ""
    var updatedContent = ""
    coordinator.onArticleUpdated = { title, content, _, _ ->
      updatedTitle = title
      updatedContent = content
    }

    val article = ArticleMessage(title = "Chapter 1", content = "Initial line from mobile", selectionStart = 0, selectionEnd = 0)
    coordinator.handleIncomingArticle(article, channel = null)

    assertEquals("Chapter 1", updatedTitle)
    assertEquals("Initial line from mobile", updatedContent)
    assertEquals("Initial line from mobile", coordinator.currentContent)
    assertEquals("Initial line from mobile", coordinator.baseContent)

    val saved = docStore.loadDocument("active")
    assertNotNull(saved)
    assertEquals("Initial line from mobile", saved?.content)
    assertFalse(saved!!.metadata.isDirty)
  }

  @Test
  fun testNonOverlappingThreeWayMerge() {
    val docDir = File(tempFolder.root, "docs")
    val backupDir = File(tempFolder.root, "backups")
    val docStore = DocumentStore(documentsDirProvider = { docDir })
    val backupEngine = BackupEngine(backupsDirProvider = { backupDir })
    val coordinator = SyncCoordinator(documentStore = docStore, backupEngine = backupEngine)

    // Base document synced earlier
    val initialText = "Line 1\nLine 2\nLine 3"
    coordinator.handleIncomingArticle(ArticleMessage(title = "Story", content = initialText), channel = null)

    // Desktop edits Line 1 offline
    coordinator.handleLocalTextChange("Line 1 (desktop edit)\nLine 2\nLine 3", channel = null)
    assertEquals("Line 1 (desktop edit)\nLine 2\nLine 3", coordinator.currentContent)

    // Phone edits Line 3 concurrently
    val phoneEdit = "Line 1\nLine 2\nLine 3 (mobile edit)"
    var mergedResultContent = ""
    coordinator.onArticleUpdated = { _, content, _, _ -> mergedResultContent = content }

    coordinator.handleIncomingArticle(ArticleMessage(title = "Story", content = phoneEdit), channel = null)

    // Non-overlapping changes should be merged cleanly
    val expectedMerge = "Line 1 (desktop edit)\nLine 2\nLine 3 (mobile edit)"
    assertEquals(expectedMerge, mergedResultContent)
    assertEquals(expectedMerge, coordinator.currentContent)

    // Pre-merge backup must exist
    val backups = backupEngine.listBackups("active")
    assertTrue("Pre-merge backup must be created before three-way merge", backups.isNotEmpty())
  }

  @Test
  fun testConflictingThreeWayMergeGeneratesMarkersAndBackups() {
    val docDir = File(tempFolder.root, "docs")
    val backupDir = File(tempFolder.root, "backups")
    val docStore = DocumentStore(documentsDirProvider = { docDir })
    val backupEngine = BackupEngine(backupsDirProvider = { backupDir })
    val coordinator = SyncCoordinator(documentStore = docStore, backupEngine = backupEngine)

    coordinator.handleIncomingArticle(ArticleMessage(title = "Essay", content = "Single Line Original"), channel = null)

    // Desktop edits line
    coordinator.handleLocalTextChange("Desktop modification", channel = null)

    // Mobile concurrently edits same line
    var conflictDetected = false
    coordinator.onConflictDetected = { _, _ -> conflictDetected = true }

    coordinator.handleIncomingArticle(ArticleMessage(title = "Essay", content = "Mobile modification"), channel = null)

    assertTrue("Conflict should be detected", conflictDetected)
    assertTrue("Should include conflict markers", coordinator.currentContent.contains("<<<<<<< Desktop"))
    assertTrue("Should include conflict markers", coordinator.currentContent.contains("Desktop modification"))
    assertTrue("Should include conflict markers", coordinator.currentContent.contains("Mobile modification"))
    assertTrue("Should include conflict markers", coordinator.currentContent.contains(">>>>>>> Phone"))

    // Verify backups of conflicting states exist
    val backups = backupEngine.listBackups("active")
    val remoteBackups = backupEngine.listBackups("active_remote")
    assertTrue("Local backup must exist", backups.isNotEmpty())
    assertTrue("Remote conflict backup must exist", remoteBackups.isNotEmpty())
  }

  @Test
  fun testZeroDataLossOnDisconnection() {
    val docDir = File(tempFolder.root, "docs")
    val backupDir = File(tempFolder.root, "backups")
    val docStore = DocumentStore(documentsDirProvider = { docDir })
    val backupEngine = BackupEngine(backupsDirProvider = { backupDir })
    val coordinator = SyncCoordinator(documentStore = docStore, backupEngine = backupEngine)

    coordinator.handleIncomingArticle(ArticleMessage(title = "Doc", content = "Important Article"), channel = null)
    coordinator.handleLocalTextChange("Important Article with unsynced local notes", channel = null)

    // Connection drops!
    coordinator.handleChannelActive(active = false)

    // Crucial: buffer must NOT be wiped!
    assertEquals("Important Article with unsynced local notes", coordinator.currentContent)

    // Check on disk
    val saved = docStore.loadDocument("active")
    assertEquals("Important Article with unsynced local notes", saved?.content)
    assertTrue(saved!!.metadata.isDirty)
  }
}
