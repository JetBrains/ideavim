/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent

class VirtualEditSplitTest : IdeaVimStarterTestBase() {

  private var fileCounter = 0

  private fun open(virtualedit: String, content: String) {
    fileCounter++
    openFile(createFile("src/VirtualEdit$fileCounter.txt", content))
    exCommand("set virtualedit=$virtualedit")
    pause(1000)
  }

  private val keyPattern = Regex("<(Esc|BS|CR|Right|C-w|C-r)>")

  /** Types [keys] in the editor, with `<Esc>`, `<BS>`, `<CR>`, `<Right>`, `<C-w>` and `<C-r>` sent as key presses */
  private fun keys(keys: String) {
    typeKeys {
      var last = 0
      for (match in keyPattern.findAll(keys)) {
        if (match.range.first > last) typeText(keys.substring(last, match.range.first))
        when (match.groupValues[1]) {
          "Esc" -> key(KeyEvent.VK_ESCAPE)
          "BS" -> key(KeyEvent.VK_BACK_SPACE)
          "CR" -> key(KeyEvent.VK_ENTER)
          "Right" -> key(KeyEvent.VK_RIGHT)
          "C-w" -> hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_W)
          "C-r" -> hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_R)
        }
        last = match.range.last + 1
      }
      if (last < keys.length) typeText(keys.substring(last))
    }
    pause(700)
  }

  private fun assertText(expected: String, message: String) {
    var actual = ""
    waitUntil { actual = editorText(); actual == expected }
    assertEquals(expected.visible(), actual.visible(), message)
  }

  private fun String.visible() = replace(' ', '·')

  // ── 'virtualedit=all' ──────────────────────────────────────

  @Test
  fun `all - insert after the line end pads`() {
    open("all", "line one")
    keys("\$5lix<Esc>")
    assertText("line one    x", "\$5lix<Esc>")
  }

  @Test
  fun `all - append after the line end pads`() {
    open("all", "line one")
    keys("\$5la!<Esc>")
    assertText("line one     !", "\$5la!<Esc>")
  }

  @Test
  fun `all - replace char on a short last line pads`() {
    open("all", "line one\nab")
    keys("G5lrZ")
    assertText("line one\nab   Z", "G5lrZ")
  }

  @Test
  fun `all - put after the line end pads`() {
    open("all", "line one")
    keys("yiw\$5lp")
    assertText("line one     line", "yiw\$5lp")
  }

  @Test
  fun `all - counted append pads once`() {
    open("all", "line one")
    keys("\$5l3a!<Esc>")
    assertText("line one     !!!", "\$5l3a!<Esc>")
  }

  @Test
  fun `all - dot repeat pads at its own caret`() {
    open("all", "line one\nab")
    keys("\$5lix<Esc>")
    assertText("line one    x\nab", "\$5lix<Esc>")
    keys("j.")
    assertText("line one    x\nab          x", "j.")
  }

  @Test
  fun `all - backspace in virtual space is not repeated`() {
    open("all", "alpha bravo\nab\n\nlonger line here")
    keys("j6li<BS>X<Esc>")
    assertText("alpha bravo\nab   X\n\nlonger line here", "j6li<BS>X<Esc>")
    keys("jj6l.")
    assertText("alpha bravo\nab   X\n\nlonger lineX here", "jj6l.")
  }

  @Test
  fun `all - ctrl-w in virtual space is not repeated`() {
    open("all", "alpha bravo\nab\n\nlonger line here")
    keys("j6li<C-w>X<Esc>")
    assertText("alpha bravo\nabX\n\nlonger line here", "j6li<C-w>X<Esc>")
    keys("jj6l.")
    assertText("alpha bravo\nabX\n\nlonger lXine here", "jj6l.")
  }

  @Test
  fun `all - undo of a padded insert is one step`() {
    open("all", "line one")
    keys("\$5lix<Esc>")
    assertText("line one    x", "\$5lix<Esc>")
    keys("u")
    assertText("line one", "u")
  }

  @Test
  fun `all - x and D after the line end do nothing`() {
    open("all", "line one\nline two")
    keys("\$5lx")
    assertText("line one\nline two", "\$5lx")
    keys("D")
    assertText("line one\nline two", "D")
  }

  // ── 'virtualedit=insert' ───────────────────────────────────

  @Test
  fun `insert - right after the line end pads`() {
    open("insert", "line one")
    keys("A<Right><Right>y<Esc>")
    assertText("line one  y", "A<Right><Right>y<Esc>")
  }

  @Test
  fun `insert - ctrl-r expression at the line end`() {
    open("insert", "line one")
    keys("A<C-r>=1<CR><Esc>")
    assertText("line one1", "A<C-r>=1<CR><Esc>")
  }

  // ── 'virtualedit' turned off ───────────────────────────────

  @Test
  fun `off after all - typing at the line end does not pad`() {
    open("all", "line one\nline two")
    keys("\$5lix<Esc>")
    assertText("line one    x\nline two", "\$5lix<Esc>")
    exCommand("set virtualedit=")
    pause(1000)
    keys("jA<Right><Right>y<Esc>")
    assertText("line one    x\nline twoy", "jA<Right><Right>y<Esc>")
    keys("\$5liz<Esc>")
    assertText("line one    x\nline twozy", "\$5liz<Esc>")
  }

  @Test
  fun `off after insert - typing at the line end does not pad`() {
    open("insert", "line one")
    keys("A<Right><Right>y<Esc>")
    assertText("line one  y", "A<Right><Right>y<Esc>")
    exCommand("set virtualedit=")
    pause(1000)
    keys("A<Right><Right>z<Esc>")
    assertText("line one  yz", "A<Right><Right>z<Esc>")
  }

  // ── Sanity without 'virtualedit' ───────────────────────────

  @Test
  fun `none - append and dot repeat`() {
    open("", "line one\nab")
    keys("Afoo<Esc>")
    assertText("line onefoo\nab", "Afoo<Esc>")
    keys("j.")
    assertText("line onefoo\nabfoo", "j.")
  }

  @Test
  fun `none - x and p`() {
    open("", "line one")
    keys("0x")
    assertText("ine one", "0x")
    keys("p")
    assertText("ilne one", "p")
    keys("yiw\$p")
    assertText("ilne oneilne", "yiw\$p")
  }
}
