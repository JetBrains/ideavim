/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.state.KeyHandlerState
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

open class InsertCommandLineTextActionBase(private val insertLiterally: Boolean) : CommandLineActionHandler() {

  override fun onStartWaitingForArgument(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState) {
    val cmdLine = injector.commandLine.getActiveCommandLine() ?: return
    cmdLine.setPromptCharacter('"')
  }

  override fun execute(
    commandLine: VimCommandLine,
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?
  ): Boolean {
    val text = getText(commandLine) ?: return false
    if (text.any { it.code < 32 }) {
      val keys = injector.parser.parseKeys(text)
      replayKeys(commandLine.editor, context, keys)
    }
    else {
      insertText(commandLine, commandLine.caret.offset, text)
    }
    return true
  }

  protected open fun getText(commandLine: VimCommandLine): String? = null

  protected fun replayKeys(editor: VimEditor, context: ExecutionContext, keys: List<KeyStroke>) {
    keys.forEach { key ->
      val keyHandler = KeyHandler.getInstance()
      if (shouldInsertLiterally(key)) {
        // Reuse existing mechanisms to insert a control character literally by passing <C-V> first
        injector.parser.parseKeys("<C-V>").forEach {
          keyHandler.handleKey(editor, it, context, keyHandler.keyHandlerState)
        }
      }
      keyHandler.handleKey(editor, key, context, allowKeyMappings = false, keyHandler.keyHandlerState)
    }
  }

  protected open fun insertText(commandLine: VimCommandLine, offset: Int, text: String) {
    commandLine.insertText(offset, text)
  }

  protected open fun shouldInsertLiterally(key: KeyStroke): Boolean {
    if (insertLiterally) {
      // Don't escape <C-V>
      if (key.keyCode == KeyEvent.VK_V && key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) {
        return false
      }

      // Insert Tab and any non-character keystroke literal characters
      if (key.keyCode == KeyEvent.VK_TAB || key.keyChar == KeyEvent.CHAR_UNDEFINED) {
        return true
      }
    }

    // <C-C>, <Esc> and <CR> are inserted literally. This includes their synonyms
    if (key.keyCode == KeyEvent.VK_ENTER || key.keyCode == KeyEvent.VK_ESCAPE) {
      return true
    }
    if (key.keyChar == KeyEvent.CHAR_UNDEFINED && key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) {
      return key.keyCode == KeyEvent.VK_C
        || key.keyCode == KeyEvent.VK_J
        || key.keyCode == KeyEvent.VK_M
        || key.keyCode == KeyEvent.VK_OPEN_BRACKET
    }
    return false
  }

  override fun execute(commandLine: VimCommandLine): Boolean {
    error("Should not be called. Use execute(commandLine, editor, context, argument: Argument?)")
  }
}

open class InsertRegisterActionBase(insertLiterally: Boolean) : InsertCommandLineTextActionBase(insertLiterally) {
  override val argumentType = Argument.Type.CHARACTER

  override fun execute(
    commandLine: VimCommandLine,
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?
  ): Boolean {
    val registerName = (argument as? Argument.Character)?.character ?: return false
    if (!(injector.registerGroup.isValid(registerName))) return false

    val register = injector.registerGroup.getRegister(editor, context, registerName) ?: return false

    // If we have any non-text characters, replay them through the key handler. If it's all just plain text, insert it
    // as text. Since we're not allowed to do mapping, replaying text keystrokes should be the same as inserting text.
    if (register.keys.any { it.keyChar == KeyEvent.CHAR_UNDEFINED }) {
      replayKeys(editor, context, register.keys)
    }
    else {
      insertText(commandLine, commandLine.caret.offset, register.text)
    }
    return true
  }
}

@CommandOrMotion(keys = ["<C-R>"], modes = [Mode.CMD_LINE])
class InsertRegisterAction : InsertRegisterActionBase(insertLiterally = false)

@CommandOrMotion(keys = ["<C-R><C-R>", "<C-R><C-O>"], modes = [Mode.CMD_LINE])
class InsertRegisterLiterallyAction : InsertRegisterActionBase(insertLiterally = true)

@CommandOrMotion(keys = ["<C-R><C-L>"], modes = [Mode.CMD_LINE])
class InsertCurrentLineAction : InsertCommandLineTextActionBase(insertLiterally = false) {
  override fun getText(commandLine: VimCommandLine) =
    commandLine.editor.getLineText(commandLine.editor.primaryCaret().getBufferPosition().line)
}

@CommandOrMotion(keys = ["<C-R><C-R><C-L>", "<C-R><C-O><C-L>"], modes = [Mode.CMD_LINE])
class InsertCurrentLineLiterallyAction : InsertCommandLineTextActionBase(insertLiterally = true) {
  override fun getText(commandLine: VimCommandLine) =
    commandLine.editor.getLineText(commandLine.editor.primaryCaret().getBufferPosition().line)
}

open class InsertWordUnderCaretActionBase(private val isBigWord: Boolean, insertLiterally: Boolean)
  : InsertCommandLineTextActionBase(insertLiterally) {
  override fun getText(commandLine: VimCommandLine): String? {
    val editor = commandLine.editor
    val wordRange = injector.searchHelper.findWordAtOrFollowingCursor(editor, editor.primaryCaret(), isBigWord)
    if (wordRange == null) {
      // E348: No string under cursor
      injector.messages.showStatusBarMessage(editor, injector.messages.message("E348"))
      return null
    }
    return editor.getText(wordRange)
  }
}

/**
 * Insert the word under or following the caret into the command line.
 *
 * A word cannot contain control characters, so it can only be inserted literally, as plain text. Any matching prefix in
 * the command line is maintained, and the remaining text is inserted after the prefix. The prefix must be a valid word.
 */
@CommandOrMotion(keys = ["<C-R><C-W>", "<C-R><C-R><C-W>", "<C-R><C-O><C-W>"], modes = [Mode.CMD_LINE])
class InsertWordUnderCaretAction : InsertWordUnderCaretActionBase(isBigWord = false, insertLiterally = true) {
  override fun insertText(commandLine: VimCommandLine, offset: Int, text: String) {
    val editor = commandLine.editor

    // Get the word under the caret on the command line, so we can avoid duplicating a prefix.
    // To match Vim behaviour, we get word, not WORD
    val offset = injector.searchHelper.findNextWord(commandLine.text, editor, commandLine.caret.offset, -1, bigWord = false)
    val prefix = commandLine.text.substring(offset, commandLine.caret.offset)
    if (text.startsWith(prefix, ignoreCase = true)) {
      commandLine.insertText(commandLine.caret.offset, text.substring(prefix.length))
    }
    else {
      commandLine.insertText(commandLine.caret.offset, text)
    }
  }
}

@CommandOrMotion(keys = ["<C-R><C-A>"], modes = [Mode.CMD_LINE])
class InsertBigWordUnderCaretAction : InsertWordUnderCaretActionBase(isBigWord = true, insertLiterally = false)
