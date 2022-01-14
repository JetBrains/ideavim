/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

/*
class NormalCommandTest : VimTestCase() {

  fun `test simple execution`() {
    doTest("normal x", "123<caret>456", "123<caret>56")
  }

  fun `test short command`() {
    doTest("norm x", "123<caret>456", "123<caret>56")
  }

  fun `test multiple commands`() {
    doTest("normal xiNewText", "123<caret>456", "123NewTex<caret>t56")
  }

  fun `test range single stroke`() {
    doTest(".norm x", "123<caret>456", "<caret>23456")
  }

  fun `test range multiple strokes`() {
    doTest(
      "1,3norm x",
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

  fun `test with mapping`() {
    configureByText(
      """
            <caret>123456
            123456
            123456
      """.trimIndent()
    )
    typeText(commandToKeys("map G dd"))
    typeText(commandToKeys("normal G"))
    assertState(
      """
      <caret>123456
      123456
      """.trimIndent()
    )
  }

  fun `test with disabled mapping`() {
    configureByText(
      """
            <caret>123456
            123456
            123456
      """.trimIndent()
    )
    typeText(commandToKeys("map G dd"))
    typeText(commandToKeys("normal! G"))
    assertState(
      """
            123456
            123456
            <caret>123456
      """.trimIndent()
    )
  }

  fun `test from visual mode`() {
    configureByText(
      """
            <caret>123456
            123456
            123456
            123456
            123456
      """.trimIndent()
    )
    typeText(parseKeys("Vjj"))
    typeText(commandToKeys("normal x"))
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

  fun `test execute visual mode`() {
    configureByText(
      """
        <caret>123456
        123456
        123456
        123456
        123456
      """.trimIndent()
    )
    typeText(commandToKeys("normal Vjj"))
    typeText(parseKeys("x"))
    assertState(
      """
          <caret>123456
          123456
      """.trimIndent()
    )
  }

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
    typeText(parseKeys("qqxq", "jVjjj"))
    typeText(commandToKeys("norm @q"))
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

  fun `test command executes at selection start`() {
    configureByText("hello <caret>world !")
    typeText(parseKeys("vw"))
    typeText(parseKeys(":<C-u>norm x<CR>"))
    assertState("hello <caret>orld !")
  }

  fun `test false escape`() {
    configureByText("hello <caret>world !")
    typeText(commandToKeys("norm i<Esc>"))
    assertState("hello <Esc<caret>>world !")
  }

  private fun doTest(command: String, before: String, after: String) {
    myFixture.configureByText("a.java", before)
    typeText(commandToKeys(command))
    myFixture.checkResult(after)
  }
}
*/
