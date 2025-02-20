/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class PrintCommandTest : VimTestCase() {
  @Test
  fun `test default range`() {
    configureByText(initialText)
    typeText(commandToKeys("p"))
    assertExOutput("    Lorem Ipsum")
  }

  @Test
  fun `test clears output between execution`() {
    configureByText(initialText)
    typeText(commandToKeys("p"))
    assertExOutput("    Lorem Ipsum", clear = false)
    // TODO: We need a better way to handle output
    // We should be waiting for a keypress now, such as <Enter> or <Esc> to close the output panel. But that's handled
    // by a separate key event loop which doesn't operate in tests.
    // Simulate closing the output panel in the same way as if we'd entered the right key
    injector.outputPanel.getCurrentOutputPanel()?.close()
    typeText(commandToKeys("p"))
    assertExOutput("    Lorem Ipsum")
  }

  @Test
  fun `test default range with P`() {
    configureByText(initialText)
    typeText(commandToKeys("P"))
    assertExOutput("    Lorem Ipsum")
  }

  @Test
  fun `test full text`() {
    configureByText(initialText)
    typeText(commandToKeys("%p"))
    assertExOutput(initialText)
  }

  @Test
  fun `test moves caret to start of last line of range skipping whitespace`() {
    doTest(
      exCommand("2,5p"),
      initialText,
      """
      |    Lorem Ipsum
      |
      |    Lorem ipsum dolor sit amet,
      |    consectetur adipiscing elit
      |    ${c}Sed in orci mauris.
      |    Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
    assertExOutput(
      """
      |
      |    Lorem ipsum dolor sit amet,
      |    consectetur adipiscing elit
      |    Sed in orci mauris.
      """.trimMargin(),
    )
  }

  @Test
  fun `test moves caret to start of last line of range skipping whitespace with number option`() {
    configureByText(initialText)
    val editor = fixture.editor.vim
    ApplicationManager.getApplication().invokeAndWait {
      injector.options(editor).number = true
    }
    typeText(commandToKeys("2,5p"))
    assertExOutput(
      """
      |2 
      |3     Lorem ipsum dolor sit amet,
      |4     consectetur adipiscing elit
      |5     Sed in orci mauris.
      """.trimMargin(),
    )
    ApplicationManager.getApplication().invokeAndWait {
      injector.options(editor).number = false
    }
  }

  @Test
  fun `test with count`() {
    doTest(
      exCommand("p3"),
      initialText,
      """
      |    Lorem Ipsum
      |
      |    ${c}Lorem ipsum dolor sit amet,
      |    consectetur adipiscing elit
      |    Sed in orci mauris.
      |    Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
    assertExOutput(
      """
      |    Lorem Ipsum
      |
      |    Lorem ipsum dolor sit amet,
      """.trimMargin(),
    )
  }

  @Test
  fun `test with invalid count`() {
    // Note that caret isn't moved
    doTest(
      exCommand("p3,4"),
      initialText,
      """
      |${c}    Lorem Ipsum
      |
      |    Lorem ipsum dolor sit amet,
      |    consectetur adipiscing elit
      |    Sed in orci mauris.
      |    Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("E488: Trailing characters: ,4")
  }

  @Test
  fun `test with range and count`() {
    doTest(
      exCommand("2,3p4"),
      initialText,
      """
      |    Lorem Ipsum
      |
      |    Lorem ipsum dolor sit amet,
      |    consectetur adipiscing elit
      |    Sed in orci mauris.
      |    ${c}Cras id tellus in ex imperdiet egestas.
      """.trimMargin()
    )
    assertExOutput(
      """
      |    Lorem ipsum dolor sit amet,
      |    consectetur adipiscing elit
      |    Sed in orci mauris.
      |    Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
  }

  companion object {
    private val initialText = """
       |    Lorem Ipsum
       |
       |    Lorem ipsum dolor sit amet,
       |    consectetur adipiscing elit
       |    Sed in orci mauris.
       |    Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
  }
}
