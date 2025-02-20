/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option.overrides

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorSettings.LineNumerationType
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class LineNumberOptionsMapperTest : VimTestCase() {
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
  fun `test 'number' and 'relativenumber' default to current intellij settings`() {
    assertFalse(fixture.editor.settings.isLineNumbersShown)
    assertFalse(options().number)
    assertFalse(optionsIj().relativenumber)
  }

  @Test
  fun `test 'number' and 'relativenumber' defaults to global intellij settings`() {
    assertFalse(EditorSettingsExternalizable.getInstance().isLineNumbersShown)
    assertFalse(options().number)
    assertFalse(optionsIj().relativenumber)
  }

  @Test
  fun `test 'number' and 'relativenumber' options reports global intellij setting if not explicitly set`() {
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().isLineNumbersShown = false
      assertCommandOutput("set number?", "nonumber")
      assertCommandOutput("set relativenumber?", "norelativenumber")

      EditorSettingsExternalizable.getInstance().isLineNumbersShown = true
      EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.HYBRID
      assertCommandOutput("set number?", "  number")
      assertCommandOutput("set relativenumber?", "  relativenumber")
    }
  }

  @Test
  fun `test local 'number' and 'relativenumber' options reports global intellij setting if not explicitly set`() {
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().isLineNumbersShown = false
      assertCommandOutput("setlocal number?", "nonumber")
      assertCommandOutput("setlocal relativenumber?", "norelativenumber")

      EditorSettingsExternalizable.getInstance().isLineNumbersShown = true
      EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.HYBRID
      assertCommandOutput("setlocal number?", "  number")
      assertCommandOutput("setlocal relativenumber?", "  relativenumber")
    }
  }

  @Test
  fun `test 'number' and 'relativenumber' options report local intellij settings if set via IDE`() {
    fixture.editor.settings.isLineNumbersShown = false
    assertCommandOutput("set number?", "nonumber")
    assertCommandOutput("set relativenumber?", "norelativenumber")

    // View | Active Editor | Show Line Numbers. There is no UI for line numeration type
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.settings.isLineNumbersShown = true
      fixture.editor.settings.lineNumerationType = LineNumerationType.HYBRID
      assertCommandOutput("set number?", "  number")
      assertCommandOutput("set relativenumber?", "  relativenumber")

      fixture.editor.settings.lineNumerationType = LineNumerationType.ABSOLUTE
      assertCommandOutput("set number?", "  number")
      assertCommandOutput("set relativenumber?", "norelativenumber")

      fixture.editor.settings.lineNumerationType = LineNumerationType.RELATIVE
      assertCommandOutput("set number?", "nonumber")
      assertCommandOutput("set relativenumber?", "  relativenumber")
    }
  }

  @Test
  fun `test local 'number' and 'relativenumber' options report local intellij settings if set via IDE`() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.settings.isLineNumbersShown = false
      assertCommandOutput("setlocal number?", "nonumber")
      assertCommandOutput("setlocal relativenumber?", "norelativenumber")

      // View | Active Editor | Show Line Numbers. There is no UI for line numeration type
      fixture.editor.settings.isLineNumbersShown = true
      fixture.editor.settings.lineNumerationType = LineNumerationType.HYBRID
      assertCommandOutput("setlocal number?", "  number")
      assertCommandOutput("setlocal relativenumber?", "  relativenumber")

      fixture.editor.settings.lineNumerationType = LineNumerationType.ABSOLUTE
      assertCommandOutput("setlocal number?", "  number")
      assertCommandOutput("setlocal relativenumber?", "norelativenumber")

      fixture.editor.settings.lineNumerationType = LineNumerationType.RELATIVE
      assertCommandOutput("setlocal number?", "nonumber")
      assertCommandOutput("setlocal relativenumber?", "  relativenumber")
    }
  }

  @Test
  fun `test set 'number' enables absolute numbers for local intellij setting only`() {
    // Note that `:set` modifies both the local and global setting, but that global setting is a Vim setting, not the
    // global IntelliJ setting
    enterCommand("set number")
    assertTrue(fixture.editor.settings.isLineNumbersShown)
    assertEquals(LineNumerationType.ABSOLUTE, fixture.editor.settings.lineNumerationType)
    assertFalse(EditorSettingsExternalizable.getInstance().isLineNumbersShown)

    enterCommand("set nonumber")
    assertFalse(fixture.editor.settings.isLineNumbersShown)
  }

  @Test
  fun `test set 'relativenumber' enables relative numbers for local intellij setting only`() {
    // Note that `:set` modifies both the local and global setting, but that global setting is a Vim setting, not the
    // global IntelliJ setting
    enterCommand("set relativenumber")
    assertTrue(fixture.editor.settings.isLineNumbersShown)
    assertEquals(LineNumerationType.RELATIVE, fixture.editor.settings.lineNumerationType)
    assertFalse(EditorSettingsExternalizable.getInstance().isLineNumbersShown)

    enterCommand("set norelativenumber")
    assertFalse(fixture.editor.settings.isLineNumbersShown)
  }

  @Test
  fun `test set 'number' and 'relativenumber' enables hybrid line numbers for local intellij setting only`() {
    enterCommand("set number relativenumber")
    assertTrue(fixture.editor.settings.isLineNumbersShown)
    assertEquals(LineNumerationType.HYBRID, fixture.editor.settings.lineNumerationType)
    assertFalse(EditorSettingsExternalizable.getInstance().isLineNumbersShown)
  }

  @Test
  fun `test set local 'number' enables absolute numbers for local intellij setting only`() {
    // Note that `:set` modifies both the local and global setting, but that global setting is a Vim setting, not the
    // global IntelliJ setting
    enterCommand("setlocal number")
    assertTrue(fixture.editor.settings.isLineNumbersShown)
    assertEquals(LineNumerationType.ABSOLUTE, fixture.editor.settings.lineNumerationType)
    assertFalse(EditorSettingsExternalizable.getInstance().isLineNumbersShown)

    enterCommand("setlocal nonumber")
    assertFalse(fixture.editor.settings.isLineNumbersShown)
  }

  @Test
  fun `test set local 'relativenumber' enables relative numbers for local intellij setting only`() {
    // Note that `:set` modifies both the local and global setting, but that global setting is a Vim setting, not the
    // global IntelliJ setting
    enterCommand("setlocal relativenumber")
    assertTrue(fixture.editor.settings.isLineNumbersShown)
    assertEquals(LineNumerationType.RELATIVE, fixture.editor.settings.lineNumerationType)
    assertFalse(EditorSettingsExternalizable.getInstance().isLineNumbersShown)

    enterCommand("setlocal norelativenumber")
    assertFalse(fixture.editor.settings.isLineNumbersShown)
  }

  @Test
  fun `test set local 'number' and 'relativenumber' enables hybrid line numbers for local intellij setting only`() {
    enterCommand("setlocal number relativenumber")
    assertTrue(fixture.editor.settings.isLineNumbersShown)
    assertEquals(LineNumerationType.HYBRID, fixture.editor.settings.lineNumerationType)
    assertFalse(EditorSettingsExternalizable.getInstance().isLineNumbersShown)
  }

  @Test
  fun `test set global 'number' option affects IdeaVim global value only`() {
    assertFalse(Options.number.defaultValue.asBoolean())
    assertCommandOutput("setglobal number?", "nonumber")

    enterCommand("setglobal number")
    assertCommandOutput("setglobal number?", "  number")
    assertFalse(EditorSettingsExternalizable.getInstance().isLineNumbersShown)
    assertFalse(fixture.editor.settings.isLineNumbersShown)
  }

  @Test
  fun `test set global 'relativenumber' option affects IdeaVim global value only`() {
    assertFalse(Options.number.defaultValue.asBoolean())
    assertCommandOutput("setglobal relativenumber?", "norelativenumber")

    enterCommand("setglobal relativenumber")
    assertCommandOutput("setglobal relativenumber?", "  relativenumber")
    assertFalse(EditorSettingsExternalizable.getInstance().isLineNumbersShown)
    assertFalse(fixture.editor.settings.isLineNumbersShown)
  }

  @Test
  fun `test set 'number' updates IdeaVim global value as well as local`() {
    enterCommand("set number")
    assertCommandOutput("setglobal number?", "  number")
  }

  @Test
  fun `test set 'relativenumber' updates IdeaVim global value as well as local`() {
    enterCommand("set relativenumber")
    assertCommandOutput("setglobal relativenumber?", "  relativenumber")
  }

  @Test
  fun `test setting IDE value is treated like setlocal`() {
    // If we use `:set`, it updates the local and per-window global values. If we set the value from the IDE, it only
    // affects the local value
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.settings.isLineNumbersShown = true
      fixture.editor.settings.lineNumerationType = LineNumerationType.HYBRID
    }

    assertCommandOutput("setlocal number?", "  number")
    assertCommandOutput("set number?", "  number")
    assertCommandOutput("setglobal number?", "nonumber")

    assertCommandOutput("setlocal relativenumber?", "  relativenumber")
    assertCommandOutput("set relativenumber?", "  relativenumber")
    assertCommandOutput("setglobal relativenumber?", "norelativenumber")
  }

  @Test
  fun `test setting global whitespace IDE value will not update explicitly set value`() {
    enterCommand("set number relativenumber")

    EditorSettingsExternalizable.getInstance().isLineNumbersShown = false
    assertCommandOutput("setlocal number?", "  number")
    assertCommandOutput("set number?", "  number")
    assertCommandOutput("setglobal number?", "  number")

    enterCommand("set nonumber")

    EditorSettingsExternalizable.getInstance().isLineNumbersShown = true
    assertCommandOutput("setlocal number?", "nonumber")
    assertCommandOutput("set number?", "nonumber")
    assertCommandOutput("setglobal number?", "nonumber")
  }

  @Test
  fun `test setting global numeration type IDE value will not update explicitly set value`() {
    enterCommand("set number relativenumber")

    EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.ABSOLUTE
    assertCommandOutput("setlocal relativenumber?", "  relativenumber")
    assertCommandOutput("set relativenumber?", "  relativenumber")
    assertCommandOutput("setglobal relativenumber?", "  relativenumber")

    enterCommand("set norelativenumber")

    EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.HYBRID
    assertCommandOutput("setlocal relativenumber?", "norelativenumber")
    assertCommandOutput("set relativenumber?", "norelativenumber")
    assertCommandOutput("setglobal relativenumber?", "norelativenumber")
  }

  @Test
  fun `test setting global whitespace IDE value will update effective Vim value set during plugin startup`() {
    // Default value is false. Update the global value to something different, but make sure the Vim options are default
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().isLineNumbersShown = true
      injector.optionGroup.resetAllOptionsForTesting()

      try {
        injector.optionGroup.startInitVimRc()

        // This is the same value as the global IDE setting. That's ok. We just want to explicitly set the Vim option
        enterCommand("set number")
      } finally {
        injector.optionGroup.endInitVimRc()
      }

      EditorSettingsExternalizable.getInstance().isLineNumbersShown = false
      assertCommandOutput("setlocal number?", "nonumber")
      assertCommandOutput("set number?", "nonumber")
      assertCommandOutput("setglobal number?", "nonumber")
    }
  }

  @Test
  fun `test setting global numeration type IDE value will update effective Vim value set during plugin startup`() {
    // Default value is ABSOLUTE. Update the global value to something different, but make sure the Vim options are default
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.RELATIVE
      injector.optionGroup.resetAllOptionsForTesting()

      try {
        injector.optionGroup.startInitVimRc()
        enterCommand("set relativenumber")
      } finally {
        injector.optionGroup.endInitVimRc()
      }

      EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.ABSOLUTE
      assertCommandOutput("setlocal relativenumber?", "norelativenumber")
      assertCommandOutput("set relativenumber?", "norelativenumber")
      assertCommandOutput("setglobal relativenumber?", "norelativenumber")
    }
  }

  @Test
  fun `test reset 'number' to default copies current global intellij setting`() {
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().isLineNumbersShown = true
      EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.ABSOLUTE
      fixture.editor.settings.isLineNumbersShown = false
      assertCommandOutput("set number?", "nonumber")

      enterCommand("set number&")
      assertTrue(fixture.editor.settings.isLineNumbersShown)
      assertTrue(EditorSettingsExternalizable.getInstance().isLineNumbersShown)
      assertEquals(LineNumerationType.ABSOLUTE, EditorSettingsExternalizable.getInstance().lineNumeration)

      // Verify that IntelliJ doesn't allow us to "unset" a local editor setting - it's a copy of the default value
      EditorSettingsExternalizable.getInstance().isLineNumbersShown = false
      assertTrue(fixture.editor.settings.isLineNumbersShown)
    }
  }

  @Test
  fun `test reset 'relativenumber' to default copies current global intellij setting`() {
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runReadAction {
        EditorSettingsExternalizable.getInstance().isLineNumbersShown = true
        EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.RELATIVE
        injector.optionGroup.resetAllOptionsForTesting()  // So changing the global values does not modify IdeaVim's values

        // Value becomes External(true) because the user is explicitly setting it
        fixture.editor.settings.isLineNumbersShown = false
        assertCommandOutput("set relativenumber?", "norelativenumber")

        enterCommand("set relativenumber&")
        assertTrue(fixture.editor.settings.isLineNumbersShown)
        assertTrue(EditorSettingsExternalizable.getInstance().isLineNumbersShown)
        assertEquals(LineNumerationType.RELATIVE, EditorSettingsExternalizable.getInstance().lineNumeration)

        // Changing the global value should not update local value, because the user has explicitly changed it
        EditorSettingsExternalizable.getInstance().isLineNumbersShown = false
        assertTrue(fixture.editor.settings.isLineNumbersShown)
      }
    }
  }

  @Test
  fun `test local reset 'number' to default copies current global intellij setting`() {
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().isLineNumbersShown = true
      EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.ABSOLUTE
      fixture.editor.settings.isLineNumbersShown = false
      assertCommandOutput("set number?", "nonumber")

      enterCommand("setlocal number&")
      assertTrue(fixture.editor.settings.isLineNumbersShown)
      assertTrue(EditorSettingsExternalizable.getInstance().isLineNumbersShown)
      assertEquals(LineNumerationType.ABSOLUTE, EditorSettingsExternalizable.getInstance().lineNumeration)

      // Verify that IntelliJ doesn't allow us to "unset" a local editor setting - it's a copy of the default value
      EditorSettingsExternalizable.getInstance().isLineNumbersShown = false
      assertTrue(fixture.editor.settings.isLineNumbersShown)
    }
  }

  @Test
  fun `test reset local 'relativenumber' to default copies current global intellij setting`() {
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().isLineNumbersShown = true
      EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.RELATIVE
      injector.optionGroup.resetAllOptionsForTesting()  // So changing the global values does not modify IdeaVim's values

      // Value becomes External(true) because the user is explicitly setting it
      fixture.editor.settings.isLineNumbersShown = false
      assertCommandOutput("set relativenumber?", "norelativenumber")

      enterCommand("setlocal relativenumber&")
      assertTrue(fixture.editor.settings.isLineNumbersShown)
      assertTrue(EditorSettingsExternalizable.getInstance().isLineNumbersShown)
      assertEquals(LineNumerationType.RELATIVE, EditorSettingsExternalizable.getInstance().lineNumeration)

      // Changing the global value should not update local value, because the user has explicitly changed it
      EditorSettingsExternalizable.getInstance().isLineNumbersShown = false
      assertTrue(fixture.editor.settings.isLineNumbersShown)
    }
  }

  @Test
  fun `test open new window without setting 'number' copies value as not-explicitly set`() {
    // New window will clone local and global local-to-window options, then apply global to local. This tests that our
    // handling of per-window "global" values is correct.
    assertCommandOutput("set number?", "nonumber")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set number?", "nonumber")

    // Changing the global setting should update the new editor
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().isLineNumbersShown = true
      EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.ABSOLUTE
    }
    assertCommandOutput("set number?", "  number")
  }

  @Test
  fun `test open new window without setting 'relativenumber' copies value as not-explicitly set`() {
    // New window will clone local and global local-to-window options, then apply global to local. This tests that our
    // handling of per-window "global" values is correct.
    ApplicationManager.getApplication().invokeAndWait {
      assertCommandOutput("set relativenumber?", "norelativenumber")

      switchToNewFile("bbb.txt", "lorem ipsum")

      assertCommandOutput("set relativenumber?", "norelativenumber")

      // Changing the global setting should update the new editor
      EditorSettingsExternalizable.getInstance().isLineNumbersShown = true
      EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.RELATIVE
      assertCommandOutput("set relativenumber?", "  relativenumber")
    }
  }

  @Test
  fun `test open new window after setting 'number' copies value as explicitly set`() {
    enterCommand("set number")
    assertCommandOutput("set number?", "  number")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set number?", "  number")

    // Changing the global setting should NOT update the new editor
    EditorSettingsExternalizable.getInstance().isLineNumbersShown = false
    assertCommandOutput("set number?", "  number")
  }

  @Test
  fun `test open new window after setting 'relativenumber' copies value as explicitly set`() {
    enterCommand("set relativenumber")
    assertCommandOutput("set relativenumber?", "  relativenumber")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set relativenumber?", "  relativenumber")

    // Changing the global setting should NOT update the new editor
    EditorSettingsExternalizable.getInstance().isLineNumbersShown = false
    assertCommandOutput("set relativenumber?", "  relativenumber")
  }

  @Test
  fun `test setglobal 'number' used when opening new window`() {
    enterCommand("setglobal number")
    assertCommandOutput("setglobal number?", "  number")
    assertCommandOutput("set number?", "nonumber")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set number?", "  number")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isLineNumbersShown = false
    assertCommandOutput("set number?", "  number")
  }

  @Test
  fun `test setglobal 'relativenumber' used when opening new window`() {
    enterCommand("setglobal relativenumber")
    assertCommandOutput("setglobal relativenumber?", "  relativenumber")
    assertCommandOutput("set relativenumber?", "norelativenumber")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set relativenumber?", "  relativenumber")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isLineNumbersShown = false
    assertCommandOutput("set relativenumber?", "  relativenumber")
  }

  @Test
  fun `test setlocal 'number' then open new window uses value from setglobal`() {
    enterCommand("setlocal number")
    assertCommandOutput("setglobal number?", "nonumber")
    assertCommandOutput("set number?", "  number")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set number?", "nonumber")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isLineNumbersShown = true
    assertCommandOutput("set number?", "nonumber")
  }

  @Test
  fun `test setlocal 'relativenumber' then open new window uses value from setglobal`() {
    enterCommand("setlocal relativenumber")
    assertCommandOutput("setglobal relativenumber?", "norelativenumber")
    assertCommandOutput("set relativenumber?", "  relativenumber")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set relativenumber?", "norelativenumber")

    // Changing the global setting should NOT update the editor
    EditorSettingsExternalizable.getInstance().isLineNumbersShown = true
    assertCommandOutput("set relativenumber?", "norelativenumber")
  }

  @Test
  fun `test setting global whitespace IDE value will update effective Vim value in new window initialised during startup`() {
    // Default value is false. Update the global value to something different, but make sure the Vim options are default
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().isLineNumbersShown = true
      injector.optionGroup.resetAllOptionsForTesting()

      try {
        injector.optionGroup.startInitVimRc()
        enterCommand("set number")
      } finally {
        injector.optionGroup.endInitVimRc()
      }

      switchToNewFile("bbb.txt", "lorem ipsum")
      assertCommandOutput("set number?", "  number")

      EditorSettingsExternalizable.getInstance().isLineNumbersShown = false
      assertCommandOutput("setlocal number?", "nonumber")
      assertCommandOutput("set number?", "nonumber")
      assertCommandOutput("setglobal number?", "nonumber")
    }
  }

  @Test
  fun `test setting global numeration type IDE value will update effective Vim value in new window initialised during startup`() {
    // Default value is ABSOLUTE. Update the global value to something different, but make sure the Vim options are default
    ApplicationManager.getApplication().invokeAndWait {
      EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.RELATIVE
      injector.optionGroup.resetAllOptionsForTesting()

      try {
        injector.optionGroup.startInitVimRc()
        enterCommand("set relativenumber")
      } finally {
        injector.optionGroup.endInitVimRc()
      }

      switchToNewFile("bbb.txt", "lorem ipsum")
      assertCommandOutput("set relativenumber?", "  relativenumber")

      EditorSettingsExternalizable.getInstance().lineNumeration = LineNumerationType.ABSOLUTE
      assertCommandOutput("setlocal relativenumber?", "norelativenumber")
      assertCommandOutput("set relativenumber?", "norelativenumber")
      assertCommandOutput("setglobal relativenumber?", "norelativenumber")
    }
  }
}
