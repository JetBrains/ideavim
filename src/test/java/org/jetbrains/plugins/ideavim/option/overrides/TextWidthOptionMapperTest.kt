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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.SettingsImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.platform.util.coroutines.childScope
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.WrapOnTyping
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.replaceService
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitUntil
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import javax.swing.SwingConstants
import kotlin.test.assertEquals

@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class TextWidthOptionMapperTest : VimTestCase() {

  // IntelliJ can have a margin set, but not act on it. We want to maintain this, not least because the right margin
  // visual guide is shown by default
  private val defaultRightMargin = 120

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    // Copied from FileEditorManagerTestCase to allow us to split windows
    val manager =
      FileEditorManagerImpl(fixture.project, (fixture.project as ComponentManagerEx).getCoroutineScope().childScope(name = "TextWidthOptionMapperTest"))
    fixture.project.replaceService(FileEditorManager::class.java, manager, fixture.testRootDisposable)

    ApplicationManager.getApplication().invokeAndWait {
      configureByText("\n")
    }
  }

  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createFixtureBuilder("IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture)
  }

  @Suppress("SameParameterValue")
  private fun openNewBufferWindow(filename: String, content: String): Editor {
    // This replaces fixture.editor
    ApplicationManager.getApplication().invokeAndWait {
      fixture.openFileInEditor(fixture.createFile(filename, content))

      // But our selection changed callback doesn't get called immediately, and that callback will deactivate the ex entry
      // panel (which causes problems if our next command is `:set`). So type something (`0` is a good no-op) to give time
      // for the event to propagate
      typeText("0")
    }

    return fixture.editor
  }


  // Note that the new split window (in a new EditorWindow) will be selected!
  private fun openSplitWindow(editor: Editor): Editor {
    val fileManager = FileEditorManagerEx.getInstanceEx(fixture.project)
    var split: EditorWindow? = null
    ApplicationManager.getApplication().invokeAndWait {
      val currentWindow = fileManager.currentWindow
      split = currentWindow!!.split(
        SwingConstants.VERTICAL,
        true,
        editor.virtualFile,
        false
      )
    }

    // Waiting till the selected editor will appear
    waitUntil {
      split!!.allComposites.first().selectedEditor != null
    }
    val newEditor = (split!!.allComposites.first().selectedEditor as TextEditor).editor

    ApplicationManager.getApplication().invokeAndWait {
      // As above - give the selection changed callback chance to catch up
      typeText("0")
    }

    return newEditor
  }

  @Test
  fun `test 'textwidth' defaults to current intellij setting`() {
    assertFalse(localWrapOnTyping)
    assertEquals(defaultRightMargin, localRightMargin)
    ApplicationManager.getApplication().invokeAndWait {
      assertEquals(0, optionsIj().textwidth)
    }
  }

  @Test
  fun `test 'textwidth' defaults to global intellij setting`() {
    assertFalse(globalWrapOnTyping)
    assertEquals(defaultRightMargin, globalRightMargin)
    ApplicationManager.getApplication().invokeAndWait {
      assertEquals(0, optionsIj().textwidth)
    }
  }

  @Test
  fun `test 'textwidth' option reports global intellij setting if not explicitly set`() {
    globalWrapOnTyping = true
    ApplicationManager.getApplication().invokeAndWait {
      globalRightMargin = 50
    }
    myAssertCommandOutput("set textwidth?", "  textwidth=50")

    globalWrapOnTyping = false
    myAssertCommandOutput("set textwidth?", "  textwidth=0")
  }

  @Test
  fun `test local 'textwidth' option reports global intellij setting if not explicitly set`() {
    globalWrapOnTyping = true
    ApplicationManager.getApplication().invokeAndWait {
      globalRightMargin = 50
    }
    myAssertCommandOutput("setlocal textwidth?", "  textwidth=50")

    globalWrapOnTyping = false
    myAssertCommandOutput("setlocal textwidth?", "  textwidth=0")
  }

  @Test
  fun `test 'textwidth' option reports local intellij setting if set via IDE`() {
    localWrapOnTyping = true
    localRightMargin = 60
    myAssertCommandOutput("set textwidth?", "  textwidth=60")

    localRightMargin = 70
    myAssertCommandOutput("set textwidth?", "  textwidth=70")

    localWrapOnTyping = false
    myAssertCommandOutput("set textwidth?", "  textwidth=0")
  }

  @Test
  fun `test local 'textwidth' option reports local intellij setting if set via IDE`() {
    localWrapOnTyping = true
    localRightMargin = 60
    myAssertCommandOutput("setlocal textwidth?", "  textwidth=60")

    localRightMargin = 70
    myAssertCommandOutput("setlocal textwidth?", "  textwidth=70")

    localWrapOnTyping = false
    myAssertCommandOutput("setlocal textwidth?", "  textwidth=0")
  }

  @Test
  fun `test set 'textwidth' modifies local intellij setting only`() {
    // Note that `:set` modifies both the local and global setting, but that global setting is a Vim setting, not the
    // global IntelliJ setting
    myEnterCommand("set textwidth=80")
    assertFalse(globalWrapOnTyping)
    assertTrue(localWrapOnTyping)
    assertEquals(80, localRightMargin)

    myEnterCommand("set textwidth=0")
    assertFalse(globalWrapOnTyping)
    assertFalse(localWrapOnTyping)
    assertEquals(80, localRightMargin)  // We don't reset the margin
  }

  @Test
  fun `test setlocal 'textwidth' modifies local intellij setting only`() {
    // Note that `:set` modifies both the local and global setting, but that global setting is a Vim setting, not the
    // global IntelliJ setting
    myEnterCommand("setlocal textwidth=80")
    assertFalse(globalWrapOnTyping)
    assertTrue(localWrapOnTyping)
    assertEquals(80, localRightMargin)

    myEnterCommand("setlocal textwidth=0")
    assertFalse(globalWrapOnTyping)
    assertFalse(localWrapOnTyping)
    assertEquals(80, localRightMargin)  // We don't reset the margin
  }

  @Test
  fun `test set 'textwidth' to 0 does not reset intellij margin setting`() {
    localRightMargin = defaultRightMargin
    localWrapOnTyping = true

    // Disabling textwidth does not reset the IntelliJ right margin. This is primarily because the right margin visual
    // guide is enabled by default, and setting it to 0 would draw the column unless we also disable the guide.
    // We reset the margin to the global value when we reset the Vim option to default, so if the user is confused, they
    // can reset to default. We should also look at implementing 'colorcolumn', and supporting the "+0" syntax to draw
    // the right margin guide dynamically
    myEnterCommand("set textwidth=0")
    assertFalse(localWrapOnTyping)
    assertEquals(defaultRightMargin, localRightMargin)
  }

  @Test
  fun `test setglobal 'textwidth' does not modify global intellij setting`() {
    assertFalse(globalWrapOnTyping)
    assertEquals(defaultRightMargin, globalRightMargin)
    myAssertCommandOutput("setglobal textwidth?", "  textwidth=0")

    myEnterCommand("setglobal textwidth=60")
    myAssertCommandOutput("setglobal textwidth?", "  textwidth=60")
    assertFalse(globalWrapOnTyping)
    assertEquals(defaultRightMargin, globalRightMargin)
  }

  @Test
  fun `test set 'textwidth' updates local and global ideavim values`() {
    myEnterCommand("set textwidth=40")
    myAssertCommandOutput("set textwidth?", "  textwidth=40")
    myAssertCommandOutput("setglobal textwidth?", "  textwidth=40")
  }

  @Test
  fun `test setting IDE value is treated like setlocal`() {
    // If we use `:set`, it updates the local and global values. If we set the value from the IDE, it only affects the
    // local value
    localWrapOnTyping = true
    localRightMargin = 80
    myAssertCommandOutput("setlocal textwidth?", "  textwidth=80")
    myAssertCommandOutput("set textwidth?", "  textwidth=80")
    myAssertCommandOutput("setglobal textwidth?", "  textwidth=0")
  }

  @Test
  fun `test reset 'textwidth' to default value copies current global intellij settings`() {
    globalWrapOnTyping = true
    ApplicationManager.getApplication().invokeAndWait {
      globalRightMargin = 80
    }

    localWrapOnTyping = false
    localRightMargin = 90
    myAssertCommandOutput("set textwidth?", "  textwidth=0")

    myEnterCommand("set textwidth&")
    myAssertCommandOutput("set textwidth?", "  textwidth=80")
    assertTrue(localWrapOnTyping)
    assertEquals(80, localRightMargin)

    // Verify that we've only copied the values instead of resetting the editor local settings
    globalWrapOnTyping = false
    myAssertCommandOutput("set textwidth?", "  textwidth=80")
  }

  @Test
  fun `test reset local 'textwidth' to default value copies current global intellij settings`() {
    globalWrapOnTyping = true
    ApplicationManager.getApplication().invokeAndWait {
      globalRightMargin = 80
    }

    localWrapOnTyping = false
    localRightMargin = 90
    myAssertCommandOutput("set textwidth?", "  textwidth=0")

    myEnterCommand("setlocal textwidth&")
    myAssertCommandOutput("set textwidth?", "  textwidth=80")
    assertTrue(localWrapOnTyping)
    assertEquals(80, localRightMargin)

    // Verify that we've only copied the values instead of resetting the editor local settings
    globalWrapOnTyping = false
    myAssertCommandOutput("set textwidth?", "  textwidth=80")
  }

  private fun myAssertCommandOutput(command: String, expected: String) {
    ApplicationManager.getApplication().invokeAndWait {
      assertCommandOutput(command, expected)
    }
  }

  private fun myEnterCommand(command: String) {
    ApplicationManager.getApplication().invokeAndWait {
      enterCommand(command)
    }
  }

  @Test
  fun `test open new window without setting ideavim will initialise 'textwidth' to defaults`() {
    // 'textwidth' is local-to-buffer, so doesn't get copied to new windows. We should have the default
    globalWrapOnTyping = true
    ApplicationManager.getApplication().invokeAndWait {
      globalRightMargin = 80
    }
    myAssertCommandOutput("set textwidth?", "  textwidth=80")

    openNewBufferWindow("bbb.txt", "lorem ipsum")

    myAssertCommandOutput("set textwidth?", "  textwidth=80")

    // Changing the global setting should update the new editor
    ApplicationManager.getApplication().invokeAndWait {
      globalRightMargin = 100
    }
    myAssertCommandOutput("set textwidth?", "  textwidth=100")
  }

  @Test
  fun `test open new window after setting ideavim value will initialise 'textwidth' to setglobal value`() {
    globalWrapOnTyping = true
    ApplicationManager.getApplication().invokeAndWait {
      globalRightMargin = 80
    }
    myEnterCommand("set textwidth=78")
    myAssertCommandOutput("set textwidth?", "  textwidth=78")

    openNewBufferWindow("bbb.txt", "lorem ipsum")

    myAssertCommandOutput("set textwidth?", "  textwidth=78")

    // Changing the global setting should NOT update the new editor
    globalRightMargin = 100
    myAssertCommandOutput("set textwidth?", "  textwidth=78")
  }

  @Test
  fun `test setglobal value used when opening new window`() {
    myEnterCommand("setglobal textwidth=50")

    ApplicationManager.getApplication().invokeAndWait {
      openNewBufferWindow("bbb.txt", "lorem ipsum")
    }

    myAssertCommandOutput("set textwidth?", "  textwidth=50")

    // Changing the global value should NOT update the editor
    globalWrapOnTyping = false
    myAssertCommandOutput("set textwidth?", "  textwidth=50")
  }

  @Test
  fun `test set local-to-buffer 'textwidth' option updates all editors for the buffer`() {
    val originalWindow = fixture.editor

    var newBufferWindow: Editor? = null
    ApplicationManager.getApplication().invokeAndWait {
      newBufferWindow = openNewBufferWindow("bbb.txt", "lorem ipsum")
    }
    val splitWindow = openSplitWindow(newBufferWindow!!)

    // The current window is newBufferWindow - bbb.txt
    // This should affect newBufferWindow and splitWindow, but not originalWindow
    myEnterCommand("set textwidth=50")

    assertFalse(originalWindow.settings.isWrapWhenTypingReachesRightMargin(fixture.project))
    assertTrue(newBufferWindow!!.settings.isWrapWhenTypingReachesRightMargin(fixture.project))
    assertEquals(50, newBufferWindow!!.settings.getRightMargin(fixture.project))
    assertTrue(splitWindow.settings.isWrapWhenTypingReachesRightMargin(fixture.project))
    assertEquals(50, splitWindow.settings.getRightMargin(fixture.project))
  }

  private var globalWrapOnTyping: Boolean
    get() {
      var language: Language? = null
      ApplicationManager.getApplication().invokeAndWait {
        language = TextEditorImpl.getDocumentLanguage(fixture.editor)
      }
      return CodeStyle.getSettings(fixture.editor).isWrapOnTyping(language)
    }
    set(value) {
      var language: Language? = null
      ApplicationManager.getApplication().invokeAndWait {
        language = TextEditorImpl.getDocumentLanguage(fixture.editor)
      }
      val commonSettings = CodeStyle.getSettings(fixture.editor).getCommonSettings(language)
      if (commonSettings.language == Language.ANY) {
        CodeStyle.getSettings(fixture.editor).WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN = value
      } else {
        commonSettings.WRAP_ON_TYPING = if (value) WrapOnTyping.WRAP.intValue else WrapOnTyping.NO_WRAP.intValue
      }
      // Setting the value directly doesn't invalidate the cached property value. Not sure if there's a better way
      (fixture.editor.settings as SettingsImpl).reinitSettings()
    }

  private var localWrapOnTyping: Boolean
    get() = fixture.editor.settings.isWrapWhenTypingReachesRightMargin(fixture.project)
    set(value) = fixture.editor.settings.setWrapWhenTypingReachesRightMargin(value)

  private var globalRightMargin: Int
    get() {
      var language: Language? = null
      ApplicationManager.getApplication().invokeAndWait {
        language = TextEditorImpl.getDocumentLanguage(fixture.editor)
      }
      return CodeStyle.getSettings(fixture.editor).getRightMargin(language)
    }
    set(value) {
      var language: Language? = null
      ApplicationManager.getApplication().invokeAndWait {
        language = TextEditorImpl.getDocumentLanguage(fixture.editor)
      }
      CodeStyle.getSettings(fixture.editor).setRightMargin(language, value)
      // Setting the value directly doesn't invalidate the cached property value. Not sure if there's a better way
      (fixture.editor.settings as SettingsImpl).reinitSettings()
    }

  private var localRightMargin: Int
    get() = fixture.editor.settings.getRightMargin(fixture.project)
    set(value) {
      ApplicationManager.getApplication().invokeAndWait {
        fixture.editor.settings.setRightMargin(value)
      }
    }
}
