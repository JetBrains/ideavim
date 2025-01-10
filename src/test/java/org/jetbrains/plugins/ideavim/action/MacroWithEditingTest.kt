/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitAndAssert
import org.junit.jupiter.api.Test

class MacroWithEditingTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test print macro`() {
    typeTextInFile(injector.parser.parseKeys("qa" + "iHello<Esc>" + "q"), "")
    setText("")
    typeText(injector.parser.parseKeys("\"ap"))
    assertState("iHello" + 27.toChar())
  }

  @Test
  fun `test copy and perform macro`() {
    typeTextInFile(injector.parser.parseKeys("^v\$h\"wy"), "iHello")
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    kotlin.test.assertEquals("iHello", VimPlugin.getRegister().getRegister(vimEditor, context, 'w')?.text)
    setText("")
    typeText(injector.parser.parseKeys("@w"))
    waitAndAssert {
      fixture.editor.document.text == "Hello"
    }
  }

  @Test
  fun `test copy and perform macro ctrl_a`() {
    typeTextInFile(injector.parser.parseKeys("^v\$h\"wy"), "\u0001")
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    kotlin.test.assertEquals(
      injector.parser.parseKeys("<C-A>"),
      injector.registerGroup.getRegister(vimEditor, context, 'w')!!.keys
    )
    setText("1")
    typeText(injector.parser.parseKeys("@w"))
    waitAndAssert {
      fixture.editor.document.text == "2"
    }
  }
}
