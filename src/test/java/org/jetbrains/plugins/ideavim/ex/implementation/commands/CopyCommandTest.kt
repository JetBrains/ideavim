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

  @Test
  fun `test copy line and undo`() {
    configureByText(
      """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit.
      Nunc tincidunt viverra ligula non ${c}scelerisque.
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor.
      """.trimIndent()
    )

    enterCommand("copy .")
    assertState(
      """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit.
      Nunc tincidunt viverra ligula non scelerisque.
      ${c}Nunc tincidunt viverra ligula non scelerisque.
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor.
      """.trimIndent()
    )

    typeText("u")
    assertState(
      """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit.
      Nunc tincidunt viverra ligula non ${c}scelerisque.
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor.
      """.trimIndent()
    )
  }

  @Test
  fun `test copy range and undo`() {
    configureByText(
      """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit.
      Morbi nec luctus tortor, id venenatis lacus.
      Nunc sit amet tellus vel ${c}purus cursus posuere et at purus.
      Ut id dapibus augue.
      Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
      """.trimIndent()
    )

    enterCommand("2,3copy $")
    assertState(
      """
      |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
      |Morbi nec luctus tortor, id venenatis lacus.
      |Nunc sit amet tellus vel purus cursus posuere et at purus.
      |Ut id dapibus augue.
      |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
      |${c}Morbi nec luctus tortor, id venenatis lacus.
      |Nunc sit amet tellus vel purus cursus posuere et at purus.
      |""".trimMargin()
    )

    typeText("u")
    assertState(
      """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit.
      Morbi nec luctus tortor, id venenatis lacus.
      Nunc sit amet tellus vel ${c}purus cursus posuere et at purus.
      Ut id dapibus augue.
      Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
      """.trimIndent()
    )
  }

  @Test
  fun `test t command synonym and undo`() {
    configureByText(
      """
      Line 1
      Line 2
      Line ${c}3
      Line 4
      """.trimIndent()
    )

    enterCommand("t-1")
    assertState(
      """
      Line 1
      Line 2
      ${c}Line 3
      Line 3
      Line 4
      """.trimIndent()
    )

    typeText("u")
    assertState(
      """
      Line 1
      Line 2
      Line ${c}3
      Line 4
      """.trimIndent()
    )
  }
}
