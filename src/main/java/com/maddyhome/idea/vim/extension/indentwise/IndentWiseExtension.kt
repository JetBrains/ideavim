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
 */
internal class IndentWiseExtension : VimExtension {
  override fun getName(): String = "indentwise"

  data class IndentAction(
    val indentLevel: IndentLevel,
    val plugName: String,
    val keys: String,
    val direction: Direction,
  )

  private val indentActions = listOf(
    IndentAction(IndentLevel.LESSER, "<Plug>(IndentWisePreviousLesserIndent)", "[-", Direction.PREVIOUS),
    IndentAction(IndentLevel.GREATER, "<Plug>(IndentWisePreviousGreaterIndent)", "[+", Direction.PREVIOUS),
    IndentAction(IndentLevel.EQUAL, "<Plug>(IndentWisePreviousEqualIndent)", "[=", Direction.PREVIOUS),
    IndentAction(IndentLevel.LESSER, "<Plug>(IndentWiseNextLesserIndent)", "]-", Direction.NEXT),
    IndentAction(IndentLevel.GREATER, "<Plug>(IndentWiseNextGreaterIndent)", "]+", Direction.NEXT),
    IndentAction(IndentLevel.EQUAL, "<Plug>(IndentWiseNextEqualIndent)", "]=", Direction.NEXT),
    IndentAction(IndentLevel.BLOCK, "<Plug>(IndentWiseBlockScopeBoundaryBegin)", "[%", Direction.PREVIOUS),
    IndentAction(IndentLevel.BLOCK, "<Plug>(IndentWiseBlockScopeBoundaryEnd)", "]%", Direction.NEXT),
  )

  override fun init() {
    indentActions.forEach { (indentLevel, plugName, keys, direction) ->
      putExtensionHandlerMapping(
        MappingMode.NXO,
        injector.parser.parseKeys(plugName),
        owner,
        IndentWiseMotionHandler(indentLevel, direction),
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
    BLOCK,
  }

  enum class Direction(val step: Int) {
    PREVIOUS(-1),
    NEXT(1),
  }

  /**
   * Handles the motion when invoked directly (normal/visual mode). In operator-pending mode the work is delegated to
   * [IndentWiseMotionAction] so the operator can treat the move as a proper line-wise motion.
   */
  class IndentWiseMotionHandler(
    private val indentLevel: IndentLevel,
    private val direction: Direction,
  ) : ExtensionHandler {
    override fun execute(
      editor: VimEditor,
      context: ExecutionContext,
      operatorArguments: OperatorArguments,
    ) {
      if (editor.mode is Mode.OP_PENDING) {
        KeyHandler.getInstance().keyHandlerState.commandBuilder
          .addAction(IndentWiseMotionAction(indentLevel, direction))
        return
      }
      editor.sortedCarets().forEach { _ -> moveCaretToTarget(editor, operatorArguments.count1) }
    }

    private fun moveCaretToTarget(editor: VimEditor, count: Int) {
      val caret = editor.currentCaret()
      val targetLine = if (indentLevel == IndentLevel.BLOCK) {
        IndentNavigator.blockScopeBoundary(editor, caret.getLine(), direction, count)
      } else {
        repeatedIndentTarget(editor, caret.getLine(), count) ?: return
      }
      caret.moveToOffset(editor.getLeadingCharacterOffset(targetLine))
    }

    private fun repeatedIndentTarget(editor: VimEditor, startLine: Int, count: Int): Int? {
      var line = startLine
      repeat(count) {
        line = IndentNavigator.findIndentTarget(editor, line, indentLevel, direction) ?: return null
      }
      return line
    }
  }

  private class IndentWiseMotionAction(
    private val indentLevel: IndentLevel,
    private val direction: Direction,
  ) : MotionActionHandler.ForEachCaret() {
    override val motionType: MotionType = MotionType.LINE_WISE

    override fun getOffset(
      editor: VimEditor,
      caret: ImmutableVimCaret,
      context: ExecutionContext,
      argument: Argument?,
      operatorArguments: OperatorArguments,
    ): Motion {
      val currentLine = editor.currentCaret().getLine()
      if (indentLevel == IndentLevel.BLOCK) {
        val blockLine = IndentNavigator.blockScopeBoundary(editor, currentLine, direction, operatorArguments.count1)
        return editor.getLeadingCharacterOffset(blockLine).toMotionOrError()
      }
      val targetLine = IndentNavigator.findIndentTarget(editor, currentLine, indentLevel, direction)
        ?: return 0.toMotionOrError()
      return editor.getLeadingCharacterOffset(exclusiveLine(targetLine)).toMotionOrError()
    }

    /**
     * The indent motions are exclusive: the target indent line itself is not part of the operated/selected range, so
     * the motion stops on the line just inside it (one step back towards the caret's origin).
     */
    private fun exclusiveLine(targetLine: Int): Int = targetLine - direction.step
  }

  private object IndentNavigator {
    /**
     * Finds the nearest line, scanning in [direction] from [startLine], whose indentation satisfies [indentLevel]
     * relative to the start line. Blank lines are skipped. Returns null if no such line exists.
     */
    fun findIndentTarget(editor: VimEditor, startLine: Int, indentLevel: IndentLevel, direction: Direction): Int? {
      val referenceIndent = editor.visualIndent(startLine)
      var line = startLine + direction.step
      while (editor.containsLine(line) && !matchesIndent(editor, line, indentLevel, referenceIndent)) {
        line += direction.step
      }
      if (!editor.containsLine(line)) return null
      return line
    }

    /**
     * Finds the boundary of the current indentation block, scanning in [direction]. Each [count] step moves to the
     * boundary of the enclosing block. If no enclosing block is found, falls back to the first/last non-blank line.
     */
    fun blockScopeBoundary(editor: VimEditor, startLine: Int, direction: Direction, count: Int): Int {
      var referenceLine = startLine
      var target = startLine
      repeat(count) {
        val boundary = firstLineWithLesserIndent(editor, referenceLine, direction)
          ?: return outermostNonBlankLine(editor, direction)
        target = boundary - direction.step
        referenceLine = boundary
      }
      return target
    }

    private fun firstLineWithLesserIndent(editor: VimEditor, fromLine: Int, direction: Direction): Int? {
      val referenceIndent = editor.visualIndent(fromLine)
      var line = fromLine + direction.step
      while (editor.containsLine(line)) {
        if (!editor.isBlank(line) && editor.visualIndent(line) < referenceIndent) return line
        line += direction.step
      }
      return null
    }

    private fun outermostNonBlankLine(editor: VimEditor, direction: Direction): Int = when (direction) {
      Direction.PREVIOUS -> (0 until editor.lineCount()).firstOrNull { !editor.isBlank(it) } ?: 0
      Direction.NEXT -> (editor.lineCount() - 1 downTo 0).firstOrNull { !editor.isBlank(it) }
        ?: (editor.lineCount() - 1)
    }

    private fun matchesIndent(editor: VimEditor, line: Int, indentLevel: IndentLevel, referenceIndent: Int): Boolean {
      if (editor.isBlank(line)) return false
      val indent = editor.visualIndent(line)
      return when (indentLevel) {
        IndentLevel.LESSER -> indent < referenceIndent
        IndentLevel.EQUAL -> indent == referenceIndent
        IndentLevel.GREATER -> indent > referenceIndent
        IndentLevel.BLOCK -> error("BLOCK is handled by blockScopeBoundary, not findIndentTarget")
      }
    }
  }

  companion object {
    private fun VimEditor.containsLine(line: Int): Boolean = line in 0 until lineCount()

    private fun VimEditor.isBlank(line: Int): Boolean = getLineText(line).isBlank()

    private fun VimEditor.visualIndent(line: Int): Int =
      offsetToVisualPosition(getLeadingCharacterOffset(line)).column
  }
}
