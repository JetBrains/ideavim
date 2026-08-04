/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.yankring

import com.intellij.vim.api.models.TextType
import com.maddyhome.idea.vim.common.VimRing

/**
 * One entry in the ring: the text that was yanked, deleted or changed, plus how it was stored.
 *
 * The type has to travel with the text, because it decides how the entry pastes back. YankRing.vim
 * encodes it into the same string as the text only because vimscript has no product types.
 *
 * There is no element number here. YankRing's `Elem` column is a positional counter recomputed on
 * every display (`let disp_item_nr = 1 ... += 1` in `s:YRShow`), not a stable identity.
 */
internal data class YankRingEntry(val text: String, val type: TextType)

/**
 * The yank ring itself, owned by the extension.
 *
 * [VimRing] supplies the ordering, the deduplication, the size cap and the cursor used by
 * `<C-P>` / `<C-N>`. Entries are keyed on text and type rather than on the entry itself so that
 * deduplication means what a user expects: yanking the same word twice leaves one entry.
 */
internal object YankRing {
  private val ring = VimRing<YankRingEntry>(
    maxSize = { maxHistory },
    keyOf = { it.text to it.type },
  )

  /**
   * `g:yankring_max_history` in the original plugin. Not configurable yet - see the roadmap's
   * group D.
   */
  private const val maxHistory: Int = 100

  fun record(text: String, type: TextType) {
    if (text.isEmpty()) return

    ring.add(YankRingEntry(text, type))
  }

  /**
   * Entries newest first, which is the order `:YRShow` lists them in.
   */
  fun entries(): List<YankRingEntry> = ring.getEntries().asReversed()

  fun clear() = ring.clear()
}
