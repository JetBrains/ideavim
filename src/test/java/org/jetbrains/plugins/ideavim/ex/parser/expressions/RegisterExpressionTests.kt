/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.vimscript.model.expressions.RegisterExpression
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RegisterExpressionTests : VimTestCase("\n") {
  @Test
  fun `test parsing non-empty register expression`() {
    assertEquals(RegisterExpression('s'), VimscriptParser.parseExpression("@s"))
  }

  @Test
  fun `test parsing unnamed register expression`() {
    assertEquals(RegisterExpression('@'), VimscriptParser.parseExpression("@@"))
  }

  @Test
  fun `test only parses known registers`() {
    assertEquals(null, VimscriptParser.parseExpression("@]"))
  }

  @Test
  fun `test evaluates register contents`() {
    val executionContext = injector.executionContextManager.getEditorExecutionContext(fixture.editor.vim)
    injector.registerGroup.storeText(fixture.editor.vim, executionContext, 's', "hello, world!")
    assertEquals("hello, world!", VimscriptParser.parseExpression("@s")?.evaluate()?.toVimString()?.value)
  }

  // In `Vim -u NONE`, so all registers are empty:
  // `:echo string(@a)` => ''
  // `:echo type(@a) == v:t_string` => 1
  // `:echo @a == v:null` => 1 (it's a null value, but it's a string!)
  // The uninitialised register does not appear in `:registers`. If we set it to an empty string, then it does.
  // Making VimString nullable would be a breaking change to external extensions. We might be able to introduce a
  // VimSpecial that could be used to represent v:null (and v:none, maybe v:true, v:false), and use that when evaluating
  // an uninitialised register.
  @VimBehaviorDiffers("Vim returns a null string, which we don't support")
  @Test
  fun `test evaluates uninitialised register to empty string`() {
    assertEquals("", VimscriptParser.parseExpression("@a")?.evaluate()?.toVimString()?.value)
  }
}
