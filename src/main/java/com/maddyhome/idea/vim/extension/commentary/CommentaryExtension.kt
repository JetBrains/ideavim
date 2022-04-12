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

import com.intellij.codeInsight.generation.CommentByBlockCommentHandler
import com.intellij.codeInsight.generation.CommentByLineCommentHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.CommandState.Companion.getInstance
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.common.MappingMode
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.addCommand
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.VimExtensionFacade.setOperatorFunction
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.PsiHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.IjVimEditor
import java.util.*

class CommentaryExtension : VimExtension {

  companion object {
    fun doCommentary(editor: Editor, range: TextRange, selectionType: SelectionType, resetCaret: Boolean): Boolean {
      val mode = getInstance(IjVimEditor(editor)).mode
      if (mode !== CommandState.Mode.VISUAL) {
        editor.selectionModel.setSelection(range.startOffset, range.endOffset)
      }

      val handler =
        if (selectionType === SelectionType.CHARACTER_WISE) CommentByBlockCommentHandler() else CommentByLineCommentHandler()

      return runWriteAction {
        try {
          val proj = editor.project ?: return@runWriteAction false
          val file = PsiDocumentManager.getInstance(proj).getPsiFile(editor.document) ?: return@runWriteAction false
          handler.invoke(editor.project!!, editor, editor.caretModel.currentCaret, file)
          handler.postInvoke()
          return@runWriteAction true
        } finally {
          // Remove the selection, if we added it
          if (mode !== CommandState.Mode.VISUAL) {
            editor.selectionModel.removeSelection()
          }

          // Put the caret back at the start of the range, as though it was moved by the operator's motion argument.
          // This is what Vim does. If IntelliJ is configured to add comments at the start of the line, this might put
          // the caret in the "wrong" place. E.g. gc_ should put the caret on the first non-whitespace character. This
          // is calculated by the motion, saved in the marks and then we insert the comment. If it's inserted at the
          // first non-whitespace character, then the caret is in the right place. If it's inserted at the first column,
          // then the caret is now in a bit of a weird place. We can't detect this scenario, so we just have to accept
          // the difference
          if (resetCaret) {
            editor.caretModel.primaryCaret.moveToOffset(range.startOffset)
          }
        }
      }
    }
  }

  override fun getName() = "commentary"

  override fun init() {
    putExtensionHandlerMapping(MappingMode.NO, parseKeys("<Plug>Commentary"), owner, CommentMotionHandler(), false)
    putExtensionHandlerMapping(MappingMode.X, parseKeys("<Plug>Commentary"), owner, CommentMotionVHandler(), false)
    putKeyMappingIfMissing(MappingMode.N, parseKeys("<Plug>CommentaryLine"), owner, parseKeys("gc_"), true)

    putKeyMappingIfMissing(MappingMode.NXO, parseKeys("gc"), owner, parseKeys("<Plug>Commentary"), true)
    putKeyMappingIfMissing(MappingMode.N, parseKeys("gcc"), owner, parseKeys("<Plug>CommentaryLine"), true)
    putKeyMappingIfMissing(MappingMode.N, parseKeys("gcu"), owner, parseKeys("<Plug>Commentary<Plug>Commentary"), true)

    addCommand("Commentary", CommentaryCommandAliasHandler())
  }

  private class CommentMotionHandler : VimExtensionHandler {
    override fun isRepeatable() = true

    override fun execute(editor: Editor, context: DataContext) {
      val commandState = getInstance(IjVimEditor(editor))
      val count = maxOf(1, commandState.commandBuilder.count)

      if (commandState.isOperatorPending) {
        val textObjectHandler = CommentTextObjectHandler()
        commandState.commandBuilder.completeCommandPart(Argument(Command(count, textObjectHandler, Command.Type.MOTION,
          EnumSet.noneOf(CommandFlags::class.java))))
      }
      else {
        setOperatorFunction(Operator())
        executeNormalWithoutMapping(parseKeys("g@"), editor)
      }
    }

    private class CommentTextObjectHandler : TextObjectActionHandler() {
      override val visualType: TextObjectVisualType = TextObjectVisualType.LINE_WISE

      override fun getRange(
        editor: VimEditor,
        caret: VimCaret,
        context: ExecutionContext,
        count: Int,
        rawCount: Int,
        argument: Argument?
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
  }

  private class CommentMotionVHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      if (editor.caretModel.primaryCaret.hasSelection()) {
        // always use line-wise comments
        Operator().apply(editor, context, SelectionType.LINE_WISE)
        runWriteAction {
          // Leave visual mode. The operator will only remove selection if it adds it. We could let the operator remove
          // the selection always, which would asynchronously exit visual mode, but this makes it synchronous
          executeNormalWithoutMapping(parseKeys("<Esc>"), editor)
        }
      }
    }
  }

  private class Operator : OperatorFunction {
    override fun apply(editor: Editor, context: DataContext, selectionType: SelectionType): Boolean {
      val range = getCommentRange(editor) ?: return false
      return doCommentary(editor, range, selectionType, true)
    }

    private fun getCommentRange(editor: Editor): TextRange? {
      val mode = getInstance(IjVimEditor(editor)).mode
      return when (mode) {
        CommandState.Mode.COMMAND -> VimPlugin.getMark().getChangeMarks(IjVimEditor(editor))
        CommandState.Mode.VISUAL -> editor.caretModel.primaryCaret.let { TextRange(it.selectionStart, it.selectionEnd) }
        else -> null
      }
    }
  }

  private class CommentaryCommandAliasHandler: CommandAliasHandler {
    override fun execute(command:String, ranges: Ranges, editor: Editor, context: DataContext) {
      doCommentary(editor, ranges.getTextRange(editor, -1), SelectionType.LINE_WISE, false)
    }
  }
}