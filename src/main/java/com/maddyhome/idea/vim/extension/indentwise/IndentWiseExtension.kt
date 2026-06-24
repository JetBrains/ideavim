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

  override fun init() {
    putExtensionHandlerMapping(
      MappingMode.NXO,
      injector.parser.parseKeys("<Plug>IndentWisePreviousLesserIndent"),
      owner,
      IndentWiseLessIndentHandler(),
      false
    )
    putKeyMappingIfMissing(
      MappingMode.NXO,
      injector.parser.parseKeys("[-"),
      owner,
      injector.parser.parseKeys("<Plug>IndentWisePreviousLesserIndent"),
      true,
    )
  }

  class IndentWiseLessIndentHandler : ExtensionHandler {
    override fun execute(
      editor: VimEditor,
      context: ExecutionContext,
      operatorArguments: OperatorArguments,
    ) {
      if (editor.mode is Mode.OP_PENDING) {
        val commandBuilder = KeyHandler.getInstance().keyHandlerState.commandBuilder
        commandBuilder.addAction(IndentWiseMotionAction())
      } else {
        editor.sortedCarets().forEach { caret ->
          repeat(operatorArguments.count1) {
            moveToPreviousIndent(editor)
          }
        }
      }
    }

    private fun moveToPreviousIndent(editor: VimEditor) {
      val line = IndentWiseMotionAction.prevIndent(editor) ?: return
      editor.currentCaret().moveToOffset(editor.getLeadingCharacterOffset(line))
    }

  }

  private class IndentWiseMotionAction() : MotionActionHandler.ForEachCaret() {
    override val motionType: MotionType = MotionType.LINE_WISE

    override fun getOffset(
      editor: VimEditor,
      caret: ImmutableVimCaret,
      context: ExecutionContext,
      argument: Argument?,
      operatorArguments: OperatorArguments,
    ): Motion {
      val line = prevIndent(editor) ?: return 0.toMotionOrError()
      val target = editor.getLeadingCharacterOffset(line + 1)
      return target.toMotionOrError()
    }

    companion object {

      fun prevIndent(editor: VimEditor): Int? {
        var line = editor.currentCaret().getLine()
        val beginningLine = line
        val indent = editor.getVisualIndent(line)
        do {
          line--
        } while (line > 0 && (editor.getVisualIndent(line) >= indent || editor.getLineText(line).trim().isEmpty()))
        if (line < 0 || line == beginningLine) return null
        if (editor.getVisualIndent(line) >= indent) return null
        return line
      }

      fun VimEditor.getVisualIndent(line: Int): Int {
        val leadingOffset = this.getLeadingCharacterOffset(line)
        return this.offsetToVisualPosition(leadingOffset).column
      }
    }
  }
}
