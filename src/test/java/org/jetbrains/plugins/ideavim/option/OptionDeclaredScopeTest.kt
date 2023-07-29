/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import com.intellij.util.childScope
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import javax.swing.SwingConstants
import kotlin.test.assertEquals

// Tests the implementation of global, local to buffer, local to window and global-local
@TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
class OptionDeclaredScopeTest : VimTestCase() {
  private val optionName = "test"
  private val defaultValue = VimString("defaultValue")
  private val setValue = VimString("setValue")
  private lateinit var manager: FileEditorManagerImpl
  private lateinit var originalEditor: Editor
  private lateinit var otherBufferWindow: Editor  // A new buffer, opened in a new window
  private lateinit var splitWindow: Editor      // A new window, split from the original editor

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    // Copied from FileEditorManagerTestCase to allow us to split windows
    @Suppress("DEPRECATION")
    manager = FileEditorManagerImpl(fixture.project, fixture.project.coroutineScope.childScope())
    fixture.project.replaceService(FileEditorManager::class.java, manager, fixture.testRootDisposable)

    // Create a new editor that will represent a new buffer in a separate window. It will have default values
    otherBufferWindow = openNewBufferWindow("bbb.txt")

    // Create the original editor last, so that fixture.editor will point to this file
    // It is STRONGLY RECOMMENDED to use originalEditor instead of fixture.editor, so we know which editor we're using
    originalEditor = configureByText("\n")  // aaa.txt

    manager.currentWindow.let {
      // Split the original editor into a new window, then reset the focus back to the originalEditor's EditorWindow
      // We do this before setting any custom options, so it will have default values for everything
      splitWindow = openSplitWindow(originalEditor) // aaa.txt
      manager.currentWindow = it
    }
  }

  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  // Note that this overwrites fixture.editor! This is the equivalent of `:new {file}`
  private fun openNewBufferWindow(filename: String): Editor {
    fixture.openFileInEditor(fixture.createFile(filename, "lorem ipsum"))
    return fixture.editor
  }

  // Note that the new split window (in a new EditorWindow) will be selected!
  private fun openSplitWindow(editor: Editor): Editor {
    val fileManager = FileEditorManagerEx.getInstanceEx(fixture.project)
    return (fileManager.currentWindow!!.split(
      SwingConstants.VERTICAL,
      true,
      editor.virtualFile,
      false
    )!!.allComposites.first().selectedEditor as TextEditor).editor
  }

  private fun closeWindow(editor: Editor) {
    val fileManager = FileEditorManagerEx.getInstanceEx(fixture.project)
    fileManager.closeFile(editor.virtualFile)
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    super.tearDown(testInfo)
    injector.optionGroup.removeOption(optionName)
  }

  // Check option values are correct after dynamically adding an option
  @Test
  fun `test initialise option values when dynamically adding new global option after editor opened`() {
    val option = StringOption(optionName, OptionDeclaredScope.GLOBAL, "test", defaultValue)
    injector.optionGroup.addOption(option)

    assertEquals(defaultValue, getLocalValue(option, originalEditor))
    assertEquals(defaultValue, getGlobalValue(option))
  }

  @Test
  fun `test initialise option values when dynamically adding new local-to-buffer option after editor opened`() {
    val option = StringOption(optionName, OptionDeclaredScope.LOCAL_TO_BUFFER, "test", defaultValue)
    injector.optionGroup.addOption(option)

    assertEquals(defaultValue, getLocalValue(option, originalEditor))
    assertEquals(defaultValue, getGlobalValue(option))
  }

  @Test
  fun `test initialise option values when dynamically adding new local-to-window option after editor opened`() {
    val option = StringOption(optionName, OptionDeclaredScope.LOCAL_TO_WINDOW, "test", defaultValue)
    injector.optionGroup.addOption(option)

    assertEquals(defaultValue, getLocalValue(option, originalEditor))
    assertEquals(defaultValue, getGlobalValue(option))
  }


  // Check option values are set and read from the correct scopes in different buffers/windows
  // Note that these tests are very similar to OptionScopeTest, but test changes across buffers/windows
  @Test
  fun `test global option affects all buffers and windows`() {
    withOption(OptionDeclaredScope.GLOBAL) {
      // It's a global option, so setting and getting local value will always deal with the global value
      setLocalValue(originalEditor)

      assertEffectiveValueChanged(otherBufferWindow)
      assertEffectiveValueChanged(splitWindow)
    }
  }

  @Test
  fun `test local-to-buffer option does not affect other buffer window, but does affect original buffer split window`() {
    withOption(OptionDeclaredScope.LOCAL_TO_BUFFER) {
      setLocalValue(originalEditor)

      assertEffectiveValueUnmodified(otherBufferWindow)
      assertEffectiveValueChanged(splitWindow)
    }
  }

  @Test
  fun `test local-to-window option does not affect other buffer window or split window`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      setEffectiveValue(originalEditor)

      assertEffectiveValueUnmodified(otherBufferWindow)
      assertEffectiveValueUnmodified(splitWindow)
    }
  }

  @Test
  fun `test global-local with auto scope acts like global and affects all buffers and windows`() {
    // It doesn't matter if we use local to buffer or window
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER) {
      setEffectiveValue(originalEditor)

      assertEffectiveValueChanged(otherBufferWindow)
      assertEffectiveValueChanged(splitWindow)
    }
  }

  @Test
  fun `test global-or-local-to-buffer option at local scope does not affect other buffer, but does affect split window`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER) {
      // Note that this should definitely be LOCAL! If we use AUTO, we get GLOBAL behaviour. There is a crossover with
      // OptionScopeTests, but this is testing the behaviour of global-local, rather than the behaviour of AUTO
      setLocalValue(originalEditor)

      assertEffectiveValueUnmodified(otherBufferWindow)
      assertEffectiveValueChanged(splitWindow)
    }
  }

  @Test
  fun `test global-or-local-to-window option at local scope does not affect other buffer or split window`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW) {
      // Note that this should definitely be LOCAL! If we use AUTO, we get GLOBAL behaviour. There is a crossover with
      // OptionScopeTests, but this is testing the behaviour of global-local, rather than the behaviour of AUTO
      setLocalValue(originalEditor)

      assertEffectiveValueUnmodified(otherBufferWindow)
      assertEffectiveValueUnmodified(splitWindow)
    }
  }


  // Check option initialisation when editing buffers and/or opening windows
  // Scenarios:
  // * Local-to-buffer + local-to-window
  // * Split the current window
  // * Edit a new buffer in the current window (IntelliJ does not support this)
  // * Edit a new buffer in a new window
  // * Edit a buffer that has previously been edited

  @Test
  fun `test initialise new buffer window with global copy of local-to-buffer option`() {
    withOption(OptionDeclaredScope.LOCAL_TO_BUFFER) {
      setGlobalValue()

      val newBuffer = openNewBufferWindow("ccc.txt")

      assertEffectiveValueChanged(newBuffer)
      assertGlobalValueChanged()
    }
  }

  @Test
  fun `test initialise new buffer window with global copy of local-to-buffer option 2`() {
    withOption(OptionDeclaredScope.LOCAL_TO_BUFFER) {
      // Set the local value, open a new buffer, and make sure the local value isn't copied across
      setLocalValue(originalEditor)

      val newBuffer = openNewBufferWindow("ccc.txt")

      assertEffectiveValueUnmodified(newBuffer)
      assertGlobalValueUnmodified()
    }
  }

  @Test
  fun `test initialise new split window by duplicating per-window global and local values of local-to-window option`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      // TODO: This should be a per-window "global" value, but we currently have no way to set that value
      // Set the (per-window) "global" value, make sure the new split window gets this value
      setGlobalValue()

      // Note that opening a window split should get a copy of the local values, too
      val newWindow = openSplitWindow(originalEditor)

      // When we split a window, we copy both per-window global + local values, rather than initialising from global
      assertEffectiveValueUnmodified(newWindow)
      assertGlobalValueChanged()
    }
  }

  @Test
  fun `test initialise new split window by duplicating per-window global and local values of local-to-window option 2`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      // Set the window's local value. Both "global" and local values are copied across to a new split window
      setLocalValue(originalEditor)

      val newWindow = openSplitWindow(originalEditor)

      // When we split a window, we copy both per-window global + local values, rather than initialising from global
      assertEffectiveValueChanged(newWindow)
      assertGlobalValueUnmodified()
    }
  }

  @Test
  fun `test initialise new split window by duplicating per-window global and local values of global-local local-to-window option`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW) {
      // TODO: This should be a per-window "global" value, but we currently have no way to set that value
      // Set the (per-window) "global" value, make sure the new split window gets this value
      setGlobalValue()

      // Note that opening a window split should get a copy of the local values, too
      val newWindow = openSplitWindow(originalEditor)

      // When we split a window, we copy both per-window global + local values, rather than initialising from global
      // The local value is unset, so the effective value is the same as the changed value, which is modified
      assertEffectiveValueChanged(newWindow)
      assertLocalValueUnset(newWindow)
      assertGlobalValueChanged()
    }
  }

  @Test
  fun `test initialise new split window by duplicating per-window global and local values of global-local local-to-window option 2`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW) {
      // Set the window's local value. Both "global" and local values are copied across to a new split window
      setLocalValue(originalEditor)

      val newWindow = openSplitWindow(originalEditor)

      // When we split a window, we copy both per-window global + local values, rather than initialising from global
      assertEffectiveValueChanged(newWindow)
      assertGlobalValueUnmodified()
    }
  }

  @Test
  fun `test initialise new buffer window with per-window global copy of local-to-window option`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      // TODO: This should be a per-window "global" value, but we currently have no way to set that value
      // Set the (per-window) "global" value, make sure the new window gets this value
      setGlobalValue()

      // This is the same as `:new {file}`
      val newWindow = openNewBufferWindow("ccc.txt")

      assertEffectiveValueChanged(newWindow)
      assertGlobalValueChanged()  // Assert per-window global?!
    }
  }

  @Disabled("IdeaVim does not currently support reusing the current window")
  fun `test initialise new buffer in current window with per-window global copy of local-to-window option`() {
    TODO("The IntelliJ implementation does not support reusing the current window for a different buffer/file")

    // In other words, `:edit {file}` acts like `:new {file}` and always opens a new window. I think it should be
    // possible to make it work more like traditional `:edit`, though. E.g. the preview tab can edit multiple files, and
    // there is an Advanced Setting for "Open declaration source in the same tab".
    // It appears that the platform doesn't actually reuse editors, but will close other editors automatically, which
    // has the same effect. When opening a new editor window, `EditorWindow.setComposite` will call `trimToSize` which
    // keeps the number of open tabs under the user's limit, and also closes any tabs that are marked as preview tabs.
    // If `UISettings.reuseNotModifiedTabs` is set, it will also try to close the current tab, which would have the
    // effect of looking like the current tab has been reused. It will only close the tab if it's not pinned and it's
    // not currently modified. It also checks to see if it (or a descendant) has the focus.
    // We could change `FileGroup.openFile` to temporarily set `UISettings.reuseNotModifiedTabs` and to throw an error
    // if the file is modified (`:edit! {file}`) should discard the current changes. We would also need to reset the
    // focus before trying to open the file (it's currently on the ex text field, even though that has been closed by
    // this point).
    // This would still open a new window if the initial current window was pinned, but Vim doesn't have a similar
    // concept, so we would get to decide behaviour - fall back to `:new` or report an error?

    // If the above is the implementation of `:edit`, then the behaviour is the same as for `:new`, because both open a
    // new editor window, but the implementation of `:edit` would also close the current window
    // I'm not sure how to test it though
  }

  @Test
  fun `test reopening a buffer should maintain local-to-buffer options`() {
    withOption(OptionDeclaredScope.LOCAL_TO_BUFFER) {
      setLocalValue(otherBufferWindow)

      val file = otherBufferWindow.virtualFile
      closeWindow(otherBufferWindow)
      fixture.openFileInEditor(file)
      val newBufferWindow = fixture.editor

      assertEffectiveValueChanged(newBufferWindow)
      assertGlobalValueUnmodified()
    }
  }

  @Disabled("IdeaVim does not maintain a history of local to window option values. It is unclear what Vim's behaviour here is. " +
    "Leaving this test as documentation that Vim does actually do this")
  fun `test reopening a buffer should maintain the last used local-to-window options`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      setLocalValue(otherBufferWindow)

      val file = otherBufferWindow.virtualFile
      closeWindow(otherBufferWindow)
      fixture.openFileInEditor(file)
      val newBufferWindow = fixture.editor

      assertEffectiveValueChanged(newBufferWindow)
      assertGlobalValueUnmodified()
    }

    TODO("Not implemented. This is Vim behaviour, but this data is not saved by IdeaVim")
  }

  private inline fun withOption(declaredScope: OptionDeclaredScope, action: Option<VimString>.() -> Unit) {
    StringOption(optionName, declaredScope, optionName, defaultValue).let { option ->
      injector.optionGroup.addOption(option)
      option.action()
    }
  }

  private fun Option<VimString>.setGlobalValue() =
    injector.optionGroup.setOptionValue(this, OptionScope.GLOBAL, setValue)

  private fun Option<VimString>.setLocalValue(editor: Editor) =
    injector.optionGroup.setOptionValue(this, OptionScope.LOCAL(editor.vim), setValue)

  private fun Option<VimString>.setEffectiveValue(editor: Editor) {
    injector.optionGroup.setOptionValue(this, OptionScope.AUTO(editor.vim), setValue)
  }

  private fun getGlobalValue(option: Option<VimString>) =
    injector.optionGroup.getOptionValue(option, OptionScope.GLOBAL)

  private fun getLocalValue(option: Option<VimString>, editor: Editor) =
    injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(editor.vim))

  private fun getEffectiveValue(option: Option<VimString>, editor: Editor) =
    injector.optionGroup.getOptionValue(option, OptionScope.AUTO(editor.vim))

  private fun assertValueUnmodified(actualValue: VimString) = assertEquals(defaultValue, actualValue)
  private fun assertValueChanged(actualValue: VimString) = assertEquals(setValue, actualValue)

  private fun Option<VimString>.assertEffectiveValueUnmodified(editor: Editor) =
    assertValueUnmodified(getEffectiveValue(this, editor))

  private fun Option<VimString>.assertEffectiveValueChanged(editor: Editor) =
    assertValueChanged(getEffectiveValue(this, editor))

  private fun Option<VimString>.assertGlobalValueUnmodified() = assertValueUnmodified(getGlobalValue(this))
  private fun Option<VimString>.assertGlobalValueChanged() = assertValueChanged(getGlobalValue(this))

  private fun Option<VimString>.assertLocalValueUnset(editor: Editor) =
    assertEquals(this.unsetValue, getLocalValue(this, editor))
}