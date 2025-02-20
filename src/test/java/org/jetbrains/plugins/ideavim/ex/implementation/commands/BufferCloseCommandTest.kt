/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * @author Michal Placek
 */
class BufferCloseCommandTest : VimTestCase() {
  @Test
  fun `test close file by bd command`() {
    val psiFile1 = fixture.configureByText("A_Discovery1", "Lorem ipsum dolor sit amet,")
    val psiFile2 = fixture.configureByText("A_Discovery2", "consectetur adipiscing elit")

    ApplicationManager.getApplication().invokeAndWait {
      fileManager.openFile(psiFile1.virtualFile, false)
      fileManager.openFile(psiFile2.virtualFile, true)
    }
    assertPluginError(false)

    typeText(commandToKeys("bd"))

    assertPluginError(false)
  }
}
