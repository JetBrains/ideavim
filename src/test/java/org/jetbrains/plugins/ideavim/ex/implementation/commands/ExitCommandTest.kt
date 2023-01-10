/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class ExitCommandTest : VimTestCase() {
  fun `test single file`() {
    setupChecks {
      neoVim.exitOnTearDown = false
    }
    @Suppress("IdeaVimAssertState")
    val psiFile = myFixture.configureByText("A_Discovery", "I found it in a legendary land")
    fileManager.openFile(psiFile.virtualFile, false)
    TestCase.assertNotNull(fileManager.currentFile)

    typeText(commandToKeys("qa"))
    TestCase.assertNull(fileManager.currentFile)
  }

  fun `test full command`() {
    setupChecks {
      neoVim.exitOnTearDown = false
    }
    @Suppress("IdeaVimAssertState")
    val psiFile = myFixture.configureByText("A_Discovery", "I found it in a legendary land")
    fileManager.openFile(psiFile.virtualFile, false)
    TestCase.assertNotNull(fileManager.currentFile)

    typeText(commandToKeys("qall"))
    TestCase.assertNull(fileManager.currentFile)
  }

  @Suppress("IdeaVimAssertState")
  fun `test multiple files`() {
    setupChecks {
      neoVim.exitOnTearDown = false
    }
    val psiFile1 = myFixture.configureByText("A_Discovery1", "I found it in a legendary land")
    val psiFile2 = myFixture.configureByText("A_Discovery2", "all rocks and lavender and tufted grass,")
    fileManager.openFile(psiFile1.virtualFile, false)
    fileManager.openFile(psiFile2.virtualFile, false)
    TestCase.assertNotNull(fileManager.currentFile)

    typeText(commandToKeys("qa"))
    TestCase.assertNull(fileManager.currentFile)
  }
}
