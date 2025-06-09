/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

interface MappingScope {
  fun nmap(from: String, to: String)
  fun vmap(from: String, to: String)

  fun nmap(from: String, isRepeatable: Boolean = false, action: VimScope.() -> Unit)
  fun vmap(from: String, isRepeatable: Boolean = false, action: VimScope.() -> Unit)

  fun nmap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: VimScope.() -> Unit,
  )

  fun vmap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: VimScope.() -> Unit,
  )
}