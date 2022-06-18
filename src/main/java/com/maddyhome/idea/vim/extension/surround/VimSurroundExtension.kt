/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.MappingMode
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.getRegister
import com.maddyhome.idea.vim.extension.VimExtensionFacade.inputKeyStroke
import com.maddyhome.idea.vim.extension.VimExtensionFacade.inputString
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.VimExtensionFacade.setOperatorFunction
import com.maddyhome.idea.vim.extension.VimExtensionFacade.setRegister
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.helpers.ClipboardOptionHelper
import org.jetbrains.annotations.NonNls
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

  @NonNls
  private val NO_MAPPINGS = "surround_no_mappings"

  override fun init() {
    putExtensionHandlerMapping(MappingMode.N, injector.parser.parseKeys("<Plug>YSurround"), owner, YSurroundHandler(), false)
    putExtensionHandlerMapping(MappingMode.N, injector.parser.parseKeys("<Plug>CSurround"), owner, CSurroundHandler(), false)
    putExtensionHandlerMapping(MappingMode.N, injector.parser.parseKeys("<Plug>DSurround"), owner, DSurroundHandler(), false)
    putExtensionHandlerMapping(MappingMode.XO, injector.parser.parseKeys("<Plug>VSurround"), owner, VSurroundHandler(), false)

    val noMappings = VimPlugin.getVariableService().getGlobalVariableValue(NO_MAPPINGS)?.asBoolean() ?: false
    if (!noMappings) {
      putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys("ys"), owner, injector.parser.parseKeys("<Plug>YSurround"), true)
      putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys("cs"), owner, injector.parser.parseKeys("<Plug>CSurround"), true)
      putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys("ds"), owner, injector.parser.parseKeys("<Plug>DSurround"), true)
      putKeyMappingIfMissing(MappingMode.XO, injector.parser.parseKeys("S"), owner, injector.parser.parseKeys("<Plug>VSurround"), true)
    }
  }

  private class YSurroundHandler : VimExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext) {
      setOperatorFunction(Operator())
      executeNormalWithoutMapping(injector.parser.parseKeys("g@"), editor.ij)
    }
  }

  private class VSurroundHandler : VimExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext) {
      val selectionStart = editor.ij.caretModel.primaryCaret.selectionStart
      // NB: Operator ignores SelectionType anyway
      if (!Operator().apply(editor.ij, context.ij, SelectionType.CHARACTER_WISE)) {
        return
      }
      runWriteAction {
        // Leave visual mode
        executeNormalWithoutMapping(injector.parser.parseKeys("<Esc>"), editor.ij)
        editor.ij.caretModel.moveToOffset(selectionStart)
      }
    }
  }

  private class CSurroundHandler : VimExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext) {
      val charFrom = getChar(editor.ij)
      if (charFrom.code == 0) return

      val charTo = getChar(editor.ij)
      if (charTo.code == 0) return

      val newSurround = getOrInputPair(charTo, editor.ij) ?: return
      runWriteAction { change(editor.ij, charFrom, newSurround) }
    }

    companion object {
      fun change(editor: Editor, charFrom: Char, newSurround: Pair<String, String>?) {
        // We take over the " register, so preserve it
        val oldValue: List<KeyStroke>? = getRegister(REGISTER)
        // Empty the " register
        setRegister(REGISTER, null)
        // Extract the inner value
        perform("di" + pick(charFrom), editor)
        val innerValue: MutableList<KeyStroke> = getRegister(REGISTER)?.toMutableList() ?: mutableListOf()
        // If the surrounding characters were not found, the register will be empty
        if (innerValue.isNotEmpty()) {
          // Delete the surrounding
          perform("da" + pick(charFrom), editor)
          // Insert the surrounding characters and paste
          if (newSurround != null) {
            innerValue.addAll(0, injector.parser.parseKeys(newSurround.first))
            innerValue.addAll(injector.parser.parseKeys(newSurround.second))
          }
          pasteSurround(innerValue, editor)
          // Jump back to start
          executeNormalWithoutMapping(injector.parser.parseKeys("`["), editor)
        }
        // Restore the old value
        setRegister(REGISTER, oldValue)
      }

      private fun perform(sequence: String, editor: Editor) {
        ClipboardOptionHelper.IdeaputDisabler()
          .use { executeNormalWithoutMapping(injector.parser.parseKeys("\"" + REGISTER + sequence), editor) }
      }

      private fun pasteSurround(
        innerValue: List<KeyStroke?>,
        editor: Editor,
      ) { // This logic is direct from vim-surround
        val offset = editor.caretModel.offset
        val lineEndOffset = EditorHelper.getLineEndForOffset(editor, offset)
        val motionEndMark = VimPlugin.getMark().getMark(editor.vim, ']')
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
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext) {
      // Deleting surround is just changing the surrounding to "nothing"
      val charFrom = getChar(editor.ij)
      if (charFrom.code == 0) return

      runWriteAction { CSurroundHandler.change(editor.ij, charFrom, null) }
    }
  }

  private class Operator : OperatorFunction {
    override fun apply(editor: Editor, context: DataContext, selectionType: SelectionType): Boolean {
      val c = getChar(editor)
      if (c.code == 0) return true

      val pair = getOrInputPair(c, editor) ?: return false
      // XXX: Will it work with line-wise or block-wise selections?
      val range = getSurroundRange(editor) ?: return false
      runWriteAction {
        val change = VimPlugin.getChange()
        val leftSurround = pair.first
        val primaryCaret = editor.caretModel.primaryCaret
        change.insertText(IjVimEditor(editor), IjVimCaret(primaryCaret), range.startOffset, leftSurround)
        change.insertText(IjVimEditor(editor), IjVimCaret(primaryCaret), range.endOffset + leftSurround.length, pair.second)
        // Jump back to start
        executeNormalWithoutMapping(injector.parser.parseKeys("`["), editor)
      }
      return true
    }

    private fun getSurroundRange(editor: Editor): TextRange? = when (editor.mode) {
      CommandState.Mode.COMMAND -> VimPlugin.getMark().getChangeMarks(editor.vim)
      CommandState.Mode.VISUAL -> editor.caretModel.primaryCaret.run { TextRange(selectionStart, selectionEnd) }
      else -> null
    }
  }

  companion object {
    private const val REGISTER = '"'

    private val tagNameAndAttributesCapturePattern = "(\\S+)([^>]*)>".toPattern()

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
      withInternalSpaces: Boolean,
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
      return if (keyChar == KeyEvent.CHAR_UNDEFINED || keyChar.code == KeyEvent.VK_ESCAPE) {
        0.toChar()
      } else keyChar
    }
  }
}
