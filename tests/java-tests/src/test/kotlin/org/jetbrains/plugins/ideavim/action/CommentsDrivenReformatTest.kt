/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * End-to-end coverage of `gq` honoring the buffer-local `'comments'` value.
 *
 * Uses a leader not present in the default `'comments'` string so the assertion
 * fails if the option value is not read at wrap time.
 */
@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "IdeaVim wraps via the 'comments' option and its filetype presets.",
)
class CommentsDrivenReformatTest : VimTestCase() {

  @Test
  fun `custom REM marker from setlocal drives wrap continuation`() {
    configureByText(
      PlainTextFileType.INSTANCE,
      "REM ${c}some long custom-marker comment text that must wrap to respect textwidth",
    )
    enterCommand("setlocal comments=:REM")
    enterCommand("set textwidth=30")
    typeText(injector.parser.parseKeys("gqq"))
    assertState(
      """
      ${c}REM some long custom-marker
      REM comment text that must
      REM wrap to respect textwidth
      """.trimIndent(),
    )
  }

}
