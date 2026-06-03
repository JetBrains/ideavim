/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.resize

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.ui.Splitter
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.newapi.ij
import java.awt.Component
import java.awt.Container
import java.awt.Rectangle

/**
 * Resizes editor windows, translating Vim's cell-based `:resize` / `:vertical resize` semantics onto
 * IntelliJ's proportion-based [Splitter] model.
 */
internal class ResizeService {

  /** Sets the *height* of the window currently holding [editor] (Vim's `:resize`). */
  fun resizeCurrentWindowHeight(editor: VimEditor, argument: ResizeArgument): Unit =
    resize(editor, argument, Axis.HEIGHT)

  /** Sets the *width* of the window currently holding [editor] (Vim's `:vertical resize`). */
  fun resizeCurrentWindowWidth(editor: VimEditor, argument: ResizeArgument): Unit =
    resize(editor, argument, Axis.WIDTH)

  /**
   * Makes all editor windows (almost) equally high and wide (Vim's `CTRL-W =`).
   *
   * IntelliJ lays windows out as a binary tree of splitters, so equalising both axes at once just means
   * sizing every splitter by how many windows live on each side: a side holding more windows keeps a
   * proportionally larger share. Unlike a single `:resize`, this balances 3-or-more-way splits too.
   */
  fun equalizeWindows(editor: VimEditor) {
    val ijEditor = editor.ij
    val project = ijEditor.project ?: return
    val splittersRoot = FileEditorManagerEx.getInstanceEx(project).splitters

    for (splitter in splittersIn(splittersRoot)) {
      val firstWindows = windowCountIn(splitter.firstComponent ?: continue)
      val secondWindows = windowCountIn(splitter.secondComponent ?: continue)
      val proportion = firstWindows.toFloat() / (firstWindows + secondWindows)
      splitter.proportion = proportion.coerceIn(MIN_PROPORTION, MAX_PROPORTION)
    }
  }

  private fun resize(editor: VimEditor, argument: ResizeArgument, axis: Axis) {
    val ijEditor = editor.ij
    val project = ijEditor.project ?: return

    val window = FileEditorManagerEx.getInstanceEx(project).splitters.currentWindow ?: return

    if (argument == ResizeArgument.Maximize) {
      maximizeWindow(window, axis)
      return
    }

    val (splitter, child) = findEnclosingSplitter(window, axis.splitOrientation) ?: return
    val first = splitter.firstComponent ?: return
    val second = splitter.secondComponent ?: return

    // The proportion divides the area shared by the two children (the divider sits between them and is
    // excluded), so the children's current extents along this axis sum to that usable size.
    val usable = axis.extentOf(first) + axis.extentOf(second)
    if (usable <= 0) return

    val proportion = computeProportion(
      argument = argument,
      usable = usable,
      allocated = axis.extentOf(child), // current window's slice, chrome included
      visibleText = axis.extentOf(ijEditor.scrollingModel.visibleArea), // text region only, chrome excluded
      cellSize = axis.cellSize(ijEditor),
      firstComponent = child === first,
    )

    splitter.proportion = proportion.coerceIn(MIN_PROPORTION, MAX_PROPORTION)
  }

  private fun computeProportion(
    argument: ResizeArgument,
    usable: Int,
    allocated: Int,
    visibleText: Int,
    cellSize: Float,
    firstComponent: Boolean,
  ): Float {
    // Size, in pixels, the current window's slice should occupy along the axis.
    val target = when (argument) {
      // Maximize is fully handled by maximizeWindow() before reaching here.
      ResizeArgument.Maximize -> error("Maximize must not reach computeProportion")

      // Chrome cancels out: grow/shrink the existing slice by N cells.
      is ResizeArgument.Relative -> allocated + argument.count * cellSize

      // Add back the non-text chrome so the count maps to visible cells, not the whole slice.
      is ResizeArgument.Absolute -> {
        val chrome = (allocated - visibleText).coerceAtLeast(0)
        argument.count * cellSize + chrome
      }
    }

    return if (firstComponent) target / usable else 1f - target / usable
  }

  /**
   * Maximises [window] along [axis] (Vim's `CTRL-W _` / `CTRL-W |`).
   */
  private fun maximizeWindow(window: EditorWindow, axis: Axis) {
    for ((splitter, child) in enclosingSplitters(window)) {
      if (splitter.orientation != axis.splitOrientation) continue
      val inFirstComponent = splitter.firstComponent === child
      splitter.proportion = if (inFirstComponent) splitter.maximumProportion else splitter.minimumProportion
    }
  }

  /**
   * Walks up the Swing hierarchy from [window]'s component to the nearest enclosing [Splitter] with the
   * given [orientation][Splitter.getOrientation], returning that splitter and the direct child the window
   * lives inside (so the caller can tell whether it is the first or second component).
   */
  private fun findEnclosingSplitter(window: EditorWindow, orientation: Boolean): Pair<Splitter, Component>? =
    enclosingSplitters(window).firstOrNull { (splitter, _) -> splitter.orientation == orientation }

  /**
   * Every [Splitter] enclosing [window], innermost first, paired with the direct child the path ascends
   * through (so callers can tell which side of each splitter the window is on).
   */
  private fun enclosingSplitters(window: EditorWindow): List<Pair<Splitter, Component>> {
    val result = mutableListOf<Pair<Splitter, Component>>()
    var child: Component = window.tabbedPane.component
    var parent = child.parent
    while (parent != null) {
      if (parent is Splitter) result.add(parent to child)
      child = parent
      parent = parent.parent
    }
    return result
  }

  /** The number of leaf windows under [component]: a binary splitter subtree with k splitters has k + 1. */
  private fun windowCountIn(component: Component): Int = splittersIn(component).size + 1

  private fun splittersIn(component: Component): List<Splitter> = buildList { collectSplitters(component, this) }

  private fun collectSplitters(component: Component, into: MutableList<Splitter>) {
    if (component is Splitter) into.add(component)
    if (component is Container) component.components.forEach { collectSplitters(it, into) }
  }

  /**
   * The dimension a resize acts on. Vim counts rows for height and columns for width; IntelliJ measures
   * both in pixels, so each axis knows its pixel cell size and which Swing extent / splitter to read.
   */
  private enum class Axis {
    /** Window height. Stacked top/bottom by a vertical splitter (orientation == true). */
    HEIGHT {
      override val splitOrientation = true
      override fun extentOf(component: Component) = component.height
      override fun extentOf(rectangle: Rectangle) = rectangle.height
      override fun cellSize(editor: Editor) = editor.lineHeight.toFloat()
    },

    /** Window width. Placed left/right by a horizontal splitter (orientation == false). */
    WIDTH {
      override val splitOrientation = false
      override fun extentOf(component: Component) = component.width
      override fun extentOf(rectangle: Rectangle) = rectangle.width
      override fun cellSize(editor: Editor) = EditorHelper.getPlainSpaceWidthFloat(editor)
    };

    /** The [Splitter.getOrientation] value of the split that resizes this axis. */
    abstract val splitOrientation: Boolean
    abstract fun extentOf(component: Component): Int
    abstract fun extentOf(rectangle: Rectangle): Int
    abstract fun cellSize(editor: Editor): Float
  }

  companion object {
    private const val MIN_PROPORTION = 0.05f
    private const val MAX_PROPORTION = 0.95f
  }
}
