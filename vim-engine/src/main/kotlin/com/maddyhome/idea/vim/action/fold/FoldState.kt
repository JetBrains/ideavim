/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.fold

import com.maddyhome.idea.vim.api.Key
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimFoldRegion
import com.maddyhome.idea.vim.api.getOrPutWindowData
import com.maddyhome.idea.vim.api.injector

object FoldState {

  private val foldLevelKey = Key<Int>("foldlevel")

  fun getFoldLevel(editor: VimEditor): Int {
    return injector.vimStorageService.getOrPutWindowData(editor, foldLevelKey) { 0 }
  }

  fun setFoldLevel(editor: VimEditor, level: Int) {
    val coercedLevel = level.coerceAtLeast(0)
    injector.vimStorageService.putDataToWindow(editor, foldLevelKey, coercedLevel)
    applyFoldLevel(editor, coercedLevel)
  }

  fun closeAllFolds(editor: VimEditor) {
    setFoldLevel(editor, 0)
  }

  fun openAllFolds(editor: VimEditor) {
    val allFolds = editor.getAllFoldRegions()
    if (allFolds.isEmpty()) return

    val maxDepth = calculateMaxFoldDepth(allFolds)
    setFoldLevel(editor, maxDepth + 1)
  }

  private fun applyFoldLevel(editor: VimEditor, foldLevel: Int) {
    val allFolds = editor.getAllFoldRegions()
    if (allFolds.isEmpty()) return

    val foldsWithDepth = calculateFoldsDepths(allFolds)

    foldsWithDepth.forEach { (fold, depth) ->
      fold.isExpanded = isBelowFoldLevel(depth, foldLevel)
    }
  }

  private fun isBelowFoldLevel(depth: Int, foldLevel: Int): Boolean = depth < foldLevel

  private fun calculateFoldsDepths(allFolds: List<VimFoldRegion>): List<FoldDepth> = allFolds.map { fold ->
    val depth = calculateFoldDepth(fold, allFolds)
    FoldDepth(fold, depth)
  }

  private fun calculateFoldDepth(fold: VimFoldRegion, allFolds: List<VimFoldRegion>): Int {
    return allFolds.count { otherFold ->
      isStrictlyInsideFold(fold, otherFold)
    }
  }

  private fun isStrictlyInsideFold(fold: VimFoldRegion, otherFold: VimFoldRegion): Boolean {
    if (otherFold.startOffset > fold.startOffset || otherFold.endOffset < fold.endOffset) {
      return false
    }

    return otherFold.startOffset != fold.startOffset || otherFold.endOffset != fold.endOffset
  }

  private fun calculateMaxFoldDepth(allFolds: List<VimFoldRegion>): Int {
    if (allFolds.isEmpty()) return 0
    return calculateFoldsDepths(allFolds).maxOf { it.depth }
  }
}

private data class FoldDepth(val fold: VimFoldRegion, val depth: Int)
