/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.copy

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.change.insert.replaceTextPastedInReplaceMode
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

/**
 * Makes the IDE paste (e.g. Cmd+V, Edit | Paste, or paste from history) replace text in Replace mode, like Vim does,
 * instead of inserting it.
 *
 * The IDE still does the paste, so everything it does when pasting (adding imports, reformatting, etc.) keeps working.
 * Afterwards, the pasted text replaces the characters that followed the caret, and is recorded in the replace mask so
 * that backspace can put them back.
 *
 * This is a post processor rather than a handler for the paste action, because some plugins (e.g. Jupyter) register
 * their own paste handler that pastes without calling the other handlers. All of them call the post processors, as part
 * of the same command, so the paste and the replacement are undone together.
 */
internal class ReplaceModePastePostProcessor : CopyPastePostProcessor<ReplaceModePastePostProcessor.PasteData>() {
  override fun collectTransferableData(
    file: PsiFile,
    editor: Editor,
    startOffsets: IntArray,
    endOffsets: IntArray,
  ): List<PasteData> = emptyList()

  // The IDE only calls processTransferableData if this returns some data. We don't know which editor the text is pasted
  // into yet, so we always return it and check the mode there
  override fun extractTransferableData(content: Transferable): List<PasteData> = listOf(PasteData)

  override fun requiresAllDocumentsToBeCommitted(editor: Editor, project: Project): Boolean = false

  override fun processTransferableData(
    project: Project,
    editor: Editor,
    bounds: RangeMarker,
    caretOffset: Int,
    indented: Ref<in Boolean>,
    values: List<PasteData>,
  ) {
    if (VimPlugin.isNotEnabled() || editor.isIdeaVimDisabledHere || editor.vim.mode != Mode.REPLACE) return
    if (!bounds.isValid) return

    ApplicationManager.getApplication().runWriteAction {
      replaceTextPastedInReplaceMode(editor.vim, bounds.startOffset, bounds.endOffset)
    }
  }

  object PasteData : TextBlockTransferableData {
    private val flavor = DataFlavor(PasteData::class.java, "IdeaVim Replace mode paste")

    override fun getFlavor(): DataFlavor = flavor
  }
}
