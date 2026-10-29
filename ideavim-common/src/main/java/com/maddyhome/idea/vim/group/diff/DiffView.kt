/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.diff

import com.intellij.diff.FrameDiffTool
import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.diff.tools.util.base.DiffViewerListener
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer
import com.intellij.diff.util.Side
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition

/** A position in the text of one side of a diff */
internal data class DiffPosition(val side: Side, val line: Int, val column: Int)

/** A position in one of the editors of a diff view */
internal data class DiffEditorPosition(val editor: Editor, val line: Int, val column: Int)

/**
 * The viewer a diff tab shows its diff in
 *
 * The user can switch a tab between side-by-side and unified view at any time, so positions are kept per side, and each
 * view translates them to and from its own editors.
 */
internal sealed interface DiffView {
  /** The position of [caret] in [editor], or null if [editor] is not one of this view's editors */
  fun positionOf(editor: Editor, caret: LogicalPosition): DiffPosition?

  /** Where [position] is shown. A view with a current side makes the side of [position] current */
  fun show(position: DiffPosition): DiffEditorPosition

  /** Runs [action] once the view shows its text. A view may still be computing the diff after it appears */
  fun whenReady(action: () -> Unit)

  class SideBySide(private val viewer: TwosideTextDiffViewer) : DiffView {
    override fun positionOf(editor: Editor, caret: LogicalPosition): DiffPosition? {
      val side = Side.fromValue(viewer.editors, editor) ?: return null
      return DiffPosition(side, caret.line, caret.column)
    }

    override fun show(position: DiffPosition): DiffEditorPosition {
      viewer.currentSide = position.side
      return DiffEditorPosition(viewer.getEditor(position.side), position.line, position.column)
    }

    override fun whenReady(action: () -> Unit) = action()
  }

  /** Both sides in one editor, with the lines of each side interleaved */
  class Unified(private val viewer: UnifiedDiffViewer) : DiffView {
    private val isReady: Boolean
      get() = viewer.diffChanges != null && !viewer.hasPendingRediff()

    override fun positionOf(editor: Editor, caret: LogicalPosition): DiffPosition? {
      if (editor != viewer.editor || !isReady) return null
      // An unchanged line belongs to both sides. The viewer then picks its master side, which shows the file
      val lines = viewer.transferLineFromOneside(caret.line)
      val side = lines.second
      return DiffPosition(side, side.select(lines.first), caret.column)
    }

    override fun show(position: DiffPosition): DiffEditorPosition {
      val line = viewer.transferLineToOneside(position.side, position.line)
      return DiffEditorPosition(viewer.editor, line, position.column)
    }

    override fun whenReady(action: () -> Unit) {
      if (isReady) return action()
      viewer.addListener(object : DiffViewerListener() {
        override fun onAfterRediff() {
          viewer.removeListener(this)
          action()
        }
      })
    }
  }

  companion object {
    fun of(viewer: FrameDiffTool.DiffViewer?): DiffView? = when (viewer) {
      is TwosideTextDiffViewer -> SideBySide(viewer)
      is UnifiedDiffViewer -> Unified(viewer)
      else -> null
    }
  }
}
