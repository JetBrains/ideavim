/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.diff

import com.intellij.diff.util.Side
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.mark.DiffSide
import com.maddyhome.idea.vim.mark.Jump
import com.maddyhome.idea.vim.mark.JumpSource
import com.maddyhome.idea.vim.newapi.vim

/**
 * Jumps recorded in diff tabs: which diff, side and line the caret is on, and how to bring that diff back
 */
internal object DiffJumps {
  /** The jump to the caret of [editor], or null if [editor] is not part of a diff tab */
  fun jumpAtCaret(editor: Editor): Jump? {
    if (editor.editorKind != EditorKind.DIFF) return null
    val project = editor.project ?: return null
    for (tab in DiffTab.allOpen(project)) {
      val position = tab.view?.positionOf(editor) ?: continue
      val file = tab.shownFile ?: return null
      val source = JumpSource.Diff(tab.id, position.side.toDiffSide(), isInFile = tab.fileOn(position.side) != null)
      return Jump(position.line, position.column, file.path, file.fileSystem.protocol, source)
    }
    return null
  }

  /**
   * Brings back the diff [jump] was recorded in, showing the file of the jump, and moves its caret to the jump
   *
   * The diff the jump was recorded in comes first. If it is closed, a jump to the file itself can go to any other diff
   * of the file, because the file looks the same in all of them. A jump to anything else, such as an older revision of
   * the file, has nowhere else to go.
   *
   * @return false if no open diff can show the jump
   */
  fun show(project: Project, jump: Jump, source: JumpSource.Diff): Boolean {
    val path = jump.filepath
    val position = DiffPosition(source.side.toSide(), jump.line, jump.col)
    val tabs = DiffTab.allOpen(project)

    val ownTab = tabs.find { it.id == source.diffId && it.canShow(path) }
    if (ownTab != null) {
      ownTab.show(path) { view -> moveCaret(project, view, position) }
      return true
    }

    val otherTab = if (source.isInFile) otherTabShowing(path, tabs) else null
    if (otherTab != null) {
      otherTab.show(path) { view -> moveCaret(project, view, position.onSideOf(path, otherTab)) }
      return true
    }

    return false
  }

  /** Prefers a tab that already shows [path] over one that has to switch to it */
  private fun otherTabShowing(path: String, tabs: List<DiffTab>): DiffTab? {
    return tabs.filter { it.canShow(path) }.minByOrNull { if (it.shows(path)) 0 else 1 }
  }

  /**
   * The same position, on the side of [tab] that shows the file at [path]. Another diff may show it on the other side
   */
  private fun DiffPosition.onSideOf(path: String, tab: DiffTab): DiffPosition {
    return if (tab.fileOn(side)?.path == path) this else copy(side = side.other())
  }

  private fun moveCaret(project: Project, view: DiffView, position: DiffPosition) {
    val target = view.show(position)
    val editor = target.editor.vim
    val offset = editor.bufferPositionToOffset(BufferPosition(target.line, target.column, false))
    editor.currentCaret().moveToOffset(editor.normalizeOffset(offset, false))
    IdeFocusManager.getInstance(project).requestFocus(target.editor.contentComponent, true)
  }

  private fun Side.toDiffSide(): DiffSide = if (isLeft) DiffSide.LEFT else DiffSide.RIGHT

  private fun DiffSide.toSide(): Side = Side.fromLeft(this == DiffSide.LEFT)
}
