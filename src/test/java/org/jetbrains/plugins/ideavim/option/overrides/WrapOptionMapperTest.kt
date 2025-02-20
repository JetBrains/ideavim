/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option.overrides

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import com.maddyhome.idea.vim.api.injector
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
    manager =
      FileEditorManagerImpl(fixture.project, (fixture.project as ComponentManagerEx).getCoroutineScope().childScope())
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
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile(filename, content))
    }

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
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
      assertCommandOutput("set wrap?", "nowrap")

      EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
      assertCommandOutput("set wrap?", "  wrap")
    }
  }

  @Test
  fun `test local 'wrap' option reports current global intellij setting if not explicitly set`() {
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
      assertCommandOutput("setlocal wrap?", "nowrap")

      EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
      assertCommandOutput("setlocal wrap?", "  wrap")
    }
  }

  @Test
  fun `test 'wrap' option reports local intellij setting if set via IDE`() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.settings.isUseSoftWraps = true
      assertCommandOutput("set wrap?", "  wrap")

      fixture.editor.settings.isUseSoftWraps = false
      assertCommandOutput("set wrap?", "nowrap")
    }
  }

  @Test
  fun `test local 'wrap' option reports local intellij setting if set via IDE`() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.settings.isUseSoftWraps = true
      assertCommandOutput("setlocal wrap?", "  wrap")

      fixture.editor.settings.isUseSoftWraps = false
      assertCommandOutput("setlocal wrap?", "nowrap")
    }
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
  fun `test setglobal 'wrap' option affects IdeaVim global value only`() {
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
      assertCommandOutput("setglobal wrap?", "  wrap")  // Default for IdeaVim option is true

      EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
      enterCommand("setglobal nowrap")
      assertCommandOutput("setglobal wrap?", "nowrap")
      assertTrue(EditorSettingsExternalizable.getInstance().isUseSoftWraps)
    }
  }

  @Test
  fun `test set updates IdeaVim global value as well as local`() {
    // `:set` will update both the local value, and the IdeaVim-only global value
    enterCommand("set nowrap")
    assertCommandOutput("setglobal wrap?", "nowrap")
  }

  @Test
  fun `test setting local IDE value is treated like setlocal`() {
    // If we use `:set`, it updates the local and per-window global values. If we set the value from the IDE, it only
    // affects the local value
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.settings.isUseSoftWraps = false
      assertCommandOutput("setlocal wrap?", "nowrap")
      assertCommandOutput("set wrap?", "nowrap")
      assertCommandOutput("setglobal wrap?", "  wrap")
    }
  }

  @Test
  fun `test setting global IDE value will not update explicitly set value`() {
    enterCommand("set nowrap")

    EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
    assertCommandOutput("setlocal wrap?", "nowrap")
    assertCommandOutput("set wrap?", "nowrap")
    assertCommandOutput("setglobal wrap?", "nowrap")

    EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
    assertCommandOutput("setlocal wrap?", "nowrap")
    assertCommandOutput("set wrap?", "nowrap")
    assertCommandOutput("setglobal wrap?", "nowrap")
  }

  @Test
  fun `test setting global IDE value will update effective Vim value set during plugin startup`() {
    ApplicationManager.getApplication().invokeAndWait {
      try {
        injector.optionGroup.startInitVimRc()
        enterCommand("set nowrap")
      } finally {
        injector.optionGroup.endInitVimRc()
      }

      // Default is true, so reset it to false, then set back to true
      EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
      EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
      assertCommandOutput("setlocal wrap?", "  wrap")
      assertCommandOutput("set wrap?", "  wrap")
      assertCommandOutput("setglobal wrap?", "  wrap")
    }
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
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
      fixture.editor.settings.isUseSoftWraps = false
      assertCommandOutput("set wrap?", "nowrap")

      enterCommand("set wrap&")
      assertTrue(fixture.editor.settings.isUseSoftWraps)
      assertTrue(EditorSettingsExternalizable.getInstance().isUseSoftWraps)

      // Verify that IntelliJ doesn't allow us to "unset" a local editor setting - it's a copy of the default value
      EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
      assertTrue(fixture.editor.settings.isUseSoftWraps)
    }
  }

  @Test
  fun `test reset local 'wrap' to default copies current global intellij setting`() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.settings.isUseSoftWraps = false
      assertCommandOutput("setlocal wrap?", "nowrap")

      enterCommand("setlocal wrap&")
      assertTrue(fixture.editor.settings.isUseSoftWraps)
      assertTrue(EditorSettingsExternalizable.getInstance().isUseSoftWraps)

      // Verify that IntelliJ doesn't allow us to "unset" a local editor setting - it's a copy of the default value
      EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
      assertTrue(fixture.editor.settings.isUseSoftWraps)
    }
  }

  @Test
  fun `test open new window without setting the option copies value as not-explicitly set`() {
    // New window will clone local and global local-to-window options, then apply global to local. This tests that our
    // handling of per-window "global" values is correct.
    ApplicationManager.getApplication().invokeAndWait {
      assertCommandOutput("set wrap?", "  wrap")

      switchToNewFile("bbb.txt", "lorem ipsum")

      assertCommandOutput("set wrap?", "  wrap")

      // Changing the global setting should update the new editor
      EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
      assertCommandOutput("set wrap?", "nowrap")
    }
  }

  @Test
  fun `test open new window after setting option copies value as explicitly set`() {
    enterCommand("set nowrap")
    assertCommandOutput("set wrap?", "nowrap")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set wrap?", "nowrap")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
    assertCommandOutput("set wrap?", "nowrap")
  }

  @Test
  fun `test setglobal 'wrap' used when opening new window`() {
    enterCommand("setglobal nowrap")
    assertCommandOutput("setglobal wrap?", "nowrap")
    assertCommandOutput("set wrap?", "  wrap")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set wrap?", "nowrap")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
    assertCommandOutput("set wrap?", "nowrap")
  }

  @Test
  fun `test setlocal 'wrap' then open new window uses value from setglobal`() {
    enterCommand("setlocal nowrap")
    assertCommandOutput("setglobal wrap?", "  wrap")
    assertCommandOutput("set wrap?", "nowrap")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set wrap?", "  wrap")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
    assertCommandOutput("set wrap?", "  wrap")
  }

  @Test
  fun `test setting global IDE value will update effective Vim value in new window initialised from value set during startup`() {
    ApplicationManager.getApplication().invokeAndWait {
      try {
        injector.optionGroup.startInitVimRc()
        enterCommand("set nowrap")
      } finally {
        injector.optionGroup.endInitVimRc()
      }

      switchToNewFile("bbb.txt", "lorem ipsum")
      assertCommandOutput("set wrap?", "nowrap")

      // Changing the global setting should update the editor, because it was initially set during plugin startup
      // Default is true, so reset before changing again
      EditorSettingsExternalizable.getInstance().isUseSoftWraps = false
      EditorSettingsExternalizable.getInstance().isUseSoftWraps = true
      assertCommandOutput("set wrap?", "  wrap")
    }
  }
}
