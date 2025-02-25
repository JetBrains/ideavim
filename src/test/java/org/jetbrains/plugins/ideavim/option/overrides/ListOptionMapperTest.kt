/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option.overrides

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.IjOptions
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class ListOptionMapperTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
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
  fun `test 'list' defaults to current intellij setting`() {
    assertFalse(fixture.editor.settings.isWhitespacesShown)
    assertFalse(optionsIj().list)
  }

  @Test
  fun `test 'list' defaults to global intellij setting`() {
    assertFalse(EditorSettingsExternalizable.getInstance().isWhitespacesShown)
    assertFalse(optionsIj().list)
  }

  @Test
  fun `test 'list' option reports global intellij setting if not explicitly set`() {
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = true
    assertCommandOutput("set list?", "  list")

    EditorSettingsExternalizable.getInstance().isWhitespacesShown = false
    assertCommandOutput("set list?", "nolist")
  }

  @Test
  fun `test local 'list' option reports global intellij setting if not explicitly set`() {
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = true
    assertCommandOutput("set list?", "  list")

    EditorSettingsExternalizable.getInstance().isWhitespacesShown = false
    assertCommandOutput("set list?", "nolist")
  }

  @Test
  fun `test 'list' option reports local intellij setting if set via IDE`() {
    fixture.editor.settings.isWhitespacesShown = true
    assertCommandOutput("set list?", "  list")

    // View | Active Editor | Show Whitespaces
    fixture.editor.settings.isWhitespacesShown = false
    assertCommandOutput("set list?", "nolist")
  }

  @Test
  fun `test local 'list' option reports local intellij setting if set via IDE`() {
    fixture.editor.settings.isWhitespacesShown = true
    assertCommandOutput("setlocal list?", "  list")

    // View | Active Editor | Show Whitespaces
    fixture.editor.settings.isWhitespacesShown = false
    assertCommandOutput("setlocal list?", "nolist")
  }

  @Test
  fun `test set 'list' modifies local intellij setting only`() {
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = true

    // Note that `:set` modifies both the local and global setting, but that global setting is a Vim setting, not the
    // global IntelliJ setting
    enterCommand("set nolist")
    assertFalse(fixture.editor.settings.isWhitespacesShown)
    assertTrue(EditorSettingsExternalizable.getInstance().isWhitespacesShown)

    enterCommand("set list")
    assertTrue(fixture.editor.settings.isWhitespacesShown)
    assertTrue(EditorSettingsExternalizable.getInstance().isWhitespacesShown)
  }

  @Test
  fun `test setlocal 'list' modifies local intellij setting only`() {
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = true

    // Note that `:set` modifies both the local and global setting, but that global setting is a Vim setting, not the
    // global IntelliJ setting
    enterCommand("setlocal nolist")
    assertFalse(fixture.editor.settings.isWhitespacesShown)
    assertTrue(EditorSettingsExternalizable.getInstance().isWhitespacesShown)

    enterCommand("setlocal list")
    assertTrue(fixture.editor.settings.isWhitespacesShown)
    assertTrue(EditorSettingsExternalizable.getInstance().isWhitespacesShown)
  }

  @Test
  fun `test setglobal 'list' option affects IdeaVim global value only`() {
    assertFalse(IjOptions.list.defaultValue.booleanValue)  // Vim default
    assertCommandOutput("setglobal list?", "nolist")

    enterCommand("setglobal list")
    assertCommandOutput("setglobal list?", "  list")
    assertFalse(EditorSettingsExternalizable.getInstance().isWhitespacesShown)
  }

  @Test
  fun `test set 'list' updates IdeaVim global value as well as local`() {
    enterCommand("set list")
    assertCommandOutput("setglobal list?", "  list")
  }

  @Test
  fun `test set IDE setting is treated like setlocal`() {
    // If we use `:set`, it updates the local and per-window global values. If we set the value from the IDE, it only
    // affects the local value
    // View | Active Editor | Show Whitespaces
    fixture.editor.settings.isWhitespacesShown = true
    assertCommandOutput("setlocal list?", "  list")
    assertCommandOutput("set list?", "  list")
    assertCommandOutput("setglobal list?", "nolist")
  }

  @Test
  fun `test setting global IDE value will not update explicitly set value`() {
    enterCommand("set list")

    EditorSettingsExternalizable.getInstance().isWhitespacesShown = false
    assertCommandOutput("setlocal list?", "  list")
    assertCommandOutput("set list?", "  list")
    assertCommandOutput("setglobal list?", "  list")

    enterCommand("set nolist")

    EditorSettingsExternalizable.getInstance().isWhitespacesShown = true
    assertCommandOutput("setlocal list?", "nolist")
    assertCommandOutput("set list?", "nolist")
    assertCommandOutput("setglobal list?", "nolist")
  }

  @Test
  fun `test setting global IDE value will update effective Vim value set during plugin startup`() {
    // Default value is false. Update the global value to something different, but make sure the Vim options are default
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = true
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runReadAction {
        injector.optionGroup.resetAllOptionsForTesting()
      }
    }

    try {
      injector.optionGroup.startInitVimRc()
      enterCommand("set list")
    } finally {
      injector.optionGroup.endInitVimRc()
    }

    EditorSettingsExternalizable.getInstance().isWhitespacesShown = false
    assertCommandOutput("setlocal list?", "nolist")
    assertCommandOutput("set list?", "nolist")
    assertCommandOutput("setglobal list?", "nolist")
  }

  @Test
  fun `test setglobal does not modify effective value`() {
    enterCommand("setglobal list")
    assertFalse(fixture.editor.settings.isWhitespacesShown)
  }

  @Test
  fun `test setglobal does not modify persistent IDE global value`() {
    enterCommand("setglobal list")
    assertFalse(EditorSettingsExternalizable.getInstance().isWhitespacesShown)
  }

  @Test
  fun `test reset 'list' to default copies current global intellij setting`() {
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = true
    fixture.editor.settings.isWhitespacesShown = false
    assertCommandOutput("set list?", "nolist")

    enterCommand("set list&")
    assertTrue(fixture.editor.settings.isWhitespacesShown)
    assertTrue(EditorSettingsExternalizable.getInstance().isWhitespacesShown)

    // Verify that IntelliJ doesn't allow us to "unset" a local editor setting - it's a copy of the default value
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = false
    assertTrue(fixture.editor.settings.isWhitespacesShown)
  }

  @Test
  fun `test reset local 'list' to default copies current global intellij setting`() {
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = true
    fixture.editor.settings.isWhitespacesShown = false
    assertCommandOutput("set list?", "nolist")

    enterCommand("setlocal list&")
    assertTrue(fixture.editor.settings.isWhitespacesShown)
    assertTrue(EditorSettingsExternalizable.getInstance().isWhitespacesShown)

    // Verify that IntelliJ doesn't allow us to "unset" a local editor setting - it's a copy of the default value
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = false
    assertTrue(fixture.editor.settings.isWhitespacesShown)
  }

  @Test
  fun `test open new window without setting the option copies value as not-explicitly set`() {
    // New window will clone local and global local-to-window options, then apply global to local. This tests that our
    // handling of per-window "global" values is correct.
    assertCommandOutput("set list?", "nolist")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set list?", "nolist")

    // Changing the global setting should update the new editor
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = true
    assertCommandOutput("set list?", "  list")
  }


  @Test
  fun `test open new window after setting option copies value as explicitly set`() {
    enterCommand("set list")
    assertCommandOutput("set list?", "  list")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set list?", "  list")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = false
    assertCommandOutput("set list?", "  list")
  }

  @Test
  fun `test setglobal 'list' used when opening new window`() {
    enterCommand("setglobal list")
    assertCommandOutput("setglobal list?", "  list")
    assertCommandOutput("set list?", "nolist")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set list?", "  list")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = false
    assertCommandOutput("set list?", "  list")
  }

  @Test
  fun `test setlocal 'list' then open new window uses value from setglobal`() {
    enterCommand("setlocal list")
    assertCommandOutput("setglobal list?", "nolist")
    assertCommandOutput("set list?", "  list")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set list?", "nolist")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = true
    assertCommandOutput("set list?", "nolist")
  }

  @Test
  fun `test setting global IDE value will update effective Vim value in new window initialised from value set during startup`() {
    // Default value is false. Update the global value to something different, but make sure the Vim options are default
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = true
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runReadAction {
        injector.optionGroup.resetAllOptionsForTesting()
      }
    }

    try {
      injector.optionGroup.startInitVimRc()
      enterCommand("set list")
    } finally {
      injector.optionGroup.endInitVimRc()
    }

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set list?", "  list")

    // Changing the global setting should update the editor, because it was initially set during plugin startup
    EditorSettingsExternalizable.getInstance().isWhitespacesShown = false
    assertCommandOutput("set list?", "nolist")
  }
}
