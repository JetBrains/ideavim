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
import com.maddyhome.idea.vim.api.EngineEditorHelperBase
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimRangeMarker
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim

@Service
internal class IjEditorHelper : EngineEditorHelperBase() {
  override fun amountOfInlaysBeforeVisualPosition(editor: VimEditor, pos: VimVisualPosition): Int {
    require(pos.line >= 0)
    require(pos.column >= 0)
    val visualPosition = VisualPosition(pos.line, pos.column, pos.leansRight)
    return (editor as IjVimEditor).editor.amountOfInlaysBeforeVisualPosition(visualPosition)
  }

  override fun getVisualLineAtTopOfScreen(editor: VimEditor): Int {
    return EditorHelper.getVisualLineAtTopOfScreen(editor.ij)
  }

  override fun getApproximateScreenWidth(editor: VimEditor): Int {
    return EditorHelper.getApproximateScreenWidth(editor.ij)
  }

  override fun getApproximateOutputPanelWidth(editor: VimEditor): Int {
    return EditorHelper.getApproximateOutputPanelWidth(editor.ij)
  }

  override fun handleWithReadonlyFragmentModificationHandler(editor: VimEditor, exception: Exception) {
    return EditorActionManager.getInstance()
      .getReadonlyFragmentModificationHandler(editor.ij.document)
      .handle(exception as ReadOnlyFragmentModificationException?)
  }

  override fun getVisualLineAtBottomOfScreen(editor: VimEditor): Int {
    return EditorHelper.getVisualLineAtBottomOfScreen(editor.ij)
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
      override val isValid: Boolean
        get() = ijRangeMarker.isValid

      override fun dispose() {
        ijRangeMarker.dispose()
      }
    }
  }

  override fun getVisualLineLength(editor: VimEditor, visualLine: Int) =
    EditorHelper.getVisualLineLength(editor.ij, visualLine)

  override fun normalizeVisualColumn(editor: VimEditor, visualLine: Int, col: Int, allowEnd: Boolean) =
    EditorHelper.normalizeVisualColumn(editor.ij, visualLine, col, allowEnd)
}
