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
import com.intellij.testFramework.replaceService
import com.intellij.util.childScope
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import javax.swing.SwingConstants
import kotlin.test.assertEquals

// Tests the implementation of global, local to buffer, local to window and global-local
@TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
class OptionDeclaredScopeTest : VimTestCase() {
  private val optionName = "test"
  private val defaultValue = VimString("defaultValue")
  private val localValue = VimString("localValue")
  private lateinit var manager: FileEditorManagerImpl
  private lateinit var originalEditor: Editor
  private lateinit var newBuffer: Editor
  private lateinit var newWindow: Editor

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    // Copied from FileEditorManagerTestCase to allow us to split windows
    @Suppress("DEPRECATION")
    manager = FileEditorManagerImpl(fixture.project, fixture.project.coroutineScope.childScope())
    fixture.project.replaceService(FileEditorManager::class.java, manager, fixture.testRootDisposable)

    // Create the new buffer before changing the value, or AUTO can write to both local and global values
    val fileManager = FileEditorManagerEx.getInstanceEx(fixture.project)
    val newFile = fixture.configureByText("bbb.txt", "new file")
    newBuffer = (fileManager.openFile(newFile.virtualFile, false).first() as TextEditor).editor

    // Create the original editor last, so that fixture.editor will point to this file
    originalEditor = configureByText("\n")

    newWindow = (fileManager.currentWindow!!.split(
      SwingConstants.VERTICAL,
      true,
      originalEditor.virtualFile,
      false
    )!!.allComposites.first().selectedEditor as TextEditor).editor
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    super.tearDown(testInfo)
    injector.optionGroup.removeOption(optionName)
  }

  @Test
  fun `test global option affects all buffers and windows`() {
    val option = StringOption(optionName, OptionDeclaredScope.GLOBAL, "test", defaultValue)
    injector.optionGroup.addOption(option)

    injector.optionGroup.setOptionValue(option, OptionScope.LOCAL(fixture.editor.vim), localValue)

    // TODO: This should be AUTO
    val newBufferValue = injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(newBuffer.vim))
    assertEquals(localValue, newBufferValue)

    val newWindowValue = injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(newWindow.vim))
    assertEquals(localValue, newWindowValue)
  }

  @Test
  fun `test buffer local option is different in new buffer, but same in new window`() {
    val option = StringOption(optionName, OptionDeclaredScope.LOCAL_TO_BUFFER, "test", defaultValue)
    injector.optionGroup.addOption(option)

    injector.optionGroup.setOptionValue(option, OptionScope.LOCAL(fixture.editor.vim), localValue)

    // TODO: This should be AUTO
    val newBufferValue = injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(newBuffer.vim))
    assertEquals(defaultValue, newBufferValue)

    val newWindowValue = injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(newWindow.vim))
    assertEquals(localValue, newWindowValue)
  }

  @Test
  fun `test window local option is different in new buffer and new window`() {
    val option = StringOption(optionName, OptionDeclaredScope.LOCAL_TO_WINDOW, "test", defaultValue)
    injector.optionGroup.addOption(option)

    // TODO: This should be AUTO
    injector.optionGroup.setOptionValue(option, OptionScope.LOCAL(fixture.editor.vim), localValue)

    val newBufferValue = injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(newBuffer.vim))
    assertEquals(defaultValue, newBufferValue)

    val newWindowValue = injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(newWindow.vim))
    assertEquals(defaultValue, newWindowValue)
  }

  @Test
  fun `test global-local with auto scope acts like global and affects all buffers and windows`() {
    // It doesn't matter if we use local to buffer or window
    val option = StringOption(optionName, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, "test", defaultValue)
    injector.optionGroup.addOption(option)

    // TODO: This should be AUTO
    // Using GLOBAL just sets the global value
    injector.optionGroup.setOptionValue(option, OptionScope.GLOBAL, localValue)

    val newBufferValue = injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(newBuffer.vim))
    assertEquals(localValue, newBufferValue)

    val newWindowValue = injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(newWindow.vim))
    assertEquals(localValue, newWindowValue)
  }

  @Test
  fun `test global or local to buffer option at local scope is different in new buffer, but same in new window`() {
    val option = StringOption(optionName, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER, "test", defaultValue)
    injector.optionGroup.addOption(option)

    // Note that this should definitely be LOCAL! If we use AUTO, we get GLOBAL behaviour. There is a crossover with
    // OptionScopeTests, but this is testing the behaviour of global-local, rather than the behaviour of AUTO
    injector.optionGroup.setOptionValue(option, OptionScope.LOCAL(fixture.editor.vim), localValue)

    val newBufferValue = injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(newBuffer.vim))
    assertEquals(defaultValue, newBufferValue)

    val newWindowValue = injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(newWindow.vim))
    assertEquals(localValue, newWindowValue)
  }

  @Test
  fun `test global or local to window option at local scope is different in new buffer and new window`() {
    val option = StringOption(optionName, OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", defaultValue)
    injector.optionGroup.addOption(option)

    // Note that this should definitely be LOCAL! If we use AUTO, we get GLOBAL behaviour. There is a crossover with
    // OptionScopeTests, but this is testing the behaviour of global-local, rather than the behaviour of AUTO
    injector.optionGroup.setOptionValue(option, OptionScope.LOCAL(fixture.editor.vim), localValue)

    val newBufferValue = injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(newBuffer.vim))
    assertEquals(defaultValue, newBufferValue)

    val newWindowValue = injector.optionGroup.getOptionValue(option, OptionScope.LOCAL(newWindow.vim))
    assertEquals(defaultValue, newWindowValue)
  }
}