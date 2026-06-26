/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.indentwise

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLeadingCharacterOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.state.mode.Mode

/**
 * Port of vim-indentwise (https://github.com/jeetsukumaran/vim-indentwise).
 *
 * Provides motions that navigate the buffer based on the indentation level of lines.
 *
 */
internal class IndentWiseExtension : VimExtension {
  override fun getName(): String = "vim-indentwise"

  data class IndentAction(
    val indentLevel: IndentLevel,
    val plugName: String,
    val keys: String,
    val direction: IndentActionDirection,
  )

  private val indentActions = listOf(
    IndentAction(IndentLevel.LESSER, "<Plug>IndentWisePreviousLesserIndent", "[-", IndentActionDirection.PREVIOUS),
    IndentAction(IndentLevel.GREATER, "<Plug>IndentWisePreviousGreaterIndent", "[+", IndentActionDirection.PREVIOUS),
    IndentAction(IndentLevel.EQUAL, "<Plug>IndentWisePreviousEqualIndent", "[=", IndentActionDirection.PREVIOUS),
    IndentAction(IndentLevel.LESSER, "<Plug>IndentWiseNextLesserIndent", "]-", IndentActionDirection.NEXT),
    IndentAction(IndentLevel.GREATER, "<Plug>IndentWiseNextGreaterIndent", "]+", IndentActionDirection.NEXT),
    IndentAction(IndentLevel.EQUAL, "<Plug>IndentWiseNextEqualIndent", "]=", IndentActionDirection.NEXT),
    IndentAction(IndentLevel.BLOCK, "<Plug>IndentWiseBlockScopeBoundaryBegin", "[%", IndentActionDirection.PREVIOUS),
    IndentAction(IndentLevel.BLOCK, "<Plug>IndentWiseBlockScopeBoundaryEnd", "]%", IndentActionDirection.NEXT),
  )

  override fun init() {
    indentActions.forEach { (indentLevel, plugName, keys, direction) ->
      putExtensionHandlerMapping(
        MappingMode.NXO,
        injector.parser.parseKeys(plugName),
        owner,
        IndentWiseLessIndentHandler(indentLevel, direction),
        false
      )
      putKeyMappingIfMissing(
        MappingMode.NXO,
        injector.parser.parseKeys(keys),
        owner,
        injector.parser.parseKeys(plugName),
        true,
      )
    }
  }

  enum class IndentLevel {
    LESSER,
    EQUAL,
    GREATER,
    BLOCK
  }

  enum class IndentActionDirection {
    PREVIOUS,
    NEXT,
  }

  class IndentWiseLessIndentHandler(val indentLevel: IndentLevel, val direction: IndentActionDirection) :
    ExtensionHandler {
    override fun execute(
      editor: VimEditor,
      context: ExecutionContext,
      operatorArguments: OperatorArguments,
    ) {
      if (editor.mode is Mode.OP_PENDING) {
        val commandBuilder = KeyHandler.getInstance().keyHandlerState.commandBuilder
        commandBuilder.addAction(IndentWiseMotionAction(indentLevel, direction))
      } else {
        editor.sortedCarets().forEach { _ ->
          if (indentLevel == IndentLevel.BLOCK) {
            val line = IndentWiseMotionAction.blockBoundary(
              editor,
              editor.currentCaret().getLine(),
              direction,
              operatorArguments.count1,
            )
            editor.currentCaret().moveToOffset(editor.getLeadingCharacterOffset(line))
          } else {
            repeat(operatorArguments.count1) {
              moveToPreviousIndent(editor)
            }
          }
        }
      }
    }

    private fun moveToPreviousIndent(editor: VimEditor) {
      val line = IndentWiseMotionAction.findIndentTarget(editor, indentLevel, direction) ?: return
      editor.currentCaret().moveToOffset(editor.getLeadingCharacterOffset(line))
    }

  }

  private class IndentWiseMotionAction(val indentLevel: IndentLevel, val direction: IndentActionDirection) :
    MotionActionHandler.ForEachCaret() {
    override val motionType: MotionType = MotionType.LINE_WISE

    override fun getOffset(
      editor: VimEditor,
      caret: ImmutableVimCaret,
      context: ExecutionContext,
      argument: Argument?,
      operatorArguments: OperatorArguments,
    ): Motion {
      if (indentLevel == IndentLevel.BLOCK) {
        val blockLine = blockBoundary(editor, editor.currentCaret().getLine(), direction, operatorArguments.count1)
        return editor.getLeadingCharacterOffset(blockLine).toMotionOrError()
      }
      val line = findIndentTarget(editor, indentLevel, direction) ?: return 0.toMotionOrError()
      val target = editor.getLeadingCharacterOffset(line + (if (direction == IndentActionDirection.PREVIOUS) 1 else -1))
      return target.toMotionOrError()
    }

    companion object {

      fun findIndentTarget(editor: VimEditor, indentLevel: IndentLevel, direction: IndentActionDirection): Int? {
        var line = editor.currentCaret().getLine()
        val beginningLine = line
        val indent = editor.getVisualIndent(line)
        do {
          line += if (direction == IndentActionDirection.PREVIOUS) -1 else 1
        } while (validLine(editor, line, direction) && (invalidIndent(
            indentLevel,
            line,
            indent,
            editor
          ) || editor.getLineText(line).trim()
            .isEmpty())
        )
        val invalidLine = invalidLine(editor, line, direction)
        if (invalidLine || line == beginningLine) return null
        if (invalidIndent(indentLevel, line, indent, editor)) return null

        return line
      }

      fun blockBoundary(
        editor: VimEditor,
        startLine: Int,
        direction: IndentActionDirection,
        count: Int,
      ): Int {
        val step = if (direction == IndentActionDirection.PREVIOUS) -1 else 1
        var referenceLine = startLine
        var target = startLine
        repeat(count) {
          val referenceIndent = editor.getVisualIndent(referenceLine)
          var line = referenceLine + step
          var boundary = -1
          while (line >= 0 && line < editor.lineCount()) {
            if (editor.getLineText(line).trim().isEmpty()) {
              line += step
              continue
            }
            if (editor.getVisualIndent(line) < referenceIndent) {
              boundary = line
              break
            }
            line += step
          }
          if (boundary < 0) {
            return fallbackLine(editor, direction)
          }
          target = boundary - step
          referenceLine = boundary
        }
        return target
      }

      private fun fallbackLine(editor: VimEditor, direction: IndentActionDirection): Int {
        return if (direction == IndentActionDirection.PREVIOUS) {
          var line = 0
          while (line < editor.lineCount() && editor.getLineText(line).trim().isEmpty()) line++
          if (line < editor.lineCount()) line else 0
        } else {
          var line = editor.lineCount() - 1
          while (line >= 0 && editor.getLineText(line).trim().isEmpty()) line--
          if (line >= 0) line else editor.lineCount() - 1
        }
      }

      private fun validLine(
        editor: VimEditor,
        line: Int,
        direction: IndentActionDirection,
      ): Boolean {
        return when (direction) {
          IndentActionDirection.PREVIOUS -> line > 0
          IndentActionDirection.NEXT -> line < editor.lineCount()
        }
      }

      private fun invalidLine(
        editor: VimEditor,
        line: Int,
        direction: IndentActionDirection,
      ): Boolean {
        return when (direction) {
          IndentActionDirection.PREVIOUS -> line < 0
          IndentActionDirection.NEXT -> line >= editor.lineCount()
        }
      }

      fun invalidIndent(indentLevel: IndentLevel, line: Int, indent: Int, editor: VimEditor): Boolean {
        return when (indentLevel) {
          IndentLevel.LESSER -> editor.getVisualIndent(line) >= indent
          IndentLevel.EQUAL -> editor.getVisualIndent(line) != indent
          IndentLevel.GREATER -> editor.getVisualIndent(line) <= indent
          IndentLevel.BLOCK -> editor.getVisualIndent(line) == indent
        }
      }

      fun VimEditor.getVisualIndent(line: Int): Int {
        val leadingOffset = this.getLeadingCharacterOffset(line)
        return this.offsetToVisualPosition(leadingOffset).column
      }
    }
  }
}
