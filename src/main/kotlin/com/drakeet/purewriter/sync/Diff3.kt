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

data class MergeConflict(
  val baseLines: List<String>,
  val localLines: List<String>,
  val remoteLines: List<String>
)

data class MergeResult(
  val hasConflict: Boolean,
  val mergedLines: List<String>,
  val conflicts: List<MergeConflict> = emptyList()
) {
  val mergedText: String get() = mergedLines.joinToString("\n")
}

object Diff3 {

  fun merge(baseText: String, localText: String, remoteText: String): MergeResult {
    if (localText == remoteText) {
      return MergeResult(hasConflict = false, mergedLines = splitLines(localText))
    }
    if (localText == baseText) {
      return MergeResult(hasConflict = false, mergedLines = splitLines(remoteText))
    }
    if (remoteText == baseText) {
      return MergeResult(hasConflict = false, mergedLines = splitLines(localText))
    }

    val baseLines = splitLines(baseText)
    val localLines = splitLines(localText)
    val remoteLines = splitLines(remoteText)

    return mergeLines(baseLines, localLines, remoteLines)
  }

  fun splitLines(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    return text.split("\r\n", "\n", "\r")
  }

  /**
   * Computes Longest Common Subsequence indices for two lists.
   * Returns list of pairs (indexInA, indexInB).
   */
  fun lcsIndices(a: List<String>, b: List<String>): List<Pair<Int, Int>> {
    val m = a.size
    val n = b.size
    if (m == 0 || n == 0) return emptyList()

    val dp = Array(m + 1) { IntArray(n + 1) }
    for (i in 0 until m) {
      for (j in 0 until n) {
        if (a[i] == b[j]) {
          dp[i + 1][j + 1] = dp[i][j] + 1
        } else {
          dp[i + 1][j + 1] = maxOf(dp[i + 1][j], dp[i][j + 1])
        }
      }
    }

    val matches = mutableListOf<Pair<Int, Int>>()
    var i = m
    var j = n
    while (i > 0 && j > 0) {
      if (a[i - 1] == b[j - 1]) {
        matches.add(Pair(i - 1, j - 1))
        i--
        j--
      } else if (dp[i - 1][j] >= dp[i][j - 1]) {
        i--
      } else {
        j--
      }
    }
    matches.reverse()
    return matches
  }

  data class Anchor(val baseIdx: Int, val localIdx: Int, val remoteIdx: Int)

  fun mergeLines(base: List<String>, local: List<String>, remote: List<String>): MergeResult {
    val lcsLocal = lcsIndices(base, local).toMap()   // baseIdx -> localIdx
    val lcsRemote = lcsIndices(base, remote).toMap() // baseIdx -> remoteIdx

    // Find all base line indices that are preserved in strictly increasing order in BOTH local and remote
    val anchors = mutableListOf<Anchor>()
    var lastLocalIdx = -1
    var lastRemoteIdx = -1

    for (bIdx in base.indices) {
      val lIdx = lcsLocal[bIdx]
      val rIdx = lcsRemote[bIdx]
      if (lIdx != null && rIdx != null) {
        if (lIdx > lastLocalIdx && rIdx > lastRemoteIdx) {
          anchors.add(Anchor(bIdx, lIdx, rIdx))
          lastLocalIdx = lIdx
          lastRemoteIdx = rIdx
        }
      }
    }

    val resultLines = mutableListOf<String>()
    val conflicts = mutableListOf<MergeConflict>()
    var hasConflict = false

    var prevBase = -1
    var prevLocal = -1
    var prevRemote = -1

    fun processChunk(
      baseChunk: List<String>,
      localChunk: List<String>,
      remoteChunk: List<String>
    ) {
      if (localChunk == baseChunk && remoteChunk == baseChunk) {
        resultLines.addAll(baseChunk)
      } else if (localChunk == remoteChunk) {
        resultLines.addAll(localChunk)
      } else if (localChunk != baseChunk && remoteChunk == baseChunk) {
        resultLines.addAll(localChunk)
      } else if (localChunk == baseChunk && remoteChunk != baseChunk) {
        resultLines.addAll(remoteChunk)
      } else {
        // Both sides changed and are not equal: CONFLICT
        hasConflict = true
        conflicts.add(MergeConflict(baseChunk, localChunk, remoteChunk))
        resultLines.add("<<<<<<< Desktop")
        resultLines.addAll(localChunk)
        resultLines.add("=======")
        resultLines.addAll(remoteChunk)
        resultLines.add(">>>>>>> Phone")
      }
    }

    for (anchor in anchors) {
      val bChunk = base.subList(prevBase + 1, anchor.baseIdx)
      val lChunk = local.subList(prevLocal + 1, anchor.localIdx)
      val rChunk = remote.subList(prevRemote + 1, anchor.remoteIdx)

      processChunk(bChunk, lChunk, rChunk)
      resultLines.add(base[anchor.baseIdx])

      prevBase = anchor.baseIdx
      prevLocal = anchor.localIdx
      prevRemote = anchor.remoteIdx
    }

    // Trailing chunk after the last anchor
    val trailingBase = base.subList(prevBase + 1, base.size)
    val trailingLocal = local.subList(prevLocal + 1, local.size)
    val trailingRemote = remote.subList(prevRemote + 1, remote.size)
    processChunk(trailingBase, trailingLocal, trailingRemote)

    return MergeResult(hasConflict, resultLines, conflicts)
  }
}
