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

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.MessageFormat
import java.util.*

class UTF8Control : ResourceBundle.Control() {
  override fun newBundle(
    baseName: String,
    locale: Locale,
    format: String,
    loader: ClassLoader,
    reload: Boolean
  ): ResourceBundle? {
    val bundleName = toBundleName(baseName, locale)
    val resourceName = toResourceName(bundleName, "properties")
    val stream = loader.getResourceAsStream(resourceName) ?: return null
    return InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
      PropertyResourceBundle(reader)
    }
  }
}

object I18n {
  private const val BUNDLE_BASE_NAME = "com/drakeet/purewriter/i18n/messages"

  @Volatile
  var currentLocale: Locale = Locale.getDefault()
    private set

  @Volatile
  private var bundle: ResourceBundle = loadBundle(currentLocale)

  fun setLocale(locale: Locale) {
    currentLocale = locale
    bundle = loadBundle(locale)
  }

  private fun loadBundle(loc: Locale): ResourceBundle {
    val classLoader = I18n::class.java.classLoader
    return try {
      ResourceBundle.getBundle(BUNDLE_BASE_NAME, loc, classLoader, UTF8Control())
    } catch (e: Exception) {
      try {
        ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ENGLISH, classLoader, UTF8Control())
      } catch (ex: Exception) {
        EmptyResourceBundle
      }
    }
  }

  fun get(key: String, vararg args: Any): String {
    val pattern = try {
      bundle.getString(key)
    } catch (e: Exception) {
      key
    }
    return if (args.isNotEmpty()) {
      try {
        MessageFormat.format(pattern, *args)
      } catch (e: Exception) {
        pattern
      }
    } else {
      pattern
    }
  }

  private object EmptyResourceBundle : ResourceBundle() {
    override fun handleGetObject(key: String): Any = key
    override fun getKeys(): Enumeration<String> = Collections.emptyEnumeration()
  }
}
