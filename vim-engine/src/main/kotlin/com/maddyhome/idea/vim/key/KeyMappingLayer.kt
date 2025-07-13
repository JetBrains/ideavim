/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.key.VimKeyStroke

interface KeyMappingLayer {
  fun isPrefix(keys: List<VimKeyStroke>): Boolean
  fun getLayer(keys: List<VimKeyStroke>): MappingInfoLayer?
}
