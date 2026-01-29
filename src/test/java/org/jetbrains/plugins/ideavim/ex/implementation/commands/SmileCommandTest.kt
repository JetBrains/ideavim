/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.ui.OutputPanel
import com.maddyhome.idea.vim.vimscript.model.commands.SmileCommand
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SmileCommandTest : VimTestCase() {

  private fun loadResourceContent(resourcePath: String): String {
    return SmileCommand::class.java.getResourceAsStream(resourcePath)
      ?.bufferedReader()
      ?.use { it.readText() }
      ?: throw IllegalStateException("Could not load resource: $resourcePath")
  }

  @Test
  fun `test smile command with default file`() {
    configureByText("\n")
    typeText(commandToKeys("smile"))

    val output: String = OutputPanel.getInstance(fixture.editor).text.trimEnd()
    val expectedContent: String = loadResourceContent(SmileCommand.DEFAULT_RESOURCE_PATH).trimEnd()

    assertEquals(expectedContent, output)
  }

  @Test
  fun `test smile command with kotlin file`() {
    configureByText("\n")
    configureByFileName("Test.kt")
    typeText(commandToKeys("smile"))

    val output = OutputPanel.getInstance(fixture.editor).text.trimEnd()
    val expectedContent = loadResourceContent(SmileCommand.KOTLIN_RESOURCE_PATH).trimEnd()

    assertEquals(expectedContent, output)
  }

  @Test
  fun `test smile command with kotlin script file`() {
    configureByText("\n")
    configureByFileName("Test.kts")
    typeText(commandToKeys("smile"))

    val output: String = OutputPanel.getInstance(fixture.editor).text.trimEnd()
    val expectedContent: String = loadResourceContent(SmileCommand.KOTLIN_RESOURCE_PATH).trimEnd()

    assertEquals(expectedContent, output)
  }

  @Test
  fun `test smile command with java file`() {
    configureByText("\n")
    configureByFileName("Test.java")
    typeText(commandToKeys("smile"))

    val output: String = OutputPanel.getInstance(fixture.editor).text.trimEnd()
    val expectedContent: String = loadResourceContent(SmileCommand.JAVA_RESOURCE_PATH).trimEnd()

    assertEquals(expectedContent, output)
  }

  @Test
  fun `test smile command with python file`() {
    configureByText("\n")
    configureByFileName("Test.py")
    typeText(commandToKeys("smile"))

    val output: String = OutputPanel.getInstance(fixture.editor).text.trimEnd()
    val expectedContent: String = loadResourceContent(SmileCommand.PYTHON_RESOURCE_PATH).trimEnd()

    assertEquals(expectedContent, output)
  }

  @Test
  fun `test smile command with unknown file extension`() {
    configureByText("\n")
    configureByFileName("Test.unknown")
    typeText(commandToKeys("smile"))

    val output: String = OutputPanel.getInstance(fixture.editor).text.trimEnd()
    val expectedContent: String = loadResourceContent(SmileCommand.DEFAULT_RESOURCE_PATH).trimEnd()

    assertEquals(expectedContent, output)
  }

  @Test
  fun `test smile command with gitignore file`() {
    configureByText("\n")
    configureByFileName(".gitignore")
    typeText(commandToKeys("smile"))

    val output: String = OutputPanel.getInstance(fixture.editor).text.trimEnd()
    val expectedContent: String = loadResourceContent(SmileCommand.DEFAULT_RESOURCE_PATH).trimEnd()

    assertEquals(expectedContent, output)
  }

}
