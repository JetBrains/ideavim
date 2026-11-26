/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.matchup

import com.intellij.codeInsight.hint.EditorFragmentComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.ui.LightweightHint
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.matchit.BlockPatterns
import com.maddyhome.idea.vim.extension.matchit.Delimiter
import com.maddyhome.idea.vim.extension.matchit.blockPatterns
import com.maddyhome.idea.vim.extension.matchit.findDelimiterAt
import com.maddyhome.idea.vim.extension.matchit.findSurroundingBlocks
import com.maddyhome.idea.vim.extension.matchit.wordBlockPatterns
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.VimPluginContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import kotlin.math.max

/**
 * Highlights the words of the block the cursor is on, e.g. "if", "else" and "end", and shows the line of a match
 * that is off screen.
 *
 * The brackets of 'matchpairs' are left to the IDE, which already highlights them. The search runs in the background
 * on committed PSI, since comments and strings are skipped, and only the latest search for an editor is shown.
 */
internal class MatchParen :
  CaretListener, DocumentListener, EditorFactoryListener, VisibleAreaListener, ModeChangeListener {
  private var disposable: Disposable? = null
  private var alarm: Alarm? = null

  /** `:NoMatchParen` and `:DoMatchParen` */
  var isEnabled: Boolean = true
    set(value) {
      field = value
      if (value) EditorFactory.getInstance().allEditors.forEach(::scheduleUpdate) else clearAll()
    }

  fun attach() {
    detach()
    val disposable = Disposer.newDisposable(VimPlugin.getInstance(), "matchup matchparen")
    this.disposable = disposable
    alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, disposable)
    val multicaster = EditorFactory.getInstance().eventMulticaster
    multicaster.addCaretListener(this, disposable)
    multicaster.addDocumentListener(this, disposable)
    EditorFactory.getInstance().addEditorFactoryListener(this, disposable)
    multicaster.addVisibleAreaListener(this, disposable)
    injector.listenersNotifier.modeChangeListeners.add(this)
    VimPlugin.getInstance().onOffDisposable?.let { Disposer.register(it) { clearAll() } }
    isEnabled = getVariable(null, Scope.GLOBAL_VARIABLE, "matchup_matchparen_enabled")?.asInt() != 0
  }

  fun detach() {
    injector.listenersNotifier.modeChangeListeners.remove(this)
    disposable?.let(Disposer::dispose)
    disposable = null
    alarm = null
    clearAll()
  }

  override fun caretPositionChanged(event: CaretEvent) {
    val editor = event.editor
    if (event.caret != editor.caretModel.primaryCaret) return
    editor.getUserData(HIGHLIGHTS)?.isSurroundRequested = false
    scheduleUpdate(editor)
  }

  override fun documentChanged(event: DocumentEvent) {
    EditorFactory.getInstance().getEditors(event.document).forEach(::scheduleUpdate)
  }

  override fun editorCreated(event: EditorFactoryEvent) {
    scheduleUpdate(event.editor)
  }

  override fun visibleAreaChanged(event: VisibleAreaEvent) {
    event.editor.getUserData(HIGHLIGHTS)?.refreshOffscreen()
  }

  override fun modeChanged(editor: VimEditor, oldMode: Mode) {
    scheduleUpdate(editor.ij)
  }

  /** `<Plug>(matchup-hi-surround)`, which highlights the block around the cursor until the cursor moves. */
  fun highlightSurrounding(editor: Editor) {
    editor.matchParenHighlights.isSurroundRequested = true
    update(editor)
  }

  private fun scheduleUpdate(editor: Editor) {
    if (disposable == null || editor.isDisposed) return
    val highlights = editor.getUserData(HIGHLIGHTS)
    if (!isEnabled || !VimPlugin.isEnabled() || editor.isIdeaVimDisabledHere) {
      highlights?.clear()
      return
    }

    val settings = DeferredSettings.read(editor)
    if (settings == null) {
      // A command can move the cursor many times, and only where it ends up matters.
      val highlightsToUpdate = editor.matchParenHighlights
      if (highlightsToUpdate.isUpdatePending) return
      highlightsToUpdate.isUpdatePending = true
      ApplicationManager.getApplication().invokeLater({
        highlightsToUpdate.isUpdatePending = false
        update(editor)
      }, ModalityState.any())
      return
    }
    val alarm = alarm ?: return
    val highlightsToHide = editor.matchParenHighlights
    alarm.cancelRequest(highlightsToHide.showRequest)
    alarm.addRequest(highlightsToHide.showRequest, settings.showDelay)
    if (highlightsToHide.isShowing && !highlightsToHide.isHideScheduled) {
      highlightsToHide.isHideScheduled = true
      alarm.addRequest(highlightsToHide.hideRequest, settings.hideDelay)
    }
  }

  private fun update(editor: Editor) {
    if (disposable == null || editor.isDisposed) return
    val highlights = editor.matchParenHighlights
    alarm?.cancelRequest(highlights.hideRequest)
    highlights.isHideScheduled = false

    val mode = editor.vim.mode
    // An operator is waiting for its motion, which is about to move the cursor anyway.
    if (mode is Mode.OP_PENDING) return
    if (!isEnabled || !VimPlugin.isEnabled() || editor.isIdeaVimDisabledHere || isDisabledInBuffer(editor) ||
      isDisabledInMode(editor, mode)
    ) {
      highlights.clear()
      return
    }

    val request = MatchParenRequest(
      caretOffset = editor.caretModel.primaryCaret.offset,
      isInsert = mode == Mode.INSERT || mode == Mode.REPLACE,
      wordPatterns = wordBlockPatterns(editor),
      surroundPatterns = blockPatterns(editor),
      isSurround = highlights.isSurroundRequested ||
        getVariable(editor, Scope.GLOBAL_VARIABLE, "matchup_matchparen_hi_surround_always")?.asInt() == 1,
      isSingleton = getVariable(editor, Scope.GLOBAL_VARIABLE, "matchup_matchparen_singleton")?.asInt() == 1,
      searchLines = getVariable(editor, Scope.GLOBAL_VARIABLE, "matchup_matchparen_stopline")?.asInt()
        ?: DEFAULT_STOPLINE,
      surroundSearchLines = 2 * max(1, editor.scrollingModel.visibleArea.height / editor.lineHeight),
      modificationStamp = editor.document.modificationStamp,
    )
    val showsOffscreen = showsOffscreen(editor)
    val generation = highlights.generation

    var action = ReadAction.nonBlocking<List<Delimiter>> { request.findDelimitersInBackground(editor) }
      .expireWhen { editor.isDisposed }
      .coalesceBy(highlights)
      .finishOnUiThread(ModalityState.any()) { delimiters ->
        if (highlights.generation != generation) return@finishOnUiThread
        if (editor.document.modificationStamp != request.modificationStamp) return@finishOnUiThread
        if (editor.caretModel.primaryCaret.offset != request.caretOffset) return@finishOnUiThread
        highlights.show(delimiters, request.caretOffset, showsOffscreen)
      }
    disposable?.let { action = action.expireWith(it) }
    editor.project?.let { action = action.withDocumentsCommitted(it) }
    action.submit(AppExecutorUtil.getAppExecutorService())
  }

  private fun clearAll() {
    EditorFactory.getInstance().allEditors.forEach { it.getUserData(HIGHLIGHTS)?.clear() }
  }

  private fun isDisabledInBuffer(editor: Editor): Boolean {
    return getVariable(editor, Scope.BUFFER_VARIABLE, "matchup_matchparen_enabled")?.asInt() == 0
  }

  /** `g:matchup_matchparen_nomode`, a string of the `mode()` letters of the modes without highlighting. */
  private fun isDisabledInMode(editor: Editor, mode: Mode): Boolean {
    val noMode = getVariable(editor, Scope.GLOBAL_VARIABLE, "matchup_matchparen_nomode")?.asString() ?: return false
    val modeChar = when (mode) {
      is Mode.NORMAL -> 'n'
      Mode.INSERT -> 'i'
      Mode.REPLACE -> 'R'
      is Mode.VISUAL -> mode.selectionType.modeChar('v', 'V', '\u0016')
      is Mode.SELECT -> mode.selectionType.modeChar('s', 'S', '\u0013')
      is Mode.CMD_LINE -> 'c'
      is Mode.OP_PENDING -> return false
    }
    return modeChar in noMode
  }

  private fun SelectionType.modeChar(characterwise: Char, linewise: Char, blockwise: Char): Char = when (this) {
    SelectionType.CHARACTER_WISE -> characterwise
    SelectionType.LINE_WISE -> linewise
    SelectionType.BLOCK_WISE -> blockwise
  }

  /**
   * `g:matchup_matchparen_offscreen`. There is no status line to show the match in, so the `status` method shows the
   * same hint with the match's line as `popup`. An empty dictionary turns it off, and so does `status_manual`, which
   * only gives the text to the user's own status line.
   */
  private fun showsOffscreen(editor: Editor): Boolean {
    val offscreen = getVariable(editor, Scope.GLOBAL_VARIABLE, "matchup_matchparen_offscreen") ?: return true
    val dictionary = (offscreen as? VimDictionary)?.dictionary ?: return false
    val method = dictionary[VimString("method")]?.asString() ?: return false
    return method == "status" || method == "popup"
  }

  /** `g:matchup_matchparen_deferred`, with the delays in milliseconds, or null when highlighting isn't deferred. */
  private class DeferredSettings(val showDelay: Int, val hideDelay: Int) {
    companion object {
      fun read(editor: Editor): DeferredSettings? {
        if (getVariable(editor, Scope.GLOBAL_VARIABLE, "matchup_matchparen_deferred")?.asInt() != 1) return null
        return DeferredSettings(
          getVariable(editor, Scope.GLOBAL_VARIABLE, "matchup_matchparen_deferred_show_delay")?.asInt() ?: 50,
          getVariable(editor, Scope.GLOBAL_VARIABLE, "matchup_matchparen_deferred_hide_delay")?.asInt() ?: 700,
        )
      }
    }
  }

  private val MatchParenHighlights.showRequest: Runnable
    get() = showRunnable ?: Runnable { update(editor) }.also { showRunnable = it }

  private val MatchParenHighlights.hideRequest: Runnable
    get() = hideRunnable ?: Runnable {
      isHideScheduled = false
      clear()
    }.also { hideRunnable = it }

  companion object {
    const val DEFAULT_STOPLINE = 400
  }
}

/**
 * What to search for, captured on the EDT so that the background search doesn't read Vim state.
 */
internal class MatchParenRequest(
  val caretOffset: Int,
  val isInsert: Boolean,
  val wordPatterns: List<BlockPatterns>,
  val surroundPatterns: List<BlockPatterns>,
  val isSurround: Boolean,
  val isSingleton: Boolean,
  val searchLines: Int,
  val surroundSearchLines: Int,
  val modificationStamp: Long,
) {
  /**
   * The editor can be closed while the search runs, and then reading its positions throws. Nothing is found then, and
   * the read action expires before the result reaches the editor. A thrown [ProcessCanceledException] would fail the
   * read action instead, because its progress isn't cancelled.
   */
  fun findDelimitersInBackground(editor: Editor): List<Delimiter> = try {
    findDelimiters(editor)
  } catch (e: RuntimeException) {
    if (e is ProcessCanceledException || !editor.isDisposed) throw e
    emptyList()
  }

  /**
   * The delimiters to highlight. In Insert mode the cursor is between two characters, so the word that ends right
   * before it counts as well.
   */
  private fun findDelimiters(editor: Editor): List<Delimiter> {
    val textLength = editor.document.textLength
    if (textLength == 0) return emptyList()
    val offsets = listOfNotNull(
      caretOffset.takeIf { it < textLength },
      (caretOffset - 1).takeIf { isInsert && it >= 0 },
    )
    for (offset in offsets) {
      val (delimiter, block) = findDelimiterAt(editor, offset, wordPatterns, searchLines) ?: continue
      if (block != null) return block.delimiters
      if (isSingleton) return listOf(delimiter)
    }
    if (!isSurround || offsets.isEmpty()) return emptyList()
    return findSurroundingBlocks(editor, offsets.first(), surroundSearchLines, surroundPatterns).firstOrNull()?.delimiters
      ?: emptyList()
  }
}

/**
 * Owns the matchparen highlighters of a single editor, and the hint that shows a match that is off screen. Nothing
 * else adds or removes them.
 */
internal class MatchParenHighlights(val editor: Editor) {
  private val highlighters = mutableListOf<RangeHighlighter>()
  private var hint: LightweightHint? = null
  private var shown: ShownDelimiters? = null

  /** Changes whenever the highlights are cleared, so that a search started before isn't shown after. */
  var generation = 0
    private set

  var isSurroundRequested = false
  var isUpdatePending = false
  var isHideScheduled = false
  var showRunnable: Runnable? = null
  var hideRunnable: Runnable? = null

  val isShowing: Boolean
    get() = highlighters.isNotEmpty()

  /** The highlighted ranges, ordered by offset. */
  val ranges: List<TextRange>
    get() = highlighters.filter { it.isValid }.map { it.textRange }

  /** The line shown in the hint for a match that is off screen, or null when there is none. */
  var offscreenLine: Int? = null
    private set

  fun show(delimiters: List<Delimiter>, caretOffset: Int, showsOffscreen: Boolean) {
    val newRanges = delimiters.map { TextRange(it.startOffset, it.endOffset) }
    if (newRanges != ranges) {
      removeHighlighters()
      newRanges.forEach { range ->
        highlighters += editor.markupModel.addRangeHighlighter(
          CodeInsightColors.MATCHED_BRACE_ATTRIBUTES,
          range.startOffset,
          range.endOffset,
          HighlighterLayer.SELECTION - 1,
          HighlighterTargetArea.EXACT_RANGE,
        )
      }
    }
    shown = ShownDelimiters(if (showsOffscreen) delimiters else emptyList(), caretOffset)
    refreshOffscreen()
  }

  /** The view scrolled, which can bring the match on or off screen. */
  fun refreshOffscreen() {
    val shown = shown ?: return
    showOffscreen(shown.delimiters, shown.caretOffset)
  }

  fun clear() {
    generation++
    shown = null
    removeHighlighters()
    hideHint()
    offscreenLine = null
  }

  /**
   * When the cursor is on a delimiter whose match is off screen, shows the match's line. If matches are off screen on
   * both sides, the one below wins.
   */
  private fun showOffscreen(delimiters: List<Delimiter>, caretOffset: Int) {
    val visibleArea = editor.scrollingModel.visibleArea
    val offscreen = delimiters.takeIf { visibleArea.height > 0 }.orEmpty()
      .filterNot { caretOffset in it.startOffset until it.endOffset }
      .filter { delimiter ->
        val y = editor.offsetToXY(delimiter.startOffset).y
        y < visibleArea.y || y + editor.lineHeight > visibleArea.y + visibleArea.height
      }
      .maxByOrNull { it.startOffset }
    val line = offscreen?.let { editor.document.getLineNumber(it.startOffset) }
    if (line == offscreenLine && (line == null || hint?.isVisible == true)) return

    hideHint()
    offscreenLine = line
    if (line == null || !editor.component.isShowing) return
    val document = editor.document
    val range = TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line))
    hint = EditorFragmentComponent.showEditorFragmentHint(editor, range, true, false)
  }

  private fun hideHint() {
    hint?.hide()
    hint = null
  }

  private fun removeHighlighters() {
    highlighters.forEach { editor.markupModel.removeHighlighter(it) }
    highlighters.clear()
  }
}

private class ShownDelimiters(val delimiters: List<Delimiter>, val caretOffset: Int)

private val HIGHLIGHTS = Key.create<MatchParenHighlights>("ideavim.matchup.matchParenHighlights")

internal val Editor.matchParenHighlights: MatchParenHighlights
  get() = getUserData(HIGHLIGHTS) ?: MatchParenHighlights(this).also { putUserData(HIGHLIGHTS, it) }

internal class HighlightSurroundingHandler(private val matchParen: MatchParen) : ExtensionHandler {
  override val isRepeatable: Boolean
    get() = false

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    matchParen.highlightSurrounding(editor.ij)
  }
}

internal class MatchParenCommandHandler(
  private val matchParen: MatchParen,
  private val isEnabled: Boolean,
) : CommandAliasHandler {
  override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
    matchParen.isEnabled = isEnabled
  }
}

private fun getVariable(editor: Editor?, scope: Scope, name: String): VimDataType? {
  val vimEditor = editor?.vim ?: injector.fallbackWindow
  return injector.variableService.getNullableVariableValue(
    VariableExpression(scope, name),
    vimEditor,
    injector.executionContextManager.getEditorExecutionContext(vimEditor),
    VimPluginContext,
  )
}

private fun VimDataType.asInt(): Int? = try {
  toVimNumber().value
} catch (_: Exception) {
  null
}

private fun VimDataType.asString(): String? = try {
  toVimString().value
} catch (_: Exception) {
  null
}
