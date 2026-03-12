/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.comment

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorId
import com.intellij.openapi.editor.impl.findEditorOrNull
import com.maddyhome.idea.vim.group.onEdt

/**
 * RPC handler for [CommentaryRemoteApi].
 *
 * Sets selection on the backend editor and executes the platform's comment action.
 * Because this runs on the backend, [com.intellij.openapi.command.CommandProcessor]
 * groups all document modifications as a single undo step.
 *
 * The selection is set on the backend editor only — it doesn't affect the frontend
 * editor's visual state, and is cleaned up immediately after the action executes.
 */
internal class CommentaryRemoteApiImpl : CommentaryRemoteApi {

  override suspend fun toggleLineComment(editorId: EditorId, startLine: Int, endLine: Int, caretOffset: Int) = onEdt {
    val editor = editorId.findEditorOrNull() ?: return@onEdt
    val document = editor.document

    val startOffset = document.getLineStartOffset(startLine)
    val endOffset = document.getLineEndOffset(endLine)

    executeCommentAction(editor, startOffset, endOffset, caretOffset, IdeActions.ACTION_COMMENT_LINE)
  }

  override suspend fun toggleBlockComment(editorId: EditorId, startOffset: Int, endOffset: Int, caretOffset: Int) =
    onEdt {
      val editor = editorId.findEditorOrNull() ?: return@onEdt
      // Try block comment first, fall back to line comment
      if (!executeCommentAction(editor, startOffset, endOffset, caretOffset, IdeActions.ACTION_COMMENT_BLOCK)) {
        executeCommentAction(editor, startOffset, endOffset, caretOffset, IdeActions.ACTION_COMMENT_LINE)
      }
    }

  private fun executeCommentAction(
    editor: Editor,
    startOffset: Int,
    endOffset: Int,
    caretOffset: Int,
    actionId: String,
  ): Boolean {
    var result = false
    // Wrap selection + action + caret reset + cleanup in a single command so everything
    // is a single undo step. In remdev, undo restores pre-command editor state — if
    // selection is set before the command, undo would restore it. The nested tryToExecute
    // command merges into this outer command.
    CommandProcessor.getInstance().executeCommand(editor.project, {
      editor.selectionModel.setSelection(startOffset, endOffset)
      val action = ActionManager.getInstance().getAction(actionId)
      result = ActionManager.getInstance().tryToExecute(action, null, editor.contentComponent, "IdeaVim", true)
        .let { it.waitFor(5_000); it.isDone }
      editor.selectionModel.removeSelection()
      if (caretOffset >= 0) {
        editor.caretModel.moveToOffset(caretOffset)
      }
    }, "Commentary", null)
    return result
  }
}
