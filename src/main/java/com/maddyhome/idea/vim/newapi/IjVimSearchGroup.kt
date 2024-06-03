/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimSearchGroupBase
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.Direction.Companion.fromInt
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.TestInputModel.Companion.getInstance
import com.maddyhome.idea.vim.helper.addSubstitutionConfirmationHighlight
import com.maddyhome.idea.vim.helper.highlightSearchResults
import com.maddyhome.idea.vim.helper.isCloseKeyStroke
import com.maddyhome.idea.vim.helper.shouldIgnoreCase
import com.maddyhome.idea.vim.helper.updateSearchHighlights
import com.maddyhome.idea.vim.helper.vimLastHighlighters
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import com.maddyhome.idea.vim.ui.ModalEntry
import com.maddyhome.idea.vim.vimscript.model.functions.handlers.SubmatchFunctionHandler
import org.jdom.Element
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

@State(
  name = "VimSearchSettings",
  storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)]
)
public open class IjVimSearchGroup : VimSearchGroupBase(), PersistentStateComponent<Element> {
  public companion object {
    private val logger = vimLogger<IjVimSearchGroup>()
  }

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
      )
      caret.moveToOffset(startOffset)
      ModalEntry.activate(editor, keyStrokeProcessor)
      exEntryPanel.deactivate(refocusOwningEditor = true, resetCaret = false)
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

  public fun saveData(element: Element) {
    logger.debug("saveData")
    val search = Element("search")

    addOptionalTextElement(search, "last-search", lastSearchPattern)
    addOptionalTextElement(search, "last-substitute", lastSubstitutePattern)
    addOptionalTextElement(search, "last-offset", lastPatternTrailing)
    addOptionalTextElement(search, "last-replace", lastReplaceString)
    addOptionalTextElement(
      search,
      "last-pattern",
      if (lastPatternType == PatternType.SEARCH) lastSearchPattern else lastSubstitutePattern
    )
    addOptionalTextElement(search, "last-dir", getLastSearchDirection().toInt().toString())
    addOptionalTextElement(search, "show-last", showSearchHighlight.toString())

    element.addContent(search)
  }

  private fun addOptionalTextElement(element: Element, name: String, text: String?) {
    if (text != null) {
      element.addContent(VimPlugin.getXML().setSafeXmlText(Element(name), text))
    }
  }

  public fun readData(element: Element) {
    logger.debug("readData")
    val search = element.getChild("search") ?: return

    lastSearchPattern = getSafeChildText(search, "last-search")
    lastSubstitutePattern = getSafeChildText(search, "last-substitute")
    lastReplaceString = getSafeChildText(search, "last-replace")
    lastPatternTrailing = getSafeChildText(search, "last-offset", "")

    val lastPatternText = getSafeChildText(search, "last-pattern")
    if (lastPatternText == null || lastPatternText == lastSearchPattern) {
      lastPatternType = PatternType.SEARCH
    } else {
      lastPatternType = PatternType.SUBSTITUTE
    }

    val dir = search.getChild("last-dir")
    try {
      lastDirection = fromInt(dir.text.toInt())
    } catch (e: NumberFormatException) {
      lastDirection = Direction.FORWARDS
    }

    val show = search.getChild("show-last")
    val disableHighlight = injector.globalOptions().viminfo.contains("h")
    showSearchHighlight = !disableHighlight && show.text.toBoolean()
    if (logger.isDebug()) {
      logger.debug("show=" + show + "(" + show.text + ")")
      logger.debug("showSearchHighlight=$showSearchHighlight")
    }
  }

  private fun getSafeChildText(element: Element, name: String): String? {
    val child = element.getChild(name)
    return if (child != null) VimPlugin.getXML().getSafeXmlText(child) else null
  }

  private fun getSafeChildText(element: Element, name: String, defaultValue: String): String {
    val child = element.getChild(name)
    if (child != null) {
      val value = VimPlugin.getXML().getSafeXmlText(child)
      return value ?: defaultValue
    }
    return defaultValue
  }

  public override fun getState(): Element? {
    val element = Element("search")
    saveData(element)
    return element
  }

  public override fun loadState(state: Element) {
    readData(state)
  }

  /**
   * Updates search highlights when the selected editor changes
   */
  public fun fileEditorManagerSelectionChangedCallback(@Suppress("unused") event: FileEditorManagerEvent) {
    updateSearchHighlights(false)
  }

  public fun turnOn() {
    updateSearchHighlights(false)
  }

  public fun turnOff() {
    val show = showSearchHighlight
    clearSearchHighlight()
    showSearchHighlight = show
  }

  private class IjSearchHighlight(private val editor: Editor, private val highlighter: RangeHighlighter) :
    SearchHighlight() {

    override fun remove() {
      editor.markupModel.removeHighlighter(highlighter)
    }
  }


  /**
   * Removes and adds highlights for current search pattern when the document is edited
   */
  public class DocumentSearchListener @Contract(pure = true) private constructor() : DocumentListener {
    public override fun documentChanged(event: DocumentEvent) {
      // Loop over all local editors for the changed document, across all projects, and update search highlights.
      // Note that the change may have come from a remote guest in Code With Me scenarios (in which case
      // ClientId.current will be a guest ID), but we don't care - we still need to add/remove highlights for the
      // changed text. Make sure we only update local editors, though.
      val document = event.document
      for (vimEditor in injector.editorGroup.getEditors(IjVimDocument(document))) {
        val editor = (vimEditor as IjVimEditor).editor
        var existingHighlighters = editor.vimLastHighlighters ?: continue

        if (logger.isDebug()) {
          logger.debug("hls=$existingHighlighters")
          logger.debug("event=$event")
        }

        // We can only re-highlight whole lines, so clear any highlights in the affected lines.
        // If we're deleting lines, this will clear + re-highlight the new current line, which hasn't been modified.
        // However, we still want to re-highlight this line in case any highlights cross the line boundaries.
        // If we're adding lines, this will clear + re-highlight all new lines.
        val startPosition = editor.offsetToLogicalPosition(event.offset)
        val endPosition = editor.offsetToLogicalPosition(event.offset + event.newLength)
        val startLineOffset = document.getLineStartOffset(startPosition.line)
        val endLineOffset = document.getLineEndOffset(endPosition.line)

        // Remove any highlights that have already been deleted, and remove + clear those that intersect with the change
        val iter = existingHighlighters.iterator()
        while (iter.hasNext()) {
          val highlighter = iter.next()
          if (!highlighter.isValid) {
            iter.remove()
          } else if (highlighter.textRange.intersects(startLineOffset, endLineOffset)) {
            iter.remove()
            editor.markupModel.removeHighlighter(highlighter)
          }
        }

        (injector.searchGroup as VimSearchGroupBase).highlightSearchLines(editor.vim, startPosition.line, endPosition.line)

        if (logger.isDebug()) {
          existingHighlighters = editor.vimLastHighlighters!!
          logger.debug("sl=" + startPosition.line + ", el=" + endPosition.line)
          logger.debug("hls=$existingHighlighters")
        }
      }
    }

    public companion object {
      public var INSTANCE: DocumentSearchListener = DocumentSearchListener()
    }
  }
}
