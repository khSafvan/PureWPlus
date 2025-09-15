/*
 * Copyright (C) 2019 Drakeet Xu <drakeet@drakeet.com>
 *
 * This file is part of Pure Writer Desktop
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

import javafx.stage.Stage
import java.util.*

/**
 * @author Drakeet Xu
 */
lateinit var mainStage: Stage

var isSyncingTitle: Boolean = false
var isSyncingContent: Boolean = false
var isSyncingSelection: Boolean = false

fun beginSyncing() {
  isSyncingTitle = true
  isSyncingContent = true
  isSyncingSelection = true
}

val isSyncing: Boolean
  get() {
    return isSyncingTitle || isSyncingContent || isSyncingSelection
  }

val isZh get() = Locale.getDefault().language == Locale.CHINA.language

val isTraditionalChinese
  get() = with(Locale.getDefault()) {
    this == Locale.TRADITIONAL_CHINESE || this == Locale("zh", "HK")
  }

private val zhHansToHant = mapOf(
  '纯' to '純', '写' to '寫', '开' to '開', '关' to '關',
  '请' to '請', '机' to '機', '连' to '連', '与' to '與',
  '断' to '斷', '点' to '點', '击' to '擊', '顶' to '頂',
  '云' to '雲', '图' to '圖', '标' to '標', '获' to '獲',
  '于' to '於', '确' to '確', '将' to '將', '会' to '會',
  '识' to '識', '别' to '別', '为' to '為', '组' to '組',
  '数' to '數', '动' to '動', '当' to '當', '须' to '須',
  '协' to '協'
)

fun String.toTraditionalIfNeeded(): String {
  if (!isZh || !isTraditionalChinese) return this
  val sb = StringBuilder(length)
  for (ch in this) {
    sb.append(zhHansToHant[ch] ?: ch)
  }
  return sb.toString()
}

val stageTitle
  get() = when {
    isZh -> "纯纯写作".toTraditionalIfNeeded()
    else -> "Pure Writer"
  }
