/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class OptionTest : VimTestCase() {

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test option`() {
    configureByText("\n")
    enterCommand("set ignorecase")   // Default is off
    typeText(commandToKeys("if &ic | echo 'ignore case is on' | else | echo 'ignore case is off' | endif"))
    assertExOutput("ignore case is on\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test option2`() {
    configureByText("\n")
    enterCommand("set ignorecase")   // Default is off
    enterCommand("set noignorecase")
    typeText(commandToKeys("if &ic | echo 'ignore case is on' | else | echo 'ignore case is off' | endif"))
    assertExOutput("ignore case is off\n")
  }

  fun `test multiple options`() {
    configureByText("\n")
    enterCommand("set ignorecase digraph") // Both off by default
    typeText(commandToKeys("if &ic | echo 'ignore case is on' | else | echo 'ignore case is off' | endif"))
    assertExOutput("ignore case is on\n")
    typeText(commandToKeys("if &dg | echo 'digraph is on' | else | echo 'digraph is off' | endif"))
    assertExOutput("digraph is on\n")
  }
}
