/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class DeleteLinesCommandTest : VimTestCase() {
  @Test
  fun `test delete command without range`() {
    configureByText(
      """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
    """.trimIndent()
    )
    enterCommand("d")
    assertState(
      """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      ${c}Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
    """.trimIndent()
    )
  }

  @Test
  fun `test delete command with single line range`() {
    doTest(
      exCommand("2d"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
  }

  @Test
  fun `test delete first line with 0`() {
    doTest(
      exCommand("0d"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
  }

  @Test
  fun `test delete command with line range`() {
    doTest(
      exCommand("2,5d"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
  }

  @Test
  fun `test delete command with range starting with 0`() {
    doTest(
      exCommand("0,5d"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |${c}Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
  }

  @Test
  fun `test delete command with missing start address in range`() {
    doTest(
      exCommand(",5d"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
  }

  @Test
  fun `test delete command with missing end address in range`() {
    doTest(
      exCommand("2,d"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
  }

  @Test
  fun `test delete command with range starting with negative offset`() {
    doTest(
      exCommand("-10,5d"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E16: Invalid range")
  }

  @Test
  fun `test delete to register`() {
    doTest(
      exCommand("d a"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    assertRegister('a', "Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.^J")
  }

  @Test
  fun `test delete appends to register`() {
    doTest(
      listOf("\"ayy", exCommand("d A")),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    assertRegister(
      'a', "Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.^J" +
        "Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.^J"
    )
  }

  @Test
  fun `test delete to read only register`() {
    doTest(
      exCommand("d :"), // `%` and `.` are both read-only registers and range addresses
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: :")
  }

  @Test
  fun `test delete to invalid register`() {
    doTest(
      exCommand("d ("),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: (")
  }

  @Test
  fun `test delete with count`() {
    doTest(
      exCommand("d 3"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
  }

  @Test
  fun `test delete with count with no separating space`() {
    doTest(
      exCommand("d3"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
  }

  @Test
  fun `test delete with count and single line range`() {
    doTest(
      exCommand("2d4"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
  }

  @Test
  fun `test delete with count and line range`() {
    doTest(
      exCommand("2,4d3"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |${c}Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
  }

  @Test
  fun `test delete command with count specified as range reports errors`() {
    doTest(
      exCommand("d 1,4"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: ,4")
  }

  @Test
  fun `test delete command with count specified as address reports errors`() {
    doTest(
      exCommand("d ."),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: .")
  }

  @Test
  fun `test delete with register and count`() {
    doTest(
      exCommand("d a 3"),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Morbi nec luctus tortor, id venenatis lacus.
        |Nunc sit amet tellus vel purus cursus posuere et at purus.
        |Ut id dapibus augue.
        |Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        |${c}Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.
        |Pellentesque orci dolor, tristique quis rutrum non, scelerisque id dui.
      """.trimMargin()
    )
    assertRegister(
      'a', "Morbi nec luctus tortor, id venenatis lacus.^J" +
        "Nunc sit amet tellus vel purus cursus posuere et at purus.^J" +
        "Ut id dapibus augue.^J"
    )
  }
}
