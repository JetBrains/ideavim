/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.autocmd.IjFileTypeMapping
import com.maddyhome.idea.vim.helper.CommenterMarkers
import com.maddyhome.idea.vim.helper.CommenterToComments
import com.maddyhome.idea.vim.helper.FiletypePresets
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

/**
 * Resolves a buffer-local `'comments'` value when an editor is created.
 *
 * Delegates to [OptionGroup.setBufferLocalDefaultIfUntouched], which preserves
 * any value the user explicitly set via `.ideavimrc` or interactive `:set`.
 */
object CommentsOptionInitializer {
  fun initializeForEditor(editor: Editor) {
    val optionGroup = injector.optionGroup as? OptionGroup ?: return
    val resolved = resolveComments(editor) ?: return
    optionGroup.setBufferLocalDefaultIfUntouched(
      Options.comments,
      editor.vim,
      VimString(resolved),
    )
  }

  private fun resolveComments(editor: Editor): String? {
    val filetypeName = filetypeOf(editor) ?: return null
    return FiletypePresets.presetFor(filetypeName) ?: deriveFromCommenter(editor)
  }

  private fun filetypeOf(editor: Editor): String? {
    val virtualFile: VirtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
    return IjFileTypeMapping.toVimFileType(virtualFile)
  }

  private fun deriveFromCommenter(editor: Editor): String? {
    val project = editor.project ?: return null
    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null
    val commenter = LanguageCommenters.INSTANCE.forLanguage(psiFile.language) ?: return null
    return CommenterToComments.derive(
      CommenterMarkers(
        linePrefix = commenter.lineCommentPrefix,
        blockPrefix = commenter.blockCommentPrefix,
        blockSuffix = commenter.blockCommentSuffix,
      ),
    )
  }
}
