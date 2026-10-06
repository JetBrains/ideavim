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
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.action.VimShortcutKeyAction
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VimShortcutKeyActionTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `S-Tab is not a Vim-only editor key so sethandler can release it to the IDE`() {
    val shiftTab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)
    assertFalse(VimShortcutKeyAction.VIM_ONLY_EDITOR_KEYS.contains(shiftTab))
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `End is not a Vim-only editor key so sethandler can release it to the IDE`() {
    val modifiers = listOf(
      0,
      InputEvent.CTRL_DOWN_MASK,
      InputEvent.SHIFT_DOWN_MASK,
      InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK,
    )
    for (modifier in modifiers) {
      val end = KeyStroke.getKeyStroke(KeyEvent.VK_END, modifier)
      assertFalse(VimShortcutKeyAction.VIM_ONLY_EDITOR_KEYS.contains(end), "$end should not be a Vim-only editor key")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `Enter, Esc and arrows are not Vim-only editor keys so sethandler can release them to the IDE`() {
    val keyCodes = listOf(
      KeyEvent.VK_ENTER,
      KeyEvent.VK_ESCAPE,
      KeyEvent.VK_UP,
      KeyEvent.VK_DOWN,
      KeyEvent.VK_LEFT,
      KeyEvent.VK_RIGHT,
    )
    for (keyCode in keyCodes) {
      val key = KeyStroke.getKeyStroke(keyCode, 0)
      assertFalse(VimShortcutKeyAction.VIM_ONLY_EDITOR_KEYS.contains(key), "$key should not be a Vim-only editor key")
      assertTrue(VimShortcutKeyAction.VIM_DEFAULT_EDITOR_KEYS.contains(key), "$key should be handled by Vim by default")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `Esc is handled by Vim by default`() {
    configureByText("Lorem ipsum")
    typeText("i")
    assertTrue(isEnabledFor(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)))
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `Esc is passed to the IDE in modes where its handler is IDE`() {
    configureByText("Lorem ipsum")
    typeText(commandToKeys("sethandler <Esc> i:ide"))
    val esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
    assertTrue(isEnabledFor(esc), "Esc should stay with Vim in Normal mode")
    typeText("i")
    assertFalse(isEnabledFor(esc), "Esc should go to the IDE in Insert mode")
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `Enter is passed to the IDE when its handler is IDE`() {
    configureByText("Lorem ipsum")
    typeText(commandToKeys("sethandler <CR> a:ide"))
    assertFalse(isEnabledFor(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)))
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `handler for all shortcuts does not pass Enter and Esc to the IDE`() {
    configureByText("Lorem ipsum")
    typeText(commandToKeys("sethandler a:ide"))
    typeText("i")
    assertTrue(isEnabledFor(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)))
    assertTrue(isEnabledFor(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)))
  }

  private fun isEnabledFor(keyStroke: KeyStroke): Boolean {
    var enabled = false
    ApplicationManager.getApplication().invokeAndWait {
      val component = fixture.editor.contentComponent
      val keyEvent = KeyEvent(
        component,
        KeyEvent.KEY_PRESSED,
        System.currentTimeMillis(),
        keyStroke.modifiers,
        keyStroke.keyCode,
        KeyEvent.CHAR_UNDEFINED,
      )
      val dataContext = SimpleDataContext.builder()
        .add(CommonDataKeys.PROJECT, fixture.project)
        .add(CommonDataKeys.EDITOR, fixture.editor)
        .add(PlatformCoreDataKeys.CONTEXT_COMPONENT, component)
        .build()
      val action = VimShortcutKeyAction.instance
      val event = AnActionEvent(
        keyEvent,
        dataContext,
        ActionPlaces.KEYBOARD_SHORTCUT,
        action.templatePresentation.clone(),
        ActionManager.getInstance(),
        0,
      )
      action.update(event)
      enabled = event.presentation.isEnabled
    }
    return enabled
  }
}
