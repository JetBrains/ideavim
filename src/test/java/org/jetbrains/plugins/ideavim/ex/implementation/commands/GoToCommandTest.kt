/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.GotoCharacterCommand
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GoToCommandTest : VimTestCase() {
  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("goto 10")
    assertTrue(command is GotoCharacterCommand)
    assertEquals("10", command.argument)
  }

  @Test
  fun `go to 10th character`() {
    configureByText("""
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
    """.trimIndent())
    enterCommand("goto 10")
    assertState("""
      Lorem ips${c}um dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
    """.trimIndent())
  }
}