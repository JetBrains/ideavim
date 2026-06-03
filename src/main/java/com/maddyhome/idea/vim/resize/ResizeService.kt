/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.resize

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.ui.Splitter
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.newapi.ij
import java.awt.Component
import javax.swing.SwingUtilities

/**
 * Resizes editor windows, translating Vim's row-based `:resize` semantics onto IntelliJ's
 * proportion-based [Splitter] model.
 */
internal class ResizeService {

  /**
   * Sets the *height* of the window currently holding [editor] according to [argument].
   *
   * Does nothing if there is no enclosing top/bottom split to resize.
   */
  fun resizeCurrentWindowHeight(editor: VimEditor, argument: ResizeArgument) {
    val ijEditor = editor.ij
    val project = ijEditor.project ?: return

    val window = FileEditorManagerEx.getInstanceEx(project).splitters.currentWindow ?: return

    val (splitter, child) = findEnclosingHeightSplitter(window) ?: return
    val first = splitter.firstComponent ?: return
    val second = splitter.secondComponent ?: return

    // The proportion divides the area shared by the two children (the divider sits between them and is
    // excluded), so the children's current heights sum to that usable height regardless of proportion.
    val usableHeight = first.height + second.height
    if (usableHeight <= 0) return

    val proportion = computeProportion(
      argument = argument,
      usableHeight = usableHeight,
      allocatedHeight = child.height, // height of the current window's slice, chrome included
      visibleTextHeight = ijEditor.scrollingModel.visibleArea.height, // text region only, chrome excluded
      lineHeight = ijEditor.lineHeight,
      firstComponent = child === first,
    )

    splitter.proportion = proportion.coerceIn(MIN_PROPORTION, MAX_PROPORTION)
  }

  private fun computeProportion(
    argument: ResizeArgument,
    usableHeight: Int,
    allocatedHeight: Int,
    visibleTextHeight: Int,
    lineHeight: Int,
    firstComponent: Boolean,
  ): Float {
    // Height, in pixels, the current window's slice should occupy.
    val targetHeight = when (argument) {
      ResizeArgument.Maximize -> return if (firstComponent) MAX_PROPORTION else MIN_PROPORTION

      // Chrome cancels out: grow/shrink the existing slice by N line heights.
      is ResizeArgument.Relative -> allocatedHeight + argument.rows * lineHeight.toFloat()

      // Add back the non-text chrome so `rows` counts visible text lines, not the whole slice.
      is ResizeArgument.Absolute -> {
        val chrome = (allocatedHeight - visibleTextHeight).coerceAtLeast(0)
        argument.rows * lineHeight.toFloat() + chrome
      }
    }

    return if (firstComponent) targetHeight / usableHeight else 1f - targetHeight / usableHeight
  }

  /**
   * Walks up the Swing hierarchy from [window]'s component to the nearest enclosing [Splitter] that
   * controls the window's height, returning that splitter and the direct child the window lives inside
   * (so the caller can tell whether it is the first or second component).
   */
  private fun findEnclosingHeightSplitter(window: EditorWindow): Pair<Splitter, Component>? {
    // A vertical splitter (orientation == true) stacks its children top/bottom, so it controls height.
    val controlsHeight = true
    var child: Component = window.tabbedPane.component
    var parent = child.parent
    while (parent != null) {
      if (parent is Splitter && parent.orientation == controlsHeight && SwingUtilities.isDescendingFrom(child, parent)) {
        return parent to child
      }
      child = parent
      parent = parent.parent
    }
    return null
  }

  companion object {
    private const val MIN_PROPORTION = 0.05f
    private const val MAX_PROPORTION = 0.95f
  }
}
