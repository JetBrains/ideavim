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
import com.maddyhome.idea.vim.api.LocalOptionInitialisationScenario
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

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
internal interface MultiStepAction : Action {
  fun reset()
}

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
internal class HistoryUpAction : TextAction(ExEditorKit.HistoryUp) {
  override fun actionPerformed(actionEvent: ActionEvent) {
    val target = getTextComponent(actionEvent) as ExTextField
    target.selectHistory(true, false)
  }
}

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
internal class HistoryDownAction : TextAction(ExEditorKit.HistoryDown) {
  override fun actionPerformed(actionEvent: ActionEvent) {
    val target = getTextComponent(actionEvent) as ExTextField
    target.selectHistory(false, false)
  }
}

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
internal class HistoryUpFilterAction : TextAction(ExEditorKit.HistoryUpFilter) {
  override fun actionPerformed(actionEvent: ActionEvent) {
    val target = getTextComponent(actionEvent) as ExTextField
    target.selectHistory(true, true)
  }
}

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
internal class HistoryDownFilterAction : TextAction(ExEditorKit.HistoryDownFilter) {
  override fun actionPerformed(actionEvent: ActionEvent) {
    val target = getTextComponent(actionEvent) as ExTextField
    target.selectHistory(false, true)
  }
}

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
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
    val keyHandler = KeyHandler.getInstance()
    keyHandler.handleKey(entry.editor!!.vim, stroke, entry.context.vim, keyHandler.keyHandlerState)
  }

  companion object {
    private val logger = logger<CompleteEntryAction>()
  }
}

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
internal class CancelEntryAction : TextAction(ExEditorKit.CancelEntry) {
  override fun actionPerformed(e: ActionEvent) {
    val target = getTextComponent(e) as ExTextField
    target.cancel()
  }
}

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
internal class EscapeCharAction : TextAction(ExEditorKit.EscapeChar) {
  override fun actionPerformed(e: ActionEvent) {
    val target = getTextComponent(e) as ExTextField
    target.escape()
  }
}

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
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

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
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

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
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

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
internal class DeletePreviousWordAction : TextAction(DefaultEditorKit.deletePrevWordAction) {
  /**
   * Invoked when an action occurs.
   */
  override fun actionPerformed(e: ActionEvent) {
    val target = getTextComponent(e) as ExTextField
    target.saveLastEntry()
    val doc = target.document
    val caret = target.caret
    val project = target.editor!!.project

    // Create a VimEditor instance on the Swing text field which we can pass to the search helpers. We need an editor
    // rather than just working on a buffer because the search helpers need local options (specifically the local to
    // buffer 'iskeyword'). We use the CMD_LINE scenario to initialise local options from the main editor. The options
    // service will copy all local-to-buffer and local-to-window options, effectively cloning the options.
    // TODO: Over time, we should migrate all ex actions to be based on VimEditor
    // This will mean we always have an editor that has been initialised for options, etc. But also means that we can
    // share the command line entry actions between IdeaVim implementations
    val editor = TextComponentEditorImpl(project, target).vim
    injector.optionGroup.initialiseLocalOptions(editor, target.editor!!.vim, LocalOptionInitialisationScenario.CMD_LINE)

    val offset = injector.searchHelper.findNextWord(editor, caret.dot, -1, bigWord = false, spaceWords = false)
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

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
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

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
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
