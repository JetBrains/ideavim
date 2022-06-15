package com.maddyhome.idea.vim.helper

import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.util.text.StringUtil
import com.maddyhome.idea.vim.api.EngineEditorHelper
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import java.nio.CharBuffer

@Service
class IjEditorHelper : EngineEditorHelper {
  override fun normalizeOffset(editor: VimEditor, offset: Int, allowEnd: Boolean): Int {
    return EditorHelper.normalizeOffset((editor as IjVimEditor).editor, offset, allowEnd)
  }

  override fun normalizeOffset(editor: VimEditor, line: Int, offset: Int, allowEnd: Boolean): Int {
    return EditorHelper.normalizeOffset((editor as IjVimEditor).editor, line, offset, allowEnd)
  }

  override fun getText(editor: VimEditor, range: TextRange): String {
    return EditorHelper.getText((editor as IjVimEditor).editor, range)
  }

  override fun getOffset(editor: VimEditor, line: Int, column: Int): Int {
    return EditorHelper.getOffset((editor as IjVimEditor).editor, line, column)
  }

  override fun logicalLineToVisualLine(editor: VimEditor, line: Int): Int {
    return EditorHelper.logicalLineToVisualLine((editor as IjVimEditor).editor, line)
  }

  override fun normalizeVisualLine(editor: VimEditor, line: Int): Int {
    return EditorHelper.normalizeVisualLine((editor as IjVimEditor).editor, line)
  }

  override fun normalizeVisualColumn(editor: VimEditor, line: Int, col: Int, allowEnd: Boolean): Int {
    return EditorHelper.normalizeVisualColumn((editor as IjVimEditor).editor, line, col, allowEnd)
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

  override fun getVisualLineCount(editor: VimEditor): Int {
    return EditorHelper.getVisualLineCount(editor)
  }

  override fun prepareLastColumn(caret: VimCaret): Int {
    return EditorHelper.prepareLastColumn((caret as IjVimCaret).caret)
  }

  override fun updateLastColumn(caret: VimCaret, prevLastColumn: Int) {
    EditorHelper.updateLastColumn((caret as IjVimCaret).caret, prevLastColumn)
  }

  override fun getLineEndOffset(editor: VimEditor, line: Int, allowEnd: Boolean): Int {
    return EditorHelper.getLineEndOffset((editor as IjVimEditor).editor, line, allowEnd)
  }

  override fun getLineStartOffset(editor: VimEditor, line: Int): Int {
    return EditorHelper.getLineStartOffset((editor as IjVimEditor).editor, line)
  }

  override fun getLineStartForOffset(editor: VimEditor, line: Int): Int {
    return EditorHelper.getLineStartForOffset((editor as IjVimEditor).editor, line)
  }

  override fun getLineEndForOffset(editor: VimEditor, offset: Int): Int {
    return EditorHelper.getLineEndForOffset((editor as IjVimEditor).editor, offset)
  }

  override fun visualLineToLogicalLine(editor: VimEditor, line: Int): Int {
    return EditorHelper.visualLineToLogicalLine(editor.ij, line)
  }

  override fun normalizeLine(editor: VimEditor, line: Int): Int {
    return EditorHelper.normalizeLine(editor.ij, line)
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

  override fun getLineLength(editor: VimEditor, logicalLine: Int): Int {
    return EditorHelper.getLineLength(editor.ij, logicalLine)
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

  override fun getVisualLineLength(editor: VimEditor, line: Int): Int {
    return EditorHelper.getVisualLineLength(editor.ij, line)
  }

  override fun getLeadingWhitespace(editor: VimEditor, line: Int): String {
    return EditorHelper.getLeadingWhitespace(editor.ij, line)
  }
}
