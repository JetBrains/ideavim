/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileTypes.FileType
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
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.ExOutputModel.Companion.getInstance
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import com.maddyhome.idea.vim.group.visual.VimVisualTimer.swingTimer
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.RunnableHelper.runWriteCommand
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.StringHelper.stringToKeys
import com.maddyhome.idea.vim.helper.TestInputModel
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.isBlockCaretShape
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.ToKeysMappingInfo
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.OptionsManager.getOption
import com.maddyhome.idea.vim.option.OptionsManager.ideastrictmode
import com.maddyhome.idea.vim.option.OptionsManager.resetAllOptions
import com.maddyhome.idea.vim.option.ToggleOption
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import org.junit.Assert
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
    // Note that myFixture.editor is usually null here. It's only set once configureByText has been called
    KeyHandler.getInstance().fullReset(myFixture.editor)
    resetAllOptions()
    VimPlugin.getKey().resetKeyMappings()
    VimPlugin.getSearch().resetState()
    if (!VimPlugin.isEnabled()) VimPlugin.setEnabled(true)
    ideastrictmode.set()
    Checks.reset()

    // Make sure the entry text field gets a bounds, or we won't be able to work out caret location
    ExEntryPanel.getInstance().entry.setBounds(0, 0, 100, 25)

    NeovimTesting.setUp(this)
  }

  private val testDataPath: String
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

  protected val screenWidth: Int
    get() = 80
  protected val screenHeight: Int
    get() = 35

  protected fun setEditorVisibleSize(width: Int, height: Int) {
    EditorTestUtil.setEditorVisibleSize(myFixture.editor, width, height)
  }

  protected fun configureByText(content: String) = configureByText(PlainTextFileType.INSTANCE, content)
  protected fun configureByJavaText(content: String) = configureByText(JavaFileType.INSTANCE, content)
  protected fun configureByXmlText(content: String) = configureByText(XmlFileType.INSTANCE, content)

  private fun configureByText(fileType: FileType, content: String): Editor {
    myFixture.configureByText(fileType, content)
    setEditorVisibleSize(screenWidth, screenHeight)
    return myFixture.editor
  }

  protected fun configureByFileName(fileName: String): Editor {
    myFixture.configureByText(fileName, "\n")
    setEditorVisibleSize(screenWidth, screenHeight)
    return myFixture.editor
  }

  @Suppress("SameParameterValue")
  protected fun configureByPages(pageCount: Int) {
    val stringBuilder = StringBuilder()
    repeat(pageCount * screenHeight) {
      stringBuilder.appendln("I found it in a legendary land")
    }
    configureByText(stringBuilder.toString())
  }

  protected fun configureByLines(lineCount: Int, line: String) {
    val stringBuilder = StringBuilder()
    repeat(lineCount) {
      stringBuilder.appendln(line)
    }
    configureByText(stringBuilder.toString())
  }

  protected fun configureByColumns(columnCount: Int) {
    val content = buildString {
      repeat(columnCount) {
        append('0' + (it % 10))
      }
    }
    configureByText(content)
  }

  @JvmOverloads
  protected fun setPositionAndScroll(scrollToLogicalLine: Int, caretLogicalLine: Int, caretLogicalColumn: Int = 0) {

    // Note that it is possible to request a position which would be invalid under normal Vim!
    // We disable scrolloff + scrolljump, position as requested, and reset. When resetting scrolloff, Vim will
    // recalculate the correct offsets, and that could move the top and/or caret line
    val scrolloff = OptionsManager.scrolloff.value()
    val scrolljump = OptionsManager.scrolljump.value()
    OptionsManager.scrolloff.set(0)
    OptionsManager.scrolljump.set(1)

    // Convert to visual lines to handle any collapsed folds
    val scrollToVisualLine = EditorHelper.logicalLineToVisualLine(myFixture.editor, scrollToLogicalLine)
    val bottomVisualLine = scrollToVisualLine + EditorHelper.getApproximateScreenHeight(myFixture.editor) - 1
    val bottomLogicalLine = EditorHelper.visualLineToLogicalLine(myFixture.editor, bottomVisualLine)

    // Make sure we're not trying to put caret in an invalid location
    val boundsTop = EditorHelper.visualLineToLogicalLine(myFixture.editor, scrollToVisualLine)
    val boundsBottom = EditorHelper.visualLineToLogicalLine(myFixture.editor, bottomVisualLine)
    Assert.assertTrue("Caret line $caretLogicalLine not inside legal screen bounds (${boundsTop} - ${boundsBottom})",
      caretLogicalLine in boundsTop..boundsBottom)

    typeText(parseKeys("${scrollToLogicalLine+1}z<CR>", "${caretLogicalLine+1}G", "${caretLogicalColumn+1}|"))

    OptionsManager.scrolljump.set(scrolljump)
    OptionsManager.scrolloff.set(scrolloff)

    // Make sure we're where we want to be
    assertVisibleArea(scrollToLogicalLine, bottomLogicalLine)
    assertPosition(caretLogicalLine, caretLogicalColumn)
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

  fun assertVisualPosition(visualLine: Int, visualColumn: Int) {
    val carets = myFixture.editor.caretModel.allCarets
    Assert.assertEquals("Wrong amount of carets", 1, carets.size)
    val actualPosition = carets[0].visualPosition
    Assert.assertEquals(VisualPosition(visualLine, visualColumn), actualPosition)
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

  fun assertOffsetAt(text: String) {
    val indexOf = myFixture.editor.document.charsSequence.indexOf(text)
    if (indexOf < 0) kotlin.test.fail()
    assertOffset(indexOf)
  }

  // Use logical rather than visual lines, so we can correctly test handling of collapsed folds and soft wraps
  fun assertVisibleArea(topLogicalLine: Int, bottomLogicalLine: Int) {
    val actualVisualTop = EditorHelper.getVisualLineAtTopOfScreen(myFixture.editor)
    val actualLogicalTop = EditorHelper.visualLineToLogicalLine(myFixture.editor, actualVisualTop)
    val actualVisualBottom = EditorHelper.getVisualLineAtBottomOfScreen(myFixture.editor)
    val actualLogicalBottom = EditorHelper.visualLineToLogicalLine(myFixture.editor, actualVisualBottom)

    Assert.assertEquals("Top logical lines don't match", topLogicalLine, actualLogicalTop)
    Assert.assertEquals("Bottom logical lines don't match", bottomLogicalLine, actualLogicalBottom)
  }

  fun assertVisibleLineBounds(logicalLine: Int, leftLogicalColumn: Int, rightLogicalColumn: Int) {
    val visualLine = EditorHelper.logicalLineToVisualLine(myFixture.editor, logicalLine)
    val actualLeftVisualColumn = EditorHelper.getVisualColumnAtLeftOfScreen(myFixture.editor, visualLine)
    val actualLeftLogicalColumn = myFixture.editor.visualToLogicalPosition(VisualPosition(visualLine, actualLeftVisualColumn)).column
    val actualRightVisualColumn = EditorHelper.getVisualColumnAtRightOfScreen(myFixture.editor, visualLine)
    val actualRightLogicalColumn =  myFixture.editor.visualToLogicalPosition(VisualPosition(visualLine, actualRightVisualColumn)).column

    val expected = ScreenBounds(leftLogicalColumn, rightLogicalColumn)
    val actual = ScreenBounds(actualLeftLogicalColumn, actualRightLogicalColumn)
    Assert.assertEquals(expected, actual)
  }

  fun putMapping(modes: Set<MappingMode>, from: String, to: String, recursive: Boolean) {
    VimPlugin.getKey().putKeyMapping(modes, parseKeys(from), MappingOwner.IdeaVim, parseKeys(to), recursive)
  }

  fun assertNoMapping(from: String) {
    val keys = parseKeys(from)
    for (mode in MappingMode.ALL) {
      assertNull(VimPlugin.getKey().getKeyMapping(mode).get(keys))
    }
  }

  fun assertNoMapping(from: String, modes: Set<MappingMode>) {
    val keys = parseKeys(from)
    for (mode in modes) {
      assertNull(VimPlugin.getKey().getKeyMapping(mode).get(keys))
    }
  }

  fun assertMappingExists(from: String, to: String, modes: Set<MappingMode>) {
    val keys = parseKeys(from)
    val toKeys = parseKeys(to)
    for (mode in modes) {
      val info = VimPlugin.getKey().getKeyMapping(mode).get(keys)
      kotlin.test.assertNotNull(info)
      if (info is ToKeysMappingInfo) {
        assertEquals(toKeys, info.toKeys)
      }
    }
  }

  private data class ScreenBounds(val leftLogicalColumn: Int, val rightLogicalColumn: Int) {
    override fun toString(): String {
      return "[$leftLogicalColumn-$rightLogicalColumn]"
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
    typeText(parseKeys(keys))
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
    if (Checks.caretShape) assertEquals(myFixture.editor.mode.isBlockCaretShape, myFixture.editor.settings.isBlockCursor)
  }

  protected val fileManager: FileEditorManagerEx
    get() = FileEditorManagerEx.getInstanceEx(myFixture.project)

  protected fun addInlay(offset: Int, relatesToPrecedingText: Boolean, widthInColumns: Int): Inlay<*> {
    // Enforce deterministic tests for inlays. Default text char width is different per platform (e.g. Windows is 7 and
    // Mac is 8) and using the same inlay width on all platforms can cause columns to be on or off screen unexpectedly.
    // If inlay width is related to character width, we will scale correctly across different platforms
    val columnWidth = EditorUtil.getPlainSpaceWidth(myFixture.editor)
    return EditorTestUtil.addInlay(myFixture.editor, offset, relatesToPrecedingText, widthInColumns * columnWidth)!!
  }

  // Disable or enable checks for the particular test
  protected inline fun setupChecks(setup: Checks.() -> Unit) {
    Checks.setup()
  }

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
          keyHandler.handleKey(editor, key, dataContext)
          key = inputModel.nextKeyStroke()
        }
      }, null, null)
    }

    @JvmStatic
    fun commandToKeys(command: String): List<KeyStroke> {
      val keys: MutableList<KeyStroke> = ArrayList()
      keys.addAll(parseKeys(":"))
      keys.addAll(stringToKeys(command))
      keys.addAll(parseKeys("<Enter>"))
      return keys
    }

    fun exCommand(command: String) = ":$command<Enter>"

    fun searchToKeys(pattern: String, forwards: Boolean): List<KeyStroke> {
      val keys: MutableList<KeyStroke> = ArrayList()
      keys.addAll(parseKeys(if (forwards) "/" else "?"))
      keys.addAll(stringToKeys(pattern))
      keys.addAll(parseKeys("<Enter>"))
      return keys
    }

    fun String.dotToTab(): String = replace('.', '\t')
  }

  object Checks {
    var caretShape: Boolean = true

    val neoVim = NeoVim()

    fun reset() {
      caretShape = true

      neoVim.reset()
    }

    class NeoVim {
      var ignoredRegisters: Set<Char> = setOf()

      fun reset() {
        ignoredRegisters = setOf()
      }
    }
  }
}
