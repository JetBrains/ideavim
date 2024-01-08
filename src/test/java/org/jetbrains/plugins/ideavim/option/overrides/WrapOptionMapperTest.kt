/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option.overrides

import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class WrapOptionMapperTest : VimTestCase() {
  private lateinit var manager: FileEditorManagerImpl

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    // Copied from FileEditorManagerTestCase to allow us to split windows
    @Suppress("DEPRECATION")
    manager = FileEditorManagerImpl(fixture.project, fixture.project.coroutineScope.childScope())
    fixture.project.replaceService(FileEditorManager::class.java, manager, fixture.testRootDisposable)

    configureByText("\n")
  }

  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  @Suppress("SameParameterValue")
  private fun switchToNewFile(filename: String, content: String) {
    // This replaces fixture.editor
    fixture.openFileInEditor(fixture.createFile(filename, content))

    // But our selection changed callback doesn't get called immediately, and that callback will deactivate the ex entry
    // panel (which causes problems if our next command is `:set`). So type something (`0` is a good no-op) to give time
    // for the event to propagate
    typeText("0")
  }

  @Test
  fun `test 'wrap' defaults to current intellij setting`() {
    assertTrue(fixture.editor.settings.isUseSoftWraps)
    assertTrue(optionsIj().wrap)
  }

  @Test
  fun `test 'wrap' defaults to global intellij setting`() {
    assertTrue(EditorSettingsExternalizable.getInstance().isUseSoftWraps)
    assertTrue(optionsIj().wrap)
  }

  @Test
  fun `test 'wrap' option reports current global intellij setting if not explicitly set`() {
    EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
    assertCommandOutput("set wrap?", "nowrap\n")

    EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
    assertCommandOutput("set wrap?", "  wrap\n")
  }

  @Test
  fun `test local 'wrap' option reports current global intellij setting if not explicitly set`() {
    EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
    assertCommandOutput("setlocal wrap?", "nowrap\n")

    EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
    assertCommandOutput("setlocal wrap?", "  wrap\n")
  }

  @Test
  fun `test 'wrap' option reports local intellij setting if set via IDE`() {
    fixture.editor.settings.isUseSoftWraps = true
    assertCommandOutput("set wrap?", "  wrap\n")

    fixture.editor.settings.isUseSoftWraps = false
    assertCommandOutput("set wrap?", "nowrap\n")
  }

  @Test
  fun `test local 'wrap' option reports local intellij setting if set via IDE`() {
    fixture.editor.settings.isUseSoftWraps = true
    assertCommandOutput("setlocal wrap?", "  wrap\n")

    fixture.editor.settings.isUseSoftWraps = false
    assertCommandOutput("setlocal wrap?", "nowrap\n")
  }

  @Test
  fun `test set 'wrap' modifies local intellij setting only`() {
    // Note that `:set` modifies both the local and global setting, but that global setting is a Vim setting, not the
    // global IntelliJ setting
    enterCommand("set nowrap")
    assertFalse(fixture.editor.settings.isUseSoftWraps)
    assertTrue(EditorSettingsExternalizable.getInstance().isUseSoftWraps)

    enterCommand("set wrap")
    assertTrue(fixture.editor.settings.isUseSoftWraps)
    assertTrue(EditorSettingsExternalizable.getInstance().isUseSoftWraps)
  }

  @Test
  fun `test setlocal 'wrap' modifies local intellij setting only`() {
    enterCommand("setlocal nowrap")
    assertFalse(fixture.editor.settings.isUseSoftWraps)
    assertTrue(EditorSettingsExternalizable.getInstance().isUseSoftWraps)

    enterCommand("setlocal wrap")
    assertTrue(fixture.editor.settings.isUseSoftWraps)
    assertTrue(EditorSettingsExternalizable.getInstance().isUseSoftWraps)
  }

  @Test
  fun `test global 'wrap' option affects IdeaVim value only`() {
    EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
    assertCommandOutput("setglobal wrap?", "  wrap\n")  // Default for IdeaVim option is true

    EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
    enterCommand("setglobal nowrap")
    assertCommandOutput("setglobal wrap?", "nowrap\n")
    assertTrue(EditorSettingsExternalizable.getInstance().isUseSoftWraps)
  }

  @Test
  fun `test setglobal reports state from last call to set`() {
    // `:set` will update both the local value, and the IdeaVim-only global value
    enterCommand("set nowrap")
    assertCommandOutput("setglobal wrap?", "nowrap\n")
  }

  @Test
  fun `test setting IDE value is treated like setlocal`() {
    // If we use `:set`, it updates the local and per-window global values. If we set the value from the IDE, it only
    // affects the local value
    fixture.editor.settings.isUseSoftWraps = false
    assertCommandOutput("setlocal wrap?", "nowrap\n")
    assertCommandOutput("set wrap?", "nowrap\n")
    assertCommandOutput("setglobal wrap?", "  wrap\n")
  }

  @Test
  fun `test setglobal does not modify effective value`() {
    enterCommand("setglobal nowrap")
    assertTrue(fixture.editor.settings.isUseSoftWraps)
  }

  @Test
  fun `test setglobal does not modify IDEs persistent global value`() {
    enterCommand("setglobal nowrap")
    assertTrue(EditorSettingsExternalizable.getInstance().isUseSoftWraps)
  }

  @Test
  fun `test reset 'wrap' to default copies current global intellij setting`() {
    EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
    fixture.editor.settings.isUseSoftWraps = false
    assertCommandOutput("set wrap?", "nowrap\n")

    enterCommand("set wrap&")
    assertTrue(fixture.editor.settings.isUseSoftWraps)
    assertTrue(EditorSettingsExternalizable.getInstance().isUseSoftWraps)

    // Verify that IntelliJ doesn't allow us to "unset" a local editor setting - it's a copy of the default value
    EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
    assertTrue(fixture.editor.settings.isUseSoftWraps)
  }

  @Test
  fun `test reset local 'wrap' to default copies current global intellij setting`() {
    fixture.editor.settings.isUseSoftWraps = false
    assertCommandOutput("setlocal wrap?", "nowrap\n")

    enterCommand("setlocal wrap&")
    assertTrue(fixture.editor.settings.isUseSoftWraps)
    assertTrue(EditorSettingsExternalizable.getInstance().isUseSoftWraps)

    // Verify that IntelliJ doesn't allow us to "unset" a local editor setting - it's a copy of the default value
    EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
    assertTrue(fixture.editor.settings.isUseSoftWraps)
  }

  @Test
  fun `test open new window without setting the option copies value as not-explicitly set`() {
    // New window will clone local and global local-to-window options, then apply global to local. This tests that our
    // handling of per-window "global" values is correct.
    assertCommandOutput("set wrap?", "  wrap\n")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set wrap?", "  wrap\n")

    // Changing the global setting should update the new editor
    EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
    assertCommandOutput("set wrap?", "nowrap\n")
  }

  @Test
  fun `test open new window after setting option copies value as explicitly set`() {
    enterCommand("set nowrap")
    assertCommandOutput("set wrap?", "nowrap\n")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set wrap?", "nowrap\n")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
    assertCommandOutput("set wrap?", "nowrap\n")
  }

  @Test
  fun `test setglobal 'wrap' used when opening new window`() {
    enterCommand("setglobal nowrap")
    assertCommandOutput("setglobal wrap?", "nowrap\n")
    assertCommandOutput("set wrap?", "  wrap\n")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set wrap?", "nowrap\n")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
    assertCommandOutput("set wrap?", "nowrap\n")
  }

  @Test
  fun `test setlocal 'wrap' then open new window uses value from setglobal`() {
    enterCommand("setlocal nowrap")
    assertCommandOutput("setglobal wrap?", "  wrap\n")
    assertCommandOutput("set wrap?", "nowrap\n")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set wrap?", "  wrap\n")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
    assertCommandOutput("set wrap?", "  wrap\n")
  }
}
