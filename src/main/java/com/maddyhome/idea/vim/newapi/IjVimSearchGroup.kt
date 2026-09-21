/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimSearchGroupBase
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.Direction.Companion.fromInt
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.group.XMLGroup
import com.maddyhome.idea.vim.helper.addSubstitutionConfirmationHighlight
import com.maddyhome.idea.vim.helper.clearCurrentSearchMatchHighlight
import com.maddyhome.idea.vim.helper.updateSearchCount
import com.maddyhome.idea.vim.helper.updateSearchHighlights
import com.maddyhome.idea.vim.helper.vimIncsearchCurrentMatchOffset
import com.maddyhome.idea.vim.helper.vimSearchHighlights
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import org.jdom.Element
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.TestOnly

@State(
  name = "VimSearchSettings",
  storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)]
)
open class IjVimSearchGroup : VimSearchGroupBase(), PersistentStateComponent<Element>, VimSearchGroupLegacyLoader {
  companion object {
    private val logger by lazy { vimLogger<IjVimSearchGroup>() }
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

  override fun updateSearchHighlights(force: Boolean) {
    updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, force)
  }

  override fun updateSearchCount(matchOffset: Int) {
    updateSearchCount(getLastUsedPattern(), lastIgnoreSmartCase, matchOffset)
  }

  override fun resetIncsearchHighlights() {
    updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, true)
  }

  override fun addSubstitutionConfirmationHighlight(
    editor: VimEditor,
    startOffset: Int,
    endOffset: Int,
  ): SearchHighlight {

    val ijEditor = editor.ij
    val highlighter = addSubstitutionConfirmationHighlight(
      ijEditor,
      startOffset,
      endOffset
    )
    return IjSearchHighlight(ijEditor, highlighter)
  }

  @TestOnly
  override fun resetState() {
    super.resetState()
    showSearchHighlight = injector.globalOptions().hlsearch
  }

  override fun isSomeTextHighlighted(): Boolean {
    val vimEditors = injector.editorGroup.getEditors().filter {
      (injector.application.isUnitTest() || it.ij.component.isShowing)
    }
    for (vimEditor in vimEditors) {
      val editor = vimEditor.ij
      if (editor.vimSearchHighlights.hasHighlights) {
        return true
      }
    }
    return false
  }

  override fun getCurrentIncsearchResultRange(editor: VimEditor): TextRange? {
    val ijEditor = editor.ij
    val currentOffset = ijEditor.vimIncsearchCurrentMatchOffset ?: return null
    val currentHighlighter = ijEditor.vimSearchHighlights.activeHighlighters.find { it.startOffset == currentOffset }
    return currentHighlighter?.textRange?.vim
  }

  override fun setShouldShowSearchHighlights() {
    showSearchHighlight = injector.globalOptions().hlsearch
  }

  override fun clearSearchHighlight() {
    showSearchHighlight = false
    updateSearchHighlights(false)
  }

  override fun saveData(element: Element) {
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
      element.addContent(XMLGroup.getInstance().setSafeXmlText(Element(name), text))
    }
  }

  override fun readData(element: Element) {
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

    val show = search.getChild("show-last")?.text?.toBoolean() ?: false
    showSearchHighlight = show && !injector.globalOptions().viminfo.contains("h")
    if (logger.isDebug()) {
      logger.debug("show=$show")
      logger.debug("showSearchHighlight=$showSearchHighlight")
    }
  }

  private fun getSafeChildText(element: Element, name: String): String? {
    val child = element.getChild(name)
    return if (child != null) XMLGroup.getInstance().getSafeXmlText(child) else null
  }

  private fun getSafeChildText(element: Element, name: String, defaultValue: String): String {
    val child = element.getChild(name)
    if (child != null) {
      val value = XMLGroup.getInstance().getSafeXmlText(child)
      return value ?: defaultValue
    }
    return defaultValue
  }

  override fun getState(): Element? {
    val element = Element("search")
    saveData(element)
    return element
  }

  override fun loadState(state: Element) {
    readData(state)
  }

  /**
   * Updates search highlights when the selected editor changes
   */
  fun fileEditorManagerSelectionChangedCallback(@Suppress("unused") event: FileEditorManagerEvent) {
    updateSearchHighlights(false)
  }

  fun turnOn() {
    updateSearchHighlights(false)
  }

  fun turnOff() {
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
   * Rebuilds the search highlights of the changed document, since the edit may have created or destroyed matches
   */
  class DocumentSearchListener @Contract(pure = true) private constructor() : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
      // Loop over all local editors for the changed document, across all projects, and update search highlights.
      // Note that the change may have come from a remote guest in Code With Me scenarios (in which case
      // ClientId.current will be a guest ID), but we don't care - we still need to add/remove highlights for the
      // changed text. Make sure we only update local editors, though.
      for (vimEditor in injector.editorGroup.getEditors(IjVimDocument(event.document))) {
        val editor = vimEditor.ij
        if (!editor.vimSearchHighlights.isActive) continue

        editor.vimSearchHighlights.refreshAfterDocumentChange()

        // The rebuilt highlights are all normal matches. An edit never makes a match current - only an in-progress
        // 'incsearch' has one, and this puts its highlight back if the rebuild removed it
        clearCurrentSearchMatchHighlight(editor)
      }
    }

    companion object {
      var INSTANCE: DocumentSearchListener = DocumentSearchListener()
    }
  }
}
