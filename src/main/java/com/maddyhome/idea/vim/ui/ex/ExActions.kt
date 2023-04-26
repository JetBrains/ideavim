/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.textarea.TextComponentEditorImpl
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.Action
import javax.swing.KeyStroke
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultEditorKit
import javax.swing.text.Document
import javax.swing.text.TextAction
import kotlin.math.abs
import kotlin.math.min

internal interface MultiStepAction : Action {
  fun reset()
}

internal class HistoryUpAction : TextAction(ExEditorKit.HistoryUp) {
  override fun actionPerformed(actionEvent: ActionEvent) {
    val target = getTextComponent(actionEvent) as ExTextField
    target.selectHistory(true, false)
  }
}

internal class HistoryDownAction : TextAction(ExEditorKit.HistoryDown) {
  override fun actionPerformed(actionEvent: ActionEvent) {
    val target = getTextComponent(actionEvent) as ExTextField
    target.selectHistory(false, false)
  }
}

internal class HistoryUpFilterAction : TextAction(ExEditorKit.HistoryUpFilter) {
  override fun actionPerformed(actionEvent: ActionEvent) {
    val target = getTextComponent(actionEvent) as ExTextField
    target.selectHistory(true, true)
  }
}

internal class HistoryDownFilterAction : TextAction(ExEditorKit.HistoryDownFilter) {
  override fun actionPerformed(actionEvent: ActionEvent) {
    val target = getTextComponent(actionEvent) as ExTextField
    target.selectHistory(false, true)
  }
}

internal class InsertRegisterAction : TextAction(ExEditorKit.InsertRegister), MultiStepAction {
  private enum class State {
    SKIP_CTRL_R, WAIT_REGISTER
  }

  private var state = State.SKIP_CTRL_R

  override fun actionPerformed(e: ActionEvent) {
    val target = getTextComponent(e) as ExTextField
    val key = ExEditorKit.convert(e)
    if (key != null) {
      when (state) {
        State.SKIP_CTRL_R -> {
          state = State.WAIT_REGISTER
          target.setCurrentAction(this, '\"')
        }
        State.WAIT_REGISTER -> {
          state = State.SKIP_CTRL_R
          target.clearCurrentAction()
          val c = key.keyChar
          if (c != KeyEvent.CHAR_UNDEFINED) {
            val register = VimPlugin.getRegister().getRegister(c)
            if (register != null) {
              val oldText = target.actualText
              val text = register.text
              if (text != null) {
                val offset = target.caretPosition
                target.text = oldText.substring(0, offset) + text + oldText.substring(offset)
                target.caretPosition = offset + text.length
              }
            }
          } else if (key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0 && key.keyCode == KeyEvent.VK_C) {
            // Eat any unused keys, unless it's <C-C>, in which case forward on and cancel entry
            target.handleKey(key)
          }
        }
      }
    }
  }

  override fun reset() {
    state = State.SKIP_CTRL_R
  }
}

internal class CompleteEntryAction : TextAction(ExEditorKit.CompleteEntry) {
  override fun actionPerformed(actionEvent: ActionEvent) {
    logger.debug("complete entry")
    val stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)

    // We send the <Enter> keystroke through the key handler rather than calling ProcessGroup#processExEntry directly.
    // We do this for a couple of reasons:
    // * The C mode mapping for ProcessExEntryAction handles the actual entry, and most importantly, it does so as a
    //   write action
    // * The key handler routines get the chance to clean up and reset state
    val entry = ExEntryPanel.getInstance().entry
    KeyHandler.getInstance().handleKey(entry.editor.vim, stroke, entry.context.vim)
  }

  companion object {
    private val logger = logger<CompleteEntryAction>()
  }
}

internal class CancelEntryAction : TextAction(ExEditorKit.CancelEntry) {
  override fun actionPerformed(e: ActionEvent) {
    val target = getTextComponent(e) as ExTextField
    target.cancel()
  }
}

internal class EscapeCharAction : TextAction(ExEditorKit.EscapeChar) {
  override fun actionPerformed(e: ActionEvent) {
    val target = getTextComponent(e) as ExTextField
    target.escape()
  }
}

internal abstract class DeleteCharAction internal constructor(name: String?) : TextAction(name) {
  @kotlin.jvm.Throws(BadLocationException::class)
  fun deleteSelection(doc: Document, dot: Int, mark: Int): Boolean {
    if (dot != mark) {
      doc.remove(min(dot, mark), abs(dot - mark))
      return true
    }
    return false
  }

  @kotlin.jvm.Throws(BadLocationException::class)
  fun deleteNextChar(doc: Document, dot: Int): Boolean {
    if (dot < doc.length) {
      var delChars = 1
      if (dot < doc.length - 1) {
        val dotChars = doc.getText(dot, 2)
        val c0 = dotChars[0]
        val c1 = dotChars[1]
        if (c0 in '\uD800'..'\uDBFF' && c1 in '\uDC00'..'\uDFFF') {
          delChars = 2
        }
      }
      doc.remove(dot, delChars)
      return true
    }
    return false
  }

  @kotlin.jvm.Throws(BadLocationException::class)
  fun deletePrevChar(doc: Document, dot: Int): Boolean {
    if (dot > 0) {
      var delChars = 1
      if (dot > 1) {
        val dotChars = doc.getText(dot - 2, 2)
        val c0 = dotChars[0]
        val c1 = dotChars[1]
        if (c0 in '\uD800'..'\uDBFF' && c1 in '\uDC00'..'\uDFFF') {
          delChars = 2
        }
      }
      doc.remove(dot - delChars, delChars)
      return true
    }
    return false
  }
}

internal class DeleteNextCharAction : DeleteCharAction(DefaultEditorKit.deleteNextCharAction) {
  override fun actionPerformed(e: ActionEvent) {
    val target = getTextComponent(e) as ExTextField
    target.saveLastEntry()
    try {
      val doc = target.document
      val caret = target.caret
      val dot = caret.dot
      val mark = caret.mark
      if (!deleteSelection(doc, dot, mark) && !deleteNextChar(doc, dot) && !deletePrevChar(doc, dot)) {
        target.cancel()
      }
    } catch (ex: BadLocationException) {
      // ignore
    }
  }
}

internal class DeletePreviousCharAction : DeleteCharAction(DefaultEditorKit.deletePrevCharAction) {
  override fun actionPerformed(e: ActionEvent) {
    val target = getTextComponent(e) as ExTextField
    target.saveLastEntry()
    try {
      val doc = target.document
      val caret = target.caret
      val dot = caret.dot
      val mark = caret.mark
      if (!deleteSelection(doc, dot, mark) && !deletePrevChar(doc, dot)) {
        if (dot == 0 && doc.length == 0) {
          target.cancel()
        }
      }
    } catch (bl: BadLocationException) {
      // ignore
    }
  }
}

internal class DeletePreviousWordAction : TextAction(DefaultEditorKit.deletePrevWordAction) {
  /**
   * Invoked when an action occurs.
   */
  override fun actionPerformed(e: ActionEvent) {
    val target = getTextComponent(e) as ExTextField
    target.saveLastEntry()
    val doc = target.document
    val caret = target.caret
    val project = target.editor.project

    // Note that we need an editor when searching because we need per-editor options (i.e. 'iskeyword')
    // TODO: We also need to initialise the options when creating TextComponentImpl
    val editor = TextComponentEditorImpl(project, target)
    val offset = injector.searchHelper.findNextWord(editor.vim, caret.dot, -1, bigWord = false, spaceWords = false)
    if (logger.isDebugEnabled) logger.debug("offset=$offset")
    try {
      val pos = caret.dot
      doc.remove(offset, pos - offset)
    } catch (ex: BadLocationException) {
      // ignore
    }
  }

  companion object {
    private val logger = logger<DeletePreviousWordAction>()
  }
}

internal class DeleteToCursorAction : TextAction(ExEditorKit.DeleteToCursor) {
  /**
   * Invoked when an action occurs.
   */
  override fun actionPerformed(e: ActionEvent) {
    val target = getTextComponent(e) as ExTextField
    target.saveLastEntry()
    val doc = target.document
    val caret = target.caret
    try {
      doc.remove(0, caret.dot)
    } catch (ex: BadLocationException) {
      // ignore
    }
  }
}

internal class ToggleInsertReplaceAction : TextAction(ExEditorKit.ToggleInsertReplace) {
  /**
   * Invoked when an action occurs.
   */
  override fun actionPerformed(e: ActionEvent) {
    logger.debug("actionPerformed")
    val target = getTextComponent(e) as ExTextField
    target.toggleInsertReplace()
  }

  init {
    logger.debug("ToggleInsertReplaceAction()")
  }

  companion object {
    private val logger = logger<ToggleInsertReplaceAction>()
  }
}
