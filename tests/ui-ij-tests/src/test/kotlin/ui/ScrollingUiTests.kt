/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui

import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import ui.pages.Editor
import ui.pages.IdeaFrame
import ui.utils.invokeActionJs
import java.time.Duration
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Vertical scrolling in a real IDE, where the sticky lines panel, rendered doc comments, animated scrolling, the mouse
 * wheel, the ex entry panel and splits behave as they do for users. The unit tests cover the details, these check
 * that the caret keeps its 'scrolloff' context and stays on the screen.
 *
 * Every check reads the editor that currently has focus, so they also work after splitting the window.
 */
internal const val SCROLL_TEST_FILE = "MyScroll.java"

// A class with 40 methods, each with a doc comment and a nested block, so the sticky lines panel changes height while
// scrolling and the doc comments can be rendered
internal val scrollTestText: String = buildString {
  appendLine("public class MyScroll {")
  for (i in 0 until 40) {
    appendLine("  /**")
    appendLine("   * Returns the value of method $i.")
    appendLine("   * The comment has a few lines, so it takes a few rows when it is rendered.")
    appendLine("   */")
    appendLine("  static int method$i(int value) {")
    appendLine("    if (value > 0) {")
    repeat(6) { appendLine("      value = value + $it;") }
    appendLine("    }")
    appendLine("    return value + $i;")
    appendLine("  }")
    appendLine()
  }
  append("}")
}

internal fun IdeaFrame.testScrolling(editor: Editor) {
  testScrolloffKeepsContextWhileMoving(editor)
  testScrolloffKeepsCaretCentred(editor)
  testScrolloffWithRenderedDocComments(editor)
  testScreenLineMotions(editor)
  testMouseWheelMovesCaretWithIdeaScrollCursor(editor)
  testIncsearchKeepsMatchAboveCommandLine(editor)
  testScrolloffInSplit(editor)
}

private fun IdeaFrame.testScrolloffKeepsContextWhileMoving(editor: Editor) = step("scrolloff while moving") {
  vimCommand(":set scrolloff=5")
  type(editor, "gg")

  moveAndCheck(editor, "j", 80) { assertBottomContext(it, 5) }
  moveAndCheck(editor, "k", 80) { assertTopContext(it, 5) }

  vimCommand(":set scrolloff&")
}

private fun IdeaFrame.testScrolloffKeepsCaretCentred(editor: Editor) = step("scrolloff=999") {
  vimCommand(":set scrolloff=999")
  type(editor, "100G")

  moveAndCheck(editor, "j", 20) { state ->
    // The sticky lines panel changes height while scrolling, so there isn't always an exact middle
    assertTrue(abs(state.rowsAbove - state.rowsBelow) <= 2, "Caret is not centred: $state")
  }

  // Typing must not scroll, even though the middle can't always be reached exactly
  val before = editor.scrollState()
  type(editor, "i")
  for (c in "abc") {
    val text = editor.text
    type(editor, c.toString())
    waitFor(Duration.ofSeconds(5)) { editor.text != text }
    editor.waitForScrollToFinish()
    assertEquals(before.scrollY, editor.scrollState().scrollY, "Editor scrolled after typing '$c'")
  }
  keyboard { escape() }
  type(editor, "u")
  waitFor(Duration.ofSeconds(5)) { editor.text == scrollTestText }

  vimCommand(":set scrolloff&")
}

// Rendered doc comments are custom fold regions: a single visual line that takes several rows
private fun IdeaFrame.testScrolloffWithRenderedDocComments(editor: Editor) = step("scrolloff with rendered docs") {
  remoteRobot.invokeActionJs("ToggleRenderedDocPresentationForAll")
  waitFor(Duration.ofSeconds(10)) { editor.hasRenderedDocComments() }

  vimCommand(":set scrolloff=5")
  type(editor, "gg")
  // The context is counted in whole rows, so a doc comment in it can leave up to a row less on the screen
  moveAndCheck(editor, "j", 60) { assertBottomContext(it, 5, missingRows = 1) }
  moveAndCheck(editor, "k", 60) { assertTopContext(it, 5, missingRows = 1) }
  vimCommand(":set scrolloff&")

  remoteRobot.invokeActionJs("ToggleRenderedDocPresentationForAll")
  waitFor(Duration.ofSeconds(10)) { !editor.hasRenderedDocComments() }
}

private fun IdeaFrame.testMouseWheelMovesCaretWithIdeaScrollCursor(editor: Editor) = step("ideascrollcursor") {
  vimCommand(":set ideascrollcursor scrolloff=3")
  type(editor, "gg")

  editor.runJs("robot.rotateMouseWheel(component, 10)")
  editor.waitForScrollToFinish()
  editor.scrollState().let { state ->
    assertTrue(state.scrollY > 0, "Mouse wheel didn't scroll: $state")
    assertTopContext(state, 3)
    assertBottomContext(state, 3)
  }

  editor.runJs("robot.rotateMouseWheel(component, -5)")
  editor.waitForScrollToFinish()
  editor.scrollState().let { state ->
    assertTopContext(state, 3)
    assertBottomContext(state, 3)
  }

  vimCommand(":set noideascrollcursor scrolloff&")
}

// The command line is drawn over the bottom of the editor, so scrolling to a match must keep it above
private fun IdeaFrame.testIncsearchKeepsMatchAboveCommandLine(editor: Editor) = step("incsearch") {
  vimCommand(":set incsearch scrolloff=0")
  type(editor, "gg")
  // The search remembers the scroll position it starts at, which is still changing while the scroll is animated
  editor.waitForScrollToFinish()
  val before = editor.scrollState()

  type(editor, "/method30(")
  waitFor(Duration.ofSeconds(5)) { editor.scrollState().scrollY != before.scrollY }
  editor.waitForScrollToFinish()

  val commandLine = find<ComponentFixture>(byXpath("//div[@class='ExEntryPanel']"), Duration.ofSeconds(5))
  val matchLine = scrollTestText.lines().indexOfFirst { it.contains("method30(") }
  val match = editor.lineOnScreen(matchLine)
  val commandLineTop = commandLine.callJs<Int>("component.getLocationOnScreen().y", runInEdt = true)
  assertTrue(match.top >= match.textAreaTop, "Match is above the text area: $match")
  assertTrue(match.bottom <= commandLineTop, "Match is under the command line at $commandLineTop: $match")

  // Cancelling the search should restore the original scroll position. It only restores the caret, which ends up off
  // the screen, since IncsearchSession.close stopped resetting the scroll (VIM-2529)
  keyboard { escape() }
  editor.waitForScrollToFinish()
  // assertEquals(before.scrollY, editor.scrollState().scrollY, "Scroll position wasn't restored")

  vimCommand(":set noincsearch scrolloff&")
}

private fun IdeaFrame.testScrolloffInSplit(editor: Editor) = step("scrolloff in a split") {
  vimCommand(":vsplit")
  waitFor(Duration.ofSeconds(10)) {
    findAll<Editor>(byXpath("//div[@accessiblename='Editor for $SCROLL_TEST_FILE' and @class='EditorComponentImpl']"))
      .size == 2
  }

  // The local value only applies to the new window
  vimCommand(":setlocal scrolloff=10")
  type(editor, "gg")
  moveAndCheck(editor, "j", 50) { assertBottomContext(it, 10) }

  vimCommand(":q")
  waitFor(Duration.ofSeconds(10)) {
    findAll<Editor>(byXpath("//div[@accessiblename='Editor for $SCROLL_TEST_FILE' and @class='EditorComponentImpl']"))
      .size == 1
  }
  type(editor, "gg")
}

// H, M and L move the caret to the top, middle and bottom of the text area, below the sticky lines panel, without
// scrolling. ScreenLineMotionsStickyLinesTest checks every combination with a panel and doc comments of its own, this
// checks a few positions with the real ones of Java
private fun IdeaFrame.testScreenLineMotions(editor: Editor) {
  for (scrollOffset in SCREEN_LINE_SCROLL_OFFSETS) testScreenLineMotions(editor, scrollOffset, renderedDocs = false)

  remoteRobot.invokeActionJs("ToggleRenderedDocPresentationForAll")
  waitFor(Duration.ofSeconds(10)) { editor.hasRenderedDocComments() }
  for (scrollOffset in SCREEN_LINE_SCROLL_OFFSETS) testScreenLineMotions(editor, scrollOffset, renderedDocs = true)
  remoteRobot.invokeActionJs("ToggleRenderedDocPresentationForAll")
  waitFor(Duration.ofSeconds(10)) { !editor.hasRenderedDocComments() }
}

private val SCREEN_LINE_SCROLL_OFFSETS = listOf(0, 5)

// The model counts every visual line in whole rows, and a rendered doc comment takes a fraction of a row more than
// it counts. With them on the screen only the bounds are checked: the caret lands within a row of the context, and
// somewhere in the middle
private fun IdeaFrame.testScreenLineMotions(editor: Editor, scrollOffset: Int, renderedDocs: Boolean) =
  step("H M L with scrolloff=$scrollOffset${if (renderedDocs) " and rendered docs" else ""}") {
    vimCommand(":set scrolloff=$scrollOffset")
    type(editor, "gg")
    editor.waitForScrollToFinish()
    val exact = !renderedDocs
    checkWithoutScrolling(editor, "H") { assertAtTop(it, scrollOffset, exact) }
    checkWithoutScrolling(editor, "L") { assertAtBottom(it, scrollOffset, exact) }

    // Stays clear of the end of the file, where 'scrolloff' doesn't apply at the bottom
    for (line in 40..440 step 50) {
      type(editor, "${line}zt")
      editor.waitForScrollToFinish()
      checkWithoutScrolling(editor, "H") { assertAtTop(it, scrollOffset, exact) }
      checkWithoutScrolling(editor, "M") { assertInMiddle(it, exact) }
      checkWithoutScrolling(editor, "L") { assertAtBottom(it, scrollOffset, exact) }
    }

    vimCommand(":set scrolloff&")
  }

// The first line with 'scrolloff' rows above it, or the first line of the file if it is shown
private fun assertAtTop(state: ScrollState, scrollOffset: Int, exact: Boolean) {
  val wanted = if (state.isTopOfFileVisible) 0 else scrollOffset
  assertTopContext(state, wanted, missingRows = if (exact) 0 else 1)
  if (exact) {
    assertTrue(state.rowsAboveLineAbove < wanted, "The line above the caret is far enough from the top: $state")
  }
}

// The last line with 'scrolloff' rows below it. A transparent horizontal scrollbar can cover the last row
private fun assertAtBottom(state: ScrollState, scrollOffset: Int, exact: Boolean) {
  assertBottomContext(state, scrollOffset, missingRows = if (exact) 0 else 1)
  if (exact) {
    assertTrue(
      state.rowsBelowLineBelow < scrollOffset + 1,
      "The line below the caret is far enough from the bottom: $state",
    )
  }
}

// The caret line covers the middle row of the text, give or take a row for the filler around it
private fun assertInMiddle(state: ScrollState, exact: Boolean) {
  val middle = state.textRows / 2
  val slack = if (exact) 1 else 3
  assertTrue(
    state.rowsAbove <= middle + slack && state.rowsAbove + state.caretRows - 1 >= middle - slack,
    "Caret is not on the middle row $middle: $state",
  )
}

private fun IdeaFrame.checkWithoutScrolling(editor: Editor, motion: String, check: (ScrollState) -> Unit) {
  val before = editor.scrollState()
  type(editor, motion)
  editor.waitForScrollToFinish()
  val after = editor.scrollState()
  step("'$motion' at ${before.scrollY}: $after") {
    assertEquals(before.scrollY, after.scrollY, "'$motion' scrolled: $before -> $after")
    check(after)
  }
}

/**
 * Where the caret line is on the screen, in rows of the text area below the sticky lines panel
 */
private data class ScrollState(
  val caretLine: Int,
  val rowsAbove: Int,
  val rowsBelow: Int,
  val scrollY: Int,
  val isTopOfFileVisible: Boolean,
  val isBottomOfFileVisible: Boolean,
  val stickyRows: Int,
  // Rows above the visual line above the caret, and below the one below it, negative if there's no such line
  val rowsAboveLineAbove: Int,
  val rowsBelowLineBelow: Int,
  val caretRows: Int,
  // Rows of the text area that show text, which is fewer than the text area at the end of the file
  val textRows: Int,
)

private data class LineOnScreen(val top: Int, val bottom: Int, val textAreaTop: Int)

// Rows are rounded down, so `missingRows = 1` allows for any fraction of a row less
private fun assertTopContext(state: ScrollState, scrollOffset: Int, missingRows: Int = 0) {
  assertTrue(state.rowsBelow >= 0, "Caret is below the screen: $state")
  assertTrue(
    state.rowsAbove >= scrollOffset - missingRows || state.isTopOfFileVisible && state.rowsAbove >= 0,
    "Fewer than $scrollOffset rows above the caret: $state",
  )
}

private fun assertBottomContext(state: ScrollState, scrollOffset: Int, missingRows: Int = 0) {
  assertTrue(state.rowsAbove >= 0, "Caret is above the screen or under the sticky lines: $state")
  assertTrue(
    state.rowsBelow >= scrollOffset - missingRows || state.isBottomOfFileVisible && state.rowsBelow >= 0,
    "Fewer than $scrollOffset rows below the caret: $state",
  )
}

private fun IdeaFrame.moveAndCheck(editor: Editor, motion: String, count: Int, check: (ScrollState) -> Unit) {
  repeat(count) { move ->
    val before = editor.scrollState()
    type(editor, motion)
    waitFor(Duration.ofSeconds(5)) { editor.scrollState().caretLine != before.caretLine }
    editor.waitForScrollToFinish()
    val after = editor.scrollState()
    step("'$motion' #$move: $after") { check(after) }
  }
}

private fun IdeaFrame.vimCommand(command: String) {
  keyboard {
    enterText(command)
    enter()
  }
}

private fun IdeaFrame.type(editor: Editor, keys: String) {
  editor.keyboard { enterText(keys) }
}

// The JS below reads the focused editor, which is the right one after splitting the window
private const val FOCUSED_EDITOR =
  "const e = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(component.getEditor().getProject())" +
    ".getSelectedTextEditor();"

private fun Editor.scrollState(): ScrollState {
  val values = callJs<String>(
    """
    $FOCUSED_EDITOR
    const area = e.getScrollingModel().getVisibleArea();
    const lineHeight = e.getLineHeight();
    const sticky = e.getStickyLinesPanelHeight();
    const caretLine = e.getCaretModel().getVisualPosition().line;
    const caret = e.visualLineToYRange(caretLine);
    const lastLine = e.offsetToVisualPosition(e.getDocument().getTextLength()).line;
    const top = area.y + sticky;
    const scrollbar = !com.intellij.openapi.util.SystemInfo.isMac &&
      com.intellij.openapi.util.registry.Registry.is("editor.transparent.scrollbar") ?
      e.getScrollPane().getHorizontalScrollBar().getHeight() : 0;
    const bottom = area.y + area.height - scrollbar;
    const lineAbove = caretLine > 0 ? e.visualLineToYRange(caretLine - 1) : null;
    const lineBelow = caretLine < lastLine ? e.visualLineToYRange(caretLine + 1) : null;
    [
      e.getCaretModel().getLogicalPosition().line,
      Math.floor((caret[0] - top) / lineHeight),
      Math.floor((bottom - caret[1]) / lineHeight),
      area.y,
      area.y == 0,
      e.visualLineToYRange(lastLine)[1] <= bottom,
      Math.round(sticky / lineHeight),
      lineAbove ? Math.floor((lineAbove[0] - top) / lineHeight) : -1,
      lineBelow ? Math.floor((bottom - lineBelow[1]) / lineHeight) : -1,
      Math.round((caret[1] - caret[0]) / lineHeight),
      Math.floor((Math.min(bottom, e.visualLineToYRange(lastLine)[1]) - top) / lineHeight)
    ].join(',')
    """,
    runInEdt = true,
  )
  val v = values.split(',')
  return ScrollState(
    caretLine = v[0].toDouble().toInt(),
    rowsAbove = v[1].toDouble().toInt(),
    rowsBelow = v[2].toDouble().toInt(),
    scrollY = v[3].toDouble().toInt(),
    isTopOfFileVisible = v[4].toBoolean(),
    isBottomOfFileVisible = v[5].toBoolean(),
    stickyRows = v[6].toDouble().toInt(),
    rowsAboveLineAbove = v[7].toDouble().toInt(),
    rowsBelowLineBelow = v[8].toDouble().toInt(),
    caretRows = v[9].toDouble().toInt(),
    textRows = v[10].toDouble().toInt(),
  )
}

// Screen coordinates of a logical line and of the top of the text area, below the sticky lines panel
private fun Editor.lineOnScreen(logicalLine: Int): LineOnScreen {
  val values = callJs<String>(
    """
    $FOCUSED_EDITOR
    const area = e.getScrollingModel().getVisibleArea();
    const viewportTop = e.getScrollPane().getViewport().getLocationOnScreen().y;
    const visualLine = e.logicalToVisualPosition(new com.intellij.openapi.editor.LogicalPosition($logicalLine, 0)).line;
    const range = e.visualLineToYRange(visualLine);
    [
      viewportTop + range[0] - area.y,
      viewportTop + range[1] - area.y,
      viewportTop + e.getStickyLinesPanelHeight()
    ].join(',')
    """,
    runInEdt = true,
  )
  val v = values.split(',').map { it.toDouble().toInt() }
  return LineOnScreen(top = v[0], bottom = v[1], textAreaTop = v[2])
}

private fun Editor.waitForScrollToFinish() {
  waitFor(Duration.ofSeconds(5)) {
    callJs(
      """
      $FOCUSED_EDITOR
      const model = e.getScrollingModel();
      model.getVisibleArea().equals(model.getVisibleAreaOnScrollingFinished())
      """,
      runInEdt = true,
    )
  }
  // 'ideascrollcursor' and the sticky lines panel react to the scroll that has just finished
  Thread.sleep(300)
}

private fun Editor.hasRenderedDocComments(): Boolean = callJs(
  """
  $FOCUSED_EDITOR
  const regions = e.getFoldingModel().getAllFoldRegions();
  let found = false;
  for (let i = 0; i < regions.length; i++) {
    if (regions[i] instanceof com.intellij.openapi.editor.CustomFoldRegion) found = true;
  }
  found
  """,
  runInEdt = true,
)
