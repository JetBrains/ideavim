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
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
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
class BreakIndentOptionMapperTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
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
  fun `test 'breakindent' defaults to current intellij setting`() {
    assertFalse(fixture.editor.settings.isUseCustomSoftWrapIndent)
    assertFalse(optionsIj().breakindent)
  }

  @Test
  fun `test 'breakindent' defaults to global intellij setting`() {
    assertFalse(EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent)
    assertFalse(optionsIj().breakindent)
  }

  @Test
  fun `test 'breakindent' option reports global intellij setting if not explicitly set`() {
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = false
    assertCommandOutput("set breakindent?", "nobreakindent")

    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = true
    assertCommandOutput("set breakindent?", "  breakindent")
  }

  @Test
  fun `test local 'breakindent' option reports global intellij setting if not explicitly set`() {
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = false
    assertCommandOutput("setlocal breakindent?", "nobreakindent")

    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = true
    assertCommandOutput("setlocal breakindent?", "  breakindent")
  }

  @Test
  fun `test 'breakindent' option reports local intellij setting if set via IDE`() {
    fixture.editor.settings.isUseCustomSoftWrapIndent = false
    assertCommandOutput("set breakindent?", "nobreakindent")

    // Note that there is no actual UI in the IDE to set this
    fixture.editor.settings.isUseCustomSoftWrapIndent = true
    assertCommandOutput("set breakindent?", "  breakindent")
  }

  @Test
  fun `test local 'breakindent' option reports local intellij setting if set via IDE`() {
    fixture.editor.settings.isUseCustomSoftWrapIndent = false
    assertCommandOutput("setlocal breakindent?", "nobreakindent")

    // Note that there is no actual UI in the IDE to set this
    fixture.editor.settings.isUseCustomSoftWrapIndent = true
    assertCommandOutput("setlocal breakindent?", "  breakindent")
  }

  @Test
  fun `test set 'breakindent' modifies local intellij setting only`() {
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = true

    // Note that `:set` modifies both the local and global setting, but that global setting is a Vim setting, not the
    // global IntelliJ setting
    enterCommand("set nobreakindent")
    assertFalse(fixture.editor.settings.isUseCustomSoftWrapIndent)
    assertTrue(EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent)

    enterCommand("set breakindent")
    assertTrue(fixture.editor.settings.isUseCustomSoftWrapIndent)
    assertTrue(EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent)
  }

  @Test
  fun `test setlocal 'breakindent' modifies local intellij setting only`() {
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = true

    enterCommand("setlocal nobreakindent")
    assertFalse(fixture.editor.settings.isUseCustomSoftWrapIndent)
    assertTrue(EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent)

    enterCommand("setlocal breakindent")
    assertTrue(fixture.editor.settings.isUseCustomSoftWrapIndent)
    assertTrue(EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent)
  }

  @Test
  fun `test setglobal 'breakindent' option affects IdeaVim global value only`() {
    assertFalse(IjOptions.breakindent.defaultValue.asBoolean()) // Vim default
    assertCommandOutput("setglobal breakindent?", "nobreakindent")

    enterCommand("setglobal breakindent")
    assertCommandOutput("setglobal breakindent?", "  breakindent")
    assertFalse(EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent)
  }

  @Test
  fun `test set updates IdeaVim global value as well as local`() {
    enterCommand("set breakindent")
    assertCommandOutput("setglobal breakindent?", "  breakindent")
  }

  @Test
  fun `test setting local IDE value is treated like setlocal`() {
    // If we use `:set`, it updates the local and per-window global values. If we set the value from the IDE, it only
    // affects the local value
    // Note that there is no actual UI in the IDE to set this
    fixture.editor.settings.isUseCustomSoftWrapIndent = true
    assertCommandOutput("setlocal breakindent?", "  breakindent")
    assertCommandOutput("set breakindent?", "  breakindent")
    assertCommandOutput("setglobal breakindent?", "nobreakindent")
  }

  @Test
  fun `test setting global IDE value will not update explicitly set value`() {
    enterCommand("set breakindent")

    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = false
    assertCommandOutput("setlocal breakindent?", "  breakindent")
    assertCommandOutput("set breakindent?", "  breakindent")
    assertCommandOutput("setglobal breakindent?", "  breakindent")

    enterCommand("set nobreakindent")

    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = true
    assertCommandOutput("setlocal breakindent?", "nobreakindent")
    assertCommandOutput("set breakindent?", "nobreakindent")
    assertCommandOutput("setglobal breakindent?", "nobreakindent")
  }

  @Test
  fun `test setting global IDE value will update effective Vim value set during plugin startup`() {
    // Default value is false. Update the global value to something different, but make sure the Vim options are default
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = true
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runReadAction {
        injector.optionGroup.resetAllOptionsForTesting()
      }
    }

    try {
      injector.optionGroup.startInitVimRc()

      // This is the same value as the global IDE setting. That's ok. We just want to explicitly set the Vim option
      enterCommand("set breakindent")
    } finally {
      injector.optionGroup.endInitVimRc()
    }

    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = false
    assertCommandOutput("setlocal breakindent?", "nobreakindent")
    assertCommandOutput("set breakindent?", "nobreakindent")
    assertCommandOutput("setglobal breakindent?", "nobreakindent")
  }

  @Test
  fun `test setglobal does not modify effective value`() {
    enterCommand("setglobal breakindent")
    assertFalse(fixture.editor.settings.isUseCustomSoftWrapIndent)
  }

  @Test
  fun `test setglobal does not modify persistent IDE global value`() {
    enterCommand("setglobal breakindent")
    assertFalse(EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent)
  }

  @Test
  fun `test reset 'breakindent' to default copies current global intellij setting`() {
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = true
    fixture.editor.settings.isUseCustomSoftWrapIndent = false
    assertCommandOutput("set breakindent?", "nobreakindent")

    enterCommand("set breakindent&")
    assertTrue(fixture.editor.settings.isUseCustomSoftWrapIndent)
    assertTrue(EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent)

    // Verify that IntelliJ doesn't allow us to "unset" a local editor setting - it's a copy of the default value
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = false
    assertTrue(fixture.editor.settings.isUseCustomSoftWrapIndent)
  }

  @Test
  fun `test reset local 'breakindent' to default copies current global intellij setting`() {
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = true
    fixture.editor.settings.isUseCustomSoftWrapIndent = false
    assertCommandOutput("set breakindent?", "nobreakindent")

    enterCommand("setlocal breakindent&")
    assertTrue(fixture.editor.settings.isUseCustomSoftWrapIndent)
    assertTrue(EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent)

    // Verify that IntelliJ doesn't allow us to "unset" a local editor setting - it's a copy of the default value
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = false
    assertTrue(fixture.editor.settings.isUseCustomSoftWrapIndent)
  }

  @Test
  fun `test open new window without setting the option copies value as not-explicitly set`() {
    // New window will clone local and global local-to-window options, then apply global to local. This tests that our
    // handling of per-window "global" values is correct.
    assertCommandOutput("set breakindent?", "nobreakindent")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set breakindent?", "nobreakindent")

    // Changing the global setting should update the new editor
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = true
    assertCommandOutput("set breakindent?", "  breakindent")
  }

  @Test
  fun `test open new window after setting option copies value as explicitly set`() {
    enterCommand("set breakindent")
    assertCommandOutput("set breakindent?", "  breakindent")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set breakindent?", "  breakindent")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = false
    assertCommandOutput("set breakindent?", "  breakindent")
  }

  @Test
  fun `test setglobal 'breakindent' used when opening new window`() {
    enterCommand("setglobal breakindent")
    assertCommandOutput("setglobal breakindent?", "  breakindent")
    assertCommandOutput("set breakindent?", "nobreakindent")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set breakindent?", "  breakindent")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = false
    assertCommandOutput("set breakindent?", "  breakindent")
  }

  @Test
  fun `test setlocal 'breakindent' then open new window uses value from setglobal`() {
    enterCommand("setlocal breakindent")
    assertCommandOutput("setglobal breakindent?", "nobreakindent")
    assertCommandOutput("set breakindent?", "  breakindent")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set breakindent?", "nobreakindent")
    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = true
    assertCommandOutput("set breakindent?", "nobreakindent")
  }

  @Test
  fun `test setting global IDE value will update effective Vim value in new window initialised from value set during startup`() {
    // Default value is false. Update the global value to something different, but make sure the Vim options are default
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = true
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runReadAction {
        injector.optionGroup.resetAllOptionsForTesting()
      }
    }

    try {
      injector.optionGroup.startInitVimRc()

      // This is the same value as the global IDE setting. That's ok. We just want to explicitly set the Vim option
      enterCommand("set breakindent")
    } finally {
      injector.optionGroup.endInitVimRc()
    }

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set breakindent?", "  breakindent")

    // Changing the global setting should update the editor, because it was initially set during plugin startup
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent = false
    assertCommandOutput("set breakindent?", "nobreakindent")
  }
}
