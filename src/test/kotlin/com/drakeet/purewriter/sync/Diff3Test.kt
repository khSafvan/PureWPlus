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

import org.junit.Assert.*
import org.junit.Test

class Diff3Test {

  @Test
  fun testNoChanges() {
    val base = "Line 1\nLine 2\nLine 3"
    val result = Diff3.merge(base, base, base)
    assertFalse(result.hasConflict)
    assertEquals(base, result.mergedText)
  }

  @Test
  fun testOnlyLocalChanged() {
    val base = "Line 1\nLine 2\nLine 3"
    val local = "Line 1\nLine 2 modified\nLine 3"
    val result = Diff3.merge(base, local, base)
    assertFalse(result.hasConflict)
    assertEquals(local, result.mergedText)
  }

  @Test
  fun testOnlyRemoteChanged() {
    val base = "Line 1\nLine 2\nLine 3"
    val remote = "Line 1\nLine 2\nLine 3 updated"
    val result = Diff3.merge(base, base, remote)
    assertFalse(result.hasConflict)
    assertEquals(remote, result.mergedText)
  }

  @Test
  fun testIdenticalChangesBothSides() {
    val base = "Line 1\nLine 2\nLine 3"
    val local = "Line 1\nLine 2 both\nLine 3"
    val remote = "Line 1\nLine 2 both\nLine 3"
    val result = Diff3.merge(base, local, remote)
    assertFalse(result.hasConflict)
    assertEquals("Line 1\nLine 2 both\nLine 3", result.mergedText)
  }

  @Test
  fun testDisjointNonOverlappingEdits() {
    val base = """
      Title: My Story
      Chapter 1: Intro
      Paragraph 1
      Chapter 2: Journey
      Paragraph 2
      The End
    """.trimIndent()

    // Desktop edits Chapter 1
    val local = """
      Title: My Story
      Chapter 1: Intro - Desktop Polish
      Paragraph 1
      Chapter 2: Journey
      Paragraph 2
      The End
    """.trimIndent()

    // Phone edits Chapter 2
    val remote = """
      Title: My Story
      Chapter 1: Intro
      Paragraph 1
      Chapter 2: Journey - Mobile Edit
      Paragraph 2
      The End
    """.trimIndent()

    val result = Diff3.merge(base, local, remote)
    assertFalse("Disjoint edits must cleanly merge without conflict", result.hasConflict)

    val expected = """
      Title: My Story
      Chapter 1: Intro - Desktop Polish
      Paragraph 1
      Chapter 2: Journey - Mobile Edit
      Paragraph 2
      The End
    """.trimIndent()

    assertEquals(expected, result.mergedText)
  }

  @Test
  fun testConflictingEditsOnSameLine() {
    val base = "Chapter 1\nOriginal text\nEnd"
    val local = "Chapter 1\nDesktop revised text\nEnd"
    val remote = "Chapter 1\nPhone revised text\nEnd"

    val result = Diff3.merge(base, local, remote)
    assertTrue("Conflicting edits on the same line must be detected", result.hasConflict)
    assertEquals(1, result.conflicts.size)
    assertTrue(result.mergedText.contains("<<<<<<< Desktop"))
    assertTrue(result.mergedText.contains("Desktop revised text"))
    assertTrue(result.mergedText.contains("======="))
    assertTrue(result.mergedText.contains("Phone revised text"))
    assertTrue(result.mergedText.contains(">>>>>>> Phone"))
  }

  @Test
  fun testPrefixAndSuffixAdditions() {
    val base = "Middle Line"
    val local = "Desktop Prefix\nMiddle Line"
    val remote = "Middle Line\nPhone Suffix"

    val result = Diff3.merge(base, local, remote)
    assertFalse("Non-overlapping prefix and suffix additions should cleanly merge", result.hasConflict)
    assertEquals("Desktop Prefix\nMiddle Line\nPhone Suffix", result.mergedText)
  }
}
