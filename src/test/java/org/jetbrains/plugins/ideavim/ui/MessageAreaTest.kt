/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("SpellCheckingInspection")

package org.jetbrains.plugins.ideavim.ui

import com.intellij.util.application
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.ui.OutputPanel
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertNotNull
import kotlin.math.ceil
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the behaviour of the message area, showing command output and other messages, either with a pager or inline
 *
 * Vim casually refers to the "message area" without making it an official UI component. The bottom of the screen is
 * split into an optional status line (see `:help status-line`) above a command line. The command line can optionally
 * contain the `'ruler'`, `'showcmd'`, `'showmode'` as well as search count and other file information, and of course,
 * it's used for entering commands. It also shares screen space with the message area.
 *
 * Vim uses the message area to display error messages and notifications such as the current search term. It is also
 * used to display command output such as from `:echo` or `:map`. And it also shows the output of external commands
 * invoked with `:!{filter}`. Vim uses the `v:echospace` variable to see how many characters it can write to the
 * message area without overwriting the ruler, showcmd, and so on. If the output is larger than this space, it's
 * multi-line output, or it's the output of an external program, the message area extends over the screen and is
 * controlled by the pager (see `:help pager`). This displays a `more-prompt` for each page and finally ends with a
 * `hit-enter` prompt before clearing the message area and redrawing. The user can increase the size of the message area
 * with the `'cmdheight'` option.
 *
 * The message area is rarely cleared. It is cleared when the user explicitly redraws the screen and (in some
 * configurations) when the screen is scrolled. However, because it's at the bottom of the screen, it's fairly
 * unintrusive, even when the messages are stale.
 *
 * IdeaVim has a UI component to represent the command line, and this is overlaid on the editor and hidden when not
 * needed. It has a similar component for the message area. For multi-line output, the output panel is treated
 * as a pager, showing the `more-prompt` and finally the `hit-enter` prompt. When showing single line output, the output
 * panel is shown without the pager and without a way for the user to dismiss it. This is a more intrusive means of
 * displaying messages, so IdeaVim makes it easier to hide the message area than Vim does.
 */
@TestWithoutNeovim(reason = SkipNeovimReason.IDEAVIM_API_USED)
class MessageAreaTest : VimTestCase("\n") {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
    enterCommand("set nowrap")
  }

  @Disabled("How to test running an external command?")
  @Test
  fun `test external command always uses pager`() {
    // Perhaps run a command that will definitely fail, so we show the error output in a pager?
  }

  @Disabled("Fails. We currently only count actual lines, not wrapped text")
  @Test
  fun `test long wrapped single line command output uses pager`() {
    enterCommand("echo '123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890'")
    assertPager()
    assertHitEnterPrompt()
  }

  @Test
  fun `test command output longer than single line uses pager`() {
    enterCommandForMultiLineOutput()
    assertPager()
  }

  @Test
  fun `test single-page multi-line pager content immediately shows hit-enter prompt`() {
    enterCommandForMultiLineOutput()
    assertHitEnterPrompt()
  }

  @Test
  fun `test enter at single-page hit-enter prompt closes message area without reusing key`() {
    configureByText("""
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommandForMultiLineOutput()
    assertHitEnterPrompt()

    doTypeText("<Enter>")
    assertExOutputClosed()

    // if Enter was passed through to the editor, the caret would move to the next line
    assertState("""
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
  }

  @Test
  fun `test space at single-page hit-enter prompt closes message area without reusing key`() {
    configureByText("""
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommandForMultiLineOutput()
    assertHitEnterPrompt()

    doTypeText("<Space>")
    assertExOutputClosed()

    // if Space was passed through to the editor, the caret would move to the next character
    assertState("""
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
  }

  @Test
  fun `test escape at single-page hit-enter prompt closes message area without reusing escape`() {
    enterCommandForMultiLineOutput()
    assertHitEnterPrompt()
    doTypeText("<Esc>")
    assertExOutputClosed()
    // TODO: How to verify that escape does nothing here? Mapping?
  }

  @Test
  fun `test 'q' at single-page hit-enter prompt closes message area without reusing 'q'`() {
    enterCommandForMultiLineOutput()
    assertHitEnterPrompt()

    doTypeText("q")
    assertExOutputClosed()

    // If we pass `q` to the editor, then this would be `qa` and we'd be recording a macro. If we don't pass it through,
    // the `a` puts us in Insert mode
    doTypeText("a")
    assertState(Mode.INSERT)
  }

  @Test
  fun `test down at single-page hit-enter prompt closes message area and moves caret down a line`() {
    configureByText("""
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommandForMultiLineOutput()
    assertHitEnterPrompt()

    doTypeText("<Down>")
    assertExOutputClosed()

    assertState("""
      |Lorem ipsum dolor sit amet,
      |${c}consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
  }

  @Test
  fun `test 'd' at single-page hit-enter prompt closes message area and reuses key`() {
    configureByText("""
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommandForMultiLineOutput()
    assertHitEnterPrompt()

    doTypeText("d")
    assertExOutputClosed()

    // Reusing the key will be like we've typed `dw` in the editor
    doTypeText("w")
    assertState("""
      |${c}ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
  }

  @Test
  fun `test page down at single-page hit-enter prompt closes message area and moves caret down a page`() {
    configureByPages(3)
    enterCommandForMultiLineOutput()
    assertHitEnterPrompt()

    doTypeText("<PageDown>")
    assertExOutputClosed()

    // -1 to convert to 0-based value, and -1 to put the last line of previous page at top of this page
    assertTopLogicalLine(screenHeight - 2)
  }

  @Test
  fun `test page up at single-page hit-enter prompt does nothing`() {
    enterCommandForMultiLineOutput()
    assertHitEnterPrompt()

    doTypeText("<PageUp>")
    assertPagerTopLine(0)
  }

  @Test
  fun `test any other key at single-page hit-enter prompt closes message area and reuses key`() {
    configureByText("""
      |${c}Lorem ipsum dolor sit amet,
    """.trimMargin())
    enterCommandForMultiLineOutput()
    assertHitEnterPrompt()

    doTypeText("x")
    assertExOutputClosed()

    // `x` deletes the `L`
    assertState("""
      |${c}orem ipsum dolor sit amet,
    """.trimMargin())
  }

  @Test
  fun `test multi-page content with 'nomore' option skips more-prompt for hit-enter prompt`() {
    enterCommand("set nomore")
    enterCommandForMultiPageOutput()
    assertHitEnterPrompt()
  }

  @Test
  fun `test multi-page output content shows more-prompt`() {
    enterCommandForMultiPageOutput()
    assertMorePrompt()
  }

  @Test
  fun `test enter at more-prompt scrolls down a single line`() {
    enterCommandForMultiPageOutput()
    assertPagerTopLine(0)
    doTypeText("<Enter>")
    assertPagerTopLine(1)
  }

  @Test
  fun `test space at more-prompt scrolls down a page`() {
    enterCommandForMultiPageOutput()
    assertPagerTopLine(0)
    doTypeText("<Space>")
    // Initial page displays pageSize lines (e.g., 41), scrolls down to show the next line at the top of the next page
    // (there is no overlap). So that puts line 42 at the top of the next page aka 41 in 0-based indexing
    assertPagerTopLine(pageSize)
  }

  @Test
  fun `test escape at more-prompt closes message area without reusing escape`() {
    enterCommandForMultiPageOutput()
    assertMorePrompt()
    doTypeText("<Esc>")
    assertExOutputClosed()
    // TODO: How to verify that escape does nothing here? Mapping?
  }

  @Test
  fun `test 'q' at more-prompt closes message area without reusing 'q'`() {
    enterCommandForMultiPageOutput()
    assertMorePrompt()

    doTypeText("q")
    assertExOutputClosed()

    // If we pass `q` to the editor, then this would be `qa` and we'd be recording a macro. If we don't pass it through,
    // the `a` puts us in Insert mode
    doTypeText("a")
    assertState(Mode.INSERT)
  }

  @Test
  fun `test down at more-prompt scrolls down a single line`() {
    enterCommandForMultiPageOutput()
    assertPagerTopLine(0)
    doTypeText("<Down>")
    assertPagerTopLine(1)
  }

  @Test
  fun `test up at more-prompt scrolls up a single line`() {
    enterCommandForMultiPageOutput()
    doTypeText("<Down>")
    assertPagerTopLine(1)
    doTypeText("<Up>")
    assertPagerTopLine(0)
  }

  @Test
  fun `test 'd' at more-prompt scrolls down half a page`() {
    enterCommandForMultiPageOutput()
    assertPagerTopLine(0)
    doTypeText("d")
    // When scrolling half a page, we put the next line after the half-way point at the top. For odd numbers, this is
    // easy - it's mid-point+1. For even numbers, we need to round up to the next integer first.
    // This naturally gives us 0-based line numbers
    // E.g., page size of 41 gives the next 0-based line after the half-way point as 21. 40 gives us 20
    assertPagerTopLine(ceil(pageSize / 2.0).toInt())
  }

  @Test
  fun `test page down at more-prompt scrolls down a page`() {
    enterCommandForMultiPageOutput()
    assertMorePrompt()
    doTypeText("<PageDown>")
    assertPagerTopLine(pageSize)
  }

  @Test
  fun `test page up at more-prompt scrolls up a page`() {
    enterCommandForMultiPageOutput()
    assertMorePrompt()
    doTypeText("<PageDown>")
    assertPagerTopLine(pageSize)
    doTypeText("<PageUp>")
    assertPagerTopLine(0)
  }

  @Test
  fun `test any other key at more-prompt shows verbose more-prompt`() {
    enterCommandForMultiPageOutput()
    doTypeText("x")
    assertVerboseMorePrompt()
  }

  @Test
  fun `test scrolled multipage output ends with hit-enter prompt`() {
    enterCommandForMultiPageOutput()
    scrollOutputToEnd()
    assertHitEnterPrompt()
  }

  @Test
  fun `test enter at multipage hit-enter prompt closes message area without reusing key`() {
    configureByText("""
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommandForMultiPageOutput()
    scrollOutputToEnd()
    assertHitEnterPrompt()

    doTypeText("<Enter>")
    assertExOutputClosed()

    // if Enter was passed through to the editor, the caret would move to the next line
    assertState("""
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
  }

  @Test
  fun `test space at multipage hit-enter prompt closes message area without reusing key`() {
    configureByText("""
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommandForMultiPageOutput()
    scrollOutputToEnd()
    assertHitEnterPrompt()

    doTypeText("<Space>")
    assertExOutputClosed()

    // if Space was passed through to the editor, the caret would move to the next character
    assertState("""
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
  }

  @Test
  fun `test escape at multipage hit-enter prompt closes message area without reusing key`() {
    enterCommandForMultiPageOutput()
    scrollOutputToEnd()
    assertHitEnterPrompt()
    doTypeText("<Esc>")
    assertExOutputClosed()
    // TODO: How to verify that escape does nothing here? Mapping?
  }

  @Test
  fun `test 'q' at multipage hit-enter prompt closes message area without reusing key`() {
    enterCommandForMultiPageOutput()
    scrollOutputToEnd()
    assertHitEnterPrompt()

    doTypeText("q")
    assertExOutputClosed()

    // If we pass `q` to the editor, then this would be `qa` and we'd be recording a macro. If we don't pass it through,
    // the `a` puts us in Insert mode
    doTypeText("a")
    assertState(Mode.INSERT)
  }

  @VimBehaviorDiffers("Vim does not hide the messages if the content can be scrolled")
  @Test
  fun `test down at multipage hit-enter prompt closes message area and reuses key`() {
    configureByText("""
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommandForMultiPageOutput()
    scrollOutputToEnd()
    assertHitEnterPrompt()

    doTypeText("<Down>")
    assertExOutputClosed()

    assertState("""
      |Lorem ipsum dolor sit amet,
      |${c}consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
  }

  @Test
  fun `test up at multipage hit-enter prompt scrolls up a single line`() {
    enterCommandForMultiPageOutput()
    scrollOutputToEnd()
    assertPagerTopLine(100 - pageSize + 1)
    doTypeText("<Up>")
    assertPagerTopLine(100 - pageSize)
  }

  @VimBehaviorDiffers("Vim does not hide the messages if the content can be scrolled")
  @Test
  fun `test page down at multipage hit-enter prompt closes message area and reuses key`() {
    configureByText("""
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommandForMultiPageOutput()
    scrollOutputToEnd()
    assertHitEnterPrompt()

    doTypeText("<PageDown>")
    assertExOutputClosed()

    assertState("""
      |Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |${c}Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
  }

  @Test
  fun `test page up at multipage hit-enter prompt scrolls up a single line`() {
    enterCommandForMultiPageOutput()
    scrollOutputToEnd()
    assertPagerTopLine(100 - pageSize + 1)
    doTypeText("<PageUp>")
    assertPagerTopLine(100 - pageSize + 1 - pageSize)
  }

  @Test
  fun `test any other key at multipage hit-enter prompt closes message area and reuses key`() {
    configureByText("""
      |${c}Lorem ipsum dolor sit amet,
    """.trimMargin())
    enterCommandForMultiPageOutput()
    scrollOutputToEnd()
    assertHitEnterPrompt()

    doTypeText("x")
    assertExOutputClosed()

    // `x` deletes the `L`
    assertState("""
      |${c}orem ipsum dolor sit amet,
    """.trimMargin())
  }

  // Single-line output panel

  @Test
  fun `test command output of single line uses single line message area`() {
    enterCommandForSingleLineOutput()
    assertStaticMessageArea()
  }

  @Test
  fun `test search notification is shown in single line message area`() {
    configureByText("Lorem ipsum")
    enterSearch("ipsum")
    assertStaticMessageArea("/ipsum")
  }

  @Test
  fun `test next search notification is shown in single line message area`() {
    configureByText("Lorem ipsum ipsum ipsum")
    enterSearch("ipsum")
    doTypeText("n")
    assertStaticMessageArea("/ipsum")
  }

  @Test
  fun `test previous search notification is shown in single line message area`() {
    configureByText("Lorem ipsum ipsum ipsum")
    enterSearch("ipsum")
    doTypeText("nn", "N")
    assertStaticMessageArea("?ipsum")
  }

  @Test
  fun `test error notification is shown in single line message area`() {
    enterCommand("foo")
    assertStaticMessageArea("E492: Not an editor command: foo")
  }

  @Test
  fun `test single line message area is not dismissed by cursor movement`() {
    configureByText("""
      |${c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommandForSingleLineOutput()
    assertStaticMessageArea()
    doTypeText("jjj")   // Handled by editor, doesn't remove message area panel
    assertExOutputOpen()
  }

  // Ideally, we'd have tests to verify that the single-line message area doesn't close when you hit Escape or `q`.
  // However, this only works in production because the panel is created without focus, and focus doesn't happen in
  // tests. Therefore, we can't test typing because we manually and explicitly choose where the keystrokes go.

  @Test
  fun `test hide single line message area when mode changes to Insert`() {
    enterCommandForSingleLineOutput()
    assertStaticMessageArea()
    doTypeText("i")
    assertExOutputClosed()
  }

  @Test
  fun `test hide single line message area when mode changes to Replace`() {
    enterCommandForSingleLineOutput()
    assertStaticMessageArea()
    doTypeText("R")
    assertExOutputClosed()
  }

  @Test
  fun `test hide single line message area when mode changes to Visual`() {
    enterCommandForSingleLineOutput()
    assertStaticMessageArea()
    doTypeText("v")
    assertExOutputClosed()
  }

  @Test
  fun `test hide single line message area with explicit redraw`() {
    enterCommandForSingleLineOutput()
    assertStaticMessageArea()
    doTypeText("<C-L>")
    assertExOutputClosed()
  }

  @Test
  fun `test hide single line message area with escape`() {
    enterCommandForSingleLineOutput()
    assertStaticMessageArea()
    doTypeText("<Esc>")
    assertExOutputClosed()
  }

  @Test
  fun `test hide single line message area when scrolling line down`() {
    configureByPages(3)
    doTypeText("<C-F>")
    enterCommandForSingleLineOutput()
    assertStaticMessageArea()
    doTypeText("<C-E>")
    assertExOutputClosed()
  }

  @Test
  fun `test hide single line message area when scrolling line up`() {
    configureByPages(3)
    doTypeText("<C-F>")
    enterCommandForSingleLineOutput()
    assertStaticMessageArea()
    doTypeText("<C-Y>")
    assertExOutputClosed()
  }

  @Test
  fun `test hide single line message area when scrolling horizontally`() {
    configureByText("1234567890".repeat(10))
    enterCommandForSingleLineOutput()
    assertStaticMessageArea()
    doTypeText("$")
    assertExOutputClosed()
  }

  @Test
  fun `test hide single line message area when inserting line above`() {
    configureByText("""
      |{$c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommandForSingleLineOutput()
    assertStaticMessageArea()
    doTypeText("O")
    assertExOutputClosed()
  }

  @Test
  fun `test hide single line message area when inserting line below`() {
    configureByText("""
      |{$c}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommandForSingleLineOutput()
    assertStaticMessageArea()
    doTypeText("o")
    assertExOutputClosed()
  }

  @Test
  fun `test single line message area not hidden by scrolling caused by current action`() {
    configureByPages(5)
    doTypeText("G", "k")
    assertPosition(174, 0)
    enterCommandForSingleLineOutput()
    enterSearch("ipsum")  // Move to occurrence on last line
    assertPosition(174, 6)
    doTypeText("n")       // Wrap and scroll to top of file. Scrolling normally closes the message area
    assertPosition(0, 6)
    assertStaticMessageArea("search hit BOTTOM, continuing at TOP")
  }

  private fun enterCommandForSingleLineOutput() {
    enterCommand("echo 'lorem ipsum'")
  }

  private fun enterCommandForMultiLineOutput() {
    enterCommand("echo \"line 1\\nline 2\\nline 3\\nline 4\\nline 5\"")
  }

  private fun enterCommandForMultiPageOutput() {
    enterCommand("echo repeat(\"lorem ipsum\\n\", 100)")
  }

  private fun scrollOutputToEnd() {
    getOutputPanel().scrollToEnd()
  }

  private fun getOutputPanel(): OutputPanel {
    return injector.outputPanel.getCurrentOutputPanel() as OutputPanel
  }

  // TODO: Maybe force a specific font height for tests, then we could hard code page sizes?
  private val pageSize
    get() = getOutputPanel().pageSize

  private fun assertExOutputOpen() {
    assertNotNull(injector.outputPanel.getCurrentOutputPanel(), "Expecting ex output panel to be visible")
  }

  private fun assertExOutputClosed() {
    assertNull(injector.outputPanel.getCurrentOutputPanel(), "Expecting ex output panel to not be visible")
  }

  private fun assertPager(): OutputPanel {
    val panel = injector.outputPanel.getCurrentOutputPanel()
    assertNotNull(panel)
    val outputPanel = panel as? OutputPanel
    assertNotNull(outputPanel)
    assertFalse(outputPanel.isSingleLine, "Expected panel to not be single line")
    return outputPanel
  }

  private fun assertPagerTopLine(expectedLine0: Int) {
    val panel = assertPager()
    assertEquals(expectedLine0, panel.topLine)
  }

  private fun assertMorePrompt() {
    val outputPanel = assertPager()
    assertFalse(outputPanel.isAtEnd, "Expected panel to not be at end (more-prompt)")
    assertEquals("-- MORE --", outputPanel.promptText)
  }

  private fun assertVerboseMorePrompt() {
    val outputPanel = assertPager()
    assertFalse(outputPanel.isAtEnd, "Expected panel to not be at end (more-prompt)")
    assertEquals("-- MORE -- (RET: line, SPACE: page, d: half page, q: quit)", outputPanel.promptText)
  }

  private fun assertHitEnterPrompt() {
    val outputPanel = assertPager()
    assertTrue(outputPanel.isAtEnd, "Expected panel to be at end (hit-enter prompt)")
    assertEquals("Hit ENTER or type command to continue", outputPanel.promptText)
  }

  private fun assertStaticMessageArea(text: String? = null) {
    val panel = injector.outputPanel.getCurrentOutputPanel()
    assertNotNull(panel)
    val outputPanel = panel as? OutputPanel
    assertNotNull(outputPanel)
    assertTrue(outputPanel.isSingleLine, "Expected panel to be single line")
    if (text != null) {
      assertEquals(text, outputPanel.text)
    }
  }

  private fun doTypeText(vararg keys: String) {
    val panel = injector.outputPanel.getCurrentOutputPanel() as? OutputPanel

    // Note that this check means we can't correctly test key handling with a single-line message area. We're manually
    // doing the routing that would normally be handled by focus, but focus isn't supported in unit tests. If we
    // manually decide not to send the keystroke to the panel, then of course it won't close when we press `q`.
    if (panel != null && !panel.isSingleLine) {
      application.invokeAndWait {
        panel.fireKeys(*keys)
      }
    }
    else {
      typeText(*keys)
    }
  }

  private fun OutputPanel.fireKeys(vararg keys: String) {
    val keyStrokes = keys.flatMap { injector.parser.parseKeys(it) }
    keyStrokes.forEach { keyStroke ->
      handleKey(keyStroke)
    }
  }
}
