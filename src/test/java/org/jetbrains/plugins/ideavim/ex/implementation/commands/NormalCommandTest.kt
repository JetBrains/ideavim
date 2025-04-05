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

class NormalCommandTest : VimTestCase() {
  @Test
  fun `test simple execution`() {
    doTest(exCommand("normal x"), "123<caret>456", "123<caret>56")
  }

  @Test
  fun `test short command`() {
    doTest(exCommand("norm x"), "123<caret>456", "123<caret>56")
  }

  @Test
  fun `test multiple commands`() {
    doTest(exCommand("normal xiNewText"), "123<caret>456", "123NewTex<caret>t56")
  }

  @Test
  fun `test normal command with current line range moves caret to start of line before executing command`() {
    doTest(exCommand(".norm x"), "123<caret>456", "<caret>23456")
  }

  @Test
  fun `test normal command with multi-line range`() {
    doTest(
      exCommand("1,3norm x"),
      """
         123456
         123456
         123456<caret>
         123456
         123456
      """.trimIndent(),
      """
         23456
         23456
         <caret>23456
         123456
         123456
      """.trimIndent()
    )
  }

  @Test
  fun `test with mapping`() {
    configureByText(
      """
            <caret>123456
            123456
            123456
      """.trimIndent()
    )
    enterCommand("map G dd")
    enterCommand("normal G")
    assertState(
      """
      <caret>123456
      123456
      """.trimIndent()
    )
  }

  @Test
  fun `test with disabled mapping`() {
    configureByText(
      """
            <caret>123456
            123456
            123456
      """.trimIndent()
    )
    enterCommand("map G dd")
    enterCommand("normal! G")
    assertState(
      """
            123456
            123456
            <caret>123456
      """.trimIndent()
    )
  }

  @Test
  fun `test normal from Visual mode runs command on start of each line in range`() {
    configureByText(
      """
            <caret>123456
            123456
            123456
            123456
            123456
      """.trimIndent()
    )
    typeText("Vjj")
    enterCommand("normal x")  // Will give `:'<,'>normal x`
    assertState(
      """
            23456
            23456
            <caret>23456
            123456
            123456
      """.trimIndent()
    )
  }

  @Test
  fun `test normal command switches to Visual mode`() {
    configureByText(
      """
        <caret>123456
        123456
        123456
        123456
        123456
      """.trimIndent()
    )
    enterCommand("normal Vjj")
    typeText("x")
    assertState(
      """
          <caret>123456
          123456
      """.trimIndent()
    )
  }

  @Test
  fun `test execute macros`() {
    configureByText(
      """
      <caret>123456
      123456
      123456
      123456
      123456
      123456
      """.trimIndent()
    )
    typeText("qqxq", "jVjjj")
    enterCommand("norm @q")
    assertState(
      """
      23456
      23456
      23456
      23456
      <caret>23456
      123456
      """.trimIndent()
    )
  }

  @Test
  fun `test command executes at selection start`() {
    configureByText("hello <caret>world !")
    typeText("vw")
    enterCommand("<C-u>norm x")
    assertState("hello <caret>orld !")
  }

  @Test
  fun `test false escape`() {
    configureByText("hello <caret>world !")
    enterCommand("norm i<Esc>")
    assertState("hello <Esc<caret>>world !")
  }

  @Test
  fun `test C-R`() {
    configureByText("""myprop: "my value"""")
    enterCommand("""exe "norm ^dei-\<C-R>\"-"""")
    assertState("""-myprop-: "my value"""")
  }
}
