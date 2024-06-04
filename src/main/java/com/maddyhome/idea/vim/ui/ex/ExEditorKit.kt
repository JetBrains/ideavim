/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ui.ex

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.annotations.NonNls
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.Action
import javax.swing.KeyStroke
import javax.swing.text.DefaultEditorKit
import javax.swing.text.Document
import javax.swing.text.TextAction

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
internal object ExEditorKit : DefaultEditorKit() {

  @NonNls
  val CancelEntry: String = "cancel-entry"

  @NonNls
  val CompleteEntry: String = "complete-entry"

  @NonNls
  val EscapeChar: String = "escape"

  @NonNls
  val DeleteToCursor: String = "delete-to-cursor"

  @NonNls
  val ToggleInsertReplace: String = "toggle-insert"

  @NonNls
  val HistoryUp: String = "history-up"

  @NonNls
  val HistoryDown: String = "history-down"

  @NonNls
  val HistoryUpFilter: String = "history-up-filter"

  @NonNls
  val HistoryDownFilter: String = "history-down-filter"

  @NonNls
  val StartDigraph: String = "start-digraph"

  @NonNls
  val StartLiteral: String = "start-literal"

  private val logger = logger<ExEditorKit>()

  /**
   * Gets the MIME type of the data that this
   * kit represents support for.
   *
   * @return the type
   */
  @NonNls
  override fun getContentType(): String {
    return "text/ideavim"
  }

  /**
   * Fetches the set of commands that can be used
   * on a text component that is using a model and
   * view produced by this kit.
   *
   * @return the set of actions
   */
  override fun getActions(): Array<Action> {
    val res = TextAction.augmentList(super.getActions(), exActions)
    logger.debug { "res.length=${res.size}" }
    return res
  }

  /**
   * Creates an uninitialized text storage model
   * that is appropriate for this type of editor.
   *
   * @return the model
   */
  override fun createDefaultDocument(): Document {
    return ExDocument()
  }

  private val exActions = arrayOf<Action>(
    CancelEntryAction(),
    CompleteEntryAction(),
    EscapeCharAction(),
    DeleteNextCharAction(),
    DeletePreviousCharAction(),
    DeletePreviousWordAction(),
    DeleteToCursorAction(),
    HistoryUpAction(),
    HistoryDownAction(),
    HistoryUpFilterAction(),
    HistoryDownFilterAction(),
    ToggleInsertReplaceAction(),
  )

  class DefaultExKeyHandler : DefaultKeyTypedAction() {
    override fun actionPerformed(e: ActionEvent) {
      val target = getTextComponent(e) as ExTextField
      val currentAction = target.currentAction
      if (currentAction != null) {
        currentAction.actionPerformed(e)
      } else {
        val key = convert(e)
        if (key != null) {
          val c = key.keyChar
          if (c.code > 0) {
            if (target.useHandleKeyFromEx) {
              val entry = ExEntryPanel.getInstance().entry
              val editor = entry.editor
              val keyHandler = KeyHandler.getInstance()
              keyHandler.handleKey(editor!!.vim, key, entry.context.vim, keyHandler.keyHandlerState)
            } else {
              val event = ActionEvent(e.source, e.id, c.toString(), e.getWhen(), e.modifiers)
              super.actionPerformed(event)
            }
            target.saveLastEntry()
          }
        } else {
          super.actionPerformed(e)
          target.saveLastEntry()
        }
      }
    }
  }

  fun convert(event: ActionEvent): KeyStroke? {
    val cmd = event.actionCommand
    val mods = event.modifiers
    if (cmd != null && cmd.isNotEmpty()) {
      val ch = cmd[0]
      if (ch < ' ') {
        if (mods and ActionEvent.CTRL_MASK != 0) {
          return KeyStroke.getKeyStroke(KeyEvent.VK_A + ch.code - 1, mods)
        }
      } else {
        return KeyStroke.getKeyStroke(Character.valueOf(ch), mods)
      }
    }
    return null
  }
}
