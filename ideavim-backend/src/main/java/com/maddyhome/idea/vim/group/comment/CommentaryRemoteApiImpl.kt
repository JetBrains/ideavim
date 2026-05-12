/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.comment

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.actions.MultiCaretCodeInsightActionHandler
import com.intellij.codeInsight.generation.CommentByBlockCommentHandler
import com.intellij.codeInsight.generation.CommentByLineCommentHandler
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorId
import com.intellij.openapi.editor.impl.findEditorOrNull
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.maddyhome.idea.vim.group.onEdt

/**
 * Handlers are invoked directly rather than via `ActionManager.tryToExecute` because in
 * Rider / CLion Nova the action dispatch is async — `ActionCallback` signals `done` at
 * dispatch, not completion — so the action's selection survived `removeSelection()` and
 * the selection listener dropped IdeaVim into Visual-Line mode.
 */
internal class CommentaryRemoteApiImpl : CommentaryRemoteApi {

  override suspend fun toggleLineComment(editorId: EditorId, startLine: Int, endLine: Int, caretOffset: Int) = onEdt {
    val editor = editorId.findEditorOrNull() ?: return@onEdt
    val document = editor.document

    val startOffset = document.getLineStartOffset(startLine)
    val endOffset = document.getLineEndOffset(endLine)

    runCommenter(editor, startOffset, endOffset, caretOffset, lineWise = true)
  }

  override suspend fun toggleBlockComment(editorId: EditorId, startOffset: Int, endOffset: Int, caretOffset: Int) =
    onEdt {
      val editor = editorId.findEditorOrNull() ?: return@onEdt
      runCommenter(editor, startOffset, endOffset, caretOffset, lineWise = false)
    }

  private fun runCommenter(
    editor: Editor,
    startOffset: Int,
    endOffset: Int,
    caretOffset: Int,
    lineWise: Boolean,
  ) {
    val project = editor.project ?: return
    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return

    val invokeHandler = {
      CommandProcessor.getInstance().executeCommand(project, {
        ApplicationManager.getApplication().runWriteAction {
          val caret = editor.caretModel.primaryCaret
          caret.setSelection(startOffset, endOffset)
          try {
            val handler = pickHandler(psiFile, lineWise)
            handler.invoke(project, editor, caret, psiFile)
            handler.postInvoke()
          } finally {
            caret.removeSelection()
            if (caretOffset >= 0) {
              caret.moveToOffset(caretOffset)
            }
          }
        }
      }, "Commentary", null)
    }

    // normally comment action goes through rider backend comment action running on .net nto jvm so we cannot call it directly.
    // But we still want to apply space after comment as it's default bahavior there so we overrite this flag for intelij comment handler
    if (isCFamily(psiFile)) {
      val baseSettings = CodeStyle.getSettings(psiFile)
      CodeStyle.runWithLocalSettings(project, baseSettings) { localSettings ->
        localSettings.getCommonSettings(psiFile.language).LINE_COMMENT_ADD_SPACE = true
        invokeHandler()
      }
    } else {
      invokeHandler()
    }
  }

  private fun isCFamily(psiFile: PsiFile): Boolean {
    val fileTypeName = psiFile.fileType.name
    return fileTypeName == "C++" || fileTypeName == "C#" || fileTypeName == "ObjectiveC"
  }

  private fun pickHandler(psiFile: PsiFile, lineWise: Boolean): MultiCaretCodeInsightActionHandler {
    if (lineWise) return CommentByLineCommentHandler()
    val commenter = LanguageCommenters.INSTANCE.forLanguage(psiFile.language)
    val hasBlock = commenter?.blockCommentPrefix != null && commenter.blockCommentSuffix != null
    return if (hasBlock) CommentByBlockCommentHandler() else CommentByLineCommentHandler()
  }
}
