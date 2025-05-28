/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.state.mode.Mode
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
  fun `test normal command automatically exits Insert mode`() {
    doTest(exCommand("normal iFoo"), "123<caret>456", "123Fo<caret>o456", Mode.NORMAL())
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
  fun `test normal command with single letter mapping`() {
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
  fun `test normal command with multi-letter mapping`() {
    doTest(
      exCommand("normal dd"),
      """
        |${c}Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |${c}Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    ) {
      enterCommand("map dd G")
    }
  }

  @Test
  fun `test normal command with disabled mapping`() {
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

  @Test
  fun `test normal command delete and undo`() {
    configureByText(
      """
      |Line 1
      |Line ${c}2
      |Line 3
      """.trimMargin()
    )

    enterCommand("normal x")
    assertState(
      """
      |Line 1
      |Line 
      |Line 3
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |Line 1
      |Line ${c}2
      |Line 3
      """.trimMargin()
    )
  }

  @Test
  fun `test normal command with range and undo`() {
    configureByText(
      """
      |First ${c}line
      |Second line
      |Third line
      |Fourth line
      """.trimMargin()
    )

    enterCommand("2,3normal A!")
    assertState(
      """
      |First line
      |Second line!
      |Third line${c}!
      |Fourth line
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |First ${c}line
      |Second line
      |Third line
      |Fourth line
      """.trimMargin()
    )
  }

  @Test
  fun `test normal command insert and undo`() {
    configureByText(
      """
      |${c}Hello world
      """.trimMargin()
    )

    enterCommand("normal iTest ")
    assertState(
      """
      |Test${c} Hello world
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |${c}Hello world
      """.trimMargin()
    )
  }

  @Test
  fun `test normal command complex operation and undo`() {
    configureByText(
      """
      |Line ${c}1
      |Line 2
      |Line 3
      """.trimMargin()
    )

    enterCommand("normal ddp")
    assertState(
      """
      |Line 2
      |${c}Line 1
      |Line 3
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |Line ${c}1
      |Line 2
      |Line 3
      """.trimMargin()
    )
  }
}
