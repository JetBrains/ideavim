/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim

import com.intellij.application.options.CodeStyle
import com.intellij.ide.ClipboardSynchronizer
import com.intellij.ide.bookmark.BookmarksManager
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.CaretVisualAttributes
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorSettings
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.intellij.util.ui.EmptyClipboardOwner
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimShortcutKeyAction
import com.maddyhome.idea.vim.api.EffectiveOptions
import com.maddyhome.idea.vim.api.GlobalOptions
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimDigraphGroupBase
import com.maddyhome.idea.vim.api.VimOptionGroup
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.api.setToggleOption
import com.maddyhome.idea.vim.api.visualLineToBufferLine
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.group.EffectiveIjOptions
import com.maddyhome.idea.vim.group.GlobalIjOptions
import com.maddyhome.idea.vim.group.IjOptions
import com.maddyhome.idea.vim.group.visual.VimVisualTimer.swingTimer
import com.maddyhome.idea.vim.handler.isOctopusEnabled
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.TestInputModel
import com.maddyhome.idea.vim.helper.getGuiCursorMode
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.ToKeysMappingInfo
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.listener.VimListenerManager
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.ijOptions
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.options.helpers.GuiCursorOptionHelper
import com.maddyhome.idea.vim.options.helpers.GuiCursorType
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.inBlockSelection
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.parser.errors.IdeavimErrorListener
import org.jetbrains.annotations.ApiStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertThrows
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke
import kotlin.math.roundToInt
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * JUnit 5 tests
 *
 * To plugin writers: this class is internal, thus not allowed to be used by third-party plugins.
 * This is done as we have no mechanism to guarantee compatibility as we update this test case.
 * Feel free to copy this class into your plugin, or copy just needed functions.
 *
 * The tests are started on non-EDT thread without any locks.
 */
@ApiStatus.Internal
abstract class VimTestCase {
  protected lateinit var fixture: CodeInsightTestFixture

  lateinit var testInfo: TestInfo

  @BeforeEach
  open fun setUp(testInfo: TestInfo) {
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    this.fixture = createFixture(factory)
    this.fixture.setUp()
    this.fixture.testDataPath = testDataPath
    // Note that myFixture.editor is usually null here. It's only set once configureByText has been called
    val editor = this.fixture.editor
    if (editor != null) {
      KeyHandler.getInstance().fullReset(editor.vim)
    }
    KeyHandler.getInstance().keyHandlerState.reset(Mode.NORMAL())
    injector.vimState.reset()
    resetAllOptions()
    VimPlugin.getKey().resetKeyMappings()
    VimPlugin.getSearch().resetState()
    if (VimPlugin.isNotEnabled()) VimPlugin.setEnabled(true)
    injector.globalOptions().ideastrictmode = true
    Checks.reset()
    clearClipboard()

    // Make sure the entry text field gets a bounds, or we won't be able to work out caret location
    ExEntryPanel.getOrCreateInstance().entry.setBounds(0, 0, 100, 25)

    NeovimTesting.setUp(testInfo)

    injector.messages.clearError()
    injector.messages.clearStatusBarMessage()

    this.testInfo = testInfo
  }

  private fun resetAllOptions() {
    // Some options are mapped to IntelliJ settings. Make sure the IntelliJ settings match the Vim defaults
    EditorSettingsExternalizable.getInstance().apply {
      isUseCustomSoftWrapIndent = IjOptions.breakindent.defaultValue.booleanValue
      isRightMarginShown = false  // Otherwise we get `colorcolumn=+0`
      isWhitespacesShown = IjOptions.list.defaultValue.booleanValue
      isLineNumbersShown = Options.number.defaultValue.booleanValue
      lineNumeration = if (IjOptions.relativenumber.defaultValue.booleanValue) {
        if (isLineNumbersShown) EditorSettings.LineNumerationType.HYBRID else EditorSettings.LineNumerationType.RELATIVE
      } else {
        EditorSettings.LineNumerationType.ABSOLUTE
      }
      softWrapFileMasks = "*"
      isUseSoftWraps = IjOptions.wrap.defaultValue.booleanValue

      verticalScrollJump = Options.scrolljump.defaultValue.value
      verticalScrollOffset = Options.scrolloff.defaultValue.value
      horizontalScrollJump = Options.sidescroll.defaultValue.value
      horizontalScrollOffset = Options.sidescrolloff.defaultValue.value
    }

    CodeStyle.getDefaultSettings().getCommonSettings(null as Language?).apply {
      RIGHT_MARGIN = IjOptions.textwidth.defaultValue.value
      WRAP_ON_TYPING = if (IjOptions.textwidth.defaultValue > 0) {
        CommonCodeStyleSettings.WrapOnTyping.WRAP.intValue
      } else {
        CommonCodeStyleSettings.WrapOnTyping.NO_WRAP.intValue
      }
    }

    // Reset the Vim options. Important to do this after changing the IntelliJ settings, so that IdeaVim will treat
    // these values as defaults.
    VimPlugin.getOptionGroup().resetAllOptionsForTesting()
  }

  private fun setDefaultIntelliJSettings(editor: Editor) {
    // These settings don't have a global setting...
    ApplicationManager.getApplication().invokeAndWait {
      editor.settings.isCaretRowShown = IjOptions.cursorline.defaultValue.booleanValue
    }
  }

  protected open fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
    val fixture = factory.createLightFixtureBuilder(projectDescriptor, "IdeaVim").fixture
    return factory.createCodeInsightFixture(
      fixture,
      LightTempDirTestFixtureImpl(true),
    )
  }

  private val testDataPath: String
    get() = PathManager.getHomePath() + "/community/plugins/ideavim/testData"

  @AfterEach
  open fun tearDown(testInfo: TestInfo) {
    swingTimer?.stop()
    swingTimer = null
    val bookmarksManager = BookmarksManager.getInstance(fixture.project)
    bookmarksManager?.bookmarks?.forEach { bookmark ->
      bookmarksManager.remove(bookmark)
    }
    VimListenerManager.VimLastSelectedEditorTracker.resetLastSelectedEditor(fixture.project)
    SelectionVimListenerSuppressor.lock().use { fixture.tearDown() }
    ExEntryPanel.instance?.deactivate(false)
    VimPlugin.getVariableService().clear()
    VimFuncref.lambdaCounter = 0
    VimFuncref.anonymousCounter = 0
    IdeavimErrorListener.testLogger.clear()
    VimPlugin.getRegister().resetRegisters()
    VimPlugin.getSearch().resetState()
    injector.markService.resetAllMarks()
    injector.jumpService.resetJumps()
    injector.historyGroup.resetHistory()
    VimPlugin.getChange().resetRepeat()
    VimPlugin.getKey().savedShortcutConflicts.clear()
    assertTrue(KeyHandler.getInstance().keyStack.isEmpty())
    injector.outputPanel.getCurrentOutputPanel()?.close()
    injector.modalInput.getCurrentModalInput()?.deactivate(refocusOwningEditor = false, resetCaret = false)
    (injector.digraphGroup as VimDigraphGroupBase).clearCustomDigraphs()

    // Important to reset in tearDown as well as setUp, so we reset modified test options
    resetAllOptions()

    // disable all enabled extensions
    injector.extensionLoader.getEnabledExtensions().forEach {
      injector.extensionLoader.disableExtension(it.extensionName)
    }

    // Tear down neovim
    NeovimTesting.tearDown(testInfo)
  }

  protected fun enableExtensions(vararg extensionNames: String) {
    for (name in extensionNames) {
      val option = injector.optionGroup.getOption(name) as ToggleOption

      // Global value of a global option. We can pass null
      injector.optionGroup.setToggleOption(option, OptionAccessScope.GLOBAL(null))
    }
  }

  protected fun enableExtensionsNewApi(vararg extensionNames: String) {
    for (name in extensionNames) {
      injector.jsonExtensionProvider.getBundledExtensions().find { it.extensionName == name }?.let { extension ->
        injector.extensionLoader.enableExtension(extension)
      }
    }
  }

  protected fun <T> assertEmpty(collection: Collection<T>) {
    assertTrue(collection.isEmpty(), "Collection should be empty, but it contains ${collection.size} elements")
  }

  protected fun typeTextInFile(keys: List<KeyStroke?>, fileContents: String): Editor {
    configureByText(fileContents)
    return typeText(keys)
  }

  protected fun typeTextInFile(keys: String, fileContents: String): Editor {
    configureByText(fileContents)
    return typeText(keys)
  }

  protected val screenWidth: Int
    get() = 80
  protected val screenHeight: Int
    get() = 35

  protected fun setEditorVisibleSize(width: Int, height: Int) {
    val w = (width * EditorHelper.getPlainSpaceWidthFloat(fixture.editor)).roundToInt()
    val h = height * fixture.editor.lineHeight
    ApplicationManager.getApplication().invokeAndWait {
      EditorTestUtil.setEditorVisibleSizeInPixels(fixture.editor, w, h)
    }
  }

  protected fun setEditorVirtualSpace() {
    ApplicationManager.getApplication().invokeAndWait {
      // Enable virtual space at the bottom of the file and force a layout to pick up the changes
      fixture.editor.settings.isAdditionalPageAtBottom = true
      (fixture.editor as EditorEx).scrollPane.viewport.doLayout()
    }
  }

  protected fun configureByText(content: String) = configureByText(PlainTextFileType.INSTANCE, content)
  protected fun configureByXmlText(content: String) = configureByText(XmlFileType.INSTANCE, content)

  protected fun configureAndGuard(content: String) {
    val ranges = extractBrackets(content)
    ApplicationManager.getApplication().runReadAction {
      for ((start, end) in ranges) {
        fixture.editor.document.createGuardedBlock(start, end)
      }
    }
  }

  protected fun configureAndFold(content: String, @Suppress("SameParameterValue") placeholder: String) {
    val ranges = extractBrackets(content)
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.foldingModel.runBatchFoldingOperation {
        for ((start, end) in ranges) {
          val foldRegion = fixture.editor.foldingModel.addFoldRegion(start, end, placeholder)
          foldRegion?.isExpanded = false
        }
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

  protected fun configureByText(fileType: FileType, content: String): Editor {
    fixture.configureByText(fileType, content)
    // Note: Change to coroutines
    ApplicationManager.getApplication().invokeAndWait {
      setDefaultIntelliJSettings(fixture.editor)
    }
    NeovimTesting.setupEditor(fixture.editor, testInfo)
    // Note: Change to coroutines
    ApplicationManager.getApplication().invokeAndWait {
      setEditorVisibleSize(screenWidth, screenHeight)
    }
    return fixture.editor
  }

  private fun configureByText(fileName: String, content: String): Editor {
    fixture.configureByText(fileName, content)
    setDefaultIntelliJSettings(fixture.editor)
    NeovimTesting.setupEditor(fixture.editor, testInfo)
    setEditorVisibleSize(screenWidth, screenHeight)
    return fixture.editor
  }

  fun configureByTextX(fileName: String, content: String): Editor {
    fixture.configureByText(fileName, content)
    setDefaultIntelliJSettings(fixture.editor)
    NeovimTesting.setupEditor(fixture.editor, testInfo)
    setEditorVisibleSize(screenWidth, screenHeight)
    return fixture.editor
  }

  protected fun configureByFileName(fileName: String): Editor {
    fixture.configureByText(fileName, "\n")
    setDefaultIntelliJSettings(fixture.editor)
    NeovimTesting.setupEditor(fixture.editor, testInfo)
    setEditorVisibleSize(screenWidth, screenHeight)
    return fixture.editor
  }

  @Suppress("SameParameterValue")
  protected fun configureByPages(pageCount: Int) {
    val stringBuilder = StringBuilder()
    repeat(pageCount * screenHeight) {
      stringBuilder.appendLine("Lorem ipsum dolor sit amet,")
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

  protected fun configureByColumns(columnCount: Int, disableWrap: Boolean = true) {
    val content = buildString {
      repeat(columnCount) {
        append('0' + (it % 10))
      }
    }
    configureByText(content)

    // `'wrap'` is set by default. But if we're configuring long columns, we usually don't want soft wraps enabled
    if (disableWrap) {
      enterCommand("set nowrap")
    }
  }

  @JvmOverloads
  protected fun setPositionAndScroll(scrollToLogicalLine: Int, caretLogicalLine: Int, caretLogicalColumn: Int = 0) {
    // Note that it is possible to request a position which would be invalid under normal Vim!
    // We disable scrolloff + scrolljump, position as requested, and reset. When resetting scrolloff, Vim will
    // recalculate the correct offsets, and that could move the top and/or caret line
    val scrolloff = options().scrolloff
    val scrolljump = options().scrolljump

    enterCommand("set scrolloff=0")
    enterCommand("set scrolljump=1")

    typeText("${scrollToLogicalLine + 1}z<CR>", "${caretLogicalLine + 1}G", "${caretLogicalColumn + 1}|")

    enterCommand("set scrolloff=$scrolloff")
    enterCommand("set scrolljump=$scrolljump")

    // Make sure we're where we want to be. If there are block inlays, we can't easily assert the bottom line because
    // we'd have to duplicate the scrolling logic here. Asserting top when we know height is good enough
    assertTopLogicalLine(scrollToLogicalLine)
    assertPosition(caretLogicalLine, caretLogicalColumn)

    ApplicationManager.getApplication().invokeAndWait {
      // Belt and braces. Let's make sure that the caret is fully onscreen
      val bottomLogicalLine = fixture.editor.vim.visualLineToBufferLine(
        EditorHelper.getVisualLineAtBottomOfScreen(fixture.editor),
      )
      assertTrue(bottomLogicalLine >= caretLogicalLine)
      assertTrue(caretLogicalLine >= scrollToLogicalLine)
    }
  }

  protected fun typeText(vararg keys: String) = typeText(keys.flatMap { injector.parser.parseKeys(it) })

  protected fun typeText(keys: List<KeyStroke?>): Editor {
    var editor: Editor? = null
    ApplicationManager.getApplication().invokeAndWait {
      editor = typeText(fixture.editor, keys)
    }
    return editor!!
  }

  protected fun typeText(editor: Editor, keys: List<KeyStroke?>): Editor {
    NeovimTesting.typeCommand(
      keys.filterNotNull().joinToString(separator = "") { injector.parser.toKeyNotation(it) },
      testInfo,
      editor,
    )
    val project = fixture.project
    when (Checks.keyHandler) {
      Checks.KeyHandlerMethod.DIRECT_TO_VIM -> typeText(keys.filterNotNull(), editor, project)
      Checks.KeyHandlerMethod.VIA_IDE -> typeTextViaIde(keys.filterNotNull(), editor)
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
      fixture.editor.document.setText(text)
    }
  }

  protected fun executeVimscript(script: String, skipHistory: Boolean = true) {
    val context = DataContext.EMPTY_CONTEXT.vim
    injector.vimscriptExecutor.execute(
      script,
      fixture.editor.vim,
      context,
      skipHistory,
      indicateErrors = true,
      CommandLineVimLContext
    )
  }


  /**
   * Gets an accessor for effective option value
   *
   * This will return an accessor to retrieve the effective value for the current editor - a global value for global
   * options (e.g. 'clipboard') or a (potentially) local value for local to buffer, local to window or global-local
   * options (e.g. 'iskeyword', 'relativenumber' or 'scrolloff' respectively). Tests are only expected to require
   * effective values. To test other global/local values, use [VimOptionGroup].
   */
  protected fun options(): EffectiveOptions {
    assertNotNull(
      fixture.editor,
      "Editor is null! Move the call to after editor is initialised, or use optionsNoEditor",
    )
    return injector.options(fixture.editor.vim)
  }

  /**
   * Gets an accessor for the effective values of IntelliJ specific options
   *
   * This will return an accessor for the global or effective option values, but only for the IntelliJ specific options.
   */
  protected fun optionsIj(): EffectiveIjOptions {
    assertNotNull(
      fixture.editor,
      "Editor is null! Move the call to after editor is initialised, or use optionsNoEditor",
    )
    return injector.ijOptions(fixture.editor.vim)
  }

  /**
   * Gets an option value accessor purely for global options, when there is no editor available
   *
   * Tests should normally use effective option values, via [options], but that requires a test that has created an
   * editor. If the editor isn't available, this function will return an accessor that can be used to access global
   * options only. It should not be used to access local options, as there is nothing for them to be local to.
   *
   * Note that this isn't handled automatically by [options] to avoid the scenario of trying to use effective values
   * before the editor has been initialised.
   */
  protected fun optionsNoEditor(): GlobalOptions {
    assertNull(fixture.editor, "Editor is not null! Use options() to access effective option values")
    return injector.globalOptions()
  }

  /**
   * Gets an option value accessor for IntelliJ specific global options, when there is no editor available
   */
  protected fun optionsIjNoEditor(): GlobalIjOptions {
    assertNull(fixture.editor, "Editor is not null! Use options() to access effective option values")
    return injector.globalIjOptions()
  }

  fun assertState(textAfter: String) {
    fixture.checkResult(textAfter)
    NeovimTesting.assertState(fixture.editor, testInfo)
  }

  fun mode(): String? {
    return NeovimTesting.vimMode()
  }

  fun register(char: String): String? {
    return NeovimTesting.getMark(char)
  }

  protected fun assertRegister(char: Char, expected: String?) {
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    if (expected == null) {
      assertNull(injector.registerGroup.getRegister(vimEditor, context, char))
    } else {
      assertEquals(expected, injector.registerGroup.getRegister(vimEditor, context, char)?.printableString)
    }
  }

  protected fun assertRegisterString(char: Char, expected: String?) {
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val actual =
      injector.registerGroup.getRegister(vimEditor, context, char)?.keys?.let(injector.parser::toPrintableString)
    assertEquals(expected, actual, "Wrong register contents")
  }

  protected fun assertState(modeAfter: Mode) {
    assertMode(modeAfter)
    assertCaretsVisualAttributes()
  }

  fun assertPosition(line: Int, column: Int) {
    val carets = fixture.editor.caretModel.allCarets
    assertEquals(1, carets.size, "Wrong amount of carets")
    val actualPosition = carets[0].logicalPosition
    assertEquals(LogicalPosition(line, column), actualPosition)
    NeovimTesting.assertCaret(fixture.editor, testInfo)
  }

  fun assertVisualPosition(visualLine: Int, visualColumn: Int) {
    val carets = fixture.editor.caretModel.allCarets
    assertEquals(1, carets.size, "Wrong amount of carets")
    val actualPosition = carets[0].visualPosition
    assertEquals(VisualPosition(visualLine, visualColumn), actualPosition)
  }

  fun assertOffset(vararg expectedOffsets: Int) {
    val carets = fixture.editor.caretModel.allCarets
    if (expectedOffsets.size == 2 && carets.size == 1) {
      assertEquals(
        expectedOffsets.size,
        carets.size,
        "Wrong amount of carets. Did you mean to use assertPosition?",
      )
    }
    assertEquals(expectedOffsets.size, carets.size, "Wrong amount of carets")
    ApplicationManager.getApplication().runReadAction {
      for (i in expectedOffsets.indices) {
        assertEquals(expectedOffsets[i], carets[i].offset)
      }
    }

    NeovimTesting.assertState(fixture.editor, testInfo)
  }

  fun assertOffsetAt(text: String) {
    val indexOf = fixture.editor.document.charsSequence.indexOf(text)
    if (indexOf < 0) kotlin.test.fail()
    assertOffset(indexOf)
  }

  // Use logical rather than visual lines, so we can correctly test handling of collapsed folds and soft wraps
  fun assertVisibleArea(topLogicalLine: Int, bottomLogicalLine: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runReadAction {
        assertTopLogicalLine(topLogicalLine)
        assertBottomLogicalLine(bottomLogicalLine)
      }
    }
  }

  fun assertTopLogicalLine(topLogicalLine: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      val actualVisualTop = EditorHelper.getVisualLineAtTopOfScreen(fixture.editor)
      val actualLogicalTop = fixture.editor.vim.visualLineToBufferLine(actualVisualTop)

      assertEquals(topLogicalLine, actualLogicalTop, "Top logical lines don't match")
    }
  }

  fun assertBottomLogicalLine(bottomLogicalLine: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      val actualVisualBottom = EditorHelper.getVisualLineAtBottomOfScreen(fixture.editor)
      val actualLogicalBottom = fixture.editor.vim.visualLineToBufferLine(actualVisualBottom)

      assertEquals(bottomLogicalLine, actualLogicalBottom, "Bottom logical lines don't match")
    }
  }

  fun assertVisibleLineBounds(logicalLine: Int, leftLogicalColumn: Int, rightLogicalColumn: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      val visualLine = fixture.editor.vim.bufferLineToVisualLine(logicalLine)
      val actualLeftVisualColumn = EditorHelper.getVisualColumnAtLeftOfDisplay(fixture.editor, visualLine)
      val actualLeftLogicalColumn =
        fixture.editor.visualToLogicalPosition(VisualPosition(visualLine, actualLeftVisualColumn)).column
      val actualRightVisualColumn = EditorHelper.getVisualColumnAtRightOfDisplay(fixture.editor, visualLine)
      val actualRightLogicalColumn =
        fixture.editor.visualToLogicalPosition(VisualPosition(visualLine, actualRightVisualColumn)).column

      val expected = ScreenBounds(leftLogicalColumn, rightLogicalColumn)
      val actual = ScreenBounds(actualLeftLogicalColumn, actualRightLogicalColumn)
      assertEquals(expected, actual)
    }
  }

  fun assertLineCount(expected: Int) {
    assertEquals(expected, fixture.editor.vim.lineCount())
  }

  fun putMapping(modes: Set<MappingMode>, from: String, to: String, recursive: Boolean) {
    VimPlugin.getKey().putKeyMapping(
      modes,
      injector.parser.parseKeys(from),
      MappingOwner.IdeaVim.System,
      injector.parser.parseKeys(to),
      recursive,
    )
  }

  fun assertNoMapping(from: String) {
    val keys = injector.parser.parseKeys(from)
    for (mode in MappingMode.ALL) {
      assertNull(VimPlugin.getKey().getKeyMapping(mode)[keys])
    }
  }

  fun assertNoMapping(from: String, modes: Set<MappingMode>) {
    val keys = injector.parser.parseKeys(from)
    for (mode in modes) {
      assertNull(VimPlugin.getKey().getKeyMapping(mode)[keys], "Expected no mapping for $mode")
    }
  }

  fun assertMappingExists(from: String, to: String, modes: Set<MappingMode>) {
    val keys = injector.parser.parseKeys(from)
    val toKeys = injector.parser.parseKeys(to)
    for (mode in modes) {
      val info = VimPlugin.getKey().getKeyMapping(mode)[keys]
      assertNotNull<Any>(info)
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

  fun assertMode(expectedMode: Mode) {
    val mode = fixture.editor.vim.mode
    assertEquals(expectedMode, mode)
  }

  fun assertSelection(expected: String?) {
    var selected: String? = null
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runReadAction {
        selected = fixture.editor.selectionModel.selectedText
      }
    }
    assertEquals(expected, selected)
  }

  fun assertCommandOutput(command: String, expected: String) {
    enterCommand(command)
    assertExOutput(expected)
  }

  fun assertExOutput(expected: String, clear: Boolean = true) {
    val actual = injector.outputPanel.getCurrentOutputPanel()?.text
    assertNotNull(actual, "No Ex output")
    assertEquals(expected, actual)
    NeovimTesting.typeCommand("<esc>", testInfo, fixture.editor)
    if (clear) {
      // Ex output is not cleared until the output pane is activated again (we need it to persist so that :print will
      // work when called multiple times from :global). When testing, if the previous action fails before it can
      // activate the output pane, we'll be looking at stale results.
      ExOutputModel.getInstance(fixture.editor).clear()
    }
  }

  fun assertNoExOutput() {
    val actual = ExOutputModel.getInstance(fixture.editor).text
    assertEquals("", actual)
  }

  fun assertPluginError(isError: Boolean) {
    assertEquals(isError, injector.messages.isError())
  }

  fun assertPluginErrorMessage(message: String) {
    assertEquals(message, VimPlugin.getMessage())
  }

  fun assertStatusLineMessageContains(message: String) {
    assertContains(VimPlugin.getMessage(), message)
  }

  fun assertStatusLineCleared() {
    assertNull(VimPlugin.getMessage())
  }

  protected fun assertCaretsVisualAttributes() {
    if (!Checks.caretShape) return
    val editor = fixture.editor
    val attributes = GuiCursorOptionHelper.getAttributes(getGuiCursorMode(editor))
    val colour = editor.colorsScheme.getColor(EditorColors.CARET_COLOR)

    editor.caretModel.allCarets.forEach { caret ->
      // All carets should be the same except when in block sub mode, where we "hide" them (by drawing a zero width bar)
      if (caret !== editor.caretModel.primaryCaret && editor.vim.inBlockSelection) {
        assertEquals(CaretVisualAttributes.Shape.BAR, caret.visualAttributes.shape)
        assertEquals(0F, caret.visualAttributes.thickness)
      } else {
        val shape = when (attributes.type) {
          GuiCursorType.BLOCK -> CaretVisualAttributes.Shape.BLOCK
          GuiCursorType.VER -> CaretVisualAttributes.Shape.BAR
          GuiCursorType.HOR -> CaretVisualAttributes.Shape.UNDERSCORE
        }
        assertEquals(shape, editor.caretModel.primaryCaret.visualAttributes.shape)
        assertEquals(
          attributes.thickness / 100.0F,
          editor.caretModel.primaryCaret.visualAttributes.thickness,
        )
        editor.caretModel.primaryCaret.visualAttributes.color?.let {
          assertEquals(colour, it)
        }
      }
    }
  }

  @Suppress("DEPRECATION", "SameParameterValue")
  protected fun assertSearchHighlights(tooltip: String, expected: String) {
    val allHighlighters = fixture.editor.markupModel.allHighlighters

    thisLogger().debug("Current text: ${fixture.editor.document.text}")
    val actual = StringBuilder(fixture.editor.document.text)
    val inserts = mutableMapOf<Int, String>()

    // Digraphs:
    // <C-K>3" → ‷ + <C-K>3' → ‴ (current match)
    // <C-K><< → « + <C-K>>> → » (normal match)
    allHighlighters.forEach {
      // TODO: This is not the nicest way to check for current match. Add something to the highlight's user data?
      if (it.textAttributes?.effectType == EffectType.ROUNDED_BOX) {
        inserts.compute(it.startOffset) { _, v -> if (v == null) "‷" else "$v‷" }
        inserts.compute(it.endOffset) { _, v -> if (v == null) "‴" else "$v‴" }
      } else {
        inserts.compute(it.startOffset) { _, v -> if (v == null) "«" else "$v«" }
        inserts.compute(it.endOffset) { _, v -> if (v == null) "»" else "$v»" }
      }
    }

    var offset = 0
    inserts.toSortedMap().forEach { (k, v) ->
      actual.insert(k + offset, v)
      offset += v.length
    }

    assertEquals(expected, actual.toString())

    // Assert all highlighters have the correct tooltip and text attributes
    val editorColorsScheme = EditorColorsManager.getInstance().globalScheme
    val attributes = editorColorsScheme.getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES)
    val caretColour = editorColorsScheme.getColor(EditorColors.CARET_COLOR)
    allHighlighters.forEach {
      val offsets = "(${it.startOffset}, ${it.endOffset})"
      assertEquals(tooltip, it.errorStripeTooltip, "Incorrect tooltip for highlighter at $offsets")
      assertEquals(
        attributes.backgroundColor,
        it.textAttributes?.backgroundColor,
        "Incorrect background colour for highlighter at $offsets",
      )
      assertEquals(
        attributes.foregroundColor,
        it.textAttributes?.foregroundColor,
        "Incorrect foreground colour for highlighter at $offsets",
      )
      // TODO: Find a better way to identify the current match
      if (it.textAttributes?.effectType == EffectType.ROUNDED_BOX) {
        assertEquals(
          EffectType.ROUNDED_BOX,
          it.textAttributes?.effectType,
          "Incorrect effect type for highlighter at $offsets",
        )
        assertEquals(caretColour, it.textAttributes?.effectColor, "Incorrect effect colour for highlighter at $offsets")
      } else {
        assertEquals(
          attributes.effectType,
          it.textAttributes?.effectType,
          "Incorrect effect type for highlighter at $offsets",
        )
        assertEquals(
          attributes.effectColor,
          it.textAttributes?.effectColor,
          "Incorrect effect colour for highlighter at $offsets",
        )
      }
    }
  }

  protected fun assertNoSearchHighlights() {
    assertEquals(0, fixture.editor.markupModel.allHighlighters.size)
  }

  @JvmOverloads
  fun doTest(
    keys: List<String>,
    before: String,
    after: String,
    modeAfter: Mode = Mode.NORMAL(),
    fileType: FileType? = null,
    fileName: String? = null,
    afterEditorInitialized: ((Editor) -> Unit)? = null,
  ) {
    doTest(
      keys.joinToString(separator = ""),
      before,
      after,
      modeAfter,
      fileType,
      fileName,
      afterEditorInitialized,
    )
  }

  @JvmOverloads
  fun doTest(
    keys: String,
    before: String,
    after: String,
    modeAfter: Mode = Mode.NORMAL(),
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
    afterEditorInitialized?.invoke(fixture.editor)
    performTest(keys, after, modeAfter)
  }

  protected fun performTest(keys: String, after: String, modeAfter: Mode) {
    typeText(keys)
    ApplicationManager.getApplication().invokeAndWait {
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
    assertState(after)
    assertState(modeAfter)
  }

  protected fun setRegister(register: Char, keys: String) {
    VimPlugin.getRegister().setKeys(register, injector.parser.stringToKeys(keys))
    NeovimTesting.setRegister(register, keys, testInfo)
  }

  protected val fileManager: FileEditorManagerEx
    get() = FileEditorManagerEx.getInstanceEx(fixture.project)

  // Specify width in columns, not pixels, just like we do for visible screen size. The default text char width differs
  // per platform (e.g. Windows is 7, Mac is 8) so we can't guarantee correct positioning for tests if we use hard coded
  // pixel widths
  protected fun addInlay(
    offset: Int,
    relatesToPrecedingText: Boolean,
    @Suppress("SameParameterValue") widthInColumns: Int,
  ): Inlay<*> {
    var inlay: Inlay<*>? = null
    ApplicationManager.getApplication().invokeAndWait {
      val widthInPixels = (EditorHelper.getPlainSpaceWidthFloat(fixture.editor) * widthInColumns).roundToInt()
      inlay = EditorTestUtil.addInlay(fixture.editor, offset, relatesToPrecedingText, widthInPixels)
    }
    return inlay!!
  }

  // As for inline inlays, height is specified as a multiplier of line height, as we can't guarantee the same line
  // height on all platforms, so can't guarantee correct positioning for tests if we use pixels. This currently limits
  // us to integer multiples of line heights. I don't think this will cause any issues, but we can change this to a
  // float if necessary. We'd still be working scaled to the line height, so fractional values should still work.
  protected fun addBlockInlay(
    offset: Int,
    @Suppress("SameParameterValue") showAbove: Boolean,
    heightInRows: Int,
  ): Inlay<*> {
    val widthInColumns = 10 // Arbitrary width. We don't care.
    val widthInPixels = (EditorHelper.getPlainSpaceWidthFloat(fixture.editor) * widthInColumns).roundToInt()
    val heightInPixels = fixture.editor.lineHeight * heightInRows
    var inlay: Inlay<*>? = null
    ApplicationManager.getApplication().invokeAndWait {
      inlay = EditorTestUtil.addBlockInlay(fixture.editor, offset, false, showAbove, widthInPixels, heightInPixels)
    }
    return inlay!!
  }

  // Disable or enable checks for the particular test
  protected inline fun setupChecks(setup: Checks.() -> Unit) {
    Checks.setup()
  }

  protected fun assertExException(expectedErrorMessage: String, action: () -> Unit) {
    val exception = assertThrows<ExException> {
      action()
    }
    assertEquals(expectedErrorMessage, exception.message)
  }

  private fun typeTextViaIde(keys: List<KeyStroke?>, editor: Editor) {
    TestInputModel.getInstance(editor).setKeyStrokes(keys.filterNotNull())

    val inputModel = TestInputModel.getInstance(editor)
    var key = inputModel.nextKeyStroke()
    while (key != null) {
      val keyChar = key.getChar(editor)
      when (keyChar) {
        is CharType.CharDetected -> {
          fixture.type(keyChar.char)
        }

        is CharType.EditorAction -> {
          fixture.performEditorAction(keyChar.name)
        }

        CharType.UNDEFINED -> {
          val event =
            KeyEvent(editor.component, KeyEvent.KEY_PRESSED, Date().time, key.modifiers, key.keyCode, key.keyChar)

          val context = SimpleDataContext.builder()
            .setParent(EditorUtil.getEditorDataContext(editor))
            .add(PlatformCoreDataKeys.CONTEXT_COMPONENT, editor.component)
            .build()
          val e = AnActionEvent(
            event,
            context,
            ActionPlaces.KEYBOARD_SHORTCUT,
            VimShortcutKeyAction.instance.templatePresentation.clone(),
            ActionManager.getInstance(),
            0,
          )
          if (ActionUtil.lastUpdateAndCheckDumb(VimShortcutKeyAction.instance, e, true)) {
            ActionUtil.performActionDumbAwareWithCallbacks(VimShortcutKeyAction.instance, e)
          }
        }
      }
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
      key = inputModel.nextKeyStroke()
    }
  }

  private fun KeyStroke.getChar(editor: Editor): CharType {
    if (keyChar != KeyEvent.CHAR_UNDEFINED) return CharType.CharDetected(keyChar)
    if (isOctopusEnabled(this, editor)) {
      if (keyCode in setOf(KeyEvent.VK_ENTER)) {
        if (modifiers == 0) {
          return CharType.CharDetected(keyCode.toChar())
        }
      }
      if (keyCode == KeyEvent.VK_ESCAPE) return CharType.EditorAction("EditorEscape")
    }
    return CharType.UNDEFINED
  }

  private fun clearClipboard() {
    ClipboardSynchronizer.getInstance().resetContent()
    ClipboardSynchronizer.getInstance().setContent(EmptyTransferable, EmptyClipboardOwner.INSTANCE)
  }

  sealed interface CharType {
    object UNDEFINED : CharType
    class CharDetected(val char: Char) : CharType
    class EditorAction(val name: String) : CharType
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

  companion object {
    const val c = EditorTestUtil.CARET_TAG
    const val s = EditorTestUtil.SELECTION_START_TAG
    const val se = EditorTestUtil.SELECTION_END_TAG

    fun typeText(keys: List<KeyStroke?>, editor: Editor, project: Project?) {
      val keyHandler = KeyHandler.getInstance()
      val dataContext = injector.executionContextManager.getEditorExecutionContext(editor.vim)
      TestInputModel.getInstance(editor).setKeyStrokes(keys.filterNotNull())
      ApplicationManager.getApplication().invokeAndWait {
        injector.actionExecutor.executeCommand(
          editor.vim,
          Runnable {
            val inputModel = TestInputModel.getInstance(editor)
            var key = inputModel.nextKeyStroke()
            while (key != null) {
              keyHandler.handleKey(editor.vim, key, dataContext, keyHandler.keyHandlerState)
              key = inputModel.nextKeyStroke()
            }
          },
          null,
          null,
        )
      }
    }

    @JvmStatic
    fun commandToKeys(command: String): List<KeyStroke> {
      val keys: MutableList<KeyStroke> = ArrayList()

      keys.addAll(injector.parser.parseKeys(":"))
      var startIndex = if (command.startsWith(":")) 1 else 0

      // Special case support for <C-U>
      startIndex = if (command.substring(startIndex).startsWith("<C-U>", ignoreCase = true)) {
        keys.addAll(injector.parser.parseKeys("<C-U>"))
        startIndex + 5
      } else {
        startIndex
      }
      // We don't parse the rest of the command, to avoid parsing special keys in e.g. maps. Note that values such as
      // `<expr>` or `<args>` would be correctly handled by parseKeys
      keys.addAll(injector.parser.stringToKeys(command.substring(startIndex)))
      if (keys.last().keyCode != KeyEvent.VK_ENTER) {
        keys.addAll(injector.parser.parseKeys("<Enter>"))
      }
      return keys
    }

    fun exCommand(command: String) = ":$command<CR>"

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
}
