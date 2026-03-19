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

package com.drakeet.purewriter.model

data class HeadingItem(
  val level: Int,
  val title: String,
  val lineNumber: Int
)

data class DocumentStats(
  val words: Int,
  val characters: Int,
  val paragraphs: Int,
  val readingTimeMinutes: Double
)

object MarkdownOutline {

  fun parseHeadings(markdown: String): List<HeadingItem> {
    val items = mutableListOf<HeadingItem>()
    val lines = markdown.split("\r\n", "\n", "\r")
    for ((index, line) in lines.withIndex()) {
      val trimmed = line.trimStart()
      if (trimmed.startsWith("#")) {
        val level = trimmed.takeWhile { it == '#' }.length
        if (level in 1..6 && trimmed.length > level && trimmed[level] == ' ') {
          val headingTitle = trimmed.substring(level).trim()
          items.add(HeadingItem(level, headingTitle, index + 1))
        }
      }
    }
    return items
  }

  fun computeStats(content: String): DocumentStats {
    val trimmed = content.trim()
    val chars = content.length
    val words = if (trimmed.isEmpty()) 0 else trimmed.split("\\s+".toRegex()).size
    val paragraphs = if (trimmed.isEmpty()) 0 else trimmed.split(Regex("(\r?\n){2,}")).size
    val readingTime = words / 220.0
    return DocumentStats(
      words = words,
      characters = chars,
      paragraphs = paragraphs,
      readingTimeMinutes = readingTime
    )
  }
}
