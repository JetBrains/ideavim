/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.editor.FoldRegion

/**
 * Computes nesting depth for each fold region in O(N log N).
 *
 * A fold's depth is the count of other folds that contain it by offset range,
 * excluding folds with an identical (start, end) range.
 */
internal object FoldDepthCalculator {

  fun computeDepths(folds: Array<FoldRegion>): IntArray {
    if (folds.isEmpty()) return IntArray(0)
    val ranges = FoldRanges.from(folds)
    return ranges.sweepDepths(ranges.orderOuterFirst())
  }
}

private class FoldRanges(private val starts: IntArray, private val ends: IntArray) {
  val size: Int get() = starts.size

  fun orderOuterFirst(): IntArray =
    (0 until size).sortedWith(byStartAscendingEndDescending()).toIntArray()

  fun sweepDepths(orderedFolds: IntArray): IntArray {
    val depths = IntArray(size)
    val openFolds = IntArray(size)
    var openCount = 0

    for (fold in orderedFolds) {
      openCount = dropFoldsClosedBefore(openFolds, openCount, fold)
      val duplicates = countDuplicatesAtTop(openFolds, openCount, fold)
      depths[fold] = openCount - duplicates
      openFolds[openCount++] = fold
    }
    return depths
  }

  private fun byStartAscendingEndDescending() = Comparator<Int> { a, b ->
    val byStart = starts[a].compareTo(starts[b])
    if (byStart != 0) byStart else ends[b].compareTo(ends[a])
  }

  private fun dropFoldsClosedBefore(stack: IntArray, stackSize: Int, fold: Int): Int {
    var size = stackSize
    val foldStart = starts[fold]
    while (size > 0 && ends[stack[size - 1]] <= foldStart) size--
    return size
  }

  private fun countDuplicatesAtTop(stack: IntArray, stackSize: Int, fold: Int): Int {
    var count = 0
    var i = stackSize - 1
    while (i >= 0 && hasSameRange(stack[i], fold)) {
      count++
      i--
    }
    return count
  }

  private fun hasSameRange(a: Int, b: Int): Boolean =
    starts[a] == starts[b] && ends[a] == ends[b]

  companion object {
    fun from(folds: Array<FoldRegion>): FoldRanges {
      val starts = IntArray(folds.size) { folds[it].startOffset }
      val ends = IntArray(folds.size) { folds[it].endOffset }
      return FoldRanges(starts, ends)
    }
  }
}
