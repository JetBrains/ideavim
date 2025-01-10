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

@Suppress("SpellCheckingInspection")
class GotoCharacterCommandTest : VimTestCase() {
  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("goto 10")
    assertTrue(command is GotoCharacterCommand)
    assertEquals("10", command.argument)
  }

  @Test
  fun `go to offset 1 with no count or range`() {
    doTest(
      exCommand("goto"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin(),
      """
        |${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin()
    )
  }

  @Test
  fun `go to 10th character`() {
    configureByText(
      """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
    """.trimIndent()
    )
    enterCommand("goto 10")
    assertState(
      """
      Lorem ips${c}um dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
    """.trimIndent()
    )
  }

  @Test
  fun `go to 10th character via range`() {
    doTest(
      exCommand("10goto"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin(),
      """
        |Lorem ips${c}um dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin()
    )
  }

  @Test
  fun `go to character uses line number from range for character offset`() {
    doTest(
      exCommand("\$goto"),  // $ is last line (4)
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin(),
      """
        |Lor${c}em ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin()
    )
  }

  @Test
  fun `go to character with last address in range`() {
    doTest(
      exCommand("1,2,3,4,10goto"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin(),
      """
        |Lorem ips${c}um dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin()
    )
  }

  @Test
  fun `go to character with offset`() {
    doTest(
      exCommand("10-3goto"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin(),
      """
        |Lorem ${c}ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin()
    )
  }

  @Test
  fun `go to character with range and count uses count`() {
    doTest(
      exCommand("20goto 10"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin(),
      """
        |Lorem ips${c}um dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin()
    )
  }

  @Test
  fun `go to character with count as range reports trailing characters error`() {
    doTest(
      exCommand("goto 10,20"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
      """.trimMargin()
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: ,20")
  }
}
