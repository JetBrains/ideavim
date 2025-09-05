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
import com.intellij.openapi.editor.impl.SettingsImpl
import com.maddyhome.idea.vim.group.IjOptions
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class CursorLineOptionMapperTest : VimTestCase() {
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
  fun `test 'cursorline' defaults to current intellij setting`() {
    assertEquals(IjOptions.cursorline.defaultValue.booleanValue, fixture.editor.settings.isCaretRowShown)
    assertEquals(IjOptions.cursorline.defaultValue.booleanValue, optionsIj().cursorline)
  }

  @Test
  fun `test 'cursorline' defaults to global intellij setting`() {
    ApplicationManager.getApplication().invokeAndWait {
      (fixture.editor.settings as SettingsImpl).getState().apply { clearOverriding(this::myCaretRowShown) }
      assertTrue(EditorSettingsExternalizable.getInstance().isCaretRowShown)
      assertTrue(optionsIj().cursorline)
    }
  }

  @Test
  fun `test 'cursorline' option reports global intellij setting if not explicitly set`() {
    ApplicationManager.getApplication().invokeAndWait {
      (fixture.editor.settings as SettingsImpl).getState().apply { clearOverriding(this::myCaretRowShown) }
    }
    assertTrue(EditorSettingsExternalizable.getInstance().isCaretRowShown)
    assertCommandOutput("set cursorline?", "  cursorline")
  }

  @Test
  fun `test local 'cursorline' option reports global intellij setting if not explicitly set`() {
    ApplicationManager.getApplication().invokeAndWait {
      (fixture.editor.settings as SettingsImpl).getState().apply { clearOverriding(this::myCaretRowShown) }
      assertTrue(EditorSettingsExternalizable.getInstance().isCaretRowShown)
      assertCommandOutput("setlocal cursorline?", "  cursorline")
    }
  }

  @Test
  fun `test 'cursorline' option reports local intellij setting if set via IDE`() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.settings.isCaretRowShown = false
      assertCommandOutput("set cursorline?", "nocursorline")

      fixture.editor.settings.isCaretRowShown = true
      assertCommandOutput("set cursorline?", "  cursorline")
    }
  }

  @Test
  fun `test local 'cursorline' option reports local intellij setting if set via IDE`() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.settings.isCaretRowShown = false
      assertCommandOutput("setlocal cursorline?", "nocursorline")

      fixture.editor.settings.isCaretRowShown = true
      assertCommandOutput("setlocal cursorline?", "  cursorline")
    }
  }

  @Test
  fun `test set 'cursorline' modifies local intellij setting only`() {
    assertTrue(EditorSettingsExternalizable.getInstance().isCaretRowShown)

    // Note that `:set` modifies both the local and global setting, but that global setting is a Vim setting, not the
    // global IntelliJ setting
    enterCommand("set nocursorline")
    assertFalse(fixture.editor.settings.isCaretRowShown)
    assertTrue(EditorSettingsExternalizable.getInstance().isCaretRowShown)

    enterCommand("set cursorline")
    assertTrue(fixture.editor.settings.isCaretRowShown)
    assertTrue(EditorSettingsExternalizable.getInstance().isCaretRowShown)
  }

  @Test
  fun `test setlocal 'cursorline' modifies local intellij setting only`() {
    assertTrue(EditorSettingsExternalizable.getInstance().isCaretRowShown)

    enterCommand("setlocal nocursorline")
    assertFalse(fixture.editor.settings.isCaretRowShown)
    assertTrue(EditorSettingsExternalizable.getInstance().isCaretRowShown)

    enterCommand("setlocal cursorline")
    assertTrue(fixture.editor.settings.isCaretRowShown)
    assertTrue(EditorSettingsExternalizable.getInstance().isCaretRowShown)
  }

  @Test
  fun `test setglobal 'cursorline' option affects IdeaVim global value only`() {
    assertFalse(IjOptions.cursorline.defaultValue.booleanValue)
    assertCommandOutput("setglobal cursorline?", "nocursorline")

    enterCommand("setglobal cursorline")
    assertCommandOutput("setglobal cursorline?", "  cursorline")

    enterCommand("setglobal nocursorline")
    assertCommandOutput("setglobal cursorline?", "nocursorline")
    assertTrue(EditorSettingsExternalizable.getInstance().isCaretRowShown)
  }

  @Test
  fun `test set updateds IdeaVim global value as well as local`() {
    enterCommand("set cursorline")
    assertCommandOutput("setglobal cursorline?", "  cursorline")
  }

  @Test
  fun `test setting IDE value is treated like setlocal`() {
    // If we use `:set`, it updates the local and per-window global values. If we set the value from the IDE, it only
    // affects the local value
    enterCommand("setglobal cursorline")
    fixture.editor.settings.isCaretRowShown = false
    assertCommandOutput("setlocal cursorline?", "nocursorline")
    assertCommandOutput("set cursorline?", "nocursorline")
    assertCommandOutput("setglobal cursorline?", "  cursorline")
  }

  @Test
  fun `test setglobal does not modify effective value`() {
    assertEquals(IjOptions.cursorline.defaultValue.booleanValue, fixture.editor.settings.isCaretRowShown)
    enterCommand("setglobal nocursorline")
    assertEquals(IjOptions.cursorline.defaultValue.booleanValue, fixture.editor.settings.isCaretRowShown)
  }

  @Test
  fun `test setglobal does not modify persistent IDE global value`() {
    enterCommand("setglobal nocursorline")
    assertTrue(EditorSettingsExternalizable.getInstance().isCaretRowShown)
  }

  @Test
  fun `test rest 'cursorline' to default copies current global intellij setting`() {
    fixture.editor.settings.isCaretRowShown = false
    assertCommandOutput("set cursorline?", "nocursorline")

    enterCommand("set cursorline&")
    assertTrue(fixture.editor.settings.isCaretRowShown)
    assertTrue(EditorSettingsExternalizable.getInstance().isCaretRowShown)

    // We can't verify that IntelliJ doesn't allow us to "unset" a value because we can't change the global value
  }

  @Test
  fun `test reset local 'cursorline' to default copies current global intellij setting`() {
    fixture.editor.settings.isCaretRowShown = false
    assertCommandOutput("set cursorline?", "nocursorline")

    enterCommand("setlocal cursorline&")
    assertTrue(fixture.editor.settings.isCaretRowShown)
    assertTrue(EditorSettingsExternalizable.getInstance().isCaretRowShown)

    // We can't verify that IntelliJ doesn't allow us to "unset" a value because we can't change the global value
  }

  @Test
  fun `test open new window without setting the option copies value as not-explicitly set`() {
    // New window will clone local and global local-to-window options, then apply global to local. This tests that our
    // handling of per-window "global" values is correct.
    ApplicationManager.getApplication().invokeAndWait {
      (fixture.editor.settings as SettingsImpl).getState().apply { clearOverriding(this::myCaretRowShown) }
      assertCommandOutput("set cursorline?", "  cursorline")

      switchToNewFile("bbb.txt", "lorem ipsum")

      assertCommandOutput("set cursorline?", "  cursorline")

      // Can't prove that it was copied as a default value because we can't change the global value
    }
  }

  @Test
  fun `test open new window after setting option copies value as explicitly set`() {
    enterCommand("set nocursorline")
    assertCommandOutput("set cursorline?", "nocursorline")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set cursorline?", "nocursorline")

    // Can't prove that it was copied as a default value because we can't change the global value and see it update
  }

  @Test
  fun `test setglobal 'cursorline' used when opening new window`() {
    enterCommand("setglobal cursorline")
    assertCommandOutput("setglobal cursorline?", "  cursorline")
    assertCommandOutput("set cursorline?", "nocursorline")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set cursorline?", "  cursorline")

    // Can't prove that it was copied as a locally set value because we can't change the global value
  }

  @Test
  fun `test setlocal 'cursorline' then open new window uses value from setglobal`() {
    enterCommand("setlocal nocursorline")
    assertCommandOutput("setglobal cursorline?", "nocursorline")
    assertCommandOutput("set cursorline?", "nocursorline")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set cursorline?", "  cursorline")

    // Can't prove that it was copied as a locally set value because we can't change the global value
  }
}
