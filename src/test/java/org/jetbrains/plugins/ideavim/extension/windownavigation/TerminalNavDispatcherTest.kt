/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.windownavigation

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.util.Disposer
import com.intellij.terminal.JBTerminalSystemSettingsProviderBase
import com.intellij.terminal.frontend.view.TerminalAllowedActionsProvider
import com.maddyhome.idea.vim.extension.windownavigation.TerminalNavDispatcher
import com.maddyhome.idea.vim.extension.windownavigation.ToolWindowNavDispatcher
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.awt.Component
import java.awt.DefaultKeyboardFocusManager
import java.awt.KeyboardFocusManager
import java.awt.event.FocusEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.lang.reflect.Proxy

/**
 * Runs the key dispatcher of the terminal itself. If the terminal starts to take the keys before the dispatchers that
 * were added to [IdeEventQueue] earlier, e.g. as a preprocessor, these tests fail.
 */
class TerminalNavDispatcherTest : VimTestCase() {
  private val shellKeys = mutableListOf<String>()
  private val navKeys = mutableListOf<String>()
  private val navDispatcher get() = service<ToolWindowNavDispatcher>()
  private lateinit var editor: EditorEx
  private val originalFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("")
    editor = fixture.editor as EditorEx
    onEdt {
      navDispatcher.cancelSequence()
      navDispatcher.register(editor.contentComponent)
    }
    focusEditor()
    recordNavKeys()
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    KeyboardFocusManager.setCurrentKeyboardFocusManager(originalFocusManager)
    onEdt { navDispatcher.unregisterCustomShortcutSet(editor.contentComponent) }
    super.tearDown(testInfo)
  }

  @Test
  fun `test without the dispatcher the shell gets ctrl-w`() {
    onEdt {
      startTerminal()
      pressCtrl(KeyEvent.VK_W)
      type('k')
    }
    assertEquals(listOf("pressed ^W", "typed ^W", "pressed K", "typed k"), shellKeys)
  }

  @Test
  fun `test ctrl-w k goes to the navigation instead of the shell`() {
    onEdt {
      addNavDispatcher()
      startTerminal()
      pressCtrl(KeyEvent.VK_W)
      type('k')
    }
    assertEquals(listOf("^W", "k"), navKeys)
    assertFalse(navDispatcher.isInSequence)
    assertEquals(emptyList<String>(), shellKeys)
  }

  @Test
  fun `test ctrl-w ctrl-l goes to the navigation`() {
    onEdt {
      addNavDispatcher()
      startTerminal()
      pressCtrl(KeyEvent.VK_W)
      pressCtrl(KeyEvent.VK_L)
    }
    assertEquals(listOf("^W", "^L"), navKeys)
    assertFalse(navDispatcher.isInSequence)
    assertEquals(emptyList<String>(), shellKeys)
  }

  @Test
  fun `test ctrl-w arrow goes to the navigation`() {
    onEdt {
      addNavDispatcher()
      startTerminal()
      pressCtrl(KeyEvent.VK_W)
      press(KeyEvent.VK_DOWN)
    }
    assertEquals(listOf("^W", KeyEvent.getKeyText(KeyEvent.VK_DOWN)), navKeys)
    assertFalse(navDispatcher.isInSequence)
    assertEquals(emptyList<String>(), shellKeys)
  }

  @Test
  fun `test keys after a completed sequence go to the shell`() {
    onEdt {
      addNavDispatcher()
      startTerminal()
      pressCtrl(KeyEvent.VK_W)
      type('h')
      type('k')
    }
    assertEquals(listOf("^W", "h"), navKeys)
    assertEquals(listOf("pressed K", "typed k"), shellKeys)
  }

  @Test
  fun `test escape cancels the sequence`() {
    onEdt {
      addNavDispatcher()
      startTerminal()
      pressCtrl(KeyEvent.VK_W)
      press(KeyEvent.VK_ESCAPE)
      type('k')
    }
    assertEquals(listOf("^W"), navKeys)
    assertEquals(listOf("pressed K", "typed k"), shellKeys)
  }

  @Test
  fun `test an unmapped key cancels the sequence`() {
    onEdt {
      addNavDispatcher()
      startTerminal()
      pressCtrl(KeyEvent.VK_W)
      type('x')
      type('k')
    }
    assertEquals(listOf("^W", "x"), navKeys)
    assertFalse(navDispatcher.isInSequence)
    assertEquals(listOf("pressed K", "typed k"), shellKeys)
  }

  @Test
  fun `test an unmapped key without a character cancels the sequence`() {
    onEdt {
      addNavDispatcher()
      startTerminal()
      pressCtrl(KeyEvent.VK_W)
      pressCtrl(KeyEvent.VK_C)
      type('k')
    }
    assertEquals(listOf("^W", "^C"), navKeys)
    assertEquals(listOf("pressed K", "typed k"), shellKeys)
  }

  @Test
  fun `test a sequence left unfinished elsewhere doesn't break the terminal one`() {
    onEdt {
      addNavDispatcher()
      startTerminal()
      navDispatcher.actionPerformed(ctrlWEvent())
      pressCtrl(KeyEvent.VK_W)
      type('k')
    }
    assertEquals(listOf("^W", "k"), navKeys)
    assertFalse(navDispatcher.isInSequence)
    assertEquals(emptyList<String>(), shellKeys)
  }

  @Test
  fun `test ctrl-shift-w goes to the shell`() {
    onEdt {
      addNavDispatcher()
      startTerminal()
      dispatch(KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_W, KeyEvent.CHAR_UNDEFINED)
    }
    assertEquals(emptyList<String>(), navKeys)
    assertEquals(listOf("pressed ^W"), shellKeys)
  }

  // Outside the terminal the action system runs the navigation, the dispatcher must not touch its sequence
  @Test
  fun `test outside the terminal ctrl-w is left to the action system`() {
    onEdt {
      addNavDispatcher(isInTerminal = false)
      startTerminal()
      pressCtrl(KeyEvent.VK_W)
      pressCtrl(KeyEvent.VK_L)
    }
    assertEquals(listOf("^W", "^L"), navKeys)
    assertFalse(navDispatcher.isInSequence)
    assertEquals(listOf("pressed ^W", "typed ^W", "pressed ^L", "typed ^L"), shellKeys)
  }

  // The navigation dispatcher is added when VimEverywhere starts, the terminal adds its own when it gets the focus
  private fun addNavDispatcher(isInTerminal: Boolean = true) {
    IdeEventQueue.getInstance().addDispatcher(TerminalNavDispatcher { isInTerminal }, fixture.testRootDisposable)
  }

  private fun startTerminal() {
    registerAllowedActionsExtensionPoint()
    val disposable = Disposer.newDisposable(fixture.testRootDisposable)
    setupKeyEventsHandling.invoke(null, editor, overridingIdeShortcuts(), shellKeysHandler(), disposable)
    // AWT drops a focus event for a component that can't have the focus, which is any component in a headless test
    val focusGained = FocusEvent(editor.contentComponent, FocusEvent.FOCUS_GAINED)
    editor.contentComponent.focusListeners.forEach { it.focusGained(focusGained) }
  }

  // The action system looks for the shortcuts from the focus owner, and there's none in a headless test
  private fun focusEditor() {
    KeyboardFocusManager.setCurrentKeyboardFocusManager(object : DefaultKeyboardFocusManager() {
      override fun getFocusOwner(): Component = editor.contentComponent
    })
  }

  private fun recordNavKeys() {
    ApplicationManager.getApplication().messageBus.connect(fixture.testRootDisposable)
      .subscribe(AnActionListener.TOPIC, object : AnActionListener {
        override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
          val keyEvent = event.inputEvent as? KeyEvent ?: return
          if (action === navDispatcher) navKeys.add(keyText(keyEvent))
        }
      })
  }

  private fun ctrlWEvent(): AnActionEvent {
    val keyEvent = KeyEvent(
      editor.contentComponent, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_W,
      KeyEvent.CHAR_UNDEFINED,
    )
    val context = SimpleDataContext.getSimpleContext(PlatformDataKeys.CONTEXT_COMPONENT, editor.contentComponent)
    return AnActionEvent.createEvent(context, null, "test", ActionUiKind.NONE, keyEvent)
  }

  private fun keyText(e: KeyEvent): String {
    if (e.id == KeyEvent.KEY_TYPED) {
      val char = e.keyChar
      return if (char < ' ') "^${char + 64}" else "$char"
    }
    val key = KeyEvent.getKeyText(e.keyCode)
    return if (e.isControlDown) "^$key" else key
  }

  // The test IDE has the classes of the terminal plugin, but doesn't load the plugin itself
  private fun registerAllowedActionsExtensionPoint() {
    val area = ApplicationManager.getApplication().extensionArea
    if (area.hasExtensionPoint(ALLOWED_ACTIONS_EP)) return
    area.registerExtensionPoint(ALLOWED_ACTIONS_EP, TerminalAllowedActionsProvider::class.java.name, ExtensionPoint.Kind.INTERFACE, false)
    Disposer.register(fixture.testRootDisposable) { area.unregisterExtensionPoint(ALLOWED_ACTIONS_EP) }
  }

  // The real settings need the services of the terminal plugin, which the test IDE doesn't load
  private fun overridingIdeShortcuts() = mock<JBTerminalSystemSettingsProviderBase> {
    on { overrideIdeShortcuts() } doReturn true
  }

  private fun pressCtrl(keyCode: Int) {
    dispatch(KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, keyCode, KeyEvent.CHAR_UNDEFINED)
    dispatch(KeyEvent.KEY_TYPED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_UNDEFINED, (keyCode - KeyEvent.VK_A + 1).toChar())
    dispatch(KeyEvent.KEY_RELEASED, InputEvent.CTRL_DOWN_MASK, keyCode, KeyEvent.CHAR_UNDEFINED)
  }

  private fun press(keyCode: Int) {
    dispatch(KeyEvent.KEY_PRESSED, 0, keyCode, KeyEvent.CHAR_UNDEFINED)
    dispatch(KeyEvent.KEY_RELEASED, 0, keyCode, KeyEvent.CHAR_UNDEFINED)
  }

  private fun type(char: Char) {
    val keyCode = KeyEvent.getExtendedKeyCodeForChar(char.code)
    dispatch(KeyEvent.KEY_PRESSED, 0, keyCode, KeyEvent.CHAR_UNDEFINED)
    dispatch(KeyEvent.KEY_TYPED, 0, KeyEvent.VK_UNDEFINED, char)
    dispatch(KeyEvent.KEY_RELEASED, 0, keyCode, KeyEvent.CHAR_UNDEFINED)
  }

  private fun dispatch(id: Int, modifiers: Int, keyCode: Int, char: Char) {
    val location = if (id == KeyEvent.KEY_TYPED) KeyEvent.KEY_LOCATION_UNKNOWN else KeyEvent.KEY_LOCATION_STANDARD
    val event = KeyEvent(editor.contentComponent, id, System.currentTimeMillis(), modifiers, keyCode, char, location)
    IdeEventQueue.getInstance().dispatchEvent(event)
  }

  // The function the terminal view sets up the key handling with. It is internal and its name and handler type differ
  // between the platform versions, hence the reflection
  private val setupKeyEventsHandling = Class.forName("com.intellij.terminal.frontend.view.impl.TerminalEventDispatcherKt")
    .methods.single { it.name == "setupKeyEventHandling" || it.name == "setupKeyEventsHandling" }

  private fun shellKeysHandler(): Any {
    val handlerType = setupKeyEventsHandling.parameterTypes[2]
    return Proxy.newProxyInstance(handlerType.classLoader, arrayOf(handlerType)) { proxy, method, args ->
      when (method.name) {
        "keyPressed" -> shellKeys.add("pressed ${keyText(keyEvent(args[0]))}")
        "keyTyped" -> shellKeys.add("typed ${keyText(keyEvent(args[0]))}")
        "equals" -> return@newProxyInstance proxy === args[0]
        "hashCode" -> return@newProxyInstance System.identityHashCode(proxy)
        "toString" -> return@newProxyInstance "shell keys handler"
      }
      null
    }
  }

  // Older platforms wrap the key event in TimedKeyEvent
  private fun keyEvent(event: Any): KeyEvent =
    event as? KeyEvent ?: event.javaClass.getMethod("getOriginal").invoke(event) as KeyEvent

  private fun onEdt(block: () -> Unit) {
    ApplicationManager.getApplication().invokeAndWait(block)
  }

  companion object {
    private const val ALLOWED_ACTIONS_EP = "org.jetbrains.plugins.terminal.allowedActionsProvider"
  }
}
