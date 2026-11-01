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
import com.drakeet.purewriter.storage.DocumentExporter
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
import javafx.stage.FileChooser
import javafx.util.Duration
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object MaterialIcons {
  const val MENU_BOOK = "\uEA19"
  const val KEYBOARD = "\uE312"
  const val HISTORY = "\uE8B3"
  const val DOWNLOAD = "\uF090"
  const val DARK_MODE = "\uE51C"
  const val LIGHT_MODE = "\uE518"
  const val ARROW_UP = "\uE316"
  const val ARROW_DOWN = "\uE313"
  const val FIND_REPLACE = "\uE881"
  const val CLOSE = "\uE5CD"
}

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
  private lateinit var dayNightSwitch: Button

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
  private lateinit var readModeBanner: HBox

  @FXML
  private lateinit var readModeLabel: Label

  @FXML
  private lateinit var exitReadModeBtn: Button

  @FXML
  private lateinit var fontChoiceBtn: Button

  @FXML
  private lateinit var fontDecBtn: Button

  @FXML
  private lateinit var fontSizeLabel: Label

  @FXML
  private lateinit var fontIncBtn: Button

  @FXML
  private lateinit var toggleReadModeBtn: Button

  @FXML
  private lateinit var toggleTypewriterBtn: Button

  @FXML
  private lateinit var toggleDrawerBtn: Button

  @FXML
  private lateinit var exportBtn: Button

  @FXML
  private lateinit var findReplaceBox: VBox

  @FXML
  private lateinit var findField: TextField

  @FXML
  private lateinit var findMatchesLabel: Label

  @FXML
  private lateinit var prevMatchBtn: Button

  @FXML
  private lateinit var nextMatchBtn: Button

  @FXML
  private lateinit var toggleReplaceBtn: Button

  @FXML
  private lateinit var closeFindBtn: Button

  @FXML
  private lateinit var replaceRow: HBox

  @FXML
  private lateinit var replaceField: TextField

  @FXML
  private lateinit var replaceBtn: Button

  @FXML
  private lateinit var replaceAllBtn: Button

  private val client = Client()
  private val coordinator = SyncCoordinator.default

  private val channel: Channel? get() = client.channel

  private var title = "Untitled"
  private var isTypewriterMode = false
  private var isReadMode = false
  private var backupEntries: List<BackupEntry> = emptyList()
  private var searchMatches: List<Int> = emptyList()
  private var currentMatchIndex = -1
  private var isReplaceExpanded = false

  private val fontFamilies = mapOf(
    "sans" to "\"Inter\", \"Source Han Sans CN Light\", \"Source Han Sans CN\", \"Segoe UI\", sans-serif",
    "serif" to "\"DejaVu Serif\", \"Georgia\", \"Source Han Serif SC\", \"Times New Roman\", serif",
    "mono" to "\"JetBrainsMono Nerd Font\", \"FiraCode Nerd Font Mono\", \"DejaVu Sans Mono\", \"Consolas\", monospace"
  )

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

    Font.loadFont(javaClass.getResourceAsStream("MaterialSymbolsOutlined.ttf"), 16.toDouble())

    updateThemeButton()
    dayNightSwitch.setOnAction {
      Settings.darkMode = (!Settings.darkMode)
        .also {
          mainStage.scene.setDarkMode(it)
          applyTypography()
          updateThemeButton()
        }
    }

    // Localize UI labels
    drawerTitleLabel.text = I18n.get("drawer.backups")
    restoreBackupBtn.text = I18n.get("drawer.restore")
    toggleTypewriterBtn.text = I18n.get("menu.typewriter")
    toggleDrawerBtn.text = I18n.get("drawer.backups")
    toggleReadModeBtn.text = I18n.get("menu.read_mode")
    readModeLabel.text = I18n.get("read_mode.active")
    readModeLabel.graphic = createIcon(MaterialIcons.MENU_BOOK, 15.0)
    exitReadModeBtn.text = I18n.get("read_mode.exit")
    exportBtn.text = I18n.get("menu.export")
    findField.promptText = I18n.get("find.placeholder")
    replaceField.promptText = I18n.get("find.replace_placeholder")
    replaceBtn.text = I18n.get("find.replace")
    replaceAllBtn.text = I18n.get("find.replace_all")
    emptyView.text = I18n.get("empty.title", version)
    ipNoteLabel.text = I18n.get("ip.note")
    ipBottomLabel.text = I18n.get("ip.bottom", version, appVersion)

    // Setup Drawer, Typography, and Action listeners
    closeDrawerBtn.setOnAction { toggleBackupsDrawer() }
    toggleDrawerBtn.setOnAction { toggleBackupsDrawer() }
    restoreBackupBtn.setOnAction { restoreSelectedBackup() }
    toggleTypewriterBtn.setOnAction { toggleTypewriterMode() }
    toggleReadModeBtn.setOnAction { toggleReadMode() }
    exitReadModeBtn.setOnAction { setReadMode(false) }
    fontChoiceBtn.setOnAction { cycleFontChoice() }
    fontDecBtn.setOnAction { changeFontSize(-1) }
    fontIncBtn.setOnAction { changeFontSize(1) }
    exportBtn.setOnAction { exportCurrentDocument() }

    // Configure Material Symbols graphics
    toggleReadModeBtn.graphic = createIcon(MaterialIcons.MENU_BOOK)
    toggleTypewriterBtn.graphic = createIcon(MaterialIcons.KEYBOARD)
    exportBtn.graphic = createIcon(MaterialIcons.DOWNLOAD)
    toggleDrawerBtn.graphic = createIcon(MaterialIcons.HISTORY)

    closeDrawerBtn.text = MaterialIcons.CLOSE
    exitReadModeBtn.text = MaterialIcons.CLOSE
    prevMatchBtn.text = MaterialIcons.ARROW_UP
    nextMatchBtn.text = MaterialIcons.ARROW_DOWN
    toggleReplaceBtn.text = MaterialIcons.FIND_REPLACE
    closeFindBtn.text = MaterialIcons.CLOSE

    // Find and Replace listeners
    findField.textProperty().addListener { _, _, newQuery ->
      updateSearch(newQuery ?: "")
    }

    findField.setOnKeyPressed { e ->
      if (e.code == KeyCode.ENTER) {
        if (e.isShiftDown) findPrev() else findNext()
        e.consume()
      } else if (e.code == KeyCode.ESCAPE) {
        hideFindBar()
        e.consume()
      }
    }

    replaceField.setOnKeyPressed { e ->
      if (e.code == KeyCode.ENTER) {
        replaceCurrent()
        e.consume()
      } else if (e.code == KeyCode.ESCAPE) {
        hideFindBar()
        e.consume()
      }
    }

    prevMatchBtn.setOnAction { findPrev() }
    nextMatchBtn.setOnAction { findNext() }
    toggleReplaceBtn.setOnAction { toggleReplaceRow() }
    closeFindBtn.setOnAction { hideFindBar() }
    replaceBtn.setOnAction { replaceCurrent() }
    replaceAllBtn.setOnAction { replaceAll() }

    applyTypography()

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
      if (event.code == KeyCode.ESCAPE) {
        if (findReplaceBox.isVisible) {
          hideFindBar()
          event.consume()
        } else if (isReadMode) {
          setReadMode(false)
          event.consume()
        }
      } else if (event.isShortcutDown && !event.isShiftDown && event.code == KeyCode.F) {
        showFindBar(expandReplace = false)
        event.consume()
      } else if (event.isShortcutDown && !event.isShiftDown && (event.code == KeyCode.H || event.code == KeyCode.R)) {
        showFindBar(expandReplace = true)
        event.consume()
      } else if (event.isShortcutDown && event.isShiftDown && event.code == KeyCode.E) {
        exportCurrentDocument()
        event.consume()
      } else if (event.isShortcutDown && event.code == KeyCode.S) {
        coordinator.documentStore.saveDocument("active", title, contentView.text ?: "", isDirty = false)
        setSaveStatus(true)
        event.consume()
      } else if (event.isShortcutDown && event.isShiftDown && event.code == KeyCode.B) {
        toggleBackupsDrawer()
        event.consume()
      } else if (event.isShortcutDown && event.isShiftDown && event.code == KeyCode.D) {
        // Toggle distraction-free mode (hide/show status bar)
        val visible = !statusBar.isVisible
        statusBar.isVisible = visible
        statusBar.isManaged = visible
        event.consume()
      } else if (event.isShortcutDown && event.isShiftDown && event.code == KeyCode.R) {
        toggleReadMode()
        event.consume()
      } else if (event.isShortcutDown && event.isShiftDown && event.code == KeyCode.F) {
        cycleFontChoice()
        event.consume()
      } else if (event.isShortcutDown && (event.code == KeyCode.EQUALS || event.code == KeyCode.PLUS || event.code == KeyCode.ADD)) {
        changeFontSize(1)
        event.consume()
      } else if (event.isShortcutDown && (event.code == KeyCode.MINUS || event.code == KeyCode.SUBTRACT)) {
        changeFontSize(-1)
        event.consume()
      } else if (event.isShortcutDown && (event.code == KeyCode.DIGIT0 || event.code == KeyCode.NUMPAD0)) {
        Settings.fontSize = 18
        applyTypography()
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

  private fun createIcon(symbol: String, size: Double = 15.0): Label {
    return Label(symbol).apply {
      style = "-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: ${size}px;"
    }
  }

  private fun updateThemeButton() {
    dayNightSwitch.text = if (Settings.darkMode) MaterialIcons.LIGHT_MODE else MaterialIcons.DARK_MODE
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

  private fun cycleFontChoice() {
    val next = when (Settings.fontChoice.toLowerCase()) {
      "sans" -> "serif"
      "serif" -> "mono"
      else -> "sans"
    }
    Settings.fontChoice = next
    applyTypography()
  }

  private fun changeFontSize(delta: Int) {
    val current = Settings.fontSize
    val updated = (current + delta).coerceIn(12, 36)
    if (updated != current) {
      Settings.fontSize = updated
      applyTypography()
    }
  }

  private fun toggleReadMode() {
    setReadMode(!isReadMode)
  }

  private fun setReadMode(active: Boolean) {
    isReadMode = active
    contentView.isEditable = !active
    readModeBanner.isVisible = active
    readModeBanner.isManaged = active

    if (active) {
      if (!contentView.styleClass.contains("read-mode")) {
        contentView.styleClass.add("read-mode")
      }
      toggleReadModeBtn.style = "-fx-border-color: #10B981; -fx-text-fill: #10B981;"
    } else {
      contentView.styleClass.remove("read-mode")
      toggleReadModeBtn.style = ""
    }
    applyTypography()
  }

  private fun applyTypography() {
    val choice = Settings.fontChoice.toLowerCase()
    val family = fontFamilies[choice] ?: fontFamilies["sans"]!!
    val size = Settings.fontSize.coerceIn(12, 36)
    val lineSpacing = if (isReadMode) (size * 0.65).toInt() else 8
    val maxWidth = if (isReadMode) 760.0 else 840.0

    contentView.maxWidth = maxWidth
    fontSizeLabel.text = "$size"
    fontChoiceBtn.text = when (choice) {
      "serif" -> I18n.get("font.serif")
      "mono" -> I18n.get("font.mono")
      else -> I18n.get("font.sans")
    }

    contentView.style = "-fx-font-family: $family; -fx-font-size: ${size}px; -fx-line-spacing: ${lineSpacing}px;"
  }

  private fun showFindBar(expandReplace: Boolean = false) {
    findReplaceBox.isVisible = true
    findReplaceBox.isManaged = true
    if (expandReplace) {
      isReplaceExpanded = true
      replaceRow.isVisible = true
      replaceRow.isManaged = true
    }
    Platform.runLater {
      findField.requestFocus()
      val selectedText = contentView.selectedText
      if (!selectedText.isNullOrEmpty() && !selectedText.contains("\n")) {
        findField.text = selectedText
      }
      findField.selectAll()
      updateSearch(findField.text ?: "")
    }
  }

  private fun hideFindBar() {
    findReplaceBox.isVisible = false
    findReplaceBox.isManaged = false
    searchMatches = emptyList()
    currentMatchIndex = -1
    findMatchesLabel.text = ""
    contentView.requestFocus()
  }

  private fun toggleReplaceRow() {
    isReplaceExpanded = !isReplaceExpanded
    replaceRow.isVisible = isReplaceExpanded
    replaceRow.isManaged = isReplaceExpanded
    if (isReplaceExpanded) {
      replaceField.requestFocus()
    } else {
      findField.requestFocus()
    }
  }

  private fun updateSearch(query: String) {
    val text = contentView.text ?: ""
    if (query.isEmpty() || text.isEmpty()) {
      searchMatches = emptyList()
      currentMatchIndex = -1
      findMatchesLabel.text = ""
      return
    }

    val matches = mutableListOf<Int>()
    val lowerText = text.toLowerCase()
    val lowerQuery = query.toLowerCase()
    var index = lowerText.indexOf(lowerQuery)
    while (index >= 0) {
      matches.add(index)
      index = lowerText.indexOf(lowerQuery, index + lowerQuery.length)
    }

    searchMatches = matches
    if (matches.isNotEmpty()) {
      val caret = contentView.caretPosition
      val closest = matches.indexOfFirst { it >= caret }
      currentMatchIndex = if (closest >= 0) closest else 0
      highlightMatch(currentMatchIndex, query.length)
    } else {
      currentMatchIndex = -1
      findMatchesLabel.text = "0 / 0"
    }
  }

  private fun highlightMatch(index: Int, length: Int) {
    if (index < 0 || index >= searchMatches.size) {
      findMatchesLabel.text = "0 / 0"
      return
    }
    val start = searchMatches[index]
    val end = start + length
    findMatchesLabel.text = "${index + 1} / ${searchMatches.size}"
    contentView.selectRange(start, end)
  }

  private fun findNext() {
    if (searchMatches.isEmpty()) return
    val query = findField.text ?: ""
    if (query.isEmpty()) return
    currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size
    highlightMatch(currentMatchIndex, query.length)
  }

  private fun findPrev() {
    if (searchMatches.isEmpty()) return
    val query = findField.text ?: ""
    if (query.isEmpty()) return
    currentMatchIndex = if (currentMatchIndex - 1 < 0) searchMatches.size - 1 else currentMatchIndex - 1
    highlightMatch(currentMatchIndex, query.length)
  }

  private fun replaceCurrent() {
    val query = findField.text ?: ""
    if (query.isEmpty() || searchMatches.isEmpty() || currentMatchIndex < 0) return
    val replacement = replaceField.text ?: ""
    val start = searchMatches[currentMatchIndex]
    val end = start + query.length

    // Zero data loss: create snapshot
    coordinator.backupEngine.createBackup("active", contentView.text ?: "", title, reason = "pre-replace")

    val currentText = contentView.text ?: ""
    val newText = currentText.substring(0, start) + replacement + currentText.substring(end)
    contentView.replaceText(newText)
    coordinator.handleLocalTextChange(newText, channel)
    setSaveStatus(channel?.isActive == true)

    updateSearch(query)
  }

  private fun replaceAll() {
    val query = findField.text ?: ""
    if (query.isEmpty()) return
    val currentText = contentView.text ?: ""
    if (currentText.isEmpty()) return
    val replacement = replaceField.text ?: ""

    // Zero data loss: create snapshot
    coordinator.backupEngine.createBackup("active", currentText, title, reason = "pre-replace-all")

    val regex = Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
    val newText = regex.replace(currentText, Regex.escapeReplacement(replacement))
    contentView.replaceText(newText)
    coordinator.handleLocalTextChange(newText, channel)
    setSaveStatus(channel?.isActive == true)

    updateSearch(query)
  }

  private fun exportCurrentDocument() {
    val chooser = FileChooser()
    chooser.title = I18n.get("export.title")
    val safeTitle = title.replace("[\\\\/:*?\"<>|]".toRegex(), "_").ifBlank { "Untitled" }
    chooser.initialFileName = "$safeTitle.md"
    chooser.extensionFilters.addAll(
      FileChooser.ExtensionFilter("Markdown (*.md)", "*.md"),
      FileChooser.ExtensionFilter("Plain Text (*.txt)", "*.txt"),
      FileChooser.ExtensionFilter("HTML Document (*.html)", "*.html")
    )
    val file = chooser.showSaveDialog(mainStage) ?: return
    val content = contentView.text ?: ""
    when {
      file.name.endsWith(".txt", ignoreCase = true) -> DocumentExporter.exportToPlainText(title, content, file)
      file.name.endsWith(".html", ignoreCase = true) || file.name.endsWith(".htm", ignoreCase = true) -> DocumentExporter.exportToHtml(title, content, file)
      else -> DocumentExporter.exportToMarkdown(title, content, file)
    }
  }

  private fun toggleBackupsDrawer() {
    val show = !backupsDrawer.isVisible
    backupsDrawer.isVisible = show
    backupsDrawer.isManaged = show
    if (show) {
      refreshBackups()
    }
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
