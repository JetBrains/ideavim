/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.gx

import com.maddyhome.idea.vim.api.VimExternalOpener
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.mock.MockTestCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

/**
 * Behavior specs for the `gx` command (VIM-1341) implemented by
 * [com.maddyhome.idea.vim.action.motion.search.GotoUrlAction].
 *
 * `gx` opens the URL under the cursor with an external program. Because the launch is a platform
 * side effect, the command routes through the [VimExternalOpener] service; these tests replace it
 * with a mock (see [MockTestCase.mockService]) and assert which target and viewer `gx` resolved,
 * without ever launching anything. The viewer argument is the value of `g:netrw_browsex_viewer`
 * (or `null` for the OS default handler).
 */
class GxActionTest : MockTestCase() {

  @Test
  fun `test gx opens the URL under the cursor`() {
    val handler = mockService(VimExternalOpener::class.java)
    configureByText("See ${c}https://jetbrains.com for details")

    typeText("gx")

    verify(handler).open(eq("https://jetbrains.com"), eq(null))
  }

  @Test
  fun `test gx opens the whole URL when the cursor is in the middle`() {
    val handler = mockService(VimExternalOpener::class.java)
    configureByText("See https://jetbr${c}ains.com for details")

    typeText("gx")

    verify(handler).open(eq("https://jetbrains.com"), eq(null))
  }

  @Test
  fun `test gx strips surrounding parentheses and trailing punctuation`() {
    val handler = mockService(VimExternalOpener::class.java)
    configureByText("Docs (${c}https://jetbrains.com).")

    typeText("gx")

    verify(handler).open(eq("https://jetbrains.com"), eq(null))
  }

  @Test
  fun `test gx opens a bare www URL`() {
    val handler = mockService(VimExternalOpener::class.java)
    configureByText("visit ${c}www.jetbrains.com today")

    typeText("gx")

    verify(handler).open(eq("www.jetbrains.com"), eq(null))
  }

  @Test
  fun `test gx opens a URL with a path and query`() {
    val handler = mockService(VimExternalOpener::class.java)
    configureByText("${c}https://jetbrains.com/idea/download?os=mac end")

    typeText("gx")

    verify(handler).open(eq("https://jetbrains.com/idea/download?os=mac"), eq(null))
  }

  @Test
  fun `test gx opens a URL with a non-http scheme`() {
    val handler = mockService(VimExternalOpener::class.java)
    configureByText("clone ${c}ssh://git@github.com/JetBrains/ideavim.git please")

    typeText("gx")

    verify(handler).open(eq("ssh://git@github.com/JetBrains/ideavim.git"), eq(null))
  }

  @Test
  fun `test gx opens a URL with a custom application scheme`() {
    val handler = mockService(VimExternalOpener::class.java)
    configureByText("open ${c}vscode://file/Users/me/project now")

    typeText("gx")

    verify(handler).open(eq("vscode://file/Users/me/project"), eq(null))
  }

  @Test
  fun `test gx does nothing when the cursor is not on a URL`() {
    val handler = mockService(VimExternalOpener::class.java)
    configureByText("just some ${c}plain words here")

    typeText("gx")

    verify(handler, never()).open(any(), any())
  }

  @Test
  fun `test gx does not navigate like GotoDeclaration`() {
    val handler = mockService(VimExternalOpener::class.java)
    // `gd` (GotoDeclaration) on an identifier jumps to its declaration. `gx` must never navigate
    // within the editor: on a non-URL word it does nothing, and the caret stays exactly where it was.
    val text = "val ${c}counter = counter + 1"
    configureByText(text)

    typeText("gx")

    assertState(text)
    assertMode(Mode.NORMAL())
    verify(handler, never()).open(any(), any())
  }

  @Test
  fun `test gx opens the URL without moving the caret`() {
    val handler = mockService(VimExternalOpener::class.java)
    // Unlike GotoDeclaration, opening a URL is an external action - the caret does not move.
    val text = "See ${c}https://jetbrains.com now"
    configureByText(text)

    typeText("gx")

    assertState(text)
    assertMode(Mode.NORMAL())
    verify(handler).open(eq("https://jetbrains.com"), eq(null))
  }

  @Test
  fun `test gx passes g netrw_browsex_viewer as the viewer`() {
    val handler = mockService(VimExternalOpener::class.java)
    configureByText("See ${c}https://jetbrains.com")
    enterCommand("let g:netrw_browsex_viewer = 'firefox'")

    typeText("gx")

    verify(handler).open(eq("https://jetbrains.com"), eq("firefox"))
  }

  @Test
  fun `test gx passes a multi-word viewer command`() {
    val handler = mockService(VimExternalOpener::class.java)
    configureByText("See ${c}https://jetbrains.com")
    enterCommand("let g:netrw_browsex_viewer = 'open -a Safari'")

    typeText("gx")

    verify(handler).open(eq("https://jetbrains.com"), eq("open -a Safari"))
  }

  @Test
  fun `test gx treats a dash viewer as the default handler`() {
    val handler = mockService(VimExternalOpener::class.java)
    configureByText("See ${c}https://jetbrains.com")
    enterCommand("let g:netrw_browsex_viewer = '-'")

    typeText("gx")

    // netrw uses "-" to mean "fall back to the default file handler".
    verify(handler).open(eq("https://jetbrains.com"), eq(null))
  }

  @Test
  fun `test gx treats an empty viewer as the default handler`() {
    val handler = mockService(VimExternalOpener::class.java)
    configureByText("See ${c}https://jetbrains.com")
    enterCommand("let g:netrw_browsex_viewer = ''")

    typeText("gx")

    verify(handler).open(eq("https://jetbrains.com"), eq(null))
  }
}
