/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.surround

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.endsWithNewLine
import com.maddyhome.idea.vim.api.getLeadingCharacterOffset
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.setChangeMarks
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.getRegisterForCaret
import com.maddyhome.idea.vim.extension.VimExtensionFacade.inputKeyStroke
import com.maddyhome.idea.vim.extension.VimExtensionFacade.inputString
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.VimExtensionFacade.setRegisterForCaret
import com.maddyhome.idea.vim.extension.exportOperatorFunction
import com.maddyhome.idea.vim.group.findBlockRange
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.helpers.ClipboardOptionHelper
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.selectionType
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
internal class VimSurroundExtension : VimExtension {

  override fun getName() = "surround"

  @NonNls
  private val NO_MAPPINGS = "surround_no_mappings"

  override fun init() {
    putExtensionHandlerMapping(MappingMode.N, injector.parser.parseKeys("<Plug>YSurround"), owner, YSurroundHandler(), false)
    putExtensionHandlerMapping(MappingMode.N, injector.parser.parseKeys("<Plug>Yssurround"), owner, YSSurroundHandler(), false)
    putExtensionHandlerMapping(MappingMode.N, injector.parser.parseKeys("<Plug>CSurround"), owner, CSurroundHandler(), false)
    putExtensionHandlerMapping(MappingMode.N, injector.parser.parseKeys("<Plug>DSurround"), owner, DSurroundHandler(), false)
    putExtensionHandlerMapping(MappingMode.XO, injector.parser.parseKeys("<Plug>VSurround"), owner, VSurroundHandler(), false)

    val noMappings = VimPlugin.getVariableService().getGlobalVariableValue(NO_MAPPINGS)?.asBoolean() ?: false
    if (!noMappings) {
      putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys("ys"), owner, injector.parser.parseKeys("<Plug>YSurround"), true)
      putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys("yss"), owner, injector.parser.parseKeys("<Plug>Yssurround"), true)
      putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys("cs"), owner, injector.parser.parseKeys("<Plug>CSurround"), true)
      putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys("ds"), owner, injector.parser.parseKeys("<Plug>DSurround"), true)
      putKeyMappingIfMissing(MappingMode.XO, injector.parser.parseKeys("S"), owner, injector.parser.parseKeys("<Plug>VSurround"), true)
    }

    VimExtensionFacade.exportOperatorFunction(OPERATOR_FUNC, Operator())
  }

  private class YSurroundHandler : ExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      injector.globalOptions().operatorfunc = OPERATOR_FUNC
      executeNormalWithoutMapping(injector.parser.parseKeys("g@"), editor.ij)
    }
  }

  private class YSSurroundHandler : ExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val ijEditor = editor.ij
      val c = getChar(ijEditor)
      if (c.code == 0) return
      val pair = getOrInputPair(c, ijEditor, context.ij) ?: return

      editor.forEachCaret {
        val line = it.getBufferPosition().line
        val lineStartOffset = editor.getLeadingCharacterOffset(line)
        val lineEndOffset = editor.getLineEndOffset(line)
        val lastNonWhiteSpaceOffset = getLastNonWhitespaceCharacterOffset(editor.text(), lineStartOffset, lineEndOffset)
        if (lastNonWhiteSpaceOffset != null) {
          val range = TextRange(lineStartOffset, lastNonWhiteSpaceOffset + 1)
          performSurround(pair, range, it)
        }
//        it.moveToOffset(lineStartOffset)
      }
      // Jump back to start
      executeNormalWithoutMapping(injector.parser.parseKeys("`["), ijEditor)
    }

    private fun getLastNonWhitespaceCharacterOffset(chars: CharSequence, startOffset: Int, endOffset: Int): Int? {
      var i = endOffset - 1
      while (i >= startOffset) {
        if (!chars[i].isWhitespace()) return i
        --i
      }
      return null
    }
  }

  private class VSurroundHandler : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val selectionStart = editor.ij.caretModel.primaryCaret.selectionStart
      // NB: Operator ignores SelectionType anyway
      if (!Operator().apply(editor, context, editor.mode.selectionType)) {
        return
      }
      runWriteAction {
        // Leave visual mode
        executeNormalWithoutMapping(injector.parser.parseKeys("<Esc>"), editor.ij)
        editor.ij.caretModel.moveToOffset(selectionStart)
      }
    }
  }

  private class CSurroundHandler : ExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val charFrom = getChar(editor.ij)
      if (charFrom.code == 0) return

      val charTo = getChar(editor.ij)
      if (charTo.code == 0) return

      val newSurround = getOrInputPair(charTo, editor.ij, context.ij) ?: return
      runWriteAction { change(editor, context, charFrom, newSurround) }
    }

    companion object {
      fun change(editor: VimEditor, context: ExecutionContext, charFrom: Char, newSurround: Pair<String, String>?) {
        // Save old register values for carets
        val surroundings = editor.sortedCarets()
          .map {
            val oldValue: List<KeyStroke>? = getRegisterForCaret(REGISTER, it)
            setRegisterForCaret(REGISTER, it, null)
            SurroundingInfo(it, null, oldValue)
          }

        // Delete surrounding's content
        perform("di" + pick(charFrom), editor.ij)

        // Add info about surrounding's inner text and location
        surroundings.forEach {
          // Delete surrounding chars if necessary
          val currentSurrounding = getCurrentSurrounding(it.caret, pick(charFrom))
          if (currentSurrounding != null) {
            it.caret.moveToOffset(currentSurrounding.startOffset)
            editor.deleteString(currentSurrounding)
          }

          val registerValue = getRegisterForCaret(REGISTER, it.caret)
          val innerValue = if (registerValue.isNullOrEmpty()) null else registerValue
          it.innerText = innerValue
        }

        surroundings.forEach {
          if (it.innerText == null && getRegisterForCaret(REGISTER, it.caret)?.isNotEmpty() == true) {
            it.innerText = emptyList()
          }
        }

        surroundings
          .filter { it.innerText != null } // we do nothing with carets that are not inside the surrounding
          .map { surrounding ->
            val innerValue = injector.parser.toPrintableString(surrounding.innerText!!)
            val text = newSurround?.let { it.first + innerValue + it.second } ?: innerValue
            val textData = PutData.TextData(text, SelectionType.CHARACTER_WISE, emptyList(), null)
            val putData = PutData(textData, null, 1, insertTextBeforeCaret = true, rawIndent = true, caretAfterInsertedText = false)

            surrounding.caret to putData
          }.forEach {
            injector.put.putTextForCaret(editor, it.first, context, it.second)
          }

        surroundings.forEach {
          it.restoreRegister()
        }

        executeNormalWithoutMapping(injector.parser.parseKeys("`["), editor.ij)
      }

      private fun perform(sequence: String, editor: Editor) {
        ClipboardOptionHelper.IdeaputDisabler()
          .use { executeNormalWithoutMapping(injector.parser.parseKeys("\"" + REGISTER + sequence), editor) }
      }

      private fun pick(charFrom: Char) = when (charFrom) {
        'a' -> '>'
        'r' -> ']'
        else -> charFrom
      }

      private fun getCurrentSurrounding(caret: VimCaret, char: Char): TextRange? {
        val editor = caret.editor
        val searchHelper = injector.searchHelper
        return when (char) {
          't' -> searchHelper.findBlockTagRange(editor, caret, 1, true)
          '(', ')', 'b' -> findBlockRange(editor, caret, '(', 1, true)
          '[', ']' -> findBlockRange(editor, caret, '[', 1, true)
          '{', '}', 'B' -> findBlockRange(editor, caret, '{', 1, true)
          '<', '>' -> findBlockRange(editor, caret, '<', 1, true)
          '`', '\'', '"' -> {
            val caretOffset = caret.offset
            val text = editor.text()
            if (text.getOrNull(caretOffset - 1) == char && text.getOrNull(caretOffset) == char) {
              TextRange(caretOffset - 1, caretOffset + 1)
            } else {
              searchHelper.findBlockQuoteInLineRange(editor, caret, char, true)
            }
          }
          'p' -> searchHelper.findParagraphRange(editor, caret, 1, true)
          's' -> searchHelper.findSentenceRange(editor, caret, 1, true)
          else -> null
        }
      }
    }
  }

  private data class SurroundingInfo(val caret: VimCaret, var innerText: List<KeyStroke>?, val oldRegisterContent: List<KeyStroke>?) {
    fun restoreRegister() {
      setRegisterForCaret(REGISTER, caret, oldRegisterContent)
    }
  }

  private class DSurroundHandler : ExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      // Deleting surround is just changing the surrounding to "nothing"
      val charFrom = getChar(editor.ij)
      LOG.debug("DSurroundHandler: charFrom = $charFrom")
      if (charFrom.code == 0) return

      runWriteAction { CSurroundHandler.change(editor, context, charFrom, null) }
    }
  }

  private class Operator : OperatorFunction {
    override fun apply(editor: VimEditor, context: ExecutionContext, selectionType: SelectionType?): Boolean {
      val ijEditor = editor.ij
      val c = getChar(ijEditor)
      if (c.code == 0) return true

      val pair = getOrInputPair(c, ijEditor, context.ij) ?: return false
      // XXX: Will it work with line-wise or block-wise selections?
      val range = getSurroundRange(editor.currentCaret()) ?: return false
      performSurround(pair, range, editor.currentCaret(), selectionType == SelectionType.LINE_WISE)
      // Jump back to start
      executeNormalWithoutMapping(injector.parser.parseKeys("`["), ijEditor)
      return true
    }

    private fun getSurroundRange(caret: VimCaret): TextRange? {
      val editor = caret.editor
      val ijEditor = editor.ij
      return when (ijEditor.vim.mode) {
        is Mode.NORMAL -> injector.markService.getChangeMarks(caret)
        is Mode.VISUAL -> caret.run { TextRange(selectionStart, selectionEnd) }
        else -> null
      }
    }
  }
}

private val LOG = logger<VimSurroundExtension>()

private const val REGISTER = '"'

private const val OPERATOR_FUNC = "SurroundOperatorFunc"

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
  's' to (" " to ""),
)

private fun getSurroundPair(c: Char): Pair<String, String>? = if (c in SURROUND_PAIRS) {
  SURROUND_PAIRS[c]
} else if (!c.isLetter()) {
  val s = c.toString()
  s to s
} else {
  null
}

private fun inputTagPair(editor: Editor, context: DataContext): Pair<String, String>? {
  val tagInput = inputString(editor, context, "<", '>')
  val matcher = tagNameAndAttributesCapturePattern.matcher(tagInput)
  return if (matcher.find()) {
    val tagName = matcher.group(1)
    val tagAttributes = matcher.group(2)
    "<$tagName$tagAttributes>" to "</$tagName>"
  } else {
    null
  }
}

private fun inputFunctionName(
  editor: Editor,
  context: DataContext,
  withInternalSpaces: Boolean,
): Pair<String, String>? {
  val functionNameInput = inputString(editor, context, "function: ", null)
  if (functionNameInput.isEmpty()) return null
  return if (withInternalSpaces) "$functionNameInput( " to " )" else "$functionNameInput(" to ")"
}

private fun getOrInputPair(c: Char, editor: Editor, context: DataContext): Pair<String, String>? = when (c) {
  '<', 't' -> inputTagPair(editor, context)
  'f' -> inputFunctionName(editor, context, false)
  'F' -> inputFunctionName(editor, context, true)
  else -> getSurroundPair(c)
}

private fun getChar(editor: Editor): Char {
  val key = inputKeyStroke(editor)
  val keyChar = key.keyChar
  val res = if (keyChar == KeyEvent.CHAR_UNDEFINED || keyChar.code == KeyEvent.VK_ESCAPE) {
    0.toChar()
  } else {
    keyChar
  }
  LOG.trace("getChar: $res")
  return res
}

private fun performSurround(pair: Pair<String, String>, range: TextRange, caret: VimCaret, tagsOnNewLines: Boolean = false) {
  runWriteAction {
    val editor = caret.editor
    val change = VimPlugin.getChange()
    val leftSurround = pair.first + if (tagsOnNewLines) "\n" else ""

    val isEOF = range.endOffset == editor.text().length
    val hasNewLine = editor.endsWithNewLine()
    val rightSurround = if (tagsOnNewLines) {
      if (isEOF && !hasNewLine) {
        "\n" + pair.second
      } else {
        pair.second + "\n"
      }
    } else {
      pair.second
    }

    change.insertText(editor, caret, range.startOffset, leftSurround)
    change.insertText(editor, caret, range.endOffset + leftSurround.length, rightSurround)
    injector.markService.setChangeMarks(
      caret,
      TextRange(range.startOffset, range.endOffset + leftSurround.length + rightSurround.length)
    )
  }
}
