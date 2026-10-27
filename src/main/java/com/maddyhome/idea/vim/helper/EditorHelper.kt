/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.execution.console.LanguageConsoleView
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.originalFileOrSelf
import com.intellij.testFramework.LightVirtualFile
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getVisualLineCount
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.api.normalizeVisualLine
import com.maddyhome.idea.vim.common.IndentConfig
import com.maddyhome.idea.vim.newapi.IjVimDocument
import com.maddyhome.idea.vim.newapi.IjVimEditor
import java.awt.Font
import java.awt.Point
import java.awt.Rectangle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * This is a set of helper methods for working with editors. All line and column values are zero based.
 */
object EditorHelper {
  const val PYTHON_CONSOLE_FILE_NAME: String = "Python Console.py"
  const val PYTHON_CONSOLE_TOOL_WINDOW_ID: String = "Python Console"
  private const val PYDEV_CONSOLE_KEY_NAME = "PYDEV_CONSOLE_KEY"
  private const val AIR_PROMPT_DOCUMENT_KEY_NAME = "AIR_PROMPT_INPUT_DOCUMENT"
  private const val AIR_PROMPT_FILE_NAME_PREFIX = "AirPrompt-"
  private const val AI_ASSISTANT_INPUT_EDITOR_KEY_NAME = "AI_ASSISTANT_INPUT_EDITOR"
  private const val AI_ASSISTANT_INPUT_FILE_NAME_PREFIX = "AIAssistantInput-"

  // Set a max height on block inlays to be made visible at the top/bottom of a line when scrolling up/down. This
  // mitigates the visible area bouncing around too much and even pushing the cursor line off screen with large
  // multiline rendered doc comments, while still providing some visibility of the block inlay (e.g. Rider's single line
  // Code Vision)
  private const val BLOCK_INLAY_MAX_LINE_HEIGHT = 3

  private const val MAX_STICKY_LINES_ADJUSTMENTS = 3

  /**
   * The part of the editor that shows text: the viewport, minus the sticky lines panel at the top
   */
  @JvmStatic
  fun getVisibleArea(editor: Editor): Rectangle {
    val viewport = editor.viewport
    val stickyLinesHeight = editor.stickyLinesHeight
    if (stickyLinesHeight <= 0) return viewport
    val height = max(0, viewport.height - stickyLinesHeight)
    return Rectangle(viewport.x, viewport.y + stickyLinesHeight, viewport.width, height)
  }

  @JvmStatic
  fun getVisualLineAtTopOfScreen(editor: Editor): Int {
    val visibleArea = getVisibleArea(editor)
    return getFullVisualLine(editor, visibleArea.y, visibleArea.y, visibleArea.y + visibleArea.height)
  }

  @JvmStatic
  fun getVisualLineAtMiddleOfScreen(editor: Editor): Int {
    // The editor will return line numbers of virtual space if the text doesn't reach the end of the visible area
    // (either because it's too short, or it's been scrolled up)
    val lastLineBaseline = editor.logicalPositionToXY(LogicalPosition(IjVimEditor(editor).lineCount(), 0)).y
    val visibleArea = getVisibleArea(editor)
    val height = min(lastLineBaseline - visibleArea.y, visibleArea.height)
    return editor.yToVisualLine(visibleArea.y + height / 2)
  }

  @JvmStatic
  fun getNonNormalizedVisualLineAtBottomOfScreen(editor: Editor): Int {
    // The editor will return line numbers of virtual space if the text doesn't reach the end of the visible area
    // (either because it's too short, or it's been scrolled up)
    val visibleArea = getVisibleArea(editor)
    val height = visibleArea.height - getExEntryHeight() - getHorizontalScrollbarHeight(editor)
    return getFullVisualLine(editor, visibleArea.y + height, visibleArea.y, visibleArea.y + height)
  }

  @JvmStatic
  fun getVisualLineAtBottomOfScreen(editor: Editor): Int =
    IjVimEditor(editor).normalizeVisualLine(getNonNormalizedVisualLineAtBottomOfScreen(editor))

  @JvmStatic
  fun getVisualLineLength(editor: Editor, visualLine: Int): Int =
    EditorUtil.getLastVisualLineColumnNumber(editor, visualLine)

  @JvmStatic
  fun normalizeVisualColumn(editor: Editor, visualLine: Int, col: Int, allowEnd: Boolean): Int =
    max(0, min(col, getVisualLineLength(editor, visualLine) - if (allowEnd) 0 else 1))

  /**
   * Best efforts to ensure that scroll offset doesn't overlap itself.
   *
   * This is a sanity check that works fine if there are no visible block inlays. Otherwise, the screen height depends
   * on what block inlays are currently visible in the target scroll area. Given a large enough scroll offset (or small
   * enough screen), we can return a scroll offset that takes us over the half way point and causes scrolling issues -
   * skipped lines, or unexpected movement.
   *
   * TODO: Investigate better ways of handling scroll offset
   * Perhaps apply scroll offset after the move itself? Calculate a safe offset based on a target area?
   *
   * @param editor       The editor to use to normalize the scroll offset
   * @param scrollOffset The value of the 'scrolloff' option
   * @return The scroll offset value to use
   */
  @JvmStatic
  fun normalizeScrollOffset(editor: Editor, scrollOffset: Int): Int =
    min(scrollOffset, getApproximateScreenHeight(editor) / 2)

  /**
   * Best efforts to ensure the side scroll offset doesn't overlap itself and remains a sensible value. Inline inlays
   * can cause this to work incorrectly.
   *
   * @param editor           The editor to use to normalize the side scroll offset
   * @param sideScrollOffset The value of the 'sidescroll' option
   * @return The side scroll offset value to use
   */
  @JvmStatic
  fun normalizeSideScrollOffset(editor: Editor, sideScrollOffset: Int): Int =
    min(sideScrollOffset, getApproximateScreenWidth(editor) / 2)

  /**
   * Gets the number of lines than can be displayed on the screen at one time.
   *
   * Note that this value is only approximate and should be avoided whenever possible!
   */
  @JvmStatic
  fun getApproximateScreenHeight(editor: Editor): Int = getVisibleArea(editor).height / editor.lineHeight

  /**
   * Gets the number of characters that are visible on a screen line, based on screen width and assuming a fixed width
   * font. It does not include inlays or folds.
   *
   * Note that this value is only approximate and should be avoided whenever possible!
   */
  @JvmStatic
  fun getApproximateScreenWidth(editor: Editor): Int =
    (getVisibleArea(editor).width / getPlainSpaceWidthFloat(editor)).toInt()

  /**
   * Gets the number of characters that can be fit inside the output panel for an editor.
   *
   * This will be greater than the approximate screen width as it also includes any gutter components in the editor.
   */
  @JvmStatic
  fun getApproximateOutputPanelWidth(editor: Editor): Int =
    (editor.component.width / getPlainSpaceWidthFloat(editor)).toInt()

  /**
   * Gets the width of the space character in the editor's plain font as a float.
   *
   * Font width can be fractional, but [EditorUtil.getPlainSpaceWidth] returns it as an int, which can lead to rounding
   * errors.
   */
  @JvmStatic
  fun getPlainSpaceWidthFloat(editor: Editor): Float =
    EditorUtil.fontForChar(' ', Font.PLAIN, editor).charWidth2D(' '.code)

  /**
   * Gets the visual column at the left of the screen for the given visual line.
   *
   * @param visualLine The visual line to use to check for inlays and support non-proportional fonts
   */
  @JvmStatic
  fun getVisualColumnAtLeftOfDisplay(editor: Editor, visualLine: Int): Int {
    val area = getVisibleArea(editor)
    return getFullVisualColumn(editor, area.x, editor.visualLineToY(visualLine), area.x, area.x + area.width)
  }

  /**
   * Gets the visual column at the right of the screen for the given visual line.
   *
   * @param visualLine The visual line to use to check for inlays and support non-proportional fonts
   */
  @JvmStatic
  fun getVisualColumnAtRightOfDisplay(editor: Editor, visualLine: Int): Int {
    val area = getVisibleArea(editor)
    val y = editor.visualLineToY(visualLine)
    return getFullVisualColumn(editor, area.x + area.width - 1, y, area.x, area.x + area.width)
  }

  @JvmStatic
  fun scrollVertically(editor: Editor, verticalOffset: Int): Boolean {
    val scrollingModel = editor.scrollingModel
    val area = scrollingModel.visibleAreaOnScrollingFinished
    scrollingModel.scroll(area.x, verticalOffset)
    // Simulate Vim's redraw (essentially to clear messages) if the screen is scrolled
    if (area.y != verticalOffset && area.y >= 0) {
      injector.redrawService.redraw()
    }
    return scrollingModel.visibleAreaOnScrollingFinished.y != area.y
  }

  /**
   * Scrolls the editor to put the given visual line at the top of the current window, below the sticky lines panel.
   * Ensures that any block inlay elements above the given line are also visible.
   *
   * @return Returns true if the window was moved
   */
  @JvmStatic
  fun scrollVisualLineToTopOfScreen(editor: Editor, visualLine: Int): Boolean {
    val scrolled = scrollVertically(editor, editor.offsetToScrollVisualLineToTop(visualLine))
    val adjusted = editor.scrollUntilStickyLinesSettle(
      isInPlace = { !editor.isHiddenUnderStickyLines(visualLine) },
      targetOffset = { editor.offsetToScrollVisualLineToTop(visualLine) },
    )
    return scrolled || adjusted
  }

  /**
   * Scrolls the editor to place the given visual line in the middle of the current window, below the sticky lines
   * panel.
   *
   * @param allowVirtualSpace For `zz`, we want to use virtual space and move any line, including the last one, to the
   * middle of the screen. For `G` or `zb`, do not allow virtual space, so only scroll far enough to keep the last line
   * at the bottom of the screen
   * @param caretChangedLine True if the caret has just moved to this line, so we scroll even if the new position is
   * only as close to the middle as the current one
   */
  @JvmStatic
  fun scrollVisualLineToMiddleOfScreen(
    editor: Editor,
    visualLine: Int,
    allowVirtualSpace: Boolean,
    caretChangedLine: Boolean = false,
  ) {
    val lineY = editor.visualLineToY(IjVimEditor(editor).normalizeVisualLine(visualLine))
    editor.scrollToClosestToCentre(lineY, allowVirtualSpace, caretChangedLine)
  }

  /**
   * Scrolls the editor to place the given visual line at the bottom of the screen.
   *
   * When we're moving the caret down a few lines and want to scroll to keep this visible, we need to be able to place
   * a line at the bottom of the screen. Due to block inlays, we can't do this by specifying a top line to scroll to.
   *
   * @param nonNormalisedVisualLine The non-normalised visual line to place at the bottom of the current window. Might
   * be greater than visual line count to scroll to virtual space at the end of the file
   * @return True if the editor was scrolled
   */
  @JvmStatic
  fun scrollVisualLineToBottomOfScreen(editor: Editor, nonNormalisedVisualLine: Int): Boolean =
    scrollVertically(editor, editor.offsetToScrollVisualLineToBottom(nonNormalisedVisualLine))

  /**
   * Scrolls the editor to put the given visual line at the current caret location, relative to the screen, as long as
   * this doesn't add virtual space to the bottom of the file.
   *
   * Due to block inlays, the caret location is maintained as a scroll offset, rather than the number of lines from the
   * top of the screen. This means the line offset can change if the number of inlays above the caret changes during
   * scrolling. It also means that after scrolling, the top screen line isn't guaranteed to be aligned to the top of
   * the screen, unlike most other motions ('M' is the only other motion that doesn't align the top line).
   *
   * This method will also move the caret location to ensure that any inlays attached above or below the target line
   * are fully visible.
   */
  @JvmStatic
  fun scrollVisualLineToCaretLocation(editor: Editor, visualLine: Int) {
    val visibleArea = getVisibleArea(editor)
    val caretScreenOffset = editor.visualLineToY(editor.caretModel.visualPosition.line) - visibleArea.y

    // We try to keep the caret in the same location, but only if there's enough space all around for the line's
    // inlays. E.g. caret on top screen line and the line has inlays above, or caret on bottom screen line and has
    // inlays below
    val topInlayHeight = EditorUtil.getInlaysHeight(editor, visualLine, true)
    val bottomInlayHeight = EditorUtil.getInlaysHeight(editor, visualLine, false)
    val inlayOffset = when {
      topInlayHeight > caretScreenOffset -> topInlayHeight
      bottomInlayHeight > visibleArea.height - caretScreenOffset + editor.lineHeight -> -bottomInlayHeight
      else -> 0
    }

    // Scroll the given visual line to the caret location, but do not scroll down passed the end of file, or the current
    // virtual space at the bottom of the screen
    val lastLineAtBottom = max(editor.offsetToScrollVisualLineToBottom(editor.lastVisualLine), visibleArea.y)
    scrollVertically(editor, min(editor.visualLineToY(visualLine) - caretScreenOffset - inlayOffset, lastLineAtBottom))
  }

  /**
   * Scroll page down, moving text up.
   *
   * @return A pair consisting of a flag to show if scrolling was completed, and a visual line to position the caret on
   */
  @JvmStatic
  fun scrollFullPageDown(editor: Editor, pages: Int): Pair<Boolean, Int> {
    val visibleArea = getVisibleArea(editor)
    val lastVisualLine = editor.lastVisualLine

    var y = visibleArea.y + visibleArea.height
    var topBound = visibleArea.y
    var bottomBound = visibleArea.y + visibleArea.height
    var targetTopVisualLine = 0
    var caretVisualLine = -1
    var completed = true

    for (i in 0 until pages) {
      targetTopVisualLine = getFullVisualLine(editor, y, topBound, bottomBound)
      if (targetTopVisualLine >= lastVisualLine) {
        // If we're on the last page, end nicely on the last line, otherwise move the caret to the last line of the file
        caretVisualLine = lastVisualLine
        completed = i == pages - 1
        targetTopVisualLine = lastVisualLine
        break
      }

      // The help page for 'scrolling' states that a page is the number of lines in the window minus two. Scrolling a
      // page adds this page length to the current targetTopVisualLine. Or in other words, scrolling down a page puts
      // the last but one targetTopVisualLine at the top of the next page.
      // E.g. a window showing lines 1-35 has a page size of 33, and scrolling down a page shows 34 as the top line
      targetTopVisualLine--

      y = editor.visualLineToY(targetTopVisualLine)
      topBound = y
      bottomBound = y + visibleArea.height
      y = bottomBound
      caretVisualLine = targetTopVisualLine
    }

    scrollVisualLineToTopOfScreen(editor, targetTopVisualLine)
    return Pair(completed, caretVisualLine)
  }

  /**
   * Scroll page up, moving text down.
   *
   * @return A pair consisting of a flag to show if scrolling was completed, and a visual line to position the caret on
   */
  @JvmStatic
  fun scrollFullPageUp(editor: Editor, pages: Int): Pair<Boolean, Int> {
    val visibleArea = getVisibleArea(editor)
    val lineHeight = editor.lineHeight
    val lastVisualLine = editor.lastVisualLine

    var y = visibleArea.y
    var topBound = visibleArea.y
    var bottomBound = visibleArea.y + visibleArea.height
    var targetBottomVisualLine = 0
    var caretVisualLine = -1
    var completed = true

    for (i in 0 until pages) {
      // Scrolling up puts the current top line plus one at the bottom of the screen
      targetBottomVisualLine = getFullVisualLine(editor, y, topBound, bottomBound) + 1
      if (targetBottomVisualLine == 1) {
        completed = i == pages - 1
        break
      } else if (targetBottomVisualLine == lastVisualLine) {
        // Vim normally scrolls up window height minus two. When there are only one or two lines in the screen, due to
        // end of file and virtual space, it scrolls window height minus one, or just plain windows height. IntelliJ
        // doesn't allow us only one line when virtual space is enabled, so we only need to handle the two line case.
        // Subtract the +1 we added above.
        targetBottomVisualLine--
      }

      y = editor.visualLineToY(targetBottomVisualLine)
      bottomBound = y + lineHeight
      topBound = bottomBound - visibleArea.height
      y = topBound
      caretVisualLine = targetBottomVisualLine
    }

    scrollVisualLineToBottomOfScreen(editor, targetBottomVisualLine)
    return Pair(completed, caretVisualLine)
  }

  @JvmStatic
  fun scrollHorizontally(editor: Editor, horizontalOffset: Int) {
    val scrollingModel = editor.scrollingModel
    val area = scrollingModel.visibleAreaOnScrollingFinished
    scrollingModel.scroll(horizontalOffset, area.y)
    // Simulate Vim's redraw (essentially to clear messages) if the screen is scrolled
    if (area.x != horizontalOffset && area.x >= 0) {
      injector.redrawService.redraw()
    }
  }

  @JvmStatic
  fun scrollColumnToLeftOfScreen(editor: Editor, visualLine: Int, visualColumn: Int) {
    // Requested column might be an inlay (because we do simple arithmetic on visual position, and inlays and folds have
    // a visual position). If it is an inlay and is related to following text, we want to display it, so use it as the
    // target column. If it's an inlay related to preceding text, we don't want to display it at the left of the screen,
    // show the next column instead
    val inlayModel = editor.inlayModel
    val inlay = inlayModel.getInlineElementAt(VisualPosition(visualLine, visualColumn))
    val previousInlay =
      if (visualColumn > 0) inlayModel.getInlineElementAt(VisualPosition(visualLine, visualColumn - 1)) else null
    val targetVisualColumn = when {
      inlay != null && inlay.isRelatedToPrecedingText -> visualColumn + 1
      previousInlay != null && !previousInlay.isRelatedToPrecedingText -> visualColumn - 1
      else -> visualColumn
    }

    val columnLeftX = editor.visualPositionToPoint2D(VisualPosition(visualLine, targetVisualColumn)).x.roundToInt()
    scrollHorizontally(editor, columnLeftX)
  }

  @JvmStatic
  fun scrollColumnToMiddleOfScreen(editor: Editor, visualLine: Int, visualColumn: Int) {
    val point = editor.visualPositionToPoint2D(VisualPosition(visualLine, visualColumn))
    val screenWidth = getVisibleArea(editor).width

    // Snap the column to the nearest standard column grid. This positions us nicely if there are an odd or even number
    // of columns. It also works with inline inlays and folds. It is slightly inaccurate for proportional fonts, but is
    // still a good solution. Besides, what kind of monster uses Vim with proportional fonts?
    val standardColumnWidth = getPlainSpaceWidthFloat(editor)
    val screenMidColumn = (screenWidth / standardColumnWidth / 2).toInt()
    val x = max(0, (point.x - screenMidColumn * standardColumnWidth).roundToInt())
    scrollHorizontally(editor, x)
  }

  @JvmStatic
  fun scrollColumnToRightOfScreen(editor: Editor, visualLine: Int, visualColumn: Int) {
    // Requested column might be an inlay (because we do simple arithmetic on visual position, and inlays and folds have
    // a visual position). If it is an inlay and is related to preceding text, we want to display it, so use it as the
    // target column. If it's an inlay related to following text, we don't want to display it at the right of the
    // screen, show the previous column. If the target column is followed by an inlay which is associated with it, make
    // the inlay the target column so it is visible
    val inlayModel = editor.inlayModel
    val inlay = inlayModel.getInlineElementAt(VisualPosition(visualLine, visualColumn))
    val nextInlay = inlayModel.getInlineElementAt(VisualPosition(visualLine, visualColumn + 1))
    val targetVisualColumn = when {
      inlay != null && !inlay.isRelatedToPrecedingText -> visualColumn - 1
      nextInlay != null && nextInlay.isRelatedToPrecedingText -> visualColumn + 1
      else -> visualColumn
    }

    // Scroll to the left edge of the target column, minus a screenwidth, and adjusted for inlays
    val targetColumnRightX =
      editor.visualPositionToPoint2D(VisualPosition(visualLine, targetVisualColumn + 1)).x.roundToInt()
    scrollHorizontally(editor, targetColumnRightX - getVisibleArea(editor).width)
  }

  /**
   * Scrolls until the target position is reached, measuring the sticky lines panel again after each scroll.
   *
   * The panel shows the lines whose scopes start above the top of the screen, so scrolling changes its height, and
   * anything positioned relative to the panel is then out of place. The panel is updated as soon as we scroll, so we
   * adjust straight away, rather than on the next keystroke.
   */
  private fun Editor.scrollUntilStickyLinesSettle(isInPlace: () -> Boolean, targetOffset: () -> Int): Boolean {
    var scrolled = false
    repeat(MAX_STICKY_LINES_ADJUSTMENTS) {
      if (isInPlace() || !scrollVertically(this, targetOffset())) return scrolled
      scrolled = true
    }
    return scrolled
  }

  /**
   * Scrolls to the position that puts the line closest to the middle of the area below the sticky lines panel.
   *
   * The panel shows the scopes that start above the top of the screen, so the middle below it depends on where we
   * scroll to, and there isn't always an exact middle. E.g. the middle below a two line panel scrolls a method out of
   * the panel, and the middle below the remaining one line panel is back where we started. So we try the middle for
   * each panel we find, and keep the position closest to its own middle.
   *
   * Between two equally close positions, we keep the current one, so typing doesn't scroll back and forth, unless the
   * caret has moved to another line, which should scroll.
   */
  private fun Editor.scrollToClosestToCentre(lineY: Int, allowVirtualSpace: Boolean, caretChangedLine: Boolean) {
    var closestOffset = viewport.y
    var closestDistance = distanceFromCentre(lineY, allowVirtualSpace)
    val triedOffsets = mutableSetOf(closestOffset)

    while (closestDistance > 0 && triedOffsets.size <= MAX_STICKY_LINES_ADJUSTMENTS) {
      val targetOffset = offsetToCentre(lineY, allowVirtualSpace)
      if (targetOffset in triedOffsets || !scrollVertically(this, targetOffset)) break
      triedOffsets += viewport.y

      val distance = distanceFromCentre(lineY, allowVirtualSpace)
      if (distance < closestDistance || distance == closestDistance && caretChangedLine) {
        closestOffset = viewport.y
        closestDistance = distance
      }
    }
    scrollVertically(this, closestOffset)
  }

  // How far we are from where we'd scroll to centre the line, with the sticky lines panel at the current position
  private fun Editor.distanceFromCentre(lineY: Int, allowVirtualSpace: Boolean): Int =
    abs(viewport.y - offsetToCentre(lineY, allowVirtualSpace))

  private fun Editor.isHiddenUnderStickyLines(visualLine: Int): Boolean =
    visualLineToY(visualLine) < getVisibleArea(this).y

  private fun Editor.offsetToCentre(lineY: Int, allowVirtualSpace: Boolean): Int =
    limitToLastLine(offsetToCentreIn(getVisibleArea(this).height, lineY) - stickyLinesHeight, allowVirtualSpace)

  // Snaps the line to the nearest standard line height grid, which gives a good position for both an odd and even
  // number of lines and mimics what Vim does
  private fun Editor.offsetToCentreIn(areaHeight: Int, lineY: Int): Int =
    lineY - (areaHeight - lineHeight) / lineHeight / 2 * lineHeight

  private fun Editor.limitToLastLine(offset: Int, allowVirtualSpace: Boolean): Int =
    if (allowVirtualSpace) offset else min(offset, offsetToScrollVisualLineToBottom(lastVisualLine))

  private fun Editor.offsetToScrollVisualLineToTop(visualLine: Int): Int {
    val inlayHeight = min(EditorUtil.getInlaysHeight(this, visualLine, true), BLOCK_INLAY_MAX_LINE_HEIGHT * lineHeight)
    val y = visualLineToY(visualLine) - inlayHeight - stickyLinesHeight
    if (settings.isAdditionalPageAtBottom) return y

    // Don't try to scroll the editor to a location it can't reach. The editor will handle this, but when we ask for the
    // target location to move the caret to match, we'll get the incorrect value.
    // E.g. from line 100 of a 175 line, with line 100 at the top of screen, hit 100<C-E>. This should scroll line 175
    // to the top of the screen. With virtual space enabled, this is fine. If it's not enabled, we end up scrolling line
    // 146 to the top of the screen, but the caret thinks we're going to 175, and the caret is put in the wrong location
    // (To complicate things, this issue doesn't show up when running headless for tests)
    val virtualSpaceHeight = settings.additionalLinesCount * lineHeight
    val yAfterLastLine = visualLineToY(IjVimEditor(this).lineCount())
    return min(y, yAfterLastLine + virtualSpaceHeight - getVisibleArea(this).height)
  }

  // Note that we explicitly do not normalise the visual line, as we might be trying to scroll a virtual line, at the
  // end of the file
  private fun Editor.offsetToScrollVisualLineToBottom(nonNormalisedVisualLine: Int): Int {
    // Sticky lines reduce the content area at the top, but the bottom of the window is still the bottom of the viewport
    val screenHeight = viewport.height - getExEntryHeight() - getHorizontalScrollbarHeight(this)
    val inlayHeight =
      min(EditorUtil.getInlaysHeight(this, nonNormalisedVisualLine, false), BLOCK_INLAY_MAX_LINE_HEIGHT * lineHeight)
    val y = visualLineToY(nonNormalisedVisualLine) + lineHeight + inlayHeight
    return max(0, y - screenHeight)
  }

  private val Editor.viewport: Rectangle
    get() = scrollingModel.visibleAreaOnScrollingFinished

  private val Editor.stickyLinesHeight: Int
    get() = (this as? EditorImpl)?.stickyLinesPanelHeight ?: 0

  private val Editor.lastVisualLine: Int
    get() = IjVimEditor(this).getVisualLineCount() - 1

  private fun getExEntryHeight(): Int =
    injector.commandLine.getActiveCommandLineHeight() ?: injector.outputPanel.getActiveOutputPanelHeight() ?: 0

  private fun getHorizontalScrollbarHeight(editor: Editor): Int {
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

  private fun getFullVisualLine(editor: Editor, y: Int, topBound: Int, bottomBound: Int): Int {
    // Note that we ignore inlays here. We're interested in the bounds of the text line. Scrolling will handle inlays as
    // it sees fit (e.g. scrolling a line to the bottom will make sure inlays below the line are visible).
    val line = editor.yToVisualLine(y)
    val yActual = editor.visualLineToY(line)
    return when {
      yActual < topBound -> line + 1
      yActual + editor.lineHeight > bottomBound -> line - 1
      else -> line
    }
  }

  private fun getFullVisualColumn(editor: Editor, x: Int, y: Int, leftBound: Int, rightBound: Int): Int {
    // Mapping XY to a visual position will return the position of the closest character, rather than the position of
    // the character grid that contains the XY. This means two things. Firstly, we don't get back the visual position of
    // an inline inlay, and secondly, we can get the character to the left or right of X. This is the same logic for
    // positioning the caret when you click in the editor.
    // Note that visualPos.leansRight will be true for the right half side of the character grid
    var closestVisualPosition = editor.xyToVisualPosition(Point(x, y))

    // Make sure we get the character that contains this XY, not the editor's decision about the closest character. The
    // editor will give us the next character if X is over halfway through the character grid. Take into account that
    // the font size might be fractional, but the editor's area is integer. Use floating point values and round.
    var xActualLeft = editor.visualPositionToPoint2D(closestVisualPosition).x.roundToLong()
    if (xActualLeft > x) {
      closestVisualPosition = getPreviousNonInlayVisualPosition(editor, closestVisualPosition)
      xActualLeft = editor.visualPositionToPoint2D(closestVisualPosition).x.roundToLong()
    }

    if (xActualLeft < leftBound) {
      return getNextNonInlayVisualPosition(editor, closestVisualPosition).column
    }

    val nextVisualPosition = VisualPosition(closestVisualPosition.line, closestVisualPosition.column + 1)
    val xActualRight = editor.visualPositionToPoint2D(nextVisualPosition).x.roundToLong() - 1
    if (xActualRight <= rightBound) {
      return closestVisualPosition.column
    }
    return getPreviousNonInlayVisualPosition(editor, closestVisualPosition).column
  }

  private fun getNextNonInlayVisualPosition(editor: Editor, position: VisualPosition): VisualPosition {
    val inlayModel = editor.inlayModel
    val lineLength = getVisualLineLength(editor, position.line)
    var next = VisualPosition(position.line, position.column + 1)
    while (next.column < lineLength && inlayModel.hasInlineElementAt(next)) {
      next = VisualPosition(next.line, next.column + 1)
    }
    return next
  }

  private fun getPreviousNonInlayVisualPosition(editor: Editor, position: VisualPosition): VisualPosition {
    if (position.column == 0) return position
    val inlayModel = editor.inlayModel
    var previous = VisualPosition(position.line, position.column - 1)
    while (previous.column > 0 && inlayModel.hasInlineElementAt(previous)) {
      previous = VisualPosition(previous.line, previous.column - 1)
    }
    return previous
  }

  @JvmStatic
  fun getOwningEditorWindow(editor: Editor): EditorWindow? {
    val project = editor.project ?: return null
    return FileEditorManagerEx.getInstanceEx(project).windows.firstOrNull { window ->
      window.allComposites.any { composite ->
        composite.allEditors.any { it is TextEditor && it.editor == editor }
      }
    }
  }

  /**
   * Gets the editor for the virtual file within the editor manager.
   *
   * @return The matching editor or null if no match was found
   */
  @JvmStatic
  fun getEditor(file: VirtualFile?): VimEditor? {
    if (file == null) return null
    val document = FileDocumentManager.getInstance().getDocument(file) ?: return null
    return injector.editorGroup.getEditors(IjVimDocument(document)).firstOrNull()
  }

  @JvmStatic
  fun pad(editor: Editor, line: Int, to: Int): String {
    val len = IjVimEditor(editor).lineLength(line)
    if (len >= to) return ""
    return IndentConfig.create(editor).createIndentBySize(to - len)
  }

  /**
   * Get list of all carets from the editor, ordered from the end of the document to the start
   */
  @JvmStatic
  fun getOrderedCaretsList(editor: Editor): List<Caret> = editor.caretModel.allCarets.sortedByDescending { it.offset }

  /**
   * Gets the virtual file associated with this editor
   */
  @JvmStatic
  fun getVirtualFile(editor: Editor): VirtualFile? = FileDocumentManager.getInstance().getFile(editor.document)

  /**
   * Checks if editor is file editor, also it takes into account that editor can be placed in editors hierarchy
   */
  @JvmStatic
  fun isFileEditor(editor: Editor): Boolean {
    val virtualFile = getVirtualFile(editor) ?: return false
    if (virtualFile !is LightVirtualFile) return true
    val hostVirtualFile = getHostFileFromInjectedFile(virtualFile) ?: return false
    return hostVirtualFile !is LightVirtualFile
  }

  private fun getHostFileFromInjectedFile(virtualFile: VirtualFile): VirtualFile? =
    (virtualFile.originalFileOrSelf() as? VirtualFileWindow)?.delegate

  /**
   * Checks if the editor is a diff window
   */
  @JvmStatic
  fun isDiffEditor(editor: Editor): Boolean = editor.editorKind == EditorKind.DIFF

  /**
   * Checks if the editor hosts the console of a run or debug configuration, e.g. the console of a debugged Spring Boot
   * application.
   *
   * Language consoles also extend `ConsoleViewImpl`, but have their own handling - see [isPythonConsole].
   */
  @JvmStatic
  fun isRunConsole(editor: Editor): Boolean {
    val consoleView = editor.getUserData(ConsoleViewImpl.CONSOLE_VIEW_IN_EDITOR_VIEW)
    return consoleView != null && consoleView !is LanguageConsoleView
  }

  /**
   * Checks if the editor hosts a Vim command-line / search history window
   * (the synthetic buffer opened by `q:`, `q/`, or `q?`).
   *
   * Note that this resolves the file from the document rather than using `Editor.getVirtualFile()`, which is not
   * necessarily populated yet while the editor is being created - and that is exactly when this is called, to decide
   * whether the editor is Vim enabled and should have its local options initialised.
   */
  @JvmStatic
  fun isCommandHistoryWindow(editor: Editor): Boolean = getVirtualFile(editor)?.getUserData(CmdwinKeys.KIND) != null

  /**
   * Checks if the editor is the Python console, so we can enable Vim features while still applying the special
   * Enter/arrow key handling it requires.
   *
   * The console's virtual file is a light file named after the console title. That title is only "Python Console" for
   * consoles started from the tool window - "Run file in Python Console" names the console after the run
   * configuration, and the title is localised. So we primarily look for the marker the Python plugin puts on the
   * console's virtual file (`PythonConsoleView.CONSOLE_KEY`), and fall back to the name.
   *
   * The name check is still needed: the console's input editor is created inside `LanguageConsoleImpl`'s
   * constructor, i.e. before `PythonConsoleView` gets a chance to set the marker. See
   * `VimEditorFactoryListener.scheduleDeferredInitialisation` for how consoles that aren't recognisable at
   * creation time are picked up.
   */
  @JvmStatic
  fun isPythonConsole(editor: Editor): Boolean {
    val file = getVirtualFile(editor) ?: return false
    if (isMarkedAsPythonConsole(file)) return true
    // In split mode, the projected VirtualFile may have a different getName() result,
    // so we also check getPath() to reliably detect the Python console.
    return file.name.contains(PYTHON_CONSOLE_FILE_NAME) || file.path.contains(PYTHON_CONSOLE_FILE_NAME)
  }

  private fun isMarkedAsPythonConsole(file: VirtualFile): Boolean = hasUserDataFlag(file, PYDEV_CONSOLE_KEY_NAME)

  /**
   * Checks if the editor is the prompt input of an AI chat: JetBrains AI Assistant (which also hosts Junie) or AIR.
   *
   * Neither plugin exposes a public marker, so we look for their internal ones. AIR marks the prompt document with
   * `AIR_PROMPT_INPUT_DOCUMENT` and names its light file `AirPrompt-<uuid>`. AI Assistant puts
   * `AI_ASSISTANT_INPUT_EDITOR` on the editor and names the input's light file `AIAssistantInput-<uuid>`.
   * The AI Assistant key is only set after the editor is created, and not by every input variant (e.g. the in-code
   * generation popup), so the file name is what recognises it when the editor is created.
   *
   * Viewers are excluded, so code snippets rendered in chat answers keep their native behaviour.
   */
  @JvmStatic
  fun isChatInput(editor: Editor): Boolean {
    if (editor.isViewer) return false
    if (hasUserDataFlag(editor.document, AIR_PROMPT_DOCUMENT_KEY_NAME)) return true
    if (hasUserDataFlag(editor, AI_ASSISTANT_INPUT_EDITOR_KEY_NAME)) return true
    val name = getVirtualFile(editor)?.name ?: return false
    return name.startsWith(AI_ASSISTANT_INPUT_FILE_NAME_PREFIX) || name.startsWith(AIR_PROMPT_FILE_NAME_PREFIX)
  }

  private fun hasUserDataFlag(holder: UserDataHolder, keyName: String): Boolean {
    @Suppress("DEPRECATION") val key = Key.findKeyByName(keyName) ?: return false
    return holder.getUserData(key) == true
  }

  /**
   * Checks if the editor is hosted in the Commit tool window, so we can enable Vim features
   */
  @JvmStatic
  fun isCommitWindowEditor(editor: Editor): Boolean {
    @Suppress("DEPRECATION") val dataKey = Key.findKeyByName("Vcs.CommitMessage.Panel")
    if (dataKey != null && editor.document.getUserData(dataKey) != null) return true
    return getVirtualFile(editor)?.name?.contains("Dummy.txt") == true
  }

  /**
   * Checks if the editor is a Kotlin class file decompiled to a Java file, so we can enable Vim features
   *
   * The platform changed the implementation of decompiling a Kotlin .class file to Java in 2026.2. Previously, it
   * used a dummy virtual file implementation. Now it uses an instance of [LightVirtualFile]. Typically, this
   * means an in-memory file that we don't want to have Vim features for, but in this case, we do.
   *
   * To test, open a .class file generated from a Kotlin file. Then use the "Decompile to Java" action to create a
   * separate (in-memory) `.decompiled.java` file. Java-based .class files are decompiled directly in the document for
   * the .class file, so the editor is always backed by a valid file.
   *
   * Perhaps a future implementation would have an allow-list for [VirtualFile.getFileType] and allow "JAVA"?
   */
  @JvmStatic
  fun isKotlinClassDecompiledToJavaFile(editor: Editor): Boolean {
    val file = getVirtualFile(editor) ?: return false
    return hasUserDataFlag(file, "IS_KOTLIN_DECOMPILED_FILE")
  }

  /**
   * Checks if the document in the editor is modified.
   */
  @JvmStatic
  fun hasUnsavedChanges(editor: Editor): Boolean {
    val document = editor.document
    return (0 until document.lineCount).any { document.isLineModified(it) }
  }
}
