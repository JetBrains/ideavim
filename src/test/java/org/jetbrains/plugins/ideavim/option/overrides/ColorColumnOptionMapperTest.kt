/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option.overrides

import com.intellij.application.options.CodeStyle
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.editor.impl.SettingsImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
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
class ColorColumnOptionMapperTest : VimTestCase() {
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
  private fun openNewBufferWindow(filename: String, content: String): Editor {
    // This replaces fixture.editor
    fixture.openFileInEditor(fixture.createFile(filename, content))

    // But our selection changed callback doesn't get called immediately, and that callback will deactivate the ex entry
    // panel (which causes problems if our next command is `:set`). So type something (`0` is a good no-op) to give time
    // for the event to propagate
    typeText("0")

    return fixture.editor
  }

  @Test
  fun `test 'colorcolumn' accepts empty string`() {
    enterCommand("set colorcolumn=")
    assertPluginError(false)
  }

  @Test
  fun `test 'colorcolumn' accepts comma separated string of numbers`() {
    enterCommand("set colorcolumn=10,20,30")
    assertPluginError(false)
  }

  @Test
  fun `test 'colorcolumn' accepts relative values`() {
    enterCommand("set colorcolumn=10,+20,-30")
    assertPluginError(false)
  }

  @Test
  fun `test 'colorcolumn' reports invalid argument with space separated values`() {
    enterCommand("set colorcolumn=10, 20")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: colorcolumn=10,")
  }

  @Test
  fun `test 'colorcolumn' reports invalid argument with non-numeric values`() {
    enterCommand("set colorcolumn=10,aa,20")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: colorcolumn=10,aa,20")
  }

  @Test
  fun `test 'colorcolumn' defaults to current intellij setting`() {
    assertFalse(fixture.editor.settings.isRightMarginShown)
    assertEquals("", optionsIj().colorcolumn.value)
  }

  @Test
  fun `test 'colorcolumn' defaults to global intellij setting`() {
    assertFalse(EditorSettingsExternalizable.getInstance().isRightMarginShown)
    assertEquals("", optionsIj().colorcolumn.value)
  }

  @Test
  fun `test 'colorcolumn' reports '+0' to show right margin is visible`() {
    // IntelliJ only has one setting for visual guides and hard-wrap typing margin, so we have to report a special value
    // of "+0", which makes Vim show a highlight column at 'textwidth' (IntelliJ shows it even if 'textwidth' is 0)
    fixture.editor.settings.isRightMarginShown = true
    assertCommandOutput("set colorcolumn?", "  colorcolumn=+0")
  }

  @Test
  fun `test 'colorcolumn' reports '+0' at end of visual guide list`() {
    fixture.editor.settings.isRightMarginShown = true
    fixture.editor.settings.setSoftMargins(listOf(10, 20, 30))
    assertCommandOutput("set colorcolumn?", "  colorcolumn=10,20,30,+0")
  }

  @Test
  fun `test 'colorcolumn' option reports global intellij setting if not explicitly set`() {
    EditorSettingsExternalizable.getInstance().isRightMarginShown = true
    setGlobalSoftMargins(listOf(10, 20, 30))
    assertCommandOutput("set colorcolumn?", "  colorcolumn=10,20,30,+0")

    setGlobalSoftMargins(listOf(90, 80, 70))
    assertCommandOutput("set colorcolumn?", "  colorcolumn=70,80,90,+0")

    setGlobalSoftMargins(emptyList())
    assertCommandOutput("set colorcolumn?", "  colorcolumn=+0")

    EditorSettingsExternalizable.getInstance().isRightMarginShown = false
    assertCommandOutput("set colorcolumn?", "  colorcolumn=")
  }

  @Test
  fun `test local 'colorcolumn' option reports global intellij setting if not explicitly set`() {
    EditorSettingsExternalizable.getInstance().isRightMarginShown = true
    setGlobalSoftMargins(listOf(10, 20, 30))
    assertCommandOutput("setlocal colorcolumn?", "  colorcolumn=10,20,30,+0")

    setGlobalSoftMargins(listOf(90, 80, 70))
    assertCommandOutput("setlocal colorcolumn?", "  colorcolumn=70,80,90,+0")

    setGlobalSoftMargins(emptyList())
    assertCommandOutput("setlocal colorcolumn?", "  colorcolumn=+0")

    EditorSettingsExternalizable.getInstance().isRightMarginShown = false
    assertCommandOutput("setlocal colorcolumn?", "  colorcolumn=")
  }

  @Test
  fun `test 'colorcolumn' option reports local intellij setting if set via IDE`() {
    fixture.editor.settings.isRightMarginShown = true
    fixture.editor.settings.setSoftMargins(listOf(10, 20, 30))
    assertCommandOutput("set colorcolumn?", "  colorcolumn=10,20,30,+0")

    fixture.editor.settings.setSoftMargins(listOf(70, 80, 90))
    assertCommandOutput("set colorcolumn?", "  colorcolumn=70,80,90,+0")

    fixture.editor.settings.setSoftMargins(emptyList())
    assertCommandOutput("set colorcolumn?", "  colorcolumn=+0")

    fixture.editor.settings.isRightMarginShown = false
    assertCommandOutput("set colorcolumn?", "  colorcolumn=")
  }

  @Test
  fun `test local 'colorcolumn' option reports local intellij setting if set via IDE`() {
    fixture.editor.settings.isRightMarginShown = true
    fixture.editor.settings.setSoftMargins(listOf(10, 20, 30))
    assertCommandOutput("setlocal colorcolumn?", "  colorcolumn=10,20,30,+0")

    fixture.editor.settings.setSoftMargins(listOf(70, 80, 90))
    assertCommandOutput("setlocal colorcolumn?", "  colorcolumn=70,80,90,+0")

    fixture.editor.settings.setSoftMargins(emptyList())
    assertCommandOutput("setlocal colorcolumn?", "  colorcolumn=+0")

    fixture.editor.settings.isRightMarginShown = false
    assertCommandOutput("setlocal colorcolumn?", "  colorcolumn=")
  }

  @Test
  fun `test 'colorcolumn' does not report current visual guides if global right margin option is disabled`() {
    EditorSettingsExternalizable.getInstance().isRightMarginShown = false
    fixture.editor.settings.setSoftMargins(listOf(10, 20, 30))
    assertCommandOutput("set colorcolumn?", "  colorcolumn=")
  }

  @Test
  fun `test 'colorcolumn' does not report current visual guides if local right margin option is disabled`() {
    fixture.editor.settings.isRightMarginShown = false
    fixture.editor.settings.setSoftMargins(listOf(10, 20, 30))
    assertCommandOutput("set colorcolumn?", "  colorcolumn=")
  }

  @Test
  fun `test set 'colorcolumn' modifies local intellij setting only`() {
    assertFalse(EditorSettingsExternalizable.getInstance().isRightMarginShown)
    assertEmpty(getGlobalSoftMargins())

    enterCommand("set colorcolumn=10,20,30")
    assertFalse(EditorSettingsExternalizable.getInstance().isRightMarginShown)
    assertEmpty(getGlobalSoftMargins())
    assertTrue(fixture.editor.settings.isRightMarginShown)
    assertEquals(listOf(10, 20, 30), fixture.editor.settings.softMargins)

    enterCommand("set colorcolumn=50")
    assertFalse(EditorSettingsExternalizable.getInstance().isRightMarginShown)
    assertEmpty(getGlobalSoftMargins())
    assertTrue(fixture.editor.settings.isRightMarginShown)
    assertEquals(listOf(50), fixture.editor.settings.softMargins)

    enterCommand("set colorcolumn=")
    assertFalse(EditorSettingsExternalizable.getInstance().isRightMarginShown)
    assertEmpty(getGlobalSoftMargins())
    assertFalse(fixture.editor.settings.isRightMarginShown)
    assertEquals(listOf(50), fixture.editor.settings.softMargins) // The guides aren't reset
  }

  @Test
  fun `test setlocal 'colorcolumn' modifies local intellij setting only`() {
    assertFalse(EditorSettingsExternalizable.getInstance().isRightMarginShown)
    assertEmpty(getGlobalSoftMargins())

    enterCommand("setlocal colorcolumn=10,20,30")
    assertFalse(EditorSettingsExternalizable.getInstance().isRightMarginShown)
    assertEmpty(getGlobalSoftMargins())
    assertTrue(fixture.editor.settings.isRightMarginShown)
    assertEquals(listOf(10, 20, 30), fixture.editor.settings.softMargins)

    enterCommand("setlocal colorcolumn=50")
    assertFalse(EditorSettingsExternalizable.getInstance().isRightMarginShown)
    assertEmpty(getGlobalSoftMargins())
    assertTrue(fixture.editor.settings.isRightMarginShown)
    assertEquals(listOf(50), fixture.editor.settings.softMargins)

    enterCommand("setlocal colorcolumn=")
    assertFalse(EditorSettingsExternalizable.getInstance().isRightMarginShown)
    assertEmpty(getGlobalSoftMargins())
    assertFalse(fixture.editor.settings.isRightMarginShown)
    assertEquals(listOf(50), fixture.editor.settings.softMargins) // The guides aren't reset
  }

  @Test
  fun `test setglobal 'colorcolumn' option affects IdeaVim global value only`() {
    assertFalse(EditorSettingsExternalizable.getInstance().isRightMarginShown)
    assertEmpty(getGlobalSoftMargins())
    assertCommandOutput("setglobal colorcolumn?", "  colorcolumn=")

    enterCommand("setglobal colorcolumn=10,20,30")
    assertCommandOutput("setglobal colorcolumn?", "  colorcolumn=10,20,30")
    assertFalse(EditorSettingsExternalizable.getInstance().isRightMarginShown)
    assertEmpty(getGlobalSoftMargins())
  }

  @Test
  fun `test set 'colorcolumn' updates IdeaVim global value as well as local`() {
    enterCommand("set colorcolumn=10,20,30")
    assertCommandOutput("setglobal colorcolumn?", "  colorcolumn=10,20,30")
    assertCommandOutput("set colorcolumn?", "  colorcolumn=10,20,30,+0")
  }

  @Test
  fun `test setting IDE value is treated like setlocal`() {
    // If we use `:set`, it updates the local and per-window global values. If we set the value from the IDE, it only
    // affects the local value
    fixture.editor.settings.isRightMarginShown = true
    fixture.editor.settings.setSoftMargins(listOf(70, 80, 90))
    assertCommandOutput("setlocal colorcolumn?", "  colorcolumn=70,80,90,+0")
    assertCommandOutput("set colorcolumn?", "  colorcolumn=70,80,90,+0")
    assertCommandOutput("setglobal colorcolumn?", "  colorcolumn=")
  }

  @Test
  fun `test setglobal does not modify effective IDE value`() {
    enterCommand("setglobal colorcolumn=10,20,30")
    assertFalse(fixture.editor.settings.isRightMarginShown)
  }

  @Test
  fun `test setglobal does not modify persistent IDE global value`() {
    enterCommand("setglobal colorcolumn=10,20,30")
    assertFalse(EditorSettingsExternalizable.getInstance().isRightMarginShown)
    assertEmpty(getGlobalSoftMargins())
  }

  @Test
  fun `test reset 'colorcolun' to default copies current global intellij setting`() {
    fixture.editor.settings.isRightMarginShown = true
    fixture.editor.settings.setSoftMargins(listOf(10, 20, 30))

    enterCommand("set colorcolumn&")
    assertCommandOutput("set colorcolumn?", "  colorcolumn=")
    assertFalse(fixture.editor.settings.isRightMarginShown)
    assertEmpty(fixture.editor.settings.softMargins)

    // Verify that IntelliJ doesn't allow us to "unset" a local editor setting - it's a copy of the global value
    EditorSettingsExternalizable.getInstance().isRightMarginShown = true
    assertFalse(fixture.editor.settings.isRightMarginShown)
  }

  @Test
  fun `test reset local 'colorcolun' to default copies current global intellij setting`() {
    fixture.editor.settings.isRightMarginShown = true
    fixture.editor.settings.setSoftMargins(listOf(10, 20, 30))

    enterCommand("setlocal colorcolumn&")
    assertCommandOutput("setlocal colorcolumn?", "  colorcolumn=")
    assertFalse(fixture.editor.settings.isRightMarginShown)
    assertEmpty(fixture.editor.settings.softMargins)

    // Verify that IntelliJ doesn't allow us to "unset" a local editor setting - it's a copy of the global value
    EditorSettingsExternalizable.getInstance().isRightMarginShown = true
    assertFalse(fixture.editor.settings.isRightMarginShown)
  }

  @Test
  fun `test open new window without setting ideavim option will initialise 'colorcolumn' to defaults`() {
    EditorSettingsExternalizable.getInstance().isRightMarginShown = true
    setGlobalSoftMargins(listOf(10, 20, 30))
    assertCommandOutput("set colorcolumn?", "  colorcolumn=10,20,30,+0")

    openNewBufferWindow("bbb.txt", "lorem ipsum")

    assertCommandOutput("set colorcolumn?", "  colorcolumn=10,20,30,+0")

    // Changing the global value should update the editor
    setGlobalSoftMargins(listOf(40, 50, 60, 70))
    assertCommandOutput("set colorcolumn?", "  colorcolumn=40,50,60,70,+0")

    EditorSettingsExternalizable.getInstance().isRightMarginShown = false
    assertCommandOutput("set colorcolumn?", "  colorcolumn=")
  }

  @Test
  fun `test open new window after setting ideavim option will initialise 'colorcolumn' to setglobal value`() {
    EditorSettingsExternalizable.getInstance().isRightMarginShown = true
    setGlobalSoftMargins(listOf(10, 20, 30))
    enterCommand("set colorcolumn=40,50,60")
    assertCommandOutput("set colorcolumn?", "  colorcolumn=40,50,60,+0")

    openNewBufferWindow("bbb.txt", "lorem ipsum")

    assertCommandOutput("set colorcolumn?", "  colorcolumn=40,50,60,+0")

    // Changing the global value should NOT update the editor
    setGlobalSoftMargins(listOf(10, 20, 30))
    assertCommandOutput("set colorcolumn?", "  colorcolumn=40,50,60,+0")
  }

  @Test
  fun `test setglobal value used when opening new window`() {
    enterCommand("setglobal colorcolumn=10,20,30")

    openNewBufferWindow("bbb.txt", "lorem ipsum")

    assertCommandOutput("set colorcolumn?", "  colorcolumn=10,20,30,+0")

    // Changing the global value should NOT update the editor
    setGlobalSoftMargins(listOf(40, 50, 60))
    assertCommandOutput("set colorcolumn?", "  colorcolumn=10,20,30,+0")
  }

  private fun getGlobalSoftMargins(): List<Int> {
    val language = TextEditorImpl.getDocumentLanguage(fixture.editor)
    return CodeStyle.getSettings(fixture.editor).getSoftMargins(language)
  }

  private fun setGlobalSoftMargins(margins: List<Int>) {
    val language = TextEditorImpl.getDocumentLanguage(fixture.editor)
    val commonSettings = CodeStyle.getSettings(fixture.editor).getCommonSettings(language)
    if (language == null || commonSettings.language == Language.ANY) {
      CodeStyle.getSettings(fixture.editor).defaultSoftMargins = margins
    } else {
      CodeStyle.getSettings(fixture.editor).setSoftMargins(language, margins)
    }
    // Setting the value directly doesn't invalidate the cached property value. Not sure if there's a better way
    (fixture.editor.settings as SettingsImpl).reinitSettings()
  }
}
