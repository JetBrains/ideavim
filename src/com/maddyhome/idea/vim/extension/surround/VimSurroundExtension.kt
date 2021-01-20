/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim.extension.surround

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.getRegister
import com.maddyhome.idea.vim.extension.VimExtensionFacade.inputKeyStroke
import com.maddyhome.idea.vim.extension.VimExtensionFacade.inputString
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.setOperatorFunction
import com.maddyhome.idea.vim.extension.VimExtensionFacade.setRegister
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.option.ClipboardOptionsData.IdeaputDisabler
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Port of vim-surround.
 *
 * See https://github.com/tpope/vim-surround
 *
 * @author dhleong
 * @author vlan
 */
class VimSurroundExtension : VimExtension {

  override fun getName() = "surround"

  override fun init() {
    putExtensionHandlerMapping(MappingMode.N, StringHelper.parseKeys("<Plug>YSurround"), owner, YSurroundHandler(), false)
    putExtensionHandlerMapping(MappingMode.N, StringHelper.parseKeys("<Plug>CSurround"), owner, CSurroundHandler(), false)
    putExtensionHandlerMapping(MappingMode.N, StringHelper.parseKeys("<Plug>DSurround"), owner, DSurroundHandler(), false)
    putExtensionHandlerMapping(MappingMode.XO, StringHelper.parseKeys("<Plug>VSurround"), owner, VSurroundHandler(), false)

    putKeyMapping(MappingMode.N, StringHelper.parseKeys("ys"), owner, StringHelper.parseKeys("<Plug>YSurround"), true)
    putKeyMapping(MappingMode.N, StringHelper.parseKeys("cs"), owner, StringHelper.parseKeys("<Plug>CSurround"), true)
    putKeyMapping(MappingMode.N, StringHelper.parseKeys("ds"), owner, StringHelper.parseKeys("<Plug>DSurround"), true)
    putKeyMapping(MappingMode.XO, StringHelper.parseKeys("S"), owner, StringHelper.parseKeys("<Plug>VSurround"), true)
  }

  private class YSurroundHandler : VimExtensionHandler {
    override fun isRepeatable() = true

    override fun execute(editor: Editor, context: DataContext) {
      setOperatorFunction(Operator())
      executeNormalWithoutMapping(StringHelper.parseKeys("g@"), editor)
    }
  }

  private class VSurroundHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      val selectionStart = editor.caretModel.primaryCaret.selectionStart
      // NB: Operator ignores SelectionType anyway
      if (!Operator().apply(editor, context, SelectionType.CHARACTER_WISE)) {
        return
      }
      runWriteAction {
        // Leave visual mode
        executeNormalWithoutMapping(StringHelper.parseKeys("<Esc>"), editor)
        editor.caretModel.moveToOffset(selectionStart)
      }
    }
  }

  private class CSurroundHandler : VimExtensionHandler {
    override fun isRepeatable() = true

    override fun execute(editor: Editor, context: DataContext) {
      val charFrom = getChar(editor)
      if (charFrom.toInt() == 0) return

      val charTo = getChar(editor)
      if (charTo.toInt() == 0) return

      val newSurround = getOrInputPair(charTo, editor) ?: return
      runWriteAction { change(editor, charFrom, newSurround) }
    }

    companion object {
      fun change(editor: Editor, charFrom: Char, newSurround: Pair<String, String>?) {
        // We take over the " register, so preserve it
        val oldValue: List<KeyStroke>? = getRegister(REGISTER)
        // Extract the inner value
        perform("di" + pick(charFrom), editor)
        val innerValue: MutableList<KeyStroke> = getRegister(REGISTER)?.toMutableList() ?: mutableListOf()
        // Delete the surrounding
        perform("da" + pick(charFrom), editor)
        // Insert the surrounding characters and paste
        if (newSurround != null) {
          innerValue.addAll(0, StringHelper.parseKeys(escape(newSurround.first)))
          innerValue.addAll(StringHelper.parseKeys(escape(newSurround.second)))
        }
        pasteSurround(innerValue, editor)
        // Restore the old value
        setRegister(REGISTER, oldValue)
        // Jump back to start
        executeNormalWithoutMapping(StringHelper.parseKeys("`["), editor)
      }

      private fun escape(sequence: String): String = sequence.replace("<", "\\<")

      private fun perform(sequence: String, editor: Editor) {
        IdeaputDisabler().use { executeNormalWithoutMapping(StringHelper.parseKeys("\"" + REGISTER + sequence), editor) }
      }

      private fun pasteSurround(innerValue: List<KeyStroke?>, editor: Editor) { // This logic is direct from vim-surround
        val offset = editor.caretModel.offset
        val lineEndOffset = EditorHelper.getLineEndForOffset(editor, offset)
        val motionEndMark = VimPlugin.getMark().getMark(editor, ']')
        val motionEndOffset = if (motionEndMark != null) {
          EditorHelper.getOffset(editor, motionEndMark.logicalLine, motionEndMark.col)
        } else -1
        val pasteCommand = if (motionEndOffset == lineEndOffset && offset + 1 == lineEndOffset) "p" else "P"
        setRegister(REGISTER, innerValue)
        perform(pasteCommand, editor)
      }

      private fun pick(charFrom: Char) = when (charFrom) {
        'a' -> '>'
        'r' -> ']'
        else -> charFrom
      }
    }
  }

  private class DSurroundHandler : VimExtensionHandler {
    override fun isRepeatable() = true

    override fun execute(editor: Editor, context: DataContext) {
      // Deleting surround is just changing the surrounding to "nothing"
      val charFrom = getChar(editor)
      if (charFrom.toInt() == 0) return

      runWriteAction { CSurroundHandler.change(editor, charFrom, null) }
    }
  }

  private class Operator : OperatorFunction {
    override fun apply(editor: Editor, context: DataContext, selectionType: SelectionType): Boolean {
      val c = getChar(editor)
      if (c.toInt() == 0) return true

      val pair = getOrInputPair(c, editor) ?: return false
      // XXX: Will it work with line-wise or block-wise selections?
      val range = getSurroundRange(editor) ?: return false
      runWriteAction {
        val change = VimPlugin.getChange()
        val leftSurround = pair.first
        val primaryCaret = editor.caretModel.primaryCaret
        change.insertText(editor, primaryCaret, range.startOffset, leftSurround)
        change.insertText(editor, primaryCaret, range.endOffset + leftSurround.length, pair.second)
        // Jump back to start
        executeNormalWithoutMapping(StringHelper.parseKeys("`["), editor)
      }
      return true
    }

    private fun getSurroundRange(editor: Editor): TextRange? = when (editor.mode) {
      CommandState.Mode.COMMAND -> VimPlugin.getMark().getChangeMarks(editor)
      CommandState.Mode.VISUAL -> editor.caretModel.primaryCaret.run { TextRange(selectionStart, selectionEnd) }
      else -> null
    }
  }

  companion object {
    private const val REGISTER = '"'

    private val tagNameAndAttributesCapturePattern = "(\\w+)([^>]*)>".toPattern()

    private val SURROUND_PAIRS = mapOf(
      'b' to ("(" to ")"),
      '(' to ("( " to " )"),
      ')' to ("(" to ")"),
      'B' to ("{" to "}"),
      '{' to ("{ " to " }"),
      '}' to ("{" to "}"),
      'r' to ("[" to "]"),
      '[' to ("[ " to " ]"),
      ']' to ("[" to "]"),
      'a' to ("<" to ">"),
      '>' to ("<" to ">"),
      's' to (" " to "")
    )

    private fun getSurroundPair(c: Char): Pair<String, String>? = if (c in SURROUND_PAIRS) {
      SURROUND_PAIRS[c]
    } else if (!c.isLetter()) {
      val s = c.toString()
      s to s
    } else null

    private fun inputTagPair(editor: Editor): Pair<String, String>? {
      val tagInput = inputString(editor, "<", '>')
      val matcher = tagNameAndAttributesCapturePattern.matcher(tagInput)
      return if (matcher.find()) {
        val tagName = matcher.group(1)
        val tagAttributes = matcher.group(2)
        "<$tagName$tagAttributes>" to "</$tagName>"
      } else null
    }

    private fun inputFunctionName(
      editor: Editor,
      withInternalSpaces: Boolean
    ): Pair<String, String>? {
      val functionNameInput = inputString(editor, "function: ", null)
      if (functionNameInput.isEmpty()) return null
      return if (withInternalSpaces) "$functionNameInput( " to " )" else "$functionNameInput(" to ")"
    }

    private fun getOrInputPair(c: Char, editor: Editor): Pair<String, String>? = when (c) {
      '<', 't' -> inputTagPair(editor)
      'f' -> inputFunctionName(editor, false)
      'F' -> inputFunctionName(editor, true)
      else -> getSurroundPair(c)
    }

    private fun getChar(editor: Editor): Char {
      val key = inputKeyStroke(editor)
      val keyChar = key.keyChar
      return if (keyChar == KeyEvent.CHAR_UNDEFINED || keyChar.toInt() == KeyEvent.VK_ESCAPE) {
        0.toChar()
      } else keyChar
    }
  }
}
