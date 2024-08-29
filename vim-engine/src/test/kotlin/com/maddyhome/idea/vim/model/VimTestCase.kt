/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.model

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.visualLineToBufferLine
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.state.mode.Mode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import javax.swing.KeyStroke
import kotlin.test.assertNull
import kotlin.test.assertContains

interface VimTestCase {
  val editor: VimEditor

  fun <T> assertEmpty(collection: Collection<T>) {
    assertTrue(collection.isEmpty(), "Collection should be empty, but it contains ${collection.size} elements")
  }

  fun typeTextInFile(keys: List<KeyStroke?>, fileContents: String): VimEditor {
    configureByText(fileContents)
    return typeText(keys)
  }

  fun typeTextInFile(keys: String, fileContents: String): VimEditor {
    configureByText(fileContents)
    return typeText(keys)
  }

  fun configureByText(content: String) = configureByText(FileType.PLAIN_TEXT, content)
  fun configureByXmlText(content: String) = configureByText(FileType.XML, content)
  fun configureByJsonText(@Suppress("SameParameterValue") content: String) = configureByText(FileType.JSON, content)

  fun configureByText(fileType: FileType, content: String): VimEditor

  fun typeText(vararg keys: String) = typeText(keys.flatMap { injector.parser.parseKeys(it) })

  fun typeText(keys: List<KeyStroke?>): VimEditor {
    return typeText(editor, keys)
  }

  fun typeText(editor: VimEditor, keys: List<KeyStroke?>): VimEditor

  fun enterCommand(command: String): VimEditor {
    return typeText(commandToKeys(command))
  }

  fun enterSearch(pattern: String, forwards: Boolean = true): VimEditor {
    return typeText(searchToKeys(pattern, forwards))
  }

  fun assertState(textAfter: String)

  fun assertRegister(char: Char, expected: String?) {
    val actual = injector.registerGroup.getRegister(char)?.keys?.let(injector.parser::toKeyNotation)
    assertEquals(expected, actual, "Wrong register contents")
  }

  fun assertRegisterString(char: Char, expected: String?) {
    val actual = injector.registerGroup.getRegister(char)?.keys?.let(injector.parser::toPrintableString)
    assertEquals(expected, actual, "Wrong register contents")
  }

  fun assertState(modeAfter: Mode) {
    assertMode(modeAfter)
    assertCaretsVisualAttributes()
  }

  fun assertPosition(line: Int, column: Int) {
    val carets = editor.carets()
    assertEquals(1, carets.size, "Wrong amount of carets")
    val actualPosition = carets[0].getBufferPosition()
    assertEquals(BufferPosition(line, column), actualPosition)
  }

  fun assertVisualPosition(visualLine: Int, visualColumn: Int) {
    val carets = editor.carets()
    assertEquals(1, carets.size, "Wrong amount of carets")
    val actualPosition = carets[0].getVisualPosition()
    assertEquals(VimVisualPosition(visualLine, visualColumn), actualPosition)
  }

  fun assertOffset(vararg expectedOffsets: Int) {
    val carets = editor.carets().sortedBy { it.offset }
    if (expectedOffsets.size == 2 && carets.size == 1) {
      assertEquals(
        expectedOffsets.size,
        carets.size,
        "Wrong amount of carets. Did you mean to use assertPosition?",
      )
    }
    assertEquals(expectedOffsets.size, carets.size, "Wrong amount of carets")
    for (i in expectedOffsets.indices) {
      assertEquals(expectedOffsets[i], carets[i].offset)
    }
  }

  fun assertOffsetAt(text: String) {
    val indexOf = editor.text().indexOf(text)
    if (indexOf < 0) kotlin.test.fail()
    assertOffset(indexOf)
  }

  // Use logical rather than visual lines, so we can correctly test handling of collapsed folds and soft wraps
  fun assertVisibleArea(topLogicalLine: Int, bottomLogicalLine: Int) {
    assertTopLogicalLine(topLogicalLine)
    assertBottomLogicalLine(bottomLogicalLine)
  }

  fun assertTopLogicalLine(topLogicalLine: Int) {
    val actualVisualTop = injector.engineEditorHelper.getVisualLineAtTopOfScreen(editor)
    val actualLogicalTop = editor.visualLineToBufferLine(actualVisualTop)

    assertEquals(topLogicalLine, actualLogicalTop, "Top logical lines don't match")
  }

  fun assertBottomLogicalLine(bottomLogicalLine: Int) {
    val actualVisualBottom = injector.engineEditorHelper.getVisualLineAtBottomOfScreen(editor)
    val actualLogicalBottom = editor.visualLineToBufferLine(actualVisualBottom)

    assertEquals(bottomLogicalLine, actualLogicalBottom, "Bottom logical lines don't match")
  }

  fun assertMode(expectedMode: Mode) {
    assertEquals(expectedMode, editor.mode)
  }

  fun assertCommandOutput(command: String, expected: String) {
    enterCommand(command)
    assertExOutput(expected)
  }

  fun assertExOutput(expected: String) {
    val outputPanel = injector.outputPanel.getCurrentOutputPanel()
    assertNotNull(outputPanel, "No output panel")
    val actual = outputPanel!!.text
    assertEquals(expected, actual)
  }

  fun assertNoExOutput() {
    val outputPanel = injector.outputPanel.getCurrentOutputPanel()
    assertNull(outputPanel)
  }

  fun assertPluginError(isError: Boolean) {
    assertEquals(isError, injector.messages.isError())
  }

  fun assertPluginErrorMessageContains(message: String) {
    assertContains(injector.messages.getStatusBarMessage()!!, message)
  }

  fun assertStatusLineMessageContains(message: String) {
    assertContains(injector.messages.getStatusBarMessage()!!, message)
  }

  fun assertStatusLineCleared() {
    assertNull(injector.messages.getStatusBarMessage())
  }

  fun assertCaretsVisualAttributes()

  fun performTest(keys: String, after: String, modeAfter: Mode) {
    typeText(keys)
    assertState(after)
    assertState(modeAfter)
  }

  fun setRegister(register: Char, keys: String) {
    injector.registerGroup.setKeys(register, injector.parser.stringToKeys(keys))
  }

  fun assertExException(expectedErrorMessage: String, action: () -> Unit) {
    val exception = assertThrows<ExException> { action() }
    assertEquals(expectedErrorMessage, exception.message)
  }

  fun exCommand(command: String) = ":$command<CR>"

  fun commandToKeys(command: String): List<KeyStroke> {
    val keys: MutableList<KeyStroke> = ArrayList()
    if (!command.startsWith(":")) {
      keys.addAll(injector.parser.parseKeys(":"))
    }
    keys.addAll(injector.parser.stringToKeys(command)) // Avoids trying to parse 'command ... <args>' as a special char
    keys.addAll(injector.parser.parseKeys("<Enter>"))
    return keys
  }

  fun searchToKeys(pattern: String, forwards: Boolean): List<KeyStroke> {
    val keys: MutableList<KeyStroke> = ArrayList()
    keys.addAll(injector.parser.parseKeys(if (forwards) "/" else "?"))
    keys.addAll(injector.parser.stringToKeys(pattern)) // Avoids trying to parse 'command ... <args>' as a special char
    keys.addAll(injector.parser.parseKeys("<CR>"))
    return keys
  }

  fun searchCommand(pattern: String) = "$pattern<CR>"

  fun String.dotToTab(): String = replace('.', '\t')

  fun String.dotToSpace(): String = replace('.', ' ')
}