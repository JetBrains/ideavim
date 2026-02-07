/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.VimApi

/**
 * Scope that provides access to mappings.
 */
@VimApiDsl
interface MappingScope {
  // ===== Normal, Visual, Select, and Operator-pending modes (map/noremap/unmap) =====

  /**
   * Maps a [from] key sequence to [to] in normal, visual, select, and operator-pending modes.
   */
  fun map(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in normal, visual, select, and operator-pending modes.
   */
  fun map(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a [from] key sequence to [to] in normal, visual, select, and operator-pending modes non-recursively.
   */
  fun noremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in normal, visual, select, and operator-pending modes non-recursively.
   */
  fun noremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Removes a [keys] mapping in normal, visual, select, and operator-pending modes.
   *
   * The [keys] must fully match the 'from' keys of the original mapping.
   *
   * Example:
   * ```kotlin
   * map("abc", "def")       // Create mapping
   * unmap("a")              // × Does not unmap anything
   * unmap("abc")            // ✓ Properly unmaps the mapping
   * ```
   */
  fun unmap(keys: String)

  /**
   * Checks if any mapping exists that maps to [to] in normal, visual, select, and operator-pending modes.
   *
   * Returns true if there's a mapping whose right-hand side is [to] in any of the mentioned modes.
   *
   * Example:
   * ```kotlin
   * nmap("gr", "<Plug>MyAction")
   * hasmapto("<Plug>MyAction")  // Returns true - "gr" maps TO "<Plug>MyAction"
   * hasmapto("gr")               // Returns false - nothing maps TO "gr"
   * ```
   */
  fun hasmapto(to: String): Boolean

  // ===== Normal mode (nmap/nnoremap/nunmap) =====

  /**
   * Maps a [from] key sequence to [to] in normal mode.
   */
  fun nmap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in normal mode.
   */
  fun nmap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a [from] key sequence to [to] in normal mode non-recursively.
   */
  fun nnoremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in normal mode non-recursively.
   */
  fun nnoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Removes a [keys] mapping in normal mode.
   *
   * The [keys] must fully match the 'from' keys of the original mapping.
   *
   * Example:
   * ```kotlin
   * nmap("abc", "def")      // Create mapping
   * nunmap("a")             // × Does not unmap anything
   * nunmap("abc")           // ✓ Properly unmaps the mapping
   * ```
   */
  fun nunmap(keys: String)

  /**
   * Checks if any mapping exists that maps to [to] in normal mode.
   *
   * Returns true if there's a mapping whose right-hand side is [to].
   *
   * Example:
   * ```kotlin
   * nmap("gr", "<Plug>MyAction")
   * nhasmapto("<Plug>MyAction")  // Returns true - "gr" maps TO "<Plug>MyAction"
   * nhasmapto("gr")               // Returns false - nothing maps TO "gr"
   * ```
   */
  fun nhasmapto(to: String): Boolean

  // ===== Visual and select modes (vmap/vnoremap/vunmap) =====

  /**
   * Maps a [from] key sequence to [to] in visual and select modes.
   */
  fun vmap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in visual and select modes.
   */
  fun vmap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a [from] key sequence to [to] in visual and select modes non-recursively.
   */
  fun vnoremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in visual and select modes non-recursively.
   */
  fun vnoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Removes a [keys] mapping in visual and select modes.
   *
   * The [keys] must fully match the 'from' keys of the original mapping.
   *
   * Example:
   * ```kotlin
   * vmap("abc", "def")      // Create mapping
   * vunmap("a")             // × Does not unmap anything
   * vunmap("abc")           // ✓ Properly unmaps the mapping
   * ```
   */
  fun vunmap(keys: String)

  /**
   * Checks if any mapping exists that maps to [to] in visual and select modes.
   *
   * Returns true if there's a mapping whose right-hand side is [to] in any of the mentioned modes.
   *
   * Example:
   * ```kotlin
   * vmap("gr", "<Plug>MyAction")
   * vhasmapto("<Plug>MyAction")  // Returns true - "gr" maps TO "<Plug>MyAction"
   * vhasmapto("gr")               // Returns false - nothing maps TO "gr"
   * ```
   */
  fun vhasmapto(to: String): Boolean

  // ===== Visual mode (xmap/xnoremap/xunmap) =====

  /**
   * Maps a [from] key sequence to [to] in visual mode.
   */
  fun xmap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in visual mode.
   */
  fun xmap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a [from] key sequence to [to] in visual mode non-recursively.
   */
  fun xnoremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in visual mode non-recursively.
   */
  fun xnoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Removes a [keys] mapping in visual mode.
   *
   * The [keys] must fully match the 'from' keys of the original mapping.
   *
   * Example:
   * ```kotlin
   * xmap("abc", "def")      // Create mapping
   * xunmap("a")             // × Does not unmap anything
   * xunmap("abc")           // ✓ Properly unmaps the mapping
   * ```
   */
  fun xunmap(keys: String)

  /**
   * Checks if any mapping exists that maps to [to] in visual mode.
   *
   * Returns true if there's a mapping whose right-hand side is [to].
   *
   * Example:
   * ```kotlin
   * xmap("gr", "<Plug>MyAction")
   * xhasmapto("<Plug>MyAction")  // Returns true - "gr" maps TO "<Plug>MyAction"
   * xhasmapto("gr")               // Returns false - nothing maps TO "gr"
   * ```
   */
  fun xhasmapto(to: String): Boolean

  // ===== Select mode (smap/snoremap/sunmap) =====

  /**
   * Maps a [from] key sequence to [to] in select mode.
   */
  fun smap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in select mode.
   */
  fun smap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a [from] key sequence to [to] in select mode non-recursively.
   */
  fun snoremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in select mode non-recursively.
   */
  fun snoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Removes a [keys] mapping in select mode.
   *
   * The [keys] must fully match the 'from' keys of the original mapping.
   *
   * Example:
   * ```kotlin
   * smap("abc", "def")      // Create mapping
   * sunmap("a")             // × Does not unmap anything
   * sunmap("abc")           // ✓ Properly unmaps the mapping
   * ```
   */
  fun sunmap(keys: String)

  /**
   * Checks if any mapping exists that maps to [to] in select mode.
   *
   * Returns true if there's a mapping whose right-hand side is [to].
   *
   * Example:
   * ```kotlin
   * smap("gr", "<Plug>MyAction")
   * shasmapto("<Plug>MyAction")  // Returns true - "gr" maps TO "<Plug>MyAction"
   * shasmapto("gr")               // Returns false - nothing maps TO "gr"
   * ```
   */
  fun shasmapto(to: String): Boolean

  // ===== Operator pending mode (omap/onoremap/ounmap) =====

  /**
   * Maps a [from] key sequence to [to] in operator pending mode.
   */
  fun omap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in operator pending mode.
   */
  fun omap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a [from] key sequence to [to] in operator pending mode non-recursively.
   */
  fun onoremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in operator pending mode non-recursively.
   */
  fun onoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Removes a [keys] mapping in operator pending mode.
   *
   * The [keys] must fully match the 'from' keys of the original mapping.
   *
   * Example:
   * ```kotlin
   * omap("abc", "def")      // Create mapping
   * ounmap("a")             // × Does not unmap anything
   * ounmap("abc")           // ✓ Properly unmaps the mapping
   * ```
   */
  fun ounmap(keys: String)

  /**
   * Checks if any mapping exists that maps to [to] in operator pending mode.
   *
   * Returns true if there's a mapping whose right-hand side is [to].
   *
   * Example:
   * ```kotlin
   * omap("gr", "<Plug>MyAction")
   * ohasmapto("<Plug>MyAction")  // Returns true - "gr" maps TO "<Plug>MyAction"
   * ohasmapto("gr")               // Returns false - nothing maps TO "gr"
   * ```
   */
  fun ohasmapto(to: String): Boolean

  // ===== Insert mode (imap/inoremap/iunmap) =====

  /**
   * Maps a [from] key sequence to [to] in insert mode.
   */
  fun imap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in insert mode.
   */
  fun imap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a [from] key sequence to [to] in insert mode non-recursively.
   */
  fun inoremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in insert mode non-recursively.
   */
  fun inoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Removes a [keys] mapping in insert mode.
   *
   * The [keys] must fully match the 'from' keys of the original mapping.
   *
   * Example:
   * ```kotlin
   * imap("abc", "def")      // Create mapping
   * iunmap("a")             // × Does not unmap anything
   * iunmap("abc")           // ✓ Properly unmaps the mapping
   * ```
   */
  fun iunmap(keys: String)

  /**
   * Checks if any mapping exists that maps to [to] in insert mode.
   *
   * Returns true if there's a mapping whose right-hand side is [to].
   *
   * Example:
   * ```kotlin
   * imap("jk", "<Plug>MyAction")
   * ihasmapto("<Plug>MyAction")  // Returns true - "jk" maps TO "<Plug>MyAction"
   * ihasmapto("jk")               // Returns false - nothing maps TO "jk"
   * ```
   */
  fun ihasmapto(to: String): Boolean

  // ===== Command line mode (cmap/cnoremap/cunmap) =====

  /**
   * Maps a [from] key sequence to [to] in command line mode.
   */
  fun cmap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in command line mode.
   */
  fun cmap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a [from] key sequence to [to] in command line mode non-recursively.
   */
  fun cnoremap(from: String, to: String)

  /**
   * Maps a key sequence in command line mode to an action non-recursively.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun cnoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Removes a [keys] mapping in command line mode.
   *
   * The [keys] must fully match the 'from' keys of the original mapping.
   *
   * Example:
   * ```kotlin
   * cmap("abc", "def")      // Create mapping
   * cunmap("a")             // × Does not unmap anything
   * cunmap("abc")           // ✓ Properly unmaps the mapping
   * ```
   */
  fun cunmap(keys: String)

  /**
   * Checks if any mapping exists that maps to [to] in command line mode.
   *
   * Returns true if there's a mapping whose right-hand side is [to].
   *
   * Example:
   * ```kotlin
   * cmap("<C-a>", "<Plug>MyAction")
   * chasmapto("<Plug>MyAction")  // Returns true - "<C-a>" maps TO "<Plug>MyAction"
   * chasmapto("<C-a>")            // Returns false - nothing maps TO "<C-a>"
   * ```
   */
  fun chasmapto(to: String): Boolean
}
