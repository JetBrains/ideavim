/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.changelist

import com.intellij.openapi.components.Service
import org.jetbrains.annotations.TestOnly

/**
 * Per-project change list backing `g;` and `g,` (`:help changelist`).
 *
 * Index/merge semantics follow Neovim's `get_changelist` (`src/nvim/mark.c`)
 * and `changed_common` (`src/nvim/change.c`): after each recorded change the
 * index sits past the end, so the first `g;` lands on the newest entry.
 */
@Service(Service.Level.APP)
internal class ChangeListService {

  private val projectToChanges = mutableMapOf<String, MutableList<Change>>()
  private val projectToIndex = mutableMapOf<String, Int>()

  data class Change(
    val line: Int,
    val col: Int,
    val filepath: String,
    val protocol: String,
  )

  sealed interface MoveResult {
    object Empty : MoveResult
    object AtStart : MoveResult
    object AtEnd : MoveResult
    data class At(val change: Change) : MoveResult
  }

  @Synchronized
  fun addChange(projectId: String, change: Change) {
    val list = projectToChanges.getOrPut(projectId) { mutableListOf() }
    if (list.lastOrNull()?.shouldMergeWith(change) == true) {
      list[list.lastIndex] = change
    } else {
      list.add(change)
      if (list.size > CHANGE_LIST_LIMIT) list.removeAt(0)
    }
    projectToIndex[projectId] = list.size
  }

  @Synchronized
  fun goToChange(projectId: String, count: Int): MoveResult {
    val list = projectToChanges[projectId]
    if (list.isNullOrEmpty()) return MoveResult.Empty

    val current = projectToIndex.getOrPut(projectId) { list.size }
    val target = current + count

    if (target < 0 && current == 0) return MoveResult.AtStart
    if (target >= list.size && current == list.size - 1) return MoveResult.AtEnd

    val newIndex = target.coerceIn(0, list.size - 1)
    projectToIndex[projectId] = newIndex
    return MoveResult.At(list[newIndex])
  }

  private fun Change.shouldMergeWith(next: Change): Boolean =
    filepath == next.filepath &&
      line == next.line &&
      kotlin.math.abs(col - next.col) < TEXTWIDTH_FALLBACK

  @TestOnly
  @Synchronized
  fun reset() {
    projectToChanges.clear()
    projectToIndex.clear()
  }

  companion object {
    private const val CHANGE_LIST_LIMIT = 100
    private const val TEXTWIDTH_FALLBACK = 79
  }
}
