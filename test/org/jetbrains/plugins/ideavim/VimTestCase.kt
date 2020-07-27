/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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
package org.jetbrains.plugins.ideavim

import com.intellij.ide.bookmarks.Bookmark
import com.intellij.ide.bookmarks.BookmarkManager
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.CommandState.SubMode
import com.maddyhome.idea.vim.ex.ExOutputModel.Companion.getInstance
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import com.maddyhome.idea.vim.group.visual.VimVisualTimer.swingTimer
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.RunnableHelper.runWriteCommand
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.StringHelper.stringToKeys
import com.maddyhome.idea.vim.helper.TestInputModel
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.option.OptionsManager.getOption
import com.maddyhome.idea.vim.option.OptionsManager.ideastrictmode
import com.maddyhome.idea.vim.option.OptionsManager.resetAllOptions
import com.maddyhome.idea.vim.option.ToggleOption
import com.maddyhome.idea.vim.ui.ExEntryPanel
import junit.framework.Assert
import java.util.*
import java.util.function.Consumer
import javax.swing.KeyStroke

/**
 * @author vlan
 */
abstract class VimTestCase : UsefulTestCase() {
  protected lateinit var myFixture: CodeInsightTestFixture

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
    val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor)
    val fixture = fixtureBuilder.fixture
    myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture,
      LightTempDirTestFixtureImpl(true))
    myFixture.setUp()
    myFixture.testDataPath = testDataPath
    KeyHandler.getInstance().fullReset(myFixture.editor)
    resetAllOptions()
    VimPlugin.getKey().resetKeyMappings()
    VimPlugin.getSearch().resetState()
    if (!VimPlugin.isEnabled()) VimPlugin.setEnabled(true)
    ideastrictmode.set()

    // Make sure the entry text field gets a bounds, or we won't be able to work out caret location
    ExEntryPanel.getInstance().entry.setBounds(0, 0, 100, 25)

    NeovimTesting.setUp(this)
  }

  protected val testDataPath: String
    get() = PathManager.getHomePath() + "/community/plugins/ideavim/testData"

  @Throws(Exception::class)
  override fun tearDown() {
    val swingTimer = swingTimer
    swingTimer?.stop()
    val bookmarkManager = BookmarkManager.getInstance(myFixture.project)
    bookmarkManager.validBookmarks.forEach(Consumer { bookmark: Bookmark? -> bookmarkManager.removeBookmark(bookmark!!) })
    SelectionVimListenerSuppressor.lock().use { myFixture.tearDown() }
    ExEntryPanel.getInstance().deactivate(false)
    VimScriptGlobalEnvironment.getInstance().variables.clear()
    VimPlugin.getRegister().resetRegisters()
    VimPlugin.getSearch().resetState()
    VimPlugin.getMark().resetAllMarks()

    // Tear down neovim
    NeovimTesting.tearDown(this)

    super.tearDown()
  }

  protected fun enableExtensions(vararg extensionNames: String) {
    for (name in extensionNames) {
      (getOption(name) as ToggleOption).set()
    }
  }

  protected fun typeTextInFile(keys: List<KeyStroke?>, fileContents: String): Editor {
    configureByText(fileContents)
    return typeText(keys)
  }

  protected fun configureByText(content: String): Editor {
    myFixture.configureByText(PlainTextFileType.INSTANCE, content)
    return myFixture.editor
  }

  protected fun configureByFileName(fileName: String): Editor {
    myFixture.configureByText(fileName, "\n")
    return myFixture.editor
  }

  protected fun configureByJavaText(content: String): Editor {
    myFixture.configureByText(JavaFileType.INSTANCE, content)
    return myFixture.editor
  }

  protected fun configureByXmlText(content: String): Editor {
    myFixture.configureByText(XmlFileType.INSTANCE, content)
    return myFixture.editor
  }

  protected fun typeText(keys: List<KeyStroke?>): Editor {
    val editor = myFixture.editor
    val project = myFixture.project
    typeText(keys, editor, project)
    return editor
  }

  protected fun enterCommand(command: String): Editor {
    return typeText(commandToKeys(command))
  }

  protected fun enterSearch(pattern: String, forwards: Boolean = true): Editor {
    return typeText(searchToKeys(pattern, forwards))
  }

  protected fun setText(text: String) {
    WriteAction.runAndWait<RuntimeException> {
      myFixture.editor.document.setText(text)
    }
  }

  fun assertPosition(line: Int, column: Int) {
    val carets = myFixture.editor.caretModel.allCarets
    Assert.assertEquals("Wrong amount of carets", 1, carets.size)
    val actualPosition = carets[0].logicalPosition
    Assert.assertEquals(LogicalPosition(line, column), actualPosition)
  }

  fun assertOffset(vararg expectedOffsets: Int) {
    val carets = myFixture.editor.caretModel.allCarets
    if (expectedOffsets.size == 2 && carets.size == 1) {
      Assert.assertEquals("Wrong amount of carets. Did you mean to use assertPosition?", expectedOffsets.size, carets.size)
    }
    Assert.assertEquals("Wrong amount of carets", expectedOffsets.size, carets.size)
    for (i in expectedOffsets.indices) {
      Assert.assertEquals(expectedOffsets[i], carets[i].offset)
    }
  }

  fun assertMode(expectedMode: CommandState.Mode) {
    val mode = CommandState.getInstance(myFixture.editor).mode
    Assert.assertEquals(expectedMode, mode)
  }

  fun assertSubMode(expectedSubMode: SubMode) {
    val subMode = CommandState.getInstance(myFixture.editor).subMode
    Assert.assertEquals(expectedSubMode, subMode)
  }

  fun assertSelection(expected: String?) {
    val selected = myFixture.editor.selectionModel.selectedText
    Assert.assertEquals(expected, selected)
  }

  fun assertExOutput(expected: String) {
    val actual = getInstance(myFixture.editor).text
    Assert.assertNotNull("No Ex output", actual)
    Assert.assertEquals(expected, actual)
  }

  fun assertPluginError(isError: Boolean) {
    Assert.assertEquals(isError, VimPlugin.isError())
  }

  fun assertPluginErrorMessageContains(message: String) {
    Assert.assertTrue(VimPlugin.getMessage().contains(message))
  }

  protected fun assertCaretsColour() {
    val selectionColour = myFixture.editor.colorsScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR)
    val caretColour = myFixture.editor.colorsScheme.getColor(EditorColors.CARET_COLOR)
    if (myFixture.editor.inBlockSubMode) {
      val caretModel = myFixture.editor.caretModel
      caretModel.allCarets.forEach { caret: Caret ->
        if (caret !== caretModel.primaryCaret) {
          Assert.assertEquals(selectionColour, caret.visualAttributes.color)
        } else {
          val color = caret.visualAttributes.color
          if (color != null) Assert.assertEquals(caretColour, color)
        }
      }
    } else {
      myFixture.editor.caretModel.allCarets.forEach { caret: Caret ->
        val color = caret.visualAttributes.color
        if (color != null) Assert.assertEquals(caretColour, color)
      }
    }
  }

  fun doTest(keys: List<String>,
             before: String,
             after: String,
             modeAfter: CommandState.Mode,
             subModeAfter: SubMode) {
    doTest(keys.joinToString(separator = ""), before, after, modeAfter, subModeAfter)
  }

  fun doTest(keys: String,
             before: String,
             after: String,
             modeAfter: CommandState.Mode,
             subModeAfter: SubMode) {
    configureByText(before)

    NeovimTesting.setupEditor(myFixture.editor, this)
    NeovimTesting.typeCommand(keys, this)

    performTest(keys, after, modeAfter, subModeAfter)

    NeovimTesting.assertState(myFixture.editor, this)
  }

  private fun performTest(keys: String, after: String, modeAfter: CommandState.Mode, subModeAfter: SubMode) {
    typeText(StringHelper.parseKeys(keys))
    myFixture.checkResult(after)
    assertState(modeAfter, subModeAfter)
  }

  fun doTestWithoutNeovim(keys: List<KeyStroke>,
                          before: String,
                          after: String?,
                          modeAfter: CommandState.Mode, subModeAfter: SubMode,
                          afterEditorInitialized: (Editor) -> Unit) {
    configureByText(before)
    afterEditorInitialized(myFixture.editor)
    typeText(keys)
    myFixture.checkResult(after!!)
    assertState(modeAfter, subModeAfter)
  }

  protected fun setRegister(register: Char, keys: String) {
    VimPlugin.getRegister().setKeys(register, stringToKeys(keys))
    NeovimTesting.setRegister(register, keys, this)
  }

  protected fun assertState(modeAfter: CommandState.Mode, subModeAfter: SubMode) {
    assertCaretsColour()
    assertMode(modeAfter)
    assertSubMode(subModeAfter)
  }

  protected val fileManager: FileEditorManagerEx
    get() = FileEditorManagerEx.getInstanceEx(myFixture.project)

  companion object {
    const val c = EditorTestUtil.CARET_TAG
    const val s = EditorTestUtil.SELECTION_START_TAG
    const val se = EditorTestUtil.SELECTION_END_TAG

    fun typeText(keys: List<KeyStroke?>, editor: Editor, project: Project?) {
      val keyHandler = KeyHandler.getInstance()
      val dataContext = EditorDataContext(editor)
      TestInputModel.getInstance(editor).setKeyStrokes(keys)
      runWriteCommand(project, Runnable {
        val inputModel = TestInputModel.getInstance(editor)
        var key = inputModel.nextKeyStroke()
        while (key != null) {
          val exEntryPanel = ExEntryPanel.getInstance()
          if (exEntryPanel.isActive) {
            exEntryPanel.handleKey(key)
          } else {
            keyHandler.handleKey(editor, key, dataContext)
          }
          key = inputModel.nextKeyStroke()
        }
      }, null, null)
    }

    @JvmStatic
    fun commandToKeys(command: String): List<KeyStroke> {
      val keys: MutableList<KeyStroke> = ArrayList()
      keys.addAll(StringHelper.parseKeys(":"))
      keys.addAll(stringToKeys(command))
      keys.addAll(StringHelper.parseKeys("<Enter>"))
      return keys
    }

    fun exCommand(command: String) = ":$command<Enter>"

    fun searchToKeys(pattern: String, forwards: Boolean): List<KeyStroke> {
      val keys: MutableList<KeyStroke> = ArrayList()
      keys.addAll(StringHelper.parseKeys(if (forwards) "/" else "?"))
      keys.addAll(stringToKeys(pattern))
      keys.addAll(StringHelper.parseKeys("<Enter>"))
      return keys
    }

    fun String.dotToTab(): String = replace('.', '\t')
  }
}
