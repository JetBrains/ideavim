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
  /**
   * Maps a [from] key sequence to [to] in normal mode.
   */
  fun nmap(from: String, to: String)

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
   * Maps a [from] key sequence to [to] in visual mode.
   */
  fun vmap(from: String, to: String)

  /**
   * Removes a [keys] mapping in visual mode.
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
   * Maps a [from] key sequence to an [action] in normal mode.
   */
  fun nmap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a [from] key sequence to an [action] in visual mode.
   */
  fun vmap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in normal mode.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun nmap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps [keys] to an [action] with an [actionName] in visual mode.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun vmap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a [from] key sequence to [to] in all modes.
   */
  fun map(from: String, to: String)

  /**
   * Removes a [keys] mapping in all modes.
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
   * Maps a [from] key sequence to an [action] in all modes.
   */
  fun map(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in all modes.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun map(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a [from] key sequence to [to] in visual exclusive mode.
   */
  fun xmap(from: String, to: String)

  /**
   * Removes a [keys] mapping in visual exclusive mode.
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
   * Maps a [from] key sequence to an [action] in visual exclusive mode.
   */
  fun xmap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in visual exclusive mode.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun xmap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a [from] key sequence to [to] in select mode.
   */
  fun smap(from: String, to: String)

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
   * Maps a [from] key sequence to an [action] in select mode.
   */
  fun smap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in select mode.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun smap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a [from] key sequence to [to] in operator pending mode.
   */
  fun omap(from: String, to: String)

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
   * Maps a [from] key sequence to an [action] in operator pending mode.
   */
  fun omap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in operator pending mode.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun omap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a [from] key sequence to [to] in insert mode.
   */
  fun imap(from: String, to: String)

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
   * Maps a [from] key sequence to an [action] in insert mode.
   */
  fun imap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in insert mode.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun imap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a [from] key sequence to [to] in command line mode.
   */
  fun cmap(from: String, to: String)

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
   * Maps a [from] key sequence to an [action] in command line mode.
   */
  fun cmap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in command line mode.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun cmap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a [from] key sequence to [to] in normal mode non-recursively.
   */
  fun nnoremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in normal mode non-recursively.
   */
  fun nnoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in normal mode non-recursively.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun nnoremap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a [from] key sequence to [to] in visual mode non-recursively.
   */
  fun vnoremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in visual mode non-recursively.
   */
  fun vnoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in visual mode non-recursively.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun vnoremap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a [from] key sequence to [to] in all modes non-recursively.
   */
  fun noremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in all modes non-recursively.
   */
  fun noremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in all modes non-recursively.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun noremap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a [from] key sequence to [to] in visual exclusive mode non-recursively.
   */
  fun xnoremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in visual exclusive mode non-recursively.
   */
  fun xnoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in visual exclusive mode non-recursively.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun xnoremap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a [from] key sequence to [to] in select mode non-recursively.
   */
  fun snoremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in select mode non-recursively.
   */
  fun snoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in select mode non-recursively.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun snoremap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a [from] key sequence to [to] in operator pending mode non-recursively.
   */
  fun onoremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in operator pending mode non-recursively.
   */
  fun onoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in operator pending mode non-recursively.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun onoremap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a [from] key sequence to [to] in insert mode non-recursively.
   */
  fun inoremap(from: String, to: String)

  /**
   * Maps a [from] key sequence to an [action] in insert mode non-recursively.
   */
  fun inoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps [keys] to an [action] with an [actionName] in insert mode non-recursively.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun inoremap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )

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
   * Maps [keys] to an [action] with an [actionName] in command line mode non-recursively.
   *
   * [actionName] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [actionName] and from [actionName] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [actionName].
   */
  fun cnoremap(
    keys: String,
    actionName: String,
    action: suspend VimApi.() -> Unit,
  )
}
