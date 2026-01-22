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
 * @author Alex Plate
 */
class ExitCommandTest : VimTestCase() {
  @Test
  fun `test single file`() {
    setupChecks {
      neoVim.exitOnTearDown = false
    }
    @Suppress("IdeaVimAssertState")
    val psiFile = fixture.configureByText("A_Discovery", "Lorem ipsum dolor sit amet,")
    ApplicationManager.getApplication().invokeAndWait {
      fileManager.openFile(psiFile.virtualFile, false)
    }
    kotlin.test.assertNotNull<Any>(fileManager.currentFile)

    typeText(commandToKeys("qa"))
    kotlin.test.assertNull(fileManager.currentFile)
    assertPluginError(false)
  }

  @Test
  fun `test full command`() {
    setupChecks {
      neoVim.exitOnTearDown = false
    }
    @Suppress("IdeaVimAssertState")
    val psiFile = fixture.configureByText("A_Discovery", "Lorem ipsum dolor sit amet,")
    ApplicationManager.getApplication().invokeAndWait {
      fileManager.openFile(psiFile.virtualFile, false)
    }
    kotlin.test.assertNotNull<Any>(fileManager.currentFile)

    typeText(commandToKeys("qall"))
    kotlin.test.assertNull(fileManager.currentFile)
    assertPluginError(false)
  }

  @Suppress("IdeaVimAssertState")
  @Test
  fun `test multiple files`() {
    setupChecks {
      neoVim.exitOnTearDown = false
    }
    val psiFile1 = fixture.configureByText("A_Discovery1", "Lorem ipsum dolor sit amet,")
    val psiFile2 = fixture.configureByText("A_Discovery2", "consectetur adipiscing elit")
    ApplicationManager.getApplication().invokeAndWait {
      fileManager.openFile(psiFile1.virtualFile, false)
      fileManager.openFile(psiFile2.virtualFile, false)
    }
    kotlin.test.assertNotNull<Any>(fileManager.currentFile)

    typeText(commandToKeys("qa"))
    kotlin.test.assertNull(fileManager.currentFile)
    assertPluginError(false)
  }

  @Test
  fun `test xa command`() {
    setupChecks {
      neoVim.exitOnTearDown = false
    }
    @Suppress("IdeaVimAssertState")
    val psiFile = fixture.configureByText("A_Discovery", "Lorem ipsum dolor sit amet,")
    ApplicationManager.getApplication().invokeAndWait {
      fileManager.openFile(psiFile.virtualFile, false)
    }
    kotlin.test.assertNotNull<Any>(fileManager.currentFile)

    typeText(commandToKeys("xa"))
    kotlin.test.assertNull(fileManager.currentFile)
    assertPluginError(false)
  }

  @Test
  fun `test xall command`() {
    setupChecks {
      neoVim.exitOnTearDown = false
    }
    @Suppress("IdeaVimAssertState")
    val psiFile = fixture.configureByText("A_Discovery", "Lorem ipsum dolor sit amet,")
    ApplicationManager.getApplication().invokeAndWait {
      fileManager.openFile(psiFile.virtualFile, false)
    }
    kotlin.test.assertNotNull<Any>(fileManager.currentFile)

    typeText(commandToKeys("xall"))
    kotlin.test.assertNull(fileManager.currentFile)
    assertPluginError(false)
  }

  @Test
  fun `test wqa command`() {
    setupChecks {
      neoVim.exitOnTearDown = false
    }
    @Suppress("IdeaVimAssertState")
    val psiFile = fixture.configureByText("A_Discovery", "Lorem ipsum dolor sit amet,")
    ApplicationManager.getApplication().invokeAndWait {
      fileManager.openFile(psiFile.virtualFile, false)
    }
    kotlin.test.assertNotNull<Any>(fileManager.currentFile)

    typeText(commandToKeys("wqa"))
    kotlin.test.assertNull(fileManager.currentFile)
    assertPluginError(false)
  }

  @Test
  fun `test wqall command`() {
    setupChecks {
      neoVim.exitOnTearDown = false
    }
    @Suppress("IdeaVimAssertState")
    val psiFile = fixture.configureByText("A_Discovery", "Lorem ipsum dolor sit amet,")
    ApplicationManager.getApplication().invokeAndWait {
      fileManager.openFile(psiFile.virtualFile, false)
    }
    kotlin.test.assertNotNull<Any>(fileManager.currentFile)

    typeText(commandToKeys("wqall"))
    kotlin.test.assertNull(fileManager.currentFile)
    assertPluginError(false)
  }

  @Test
  fun `test quita command`() {
    setupChecks {
      neoVim.exitOnTearDown = false
    }
    @Suppress("IdeaVimAssertState")
    val psiFile = fixture.configureByText("A_Discovery", "Lorem ipsum dolor sit amet,")
    ApplicationManager.getApplication().invokeAndWait {
      fileManager.openFile(psiFile.virtualFile, false)
    }
    kotlin.test.assertNotNull<Any>(fileManager.currentFile)

    typeText(commandToKeys("quita"))
    kotlin.test.assertNull(fileManager.currentFile)
    assertPluginError(false)
  }

  @Test
  fun `test quitall command`() {
    setupChecks {
      neoVim.exitOnTearDown = false
    }
    @Suppress("IdeaVimAssertState")
    val psiFile = fixture.configureByText("A_Discovery", "Lorem ipsum dolor sit amet,")
    ApplicationManager.getApplication().invokeAndWait {
      fileManager.openFile(psiFile.virtualFile, false)
    }
    kotlin.test.assertNotNull<Any>(fileManager.currentFile)

    typeText(commandToKeys("quitall"))
    kotlin.test.assertNull(fileManager.currentFile)
    assertPluginError(false)
  }
}
