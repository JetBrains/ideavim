/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.matchup

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.extension.matchup.matchParenHighlights
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitAndAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The highlighted text is marked with « and » in the expected text.
 */
@TestWithoutNeovim(SkipNeovimReason.PLUGIN)
class MatchupMatchParenTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("matchup")
  }

  private val rubyIf = """
    |if condition
    |  puts "yes"
    |else
    |  puts "no"
    |end
  """.trimMargin()

  @Test
  fun `test cursor on opening word highlights the whole block`() {
    configureRuby(rubyIf.replaceFirst("if", "${c}if"))
    assertHighlights(
      """
      |«if» condition
      |  puts "yes"
      |«else»
      |  puts "no"
      |«end»
      """.trimMargin()
    )
  }

  @Test
  fun `test cursor in the middle of closing word`() {
    configureRuby(rubyIf.replaceFirst("end", "e${c}nd"))
    assertHighlights(
      """
      |«if» condition
      |  puts "yes"
      |«else»
      |  puts "no"
      |«end»
      """.trimMargin()
    )
  }

  @Test
  fun `test cursor on middle word`() {
    configureRuby(rubyIf.replaceFirst("else", "els${c}e"))
    assertHighlights(
      """
      |«if» condition
      |  puts "yes"
      |«else»
      |  puts "no"
      |«end»
      """.trimMargin()
    )
  }

  @Test
  fun `test nested blocks highlight only the block of the word`() {
    configureRuby(
      """
      |def foo
      |  ${c}if x
      |    bar
      |  end
      |end
      """.trimMargin()
    )
    assertHighlights(
      """
      |def foo
      |  «if» x
      |    bar
      |  «end»
      |end
      """.trimMargin()
    )
  }

  @Test
  fun `test no highlight when cursor is not on a word`() {
    configureRuby(rubyIf.replaceFirst("condition", "cond${c}ition"))
    assertHighlights(rubyIf)
  }

  @Test
  fun `test brackets are left to the IDE`() {
    configureRuby("foo${c}(bar)")
    assertHighlights("foo(bar)")
  }

  @Test
  fun `test highlight moves with the cursor`() {
    configureRuby(rubyIf.replaceFirst("if", "${c}if"))
    typeText("j")
    assertHighlights(rubyIf)
    typeText("j")
    assertHighlights(
      """
      |«if» condition
      |  puts "yes"
      |«else»
      |  puts "no"
      |«end»
      """.trimMargin()
    )
  }

  @Test
  fun `test editing a word updates the highlight`() {
    configureRuby(rubyIf.replaceFirst("if", "${c}if"))
    typeText("cwunless<Esc>")
    assertHighlights(
      """
      |«unless» condition
      |  puts "yes"
      |«else»
      |  puts "no"
      |«end»
      """.trimMargin()
    )
  }

  @Test
  fun `test deleting the closing word removes the highlight`() {
    configureRuby("${c}if x\nend")
    typeText("jdw")
    assertHighlights("if x\n")
  }

  @Test
  fun `test insert mode highlights the word before the cursor`() {
    configureRuby("if x\nend$c")
    typeText("A")
    assertHighlights("«if» x\n«end»")
  }

  @Test
  fun `test visual mode`() {
    configureRuby(rubyIf.replaceFirst("else", "${c}else"))
    typeText("v")
    assertHighlights(
      """
      |«if» condition
      |  puts "yes"
      |«else»
      |  puts "no"
      |«end»
      """.trimMargin()
    )
  }

  @Test
  fun `test unmatched word is not highlighted`() {
    configureRuby("${c}if x\n  y\n")
    assertHighlights("if x\n  y\n")
  }

  @Test
  fun `test singleton highlights an unmatched word`() {
    configureRuby("")
    executeVimscript("let g:matchup_matchparen_singleton = 1")
    configureRuby("${c}if x\n  y\n")
    assertHighlights("«if» x\n  y\n")
  }

  @Test
  fun `test html tags`() {
    configureByTextX("test.html", "<${c}div class=\"a\">\n  <p>x</p>\n</div>")
    assertHighlights("<«div class=\"a\">»\n  <p>x</p>\n<«/div»>")
  }

  @Test
  fun `test html tag in a comment is skipped`() {
    configureByTextX("test.html", "<${c}div>\n  <!-- </div> -->\n</div>")
    assertHighlights("<«div>»\n  <!-- </div> -->\n<«/div»>")
  }

  @Test
  fun `test match words`() {
    configureRuby("${c}foo\nx\nbar")
    executeVimscript("let b:match_words = '\\<foo\\>:\\<bar\\>'")
    typeText("l")
    typeText("h")
    assertHighlights("«foo»\nx\n«bar»")
  }

  @Test
  fun `test NoMatchParen and DoMatchParen`() {
    configureRuby(rubyIf.replaceFirst("if", "${c}if"))
    enterCommand("NoMatchParen")
    assertHighlights(rubyIf)
    typeText("j")
    typeText("k")
    assertHighlights(rubyIf)

    enterCommand("DoMatchParen")
    assertHighlights(
      """
      |«if» condition
      |  puts "yes"
      |«else»
      |  puts "no"
      |«end»
      """.trimMargin()
    )
  }

  @Test
  fun `test disabled in buffer`() {
    configureRuby(rubyIf.replaceFirst("if", "${c}if"))
    executeVimscript("let b:matchup_matchparen_enabled = 0")
    typeText("l")
    assertHighlights(rubyIf)
  }

  @Test
  fun `test disabled at startup`() {
    configureRuby("")
    executeVimscript("let g:matchup_matchparen_enabled = 0")
    executeVimscript("set nomatchup")
    executeVimscript("set matchup")
    configureRuby(rubyIf.replaceFirst("if", "${c}if"))
    typeText("l")
    assertHighlights(rubyIf)
  }

  @Test
  fun `test disabling the extension removes the highlight`() {
    configureRuby(rubyIf.replaceFirst("if", "${c}if"))
    assertHighlights(
      """
      |«if» condition
      |  puts "yes"
      |«else»
      |  puts "no"
      |«end»
      """.trimMargin()
    )
    executeVimscript("set nomatchup")
    assertHighlights(rubyIf)
  }

  @Test
  fun `test nomode turns off highlighting in insert mode`() {
    configureRuby("")
    executeVimscript("let g:matchup_matchparen_nomode = 'i'")
    configureRuby("${c}if x\nend")
    assertHighlights("«if» x\n«end»")
    typeText("i")
    assertMode(Mode.INSERT)
    assertHighlights("if x\nend")
  }

  @Test
  fun `test stopline limits the search`() {
    configureRuby("")
    executeVimscript("let g:matchup_matchparen_stopline = 2")
    configureRuby("${c}if x\n  a\n  b\n  c\nend")
    assertHighlights("if x\n  a\n  b\n  c\nend")
  }

  @Test
  fun `test highlight surrounding until the cursor moves`() {
    configureRuby("")
    executeVimscript("nmap <F7> <Plug>(matchup-hi-surround)")
    configureRuby("if x\n  fo${c}o\nend")
    assertHighlights("if x\n  foo\nend")
    typeText("<F7>")
    assertHighlights("«if» x\n  foo\n«end»")
    typeText("h")
    assertHighlights("if x\n  foo\nend")
  }

  @Test
  fun `test highlight surrounding brackets`() {
    configureRuby("")
    executeVimscript("nmap <F7> <Plug>(matchup-hi-surround)")
    configureRuby("foo(b${c}ar)")
    typeText("<F7>")
    assertHighlights("foo«(»bar«)»")
  }

  @Test
  fun `test always highlight surrounding`() {
    configureRuby("")
    executeVimscript("let g:matchup_matchparen_hi_surround_always = 1")
    configureRuby("if x\n  fo${c}o\nend")
    assertHighlights("«if» x\n  foo\n«end»")
  }

  @Test
  fun `test deferred highlighting`() {
    configureRuby("")
    executeVimscript("let g:matchup_matchparen_deferred = 1")
    configureRuby("${c}if x\nend")
    typeText("l")
    waitAndAssert { highlightedText() == "«if» x\n«end»" }
  }

  @Test
  fun `test match above the screen`() {
    configureRuby("if x\n" + "  a\n".repeat(100) + "end")
    typeText("G")
    waitForMatchParen()
    assertEquals(0, fixture.editor.matchParenHighlights.offscreenLine)
  }

  @Test
  fun `test match below the screen`() {
    configureRuby("${c}if x\n" + "  a\n".repeat(100) + "end")
    waitForMatchParen()
    assertEquals(101, fixture.editor.matchParenHighlights.offscreenLine)
  }

  @Test
  fun `test match on screen`() {
    configureRuby(rubyIf.replaceFirst("if", "${c}if"))
    waitForMatchParen()
    assertNull(fixture.editor.matchParenHighlights.offscreenLine)
  }

  @Test
  fun `test offscreen can be turned off`() {
    configureRuby("")
    executeVimscript("let g:matchup_matchparen_offscreen = {}")
    configureRuby("if x\n" + "  a\n".repeat(100) + "end")
    typeText("G")
    waitForMatchParen()
    assertNull(fixture.editor.matchParenHighlights.offscreenLine)
  }

  private fun configureRuby(content: String) {
    configureByTextX("test.rb", content)
  }

  private fun assertHighlights(expected: String) {
    waitForMatchParen()
    assertEquals(expected, highlightedText())
  }

  private fun highlightedText(): String {
    val text = StringBuilder(fixture.editor.document.text)
    fixture.editor.matchParenHighlights.ranges.sortedByDescending { it.startOffset }.forEach {
      text.insert(it.endOffset, '»')
      text.insert(it.startOffset, '«')
    }
    return text.toString()
  }

  private fun waitForMatchParen() {
    ApplicationManager.getApplication().invokeAndWait {
      repeat(3) {
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
      }
    }
  }
}
