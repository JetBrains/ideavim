/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.listener

import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.listener.VimListenerTestObject
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

class VimListenersTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    val manager =
      FileEditorManagerImpl(fixture.project, (fixture.project as ComponentManagerEx).getCoroutineScope().childScope())
    fixture.project.replaceService(FileEditorManager::class.java, manager, fixture.testRootDisposable)

    VimListenerTestObject.disposedCounter = 0
    VimListenerTestObject.enabled = true
  }

  @AfterEach
  fun tearDown() {
    VimListenerTestObject.disposedCounter = 0
    VimListenerTestObject.enabled = false
  }

  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  @Test
  fun `disposable is called when disabling IdeaVim functionality`() {
    configureByText("XYZ")

    try {
      VimPlugin.setEnabled(false)
      assertEquals(1, VimListenerTestObject.disposedCounter)
    } finally {
      VimPlugin.setEnabled(true)
    }
  }

  @Test
  fun `disposable is called when closing editor`() {
    configureByText("XYZ")

    val fileManager = FileEditorManagerEx.getInstanceEx(fixture.project)
    fileManager.closeFile(fixture.editor.virtualFile)

    assertEquals(1, VimListenerTestObject.disposedCounter)
  }

  @Test
  fun `disposable is called once when disabling IdeaVim functionality and then closing the editor`() {
    configureByText("XYZ")

    try {
      VimPlugin.setEnabled(false)
      assertEquals(1, VimListenerTestObject.disposedCounter)

      val fileManager = FileEditorManagerEx.getInstanceEx(fixture.project)
      fileManager.closeFile(fixture.editor.virtualFile)

      assertEquals(1, VimListenerTestObject.disposedCounter)
    } finally {
      VimPlugin.setEnabled(true)
    }
  }
}
