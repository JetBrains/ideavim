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
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitUntil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import javax.swing.SwingConstants
import kotlin.io.path.Path
import kotlin.test.assertEquals

// Tests the implementation of global, local to buffer, local to window and global-local
@TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
class OptionDeclaredScopeTest : VimTestCase() {
  private val optionName = "test"
  private val defaultValue = VimString("defaultValue")
  private val setValue = VimString("setValue")

  private lateinit var fileEditorManager: FileEditorManagerImpl
  private lateinit var mainWindow: Editor
  private lateinit var otherBufferWindow: Editor  // A new buffer, opened in a new window
  private lateinit var splitWindow: Editor      // A new window, split from the original editor

  private val fallbackWindow: VimEditor
    get() = injector.fallbackWindow

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    // Copied from FileEditorManagerTestCase to allow us to split windows
    fileEditorManager = FileEditorManagerImpl(fixture.project, (fixture.project as ComponentManagerEx).getCoroutineScope().childScope(name = "OptionDeclaredScopeTest"))
    fixture.project.replaceService(FileEditorManager::class.java, fileEditorManager, fixture.testRootDisposable)

    // Create a new editor that will represent a new buffer in a separate window. It will have default values
    otherBufferWindow = openNewBufferWindow("bbb.txt")

    var curWindow: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      // Create the original editor last, so that fixture.editor will point to this file
      // It is STRONGLY RECOMMENDED to use originalEditor instead of fixture.editor, so we know which editor we're using
      mainWindow = configureByText("\n")  // aaa.txt
      curWindow = fileEditorManager.currentWindow
    }

    curWindow.let {
      // Split the original editor into a new window, then reset the focus back to the originalEditor's EditorWindow
      // We do this before setting any custom options, so it will have default values for everything
      splitWindow = openSplitWindow(mainWindow) // aaa.txt
      fileEditorManager.currentWindow = it
    }
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    super.tearDown(testInfo)
    injector.optionGroup.removeOption(optionName)
  }

  // If we're replacing the test FileEditorManager, then we can't use the default light project descriptor
  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  /**
   * Open a new editor/Vim window for a new buffer and moves the focus to the new editor
   *
   * This is the equivalent of the `:new {file}` command in Vim, which will split the current window, then edit a new
   * file/buffer in the new split. In contrast, the `:edit {file}` command should edit a new file in the current window.
   * IdeaVim does not support the `:new {file}` command, but its implementation of `:edit {file}` has the required
   * behaviour. Therefore, this function is like asking IdeaVim to `:edit {file}`.
   *
   * The new window will have the focus.
   *
   * Note that this overwrites `fixture.editor`!
   *
   * @return the `Editor` representing the new window
   */
  private fun openNewBufferWindow(filename: String): Editor {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile(filename, "lorem ipsum"))
    }
    return fixture.editor
  }

  /**
   * Splits the given editor/Vim window vertically and moves the focus to the new editor
   *
   * Equivalent to `<C-W>v` or `:vsplit` with the `'splitright'` option enabled in Vim. (Note that IdeaVim doesn't
   * currently support `'splitright'` or `'splitbelow'`.)
   *
   * @return the `Editor` representing the new window
   */
  private fun openSplitWindow(editor: Editor): Editor {
    // Open the split with the API, rather than Vim commands, so we get the editor
    var splitWindow: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      val currentWindow = fileEditorManager.currentWindow
      splitWindow = currentWindow!!.split(
        SwingConstants.VERTICAL,
        true,
        editor.virtualFile,
        false
      )
    }

    // Waiting till the selected editor will appear
    waitUntil {
      splitWindow!!.allComposites.first().selectedEditor != null
    }
    return (splitWindow!!.allComposites.first().selectedEditor as TextEditor).editor
  }

  /**
   * Closes the given editor
   */
  private fun closeWindow(editor: Editor) {
    ApplicationManager.getApplication().invokeAndWait {
      // Just using fileEditorManager.closeFile(editor.virtualFile) can cause weird side effects, like opening a
      // different buffer in an open editor. See FileGroup.closeFile
      // But we can't just rely on the current EditorWindow. E.g., if we're trying to close a file that's not currently
      // open in the current window, or is open in a split while we want to close the *other* editor...
      val editorWindow = fileEditorManager.windows.first { window ->
        window.allComposites.any { composite ->
          composite.allEditors.filterIsInstance<TextEditor>().any { textEditor -> textEditor.editor == editor }
        }
      }
      val virtualFile = editor.virtualFile

      if (virtualFile != null) {
        editorWindow.closeFile(virtualFile)
        editorWindow.requestFocus(true)
      }
    }
  }

  // Check option values are correct after dynamically adding an option
  @Test
  fun `test initialise option values when dynamically adding new global option after editor opened`() {
    withOption(OptionDeclaredScope.GLOBAL) {
      assertOptionValues(mainWindow,        local = defaultValue, effective = defaultValue, global = defaultValue)
      assertOptionValues(splitWindow,       local = defaultValue, effective = defaultValue, global = defaultValue)
      assertOptionValues(otherBufferWindow, local = defaultValue, effective = defaultValue, global = defaultValue)
      assertOptionValues(fallbackWindow,    local = defaultValue, effective = defaultValue, global = defaultValue)
    }
  }

  @Test
  fun `test initialise option values when dynamically adding new local-to-buffer option after editor opened`() {
    withOption(OptionDeclaredScope.LOCAL_TO_BUFFER) {
      assertOptionValues(mainWindow,        local = defaultValue, effective = defaultValue, global = defaultValue)
      assertOptionValues(splitWindow,       local = defaultValue, effective = defaultValue, global = defaultValue)
      assertOptionValues(otherBufferWindow, local = defaultValue, effective = defaultValue, global = defaultValue)
      assertOptionValues(fallbackWindow,    local = defaultValue, effective = defaultValue, global = defaultValue)
    }
  }

  @Test
  fun `test initialise option values when dynamically adding new local-to-window option after editor opened`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      assertOptionValues(mainWindow,        local = defaultValue, effective = defaultValue, global = defaultValue)
      assertOptionValues(splitWindow,       local = defaultValue, effective = defaultValue, global = defaultValue)
      assertOptionValues(otherBufferWindow, local = defaultValue, effective = defaultValue, global = defaultValue)
      assertOptionValues(fallbackWindow,    local = defaultValue, effective = defaultValue, global = defaultValue)
    }
  }


  // Check option values are set and read from the correct scopes in different buffers/windows
  // Note that these tests are very similar to OptionScopeTest, but test changes across buffers/windows
  @Test
  fun `test global option affects all buffers and windows`() {
    withOption(OptionDeclaredScope.GLOBAL) {
      // It's a global option, so setting and getting local value will always deal with the global value
      setLocalValue(mainWindow)

      assertOptionValues(mainWindow,        local = _changed_, effective = _changed_, global = _changed_)
      assertOptionValues(splitWindow,       local = _changed_, effective = _changed_, global = _changed_)
      assertOptionValues(otherBufferWindow, local = _changed_, effective = _changed_, global = _changed_)
      assertOptionValues(fallbackWindow,    local = _changed_, effective = _changed_, global = _changed_)
    }
  }

  @Test
  fun `test local-to-buffer option does not affect other buffer window, but does affect original buffer split window`() {
    withOption(OptionDeclaredScope.LOCAL_TO_BUFFER) {
      setLocalValue(mainWindow)

      assertOptionValues(mainWindow,        local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(splitWindow,       local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(otherBufferWindow, local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test local-to-window option does not affect other buffer window or split window`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      // Effective will change local + (per-window) global
      setEffectiveValue(mainWindow)

      assertOptionValues(mainWindow,        local = _changed_, effective = _changed_, global = _changed_)
      assertOptionValues(splitWindow,       local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(otherBufferWindow, local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test local-to-window option at global scope does not affect other buffer window or split window`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      setGlobalValue(mainWindow)

      assertOptionValues(mainWindow,        local = unchanged, effective = unchanged, global = _changed_)
      assertOptionValues(splitWindow,       local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(otherBufferWindow, local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test global-local with effective scope acts like global and affects all buffers and windows`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER) {
      setEffectiveValue(mainWindow)

      assertOptionValues(mainWindow,        local = __unset__, effective = _changed_, global = _changed_)
      assertOptionValues(splitWindow,       local = __unset__, effective = _changed_, global = _changed_)
      assertOptionValues(otherBufferWindow, local = __unset__, effective = _changed_, global = _changed_)
      assertOptionValues(fallbackWindow,    local = __unset__, effective = _changed_, global = _changed_)
    }
  }

  @Test
  fun `test global-or-local-to-buffer option at local scope does not affect other buffer, but does affect split window`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER) {
      // Note that this should definitely be LOCAL! If we use EFFECTIVE, we get GLOBAL behaviour. There is a crossover
      // with OptionScopeTests, but this is testing the behaviour of global-local rather than the behaviour of EFFECTIVE
      setLocalValue(mainWindow)

      assertOptionValues(mainWindow,        local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(splitWindow,       local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(otherBufferWindow, local = __unset__, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = __unset__, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test global-or-local-to-window option at local scope does not affect other buffer or split window`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW) {
      // Note that this should definitely be LOCAL! If we use EFFECTIVE, we get GLOBAL behaviour. There is a crossover
      // with OptionScopeTests, but this is testing the behaviour of global-local rather than the behaviour of EFFECTIVE
      setLocalValue(mainWindow)

      assertOptionValues(mainWindow,        local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(splitWindow,       local = __unset__, effective = unchanged, global = unchanged)
      assertOptionValues(otherBufferWindow, local = __unset__, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = __unset__, effective = unchanged, global = unchanged)
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
      setGlobalValue(mainWindow)

      val newBufferWindow = openNewBufferWindow("ccc.txt")

      assertOptionValues(mainWindow,        local = unchanged, effective = unchanged, global = _changed_)
      assertOptionValues(newBufferWindow,   local = _changed_, effective = _changed_, global = _changed_)
      assertOptionValues(splitWindow,       local = unchanged, effective = unchanged, global = _changed_)
      assertOptionValues(otherBufferWindow, local = unchanged, effective = unchanged, global = _changed_)
      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = _changed_)
    }
  }

  @Test
  fun `test initialise new buffer window with global copy of local-to-buffer option 2`() {
    withOption(OptionDeclaredScope.LOCAL_TO_BUFFER) {
      // Set the local value, open a new buffer, and make sure the local value isn't copied across
      setLocalValue(mainWindow)

      val newBufferWindow = openNewBufferWindow("ccc.txt")

      assertOptionValues(mainWindow,        local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(newBufferWindow,   local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(splitWindow,       local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(otherBufferWindow, local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test initialise new split window by duplicating per-window global and local values of local-to-window option`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      // Set the (per-window) "global" value, make sure the new split window gets this value
      setGlobalValue(mainWindow)

      // Note that opening a window split should get a copy of the local values, too
      val newSplitWindow = openSplitWindow(mainWindow)

      // When we split a window, we copy both per-window global + local values, rather than initialising from global
      assertOptionValues(mainWindow,        local = unchanged, effective = unchanged, global = _changed_)
      assertOptionValues(newSplitWindow,    local = unchanged, effective = unchanged, global = _changed_)
      assertOptionValues(splitWindow,       local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(otherBufferWindow, local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test initialise new split window by duplicating per-window global and local values of local-to-window option 2`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      // Set the window's local value. Both "global" and local values are copied across to a new split window
      setLocalValue(mainWindow)

      val newSplitWindow = openSplitWindow(mainWindow)

      // When we split a window, we copy both per-window global + local values, rather than initialising from global
      assertOptionValues(mainWindow,        local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(newSplitWindow,    local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(splitWindow,       local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(otherBufferWindow, local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test initialise new split window by duplicating per-window global and local values of global-local local-to-window option`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW) {
      // Set the global value, make sure the new split window gets this value
      setGlobalValue(mainWindow)

      // Note that opening a window split should get a copy of the local values, too
      val newSplitWindow = openSplitWindow(mainWindow)

      // When we split a window, we copy both global + local values, rather than initialising from global
      // The local value is unset, so the effective value is the same as the changed value, which is modified
      assertOptionValues(mainWindow,        local = __unset__, effective = _changed_, global = _changed_)
      assertOptionValues(newSplitWindow,    local = __unset__, effective = _changed_, global = _changed_)
      assertOptionValues(splitWindow,       local = __unset__, effective = _changed_, global = _changed_)
      assertOptionValues(otherBufferWindow, local = __unset__, effective = _changed_, global = _changed_)
      assertOptionValues(fallbackWindow,    local = __unset__, effective = _changed_, global = _changed_)
    }
  }

  @Test
  fun `test initialise new split window by duplicating per-window global and local values of global-local local-to-window option 2`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW) {
      // Set the window's local value. Both "global" and local values are copied across to a new split window
      setLocalValue(mainWindow)

      val newSplitWindow = openSplitWindow(mainWindow)

      // When we split a window, we copy both global + local values, rather than initialising from global
      assertOptionValues(mainWindow,        local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(newSplitWindow,    local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(splitWindow,       local = __unset__, effective = unchanged, global = unchanged)
      assertOptionValues(otherBufferWindow, local = __unset__, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = __unset__, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test initialise new buffer window with per-window global copy of local-to-window option`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      // Set the per-window global value (this does not get applied to other windows)
      setGlobalValue(mainWindow)

      // This is the same as `:new {file}`
      val newBufferWindow = openNewBufferWindow("ccc.txt")

      assertOptionValues(mainWindow,        local = unchanged, effective = unchanged, global = _changed_)
      assertOptionValues(newBufferWindow,   local = _changed_, effective = _changed_, global = _changed_)
      assertOptionValues(splitWindow,       local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(otherBufferWindow, local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test initialise new buffer window with local copy of local-to-window option`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      setLocalValue(mainWindow)

      // This is the same as `:new {file}`
      // Copies global+local values, then resets from global value
      val newBufferWindow = openNewBufferWindow("ccc.txt")

      assertOptionValues(mainWindow,        local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(newBufferWindow,   local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(splitWindow,       local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(otherBufferWindow, local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }
  }

  @Disabled("IdeaVim does not currently support reusing the current window")
  @Test
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

      assertOptionValues(otherBufferWindow, local = _changed_, effective = _changed_, global = unchanged)

      val file = otherBufferWindow.virtualFile
      ApplicationManager.getApplication().invokeAndWait {
        closeWindow(otherBufferWindow)
        fixture.openFileInEditor(file)
      }
      val newBufferWindow = fixture.editor

      assertOptionValues(mainWindow,        local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(newBufferWindow,   local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(splitWindow,       local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }
  }

  @Disabled("IdeaVim does not maintain a history of local to window option values. It is unclear what Vim's behaviour here is. " +
    "Leaving this test as documentation that Vim does actually do this")
  @Test
  fun `test reopening a buffer should maintain the last used local-to-window options`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      setLocalValue(otherBufferWindow)

      assertOptionValues(otherBufferWindow, local = _changed_, effective = _changed_, global = unchanged)

      val file = otherBufferWindow.virtualFile
      ApplicationManager.getApplication().invokeAndWait {
        closeWindow(otherBufferWindow)
        fixture.openFileInEditor(file)
      }
      val newBufferWindow = fixture.editor

      assertOptionValues(mainWindow,        local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(newBufferWindow,   local = _changed_, effective = _changed_, global = unchanged)
      assertOptionValues(splitWindow,       local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }

    TODO("Not implemented. This is Vim behaviour, but this data is not saved by IdeaVim")
  }

  @Test
  fun `test options are not reset when disabling and re-enabling the IdeaVim plugin`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      // Sets the local and (per-window) global value
      setEffectiveValue(fixture.editor)

      ApplicationManager.getApplication().invokeAndWait {
        VimPlugin.setEnabled(false)
        VimPlugin.setEnabled(true)
      }

      assertOptionValues(mainWindow,        local = _changed_, effective = _changed_, global = _changed_)
      assertOptionValues(splitWindow,       local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(otherBufferWindow, local = unchanged, effective = unchanged, global = unchanged)
      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }
  }

  // Test handling of the fallback window when closing windows
  // Vim always has an open window, so currently set local options are passed on to the next buffer edited in the
  // current window, or the next opened window/split. IntelliJ does not always have a currently open window, so there's
  // a dummy "fallback" window that is updated when the last window in a project is closed. This is used to initialise
  // the next new window
  // Previous tests make sure that the fallback window is not unnecessarily updated when values changed (apart from
  // global, obviously)
  // * Test fallback window IS NOT updated when closing arbitrary windows. Local values only, plus per-window global
  // * Test fallback window IS updated when closing the last window. Again, local values only, plus per-window global
  // We don't need to test global values because they will obviously be the same for the fallback window. We also
  // don't need to test effective values because that will update both local + global, and if we've test local, we
  // know it's working

  @Test
  fun `test closing arbitrary window does not update fallback window for local-to-buffer local value`() {
    withOption(OptionDeclaredScope.LOCAL_TO_BUFFER) {
      setLocalValue(mainWindow)
      closeWindow(mainWindow)

      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test closing arbitrary window does not update fallback window for local-to-window local value`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      setLocalValue(mainWindow)
      closeWindow(mainWindow)

      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test closing arbitrary window does not update fallback window for local-to-window per-window global value`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      setGlobalValue(mainWindow)
      closeWindow(mainWindow)

      assertOptionValues(fallbackWindow,    local = unchanged, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test closing arbitrary window does not update fallback window for global-local-to-buffer local value`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER) {
      setLocalValue(mainWindow)
      closeWindow(mainWindow)

      assertOptionValues(fallbackWindow,    local = __unset__, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test closing arbitrary window does not update fallback window for global-local-to-window local value`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW) {
      setLocalValue(mainWindow)
      closeWindow(mainWindow)

      assertOptionValues(fallbackWindow,    local = __unset__, effective = unchanged, global = unchanged)
    }
  }

  @Test
  fun `test closing last window updates fallback window with local-to-buffer local value`() {
    withOption(OptionDeclaredScope.LOCAL_TO_BUFFER) {
      closeWindow(splitWindow)
      closeWindow(otherBufferWindow)

      setLocalValue(mainWindow)
      closeWindow(mainWindow)

      assertOptionValues(fallbackWindow,    local = _changed_, effective = _changed_, global = unchanged)
    }
  }

  @Test
  fun `test closing last window updates fallback window with local-to-window local value`() {
    withOption(OptionDeclaredScope.LOCAL_TO_WINDOW) {
      closeWindow(splitWindow)
      closeWindow(otherBufferWindow)

      setLocalValue(mainWindow)
      closeWindow(mainWindow)

      assertOptionValues(fallbackWindow,    local = _changed_, effective = _changed_, global = unchanged)
    }
  }

  @Test
  fun `test closing last window updates fallback window with global-local-to-buffer local value`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER) {
      closeWindow(splitWindow)
      closeWindow(otherBufferWindow)

      setLocalValue(mainWindow)
      closeWindow(mainWindow)

      assertOptionValues(fallbackWindow,    local = _changed_, effective = _changed_, global = unchanged)
    }
  }

  @Test
  fun `test closing last window updates fallback window with global-local-to-window local value`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW) {
      closeWindow(splitWindow)
      closeWindow(otherBufferWindow)

      setLocalValue(mainWindow)
      closeWindow(mainWindow)

      assertOptionValues(fallbackWindow,    local = _changed_, effective = _changed_, global = unchanged)
    }
  }

  @Test
  fun `test closing last window updates fallback window with local-to-window per-window global value`() {
    withOption(OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW) {
      closeWindow(splitWindow)
      closeWindow(otherBufferWindow)

      setGlobalValue(mainWindow)
      closeWindow(mainWindow)

      assertOptionValues(fallbackWindow,    local = __unset__, effective = _changed_, global = _changed_)
    }
  }


  private inline fun withOption(declaredScope: OptionDeclaredScope, action: Option<VimString>.() -> Unit) {
    StringOption(optionName, declaredScope, optionName, defaultValue).let { option ->
      injector.optionGroup.addOption(option)
      option.action()
    }
  }

  private fun Option<VimString>.setGlobalValue(editor: Editor) =
    injector.optionGroup.setOptionValue(this, OptionAccessScope.GLOBAL(editor.vim), setValue)

  private fun Option<VimString>.setLocalValue(editor: Editor) =
    injector.optionGroup.setOptionValue(this, OptionAccessScope.LOCAL(editor.vim), setValue)

  private fun Option<VimString>.setEffectiveValue(editor: Editor) {
    injector.optionGroup.setOptionValue(this, OptionAccessScope.EFFECTIVE(editor.vim), setValue)
  }

  private fun getGlobalValue(option: Option<VimString>, editor: VimEditor) =
    injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(editor))

  private fun getLocalValue(option: Option<VimString>, editor: VimEditor) =
    injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(editor))

  private fun getEffectiveValue(option: Option<VimString>, editor: VimEditor) =
    injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(editor))

  // The funny names help to keep the asserts in a nice fixed width tabular form and makes the values easier to read
  @Suppress("PrivatePropertyName")
  private val Option<VimString>.__unset__
    get() = this.unsetValue
  private val Option<VimString>.unchanged
    // Technically, for global-local options at local scope, this should return unsetValue. Use __unset__ instead
    get() = this.defaultValue
  @Suppress("PrivatePropertyName")
  private val Option<VimString>._changed_
    get() = setValue

  private fun Option<VimString>.assertOptionValues(editor: Editor, local: VimString, effective: VimString, global: VimString) {
    assertOptionValues(editor.vim, local, effective, global)
  }

  private fun Option<VimString>.assertOptionValues(editor: VimEditor, local: VimString, effective: VimString, global: VimString) {
    val filename = Path(editor.getPath() ?: "unknown file").fileName.toString()
    assertEquals(local, getLocalValue(this, editor), "Local value of '$optionName' for '$filename' is incorrect")
    assertEquals(effective, getEffectiveValue(this, editor), "Effective value of '$optionName' for '$filename' is incorrect")
    assertEquals(global, getGlobalValue(this, editor), "Global value of '$optionName' for '$filename' is incorrect")
  }
}
