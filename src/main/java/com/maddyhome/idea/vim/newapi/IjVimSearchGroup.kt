/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimSearchGroupBase
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.TestInputModel.Companion.getInstance
import com.maddyhome.idea.vim.helper.addSubstitutionConfirmationHighlight
import com.maddyhome.idea.vim.helper.highlightSearchResults
import com.maddyhome.idea.vim.helper.isCloseKeyStroke
import com.maddyhome.idea.vim.helper.shouldIgnoreCase
import com.maddyhome.idea.vim.helper.updateSearchHighlights
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import com.maddyhome.idea.vim.ui.ModalEntry
import com.maddyhome.idea.vim.vimscript.model.functions.handlers.SubmatchFunctionHandler
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

public open class IjVimSearchGroup : VimSearchGroupBase() {

  init {
    // We use the global option listener instead of the effective listener that gets called for each affected editor
    // because we handle updating the affected editors ourselves (e.g., we can filter for visible windows).
    VimPlugin.getOptionGroup().addGlobalOptionChangeListener(Options.hlsearch) {
      setShouldShowSearchHighlights()
      updateSearchHighlights(true)
    }

    val updateHighlightsIfVisible = GlobalOptionChangeListener {
      if (showSearchHighlight) {
        updateSearchHighlights(true)
      }
    }
    VimPlugin.getOptionGroup().addGlobalOptionChangeListener(Options.ignorecase, updateHighlightsIfVisible)
    VimPlugin.getOptionGroup().addGlobalOptionChangeListener(Options.smartcase, updateHighlightsIfVisible)
  }

  private var showSearchHighlight: Boolean = injector.globalOptions().hlsearch

  override fun highlightSearchLines(
    editor: VimEditor,
    startLine: Int,
    endLine: Int,
  ) {
    val pattern = getLastUsedPattern()
    if (pattern != null) {
      val results = injector.searchHelper.findAll(
        editor, pattern, startLine, endLine,
        shouldIgnoreCase(pattern, lastIgnoreSmartCase)
      )
      highlightSearchResults(editor.ij, pattern, results, -1)
    }
  }

  override fun updateSearchHighlights(force: Boolean) {
    updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, force)
  }

  override fun resetIncsearchHighlights() {
    updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, true)
  }

  override fun confirmChoice(
    editor: VimEditor,
    context: ExecutionContext,
    match: String,
    caret: VimCaret,
    startOffset: Int,
  ): ReplaceConfirmationChoice {
    val result: Ref<ReplaceConfirmationChoice> = Ref.create(ReplaceConfirmationChoice.QUIT)
    val keyStrokeProcessor: Function1<KeyStroke, Boolean> = label@{ key: KeyStroke ->
      val choice: ReplaceConfirmationChoice
      val c = key.keyChar
      choice = if (key.isCloseKeyStroke() || c == 'q') {
        ReplaceConfirmationChoice.QUIT
      } else if (c == 'y') {
        ReplaceConfirmationChoice.SUBSTITUTE_THIS
      } else if (c == 'l') {
        ReplaceConfirmationChoice.SUBSTITUTE_LAST
      } else if (c == 'n') {
        ReplaceConfirmationChoice.SKIP
      } else if (c == 'a') {
        ReplaceConfirmationChoice.SUBSTITUTE_ALL
      } else {
        return@label true
      }
      // TODO: Handle <C-E> and <C-Y>
      result.set(choice)
      false
    }
    if (ApplicationManager.getApplication().isUnitTestMode) {
      caret.moveToOffset(startOffset)
      val inputModel = getInstance(editor.ij)
      var key = inputModel.nextKeyStroke()
      while (key != null) {
        if (!keyStrokeProcessor.invoke(key)) {
          break
        }
        key = inputModel.nextKeyStroke()
      }
    } else {
      // XXX: The Ex entry panel is used only for UI here, its logic might be inappropriate for this method
      val exEntryPanel = injector.commandLine.createWithoutShortcuts(
        editor,
        context,
        MessageHelper.message("replace.with.0", match),
        "",
        1
      )
      caret.moveToOffset(startOffset)
      ModalEntry.activate(editor, keyStrokeProcessor)
      exEntryPanel.deactivate(true, false)
    }
    return result.get()
  }

  override fun addSubstitutionConfirmationHighlight(
    editor: VimEditor,
    startOffset: Int,
    endOffset: Int,
  ): SearchHighlight {

    val ijEditor = (editor as IjVimEditor).editor
    val highlighter = addSubstitutionConfirmationHighlight(
      ijEditor,
      startOffset,
      endOffset
    )
    return IjSearchHighlight(ijEditor, highlighter)
  }

  override fun setLatestMatch(match: String) {
    SubmatchFunctionHandler.getInstance().latestMatch = match
  }

  override fun replaceString(
    editor: VimEditor,
    startOffset: Int,
    endOffset: Int,
    newString: String,
  ) {
    ApplicationManager.getApplication().runWriteAction {
      (editor as IjVimEditor).editor.document.replaceString(startOffset, endOffset, newString)
    }
  }

  @TestOnly
  override fun resetState() {
    super.resetState()
    showSearchHighlight = injector.globalOptions().hlsearch
  }

  override fun setShouldShowSearchHighlights() {
    showSearchHighlight = injector.globalOptions().hlsearch
  }

  override fun clearSearchHighlight() {
    showSearchHighlight = false
    updateSearchHighlights(false)
  }

  private class IjSearchHighlight(private val editor: Editor, private val highlighter: RangeHighlighter) :
    SearchHighlight() {

    override fun remove() {
      editor.markupModel.removeHighlighter(highlighter)
    }
  }
}
