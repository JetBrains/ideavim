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
   * Maps a key sequence in normal mode.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun nmap(from: String, to: String)

  /**
   * Removes a key sequence mapping in normal mode.
   *
   * @param keys The key sequence to unmap
   */
  fun nunmap(keys: String)

  /**
   * Maps a key sequence in visual mode.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun vmap(from: String, to: String)

  /**
   * Removes a key sequence mapping in visual mode.
   *
   * @param keys The key sequence to unmap
   */
  fun vunmap(keys: String)

  /**
   * Maps a key sequence in normal mode to an action.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun nmap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in visual mode to an action.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun vmap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in normal mode to an action with a label.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun nmap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in visual mode to an action with a label.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun vmap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in all modes.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun map(from: String, to: String)

  /**
   * Removes a key sequence mapping in all modes.
   *
   * @param keys The key sequence to unmap
   */
  fun unmap(keys: String)

  /**
   * Maps a key sequence in all modes to an action.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun map(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in all modes to an action with a label.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun map(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in visual exclusive mode.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun xmap(from: String, to: String)

  /**
   * Removes a key sequence mapping in visual exclusive mode.
   *
   * @param keys The key sequence to unmap
   */
  fun xunmap(keys: String)

  /**
   * Maps a key sequence in visual exclusive mode to an action.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun xmap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in visual exclusive mode to an action with a label.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun xmap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in select mode.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun smap(from: String, to: String)

  /**
   * Removes a key sequence mapping in select mode.
   *
   * @param keys The key sequence to unmap
   */
  fun sunmap(keys: String)

  /**
   * Maps a key sequence in select mode to an action.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun smap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in select mode to an action with a label.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun smap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in operator pending mode.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun omap(from: String, to: String)

  /**
   * Removes a key sequence mapping in operator pending mode.
   *
   * @param keys The key sequence to unmap
   */
  fun ounmap(keys: String)

  /**
   * Maps a key sequence in operator pending mode to an action.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun omap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in operator pending mode to an action with a label.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun omap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in insert mode.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun imap(from: String, to: String)

  /**
   * Removes a key sequence mapping in insert mode.
   *
   * @param keys The key sequence to unmap
   */
  fun iunmap(keys: String)

  /**
   * Maps a key sequence in insert mode to an action.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun imap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in insert mode to an action with a label.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun imap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in command line mode.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun cmap(from: String, to: String)

  /**
   * Removes a key sequence mapping in command line mode.
   *
   * @param keys The key sequence to unmap
   */
  fun cunmap(keys: String)

  /**
   * Maps a key sequence in command line mode to an action.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun cmap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in command line mode to an action with a label.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun cmap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in normal mode non-recursively.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun nnoremap(from: String, to: String)

  /**
   * Maps a key sequence in normal mode to an action non-recursively.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun nnoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in normal mode to an action with a label non-recursively.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun nnoremap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in visual mode non-recursively.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun vnoremap(from: String, to: String)

  /**
   * Maps a key sequence in visual mode to an action non-recursively.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun vnoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in visual mode to an action with a label non-recursively.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun vnoremap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in all modes non-recursively.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun noremap(from: String, to: String)

  /**
   * Maps a key sequence in all modes to an action non-recursively.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun noremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in all modes to an action with a label non-recursively.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun noremap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in visual exclusive mode non-recursively.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun xnoremap(from: String, to: String)

  /**
   * Maps a key sequence in visual exclusive mode to an action non-recursively.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun xnoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in visual exclusive mode to an action with a label non-recursively.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun xnoremap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in select mode non-recursively.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun snoremap(from: String, to: String)

  /**
   * Maps a key sequence in select mode to an action non-recursively.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun snoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in select mode to an action with a label non-recursively.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun snoremap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in operator pending mode non-recursively.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun onoremap(from: String, to: String)

  /**
   * Maps a key sequence in operator pending mode to an action non-recursively.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun onoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in operator pending mode to an action with a label non-recursively.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun onoremap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in insert mode non-recursively.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  fun inoremap(from: String, to: String)

  /**
   * Maps a key sequence in insert mode to an action non-recursively.
   *
   * @param from The key sequence to map from
   * @param action The action to execute when the key sequence is pressed
   */
  fun inoremap(from: String, action: suspend VimApi.() -> Unit)

  /**
   * Maps a key sequence in insert mode to an action with a label non-recursively.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun inoremap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )

  /**
   * Maps a key sequence in command line mode non-recursively.
   *
   * @param from The key sequence to map from
   * @param to The key sequence to map to
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
   * Maps a key sequence in command line mode to an action with a label non-recursively.
   *
   * [label] is needed to provide an intermediate mapping from the [keys] to [action].
   * Two mappings will be created: from [keys] to [label] and from [label] to [action].
   * In this way, the user will be able to rewrite the default mapping to the plugin by
   * providing a custom mapping to [label].
   *
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param action The action to execute when the key sequence is pressed
   */
  fun cnoremap(
    keys: String,
    label: String,
    action: suspend VimApi.() -> Unit,
  )
}
