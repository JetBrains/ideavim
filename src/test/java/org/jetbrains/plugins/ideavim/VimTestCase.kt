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
package org.jetbrains.plugins.ideavim

import com.intellij.ide.bookmark.BookmarksManager
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.json.JsonFileType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.CaretVisualAttributes
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimShortcutKeyAction
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.command.VimStateMachine.SubMode
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ExOutputModel.Companion.getInstance
import com.maddyhome.idea.vim.group.visual.VimVisualTimer.swingTimer
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.GuicursorChangeListener
import com.maddyhome.idea.vim.helper.RunnableHelper.runWriteCommand
import com.maddyhome.idea.vim.helper.TestInputModel
import com.maddyhome.idea.vim.helper.editorMode
import com.maddyhome.idea.vim.helper.getGuiCursorMode
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.subMode
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.ToKeysMappingInfo
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.helpers.GuiCursorOptionHelper
import com.maddyhome.idea.vim.options.helpers.GuiCursorType
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.parser.errors.IdeavimErrorListener
import org.assertj.core.api.Assertions
import org.junit.Assert
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke
import kotlin.math.roundToInt

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
    myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
      fixture,
      LightTempDirTestFixtureImpl(true)
    )
    myFixture.setUp()
    myFixture.testDataPath = testDataPath
    // Note that myFixture.editor is usually null here. It's only set once configureByText has been called
    val editor = myFixture.editor
    if (editor != null) {
      KeyHandler.getInstance().fullReset(editor.vim)
    }
    VimPlugin.getOptionService().resetAllOptions()
    VimPlugin.getKey().resetKeyMappings()
    VimPlugin.getSearch().resetState()
    if (!VimPlugin.isEnabled()) VimPlugin.setEnabled(true)
    VimPlugin.getOptionService().setOption(OptionScope.GLOBAL, OptionConstants.ideastrictmodeName)
    GuicursorChangeListener.processGlobalValueChange(null)
    Checks.reset()

    // Make sure the entry text field gets a bounds, or we won't be able to work out caret location
    ExEntryPanel.getInstance().entry.setBounds(0, 0, 100, 25)

    NeovimTesting.setUp(this)

    VimPlugin.clearError()
  }

  private val testDataPath: String
    get() = PathManager.getHomePath() + "/community/plugins/ideavim/testData"

  @Throws(Exception::class)
  override fun tearDown() {
    val swingTimer = swingTimer
    swingTimer?.stop()
    val bookmarksManager = BookmarksManager.getInstance(myFixture.project)
    bookmarksManager?.bookmarks?.forEach { bookmark ->
      bookmarksManager.remove(bookmark)
    }
    SelectionVimListenerSuppressor.lock().use { myFixture.tearDown() }
    ExEntryPanel.getInstance().deactivate(false)
    VimPlugin.getVariableService().clear()
    VimFuncref.lambdaCounter = 0
    VimFuncref.anonymousCounter = 0
    IdeavimErrorListener.testLogger.clear()
    VimPlugin.getRegister().resetRegisters()
    VimPlugin.getSearch().resetState()
    VimPlugin.getMark().resetAllMarks()
    VimPlugin.getChange().resetRepeat()
    VimPlugin.getKey().savedShortcutConflicts.clear()

    // Tear down neovim
    NeovimTesting.tearDown(this)

    super.tearDown()
  }

  protected fun enableExtensions(vararg extensionNames: String) {
    for (name in extensionNames) {
      VimPlugin.getOptionService().setOption(OptionScope.GLOBAL, name)
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
    val w = (width * EditorHelper.getPlainSpaceWidthFloat(myFixture.editor)).roundToInt()
    val h = height * myFixture.editor.lineHeight
    EditorTestUtil.setEditorVisibleSizeInPixels(myFixture.editor, w, h)
  }

  protected fun setEditorVirtualSpace() {
    // Enable virtual space at the bottom of the file and force a layout to pick up the changes
    myFixture.editor.settings.isAdditionalPageAtBottom = true
    (myFixture.editor as EditorEx).scrollPane.viewport.doLayout()
  }

  protected fun configureByText(content: String) = configureByText(PlainTextFileType.INSTANCE, content)
  protected fun configureByJavaText(content: String) = configureByText(JavaFileType.INSTANCE, content)
  protected fun configureByXmlText(content: String) = configureByText(XmlFileType.INSTANCE, content)
  protected fun configureByJsonText(content: String) = configureByText(JsonFileType.INSTANCE, content)

  protected fun configureAndGuard(content: String) {
    val ranges = extractBrackets(content)
    for ((start, end) in ranges) {
      myFixture.editor.document.createGuardedBlock(start, end)
    }
  }

  protected fun configureAndFold(content: String, placeholder: String) {
    val ranges = extractBrackets(content)
    myFixture.editor.foldingModel.runBatchFoldingOperation {
      for ((start, end) in ranges) {
        val foldRegion = myFixture.editor.foldingModel.addFoldRegion(start, end, placeholder)
        foldRegion?.isExpanded = false
      }
    }
  }

  private fun extractBrackets(content: String): ArrayList<Pair<Int, Int>> {
    var myContent = content.replace(c, "").replace(s, "").replace(se, "")
    val ranges = ArrayList<Pair<Int, Int>>()
    while (true) {
      val start = myContent.indexOfFirst { it == '[' }
      if (start < 0) break
      myContent = myContent.removeRange(start, start + 1)
      val end = myContent.indexOfFirst { it == ']' }
      if (end < 0) break
      myContent = myContent.removeRange(end, end + 1)
      ranges.add(start to end)
    }
    configureByText(content.replace("[", "").replace("]", ""))
    return ranges
  }

  private fun configureByText(fileType: FileType, content: String): Editor {
    @Suppress("IdeaVimAssertState")
    myFixture.configureByText(fileType, content)
    NeovimTesting.setupEditor(myFixture.editor, this)
    setEditorVisibleSize(screenWidth, screenHeight)
    return myFixture.editor
  }

  private fun configureByText(fileName: String, content: String): Editor {
    @Suppress("IdeaVimAssertState")
    myFixture.configureByText(fileName, content)
    NeovimTesting.setupEditor(myFixture.editor, this)
    setEditorVisibleSize(screenWidth, screenHeight)
    return myFixture.editor
  }

  protected fun configureByFileName(fileName: String): Editor {
    @Suppress("IdeaVimAssertState")
    myFixture.configureByText(fileName, "\n")
    NeovimTesting.setupEditor(myFixture.editor, this)
    setEditorVisibleSize(screenWidth, screenHeight)
    return myFixture.editor
  }

  @Suppress("SameParameterValue")
  protected fun configureByPages(pageCount: Int) {
    val stringBuilder = StringBuilder()
    repeat(pageCount * screenHeight) {
      stringBuilder.appendLine("I found it in a legendary land")
    }
    configureByText(stringBuilder.toString())
  }

  protected fun configureByLines(lineCount: Int, line: String) {
    val stringBuilder = StringBuilder()
    repeat(lineCount - 1) {
      stringBuilder.appendLine(line)
    }
    stringBuilder.append(line)
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
    val scrolloff = (VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName) as VimInt).value
    val scrolljump = (VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName) as VimInt).value
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(0))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(1))

    typeText(injector.parser.parseKeys("${scrollToLogicalLine + 1}z<CR>" + "${caretLogicalLine + 1}G" + "${caretLogicalColumn + 1}|"))

    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(scrolloff))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(scrolljump))

    // Make sure we're where we want to be. If there are block inlays, we can't easily assert the bottom line because
    // we'd have to duplicate the scrolling logic here. Asserting top when we know height is good enough
    assertTopLogicalLine(scrollToLogicalLine)
    assertPosition(caretLogicalLine, caretLogicalColumn)

    // Belt and braces. Let's make sure that the caret is fully onscreen
    val bottomLogicalLine = EditorHelper.visualLineToLogicalLine(
      myFixture.editor,
      EditorHelper.getVisualLineAtBottomOfScreen(myFixture.editor)
    )
    assertTrue(bottomLogicalLine >= caretLogicalLine)
    assertTrue(caretLogicalLine >= scrollToLogicalLine)
  }

  protected fun typeText(keys: List<KeyStroke?>): Editor {
    val editor = myFixture.editor
    NeovimTesting.typeCommand(
      keys.filterNotNull().joinToString(separator = "") { injector.parser.toKeyNotation(it) },
      this,
      editor
    )
    val project = myFixture.project
    when (Checks.keyHandler) {
      Checks.KeyHandlerMethod.DIRECT_TO_VIM -> typeText(keys, editor, project)
      Checks.KeyHandlerMethod.VIA_IDE -> typeTextViaIde(keys, editor)
    }
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

  fun assertState(textAfter: String) {
    @Suppress("IdeaVimAssertState")
    myFixture.checkResult(textAfter)
    NeovimTesting.assertState(myFixture.editor, this)
  }

  protected fun assertState(modeAfter: VimStateMachine.Mode, subModeAfter: SubMode) {
    assertMode(modeAfter)
    assertSubMode(subModeAfter)
    assertCaretsVisualAttributes()
  }

  fun assertPosition(line: Int, column: Int) {
    val carets = myFixture.editor.caretModel.allCarets
    Assert.assertEquals("Wrong amount of carets", 1, carets.size)
    val actualPosition = carets[0].logicalPosition
    Assert.assertEquals(LogicalPosition(line, column), actualPosition)
    NeovimTesting.assertCaret(myFixture.editor, this)
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
      Assert.assertEquals(
        "Wrong amount of carets. Did you mean to use assertPosition?",
        expectedOffsets.size,
        carets.size
      )
    }
    Assert.assertEquals("Wrong amount of carets", expectedOffsets.size, carets.size)
    for (i in expectedOffsets.indices) {
      Assert.assertEquals(expectedOffsets[i], carets[i].offset)
    }

    NeovimTesting.assertState(myFixture.editor, this)
  }

  fun assertOffsetAt(text: String) {
    val indexOf = myFixture.editor.document.charsSequence.indexOf(text)
    if (indexOf < 0) kotlin.test.fail()
    assertOffset(indexOf)
  }

  // Use logical rather than visual lines, so we can correctly test handling of collapsed folds and soft wraps
  fun assertVisibleArea(topLogicalLine: Int, bottomLogicalLine: Int) {
    assertTopLogicalLine(topLogicalLine)
    assertBottomLogicalLine(bottomLogicalLine)
  }

  fun assertTopLogicalLine(topLogicalLine: Int) {
    val actualVisualTop = EditorHelper.getVisualLineAtTopOfScreen(myFixture.editor)
    val actualLogicalTop = EditorHelper.visualLineToLogicalLine(myFixture.editor, actualVisualTop)

    Assert.assertEquals("Top logical lines don't match", topLogicalLine, actualLogicalTop)
  }

  fun assertBottomLogicalLine(bottomLogicalLine: Int) {
    val actualVisualBottom = EditorHelper.getVisualLineAtBottomOfScreen(myFixture.editor)
    val actualLogicalBottom = EditorHelper.visualLineToLogicalLine(myFixture.editor, actualVisualBottom)

    Assert.assertEquals("Bottom logical lines don't match", bottomLogicalLine, actualLogicalBottom)
  }

  fun assertVisibleLineBounds(logicalLine: Int, leftLogicalColumn: Int, rightLogicalColumn: Int) {
    val visualLine = EditorHelper.logicalLineToVisualLine(myFixture.editor, logicalLine)
    val actualLeftVisualColumn = EditorHelper.getVisualColumnAtLeftOfScreen(myFixture.editor, visualLine)
    val actualLeftLogicalColumn =
      myFixture.editor.visualToLogicalPosition(VisualPosition(visualLine, actualLeftVisualColumn)).column
    val actualRightVisualColumn = EditorHelper.getVisualColumnAtRightOfScreen(myFixture.editor, visualLine)
    val actualRightLogicalColumn =
      myFixture.editor.visualToLogicalPosition(VisualPosition(visualLine, actualRightVisualColumn)).column

    val expected = ScreenBounds(leftLogicalColumn, rightLogicalColumn)
    val actual = ScreenBounds(actualLeftLogicalColumn, actualRightLogicalColumn)
    Assert.assertEquals(expected, actual)
  }

  fun assertLineCount(expected: Int) {
    assertEquals(expected, EditorHelper.getLineCount(myFixture.editor))
  }

  fun putMapping(modes: Set<MappingMode>, from: String, to: String, recursive: Boolean) {
    VimPlugin.getKey().putKeyMapping(modes, injector.parser.parseKeys(from), MappingOwner.IdeaVim.System, injector.parser.parseKeys(to), recursive)
  }

  fun assertNoMapping(from: String) {
    val keys = injector.parser.parseKeys(from)
    for (mode in MappingMode.ALL) {
      assertNull(VimPlugin.getKey().getKeyMapping(mode).get(keys))
    }
  }

  fun assertNoMapping(from: String, modes: Set<MappingMode>) {
    val keys = injector.parser.parseKeys(from)
    for (mode in modes) {
      assertNull(VimPlugin.getKey().getKeyMapping(mode).get(keys))
    }
  }

  fun assertMappingExists(from: String, to: String, modes: Set<MappingMode>) {
    val keys = injector.parser.parseKeys(from)
    val toKeys = injector.parser.parseKeys(to)
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

  fun assertMode(expectedMode: VimStateMachine.Mode) {
    val mode = myFixture.editor.editorMode
    Assert.assertEquals(expectedMode, mode)
  }

  fun assertSubMode(expectedSubMode: SubMode) {
    val subMode = myFixture.editor.subMode
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
    NeovimTesting.typeCommand("<esc>", this, myFixture.editor)
  }

  fun assertNoExOutput() {
    val actual = getInstance(myFixture.editor).text
    Assert.assertNull("Ex output not null", actual)
  }

  fun assertPluginError(isError: Boolean) {
    Assert.assertEquals(isError, VimPlugin.isError())
  }

  fun assertPluginErrorMessageContains(message: String) {
    Assertions.assertThat(VimPlugin.getMessage()).contains(message)
  }

  protected fun assertCaretsVisualAttributes() {
    if (!Checks.caretShape) return
    val editor = myFixture.editor
    val attributes = GuiCursorOptionHelper.getAttributes(getGuiCursorMode(editor))
    val colour = editor.colorsScheme.getColor(EditorColors.CARET_COLOR)

    editor.caretModel.allCarets.forEach { caret ->
      // All carets should be the same except when in block sub mode, where we "hide" them (by drawing a zero width bar)
      if (caret !== editor.caretModel.primaryCaret && editor.inBlockSubMode) {
        assertEquals(CaretVisualAttributes.Shape.BAR, caret.visualAttributes.shape)
        assertEquals(0F, caret.visualAttributes.thickness)
      } else {
        val shape = when (attributes.type) {
          GuiCursorType.BLOCK -> CaretVisualAttributes.Shape.BLOCK
          GuiCursorType.VER -> CaretVisualAttributes.Shape.BAR
          GuiCursorType.HOR -> CaretVisualAttributes.Shape.UNDERSCORE
        }
        assertEquals(shape, editor.caretModel.primaryCaret.visualAttributes.shape)
        assertEquals(attributes.thickness / 100.0F, editor.caretModel.primaryCaret.visualAttributes.thickness)
        editor.caretModel.primaryCaret.visualAttributes.color?.let {
          assertEquals(colour, it)
        }
      }
    }
  }

  @JvmOverloads
  fun doTest(
    keys: List<String>,
    before: String,
    after: String,
    modeAfter: VimStateMachine.Mode = VimStateMachine.Mode.COMMAND,
    subModeAfter: SubMode = SubMode.NONE,
    fileType: FileType? = null,
    fileName: String? = null,
    afterEditorInitialized: ((Editor) -> Unit)? = null,
  ) {
    doTest(keys.joinToString(separator = ""), before, after, modeAfter, subModeAfter, fileType, fileName, afterEditorInitialized)
  }

  @JvmOverloads
  fun doTest(
    keys: String,
    before: String,
    after: String,
    modeAfter: VimStateMachine.Mode = VimStateMachine.Mode.COMMAND,
    subModeAfter: SubMode = SubMode.NONE,
    fileType: FileType? = null,
    fileName: String? = null,
    afterEditorInitialized: ((Editor) -> Unit)? = null,
  ) {
    if (fileName != null) {
      configureByText(fileName, before)
    } else if (fileType != null) {
      configureByText(fileType, before)
    } else {
      configureByText(before)
    }
    afterEditorInitialized?.invoke(myFixture.editor)
    performTest(keys, after, modeAfter, subModeAfter)
  }

  protected fun performTest(keys: String, after: String, modeAfter: VimStateMachine.Mode, subModeAfter: SubMode) {
    typeText(injector.parser.parseKeys(keys))
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    assertState(after)
    assertState(modeAfter, subModeAfter)
  }

  protected fun setRegister(register: Char, keys: String) {
    VimPlugin.getRegister().setKeys(register, injector.parser.stringToKeys(keys))
    NeovimTesting.setRegister(register, keys, this)
  }

  protected val fileManager: FileEditorManagerEx
    get() = FileEditorManagerEx.getInstanceEx(myFixture.project)

  // Specify width in columns, not pixels, just like we do for visible screen size. The default text char width differs
  // per platform (e.g. Windows is 7, Mac is 8) so we can't guarantee correct positioning for tests if we use hard coded
  // pixel widths
  protected fun addInlay(offset: Int, relatesToPrecedingText: Boolean, widthInColumns: Int): Inlay<*> {
    val widthInPixels = (EditorHelper.getPlainSpaceWidthFloat(myFixture.editor) * widthInColumns).roundToInt()
    return EditorTestUtil.addInlay(myFixture.editor, offset, relatesToPrecedingText, widthInPixels)
  }

  // As for inline inlays, height is specified as a multiplier of line height, as we can't guarantee the same line
  // height on all platforms, so can't guarantee correct positioning for tests if we use pixels. This currently limits
  // us to integer multiples of line heights. I don't think this will cause any issues, but we can change this to a
  // float if necessary. We'd still be working scaled to the line height, so fractional values should still work.
  protected fun addBlockInlay(offset: Int, showAbove: Boolean, heightInRows: Int): Inlay<*> {
    val widthInColumns = 10 // Arbitrary width. We don't care.
    val widthInPixels = (EditorHelper.getPlainSpaceWidthFloat(myFixture.editor) * widthInColumns).roundToInt()
    val heightInPixels = myFixture.editor.lineHeight * heightInRows
    return EditorTestUtil.addBlockInlay(myFixture.editor, offset, false, showAbove, widthInPixels, heightInPixels)
  }

  // Disable or enable checks for the particular test
  protected inline fun setupChecks(setup: Checks.() -> Unit) {
    Checks.setup()
  }

  protected fun assertExException(expectedErrorMessage: String, action: () -> Unit) {
    assertThrows(ExException::class.java, expectedErrorMessage, action)
  }

  private fun typeTextViaIde(keys: List<KeyStroke?>, editor: Editor) {
    TestInputModel.getInstance(editor).setKeyStrokes(keys)

    val inputModel = TestInputModel.getInstance(editor)
    var key = inputModel.nextKeyStroke()
    while (key != null) {
      val keyChar = key.keyChar
      if (keyChar != KeyEvent.CHAR_UNDEFINED) {
        myFixture.type(keyChar)
      } else {
        val event =
          KeyEvent(editor.component, KeyEvent.KEY_PRESSED, Date().time, key.modifiers, key.keyCode, key.keyChar)

        val e = AnActionEvent(
          event,
          EditorDataContext.init(editor),
          ActionPlaces.KEYBOARD_SHORTCUT,
          VimShortcutKeyAction.instance.templatePresentation.clone(),
          ActionManager.getInstance(),
          0
        )
        if (ActionUtil.lastUpdateAndCheckDumb(VimShortcutKeyAction.instance, e, true)) {
          ActionUtil.performActionDumbAwareWithCallbacks(VimShortcutKeyAction.instance, e)
        }
      }
      key = inputModel.nextKeyStroke()
    }
  }

  companion object {
    const val c = EditorTestUtil.CARET_TAG
    const val s = EditorTestUtil.SELECTION_START_TAG
    const val se = EditorTestUtil.SELECTION_END_TAG

    fun typeText(keys: List<KeyStroke?>, editor: Editor, project: Project?) {
      val keyHandler = KeyHandler.getInstance()
      val dataContext = EditorDataContext.init(editor)
      TestInputModel.getInstance(editor).setKeyStrokes(keys)
      runWriteCommand(
        project,
        Runnable {
          val inputModel = TestInputModel.getInstance(editor)
          var key = inputModel.nextKeyStroke()
          while (key != null) {
            keyHandler.handleKey(editor.vim, key, dataContext.vim)
            key = inputModel.nextKeyStroke()
          }
        },
        null, null
      )
    }

    @JvmStatic
    fun commandToKeys(command: String): List<KeyStroke> {
      val keys: MutableList<KeyStroke> = ArrayList()
      keys.addAll(injector.parser.parseKeys(":"))
      keys.addAll(injector.parser.stringToKeys(command))
      keys.addAll(injector.parser.parseKeys("<Enter>"))
      return keys
    }

    fun exCommand(command: String) = ":$command<CR>"

    fun searchToKeys(pattern: String, forwards: Boolean): List<KeyStroke> {
      val keys: MutableList<KeyStroke> = ArrayList()
      keys.addAll(injector.parser.parseKeys(if (forwards) "/" else "?"))
      keys.addAll(injector.parser.stringToKeys(pattern))
      keys.addAll(injector.parser.parseKeys("<CR>"))
      return keys
    }

    fun searchCommand(pattern: String) = "$pattern<CR>"

    fun String.dotToTab(): String = replace('.', '\t')

    fun String.dotToSpace(): String = replace('.', ' ')
  }

  object Checks {
    var caretShape: Boolean = true

    val neoVim = NeoVim()

    var keyHandler = KeyHandlerMethod.VIA_IDE

    fun reset() {
      caretShape = true

      neoVim.reset()
      keyHandler = KeyHandlerMethod.VIA_IDE
    }

    class NeoVim {
      var ignoredRegisters: Set<Char> = setOf()
      var exitOnTearDown = true

      fun reset() {
        ignoredRegisters = setOf()
        exitOnTearDown = true
      }
    }

    enum class KeyHandlerMethod {
      VIA_IDE,
      DIRECT_TO_VIM,
    }
  }
}
