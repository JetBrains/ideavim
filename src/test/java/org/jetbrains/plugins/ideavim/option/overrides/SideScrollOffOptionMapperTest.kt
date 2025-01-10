/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option.overrides

import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class SideScrollOffOptionMapperTest : VimTestCase() {
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
    fixture.openFileInEditor(fixture.createFile(filename, content))

    // But our selection changed callback doesn't get called immediately, and that callback will deactivate the ex entry
    // panel (which causes problems if our next command is `:set`). So type something (`0` is a good no-op) to give time
    // for the event to propagate
    typeText("0")
  }

  @Test
  fun `test 'sidescrolloff' defaults to global intellij setting`() {
    assertEquals(0, EditorSettingsExternalizable.getInstance().horizontalScrollOffset)
    assertEquals(0, options().sidescrolloff)
  }

  @Test
  fun `test 'sidescrolloff' option reports global intellij setting if not set`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")

    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
  }

  @Test
  fun `test local 'sidescrolloff' option reports unset value if not explicitly set`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")
  }

  @Test
  fun `test global 'sidescrolloff' option reports global intellij setting if not explicitly set`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=10")
  }

  @Test
  fun `test set 'sidescrolloff' sets local intellij value to 0`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10

    enterCommand("set sidescrolloff=20")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")
    assertEquals(10, EditorSettingsExternalizable.getInstance().horizontalScrollOffset)

    // We set the local value to 0 so that IntelliJ's scrolling does nothing, so IdeaVim can control scrolling
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test setlocal 'sidescrolloff' sets local intellij value to 0`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10

    enterCommand("setlocal sidescrolloff=20")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=20")
    assertEquals(10, EditorSettingsExternalizable.getInstance().horizontalScrollOffset)

    // We set the local value to 0 so that IntelliJ's scrolling does nothing, so IdeaVim can control scrolling
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test set 'sidescrolloff' mimics global value by setting local intellij setting for all editors`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10

    val firstEditor = fixture.editor

    switchToNewFile("bbb.txt", "lorem ipsum")

    // Setting a global-local value. This should set the global value, which we mimic by changing the local value of the
    // external IDE setting
    enterCommand("set sidescrolloff=20")

    assertEquals(20, options().sidescrolloff)
    assertEquals(20, injector.options(firstEditor.vim).sidescrolloff)
  }

  @Test
  fun `test set 'sidescrolloff' updates all editors unless locally overridden`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10

    val firstEditor = fixture.editor
    enterCommand("setlocal sidescrolloff=20")

    switchToNewFile("bbb.txt", "lorem ipsum")

    // Setting a global-local value. This should set the global value, which we would normally mimic by setting the
    // local external IDE setting for all editors. We want to "disable" IntelliJ's scroll offset handling, so set to 0
    enterCommand("set sidescrolloff=30")

    assertEquals(30, options().sidescrolloff)
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
    assertEquals(20, injector.options(firstEditor.vim).sidescrolloff)
    assertEquals(0, firstEditor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test setting global IDE value will update IdeaVim value`() {
    enterCommand("set sidescrolloff=10")

    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
  }

  @Test
  fun `test setting global IDE value will not update locally set IdeaVim value`() {
    enterCommand("setlocal sidescrolloff=10")

    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
  }

  @Test
  fun `test open new window without setting the option uses current intellij value as default value`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")

    // Changing the global setting should update the new editor
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
  }

  @Test
  fun `test open new window after setting the global option correctly updates local intellij value for new window`() {
    enterCommand("set sidescrolloff=20")
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")

    // Changing the global IntelliJ setting syncs with the global Vim value
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")

    // We don't support externally changing the local editor setting
    enterCommand("setlocal sidescrolloff=30")
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=30")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=30")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=10")
    assertEquals(10, EditorSettingsExternalizable.getInstance().horizontalScrollOffset)
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test setlocal 'sidescrolloff' then open new window uses value from setglobal`() {
    enterCommand("setglobal sidescrolloff=20")
    enterCommand("setlocal sidescrolloff=10")

    switchToNewFile("bbb.txt", "lorem ipsum")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
  }

  // :set[local|global] {option}& - reset to default value

  @Test
  fun `test reset 'sidescrolloff' to default value resets global value to intellij global value`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20

    enterCommand("set sidescrolloff=10")
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")

    // Vim: global=10, local=-1 => global=default, local=unset
    enterCommand("set sidescrolloff&")
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")

    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 15
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=15")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=15")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")
  }

  @Test
  fun `test reset 'sidescrolloff' to default value resets local value to global external value`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 5

    enterCommand("setglobal sidescrolloff=10")
    enterCommand("setlocal sidescrolloff=20") // Local intellij value will be 20

    // Vim: global=10, local=20 => global=default, local=default
    enterCommand("set sidescrolloff&")
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=5")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=5")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=5")

    // Note that global=default, local=default is the same as global=default, local=unset
    // Changing the IDE value is reflected in the global Vim value, and also the local value
    // In Vim, local would be whatever the default is for the option. But then setting the option at effective scope
    // would also change the local value, so it's reasonable that changing the default value (by changing the IDE value)
    // is reflected in the local Vim value.
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 15
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=15")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=15")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=15")
  }

  @Test
  fun `test reset 'sidescrolloff' to default value resets global value to intellij global value for all editors`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20

    val firstEditor = fixture.editor

    switchToNewFile("bbb.txt", "lorem ipsum")

    enterCommand("set sidescrolloff=10")
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")
    assertEquals(10, injector.options(firstEditor.vim).sidescrolloff) // Equivalent to `set sidescrolloff?`
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
    assertEquals(0, firstEditor.settings.horizontalScrollOffset)

    enterCommand("set sidescrolloff&")
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")
    assertEquals(20, injector.options(firstEditor.vim).sidescrolloff) // Equivalent to `set sidescrolloff?`
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
    assertEquals(0, firstEditor.settings.horizontalScrollOffset)

    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 15
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=15")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=15")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")
    assertEquals(15, injector.options(firstEditor.vim).sidescrolloff) // Equivalent to `set sidescrolloff?`
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
    assertEquals(0, firstEditor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test reset 'sidescrolloff' to default value resets global value to intellij global value for all editors 2`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20
    enterCommand("setlocal sidescrolloff=7")
    val firstEditor = fixture.editor

    switchToNewFile("bbb.txt", "lorem ipsum")

    enterCommand("set sidescrolloff=10")
    enterCommand("setlocal sidescrolloff=5")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=5")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=5")
    assertEquals(7, injector.options(firstEditor.vim).sidescrolloff) // Equivalent to `set sidescrolloff?`
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
    assertEquals(0, firstEditor.settings.horizontalScrollOffset)

    // Vim: global=10, local=5 => global=default, local=default
    // local=default behaves like local=unset
    enterCommand("set sidescrolloff&")
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=20")
    assertEquals(7, injector.options(firstEditor.vim).sidescrolloff) // Equivalent to `set sidescrolloff?`
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
    assertEquals(0, firstEditor.settings.horizontalScrollOffset)

    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 15
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=15")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=15")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=15")
    assertEquals(7, injector.options(firstEditor.vim).sidescrolloff) // Equivalent to `set sidescrolloff?`
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
    assertEquals(0, firstEditor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test reset 'sidescrolloff' to default value resets global value to intellij global value for all editors unless overridden locally`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20
    enterCommand("setlocal sidescrolloff=7")
    val firstEditor = fixture.editor

    switchToNewFile("bbb.txt", "lorem ipsum")

    enterCommand("set sidescrolloff=10")
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")
    assertEquals(7, injector.options(firstEditor.vim).sidescrolloff) // `set sidescrolloff?` for first editor
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
    assertEquals(0, firstEditor.settings.horizontalScrollOffset)

    // Vim: global=10, local=-1 => global=default, local=unset
    enterCommand("set sidescrolloff&")
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")
    assertEquals(7, injector.options(firstEditor.vim).sidescrolloff) // `set sidescrolloff?` for first editor
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
    assertEquals(0, firstEditor.settings.horizontalScrollOffset)

    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 15
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=15")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=15")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")
    assertEquals(7, injector.options(firstEditor.vim).sidescrolloff) // `set sidescrolloff?` for first editor
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
    assertEquals(0, firstEditor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test reset 'sidescrolloff' to default value resets global value to intellij global value for all editors unless overridden locally 2`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20
    enterCommand("setlocal sidescrolloff=7")
    val firstEditor = fixture.editor

    switchToNewFile("bbb.txt", "lorem ipsum")

    enterCommand("set sidescrolloff=10")
    enterCommand("setlocal sidescrolloff=5")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=5")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=5")
    assertEquals(7, injector.options(firstEditor.vim).sidescrolloff) // Equivalent to `set sidescrolloff?`
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
    assertEquals(0, firstEditor.settings.horizontalScrollOffset)

    // Vim: global=10, local=5 => global=default, local=default
    // Note that global=default, local=default behaves the same as global=default, local=unset
    enterCommand("set sidescrolloff&")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=20")
    assertEquals(7, injector.options(firstEditor.vim).sidescrolloff) // Equivalent to `set sidescrolloff?`
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
    assertEquals(0, firstEditor.settings.horizontalScrollOffset)

    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 15
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=15")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=15")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=15")
    assertEquals(7, injector.options(firstEditor.vim).sidescrolloff) // Equivalent to `set sidescrolloff?`
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
    assertEquals(0, firstEditor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test reset global 'sidescrolloff' to default value resets global to intellij default value`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20
    enterCommand("setglobal sidescrolloff=10")

    // Vim: global=10, local=-1 => global=default, local=unset
    enterCommand("setglobal sidescrolloff&")

    // This resets global to default + local to unset, but doesn't modify the local intellij value

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)

    // Changing the intellij default value is reflected in IdeaVim
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 30
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=30")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=30")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test reset global 'sidescrolloff' to default value resets global value without changing local value`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20
    enterCommand("setlocal sidescrolloff=10")

    // Vim: global=20, local=10 => global=default, local=10
    enterCommand("setglobal sidescrolloff&")

    // set global value to default, but not resetting the local intellij value...

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput(
      "setglobal sidescrolloff?",
      "  sidescrolloff=20"
    ) // Vim is default of 0, but we want to use global intellij value
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=10")
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)

    // Changing the intellij default value is reflected in IdeaVim global, but not local
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 30
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=30")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=10")
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test reset local 'sidescrolloff' to default value resets local value to copy of option default value`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20
    enterCommand("setglobal sidescrolloff=10")
    enterCommand("setlocal sidescrolloff=15")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=15")

    // Vim: global=10, local=15 => global=10, local=default
    // IdeaVim's defaults are transparent, so this should come from the IDE default value
    enterCommand("setlocal sidescrolloff&")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=20")
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)

    // Changing the intellij default value is reflected in IdeaVim
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 30
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=30")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=30")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=30")
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test reset local 'sidescrolloff' to default value resets local value to default intellij value`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20
    enterCommand("setlocal sidescrolloff=10")

    // Vim: global=default, local=10 => global=default, local=default
    // local=default behaves like local=unset
    enterCommand("setlocal sidescrolloff&")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20") // Was originally default
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=20")
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)

    // Changing the intellij default value is reflected in IdeaVim
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 30
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=30")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=30")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=30")
    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
  }

  // :set[local|global] {option}< - reset effective/local/global value to global value
  // Note that we don't need to test `:set {option}<` and `:setlocal {option}<` for multiple editors, because they
  // either set or remove the local value (for the current editor), effectively resetting it back to the global value.
  // In other words, the commands only modify the local value, so cannot and do not affect other editors

  @Test
  fun `test reset effective 'sidescrolloff' to global default value does not modify unset local value`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")

    // The local value is unset, so "set {option}<" does not modify it (effective value is still global)
    // See `:help :setlocal` and https://github.com/vim/vim/issues/14062
    enterCommand("set sidescrolloff<")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")

    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)

    // Global is default and local is unset, so this should affect values
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")

    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test reset effective 'sidescrolloff' to global default value copies global value as default to local explicitly set value`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20

    enterCommand("setlocal sidescrolloff=10")
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=10")

    // The local value has been set, so "set {option}<" copies the global value (effective value is still global)
    // Global was default, so now local is default too
    // See `:help :setlocal` and https://github.com/vim/vim/issues/14062
    enterCommand("set sidescrolloff<")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=20")

    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)

    // Both global and local values should be defaults, so this should affect both
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=10")

    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test reset effective 'sidescrolloff' to global value copies global value to local explicitly set value`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10

    enterCommand("set sidescrolloff=20")  // No longer default
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")

    // The local value has been set, so "set {option}<" copies the global value (effective value is still global)
    // See `:help :setlocal` and https://github.com/vim/vim/issues/14062
    enterCommand("set sidescrolloff<")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")

    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)

    // Global is explicitly set, and local is unset. Global changes should not affect values
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")

    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test reset local 'sidescrolloff' value to default global value resets explicitly set local value to unset`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20

    enterCommand("setlocal sidescrolloff=10")
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=10")

    // "setlocal {option}<" always unsets number-based global-local options
    // See `:help :setlocal` and https://github.com/vim/vim/issues/14062
    enterCommand("setlocal sidescrolloff<")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")

    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)

    // Global is default, so this should affect global only
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")

    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test reset local 'sidescrolloff' value to global value resets explicitly set local value to unset`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10

    enterCommand("set sidescrolloff=20")  // No longer default
    enterCommand("setlocal sidescrolloff=10")
    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=10")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=10")

    // "setlocal {option}<" always unsets number-based global-local options
    // See `:help :setlocal` and https://github.com/vim/vim/issues/14062
    enterCommand("setlocal sidescrolloff<")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")

    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)

    // Global is not default, so should not be affected by this change
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 10

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")

    assertEquals(0, fixture.editor.settings.horizontalScrollOffset)
  }

  @Test
  fun `test reset global 'sidescrolloff' to global value does nothing`() {
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset = 20

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")

    // This copies the global value to the global value. It's a no-op
    enterCommand("setglobal sidescrolloff<")

    assertCommandOutput("set sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setglobal sidescrolloff?", "  sidescrolloff=20")
    assertCommandOutput("setlocal sidescrolloff?", "  sidescrolloff=-1")
  }
}
