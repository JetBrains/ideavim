/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.textobjquote

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
class VimTextObjQuoteExtensionMappingTest : VimTestCase() {
  @Test
  fun `test custom double quote motion`() {
    configureByText("She said “he${c}llo there” and left")
    executeVimscript("let g:textobj#quote#doubleMotion = 'x'")
    enableExtensions("textobj-quote")
    typeText("dix")
    assertState("She said “${c}” and left")
  }

  @Test
  fun `test custom single quote motion`() {
    configureByText("She said ‘he${c}llo there’ and left")
    executeVimscript("let g:textobj#quote#singleMotion = 'x'")
    enableExtensions("textobj-quote")
    typeText("dix")
    assertState("She said ‘${c}’ and left")
  }

  @Test
  fun `test plug mapping`() {
    configureByText("She said “he${c}llo there” and left")
    enableExtensions("textobj-quote")
    executeVimscript("omap ix <Plug>(textobj-quote-select-d-i)")
    typeText("dix")
    assertState("She said “${c}” and left")
  }

  @Test
  fun `test custom double quotes`() {
    configureByText("Sie sagte „Hal${c}lo“ und ging")
    enableExtensions("textobj-quote")
    executeVimscript("let g:textobj#quote#doubleDefault = '„“'")
    typeText("diq")
    assertState("Sie sagte „${c}“ und ging")
  }

  @Test
  fun `test custom single closing quote followed by a letter is not an apostrophe`() {
    configureByText("‚Hal${c}lo‘x und ‚Welt‘")
    enableExtensions("textobj-quote")
    executeVimscript("let g:textobj#quote#singleDefault = '‚‘'")
    typeText("diQ")
    assertState("‚${c}‘x und ‚Welt‘")
  }

  @Test
  fun `test invalid custom quotes fall back to the default ones`() {
    configureByText("She said “he${c}llo there” and left")
    enableExtensions("textobj-quote")
    executeVimscript("let g:textobj#quote#doubleDefault = 'x'")
    typeText("diq")
    assertState("She said “${c}” and left")
  }
}
