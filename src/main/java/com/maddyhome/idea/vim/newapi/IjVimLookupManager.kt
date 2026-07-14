/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.IdeLookup
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimLookupManager

class IjVimLookupManager : VimLookupManager {
  override fun getActiveLookup(editor: VimEditor): IjLookup? {
    return LookupManager.getActiveLookup(editor.ij)?.let { IjLookup(it) }
  }

  override fun showCustomLookup(
    editor: VimEditor,
    values: List<String>,
    prefix: String,
  ) {
    val ijEditor = editor.ij
    val project = ijEditor.project ?: return
    val prefixStart = ijEditor.caretModel.offset - prefix.length

    val items: Array<LookupElement> = values.map { LookupElementBuilder.create(it) }.toTypedArray()

    val lookup = LookupManager.getInstance(project).showLookup(ijEditor, *items) ?: return

    lookup.addLookupListener(object : LookupListener {
      override fun itemSelected(event: LookupEvent) {
        val value = event.item?.lookupString ?: return
        WriteCommandAction.runWriteCommandAction(project) {
          ijEditor.document.replaceString(prefixStart, ijEditor.caretModel.offset, value)
          ijEditor.caretModel.moveToOffset(prefixStart + value.length)
        }
      }
    })
  }
}

class IjLookup(val lookup: Lookup) : IdeLookup {
  override fun down(caret: ImmutableVimCaret, context: ExecutionContext) {
    EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN)
      .execute(caret.editor.ij, caret.ij, context.ij)
  }

  override fun up(caret: ImmutableVimCaret, context: ExecutionContext) {
    EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP)
      .execute(caret.editor.ij, caret.ij, context.ij)
  }

  override fun close(caret: ImmutableVimCaret, context: ExecutionContext) {
    caret.editor.ij.project?.let { LookupManager.hideActiveLookup(it) }
  }

  override fun accept(
    caret: ImmutableVimCaret,
    context: ExecutionContext,
  ) {
    EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM)
      .execute(caret.editor.ij, caret.ij, context.ij)
  }
}
