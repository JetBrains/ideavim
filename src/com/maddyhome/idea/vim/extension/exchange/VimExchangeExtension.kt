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

package com.maddyhome.idea.vim.extension.exchange

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.Mark
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimMark
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.getRegister
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.setOperatorFunction
import com.maddyhome.idea.vim.extension.VimExtensionFacade.setRegister
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.group.MarkGroup
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.StringHelper.stringToKeys
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.helper.moveToInlayAwareLogicalPosition
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.helper.subMode
import com.maddyhome.idea.vim.key.OperatorFunction
import org.jetbrains.annotations.NonNls

/**
 * This emulation misses:
 *  - `:ExchangeClear` command
 *  - `g:exchange_no_mappings` variable
 *  - `g:exchange_indent` variable (?)
 *  - Default mappings should not be applied if there is a mapping defined in `~/.ideavimrc`.
 *      This functionality requires rewriting of IdeaVim initialization, so that plugins would be
 *        loaded after `~/.ideavimrc` is executed (as vim works). But the `if no bindings` can be added even now.
 *        It just won't work if the binding is defined after `set exchange`.
 */

class VimExchangeExtension : VimExtension {

  override fun getName() = "exchange"

  override fun init() {
    putExtensionHandlerMapping(MappingMode.N, parseKeys(EXCHANGE_CMD), owner, ExchangeHandler(false), false)
    putExtensionHandlerMapping(MappingMode.X, parseKeys(EXCHANGE_CMD), owner, VExchangeHandler(), false)
    putExtensionHandlerMapping(MappingMode.N, parseKeys(EXCHANGE_CLEAR_CMD), owner, ExchangeClearHandler(), false)
    putExtensionHandlerMapping(MappingMode.N, parseKeys(EXCHANGE_LINE_CMD), owner, ExchangeHandler(true), false)

    putKeyMapping(MappingMode.N, parseKeys("cx"), owner, parseKeys(EXCHANGE_CMD), true)
    putKeyMapping(MappingMode.X, parseKeys("X"), owner, parseKeys(EXCHANGE_CMD), true)
    putKeyMapping(MappingMode.N, parseKeys("cxc"), owner, parseKeys(EXCHANGE_CLEAR_CMD), true)
    putKeyMapping(MappingMode.N, parseKeys("cxx"), owner, parseKeys(EXCHANGE_LINE_CMD), true)
  }

  companion object {
    @NonNls
    const val EXCHANGE_CMD = "<Plug>(Exchange)"
    @NonNls
    const val EXCHANGE_CLEAR_CMD = "<Plug>(ExchangeClear)"
    @NonNls
    const val EXCHANGE_LINE_CMD = "<Plug>(ExchangeLine)"

    val EXCHANGE_KEY = Key<Exchange>("exchange")

    class Exchange(val type: CommandState.SubMode, val start: Mark, val end: Mark, val text: String) {
      private var myHighlighter: RangeHighlighter? = null
      fun setHighlighter(highlighter: RangeHighlighter) {
        myHighlighter = highlighter
      }

      fun getHighlighter(): RangeHighlighter? = myHighlighter

    }

    fun clearExchange(editor: Editor) {
      editor.getUserData(EXCHANGE_KEY)?.getHighlighter()?.let {
        editor.markupModel.removeHighlighter(it)
      }
      editor.putUserData(EXCHANGE_KEY, null)
    }
  }

  private class ExchangeHandler(private val isLine: Boolean) : VimExtensionHandler {

    override fun isRepeatable() = true

    override fun execute(editor: Editor, context: DataContext) {
      setOperatorFunction(Operator(false))
      executeNormalWithoutMapping(parseKeys(if (isLine) "g@_" else "g@"), editor)
    }
  }

  private class ExchangeClearHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      clearExchange(editor)
    }
  }

  private class VExchangeHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      runWriteAction {
        val subMode = editor.subMode
        // Leave visual mode to create selection marks
        executeNormalWithoutMapping(parseKeys("<Esc>"), editor)
        Operator(true).apply(editor, context, SelectionType.fromSubMode(subMode))
      }
    }
  }

  private class Operator(private val isVisual: Boolean) : OperatorFunction {
    fun Editor.getMarkOffset(mark: Mark) = EditorHelper.getOffset(this, mark.logicalLine, mark.col)
    fun CommandState.SubMode.getString() = when (this) {
      CommandState.SubMode.VISUAL_CHARACTER -> "v"
      CommandState.SubMode.VISUAL_LINE -> "V"
      CommandState.SubMode.VISUAL_BLOCK -> "\\<C-V>"
      else -> error("Invalid SubMode: $this")
    }

    override fun apply(editor: Editor, context: DataContext, selectionType: SelectionType): Boolean {
      fun highlightExchange(ex: Exchange): RangeHighlighter {
        val attributes = editor.colorsScheme.getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES)
        val hlArea = when (ex.type) {
          CommandState.SubMode.VISUAL_LINE -> HighlighterTargetArea.LINES_IN_RANGE
          // TODO: handle other modes
          else -> HighlighterTargetArea.EXACT_RANGE
        }
        return editor.markupModel.addRangeHighlighter(
          editor.getMarkOffset(ex.start),
          (editor.getMarkOffset(ex.end) + 1).coerceAtMost(editor.fileSize),
          HighlighterLayer.SELECTION - 1,
          attributes,
          hlArea
        )
      }

      val currentExchange = getExchange(editor, isVisual, selectionType)
      val exchange1 = editor.getUserData(EXCHANGE_KEY)
      if (exchange1 == null) {
        val highlighter = highlightExchange(currentExchange)
        currentExchange.setHighlighter(highlighter)
        editor.putUserData(EXCHANGE_KEY, currentExchange)
        return true
      } else {
        val cmp = compareExchanges(exchange1, currentExchange)
        var reverse = false
        var expand = false
        val (ex1, ex2) = when (cmp) {
          ExchangeCompareResult.OVERLAP -> return false
          ExchangeCompareResult.OUTER -> {
            reverse = true
            expand = true
            Pair(currentExchange, exchange1)
          }
          ExchangeCompareResult.INNER -> {
            expand = true
            Pair(exchange1, currentExchange)
          }
          ExchangeCompareResult.GT -> {
            reverse = true
            Pair(currentExchange, exchange1)
          }
          ExchangeCompareResult.LT -> {
            Pair(exchange1, currentExchange)
          }
        }
        exchange(editor, ex1, ex2, reverse, expand)
        clearExchange(editor)
        return true
      }
    }

    private fun exchange(editor: Editor, ex1: Exchange, ex2: Exchange, reverse: Boolean, expand: Boolean) {
      fun pasteExchange(sourceExchange: Exchange, targetExchange: Exchange) {
        VimPlugin.getMark().setChangeMarks(editor, TextRange(editor.getMarkOffset(targetExchange.start), editor.getMarkOffset(targetExchange.end) + 1))
        // do this instead of direct text manipulation to set change marks
        setRegister('z', stringToKeys(sourceExchange.text), SelectionType.fromSubMode(sourceExchange.type))
        executeNormalWithoutMapping(stringToKeys("`[${targetExchange.type.getString()}`]\"zp"), editor)
      }

      fun fixCursor(ex1: Exchange, ex2: Exchange, reverse: Boolean) {
        val primaryCaret = editor.caretModel.primaryCaret
        if (reverse) {
          primaryCaret.moveToInlayAwareOffset(editor.getMarkOffset(ex1.start))
        } else {
          if (ex1.start.logicalLine == ex2.start.logicalLine) {
            val horizontalOffset = ex1.end.col - ex2.end.col
            primaryCaret.moveToInlayAwareLogicalPosition(LogicalPosition(ex1.start.logicalLine, ex1.start.col - horizontalOffset))
          } else if (ex1.end.logicalLine - ex1.start.logicalLine != ex2.end.logicalLine - ex2.start.logicalLine) {
            val verticalOffset = ex1.end.logicalLine - ex2.end.logicalLine
            primaryCaret.moveToInlayAwareLogicalPosition(LogicalPosition(ex1.start.logicalLine - verticalOffset, ex1.start.col))
          }
        }
      }

      val zRegText = getRegister('z')
      val unnRegText = getRegister('"')
      val startRegText = getRegister('*')
      val plusRegText = getRegister('+')
      runWriteAction {
        // TODO handle:
        // 	" Compare using =~ because "'==' != 0" returns 0
        //	let indent = s:get_setting('exchange_indent', 1) !~ 0 && a:x.type ==# 'V' && a:y.type ==# 'V'
        pasteExchange(ex1, ex2)
        if (!expand) {
          pasteExchange(ex2, ex1)
        }
        // TODO: handle: if ident
        if (!expand) {
          fixCursor(ex1, ex2, reverse)
        }
        setRegister('z', zRegText)
        setRegister('"', unnRegText)
        setRegister('*', startRegText)
        setRegister('+', plusRegText)
      }
    }

    private fun compareExchanges(x: Exchange, y: Exchange): ExchangeCompareResult {
      fun intersects(x: Exchange, y: Exchange) =
        x.end.logicalLine < y.start.logicalLine ||
          x.start.logicalLine > y.end.logicalLine ||
          x.end.col < y.start.col ||
          x.start.col > y.end.col

      fun comparePos(x: Mark, y: Mark): Int =
        if (x.logicalLine == y.logicalLine) {
          x.col - y.col
        } else {
          x.logicalLine - y.logicalLine
        }

      return if (x.type == CommandState.SubMode.VISUAL_BLOCK && y.type == CommandState.SubMode.VISUAL_BLOCK) {
        when {
          intersects(x, y) -> {
            ExchangeCompareResult.OVERLAP
          }
          x.start.col <= y.start.col -> {
            ExchangeCompareResult.LT
          }
          else -> {
            ExchangeCompareResult.GT
          }
        }
      } else if (comparePos(x.start, y.start) <= 0 && comparePos(x.end, y.end) >= 0) {
        ExchangeCompareResult.OUTER
      } else if (comparePos(y.start, x.start) <= 0 && comparePos(y.end, x.end) >= 0) {
        ExchangeCompareResult.INNER
      } else if (comparePos(x.start, y.end) <= 0 && comparePos(y.start, x.end) <= 0 ||
        comparePos(y.start, x.end) <= 0 && comparePos(x.start, y.end) <= 0
      ) {
        ExchangeCompareResult.OVERLAP
      } else {
        val cmp = comparePos(x.start, y.start)
        when {
          cmp == 0 -> ExchangeCompareResult.OVERLAP
          cmp < 0 -> ExchangeCompareResult.LT
          else -> ExchangeCompareResult.GT
        }
      }
    }

    enum class ExchangeCompareResult {
      OVERLAP,
      OUTER,
      INNER,
      LT,
      GT,
    }

    private fun getExchange(editor: Editor, isVisual: Boolean, selectionType: SelectionType): Exchange {
      fun getEndCol(selectionEnd: Mark, type: CommandState.SubMode): Int {
        return if (type == CommandState.SubMode.VISUAL_LINE) {
          EditorHelper.getLineLength(editor, selectionEnd.logicalLine)
        } else {
          selectionEnd.col
        }
      }

      // TODO: improve KeyStroke list to sting conversion
      fun getRegisterText(reg: Char): String = getRegister(reg)?.map { it.keyChar }?.joinToString("") ?: ""
      fun getMarks(isVisual: Boolean): Pair<Mark, Mark> {
        val (startMark, endMark) =
          if (isVisual) {
            Pair(MarkGroup.MARK_VISUAL_START, MarkGroup.MARK_VISUAL_END)
          } else {
            Pair(MarkGroup.MARK_CHANGE_START, MarkGroup.MARK_CHANGE_END)
          }
        val marks = VimPlugin.getMark()
        return Pair(marks.getMark(editor, startMark)!!, marks.getMark(editor, endMark)!!)
      }

      val unnRegText = getRegister('"')
      val starRegText = getRegister('*')
      val plusRegText = getRegister('+')

      var (selectionStart, selectionEnd) = getMarks(isVisual)
      if (isVisual) {
        executeNormalWithoutMapping(parseKeys("gvy"), editor)
        // TODO: handle
        //if &selection ==# 'exclusive' && start != end
        //			let end.column -= len(matchstr(@@, '\_.$'))
      } else {
        selectionEnd = selectionEnd.let {
          VimMark.create(
            it.key,
            it.logicalLine,
            getEndCol(it, selectionType.toSubMode()),
            it.filename,
            it.protocol
          )!!
        }
        when (selectionType) {
          SelectionType.LINE_WISE -> executeNormalWithoutMapping(stringToKeys("`[V`]y"), editor)
          SelectionType.BLOCK_WISE -> executeNormalWithoutMapping(stringToKeys("""`[<C-V>`]y"""), editor)
          SelectionType.CHARACTER_WISE -> executeNormalWithoutMapping(stringToKeys("`[v`]y"), editor)
        }
      }

      val text = getRegisterText('"')

      setRegister('"', unnRegText)
      setRegister('*', starRegText)
      setRegister('+', plusRegText)

      return Exchange(
        selectionType.toSubMode(),
        selectionStart,
        selectionEnd,
        text
      )
    }
  }
}
