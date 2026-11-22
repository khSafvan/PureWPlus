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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppConfigTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun testDefaultTypographyConfig() {
    val config = AppConfig()
    assertEquals("sans", config.fontChoice)
    assertEquals(18, config.fontSize)
  }

  @Test
  fun testConfigManagerPersistence() {
    val configFile = File(tempFolder.root, "config.json")
    val manager = ConfigManager(configFileProvider = { configFile })

    assertEquals("sans", manager.config.fontChoice)
    assertEquals(18, manager.config.fontSize)

    manager.update {
      fontChoice = "serif"
      fontSize = 22
    }

    assertTrue(configFile.exists())

    val reloadedManager = ConfigManager(configFileProvider = { configFile })
    assertEquals("serif", reloadedManager.config.fontChoice)
    assertEquals(22, reloadedManager.config.fontSize)
  }

  @Test
  fun testMaterialSymbolsFontLoad() {
    val stream = javaClass.getResourceAsStream("/com/drakeet/purewriter/MaterialSymbolsOutlined.ttf")
    assertNotNull("Font resource should exist", stream)
    stream.use {
      val font = javafx.scene.text.Font.loadFont(it, 16.0)
      println("Loaded font from resource: ${font?.name}, family: ${font?.family}")
      assertNotNull("Font should load from resource", font)
      assertEquals("Material Symbols Outlined", font.family)
    }
  }
}
