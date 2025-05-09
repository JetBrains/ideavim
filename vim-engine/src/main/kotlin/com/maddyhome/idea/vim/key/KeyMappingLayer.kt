/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import javax.swing.KeyStroke

interface KeyMappingLayer {
  fun isPrefix(keys: List<KeyStroke>): Boolean
  fun getLayer(keys: List<KeyStroke>): MappingInfoLayer?
}
