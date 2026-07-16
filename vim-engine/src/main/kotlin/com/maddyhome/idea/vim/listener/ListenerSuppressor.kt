/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger

/**
 * Base class for listener suppressors.
 * Children of this class have the ability to suppress editor listeners for the duration of a scoped block.
 *
 * The suppression is a re-entrant counter, but that counter is never exposed: the only way to acquire it is via
 * [lock], which takes a block and always releases the count in a `finally`. This makes it impossible to leak a
 * lock - there is no bare `lock()`/`unlock()` pair a caller could unbalance.
 *
 * E.g.
 * ```
 *  CaretVimListenerSuppressor.lock {
 *    caret.moveToOffset(10) // vim's caret listener will not be executed
 *  }
 * ```
 *
 * Locks can be nested - the listener is re-enabled only once the outermost block completes:
 * ```
 * CaretVimListenerSuppressor.lock {
 *   moveCaret(caret) // vim's caret listener will not be executed
 * }
 *
 * fun moveCaret(caret: Caret) {
 *   CaretVimListenerSuppressor.lock {
 *     caret.moveToOffset(10)
 *   }
 * }
 * ```
 */
sealed class VimListenerSuppressor {
  @PublishedApi
  internal var caretListenerSuppressor = 0

  /**
   * Suppress the listener for the duration of [block].
   */
  inline fun <T> lock(block: () -> T): T {
    acquire()
    try {
      return block()
    } finally {
      release()
    }
  }

  @PublishedApi
  internal fun acquire() {
    LOG.trace("Suppressor lock")
    LOG.trace { injector.application.currentStackTrace() }
    caretListenerSuppressor++
  }

  @PublishedApi
  internal fun release() {
    LOG.trace("Suppressor unlock")
    LOG.trace { injector.application.currentStackTrace() }
    caretListenerSuppressor--
  }

  val isNotLocked: Boolean
    get() = caretListenerSuppressor == 0

  @PublishedApi
  internal companion object {
    @PublishedApi
    internal val LOG = vimLogger<VimListenerSuppressor>()
  }
}

object SelectionVimListenerSuppressor : VimListenerSuppressor()

/**
 * Suppresses IdeaVim's per-caret `updateCaretsVisualAttributes` side-effect that normally
 * fires from `EditorCaretHandler.caretAdded`/`caretRemoved`. That handler is O(N) (walks all
 * carets); when IntelliJ's `setBlockSelection` adds/removes N carets it fires N events, giving
 * O(N²) per block-visual motion and a frozen EDT on large blocks.
 *
 * Lock around bulk caret-replacement operations, then run `updateCaretsVisualAttributes` once
 * at the end (see [com.maddyhome.idea.vim.group.visual.setVisualSelection]'s BLOCK_WISE branch).
 */
object CaretVisualAttributesListenerSuppressor : VimListenerSuppressor()
