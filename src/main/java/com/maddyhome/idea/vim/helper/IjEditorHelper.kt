/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.maddyhome.idea.vim.api.EngineEditorHelper
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimRangeMarker
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim

@Service
internal class IjEditorHelper : EngineEditorHelper {
  override fun amountOfInlaysBeforeVisualPosition(editor: VimEditor, pos: VimVisualPosition): Int {
    return (editor as IjVimEditor).editor.amountOfInlaysBeforeVisualPosition(
      VisualPosition(
        pos.line,
        pos.column,
        pos.leansRight,
      ),
    )
  }

  override fun getVisualLineAtTopOfScreen(editor: VimEditor): Int {
    return EditorHelper.getVisualLineAtTopOfScreen(editor.ij)
  }

  override fun getApproximateScreenWidth(editor: VimEditor): Int {
    return EditorHelper.getApproximateScreenWidth(editor.ij)
  }

  override fun handleWithReadonlyFragmentModificationHandler(editor: VimEditor, exception: Exception) {
    return EditorActionManager.getInstance()
      .getReadonlyFragmentModificationHandler(editor.ij.document)
      .handle(exception as ReadOnlyFragmentModificationException?)
  }

  override fun getVisualLineAtBottomOfScreen(editor: VimEditor): Int {
    return EditorHelper.getVisualLineAtBottomOfScreen(editor.ij)
  }

  override fun pad(editor: VimEditor, line: Int, to: Int): String {
    return EditorHelper.pad(editor.ij, line, to)
  }

  override fun inlayAwareOffsetToVisualPosition(editor: VimEditor, offset: Int): VimVisualPosition {
    return EditorUtil.inlayAwareOffsetToVisualPosition(editor.ij, offset).vim
  }

  override fun createRangeMarker(editor: VimEditor, startOffset: Int, endOffset: Int): VimRangeMarker {
    val ijRangeMarker = editor.ij.document.createRangeMarker(startOffset, endOffset)
    return object : VimRangeMarker {
      override val startOffset: Int
        get() = ijRangeMarker.startOffset
      override val endOffset: Int
        get() = ijRangeMarker.endOffset

      override fun dispose() {
        ijRangeMarker.dispose()
      }
    }
  }
}
