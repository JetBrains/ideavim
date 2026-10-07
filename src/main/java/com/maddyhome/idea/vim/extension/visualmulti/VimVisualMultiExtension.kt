/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.intellij.openapi.util.NlsSafe
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.multiplecursors.VimMultipleCursorsExtensionBase

@NlsSafe
private const val FIND_UNDER = "<Plug>(VM-Find-Under)"

@NlsSafe
private const val FIND_SUBWORD_UNDER = "<Plug>(VM-Find-Subword-Under)"

@NlsSafe
private const val SELECT_ALL = "<Plug>(VM-Select-All)"

@NlsSafe
private const val VISUAL_ALL = "<Plug>(VM-Visual-All)"

@NlsSafe
private const val SKIP_REGION = "<Plug>(VM-Skip-Region)"

@NlsSafe
private const val REMOVE_REGION = "<Plug>(VM-Remove-Region)"

/**
 * When set to `0`, only the `<C-n>` mappings are created, the same as `g:VM_default_mappings` in vim-visual-multi.
 */
@NlsSafe
private const val DEFAULT_MAPPINGS = "VM_default_mappings"

/**
 * The default `g:VM_leader` of vim-visual-multi
 */
@NlsSafe
private const val LEADER = "\\\\"

/**
 * Emulation of vim-visual-multi, built on the same native carets as [multiple-cursors][com.maddyhome.idea.vim.extension.multiplecursors.VimMultipleCursorsExtension].
 *
 * See https://github.com/mg979/vim-visual-multi
 */
internal class VimVisualMultiExtension : VimMultipleCursorsExtensionBase() {

  override fun getName() = "visual-multi"

  override fun init() {
    putPlugMapping(MappingMode.N, FIND_UNDER, NextOccurrenceHandler())
    putPlugMapping(MappingMode.X, FIND_SUBWORD_UNDER, NextOccurrenceHandler(whole = false))
    putPlugMapping(MappingMode.N, SELECT_ALL, AllOccurrencesHandler())
    putPlugMapping(MappingMode.X, VISUAL_ALL, AllOccurrencesHandler(whole = false))
    putPlugMapping(MappingMode.X, SKIP_REGION, SkipOccurrenceHandler())
    putPlugMapping(MappingMode.X, REMOVE_REGION, RemoveOccurrenceHandler())

    // vim-visual-multi always maps <C-n>, regardless of g:VM_default_mappings
    putDefaultMapping(MappingMode.N, "<C-n>", FIND_UNDER)
    putDefaultMapping(MappingMode.X, "<C-n>", FIND_SUBWORD_UNDER)

    if (isFlagEnabled(DEFAULT_MAPPINGS)) {
      putDefaultMapping(MappingMode.N, "${LEADER}A", SELECT_ALL)
      putDefaultMapping(MappingMode.X, "${LEADER}A", VISUAL_ALL)
    }
  }
}
