/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option.overrides

import com.intellij.codeInsight.actions.ReaderModeSettings
import com.intellij.codeInsight.multiverse.MultiverseTestEnabler
import com.intellij.codeInsight.multiverse.isSharedSourceSupportEnabled
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import javax.swing.SwingUtilities

/**
 * VIM-4380: `'colorcolumn'` and `'textwidth'` look up the document's language to get code style settings, which needs
 * read access. Local options are initialised from `editorCreated`, which a diff viewer calls on the EDT without a read
 * lock.
 *
 * The platform only asserts read access when looking up the editor's code insight context if shared source support
 * (multiverse) is enabled for the project, so we enable it for the project created for each test. Note that the
 * language is only looked up via the code insight context since 2026.3, so older platforms don't detect the problem.
 */
@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class DocumentLanguageReadAccessTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    @Suppress("TestOnlyProblems")
    MultiverseTestEnabler.enableSharedSourcesForTheNextProject()
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  @Test
  fun `test creating editor without read access initialises local options`() {
    assertTrue(isSharedSourceSupportEnabled(fixture.project))

    // Reader mode's own editorCreated listener also requires read access. Disable it, so it doesn't hide our problem
    ReaderModeSettings.getInstance(fixture.project).enabled = false

    // Create a new editor, so the platform hasn't cached its code insight context yet. This is what the diff viewer
    // does when showing a file
    SwingUtilities.invokeAndWait {
      assertFalse(ApplicationManager.getApplication().isReadAccessAllowed)
      val editor = EditorFactory.getInstance().createEditor(fixture.editor.document, fixture.project, EditorKind.DIFF)
      EditorFactory.getInstance().releaseEditor(editor)
    }
    assertPluginError(false)
  }
}
