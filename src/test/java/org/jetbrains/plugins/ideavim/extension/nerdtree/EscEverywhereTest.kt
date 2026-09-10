/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.nerdtree

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.TestActionEvent
import com.maddyhome.idea.vim.extension.nerdtree.EscEverywhere
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.AWTEvent
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.beans.PropertyChangeEvent
import javax.swing.JPanel

class EscEverywhereTest : VimTestCase() {

  @Test
  fun `test focusing any component registers the shortcut`() {
    onEdt {
      val extension = EscEverywhere()
      val dispatcher = service<EscEverywhere.Dispatcher>()
      val component = JPanel()

      extension.focusListener.propertyChange(focusOwnerChange(from = null, to = component))
      assertTrue(
        ActionUtil.getActions(component).contains(dispatcher),
        "Focusing a component should register the <C-[> shortcut",
      )

      extension.focusListener.propertyChange(focusOwnerChange(from = component, to = null))
      assertFalse(
        ActionUtil.getActions(component).contains(dispatcher),
        "Leaving a component should unregister the shortcut",
      )
    }
  }

  // In an editor `<C-[>` is an ordinary Vim key that the key handler already treats as `<Esc>` - and that the user
  // may have mapped to something else. Stealing it there would break both.
  @Test
  fun `test the editor handles ctrl-bracket on its own`() {
    configureByText("Lorem ipsum")
    onEdt {
      val extension = EscEverywhere()
      val dispatcher = service<EscEverywhere.Dispatcher>()
      val editorComponent = fixture.editor.contentComponent

      extension.focusListener.propertyChange(focusOwnerChange(from = null, to = editorComponent))
      assertFalse(
        ActionUtil.getActions(editorComponent).contains(dispatcher),
        "The editor must keep <C-[> for IdeaVim itself",
      )
      assertFalse(isEnabledFor(editorComponent), "The dispatcher must stand down inside an editor")
    }
  }

  // The Escape has to go through IdeEventQueue itself - that is what gives it to IdePopupManager, to the keymap
  // actions and finally to the Swing bindings of the focused component, exactly as a real Escape press.
  @Test
  fun `test ctrl-bracket is replayed as an escape key press`() {
    onEdt {
      val dispatcher = service<EscEverywhere.Dispatcher>()
      val component = JPanel()
      // The event object itself must not be kept: once AWT gets it, `preDispatchKeyEvent` retargets it to the focus
      // owner, which is null in a headless test. That retargeting is also why posting the event does not work.
      val seen = mutableListOf<Replayed>()
      val disposable = Disposer.newDisposable("EscEverywhereTest")
      val recorder = object : IdeEventQueue.NonLockedEventDispatcher {
        override fun dispatch(e: AWTEvent): Boolean {
          if (e is KeyEvent) seen.add(Replayed(e.id, e.keyCode, e.modifiersEx, e.source))
          return false
        }
      }
      IdeEventQueue.getInstance().addDispatcher(recorder, disposable)

      try {
        dispatcher.actionPerformed(ctrlBracketEvent(component))
        // The replay is deferred: the <C-[> event is still being dispatched when the action runs
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
      } finally {
        Disposer.dispose(disposable)
      }

      val pressed = seen.firstOrNull { it.id == KeyEvent.KEY_PRESSED }
      assertNotNull(pressed, "<C-[> should be replayed as a key event")
      assertEquals(KeyEvent.VK_ESCAPE, pressed!!.keyCode, "The replayed key should be Escape")
      assertEquals(0, pressed.modifiers, "The replayed Escape should carry no modifiers")
      assertEquals(component, pressed.source, "The Escape should go to the component that had the focus")
      assertTrue(
        seen.any { it.id == KeyEvent.KEY_RELEASED && it.keyCode == KeyEvent.VK_ESCAPE },
        "A real Escape press is a press and a release",
      )
    }
  }

  private fun isEnabledFor(component: Component): Boolean {
    val dispatcher = service<EscEverywhere.Dispatcher>()
    val context = SimpleDataContext.builder().add(PlatformDataKeys.CONTEXT_COMPONENT, component).build()
    val event = TestActionEvent.createTestEvent(dispatcher, context)
    dispatcher.update(event)
    return event.presentation.isEnabled
  }

  private fun ctrlBracketEvent(component: Component): AnActionEvent {
    val keyEvent = KeyEvent(
      component,
      KeyEvent.KEY_PRESSED,
      System.currentTimeMillis(),
      InputEvent.CTRL_DOWN_MASK,
      KeyEvent.VK_OPEN_BRACKET,
      KeyEvent.CHAR_UNDEFINED,
    )
    val context = SimpleDataContext.builder().add(PlatformDataKeys.CONTEXT_COMPONENT, component).build()
    return AnActionEvent.createEvent(context, null, "test", ActionUiKind.NONE, keyEvent)
  }

  private fun focusOwnerChange(from: Any?, to: Any?) =
    PropertyChangeEvent(KeyboardFocusManager.getCurrentKeyboardFocusManager(), "focusOwner", from, to)

  private data class Replayed(val id: Int, val keyCode: Int, val modifiers: Int, val source: Any?)

  private fun onEdt(block: () -> Unit) {
    ApplicationManager.getApplication().invokeAndWait(block)
  }
}
