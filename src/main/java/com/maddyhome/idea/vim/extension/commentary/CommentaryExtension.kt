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
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.CommandState.Companion.getInstance
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.MappingMode
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.VimExtensionFacade.setOperatorFunction
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.IjVimEditor

/**
 * @author dhleong
 */
class CommentaryExtension : VimExtension {
  override fun getName(): String {
    return "commentary"
  }

  override fun init() {
    putExtensionHandlerMapping(
      MappingMode.N, parseKeys("<Plug>(CommentMotion)"), owner,
      CommentMotionHandler(), false
    )
    putExtensionHandlerMapping(
      MappingMode.N, parseKeys("<Plug>(CommentLine)"), owner, CommentLineHandler(),
      false
    )
    putExtensionHandlerMapping(
      MappingMode.XO, parseKeys("<Plug>(CommentMotionV)"), owner,
      CommentMotionVHandler(), false
    )
    putKeyMappingIfMissing(MappingMode.N, parseKeys("gc"), owner, parseKeys("<Plug>(CommentMotion)"), true)
    putKeyMappingIfMissing(MappingMode.N, parseKeys("gcc"), owner, parseKeys("<Plug>(CommentLine)"), true)
    putKeyMappingIfMissing(MappingMode.XO, parseKeys("gc"), owner, parseKeys("<Plug>(CommentMotionV)"), true)
  }

  private class CommentMotionHandler : VimExtensionHandler {
    override fun isRepeatable(): Boolean {
      return true
    }

    override fun execute(editor: Editor, context: DataContext) {
      setOperatorFunction(Operator())
      executeNormalWithoutMapping(parseKeys("g@"), editor)
    }
  }

  private class CommentMotionVHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      if (!editor.caretModel.primaryCaret.hasSelection()) {
        return
      }

      // always use line-wise comments
      if (!Operator().apply(editor, context, SelectionType.LINE_WISE)) {
        return
      }
      WriteAction.run<RuntimeException> {

        // Leave visual mode
        executeNormalWithoutMapping(parseKeys("<Esc>"), editor)
        editor.caretModel.moveToOffset(editor.caretModel.primaryCaret.selectionStart)
      }
    }
  }

  private class Operator : OperatorFunction {
    override fun apply(editor: Editor, context: DataContext, selectionType: SelectionType): Boolean {
      val range = getCommentRange(editor) ?: return false
      if (getInstance(IjVimEditor(editor)).mode !== CommandState.Mode.VISUAL) {
        editor.selectionModel.setSelection(range.startOffset, range.endOffset)
      }
      val handler =
        if (selectionType === SelectionType.CHARACTER_WISE) CommentByBlockCommentHandler() else CommentByLineCommentHandler()
      return WriteAction.compute<Boolean, RuntimeException> {
        try {
          val proj = editor.project ?: return@compute false
          val file = PsiDocumentManager.getInstance(proj).getPsiFile(editor.document) ?: return@compute false
          handler.invoke(editor.project!!, editor, editor.caretModel.currentCaret, file)
          handler.postInvoke()

          // Jump back to start if in block mode
          if (selectionType === SelectionType.CHARACTER_WISE) {
            executeNormalWithoutMapping(parseKeys("`["), editor)
          }
          return@compute true
        } finally {
          // remove the selection
          editor.selectionModel.removeSelection()
        }
      }
    }

    private fun getCommentRange(editor: Editor): TextRange? {
      val mode = getInstance(IjVimEditor(editor)).mode
      return when (mode) {
        CommandState.Mode.COMMAND -> VimPlugin.getMark()
          .getChangeMarks(IjVimEditor(editor))

        CommandState.Mode.VISUAL -> {
          val primaryCaret = editor.caretModel.primaryCaret
          TextRange(primaryCaret.selectionStart, primaryCaret.selectionEnd)
        }

        else -> null
      }
    }
  }

  private class CommentLineHandler : VimExtensionHandler {
    override fun isRepeatable(): Boolean {
      return true
    }

    override fun execute(editor: Editor, context: DataContext) {
      val offset = editor.caretModel.offset
      val line = editor.document.getLineNumber(offset)
      val lineStart = editor.document.getLineStartOffset(line)
      val lineEnd = editor.document.getLineEndOffset(line)
      VimPlugin.getMark().setChangeMarks(IjVimEditor(editor), TextRange(lineStart, lineEnd))
      Operator().apply(editor, context, SelectionType.LINE_WISE)
    }
  }
}