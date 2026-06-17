/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.search

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.api.VirtualBufferKind
import com.maddyhome.idea.vim.helper.CmdwinKeys
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for `inccommand=split`, which - on top of the in-buffer preview that `nosplit` provides - opens a preview
 * window listing every line a `:substitute` will change.
 *
 * The preview window is a virtual buffer named [PREVIEW_WINDOW_NAME]. Its content is one entry per *affected* line
 * (lines without a match are omitted), prefixed with the 1-based line number and showing the replacement applied, e.g.
 * `3: zero ventilation and no plumbing are doing Xir job. That's right, it stinks.`
 */
@Suppress("SpellCheckingInspection")
class InccommandSplitTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test inccommand split opens preview window listing affected lines`() {
    configureByText(
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |${c}I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin(),
    )
    enterCommand("set inccommand=split")

    // No <CR> - the preview window should be open while still typing the command
    typeText(":", "%s/the/X/g")

    val preview = openedSubstitutePreview()
    assertNotNull(preview, "expected the substitute preview window to be open")
    assertEquals(
      """
        |1: My name is Cezary Baryka, and for X last 20 minutes I have been X owner of this glass house.
        |2: I'm slowly starting to regret X purchase, freezing cold at night, sweltering heat by day,
        |3: zero ventilation and no plumbing are doing Xir job. That's right, it stinks.
        |4: Ehhh..... I lied.... I didn't buy this pigsty, I won it from my faXr in a game of cards:
      """.trimMargin(),
      preview.readContent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test inccommand split preview lists only lines that actually match`() {
    configureByText(
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |${c}I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin(),
    )
    enterCommand("set inccommand=split")

    // "and" only appears on lines 1 and 3, so lines 2 and 4 must be omitted from the preview.
    typeText(":", "%s/and/X/g")

    assertEquals(
      """
        |1: My name is Cezary Baryka, X for the last 20 minutes I have been the owner of this glass house.
        |3: zero ventilation X no plumbing are doing their job. That's right, it stinks.
      """.trimMargin(),
      openedSubstitutePreview()?.readContent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test inccommand split preview is limited to the command range`() {
    configureByText(
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |${c}I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin(),
    )
    enterCommand("set inccommand=split")

    typeText(":", "2,3s/the/X/g")

    assertEquals(
      """
        |2: I'm slowly starting to regret X purchase, freezing cold at night, sweltering heat by day,
        |3: zero ventilation and no plumbing are doing Xir job. That's right, it stinks.
      """.trimMargin(),
      openedSubstitutePreview()?.readContent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test inccommand split closes the preview window on escape`() {
    configureByText(
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |${c}I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin(),
    )
    enterCommand("set inccommand=split")

    typeText(":", "%s/the/X/g", "<Esc>")

    assertNull(openedSubstitutePreview(), "the preview window should be closed after cancelling the command")
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test inccommand nosplit does not open a preview window`() {
    configureByText(
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |${c}I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin(),
    )
    enterCommand("set inccommand=nosplit")

    typeText(":", "%s/the/X/g")

    assertNull(openedSubstitutePreview(), "nosplit must preview in the buffer only, never open a preview window")
  }

  private fun openedSubstitutePreview(): VirtualFile? {
    var preview: VirtualFile? = null
    ApplicationManager.getApplication().invokeAndWait {
      repeat(10) {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        preview = ReadAction.compute<VirtualFile?, RuntimeException> {
          FileEditorManager.getInstance(fixture.project).openFiles
            .firstOrNull { it.getUserData(CmdwinKeys.KIND) == VirtualBufferKind.SubstitutePreview }
        }
        if (preview != null) return@invokeAndWait
      }
    }
    return preview
  }

  // The preview window is refreshed in place via its document, so read what is actually displayed (the document),
  // falling back to the VFS content.
  private fun VirtualFile.readContent(): String = ApplicationManager.getApplication().runReadAction<String> {
    val document = FileDocumentManager.getInstance().getDocument(this)
    document?.text ?: String(contentsToByteArray(), charset)
  }

  companion object {
    private const val PREVIEW_WINDOW_NAME = "[Preview Substitute]"
  }
}
