/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.yankring

import com.intellij.vim.api.models.TextType
import com.maddyhome.idea.vim.api.injector
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
  private const val MAX_HISTORY_VARIABLE: String = "yankring_max_history"
  private const val DEFAULT_MAX_HISTORY: Int = 100

  private val ring = VimRing<YankRingEntry>(
    // Read on every add rather than captured once, so that changing the variable mid-session takes
    // effect straight away - which is why VimRing takes a lambda in the first place.
    maxSize = { maxHistory() },
    keyOf = { it.text to it.type },
  )

  private fun maxHistory(): Int =
    injector.variableService.getGlobalVariableValue(MAX_HISTORY_VARIABLE)
      ?.toVimNumber()
      ?.value
      ?: DEFAULT_MAX_HISTORY

  fun record(text: String, type: TextType) {
    if (text.isEmpty()) return

    ring.add(YankRingEntry(text, type))
  }

  /**
   * Entries newest first, which is the order `:YRShow` lists them in.
   */
  fun entries(): List<YankRingEntry> = ring.getEntries().asReversed()

  /**
   * The index into [entries] that `<C-P>` / `<C-N>` should step away from after [pastedText] has
   * been pasted, so that they walk away from what is in the buffer rather than from the top of the
   * ring. Text pasted from a register the ring never saw starts at the newest entry.
   */
  fun cycleStartIndex(pastedText: String): Int =
    entries().indexOfFirst { it.text == pastedText }.coerceAtLeast(0)

  fun clear() = ring.clear()
}
