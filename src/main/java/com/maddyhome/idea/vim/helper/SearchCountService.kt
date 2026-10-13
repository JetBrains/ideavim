/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimOutputPanel
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.SearchCountService.Companion.PENDING_TIMEOUT
import com.maddyhome.idea.vim.newapi.IjVimEditorSnapshot
import com.maddyhome.idea.vim.newapi.vim
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.annotations.ApiStatus
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shows the search count, e.g. `[3/120]`, in the output panel
 *
 * Counting means searching the whole range, which takes seconds for a rare pattern in a large file. The count is only
 * for display, so it's computed in the background on a snapshot of the editor, and shown when it's ready. If that takes
 * longer than [PENDING_TIMEOUT], `[?/??]` is shown until then, the same as Vim does when it gives up counting.
 *
 * Only the latest count is shown. Starting a new one cancels the previous one.
 */
@ApiStatus.Internal
@Service(Service.Level.APP)
class SearchCountService(private val coroutineScope: CoroutineScope) {

  // Only replaced on the EDT, but read by tests waiting for the count
  @Volatile
  private var countJob: Job? = null

  /** True while the latest search count is still being computed, or hasn't been shown yet */
  fun isSearchCountActive(): Boolean = countJob?.isActive == true

  @RequiresEdt
  fun updateSearchCount(
    editor: Editor,
    pattern: String,
    searchStartLine: Int,
    searchEndLine: Int,
    ignoreCase: Boolean,
    matchOffset: Int,
  ) {
    countJob?.cancel()
    countJob = null
    val request = SearchCountRequest(
      pattern,
      searchStartLine,
      searchEndLine,
      ignoreCase,
      matchOffset,
      injector.globalOptions().maxsearchcount,
    )
    if (request.usesMarks) {
      countNow(editor, request)
    } else {
      countJob = countInBackground(editor, request)
    }
  }

  // Marks aren't part of the snapshot, so patterns with mark atoms are counted on the EDT. They are rare in a search
  private fun countNow(editor: Editor, request: SearchCountRequest) {
    val count = request.countIn(editor.vim) { false }
    openPanel(editor)?.statusText = count.text
  }

  private fun countInBackground(editor: Editor, request: SearchCountRequest): Job {
    val snapshot = IjVimEditorSnapshot.capture(editor)
    return coroutineScope.launch(Dispatchers.Default + modalityOf(editor)) {
      val counting = async {
        val job = coroutineContext.job
        request.countIn(snapshot) { !job.isActive }
      }
      val quickCount = withTimeoutOrNull(PENDING_TIMEOUT) { counting.await() }
      if (quickCount == null) {
        showInOpenPanel(editor, PENDING_TEXT)
      }
      showInOpenPanel(editor, (quickCount ?: counting.await()).text)
    }
  }

  // Without the editor's modality, the count for an editor in a dialog would only be shown after the dialog closes
  private fun modalityOf(editor: Editor): CoroutineContext =
    ModalityState.stateForComponent(editor.component).asContextElement()

  // Cancelling and showing both happen on the EDT, so a cancelled count is never shown
  private suspend fun showInOpenPanel(editor: Editor, text: String) {
    withContext(Dispatchers.EDT) { openPanel(editor)?.statusText = text }
  }

  /**
   * The output panel showing the search, e.g. `/foo`, if it's still open
   *
   * The count belongs to that message. If it has been closed by the time the count is ready, the count isn't needed.
   */
  private fun openPanel(editor: Editor): VimOutputPanel? =
    if (editor.isDisposed) null else injector.outputPanel.getCurrentOutputPanel()

  companion object {
    private val PENDING_TIMEOUT = 40.milliseconds
    private const val PENDING_TEXT = "[?/??]"

    fun getInstance(): SearchCountService = service()
  }
}

private class SearchCountRequest(
  private val pattern: String,
  private val startLine: Int,
  private val endLine: Int,
  private val ignoreCase: Boolean,
  private val matchOffset: Int,
  private val maxSearchCount: Int,
) {
  val usesMarks: Boolean
    get() = MARK_ATOM.containsMatchIn(pattern)

  fun countIn(editor: VimEditor, isCancelled: () -> Boolean): SearchCount {
    val matches = injector.searchHelper.findAll(
      editor,
      pattern,
      startLine,
      endLine,
      ignoreCase,
      maxMatchesToFind(),
      isCancelled,
    )
    return SearchCount(currentMatch(matches), matches.size, maxSearchCount)
  }

  // One more than the limit, to know that the limit was exceeded
  private fun maxMatchesToFind(): Int = if (maxSearchCount > 0) maxSearchCount + 1 else Int.MAX_VALUE

  private fun currentMatch(matches: List<TextRange>): Int =
    matches.count { it.startOffset <= matchOffset }.takeIf { it > 0 } ?: matches.size

  private companion object {
    val MARK_ATOM = Regex("""%[<>]?'""")
  }
}

private class SearchCount(private val current: Int, private val total: Int, private val maxSearchCount: Int) {
  val text: String
    get() = when {
      total == 0 -> ""
      !exceedsMaxSearchCount() -> "[$current/$total]"
      current > maxSearchCount -> "[>$maxSearchCount/>$maxSearchCount]"
      else -> "[$current/>$maxSearchCount]"
    }

  private fun exceedsMaxSearchCount(): Boolean = maxSearchCount > 0 && total > maxSearchCount
}
