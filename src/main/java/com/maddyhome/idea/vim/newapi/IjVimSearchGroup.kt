/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
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
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.handlers.SubmatchFunctionHandler
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser.parseExpression
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

public open class IjVimSearchGroup : VimSearchGroupBase() {

  init {
    // TODO: Investigate migrating these listeners to use the effective value change listener
    // This would allow us to update the editor we're told to update, rather than looping over all projects and updating
    // the highlights in that project's current document's open editors (see VIM-2779).
    // However, we probably only want to update the editors associated with the current document, so maybe the whole
    // code needs to be reworked. We're currently using the same update code for changes in the search term as well as
    // changes in the search options.
    VimPlugin.getOptionGroup().addGlobalOptionChangeListener(Options.hlsearch) {
      resetSearchHighlight()
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

  /**
   * Reset the search highlights to the last used pattern after highlighting incsearch results.
   */
  override fun resetIncsearchHighlights() {
    updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, true)
  }

  override fun confirmChoice(
    editor: VimEditor,
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
      val exEntryPanel: com.maddyhome.idea.vim.ui.ex.ExEntryPanel =
        com.maddyhome.idea.vim.ui.ex.ExEntryPanel.getInstanceWithoutShortcuts()
      val context = injector.executionContextManager.onEditor(editor, null)
      exEntryPanel.activate(
        editor.ij,
        (context as IjEditorExecutionContext).context,
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

  override fun parseVimScriptExpression(expressionString: String): Expression? {
    return parseExpression(expressionString)
  }

  override fun addSubstitutionConfirmationHighlight(editor: VimEditor, startOffset: Int, endOffset: Int) {
    val hl = addSubstitutionConfirmationHighlight(
      (editor as IjVimEditor).editor,
      startOffset,
      endOffset
    )
    editor.editor.markupModel.removeHighlighter(hl)
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

  override fun resetSearchHighlight() {
    showSearchHighlight = injector.globalOptions().hlsearch
  }

  override fun clearSearchHighlight() {
    showSearchHighlight = false
    updateSearchHighlights(false)
  }
}