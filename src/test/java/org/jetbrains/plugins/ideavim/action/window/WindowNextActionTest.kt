/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.window

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import com.maddyhome.idea.vim.group.WindowGroup
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitUntil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.awt.Component
import javax.swing.SwingConstants
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * The windows are laid out as
 * ```
 * +-----+-----+
 * | top |     |
 * +-----+right|
 * | bot |     |
 * +-----+-----+
 * ```
 * and created in the order top, right, bottom, so the creation order differs from the top-left to bottom-right one.
 * Detached windows are created in the reverse of their expected order for the same reason.
 */
@TestWithoutNeovim(
  SkipNeovimReason.SEE_DESCRIPTION,
  description = "Drives several IDE editor windows directly; the Neovim test harness only mirrors a single window",
)
class WindowNextActionTest : VimTestCase() {
  private lateinit var fileEditorManager: FileEditorManagerImpl
  private lateinit var top: EditorWindow
  private lateinit var right: EditorWindow
  private lateinit var bottom: EditorWindow

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    val scope = (fixture.project as ComponentManagerEx).getCoroutineScope()
    fileEditorManager = FileEditorManagerImpl(fixture.project, scope.childScope(name = "WindowNextActionTest"))
    fixture.project.replaceService(FileEditorManager::class.java, fileEditorManager, fixture.testRootDisposable)

    ApplicationManager.getApplication().invokeAndWait {
      configureByText("${c}text")
      top = fileEditorManager.currentWindow!!
    }
    right = split(top, SwingConstants.VERTICAL)
    bottom = split(top, SwingConstants.HORIZONTAL)
    select(top)
  }

  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  @Test
  fun `test next window goes from top-left to bottom-right and wraps around`() {
    typeText("<C-W>w")
    assertCurrentWindow(bottom)
    typeText("<C-W>w")
    assertCurrentWindow(right)
    typeText("<C-W>w")
    assertCurrentWindow(top)
  }

  @Test
  fun `test previous window goes from bottom-right to top-left and wraps around`() {
    typeText("<C-W>W")
    assertCurrentWindow(right)
    typeText("<C-W>W")
    assertCurrentWindow(bottom)
    typeText("<C-W>W")
    assertCurrentWindow(top)
  }

  @Test
  fun `test count selects window by its position on screen`() {
    typeText("2<C-W>w")
    assertCurrentWindow(bottom)
    typeText("3<C-W>w")
    assertCurrentWindow(right)
  }

  @Test
  fun `test count larger than number of windows selects last window`() {
    typeText("9<C-W>w")
    assertCurrentWindow(right)
  }

  @Test
  fun `test detached windows follow the main window from left to right`() {
    val rightDetached = detach(screenX = 2000, screenY = 0)
    val leftDetached = detach(screenX = 1000, screenY = 0)

    assertWindowOrder(top, bottom, right, leftDetached, rightDetached)
  }

  @Test
  fun `test detached windows with the same left edge are ordered from top to bottom`() {
    val lowerDetached = detach(screenX = 1000, screenY = 800)
    val upperDetached = detach(screenX = 1000, screenY = 0)

    assertWindowOrder(top, bottom, right, upperDetached, lowerDetached)
  }

  private fun split(window: EditorWindow, orientation: Int): EditorWindow {
    var newWindow: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      newWindow = window.split(orientation, true, fixture.file.virtualFile, false)
    }
    waitUntil { newWindow!!.allComposites.first().selectedEditor != null }
    return newWindow!!
  }

  private fun detach(screenX: Int, screenY: Int): EditorWindow {
    var detached: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      val windowsBefore = fileEditorManager.windows.toSet()
      fileEditorManager.openFileInNewWindow(fixture.file.virtualFile)
      detached = fileEditorManager.windows.single { it !in windowsBefore }
      // Unit tests don't show the detached frame, so move the root of its component tree instead
      generateSequence<Component>(detached.owner) { it.parent }.last().setLocation(screenX, screenY)
    }
    return detached!!
  }

  private fun select(window: EditorWindow) {
    ApplicationManager.getApplication().invokeAndWait {
      fileEditorManager.currentWindow = window
    }
  }

  // Unit tests have no focus, so the current window never moves to a detached one and the order is checked directly
  private fun assertWindowOrder(vararg expected: EditorWindow) {
    ApplicationManager.getApplication().invokeAndWait {
      assertEquals(expected.toList(), WindowGroup.allWindowsInScreenOrder(fileEditorManager))
    }
  }

  private fun assertCurrentWindow(expected: EditorWindow) {
    ApplicationManager.getApplication().invokeAndWait {
      assertSame(expected, fileEditorManager.currentWindow)
    }
  }
}
