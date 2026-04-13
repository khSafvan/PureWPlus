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
import com.drakeet.purewriter.model.EmptyArticleMessage
import com.drakeet.purewriter.model.WordCountMessage
import com.drakeet.purewriter.net.Client
import com.drakeet.purewriter.net.sendArticle
import com.drakeet.purewriter.storage.BackupEngine
import com.drakeet.purewriter.storage.DocumentRecord
import com.drakeet.purewriter.storage.DocumentStore
import io.netty.channel.Channel

class SyncCoordinator(
  val documentStore: DocumentStore = DocumentStore.default,
  val backupEngine: BackupEngine = BackupEngine.default,
  val client: Client = Client()
) {

  var activeDocId: String = "active"
    private set

  var currentTitle: String = "Untitled"
    private set

  var currentContent: String = ""
    private set

  var baseContent: String = ""
    private set

  var isConnected: Boolean = false
    private set

  var onArticleUpdated: ((title: String, content: String, selectionStart: Int, selectionEnd: Int) -> Unit)? = null
  var onConflictDetected: ((title: String, conflictResult: MergeResult) -> Unit)? = null
  var onConnectionStatusChanged: ((active: Boolean) -> Unit)? = null
  var onWordCountUpdated: ((text: String) -> Unit)? = null
  var onEmptyArticle: (() -> Unit)? = null
  var onLibraryChanged: (() -> Unit)? = null

  init {
    // Restore cached document from disk if available
    val cached = documentStore.loadDocument("active")
    if (cached != null) {
      currentTitle = cached.metadata.title
      currentContent = cached.content
      if (!cached.metadata.isDirty) {
        baseContent = cached.content
      }
    }
  }

  fun handleIncomingArticle(article: ArticleMessage, channel: Channel? = client.channel) {
    val incomingContent = article.content
    val incomingTitle = if (article.title.isNotEmpty()) article.title else "Untitled"
    val storyDocId = if (article.title.isNotBlank()) documentStore.generateDocId(incomingTitle) else "active"

    // If switching stories from mobile
    if (storyDocId != activeDocId && currentContent.isNotEmpty()) {
      documentStore.saveDocument(activeDocId, currentTitle, currentContent, isDirty = (currentContent != baseContent))
    }
    activeDocId = storyDocId

    val isLocalModified = currentContent != baseContent && currentContent.isNotEmpty()
    val isRemoteModified = incomingContent != baseContent

    if (isLocalModified && isRemoteModified && currentContent != incomingContent) {
      backupEngine.createBackup(activeDocId, currentContent, currentTitle, reason = "pre-3way-merge")
      if (activeDocId != "active") {
        backupEngine.createBackup("active", currentContent, currentTitle, reason = "pre-3way-merge")
      }

      val mergeResult = Diff3.merge(baseContent, currentContent, incomingContent)
      if (!mergeResult.hasConflict) {
        // Clean merge without conflict: update both sides
        currentContent = mergeResult.mergedText
        currentTitle = incomingTitle
        baseContent = mergeResult.mergedText
        saveActiveDocument(updateBaseHash = true, isDirty = false)

        if (channel?.isActive == true) {
          channel.sendArticle(currentTitle, currentContent, article.selectionStart, article.selectionEnd)
        }
        onArticleUpdated?.invoke(currentTitle, currentContent, article.selectionStart, article.selectionEnd)
      } else {
        // Conflicting edits: backup remote, write conflict markers, and notify UI
        backupEngine.createBackup("${activeDocId}_remote", incomingContent, incomingTitle, reason = "conflict-remote")
        if (activeDocId != "active") {
          backupEngine.createBackup("active_remote", incomingContent, incomingTitle, reason = "conflict-remote")
        }
        currentContent = mergeResult.mergedText
        currentTitle = incomingTitle
        saveActiveDocument(updateBaseHash = false, isDirty = true)

        onConflictDetected?.invoke(currentTitle, mergeResult)
        onArticleUpdated?.invoke(currentTitle, currentContent, 0, 0)
      }
    } else {
      // Clean sync from phone
      if (currentContent.isNotEmpty() && currentContent != incomingContent) {
        backupEngine.createBackup(activeDocId, currentContent, currentTitle, reason = "remote-sync")
      }
      currentContent = incomingContent
      currentTitle = incomingTitle
      baseContent = incomingContent
      saveActiveDocument(updateBaseHash = true, isDirty = false)
      onArticleUpdated?.invoke(currentTitle, currentContent, article.selectionStart, article.selectionEnd)
    }
    onLibraryChanged?.invoke()
  }

  fun handleLocalTextChange(newContent: String, channel: Channel? = client.channel, start: Int = 0, end: Int = 0) {
    if (newContent == currentContent) return
    currentContent = newContent
    saveActiveDocument(updateBaseHash = false, isDirty = true)

    if (channel?.isActive == true) {
      channel.sendArticle(currentTitle, newContent, start, end)
    }
  }

  fun switchDocument(docId: String): DocumentRecord? {
    // Save current document before switching
    if (currentContent.isNotEmpty()) {
      saveActiveDocument(updateBaseHash = false, isDirty = (currentContent != baseContent))
    }

    val targetRecord = documentStore.loadDocument(docId) ?: return null
    activeDocId = targetRecord.metadata.id
    currentTitle = targetRecord.metadata.title
    currentContent = targetRecord.content
    baseContent = targetRecord.content

    onArticleUpdated?.invoke(currentTitle, currentContent, 0, 0)
    return targetRecord
  }

  fun createNewDocument(title: String = "Untitled"): DocumentRecord {
    if (currentContent.isNotEmpty()) {
      saveActiveDocument(updateBaseHash = false, isDirty = (currentContent != baseContent))
    }
    val newId = "doc_${System.currentTimeMillis()}"
    val record = documentStore.saveDocument(newId, title, "", isDirty = false, updateBaseHash = true)
    activeDocId = newId
    currentTitle = title
    currentContent = ""
    baseContent = ""

    onArticleUpdated?.invoke(currentTitle, currentContent, 0, 0)
    onLibraryChanged?.invoke()
    return record
  }

  fun deleteDocument(docId: String): Boolean {
    val deleted = documentStore.deleteDocument(docId)
    if (deleted && activeDocId == docId) {
      activeDocId = "active"
      currentTitle = "Untitled"
      currentContent = ""
      baseContent = ""
      onArticleUpdated?.invoke(currentTitle, currentContent, 0, 0)
    }
    onLibraryChanged?.invoke()
    return deleted
  }

  private fun saveActiveDocument(updateBaseHash: Boolean, isDirty: Boolean) {
    documentStore.saveDocument(activeDocId, currentTitle, currentContent, isDirty = isDirty, updateBaseHash = updateBaseHash)
    // Mirror to active for fallback compatibility
    if (activeDocId != "active") {
      documentStore.saveDocument("active", currentTitle, currentContent, isDirty = isDirty, updateBaseHash = updateBaseHash)
    }
  }

  fun handleChannelActive(active: Boolean) {
    isConnected = active
    if (!active) {
      saveActiveDocument(updateBaseHash = false, isDirty = (currentContent != baseContent))
    }
    onConnectionStatusChanged?.invoke(active)
  }

  fun handleWordCount(msg: WordCountMessage) {
    onWordCountUpdated?.invoke(msg.text)
  }

  fun handleEmptyArticle(msg: EmptyArticleMessage) {
    onEmptyArticle?.invoke()
  }

  companion object {
    val default: SyncCoordinator by lazy { SyncCoordinator() }
  }
}
