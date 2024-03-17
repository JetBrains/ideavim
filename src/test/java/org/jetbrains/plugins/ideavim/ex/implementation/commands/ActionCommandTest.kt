/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * @author Alex Plate
 */

class ActionCommandTest : VimTestCase() {
  // VIM-652 |:action|
  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun testEditorRightAction() {
    configureByText("<caret>foo\n" + "bar\n")
    enterCommand("action EditorRight")
    assertMode(Mode.NORMAL())
    assertState("f<caret>oo\n" + "bar\n")
  }
}
