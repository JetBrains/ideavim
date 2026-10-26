/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.fugitive

import com.intellij.openapi.fileEditor.FileEditorManager
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `:ccl[ose]` closing the quickfix window that `:copen` opened, keeping the list itself.
 */
class FugitiveCcloseTest : FugitiveTestCase() {

  @Test
  fun `test cclose closes the window`() {
    grepFiveMatches()
    enterCommandInCurrentEditor("copen")

    enterCommandInCurrentEditor("cclose")

    assertFalse(isQuickFixOpen())
  }

  @Test
  fun `test ccl is an abbreviation of cclose`() {
    grepFiveMatches()
    enterCommandInCurrentEditor("copen")

    enterCommandInCurrentEditor("ccl")

    assertFalse(isQuickFixOpen())
  }

  @Test
  fun `test cclose without a window does nothing`() {
    grepFiveMatches()

    enterCommandInCurrentEditor("cclose")

    assertPluginError(false)
    assertCurrentFile("aaa.txt")
  }

  @Test
  fun `test cclose keeps the list`() {
    grepFiveMatches()
    enterCommandInCurrentEditor("copen")
    enterCommandInCurrentEditor("cclose")

    enterCommandInCurrentEditor("cnext")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(2, 4)
  }

  @Test
  fun `test copen after cclose opens the window again`() {
    grepFiveMatches()
    enterCommandInCurrentEditor("copen")
    enterCommandInCurrentEditor("cclose")

    enterCommandInCurrentEditor("copen")

    assertTrue(isQuickFixOpen())
  }

  private fun isQuickFixOpen(): Boolean =
    FileEditorManager.getInstance(fixture.project).openFiles.any { it.name == "[Quickfix List]" }
}
