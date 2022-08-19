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
package com.maddyhome.idea.vim.extension.commentary

import com.intellij.codeInsight.actions.AsyncActionExecutionService
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.addCommand
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.VimExtensionFacade.setOperatorFunction
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.PsiHelper
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import java.util.*

class CommentaryExtension : VimExtension {

  companion object {
    fun doCommentary(
      editor: VimEditor,
      context: ExecutionContext,
      range: TextRange,
      selectionType: SelectionType,
      resetCaret: Boolean,
    ): Boolean {
      val mode = editor.vimStateMachine.mode
      if (mode !== VimStateMachine.Mode.VISUAL) {
        editor.ij.selectionModel.setSelection(range.startOffset, range.endOffset)
      }

      return runWriteAction {
        // Treat block- and character-wise selections as block comments. Be ready to fall back to if the first action
        // isn't available
        val actions = if (selectionType === SelectionType.LINE_WISE) {
          listOf(IdeActions.ACTION_COMMENT_LINE, IdeActions.ACTION_COMMENT_BLOCK)
        } else {
          listOf(IdeActions.ACTION_COMMENT_BLOCK, IdeActions.ACTION_COMMENT_LINE)
        }

        val res = Ref.create<Boolean>(true)
        AsyncActionExecutionService.getInstance(editor.ij.project!!).withExecutionAfterAction(actions[0], {
          res.set(injector.actionExecutor.executeAction(actions[0], context))
        }, { afterCommenting(mode, editor, resetCaret, range) })
        if (!res.get()) {
          AsyncActionExecutionService.getInstance(editor.ij.project!!).withExecutionAfterAction(actions[1], {
            res.set(injector.actionExecutor.executeAction(actions[1], context))
          }, { afterCommenting(mode, editor, resetCaret, range) })
        }
        res.get()
      }
    }

    private fun afterCommenting(
      mode: VimStateMachine.Mode,
      editor: VimEditor,
      resetCaret: Boolean,
      range: TextRange,
    ) {
      // Remove the selection, if we added it
      if (mode !== VimStateMachine.Mode.VISUAL) {
        editor.removeSelection()
      }

      // Put the caret back at the start of the range, as though it was moved by the operator's motion argument.
      // This is what Vim does. If IntelliJ is configured to add comments at the start of the line, this might put
      // the caret in the "wrong" place. E.g. gc_ should put the caret on the first non-whitespace character. This
      // is calculated by the motion, saved in the marks, and then we insert the comment. If it's inserted at the
      // first non-whitespace character, then the caret is in the right place. If it's inserted at the first column,
      // then the caret is now in a bit of a weird place. We can't detect this scenario, so we just have to accept
      // the difference
      if (resetCaret) {
        editor.primaryCaret().moveToOffset(range.startOffset)
      }
    }
  }

  override fun getName() = "commentary"

  override fun init() {
    val plugCommentaryKeys = injector.parser.parseKeys("<Plug>Commentary")
    val plugCommentaryLineKeys = injector.parser.parseKeys("<Plug>CommentaryLine")
    putExtensionHandlerMapping(MappingMode.NX, plugCommentaryKeys, owner, CommentaryOperatorHandler(), false)
    putExtensionHandlerMapping(MappingMode.O, plugCommentaryKeys, owner, CommentaryTextObjectMotionHandler(), false)
    putKeyMappingIfMissing(MappingMode.N, plugCommentaryLineKeys, owner, injector.parser.parseKeys("gc_"), true)

    putKeyMappingIfMissing(MappingMode.NXO, injector.parser.parseKeys("gc"), owner, plugCommentaryKeys, true)
    putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys("gcc"), owner, plugCommentaryLineKeys, true)
    putKeyMappingIfMissing(
      MappingMode.N,
      injector.parser.parseKeys("gcu"),
      owner,
      injector.parser.parseKeys("<Plug>Commentary<Plug>Commentary"),
      true
    )

    // Previous versions of IdeaVim used different mappings to Vim's Commentary. Make sure everything works if someone
    // is still using the old mapping
    putKeyMapping(MappingMode.N, injector.parser.parseKeys("<Plug>(CommentMotion)"), owner, plugCommentaryKeys, true)
    putKeyMapping(MappingMode.XO, injector.parser.parseKeys("<Plug>(CommentMotionV)"), owner, plugCommentaryKeys, true)
    putKeyMapping(MappingMode.N, injector.parser.parseKeys("<Plug>(CommentLine)"), owner, plugCommentaryLineKeys, true)

    addCommand("Commentary", CommentaryCommandAliasHandler())
  }

  /**
   * Sets up the operator, pending a motion
   *
   * E.g. handles the `gc` in `gc_`, by setting the operator function, then invoking `g@` to receive the `_` motion to
   * invoke the operator. This object is both the mapping handler and the operator function.
   */
  private class CommentaryOperatorHandler : OperatorFunction, ExtensionHandler {
    override val isRepeatable = true

    // In this operator we process selection by ourselves. This is necessary for rider, VIM-1758
    override fun postProcessSelection(): Boolean {
      return false
    }

    override fun execute(editor: VimEditor, context: ExecutionContext) {
      setOperatorFunction(this)
      executeNormalWithoutMapping(injector.parser.parseKeys("g@"), editor.ij)
    }

    override fun apply(editor: VimEditor, context: ExecutionContext, selectionType: SelectionType): Boolean {
      val range = VimPlugin.getMark().getChangeMarks(editor) ?: return false
      return doCommentary(editor, context, range, selectionType, true)
    }
  }

  /**
   * The text object handler that provides the motion in e.g. `dgc`
   *
   * This object is both the `<Plug>Commentary` mapping handler and the text object handler
   */
  private class CommentaryTextObjectMotionHandler : TextObjectActionHandler(), ExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext) {
      val commandState = editor.vimStateMachine
      val count = maxOf(1, commandState.commandBuilder.count)

      val textObjectHandler = this
      commandState.commandBuilder.completeCommandPart(
        Argument(
          Command(
            count, textObjectHandler, Command.Type.MOTION,
            EnumSet.noneOf(CommandFlags::class.java)
          )
        )
      )
    }

    override val visualType: TextObjectVisualType = TextObjectVisualType.LINE_WISE

    override fun getRange(
      editor: VimEditor,
      caret: VimCaret,
      context: ExecutionContext,
      count: Int,
      rawCount: Int,
      argument: Argument?,
    ): TextRange? {

      val nativeEditor = (editor as IjVimEditor).editor
      val file = PsiHelper.getFile(nativeEditor) ?: return null
      val lastLine = editor.lineCount()

      var startLine = caret.getLogicalPosition().line
      while (startLine > 0 && isCommentLine(file, nativeEditor, startLine - 1)) startLine--
      var endLine = caret.getLogicalPosition().line - 1
      while (endLine < lastLine && isCommentLine(file, nativeEditor, endLine + 1)) endLine++

      if (startLine <= endLine) {
        val startOffset = EditorHelper.getLineStartOffset(nativeEditor, startLine)
        val endOffset = EditorHelper.getLineStartOffset(nativeEditor, endLine + 1)
        return TextRange(startOffset, endOffset)
      }

      return null
    }

    // Check all leaf nodes in the given line are whitespace, comments, or are owned by comments
    private fun isCommentLine(file: PsiFile, editor: Editor, logicalLine: Int): Boolean {
      val startOffset = EditorHelper.getLineStartOffset(editor, logicalLine)
      val endOffset = EditorHelper.getLineEndOffset(editor, logicalLine, true)
      val startElement = file.findElementAt(startOffset) ?: return false
      var next: PsiElement? = startElement
      while (next != null && next.textRange.startOffset <= endOffset) {
        if (next !is PsiWhiteSpace && !isComment(next))
          return false
        next = PsiTreeUtil.nextLeaf(next, true)
      }

      return true
    }

    private fun isComment(element: PsiElement) =
      PsiTreeUtil.getParentOfType(element, PsiComment::class.java, false) != null
  }

  /**
   * The handler for the `Commentary` user defined command
   *
   * Used like `:1,3Commentary` or `g/fun/Commentary`
   */
  private class CommentaryCommandAliasHandler : CommandAliasHandler {
    override fun execute(command: String, ranges: Ranges, editor: VimEditor, context: ExecutionContext) {
      doCommentary(editor, context, ranges.getTextRange(editor, -1), SelectionType.LINE_WISE, false)
    }
  }
}
