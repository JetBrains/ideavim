/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change.number

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ChangeNumberDecActionTest : VimTestCase() {
  @Test
  fun `test decrement hex to negative value`() {
    doTest("<C-X>", "0x0000", "0xffffffffffffffff", Mode.NORMAL())
  }

  @Test
  fun `test decrement hex to negative value by 10`() {
    doTest("10<C-X>", "0x0005", "0xfffffffffffffffb", Mode.NORMAL())
  }

  @Test
  fun `test decrement oct to negative value`() {
    doTest(
      ":set nrformats+=octal<CR><C-X>",
      "00000",
      "01777777777777777777777",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test decrement incorrect octal`() {
    doTest(":set nrformats+=octal<CR><C-X>", "008", "7", Mode.NORMAL())
  }

  @Test
  fun `test decrement oct to negative value by 10`() {
    doTest(
      ":set nrformats+=octal<CR>10<C-X>",
      "00005",
      "01777777777777777777773",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test undo after decrement number`() {
    configureByText("The answer is ${c}42")
    typeText("<C-X>")
    assertState("The answer is 4${c}1")
    typeText("u")
    assertState("The answer is ${c}42")
  }

  @Test
  fun `test undo after decrement with count`() {
    configureByText("Count: ${c}20")
    typeText("5<C-X>")
    assertState("Count: 1${c}5")
    typeText("u")
    assertState("Count: ${c}20")
  }

  @Test
  fun `test undo after decrement negative number`() {
    configureByText("Temperature: ${c}-5 degrees")
    typeText("<C-X>")
    assertState("Temperature: -${c}6 degrees")
    typeText("u")
    assertState("Temperature: ${c}-5 degrees")
  }

  @Test
  fun `test multiple undo after sequential decrements`() {
    configureByText("Value: ${c}100")
    typeText("<C-X>")
    assertState("Value: 9${c}9")
    typeText("<C-X>")
    assertState("Value: 9${c}8")
    typeText("<C-X>")
    assertState("Value: 9${c}7")
    
    // Undo third decrement
    typeText("u")
    assertState("Value: 9${c}8")
    
    // Undo second decrement
    typeText("u")
    assertState("Value: 9${c}9")
    
    // Undo first decrement
    typeText("u")
    assertState("Value: ${c}100")
  }

  @Test
  fun `test undo decrement with visual selection`() {
    configureByText("""
      ${c}10
      20
      30
    """.trimIndent())
    typeText("Vj<C-X>")  // Visual select first two lines and decrement
    assertState("""
      ${c}9
      19
      30
    """.trimIndent())
    typeText("u")
    assertState("""
      ${c}10
      20
      30
    """.trimIndent())
  }

  @Test
  fun `test undo increment and decrement combination`() {
    configureByText("Number: ${c}50")
    typeText("<C-A>")
    assertState("Number: 5${c}1")
    typeText("<C-X>")
    assertState("Number: 5${c}0")
    typeText("<C-X>")
    assertState("Number: 4${c}9")
    
    // Undo second decrement
    typeText("u")
    assertState("Number: 5${c}0")
    
    // Undo first decrement
    typeText("u")
    assertState("Number: 5${c}1")
    
    // Undo increment
    typeText("u")
    assertState("Number: ${c}50")
  }
}
