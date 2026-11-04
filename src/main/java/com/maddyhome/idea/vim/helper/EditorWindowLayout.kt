/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.stickyLines.StickyLinesModel
import com.intellij.openapi.editor.impl.stickyLines.VisualStickyLines
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import com.maddyhome.idea.vim.api.getVisualLineCount
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.scroll.WindowLayout
import com.maddyhome.idea.vim.scroll.WindowView
import java.awt.Rectangle
import kotlin.math.max
import kotlin.math.min

/**
 * The vertical layout of an editor in rows, as it is once the editor has finished scrolling.
 *
 * The text area is the viewport below the sticky lines panel, and above the command line or output panel and a
 * transparent horizontal scrollbar, which are drawn over the bottom of the editor. Block inlays are filler between
 * lines, and a line drawn taller than a row, e.g. a rendered doc comment, takes all the rows it covers.
 */
internal class EditorWindowLayout(private val editor: Editor) : WindowLayout {
  val rowHeight: Int = editor.lineHeight
  private val viewport = editor.scrollingModel.visibleAreaOnScrollingFinished
  val stickyLinesHeight: Int = editor.stickyLinesHeightAt(viewport)

  /** The scroll offset */
  val offset: Int = viewport.y

  val textAreaTop: Int = viewport.y + stickyLinesHeight
  val textAreaBottom: Int = viewport.y + viewport.height - bottomPanelsHeight()
  val textAreaHeight: Int = max(0, textAreaBottom - textAreaTop)

  override val lineCount: Int = editor.vim.getVisualLineCount()

  /** What the editor shows now */
  val view: WindowView = currentView()

  // Counted from where the view starts, which is the top of the text area unless the editor was scrolled to show part
  // of a row, e.g. with the mouse wheel
  override val height: Int = max(1, (textAreaBottom - viewTop(view)) / rowHeight)

  override fun lineRows(line: Int): Int = max(1, ceilDiv(lineBottom(line) - lineTop(line), rowHeight))

  override fun fillerRows(line: Int): Int = ceilDiv(fillerHeight(line), rowHeight)

  fun lineTop(line: Int): Int = editor.visualLineToY(line)

  // Includes the full height of a custom fold region, e.g. a rendered doc comment
  fun lineBottom(line: Int): Int = editor.visualLineToYRange(line)[1]

  /** The block inlays between [line] - 1 and [line] */
  fun fillerHeight(line: Int): Int {
    val above = if (line < lineCount) EditorUtil.getInlaysHeight(editor, line, true) else 0
    val below = if (line > 0) EditorUtil.getInlaysHeight(editor, line - 1, false) else 0
    return above + below
  }

  /** The scroll offset that shows [view], below the sticky lines panel as it is now */
  fun offsetOf(view: WindowView): Int =
    (viewTop(view) - stickyLinesHeight).coerceIn(0, max(0, maxOffset()))

  /** True if the top of [view] is not hidden under the sticky lines panel */
  fun isShown(view: WindowView): Boolean = viewTop(view) >= textAreaTop

  /** The scroll offset that puts the bottom of [line] at the bottom of the text area */
  fun offsetToShowAtBottom(line: Int): Int {
    val bottom = if (line < lineCount) lineBottom(line) else lineTop(line) + rowHeight
    return max(0, bottom - (textAreaBottom - offset))
  }

  /** The scroll offset that puts the end of the file, including the block inlays below the last line, at the bottom */
  fun offsetToShowEndOfFile(): Int =
    max(0, lineBottom(lineCount - 1) + fillerHeight(lineCount) - (textAreaBottom - offset))

  /** The scroll offset that puts [line] in the middle of the text area, below the sticky lines panel as it is now */
  fun offsetToCentre(line: Int): Int {
    val lineHeight = lineBottom(line) - lineTop(line)
    // Snap to the row grid, which gives a good position for both an odd and even number of rows
    val rowsAbove = max(0, textAreaHeight - lineHeight) / rowHeight / 2
    return lineTop(line) - rowsAbove * rowHeight - stickyLinesHeight
  }

  private fun currentView(): WindowView {
    if (lineCount == 0) return WindowView(0)
    var line = editor.yToVisualLine(textAreaTop).coerceIn(0, lineCount - 1)
    if (lineTop(line) < textAreaTop && line < lineCount - 1) line++

    val shown = lineTop(line) - textAreaTop
    val fill = fillerRows(line)
    val topFill = when {
      shown <= 0 -> 0
      shown >= gapAbove(line) -> fill
      else -> min(fill, shown / rowHeight)
    }
    return WindowView(line, topFill)
  }

  private fun viewTop(view: WindowView): Int {
    val shownFiller = if (view.topFill >= fillerRows(view.topLine)) {
      gapAbove(view.topLine)
    } else {
      view.topFill * rowHeight
    }
    return lineTop(view.topLine) - shownFiller
  }

  // The space between the line and the one above it, or the top of the editor
  private fun gapAbove(line: Int): Int = lineTop(line) - if (line > 0) lineBottom(line - 1) else 0

  private fun maxOffset(): Int {
    val contentHeight = (editor as? EditorEx)?.contentComponent?.preferredSize?.height ?: return Int.MAX_VALUE
    return contentHeight - viewport.height
  }

  private fun bottomPanelsHeight(): Int = commandLineHeight() + horizontalScrollbarHeight()

  private fun commandLineHeight(): Int =
    injector.commandLine.getActiveCommandLineHeight()?.takeIf { it > 0 }
      ?: injector.outputPanel.getActiveOutputPanelHeight()
      ?: 0

  private fun horizontalScrollbarHeight(): Int {
    // Horizontal scrollbars on macOS are either transparent AND auto-hide, so we don't need to worry about obscured
    // text, or always visible, opaque and outside the content area, so we don't need to adjust for them
    // Transparent scrollbars on Windows and Linux are overlays on the editor content area, and always visible. That
    // means they can obscure text, so we want to adjust by the scrollbar height. If they are not transparent, then they
    // are not overlays and are outside the content area. We don't need to adjust
    if (!SystemInfo.isMac && editor is EditorImpl && Registry.`is`("editor.transparent.scrollbar")) {
      return editor.scrollPane.horizontalScrollBar.height
    }
    return 0
  }

  private fun ceilDiv(value: Int, divisor: Int): Int = if (value <= 0) 0 else (value + divisor - 1) / divisor
}

// The platform hides the sticky lines panel this close to the top of the document, so a sticky line on the first line
// of the file doesn't stay stuck there for good (StickyLinesManager.visibleAreaChanged)
private const val MIN_SCROLL_OFFSET_WITH_STICKY_LINES = 3

/**
 * The height of the sticky lines panel once the editor has finished scrolling to [area].
 *
 * We can't ask the panel. It follows the editor as it scrolls, and with animated scrolling that's after we've returned,
 * so it would still give us the height for where we scrolled from. So we lay out the panel for the target position
 * ourselves, with the same platform code the panel uses.
 *
 * That code is internal API. We use it without a fallback on purpose: if the platform changes it, the build breaks,
 * rather than the scrolling quietly getting it wrong again.
 */
internal fun Editor.stickyLinesHeightAt(area: Rectangle): Int =
  if (isStickyLinesPanelShownAt(area)) layOutStickyLinesPanelAt(area) else 0

private fun Editor.isStickyLinesPanelShownAt(area: Rectangle): Boolean =
  hasStickyLinesPanel && settings.areStickyLinesShown() && area.y >= MIN_SCROLL_OFFSET_WITH_STICKY_LINES

// Only these editors get a panel (EditorImpl.createStickyLinesPanel)
private val Editor.hasStickyLinesPanel: Boolean
  get() = this is EditorImpl && project != null && editorKind == EditorKind.MAIN_EDITOR && !isMirrored

/** Lays out the sticky lines panel for the given visible area and returns its height */
private fun Editor.layOutStickyLinesPanelAt(area: Rectangle): Int {
  val stickyLinesModel = StickyLinesModel.getModel(DocumentMarkupModel.forDocument(document, project, true))
  val panel = VisualStickyLines(this, stickyLinesModel)
  panel.recalculate(area) // Finds the scopes that start above the area
  panel.lines(area) // Stacks them at the top of the area, which sets the height
  return panel.height()
}
