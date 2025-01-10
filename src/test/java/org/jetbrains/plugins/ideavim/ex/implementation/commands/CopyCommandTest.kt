/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class CopyCommandTest : VimTestCase() {
  @Test
  fun `test duplicate line below`() {
    configureByText(
      """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
    """.trimIndent()
    )
    enterCommand("copy .")
    assertState(
      """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      ${c}Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
    """.trimIndent()
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
    """,
    description = "Caret placement is wrong",
    shouldBeFixed = true
  )
  @Test
  fun `test duplicate line below with 'nostartofline'`() {
    doTest(
      exCommand("copy ."),
      """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        accumsan vitae, facilisis ac nulla.
      """.trimIndent(),
      """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        ${c}Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        accumsan vitae, facilisis ac nulla.
      """.trimIndent(),
    ) {
      enterCommand("set nostartofline")
    }
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      ${c}accumsan vitae, facilisis ac nulla.
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
    """,
    description = "Caret placement is incorrect",
    shouldBeFixed = true
  )
  @Test
  fun `test copy range to above first line`() {
    doTest(
      exCommand("3,4copy 0"),
      """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        Nunc tincidunt viverra ${c}ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        accumsan vitae, facilisis ac nulla.
      """.trimIndent(),
      """
        ${c}Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        accumsan vitae, facilisis ac nulla.
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        accumsan vitae, facilisis ac nulla.
      """.trimIndent(),
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      ${c}accumsan vitae, facilisis ac nulla.
    """,
    description = "Caret placement is incorrect",
    shouldBeFixed = true
  )
  @Test
  fun `test copy range to below last line`() {
    doTest(
      exCommand("3,4copy $"),
      """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        Nunc tincidunt viverra ${c}ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        accumsan vitae, facilisis ac nulla.
      """.trimIndent(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
        |${c}Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
        |""".trimMargin(),
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      ${c}accumsan vitae, facilisis ac nulla.
    """,
    description = "Caret placement is incorrect",
    shouldBeFixed = true
  )
  @Test
  fun `test copy with no space between command and address`() {
    doTest(
      exCommand("3,4copy$"),
      """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        Nunc tincidunt viverra ${c}ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        accumsan vitae, facilisis ac nulla.
      """.trimIndent(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        |Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        |Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
        |${c}Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        |accumsan vitae, facilisis ac nulla.
        |""".trimMargin(),
    )
    assertPluginError(false)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      ${c}accumsan vitae, facilisis ac nulla.
    """,
    description = "Caret placement is wrong",
    shouldBeFixed = true
  )
  @Test
  fun `test copy to below first address of range with no errors`() {
    doTest(
      exCommand("3,4copy 4,1,2,.,0"),
      """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        Nunc tincidunt viverra ${c}ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        accumsan vitae, facilisis ac nulla.
      """.trimIndent(),
      """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
        Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
        ${c}Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        accumsan vitae, facilisis ac nulla.
        Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
        accumsan vitae, facilisis ac nulla.
      """.trimIndent(),
    )
    assertPluginError(false)
  }
}
