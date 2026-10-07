/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.util.NlsSafe
import com.maddyhome.idea.vim.command.MappingMode

@NlsSafe
private const val NEXT_WHOLE_OCCURRENCE = "<Plug>NextWholeOccurrence"

@NlsSafe
private const val NEXT_OCCURRENCE = "<Plug>NextOccurrence"

@NlsSafe
private const val SKIP_OCCURRENCE = "<Plug>SkipOccurrence"

@NlsSafe
private const val REMOVE_OCCURRENCE = "<Plug>RemoveOccurrence"

@NlsSafe
private const val ALL_WHOLE_OCCURRENCES = "<Plug>AllWholeOccurrences"

@NlsSafe
private const val ALL_OCCURRENCES = "<Plug>AllOccurrences"

/**
 * When set to `0`, the default key mappings are not created, allowing the user to map the `<Plug>` mappings to keys of
 * their own choosing (e.g. mapping back to the old `<A-n>` based mappings). This matches the
 * `g:multi_cursor_use_default_mapping` option of the original vim-multiple-cursors plugin.
 */
@NlsSafe
private const val USE_DEFAULT_MAPPING = "multi_cursor_use_default_mapping"

/**
 * Port of vim-multiple-cursors.
 *
 * See https://github.com/terryma/vim-multiple-cursors
 * */
internal class VimMultipleCursorsExtension : VimMultipleCursorsExtensionBase() {

  override fun getName() = "multiple-cursors"

  override fun init() {
    putPlugMapping(MappingMode.NXO, NEXT_WHOLE_OCCURRENCE, NextOccurrenceHandler())
    putPlugMapping(MappingMode.NXO, NEXT_OCCURRENCE, NextOccurrenceHandler(whole = false))
    putPlugMapping(MappingMode.NXO, ALL_WHOLE_OCCURRENCES, AllOccurrencesHandler())
    putPlugMapping(MappingMode.NXO, ALL_OCCURRENCES, AllOccurrencesHandler(whole = false))
    putPlugMapping(MappingMode.X, SKIP_OCCURRENCE, SkipOccurrenceHandler())
    putPlugMapping(MappingMode.X, REMOVE_OCCURRENCE, RemoveOccurrenceHandler())

    if (isFlagEnabled(USE_DEFAULT_MAPPING)) {
      putDefaultMapping(MappingMode.NXO, "<C-n>", NEXT_WHOLE_OCCURRENCE)
      putDefaultMapping(MappingMode.NXO, "g<C-n>", NEXT_OCCURRENCE)
      putDefaultMapping(MappingMode.NXO, "<A-n>", ALL_WHOLE_OCCURRENCES)
      putDefaultMapping(MappingMode.NXO, "g<A-n>", ALL_OCCURRENCES)
      putDefaultMapping(MappingMode.X, "<C-x>", SKIP_OCCURRENCE)
      putDefaultMapping(MappingMode.X, "<C-p>", REMOVE_OCCURRENCE)
    }
  }
}
