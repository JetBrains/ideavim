/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

interface KeyMappingLayer {
  fun isPrefix(keys: List<VimKeyStroke>): Boolean
  fun getLayer(keys: List<VimKeyStroke>): MappingInfoLayer?
}

// TODO: Migrate MappingState.keys to a List<KeyStroke> so that we can avoid creating wrapper lists and these helpers
// We should use the same type for looking up key strokes in the trie as we at storing/exposing the current state.
// Be careful when migrating - MappingState.keys is used externally, so would be expecting an Iterable<KeyStroke>
@Deprecated("Use getLayer(List<KeyStroke>")
internal fun KeyMappingLayer.getLayer(keys: Iterable<VimKeyStroke>): MappingInfoLayer? =
  getLayer(keys as? List<VimKeyStroke> ?: keys.toList())

@Deprecated("Use getLayer(List<KeyStroke>")
internal fun KeyMappingLayer.isPrefix(keys: Iterable<VimKeyStroke>): Boolean =
  isPrefix(keys as? List<VimKeyStroke> ?: keys.toList())
