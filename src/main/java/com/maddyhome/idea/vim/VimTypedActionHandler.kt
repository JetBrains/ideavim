/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.ActionPlan
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx
import com.intellij.openapi.progress.ProcessCanceledException
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.key.KeyHandlerKeeper
import com.maddyhome.idea.vim.newapi.vim
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Accepts all regular keystrokes and passes them on to the Vim key handler.
 *
 * IDE shortcut keys used by Vim commands are handled by [com.maddyhome.idea.vim.action.VimShortcutKeyAction].
 *
 * This class is used in Which-Key plugin, so don't make it internal. Generally, we should provide a proper
 *   way to get ideavim keys for this plugin. See VIM-3085
 */
class VimTypedActionHandler(origHandler: TypedActionHandler) : TypedActionHandlerEx {
  private val handler = KeyHandler.getInstance()
  private val traceTime = injector.globalOptions().ideatracetime

  init {
    KeyHandlerKeeper.getInstance().originalHandler = origHandler
  }

  override fun beforeExecute(editor: Editor, charTyped: Char, context: DataContext, plan: ActionPlan) {
    LOG.trace("Before execute for typed action")
    if (editor.isIdeaVimDisabledHere) {
      LOG.trace("IdeaVim disabled here, finish")
      (KeyHandlerKeeper.getInstance().originalHandler as? TypedActionHandlerEx)?.beforeExecute(
        editor,
        charTyped,
        context,
        plan
      )
      return
    }

    LOG.trace("Executing before execute")
    val modifiers = if (charTyped == ' ' && VimKeyListener.isSpaceShift) KeyEvent.SHIFT_DOWN_MASK else 0
    val keyStroke = KeyStroke.getKeyStroke(charTyped, modifiers)

    /* Invoked before acquiring a write lock and actually handling the keystroke.
     *
     * Drafts an optional [ActionPlan] that will be used as a base for zero-latency rendering in editor.
     */
    if (editor.inInsertMode) {
      val originalHandler = KeyHandlerKeeper.getInstance().originalHandler
      if (originalHandler is TypedActionHandlerEx) {
        originalHandler.beforeExecute(editor, keyStroke.keyChar, context, plan)
      }
    }
  }

  override fun execute(editor: Editor, charTyped: Char, context: DataContext) {
    LOG.trace("Execute for typed action")
    if (editor.isIdeaVimDisabledHere) {
      LOG.trace("IdeaVim disabled here, finish")
      KeyHandlerKeeper.getInstance().originalHandler.execute(editor, charTyped, context)
      return
    }

    try {
      LOG.trace("Executing typed action")
      val modifiers = if (charTyped == ' ' && VimKeyListener.isSpaceShift) KeyEvent.SHIFT_DOWN_MASK else 0
      val keyStroke = KeyStroke.getKeyStroke(charTyped, modifiers)
      val startTime = if (traceTime) System.currentTimeMillis() else null
      handler.handleKey(editor.vim, keyStroke, context.vim, handler.keyHandlerState)
      if (startTime != null) {
        val duration = System.currentTimeMillis() - startTime
        LOG.info("VimTypedAction '$charTyped': $duration ms")
      }
    } catch (e: ProcessCanceledException) {
      throw e
    } catch (e: Throwable) {
      LOG.error(e)
    }
  }

  internal companion object {
    private val LOG = logger<VimTypedActionHandler>()
  }
}

/**
 * A nasty workaround to handle `<S-Space>` events. Probably all the key events should go trough this listener.
 */
internal object VimKeyListener : KeyAdapter() {

  var isSpaceShift = false

  override fun keyPressed(e: KeyEvent) {
    isSpaceShift = e.modifiersEx and KeyEvent.SHIFT_DOWN_MASK != 0 && e.keyChar == ' '
  }
}
