/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.actionSystem.impl.rearrangeByPromotersImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.ui.KeyStrokeAdapter
import com.maddyhome.idea.vim.action.OutsideEditorKeyDispatcher
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.globalIjOptions
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitAndAssert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.beans.PropertyChangeEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.KeyStroke
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * VIM-3667: mappings to IDE actions (e.g. `map <leader>sf <Action>(GotoFile)`) must keep working when no file
 * is open, i.e. when the IDE has no editor to deliver the keys to.
 *
 * `GotoFile`/`GotoSymbol` open the Search Everywhere popup, which cannot be shown in a headless test, so the tests
 * map to small recording actions instead. What is asserted is exactly what the user observes: whether the mapped
 * action ran, how many times, and that it received the project it needs.
 */
class ShortcutOutsideEditorTest : VimTestCase() {
  private lateinit var gotoSymbol: RecordingAction
  private lateinit var gotoFile: RecordingAction

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    gotoSymbol = RecordingAction().also { ActionManager.getInstance().registerAction(GOTO_SYMBOL_ID, it) }
    gotoFile = RecordingAction().also { ActionManager.getInstance().registerAction(GOTO_FILE_ID, it) }
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    ActionManager.getInstance().unregisterAction(GOTO_SYMBOL_ID)
    ActionManager.getInstance().unregisterAction(GOTO_FILE_ID)
    super.tearDown(testInfo)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test mapping to action runs when no file is open`() {
    configureByText("")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)")
    closeAllFiles()

    typeKeysOutsideEditor("\\ws")

    assertEquals(1, gotoSymbol.invocations.size, "Mapped action should run once when no editor is open")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test mapping does not run when no file is open and ideaoutsideeditor is off`() {
    configureByText("")
    enterCommand("set noideaoutsideeditor")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)")
    closeAllFiles()

    assertFalse(isKeyClaimedOutsideEditor('\\'), "No key must be claimed when the option is off")
    typeKeysOutsideEditor("\\ws")

    assertEquals(0, gotoSymbol.invocations.size)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test turning ideaoutsideeditor off unregisters the shortcuts from the focused component`() {
    configureByText("")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)")
    closeAllFiles()

    val dispatcher = OutsideEditorKeyDispatcher.getInstance()
    ApplicationManager.getApplication().invokeAndWait {
      val panel = JPanel()
      dispatcher.focusListener.propertyChange(focusOwnerChange(from = null, to = panel))
      assertTrue(ActionUtil.getActions(panel).contains(dispatcher), "Precondition: focusing the panel registers keys")

      injector.globalIjOptions().ideaoutsideeditor = false
      assertFalse(
        ActionUtil.getActions(panel).contains(dispatcher),
        "Turning the option off must unregister the shortcuts without waiting for a focus change",
      )
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test Escape is not registered when no mapping uses it`() {
    configureByText("")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)")
    closeAllFiles()

    val dispatcher = OutsideEditorKeyDispatcher.getInstance()
    ApplicationManager.getApplication().invokeAndWait {
      dispatcher.focusListener.propertyChange(focusOwnerChange(from = null, to = JPanel()))
      assertFalse(
        dispatcher.registeredKeys().contains(ESCAPE),
        "A registered Escape hides the local Escape actions of the parent components, e.g. closing a popup",
      )
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test mapping to action on Escape runs when no file is open`() {
    configureByText("")
    enterCommand("nmap <Esc> <Action>($GOTO_SYMBOL_ID)")
    closeAllFiles()

    val dispatcher = OutsideEditorKeyDispatcher.getInstance()
    ApplicationManager.getApplication().invokeAndWait {
      dispatcher.focusListener.propertyChange(focusOwnerChange(from = null, to = JPanel()))
      assertTrue(dispatcher.registeredKeys().contains(ESCAPE), "Precondition: the mapped Escape is registered")
    }
    pressKeyOutsideEditor(ESCAPE)

    assertEquals(1, gotoSymbol.invocations.size, "Escape mapped by the user must run its action like any other key")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test action invoked without editor receives the project`() {
    configureByText("")
    enterCommand("map <leader>sf <Action>($GOTO_FILE_ID)")
    closeAllFiles()

    typeKeysOutsideEditor("\\sf")

    assertEquals(1, gotoFile.invocations.size)
    assertSame(
      fixture.project,
      gotoFile.invocations.single(),
      "GotoFile-like actions need a project to open their popup"
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test only the matching mapping runs when several are defined`() {
    configureByText("")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)")
    enterCommand("map <leader>sf <Action>($GOTO_FILE_ID)")
    closeAllFiles()

    typeKeysOutsideEditor("\\sf")

    assertEquals(0, gotoSymbol.invocations.size)
    assertEquals(1, gotoFile.invocations.size)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test both mappings from the issue can be used one after another`() {
    configureByText("")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)")
    enterCommand("map <leader>sf <Action>($GOTO_FILE_ID)")
    closeAllFiles()

    typeKeysOutsideEditor("\\ws")
    typeKeysOutsideEditor("\\sf")
    typeKeysOutsideEditor("\\ws")

    assertEquals(2, gotoSymbol.invocations.size)
    assertEquals(1, gotoFile.invocations.size)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test mapping with custom mapleader runs when no file is open`() {
    configureByText("")
    enterCommand("let mapleader = \",\"")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)")
    closeAllFiles()

    typeKeysOutsideEditor(",ws")

    assertEquals(1, gotoSymbol.invocations.size)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test nmap mapping runs when no file is open`() {
    configureByText("")
    enterCommand("nmap <leader>ws <Action>($GOTO_SYMBOL_ID)")
    closeAllFiles()

    typeKeysOutsideEditor("\\ws")

    assertEquals(1, gotoSymbol.invocations.size, "Without an editor IdeaVim is in Normal mode, so nmap applies")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test imap mapping does not run when no file is open`() {
    configureByText("")
    enterCommand("imap <leader>ws <Action>($GOTO_SYMBOL_ID)")
    closeAllFiles()

    typeKeysOutsideEditor("\\ws")

    assertEquals(0, gotoSymbol.invocations.size, "Without an editor IdeaVim is in Normal mode, so imap must not apply")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test mapping to several actions runs all of them in order`() {
    configureByText("")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)<Action>($GOTO_FILE_ID)")
    closeAllFiles()

    typeKeysOutsideEditor("\\ws")

    assertEquals(1, gotoSymbol.invocations.size)
    assertEquals(1, gotoFile.invocations.size)
    assertTrue(gotoSymbol.lastInvocationOrder < gotoFile.lastInvocationOrder, "Actions must run in mapping order")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test unmapped keys typed without editor do not run anything`() {
    configureByText("")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)")
    closeAllFiles()

    typeKeysOutsideEditor("xyz")

    assertEquals(0, gotoSymbol.invocations.size)
    assertPluginError(false)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test mapping keeps working in editor after being used without editor`() {
    configureByText("")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)")
    closeAllFiles()
    typeKeysOutsideEditor("\\ws")
    assertEquals(1, gotoSymbol.invocations.size)

    configureByText("lorem ipsum")
    typeText("\\ws")

    assertEquals(2, gotoSymbol.invocations.size, "Using the mapping outside the editor must not break it inside")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test only keys of a user mapping are claimed outside editor`() {
    configureByText("")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)")
    closeAllFiles()

    assertTrue(isKeyClaimedOutsideEditor('\\'), "First key of a mapping must be claimed")
    assertFalse(isKeyClaimedOutsideEditor('x'), "A key outside any mapping must be left to the IDE")
    assertFalse(isKeyClaimedOutsideEditor('w'), "Second key of a mapping is not claimed before the first one is typed")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test keys of an insert mode mapping are not claimed outside editor`() {
    configureByText("")
    enterCommand("imap <leader>ws <Action>($GOTO_SYMBOL_ID)")
    closeAllFiles()

    assertFalse(isKeyClaimedOutsideEditor('\\'))
  }

  // region Only mappings to IDE actions make sense without an editor

  @Test
  fun `test mapping to Vim keys is not claimed outside editor`() {
    configureByText("")
    enterCommand("nmap j gj")
    closeAllFiles()

    assertFalse(isKeyClaimedOutsideEditor('j'), "'j' must keep starting speed search in the Project tool window")
  }

  @Test
  fun `test typing a key of a Vim keys mapping outside editor does nothing and does not fail`() {
    configureByText("")
    enterCommand("nmap j gj")
    closeAllFiles()

    // Any logged error fails the test through TestLoggerFactory: the hidden fallback editor cannot run `gj`
    typeKeysOutsideEditor("j")

    assertPluginError(false)
  }

  @Test
  fun `test window command mapping is not claimed outside editor`() {
    configureByText("")
    enterCommand("nnoremap <C-h> <C-w>h")
    closeAllFiles()

    assertFalse(isKeyClaimedOutsideEditor(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK)))
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test mapping mixing action and Vim keys is not claimed outside editor`() {
    configureByText("")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)x")
    closeAllFiles()

    assertFalse(isKeyClaimedOutsideEditor('\\'))
  }

  // endregion

  // region Keys bound to IDE actions of the focused component

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test mapping on Space wins over an IDE action bound to Space`() {
    configureByText("")
    enterCommand("nmap <Space><Space> <Action>($GOTO_FILE_ID)")
    closeAllFiles()

    val preview = RecordingAction()
    val projectView = focusedComponentWithActionOn(KeyEvent.VK_SPACE, preview)
    pressKeyOn(projectView, ' ')
    pressKeyOn(projectView, ' ')

    assertEquals(1, gotoFile.invocations.size, "The user mapping must run")
    assertEquals(0, preview.invocations.size, "Space is mapped by the user, so the IDE action must not get it")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test Space that does not start a mapping is left to the IDE action bound to it`() {
    configureByText("")
    enterCommand("nmap a<Space> <Action>($GOTO_FILE_ID)")
    closeAllFiles()

    val preview = RecordingAction()
    val projectView = focusedComponentWithActionOn(KeyEvent.VK_SPACE, preview)
    pressKeyOn(projectView, ' ')

    assertEquals(1, preview.invocations.size)
    assertEquals(0, gotoFile.invocations.size)
  }

  // endregion

  // region Modal dialogs

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test mapping is not claimed in a modal dialog`() {
    configureByText("")
    closeAllFiles()
    ApplicationManager.getApplication().invokeAndWait {
      assertFalse(
        OutsideEditorKeyDispatcher.shouldHandle(JPanel(), fixture.project, isModalContext = true),
        "Shortcuts registered in a dialog hide its local actions",
      )
      assertTrue(OutsideEditorKeyDispatcher.shouldHandle(JPanel(), fixture.project, isModalContext = false))
    }
  }

  // endregion

  // region Mode is global: the file may have been closed while in Insert mode

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test mapping runs on first attempt after file was closed in insert mode`() {
    configureByText("")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)")
    typeText("i")
    closeAllFiles()

    typeKeysOutsideEditor("\\ws")

    assertEquals(
      1,
      gotoSymbol.invocations.size,
      "Without an editor there is only Normal mode, whatever mode the closed file was in"
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test InsertLeave autocmd does not fire for keys typed outside editor`() {
    configureByText("")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)")
    enterCommand("autocmd InsertLeave * action $GOTO_FILE_ID")
    typeText("i")
    closeAllFiles()

    typeKeysOutsideEditor("\\ws")

    assertEquals(1, gotoSymbol.invocations.size)
    assertEquals(
      0,
      gotoFile.invocations.size,
      "There is no buffer to leave Insert mode in, so InsertLeave must not fire"
    )
  }

  // endregion

  // region Unfinished sequence dropped by 'timeout'

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test mapping works after a previous sequence timed out`() {
    configureByText("")
    enterCommand("map <leader>dd <Action>($GOTO_SYMBOL_ID)")
    enterCommand("set timeoutlen=100")
    closeAllFiles()

    typeKeysOutsideEditor("\\d")
    // Once the sequence is dropped, a lone 'd' no longer continues anything and must be left to the IDE
    waitAndAssert(300) { !isKeyClaimedOutsideEditor('d') }

    typeKeysOutsideEditor("\\dd")

    assertEquals(1, gotoSymbol.invocations.size)
    assertPluginError(false)
  }

  // endregion

  // region Macro recording

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test keys typed outside editor are not recorded into a macro`() {
    configureByText("lorem")
    enterCommand("map <leader>ws <Action>($GOTO_SYMBOL_ID)")
    typeText("qa")
    closeAllFiles()
    typeKeysOutsideEditor("\\ws")
    assertEquals(1, gotoSymbol.invocations.size)

    configureByText("ipsum")
    typeText("q")
    typeText("@a")

    assertEquals(
      1,
      gotoSymbol.invocations.size,
      "Replaying the macro must not run the mapping typed outside the editor"
    )
  }

  // endregion

  @Test
  fun `test dispatcher is not active while a file is open`() {
    configureByText("")
    ApplicationManager.getApplication().invokeAndWait {
      assertFalse(OutsideEditorKeyDispatcher.shouldHandle(JPanel(), fixture.project))
    }
  }

  @Test
  fun `test dispatcher is active on a non-text component when no file is open`() {
    configureByText("")
    closeAllFiles()
    ApplicationManager.getApplication().invokeAndWait {
      assertTrue(OutsideEditorKeyDispatcher.shouldHandle(JPanel(), fixture.project))
    }
  }

  @Test
  fun `test dispatcher is not active on text fields even when no file is open`() {
    configureByText("")
    closeAllFiles()
    ApplicationManager.getApplication().invokeAndWait {
      assertFalse(
        OutsideEditorKeyDispatcher.shouldHandle(JTextField(), fixture.project),
        "Typing in e.g. Search Everywhere must not be intercepted"
      )
    }
  }

  @Test
  fun `test dispatcher is not active inside a popup`() {
    configureByText("")
    closeAllFiles()
    ApplicationManager.getApplication().invokeAndWait {
      val list = JPanel()
      val popupContent = JPanel().apply { add(list) }
      val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(popupContent, null).createPopup()
      try {
        popupContent.putClientProperty(JBPopup.KEY, popup)
        assertFalse(
          OutsideEditorKeyDispatcher.shouldHandle(list, fixture.project),
          "Shortcuts registered inside a popup hide its own ones, e.g. Escape closing Search Everywhere"
        )
      } finally {
        Disposer.dispose(popup)
      }
    }
  }

  @Test
  fun `test dispatcher is not active without a project`() {
    ApplicationManager.getApplication().invokeAndWait {
      assertFalse(OutsideEditorKeyDispatcher.shouldHandle(JPanel(), null))
    }
  }

  private fun focusOwnerChange(from: Any?, to: Any?) =
    PropertyChangeEvent(KeyboardFocusManager.getCurrentKeyboardFocusManager(), "focusOwner", from, to)

  private fun closeAllFiles() {
    ApplicationManager.getApplication().invokeAndWait {
      fileManager.closeAllFiles()
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
    assertNull(
      FileEditorManager.getInstance(fixture.project).selectedTextEditor,
      "Precondition: all editors must be closed",
    )
  }

  /**
   * Types [keys] the way the IDE delivers them when the focus is on a non-editor component (e.g. the Project tool
   * window): each character is a KEY_TYPED event whose data context carries the project but no editor, handed to
   * [OutsideEditorKeyDispatcher] exactly as the keymap dispatcher would do for a registered custom shortcut.
   */
  private fun typeKeysOutsideEditor(keys: String) {
    for (char in keys) pressKeyOutsideEditor(KeyStroke.getKeyStroke(char))
  }

  private fun pressKeyOutsideEditor(keyStroke: KeyStroke) {
    val action = OutsideEditorKeyDispatcher.getInstance()
    ApplicationManager.getApplication().invokeAndWait {
      // Like IdeKeyEventDispatcher: the action is asked whether it wants the key, and only then performed
      val actionEvent = outsideEditorKeyEvent(keyStroke)
      action.update(actionEvent)
      if (actionEvent.presentation.isEnabled) {
        ActionUtil.performActionDumbAwareWithCallbacks(action, actionEvent)
      }
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
  }

  /** A component like the Project view tree, which opens a preview on Space, focused while no editor is open */
  private fun focusedComponentWithActionOn(keyCode: Int, action: AnAction): JPanel {
    val component = JPanel()
    ApplicationManager.getApplication().invokeAndWait {
      action.registerCustomShortcutSet(CustomShortcutSet(KeyStroke.getKeyStroke(keyCode, 0)), component)
      OutsideEditorKeyDispatcher.getInstance().focusListener.propertyChange(focusOwnerChange(from = null, to = component))
    }
    return component
  }

  /**
   * Presses a key the way the IDE delivers it: KEY_PRESSED first, then KEY_TYPED, which the platform drops when an
   * action already took the KEY_PRESSED event.
   */
  private fun pressKeyOn(component: JComponent, char: Char) {
    ApplicationManager.getApplication().invokeAndWait {
      val now = System.currentTimeMillis()
      val pressed = KeyEvent(component, KeyEvent.KEY_PRESSED, now, 0, KeyEvent.getExtendedKeyCodeForChar(char.code), char)
      if (!dispatchToActions(component, pressed)) {
        dispatchToActions(component, KeyEvent(component, KeyEvent.KEY_TYPED, now, 0, KeyEvent.VK_UNDEFINED, char))
      }
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
  }

  /**
   * Mirrors IdeKeyEventDispatcher: the local actions come from the first component up the hierarchy that has any for
   * the shortcut, are ordered by the promoters, and the first enabled one is performed and consumes the event.
   */
  private fun dispatchToActions(component: JComponent, event: KeyEvent): Boolean {
    val shortcut = KeyboardShortcut(KeyStrokeAdapter.getDefaultKeyStroke(event), null)
    val candidates = generateSequence<Component>(component) { it.parent }
      .filterIsInstance<JComponent>()
      .map { c -> ActionUtil.getActions(c).filter { action -> action.shortcutSet.shortcuts.any { it.startsWith(shortcut) } } }
      .firstOrNull { it.isNotEmpty() } ?: return false
    val dataContext = SimpleDataContext.builder()
      .add(CommonDataKeys.PROJECT, fixture.project)
      .add(PlatformCoreDataKeys.CONTEXT_COMPONENT, component)
      .build()
    for (action in rearrangeByPromotersImpl(candidates, dataContext, ActionPromoter.EP_NAME.extensionList)) {
      val actionEvent = AnActionEvent(
        event,
        dataContext,
        ActionPlaces.KEYBOARD_SHORTCUT,
        action.templatePresentation.clone(),
        ActionManager.getInstance(),
        0,
      )
      action.update(actionEvent)
      if (actionEvent.presentation.isEnabled) {
        ActionUtil.performActionDumbAwareWithCallbacks(action, actionEvent)
        return true
      }
    }
    return false
  }

  private fun OutsideEditorKeyDispatcher.registeredKeys(): List<KeyStroke> =
    shortcutSet.shortcuts.filterIsInstance<KeyboardShortcut>().map { it.firstKeyStroke }

  /** Must be called on the EDT. A typed character becomes a KEY_TYPED event, anything else a KEY_PRESSED one */
  private fun outsideEditorKeyEvent(keyStroke: KeyStroke): AnActionEvent {
    val nonEditorComponent = JPanel()
    val dataContext = SimpleDataContext.builder()
      .add(CommonDataKeys.PROJECT, fixture.project)
      .add(PlatformCoreDataKeys.CONTEXT_COMPONENT, nonEditorComponent)
      .build()
    val keyEvent = if (keyStroke.keyChar != KeyEvent.CHAR_UNDEFINED) {
      KeyEvent(
        nonEditorComponent,
        KeyEvent.KEY_TYPED,
        System.currentTimeMillis(),
        0,
        KeyEvent.VK_UNDEFINED,
        keyStroke.keyChar
      )
    } else {
      KeyEvent(
        nonEditorComponent,
        KeyEvent.KEY_PRESSED,
        System.currentTimeMillis(),
        keyStroke.modifiers,
        keyStroke.keyCode,
        KeyEvent.CHAR_UNDEFINED
      )
    }
    return AnActionEvent(
      keyEvent,
      dataContext,
      ActionPlaces.KEYBOARD_SHORTCUT,
      OutsideEditorKeyDispatcher.getInstance().templatePresentation.clone(),
      ActionManager.getInstance(),
      0,
    )
  }

  private fun isKeyClaimedOutsideEditor(char: Char): Boolean = isKeyClaimedOutsideEditor(KeyStroke.getKeyStroke(char))

  private fun isKeyClaimedOutsideEditor(keyStroke: KeyStroke): Boolean {
    var enabled = false
    ApplicationManager.getApplication().invokeAndWait {
      val event = outsideEditorKeyEvent(keyStroke)
      OutsideEditorKeyDispatcher.getInstance().update(event)
      enabled = event.presentation.isEnabled
    }
    return enabled
  }

  private class RecordingAction : AnAction(), DumbAware {
    val invocations = mutableListOf<Project?>()
    var lastInvocationOrder = -1

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
      invocations += e.project
      lastInvocationOrder = globalOrder++
    }
  }

  private companion object {
    const val GOTO_SYMBOL_ID = "IdeaVimTest.GotoSymbol"
    const val GOTO_FILE_ID = "IdeaVimTest.GotoFile"
    val ESCAPE: KeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
    var globalOrder = 0
  }
}
