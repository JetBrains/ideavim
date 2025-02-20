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
import com.maddyhome.idea.vim.newapi.ij
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class SideScrollOptionMapperTest : VimTestCase() {
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
  fun `test 'sidescroll' global option defaults to current intellij setting`() {
    assertEquals(0, fixture.editor.settings.horizontalScrollJump)
    assertEquals(0, options().sidescroll)
  }

  @Test
  fun `test 'sidescroll' defaults to global intellij setting`() {
    assertEquals(0, EditorSettingsExternalizable.getInstance().horizontalScrollJump)
    assertEquals(0, options().sidescroll)
  }

  @Test
  fun `test 'sidescroll' option reports global intellij setting if not explicitly set`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollJump = 7
    assertCommandOutput("set sidescroll?", "  sidescroll=7")

    // Also tests the value being modified in the IDE's settings
    EditorSettingsExternalizable.getInstance().horizontalScrollJump = 3
    assertCommandOutput("set sidescroll?", "  sidescroll=3")
  }

  // 'sidescroll' is a global option, so we don't need to test `:setlocal` or `:setglobal`

  @Test
  fun `test 'sidescroll' option reports local intellij setting if not explicitly set`() {
    // 'sidescroll' is a global option, but IntelliJ's setting is global-local. To prevent IntelliJ using incorrect
    // values for scrolling during typing (when IdeaVim's scroll implementation isn't called early enough), we set the
    // local value for all editors
    fixture.editor.settings.horizontalScrollJump = 7
    assertCommandOutput("set sidescroll?", "  sidescroll=7")

    // Also tests the value being modified in the IDE's settings
    fixture.editor.settings.horizontalScrollJump = 3
    assertCommandOutput("set sidescroll?", "  sidescroll=3")
  }

  @Test
  fun `test setting 'sidescroll' modifies local intellij setting only`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollJump = 10
    fixture.editor.settings.horizontalScrollJump = 20

    enterCommand("set sidescroll=7")
    assertEquals(7, fixture.editor.settings.horizontalScrollJump)
    assertEquals(10, EditorSettingsExternalizable.getInstance().horizontalScrollJump)
  }

  @Test
  fun `test setting global IDE value will update IdeaVim value`() {
    enterCommand("set sidescroll=7")
    EditorSettingsExternalizable.getInstance().horizontalScrollJump = 3
    assertCommandOutput("setlocal sidescroll?", "  sidescroll=3")
    assertCommandOutput("set sidescroll?", "  sidescroll=3")
    assertCommandOutput("setglobal sidescroll?", "  sidescroll=3")
  }

  @Test
  fun `test reset 'sidescroll' to default resets to global intellij setting`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollJump = 20
    fixture.editor.settings.horizontalScrollJump = 10
    assertCommandOutput("set sidescroll?", "  sidescroll=10")

    enterCommand("set sidescroll&")
    assertEquals(20, fixture.editor.settings.horizontalScrollJump)
    assertEquals(20, EditorSettingsExternalizable.getInstance().horizontalScrollJump)

    // Unlike many other overridden options, this one allows us to reset it back to global-local, so it will correctly
    // pick up the global value
    EditorSettingsExternalizable.getInstance().horizontalScrollJump = 30
    assertCommandOutput("set sidescroll?", "  sidescroll=30")
  }

  @Test
  fun `test open new window without setting the option correctly keeps global intellij setting`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollJump = 20
    assertCommandOutput("set sidescroll?", "  sidescroll=20")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set sidescroll?", "  sidescroll=20")

    // Changing the global intellij setting should update the new editor
    EditorSettingsExternalizable.getInstance().horizontalScrollJump = 30
    assertCommandOutput("set sidescroll?", "  sidescroll=30")
  }

  @Test
  fun `test open new window after setting global option should keep the global IdeaVim value`() {
    enterCommand("set sidescroll=20")
    assertNotEquals(20, EditorSettingsExternalizable.getInstance().horizontalScrollJump)

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set sidescroll?", "  sidescroll=20")
  }

  @Test
  fun `test update 'sidescroll' affects all open windows`() {
    switchToNewFile("bbb.txt", "lorem ipsum")

    enterCommand("set sidescroll=20")

    // Creating a new file in tests makes it hard to run Ex commands with the original editor, so we simply check the
    // IntelliJ settings
    assertTrue(injector.editorGroup.getEditors().all { it.ij.settings.horizontalScrollJump == 20 })
  }
}
