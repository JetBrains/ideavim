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
import java.io.Closeable

/**
 * Base class for listener suppressors.
 * Children of this class have an ability to suppress editor listeners
 *
 * E.g.
 * ```
 *  CaretVimListenerSuppressor.lock()
 *  caret.moveToOffset(10) // vim's caret listener will not be executed
 *  CaretVimListenerSuppressor.unlock()
 * ````
 *
 *  Locks can be nested:
 * ```
 * CaretVimListenerSuppressor.lock()
 * moveCaret(caret) // vim's caret listener will not be executed
 * CaretVimListenerSuppressor.unlock()
 *
 * fun moveCaret(caret: Caret) {
 *     CaretVimListenerSuppressor.lock()
 *     caret.moveToOffset(10)
 *     CaretVimListenerSuppressor.unlock()
 * }
 * ```
 *
 * [Locked] implements [Closeable], so you can use try-with-resources block
 *
 * java
 * ```
 * try (VimListenerSuppressor.Locked ignored = SelectionVimListenerSuppressor.INSTANCE.lock()) {
 *     ....
 * }
 * ```
 *
 * Kotlin
 * ```
 * SelectionVimListenerSuppressor.lock().use { ... }
 * ```
 */
sealed class VimListenerSuppressor {
  private var caretListenerSuppressor = 0

  fun lock(): Locked {
    LOG.trace("Suppressor lock")
    LOG.trace { injector.application.currentStackTrace() }
    caretListenerSuppressor++
    return Locked()
  }

  // Please try not to use lock/unlock without scoping
  // Prefer try-with-resources
  fun unlock() {
    LOG.trace("Suppressor unlock")
    LOG.trace { injector.application.currentStackTrace() }
    caretListenerSuppressor--
  }

  val isNotLocked: Boolean
    get() = caretListenerSuppressor == 0

  inner class Locked : Closeable {
    override fun close(): Unit = unlock()
  }

  companion object {
    private val LOG = vimLogger<VimListenerSuppressor>()
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
