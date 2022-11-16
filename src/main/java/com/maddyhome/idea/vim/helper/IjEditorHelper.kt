/*
 * Copyright 2003-2022 The IdeaVim authors
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
import com.intellij.openapi.util.text.StringUtil
import com.maddyhome.idea.vim.api.EngineEditorHelper
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import java.nio.CharBuffer

@Service
class IjEditorHelper : EngineEditorHelper {
  override fun logicalLineToVisualLine(editor: VimEditor, line: Int): Int {
    return EditorHelper.logicalLineToVisualLine((editor as IjVimEditor).editor, line)
  }

  override fun amountOfInlaysBeforeVisualPosition(editor: VimEditor, pos: VimVisualPosition): Int {
    return (editor as IjVimEditor).editor.amountOfInlaysBeforeVisualPosition(
      VisualPosition(
        pos.line,
        pos.column,
        pos.leansRight
      )
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

  override fun getLineBuffer(editor: VimEditor, line: Int): CharBuffer {
    return EditorHelper.getLineBuffer(editor.ij, line)
  }

  override fun getVisualLineAtBottomOfScreen(editor: VimEditor): Int {
    return EditorHelper.getVisualLineAtBottomOfScreen(editor.ij)
  }

  override fun pad(editor: VimEditor, context: ExecutionContext, line: Int, to: Int): String {
    return EditorHelper.pad(editor.ij, context.ij, line, to)
  }

  override fun getLineLength(editor: VimEditor): Int {
    return EditorHelper.getLineLength(editor.ij)
  }

  override fun getLineBreakCount(text: CharSequence): Int {
    return StringUtil.getLineBreakCount(text)
  }

  override fun inlayAwareOffsetToVisualPosition(editor: VimEditor, offset: Int): VimVisualPosition {
    return EditorUtil.inlayAwareOffsetToVisualPosition(editor.ij, offset).vim
  }

  override fun getLeadingWhitespace(editor: VimEditor, line: Int): String {
    return EditorHelper.getLeadingWhitespace(editor.ij, line)
  }

  override fun anyNonWhitespace(editor: VimEditor, offset: Int, dir: Int): Boolean {
    return SearchHelper.anyNonWhitespace(editor.ij, offset, dir)
  }
}
