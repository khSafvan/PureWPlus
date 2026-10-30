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

package com.drakeet.purewriter.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class I18nTest {

  @Test
  fun testDefaultEnglishStrings() {
    I18n.setLocale(Locale.ENGLISH)
    assertEquals("PureWPlus", I18n.get("app.name"))
    assertEquals("Connected", I18n.get("status.connected"))
    assertEquals("Offline", I18n.get("status.disconnected"))
    assertEquals("Read Mode", I18n.get("menu.read_mode"))
    assertEquals("Sans", I18n.get("font.sans"))
    assertEquals("Serif", I18n.get("font.serif"))
    assertEquals("Mono", I18n.get("font.mono"))
    assertEquals("Find...", I18n.get("find.placeholder"))
    assertEquals("Export", I18n.get("menu.export"))

    val formatted = I18n.get("status.words", 100, 500)
    assertTrue("Should format arguments", formatted.contains("100 words"))
  }

  @Test
  fun testSimplifiedChineseStrings() {
    I18n.setLocale(Locale.SIMPLIFIED_CHINESE)
    assertEquals("PureWPlus (纯纯写作)", I18n.get("app.name"))
    assertEquals("已连接", I18n.get("status.connected"))
    assertEquals("未连接", I18n.get("status.disconnected"))
  }

  @Test
  fun testTraditionalChineseStrings() {
    I18n.setLocale(Locale.TRADITIONAL_CHINESE)
    assertEquals("PureWPlus (純純寫作)", I18n.get("app.name"))
    assertEquals("已連接", I18n.get("status.connected"))
    assertEquals("歷史備份", I18n.get("drawer.backups"))
  }

  @Test
  fun testMissingKeyFallback() {
    I18n.setLocale(Locale.ENGLISH)
    val fallback = I18n.get("non.existent.key")
    assertEquals("non.existent.key", fallback)
  }
}
