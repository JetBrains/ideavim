/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import com.intellij.idea.TestFor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import javax.swing.KeyStroke

@Suppress("SpellCheckingInspection")
class InsertRegisterActionTest : VimExTestCase() {
  @Test
  fun `test insert named register`() {
    enterCommand("let @a=\"hello world\"")
    typeText(":<C-R>a")
    assertExText("hello world${c}")
  }

  @Test
  fun `test insert named register after existing command line text`() {
    enterCommand("let @a=\"hello world\"")
    typeText(":set <C-R>a")
    assertExText("set hello world${c}")
  }

  @Test
  fun `test insert named register before existing command line text`() {
    enterCommand("let @a=\"hello world\"")
    typeText(":set<Home>", "<C-R>a")
    assertExText("hello world${c}set")
  }

  @Test
  fun `test insert register with multi-line register text`() {
    // parseKeys parses <CR> in a way that Register#getText doesn't like
    val keys = mutableListOf<KeyStroke>()
    keys.addAll(injector.parser.parseKeys("hello<CR>world"))
    VimPlugin.getRegister().setKeys('c', keys)

    typeText(":<C-R>c")
    assertExText("hello\u000Dworld")
    assertRenderedExText("hello^Mworld")
  }

  @Test
  fun `test insert register replays text as if typed without applying mapping`() {
    enterCommand("let @a=\"hello z world\"")
    enterCommand("cmap z q")
    typeText(":<C-R>a")
    assertExText("hello z world")
  }

  // TODO: Add test for not applying abbreviations, once we support abbreviations
  // TODO: Add test to ensure command line completion is not triggered, once we support completion

  @VimBehaviorDiffers(
    originalVimAfter = "hello<80>kl world",
    description = "Vim encodes the caret movement as `<80>kl` and inserts it literally."
      + "IdeaVim encodes and replays"
      + "IdeaVim cannot encode caret movement as part of a let command, but could record it in a macro."
  )
  @Test
  fun `test insert register replays caret movement unlike Vim`() {
    VimPlugin.getRegister().setKeys('a', injector.parser.parseKeys("hello<Left> world"))
    typeText(":<C-R>a")
    assertExText("hell world${c}o")
  }

  @Test
  fun `test insert register replays special characters as though typed - backspace`() {
    // Don't use enterCommand, it (deliberately) doesn't parse special keys!
    typeText(":let @a=\"hellox<C-V><C-H> world\"<CR>")
    typeText(":<C-R>a")
    assertExText("hello world")
  }

  @Test
  fun `test insert register with backspace can end command line and continue processing register`() {
    // Don't use enterCommand, it (deliberately) doesn't parse special keys!
    // Note that we can't add <Esc> at the end of this because it will just get inserted literally, and that's actually
    // hard to test against
    typeText(":let @a=\"set<C-V><C-H><C-V><C-H><C-V><C-H><C-V><C-H>ihello world\"<CR>")
    typeText(":<C-R>a")
    assertExIsDeactivated()
    assertState("hello world\n")
    assertState(Mode.INSERT)
  }

  @Test
  fun `test insert register replays special characters as though typed - go to start of line`() {
    // Don't use enterCommand, it (deliberately) doesn't parse special keys!
    typeText(":let @a=\"hello<C-V><C-B> world\"<CR>")
    typeText(":<C-R>a")
    assertExText(" world${c}hello")
  }

  @Test
  fun `test insert register replays special characters as though typed - delete to start of line`() {
    // Don't use enterCommand, it (deliberately) doesn't parse special keys!
    typeText(":let @a=\"hello<C-V><C-U> world\"<CR>")
    typeText(":<C-R>a")
    assertExText(" world")
  }

  @Test
  fun `test insert register replays special characters as though typed - enter digraph`() {
    // Don't use enterCommand, it (deliberately) doesn't parse special keys!
    typeText(":let @a=\"hello<C-V><C-K>OK world\"<CR>")
    typeText(":<C-R>a")
    assertExText("hello✓ world")
  }

  // Blah blah blah. No need to test all control characters

  // According to the docs, any shortcut that cancels the command line is inserted literally - <C-C>, <Esc>, <CR>
  // In practice, this includes the synonyms <C-M> and <C-[>
  @VimBehaviorDiffers("hello^C world")
  @Test
  fun `test insert register inserts CTRL-C literally`() {
    // TODO: IdeaVim doesn't currently support capturing CTRL-C as a literal - it always cancels the command line
    // So when we try to enter <C-C> as a literal, it's treated as cancel and the let command fails. We don't clear the
    // command line until we start a new one (we currently deactivate before getting text to process, although we could
    // easily swap that), so the command line will still have what we started to type before cancelling.
    // Note that we also get broken behaviour if we use setKeys, because that will save the <C-C> as a keypress, not as
    // a typed character.
    // This test is broken, but passes. Once we support <C-V><C-C>, it will start failing and we can fix the test.

    // Don't use enterCommand, it (deliberately) doesn't parse special keys!
    typeText(":let @a=\"hello<C-V><C-C> world\"<CR>")
    typeText(":<C-R>a")
//    assertRenderedExText("hello^C world")
    assertExText("let @a=\"hello")
  }

  @Test
  fun `test insert register inserts Escape literally`() {
    // Don't use enterCommand, it (deliberately) doesn't parse special keys!
    typeText(":let @a=\"hello<C-V><Esc> world\"<CR>")
    typeText(":<C-R>a")
    assertRenderedExText("hello^[ world")
  }

  @Test
  fun `test insert register inserts CTRL-OpenBracket literally`() {
    // Don't use enterCommand, it (deliberately) doesn't parse special keys!
    typeText(":let @a=\"hello<C-V><C-[> world\"<CR>")
    typeText(":<C-R>a")
    assertRenderedExText("hello^[ world")
  }

  @Test
  fun `test insert register inserts CR literally`() {
    // Don't use enterCommand, it (deliberately) doesn't parse special keys!
    typeText(":let @a=\"hello<C-V><CR> world\"<CR>")
    typeText(":<C-R>a")
    assertRenderedExText("hello^M world")
  }

  @Test
  fun `test insert register inserts CTRL-M literally`() {
    // Don't use enterCommand, it (deliberately) doesn't parse special keys!
    typeText(":let @a=\"hello<C-V><C-M> world\"<CR>")
    typeText(":<C-R>a")
    assertRenderedExText("hello^M world")
  }

  @Disabled
  @Test
  fun `test insert register inserts CTRL-J literally`() {
    // TODO: This doesn't work. DigraphSequence converts <C-V><C-J> to `\0`
    // I don't understand the logic behind this conversion

    // Don't use enterCommand, it (deliberately) doesn't parse special keys!
    typeText(":let @a=\"hello<C-V><C-J> world\"<CR>")
    typeText(":<C-R>a")
    assertRenderedExText("hello^J world")
  }

  @VimBehaviorDiffers("hello^L world")
  @Test
  fun `test insert register inserts non-shortcut control character literally instead of replaying`() {
    // Vim documents c_CTRL-R as inserting text as if typed; it appears to insert non-shortcut control chars literally
    typeText(":let @a=\"hello<C-V><C-L> world\"<CR>")
    typeText(":<C-R>a")
//    assertRenderedExText("hello^L world")
    assertRenderedExText("hello world")
  }

  @Test
  fun `test insert numbered register`() {
    VimPlugin.getRegister().setKeys('5', injector.parser.parseKeys("greetings programs"))
    typeText(":<C-R>5")
    assertExText("greetings programs${c}")
  }

  @Test
  fun `test insert small delete register`() {
    configureByText("""
      |Lorem ipsum, dolor, sit amet,
      |conse${c}ctetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    typeText("dw")
    typeText(":<C-R>-")
    assertExText("ctetur ")
  }

  @Test
  fun `test insert last inserted text register`() {
    configureByText("""
      |Lorem ${c}ipsum, dolor, sit amet,
    """.trimMargin())
    typeText("i", "hello world", "<Esc>")
    typeText(":<C-R>.")
    assertExText("hello world")
  }

  @Test
  fun `test insert last command register`() {
    typeText(":set incsearch<CR>")
    typeText(":<C-R>:")
    assertExText("set incsearch")
  }

  @Test
  fun `test insert last search pattern register`() {
    typeText("/hello<CR>")
    typeText(":<C-R>/")
    assertExText("hello")
  }

  // TODO: Tests for filename register ("%) and alternate buffer register ("#) once supported

  @Test
  @TestFor(issues = ["VIM-3506"])
  fun `test render quote prompt when awaiting for register`() {
    enterCommand("let @w=\"world\"")
    typeText(":hello <C-R>")
    assertRenderedExText("hello \"")
    typeText("w")
    assertRenderedExText("hello world")
  }
}
