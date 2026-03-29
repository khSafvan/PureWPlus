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

import org.junit.Assert.*
import org.junit.Test

class MarkdownOutlineTest {

  @Test
  fun testParseHeadings() {
    val md = """
      # Chapter 1: Introduction
      Some text
      ## Section 1.1: Background
      More text
      ### Subsection 1.1.1
      Even more text
      # Chapter 2: Methods
    """.trimIndent()

    val headings = MarkdownOutline.parseHeadings(md)
    assertEquals(4, headings.size)
    assertEquals(1, headings[0].level)
    assertEquals("Chapter 1: Introduction", headings[0].title)
    assertEquals(1, headings[0].lineNumber)

    assertEquals(2, headings[1].level)
    assertEquals("Section 1.1: Background", headings[1].title)
    assertEquals(3, headings[1].lineNumber)

    assertEquals(3, headings[2].level)
    assertEquals("Subsection 1.1.1", headings[2].title)

    assertEquals(1, headings[3].level)
    assertEquals("Chapter 2: Methods", headings[3].title)
  }

  @Test
  fun testComputeStats() {
    val text = "The quick brown fox jumps over the lazy dog.\n\nSecond paragraph here."
    val stats = MarkdownOutline.computeStats(text)
    assertEquals(12, stats.words)
    assertEquals(text.length, stats.characters)
    assertEquals(2, stats.paragraphs)
    assertTrue(stats.readingTimeMinutes > 0.0)
  }
}
