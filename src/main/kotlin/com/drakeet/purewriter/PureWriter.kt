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

package com.drakeet.purewriter

import com.drakeet.purewriter.i18n.I18n
import com.drakeet.purewriter.model.ArticleMessage
import com.drakeet.purewriter.model.ChannelActive
import com.drakeet.purewriter.model.EmptyArticleMessage
import com.drakeet.purewriter.model.WordCountMessage
import com.drakeet.purewriter.net.Client
import com.drakeet.purewriter.net.PingPongDelegate
import com.drakeet.purewriter.net.sendArticle
import com.drakeet.purewriter.storage.BackupEntry
import com.drakeet.purewriter.sync.SyncCoordinator
import io.netty.channel.Channel
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.rxjavafx.observables.JavaFxObservable
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler.platform
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Schedulers.computation
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.util.Duration
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class PureWriter : Initializable {

  private val version = "0.3.1"
  private val appVersion = "17.7.10"

  @FXML
  private lateinit var rootLayout: BorderPane

  @FXML
  private lateinit var centerContainer: StackPane

  @FXML
  private lateinit var emptyView: Label

  @FXML
  private lateinit var ipLayout: VBox

  @FXML
  private lateinit var ipNoteLabel: Label

  @FXML
  private lateinit var ipBottomLabel: Label

  @FXML
  private lateinit var ipView: TextField

  @FXML
  private lateinit var contentView: TextArea

  @FXML
  private lateinit var dayNightSwitch: ImageView

  @FXML
  private lateinit var backupsDrawer: VBox

  @FXML
  private lateinit var drawerTitleLabel: Label

  @FXML
  private lateinit var closeDrawerBtn: Button

  @FXML
  private lateinit var backupsListView: ListView<String>

  @FXML
  private lateinit var restoreBackupBtn: Button

  @FXML
  private lateinit var statusBar: HBox

  @FXML
  private lateinit var statusConnectionDot: Label

  @FXML
  private lateinit var statusConnectionLabel: Label

  @FXML
  private lateinit var statusSaveLabel: Label

  @FXML
  private lateinit var statusCountLabel: Label

  @FXML
  private lateinit var toggleTypewriterBtn: Button

  @FXML
  private lateinit var toggleAiBtn: Button

  @FXML
  private lateinit var toggleDrawerBtn: Button

  @FXML
  private lateinit var aiDrawer: VBox

  @FXML
  private lateinit var aiTitleLabel: Label

  @FXML
  private lateinit var closeAiBtn: Button

  @FXML
  private lateinit var aiModelField: TextField

  @FXML
  private lateinit var refreshAiModelsBtn: Button

  @FXML
  private lateinit var aiContinueBtn: Button

  @FXML
  private lateinit var aiPolishBtn: Button

  @FXML
  private lateinit var aiSummarizeBtn: Button

  @FXML
  private lateinit var aiPromptArea: TextArea

  @FXML
  private lateinit var aiSendPromptBtn: Button

  @FXML
  private lateinit var aiResponseArea: TextArea

  @FXML
  private lateinit var aiInsertBtn: Button

  @FXML
  private lateinit var aiReplaceBtn: Button

  private val client = Client()
  private val coordinator = SyncCoordinator.default
  private val ollamaClient = com.drakeet.purewriter.ai.OllamaClient.default

  private val channel: Channel? get() = client.channel

  private var title = "Untitled"
  private var isTypewriterMode = false
  private var backupEntries: List<BackupEntry> = emptyList()

  override fun initialize(location: URL?, resources: ResourceBundle?) {
    rootLayout.styleClass.addAll("pane")
    ipLayout.styleClass.addAll("pane")

    val regularFont = Font.loadFont(javaClass.getResourceAsStream("SourceHanSansCN-Light.ttf"), 16.toDouble())
    if (regularFont != null) {
      contentView.font = regularFont
      ipNoteLabel.font = Font.loadFont(javaClass.getResourceAsStream("SourceHanSansCN-Light.ttf"), 14.toDouble()) ?: regularFont
      ipBottomLabel.font = ipNoteLabel.font
      emptyView.font = ipNoteLabel.font
      ipView.font = Font.loadFont(javaClass.getResourceAsStream("SourceHanSansCN-Light.ttf"), 18.toDouble()) ?: regularFont
    }

    dayNightSwitch.image = Image(javaClass.getResourceAsStream("moon.png"))
    dayNightSwitch.setOnMouseClicked {
      Settings.darkMode = (!Settings.darkMode)
        .also { mainStage.scene.setDarkMode(it) }
    }

    // Localize UI labels
    drawerTitleLabel.text = I18n.get("drawer.backups")
    restoreBackupBtn.text = I18n.get("drawer.restore")
    toggleTypewriterBtn.text = I18n.get("menu.typewriter")
    toggleDrawerBtn.text = I18n.get("drawer.backups")
    toggleAiBtn.text = "✨ AI"
    aiTitleLabel.text = I18n.get("ai.title")
    aiContinueBtn.text = I18n.get("ai.continue")
    aiPolishBtn.text = I18n.get("ai.polish")
    aiSummarizeBtn.text = I18n.get("ai.summarize")
    aiPromptArea.promptText = I18n.get("ai.prompt_placeholder")
    aiInsertBtn.text = I18n.get("ai.insert")
    aiReplaceBtn.text = I18n.get("ai.replace")
    emptyView.text = I18n.get("empty.title", version)
    ipNoteLabel.text = I18n.get("ip.note")
    ipBottomLabel.text = I18n.get("ip.bottom", version, appVersion)

    // Setup Drawer and Action listeners
    closeDrawerBtn.setOnAction { toggleBackupsDrawer() }
    toggleDrawerBtn.setOnAction { toggleBackupsDrawer() }
    restoreBackupBtn.setOnAction { restoreSelectedBackup() }
    toggleTypewriterBtn.setOnAction { toggleTypewriterMode() }
    toggleAiBtn.setOnAction { toggleAiDrawer() }
    closeAiBtn.setOnAction { toggleAiDrawer() }

    // Ollama AI wiring
    aiModelField.text = Settings.ollamaModel
    aiModelField.textProperty().addListener { _, _, newModel ->
      Settings.ollamaModel = newModel?.trim() ?: "llama3"
    }
    refreshAiModelsBtn.setOnAction { refreshAiModels() }
    aiContinueBtn.setOnAction { triggerAiAction("continue") }
    aiPolishBtn.setOnAction { triggerAiAction("polish") }
    aiSummarizeBtn.setOnAction { triggerAiAction("summarize") }
    aiSendPromptBtn.setOnAction { triggerAiCustomPrompt() }
    aiInsertBtn.setOnAction { insertAiResponse() }
    aiReplaceBtn.setOnAction { replaceWithAiResponse() }

    // Restore cached content from DocumentStore if available
    if (coordinator.currentContent.isNotEmpty()) {
      contentView.replaceText(coordinator.currentContent)
      title = coordinator.currentTitle
      mainStage.title = title
      hideIP()
      hideEmpty()
      updateCounts(coordinator.currentContent)
      setSaveStatus(true)
    } else {
      showIpViews()
      updateCounts("")
      setSaveStatus(true)
    }
    updateConnectionStatus(false)
    ipView.text = Settings.ip
    setupAutoConnect()

    // Sync coordinator event listeners
    coordinator.onArticleUpdated = { updatedTitle, updatedContent, start, end ->
      beginSyncing()
      title = updatedTitle
      mainStage.title = if (title.isNotEmpty()) title else "Untitled"
      contentView.replaceText(updatedContent)
      contentView.isVisible = true
      runCatching { contentView.selectRange(start.coerceAtLeast(1) - 1, end.coerceAtLeast(1) - 1) }
      Platform.runLater { contentView.selectRange(start, end) }
      isSyncingSelection = false
      isSyncingTitle = false
      setSaveStatus(true)
      updateCounts(updatedContent)
      hideIP()
      hideEmpty()
    }

    coordinator.onWordCountUpdated = { countText ->
      mainStage.title = "$title · $countText"
    }

    coordinator.onEmptyArticle = {
      showEmpty()
      hideIP()
    }

    RxBus.event(ChannelActive::class)
      .observeOn(platform())
      .subscribe {
        coordinator.handleChannelActive(it.active, channel)
        updateConnectionStatus(it.active)
        if (it.active) {
          hideIP()
          hideEmpty()
          // If we had dirty text offline, coordinator will have pushed it or kept it
          setSaveStatus(true)
        } else {
          showIpViews()
        }
      }

    RxBus.event(ArticleMessage::class)
      .subscribe {
        coordinator.handleIncomingArticle(it, channel)
      }

    RxBus.event(WordCountMessage::class)
      .subscribe {
        coordinator.handleWordCount(it)
      }

    RxBus.event(EmptyArticleMessage::class)
      .subscribe {
        coordinator.handleEmptyArticle(it)
      }

    contentView.textProperty().addListener { _, _, newValue ->
      Log.d("newText: $newValue")
      updateCounts(newValue ?: "")
      if (channel?.isActive != true) {
        setSaveStatus(false)
        coordinator.handleLocalTextChange(newValue ?: "", channel)
        return@addListener
      }
      if (!isSyncing) {
        coordinator.handleLocalTextChange(newValue ?: "", channel)
        setSaveStatus(true)
      }
      isSyncingContent = false
    }

    contentView.selectionProperty().addListener { _, _, newValue ->
      if (channel?.isActive != true) {
        return@addListener
      }
      if (!isSyncing) {
        invalidate(selection = newValue)
      }
      isSyncingSelection = false
    }

    // Keyboard Shortcuts
    rootLayout.setOnKeyPressed { event ->
      if (event.isShortcutDown && event.code == KeyCode.S) {
        coordinator.documentStore.saveDocument("active", title, contentView.text ?: "", isDirty = false)
        setSaveStatus(true)
        event.consume()
      } else if (event.isShortcutDown && event.isShiftDown && event.code == KeyCode.B) {
        toggleBackupsDrawer()
        event.consume()
      } else if (event.isShortcutDown && event.isShiftDown && event.code == KeyCode.A) {
        toggleAiDrawer()
        event.consume()
      } else if (event.isShortcutDown && event.isShiftDown && event.code == KeyCode.D) {
        // Toggle distraction-free mode (hide/show status bar)
        val visible = !statusBar.isVisible
        statusBar.isVisible = visible
        statusBar.isManaged = visible
        event.consume()
      } else if (event.code == KeyCode.F11) {
        runCatching { mainStage.isFullScreen = !mainStage.isFullScreen }
        event.consume()
      }
    }

    JavaFxObservable.interval(Duration.seconds(2.0))
      .subscribeOn(computation())
      .observeOn(platform())
      .subscribe {
        val active = channel?.isActive == true
        updateConnectionStatus(active)
        if (active) {
          PingPongDelegate.ping(channel!!)
        } else {
          // If disconnected and we have a valid saved IP, auto-reconnect in background
          val savedIp = Settings.ip
          if (savedIp.isNotBlank() && savedIp.matches("((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}".toRegex())) {
            startNettyClient(savedIp)
          }
        }
      }
  }

  private fun updateConnectionStatus(connected: Boolean) {
    if (connected) {
      statusConnectionDot.text = "●"
      statusConnectionDot.styleClass.setAll("status-dot-online")
      statusConnectionLabel.text = I18n.get("status.connected")
    } else {
      statusConnectionDot.text = "●"
      statusConnectionDot.styleClass.setAll("status-dot-offline")
      statusConnectionLabel.text = I18n.get("status.disconnected")
    }
  }

  private fun updateCounts(text: String) {
    val charCount = text.length
    val words = if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
    statusCountLabel.text = I18n.get("status.words", words, charCount)
  }

  private fun setSaveStatus(saved: Boolean) {
    statusSaveLabel.text = if (saved) I18n.get("status.saved") else I18n.get("status.unsaved")
  }

  private fun toggleTypewriterMode() {
    isTypewriterMode = !isTypewriterMode
    if (isTypewriterMode) {
      toggleTypewriterBtn.style = "-fx-border-color: #3B82F6; -fx-text-fill: #3B82F6;"
      contentView.positionCaret(contentView.caretPosition)
    } else {
      toggleTypewriterBtn.style = ""
    }
  }

  private fun toggleAiDrawer() {
    val show = !aiDrawer.isVisible
    aiDrawer.isVisible = show
    aiDrawer.isManaged = show
    if (show) {
      backupsDrawer.isVisible = false
      backupsDrawer.isManaged = false
    }
  }

  private fun toggleBackupsDrawer() {
    val show = !backupsDrawer.isVisible
    backupsDrawer.isVisible = show
    backupsDrawer.isManaged = show
    if (show) {
      aiDrawer.isVisible = false
      aiDrawer.isManaged = false
      refreshBackups()
    }
  }

  private fun refreshAiModels() {
    Completable.fromAction {
      val models = ollamaClient.listModels()
      Platform.runLater {
        if (models.isNotEmpty()) {
          aiModelField.text = models.first()
          Settings.ollamaModel = models.first()
        }
      }
    }.subscribeOn(Schedulers.io()).subscribe({}, {})
  }

  private fun triggerAiAction(action: String) {
    val fullText = contentView.text ?: ""
    val selectedText = contentView.selectedText ?: ""
    val targetText = if (selectedText.isNotBlank()) selectedText else fullText

    if (targetText.isBlank()) {
      aiResponseArea.text = "Document buffer is empty."
      return
    }

    val (systemPrompt, userPrompt) = when (action) {
      "continue" -> Pair(
        "You are an expert creative writer. Continue the story naturally in the same style and language without repeating what came before.",
        targetText
      )
      "polish" -> Pair(
        "You are a professional editor. Improve flow, grammar, diction, and readability while preserving the original meaning and language. Return only the revised text.",
        targetText
      )
      "summarize" -> Pair(
        "You are a concise summarizer. Provide a clean, insightful summary of the following content in the same language.",
        targetText
      )
      else -> Pair(null, targetText)
    }

    sendToOllama(systemPrompt, userPrompt)
  }

  private fun triggerAiCustomPrompt() {
    val customInstruction = aiPromptArea.text?.trim() ?: ""
    val fullText = contentView.text ?: ""
    val selectedText = contentView.selectedText ?: ""
    val contextText = if (selectedText.isNotBlank()) selectedText else fullText

    if (customInstruction.isBlank() && contextText.isBlank()) {
      aiResponseArea.text = "Please enter an instruction or write some text."
      return
    }

    val userPrompt = if (contextText.isNotBlank()) {
      "$customInstruction\n\n--- Context ---\n$contextText"
    } else {
      customInstruction
    }

    sendToOllama("You are a helpful creative writing AI assistant for PureWPlus.", userPrompt)
  }

  private fun sendToOllama(systemPrompt: String?, userPrompt: String) {
    aiResponseArea.text = "Thinking..."
    val model = aiModelField.text.trim().ifEmpty { "llama3" }

    Completable.fromAction {
      try {
        val reply = ollamaClient.generate(model, userPrompt, systemPrompt)
        Platform.runLater {
          aiResponseArea.text = reply
        }
      } catch (e: Exception) {
        Platform.runLater {
          aiResponseArea.text = "Error connecting to Ollama:\n${e.message}\n\nMake sure Ollama is running (`ollama serve`)."
        }
      }
    }.subscribeOn(Schedulers.io()).subscribe({}, {})
  }

  private fun insertAiResponse() {
    val reply = aiResponseArea.text ?: return
    if (reply.isBlank() || reply.startsWith("Thinking...") || reply.startsWith("Error")) return
    val caret = contentView.caretPosition
    contentView.insertText(caret, "\n\n$reply\n\n")
  }

  private fun replaceWithAiResponse() {
    val reply = aiResponseArea.text ?: return
    if (reply.isBlank() || reply.startsWith("Thinking...") || reply.startsWith("Error")) return
    // Pre-replace backup snapshot
    coordinator.backupEngine.createBackup("active", contentView.text ?: "", title, reason = "pre-ai-replace")
    contentView.replaceText(reply)
  }

  private fun refreshBackups() {
    backupEntries = coordinator.backupEngine.listBackups("active")
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val items = if (backupEntries.isEmpty()) {
      listOf(I18n.get("drawer.no_backups"))
    } else {
      backupEntries.map { entry ->
        val date = df.format(Date(entry.timestamp))
        val size = if (entry.file.exists()) "${entry.file.length()} B" else ""
        "$date · ${entry.reason}  $size"
      }
    }
    backupsListView.items = FXCollections.observableArrayList(items)
  }

  private fun restoreSelectedBackup() {
    val selectedIndex = backupsListView.selectionModel.selectedIndex
    if (selectedIndex < 0 || selectedIndex >= backupEntries.size) return
    val entry = backupEntries[selectedIndex]
    val restoredText = coordinator.backupEngine.readBackupContent(entry.file)

    // Pre-restore snapshot for zero data loss
    coordinator.backupEngine.createBackup("active", contentView.text ?: "", title, reason = "pre-restore")
    coordinator.handleLocalTextChange(restoredText, channel)
    contentView.replaceText(restoredText)
    setSaveStatus(false)
    refreshBackups()
  }

  private fun showEmpty() {
    emptyView.text = I18n.get("empty.title", version)
    emptyView.isVisible = true
    if (contentView.text.isNotEmpty()) {
      coordinator.backupEngine.createBackup("active", contentView.text, title, reason = "pre-empty-state")
    }
    isSyncingContent = true
    contentView.replaceText("")
    contentView.isVisible = false
    isSyncingTitle = true
    runCatching { mainStage.title = stageTitle }
  }

  private fun hideEmpty() {
    if (emptyView.isVisible) {
      emptyView.isVisible = false
    }
  }

  private var isIpViewsInitialized = false

  private fun showIpViews() {
    lastConnectingIp = null
    if (ipLayout.isVisible && isIpViewsInitialized) {
      if (ipView.text.isNotBlank() && !PingPongDelegate.isJustDisconnected) {
        ipObserver.changed(null, ipView.text, ipView.text)
      } else {
        ipView.text = Settings.ip
        PingPongDelegate.isJustDisconnected = false
      }
      return
    }

    ipNoteLabel.text = I18n.get("ip.note")
    ipBottomLabel.text = I18n.get("ip.bottom", version, appVersion)

    ipLayout.isVisible = true
    isSyncingTitle = true
    runCatching { mainStage.title = "PureWPlus (${I18n.get("status.disconnected")})" }
    isSyncingTitle = false
    Platform.runLater { ipView.requestFocus() }
    ipView.textProperty().removeListener(ipObserver)
    ipView.textProperty().addListener(ipObserver)
    isIpViewsInitialized = true
  }

  private val ipObserver = ChangeListener<String> { _, _, newValue ->
    var ip = newValue.trim().replace("。", ".")
    if (!ip.contains(".")) {
      ip = "192.168.1.$ip"
    }
    Log.d("Input IP: $ip")
    if (ip.matches("((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}".toRegex())) {
      startNettyClient(ip)
    }
  }

  private fun hideIP() {
    ipView.textProperty().removeListener(ipObserver)
    ipLayout.isVisible = false
    isIpViewsInitialized = false
  }

  private fun invalidate(
    title: String = runCatching { mainStage.title }.getOrNull() ?: this.title,
    content: String = contentView.text ?: "",
    selection: IndexRange = contentView.selection
  ) {
    Log.d("invalidate: $title, $content, $selection")
    channel?.sendArticle(
      title = title,
      content = content,
      start = selection.start,
      end = selection.end
    )
  }

  private var clientDisposable: Disposable? = null
  private var lastConnectingIp: String? = null

  private fun setupAutoConnect() {
    val savedIp = Settings.ip
    if (savedIp.isNotBlank() && savedIp.matches("((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}".toRegex())) {
      startNettyClient(savedIp)
    }
  }

  private fun startNettyClient(ip: String) {
    if (channel?.isActive == true) return
    Settings.ip = ip
    clientDisposable?.dispose()
    clientDisposable = Completable.fromAction { client.start(ip, 19621) }
      .subscribeOn(Schedulers.io())
      .subscribe({}, {
        Log.d("Client run error: ${it.message}")
      })
  }
}
