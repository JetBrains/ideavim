/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.EffectiveOptionValueChangeListener
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitUntil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import javax.swing.SwingConstants
import kotlin.test.assertEquals

private const val defaultValue = "defaultValue"
private const val defaultNumberValue = 10

@TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
class EffectiveOptionChangeListenerTest : VimTestCase() {
  private val optionName = "test"
  private lateinit var manager: FileEditorManagerImpl
  private lateinit var otherBufferWindow: Editor
  private lateinit var originalEditor: Editor
  private lateinit var splitWindow: Editor

  private object Listener : EffectiveOptionValueChangeListener {
    val notifiedEditors = mutableListOf<Editor>()

    override fun onEffectiveValueChanged(editor: VimEditor) {
      notifiedEditors.add(editor.ij)
    }
  }

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    // Copied from FileEditorManagerTestCase to allow us to split windows
    manager =
      FileEditorManagerImpl(fixture.project, (fixture.project as ComponentManagerEx).getCoroutineScope().childScope(name = "EffectiveOptionChangeListenerTest"))
    fixture.project.replaceService(FileEditorManager::class.java, manager, fixture.testRootDisposable)

    // Create a new editor that will represent a new buffer in a separate window. It will have default values
    otherBufferWindow = openNewBufferWindow("bbb.txt")

    ApplicationManager.getApplication().invokeAndWait {
      // Create the original editor last, so that fixture.editor will point to this file
      // It is STRONGLY RECOMMENDED to use originalEditor instead of fixture.editor, so we know which editor we're using
      originalEditor = configureByText("\n")  // aaa.txt
    }

    // Split the current window. Since no options have been set, it will have default values
    splitWindow = openSplitWindow(originalEditor) // aaa.txt
  }

  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  // Note that this overwrites fixture.editor! This is the equivalent of `:new {file}`
  private fun openNewBufferWindow(filename: String): Editor {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile(filename, "lorem ipsum"))
    }
    return fixture.editor
  }

  private fun openSplitWindow(editor: Editor): Editor {
    val fileManager = FileEditorManagerEx.getInstanceEx(fixture.project)
    var split: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      val currentWindow = fileManager.currentWindow
      split = currentWindow!!.split(
        SwingConstants.VERTICAL,
        true,
        editor.virtualFile,
        false
      )
    }

    // Waiting till the selected editor will appear
    waitUntil {
      split!!.allComposites.first().selectedEditor != null
    }

    return (split!!.allComposites.first().selectedEditor as TextEditor).editor
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    super.tearDown(testInfo)
    Listener.notifiedEditors.clear()
    injector.optionGroup.removeOption(optionName)
  }

  private fun addOption(scope: OptionDeclaredScope): StringOption {
    val option = StringOption(optionName, scope, optionName, defaultValue)
    injector.optionGroup.addOption(option)
    injector.optionGroup.addEffectiveOptionValueChangeListener(option, Listener)
    return option
  }

  private fun addNumberOption(scope: OptionDeclaredScope): NumberOption {
    val option = NumberOption(optionName, scope, optionName, defaultNumberValue)
    injector.optionGroup.addOption(option)
    injector.optionGroup.addEffectiveOptionValueChangeListener(option, Listener)
    return option
  }

  private fun assertNotifiedEditors(vararg editors: Editor) {
    val expected = editors.toSet()
    val actual = Listener.notifiedEditors.toSet()
    assertEquals(expected, actual)
  }

  private fun assertNoNotifications() = assertNotifiedEditors()

  @Test
  fun `test listener called for all editors when global option changes`() {
    val option = addOption(OptionDeclaredScope.GLOBAL)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor, splitWindow, otherBufferWindow)
  }

  @Test
  fun `test listener not called when global option is set to current value`() {
    val option = addOption(OptionDeclaredScope.GLOBAL)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), VimString(defaultValue))

    assertNoNotifications()
  }

  @Test
  fun `test listener called for all editors when global option changes at local scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor, splitWindow, otherBufferWindow)
  }

  @Test
  fun `test listener not called when global option set to current value at local scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(fixture.editor.vim), VimString(defaultValue))

    assertNoNotifications()
  }

  @Test
  fun `test listener called for all editors when global option changes at effective scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(originalEditor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor, splitWindow, otherBufferWindow)
  }

  @Test
  fun `test listener not called when global option set to current value at effective scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL)
    injector.optionGroup.setOptionValue(
      option,
      OptionAccessScope.EFFECTIVE(fixture.editor.vim),
      VimString(defaultValue)
    )

    assertNoNotifications()
  }

  @Test
  fun `test listener not called when local-to-buffer option changes at global scope`() {
    val option = addOption(OptionDeclaredScope.LOCAL_TO_BUFFER)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), VimString("newValue"))

    assertNoNotifications()
  }

  @Test
  fun `test listener called for all buffer editors when local-to-buffer option changes at local scope`() {
    val option = addOption(OptionDeclaredScope.LOCAL_TO_BUFFER)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(originalEditor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor, splitWindow)
  }

  @Test
  fun `test listener not called when local-to-buffer option set to current value at local scope`() {
    val option = addOption(OptionDeclaredScope.LOCAL_TO_BUFFER)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(originalEditor.vim), VimString(defaultValue))

    assertNoNotifications()
  }

  @Test
  fun `test listener called for all buffer editors when local-to-buffer option changes at effective scope`() {
    val option = addOption(OptionDeclaredScope.LOCAL_TO_BUFFER)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(originalEditor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor, splitWindow)
  }

  @Test
  fun `test listener not called when local-to-buffer option set to current value at effective scope`() {
    val option = addOption(OptionDeclaredScope.LOCAL_TO_BUFFER)
    injector.optionGroup.setOptionValue(
      option,
      OptionAccessScope.EFFECTIVE(originalEditor.vim),
      VimString(defaultValue)
    )

    assertNoNotifications()
  }

  @Test
  fun `test listener not called when local-to-window option changes at global scope`() {
    val option = addOption(OptionDeclaredScope.LOCAL_TO_WINDOW)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), VimString("newValue"))

    assertNotifiedEditors()
  }

  @Test
  fun `test listener called for single window when local-to-window option changes at local scope`() {
    val option = addOption(OptionDeclaredScope.LOCAL_TO_WINDOW)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(originalEditor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor)
  }

  @Test
  fun `test listener not called when local-to-window option set to current value at local scope`() {
    val option = addOption(OptionDeclaredScope.LOCAL_TO_WINDOW)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(originalEditor.vim), VimString(defaultValue))

    assertNoNotifications()
  }

  @Test
  fun `test listener called for single window when local-to-window option changes at effective scope`() {
    val option = addOption(OptionDeclaredScope.LOCAL_TO_WINDOW)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(originalEditor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor)
  }

  @Test
  fun `test listener not called when local-to-window option set to current value at effective scope`() {
    val option = addOption(OptionDeclaredScope.LOCAL_TO_WINDOW)
    injector.optionGroup.setOptionValue(
      option,
      OptionAccessScope.EFFECTIVE(originalEditor.vim),
      VimString(defaultValue)
    )

    assertNoNotifications()
  }

  @Test
  fun `test listener called for all editors when unset global-local local-to-buffer option changes at global scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor, splitWindow, otherBufferWindow)
  }

  @Test
  fun `test listener called for all buffer editors when unset global-local local-to-buffer option changes at local scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(originalEditor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor, splitWindow)
  }

  @Test
  fun `test listener called for all editors when unset global-local local-to-buffer option changes at effective scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(originalEditor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor, splitWindow, otherBufferWindow)
  }

  @Test
  fun `test listener called for all editors when locally modified global-local local-to-buffer option changes at effective scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(originalEditor.vim), VimString("localValue"))
    Listener.notifiedEditors.clear()

    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(originalEditor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor, splitWindow, otherBufferWindow)
  }

  @Test
  fun `test listener called for all editors when locally modified number global-local local-to-buffer option changes at effective scope`() {
    // When a number (and therefore also toggle) global-local option is set at effective scope, the local value is not
    // reset to [Option.unsetValue] but to a copy of the new value. The local editor(s) should still be notified.
    val option = addNumberOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(originalEditor.vim), VimInt(100))
    Listener.notifiedEditors.clear()

    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(originalEditor.vim), VimInt(200))

    assertNotifiedEditors(originalEditor, splitWindow, otherBufferWindow)
  }

  @Test
  fun `test listener not called for locally modified editor when global-local local-to-buffer option changes at global scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(otherBufferWindow.vim), VimString("localValue"))
    Listener.notifiedEditors.clear()

    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), VimString("newValue"))

    // Not called for editor where value is locally modified, but called for others
    assertNotifiedEditors(originalEditor, splitWindow)
  }

  @Test
  fun `test listener called for all editors when unset global-local local-to-window option changes at global scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor, splitWindow, otherBufferWindow)
  }

  @Test
  fun `test listener called for single editor when unset global-local local-to-window option changes at local scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(originalEditor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor)
  }

  @Test
  fun `test listener called for all editors when unset global-local local-to-window option changes at effective scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(originalEditor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor, splitWindow, otherBufferWindow)
  }

  @Test
  fun `test listener called for all editors when locally modified global-local local-to-window option changes at effective scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(originalEditor.vim), VimString("localValue"))
    Listener.notifiedEditors.clear()

    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(originalEditor.vim), VimString("newValue"))

    assertNotifiedEditors(originalEditor, splitWindow, otherBufferWindow)
  }

  @Test
  fun `test listener called for all editors when locally modified number global-local local-to-window option changes at effective scope`() {
    // When a number (and therefore also toggle) global-local option is set at effective scope, the local value is not
    // reset to [Option.unsetValue] but to a copy of the new value. The local editor(s) should still be notified.
    val option = addNumberOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(originalEditor.vim), VimInt(100))
    Listener.notifiedEditors.clear()

    injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(originalEditor.vim), VimInt(200))

    assertNotifiedEditors(originalEditor, splitWindow, otherBufferWindow)
  }

  @Test
  fun `test listener not called for locally modified editor when global-local local-to-window option changes at global scope`() {
    val option = addOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW)
    injector.optionGroup.setOptionValue(option, OptionAccessScope.LOCAL(otherBufferWindow.vim), VimString("localValue"))
    Listener.notifiedEditors.clear()

    injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(fixture.editor.vim), VimString("newValue"))

    // Not called for editor where value is locally modified, but called for others
    assertNotifiedEditors(originalEditor, splitWindow)
  }
}