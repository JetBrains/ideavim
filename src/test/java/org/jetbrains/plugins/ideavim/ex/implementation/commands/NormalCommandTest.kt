/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

/*
class NormalCommandTest : VimTestCase() {

  @Test
  fun `test simple execution`() {
    doTest("normal x", "123<caret>456", "123<caret>56")
  }

  @Test
  fun `test short command`() {
    doTest("norm x", "123<caret>456", "123<caret>56")
  }

  @Test
  fun `test multiple commands`() {
    doTest("normal xiNewText", "123<caret>456", "123NewTex<caret>t56")
  }

  @Test
  fun `test range single stroke`() {
    doTest(".norm x", "123<caret>456", "<caret>23456")
  }

  @Test
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

  @Test
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

  @Test
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

  @Test
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

  @Test
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

  @Test
  fun `test command executes at selection start`() {
    configureByText("hello <caret>world !")
    typeText(parseKeys("vw"))
    typeText(parseKeys(":<C-u>norm x<CR>"))
    assertState("hello <caret>orld !")
  }

  @Test
  fun `test false escape`() {
    configureByText("hello <caret>world !")
    typeText(commandToKeys("norm i<Esc>"))
    assertState("hello <Esc<caret>>world !")
  }

  @Test
  fun `test C-R`() {
    configureByText("myprop: \"my value\"")
    typeText(commandToKeys("exe \"norm ^dei-\\<C-R>\\\"-\""))
    assertState("-myprop-: \"my value\"")
  }

  private fun doTest(command: String, before: String, after: String) {
    myFixture.configureByText("a.java", before)
    typeText(commandToKeys(command))
    myFixture.checkResult(after)
  }
}
*/
