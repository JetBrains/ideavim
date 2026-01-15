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

@TestWithoutNeovim(
  reason = SkipNeovimReason.NOT_VIM_TESTING,
  description = "Tests integration between IdeaVim option and IntelliJ EditorSettingsExternalizable"
)
class ScrollJumpOptionMapperTest : VimTestCase() {
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
  fun `test 'scrolljump' global option defaults to current intellij setting`() {
    assertEquals(1, fixture.editor.settings.verticalScrollJump)
    assertEquals(1, options().scrolljump)
  }

  @Test
  fun `test 'scrolljump' defaults to global intellij setting`() {
    assertEquals(1, EditorSettingsExternalizable.getInstance().verticalScrollJump)
    assertEquals(1, options().scrolljump)
  }

  @Test
  fun `test 'scrolljump' option reports global intellij setting if not explicitly set`() {
    EditorSettingsExternalizable.getInstance().verticalScrollJump = 7
    assertCommandOutput("set scrolljump?", "  scrolljump=7")

    // Also tests the value being modified in the IDE's settings
    EditorSettingsExternalizable.getInstance().verticalScrollJump = 3
    assertCommandOutput("set scrolljump?", "  scrolljump=3")
  }

  // 'scrolljump' is a global option, so we don't need to test `:setlocal` or `:setglobal`

  @Test
  fun `test 'scrolljump' option reports local intellij setting if not explicitly set`() {
    // 'scrolljump' is a global option, but IntelliJ's setting is global-local. To prevent IntelliJ using incorrect
    // values for scrolling during typing (when IdeaVim's scroll implementation isn't called early enough), we set the
    // local value for all editors
    fixture.editor.settings.verticalScrollJump = 7
    assertCommandOutput("set scrolljump?", "  scrolljump=7")

    // Also tests the value being modified in the IDE's settings
    fixture.editor.settings.verticalScrollJump = 3
    assertCommandOutput("set scrolljump?", "  scrolljump=3")
  }

  @Test
  fun `test setting 'scrolljump' modifies local intellij setting only`() {
    EditorSettingsExternalizable.getInstance().verticalScrollJump = 10
    fixture.editor.settings.verticalScrollJump = 20

    enterCommand("set scrolljump=7")
    assertEquals(7, fixture.editor.settings.verticalScrollJump)
    assertEquals(10, EditorSettingsExternalizable.getInstance().verticalScrollJump)
  }

  @Test
  fun `test setting 'scrolljump' to negative percentage value sets local intellij setting to 0`() {
    EditorSettingsExternalizable.getInstance().verticalScrollJump = 10
    fixture.editor.settings.verticalScrollJump = 20

    enterCommand("set scrolljump=-25")
    assertEquals(0, fixture.editor.settings.verticalScrollJump)
    assertEquals(10, EditorSettingsExternalizable.getInstance().verticalScrollJump)
    assertCommandOutput("setlocal scrolljump?", "  scrolljump=-25")
    assertCommandOutput("set scrolljump?", "  scrolljump=-25")
    assertCommandOutput("setglobal scrolljump?", "  scrolljump=-25")
  }

  @Test
  fun `test setting global IDE value will update IdeaVim value`() {
    enterCommand("set scrolljump=7")
    EditorSettingsExternalizable.getInstance().verticalScrollJump = 3
    assertCommandOutput("setlocal scrolljump?", "  scrolljump=3")
    assertCommandOutput("set scrolljump?", "  scrolljump=3")
    assertCommandOutput("setglobal scrolljump?", "  scrolljump=3")
  }

  @Test
  fun `test reset 'scrolljump' to default resets to global intellij setting`() {
    EditorSettingsExternalizable.getInstance().verticalScrollJump = 20
    fixture.editor.settings.verticalScrollJump = 10
    assertCommandOutput("set scrolljump?", "  scrolljump=10")

    enterCommand("set scrolljump&")
    assertEquals(20, fixture.editor.settings.verticalScrollJump)
    assertEquals(20, EditorSettingsExternalizable.getInstance().verticalScrollJump)

    // Unlike many other overridden options, this one allows us to reset it back to global-local, so it will correctly
    // pick up the global value
    EditorSettingsExternalizable.getInstance().verticalScrollJump = 30
    assertCommandOutput("set scrolljump?", "  scrolljump=30")
  }

  @Test
  fun `test open new window without setting the option correctly keeps global intellij setting`() {
    EditorSettingsExternalizable.getInstance().verticalScrollJump = 20
    assertCommandOutput("set scrolljump?", "  scrolljump=20")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set scrolljump?", "  scrolljump=20")

    // Changing the global intellij setting should update the new editor
    EditorSettingsExternalizable.getInstance().verticalScrollJump = 30
    assertCommandOutput("set scrolljump?", "  scrolljump=30")
  }

  @Test
  fun `test open new window after setting global option should keep the global IdeaVim value`() {
    enterCommand("set scrolljump=20")
    assertNotEquals(20, EditorSettingsExternalizable.getInstance().verticalScrollJump)

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set scrolljump?", "  scrolljump=20")
  }

  @Test
  fun `test update 'scrolljump' affects all open windows`() {
    switchToNewFile("bbb.txt", "lorem ipsum")

    enterCommand("set scrolljump=20")

    // Creating a new file in tests makes it hard to run Ex commands with the original editor, so we simply check the
    // IntelliJ settings
    assertTrue(injector.editorGroup.getEditors().all { it.ij.settings.verticalScrollJump == 20 })
  }
}
