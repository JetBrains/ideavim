/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

interface MappingScope {
  /**
   * Maps a key sequence in normal mode.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun nmap(from: String, to: String)

  /**
   * Removes a key sequence mapping in normal mode.
   * @param keys The key sequence to unmap
   */
  suspend fun nunmap(keys: String)

  /**
   * Maps a key sequence in visual mode.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun vmap(from: String, to: String)

  /**
   * Removes a key sequence mapping in visual mode.
   * @param keys The key sequence to unmap
   */
  suspend fun vunmap(keys: String)

  /**
   * Maps a key sequence in normal mode to an action.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun nmap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in visual mode to an action.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun vmap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in normal mode to an action with a label.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun nmap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in visual mode to an action with a label.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun vmap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in all modes.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun map(from: String, to: String)

  /**
   * Removes a key sequence mapping in all modes.
   * @param keys The key sequence to unmap
   */
  suspend fun unmap(keys: String)

  /**
   * Maps a key sequence in all modes to an action.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun map(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in all modes to an action with a label.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun map(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in visual exclusive mode.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun xmap(from: String, to: String)

  /**
   * Removes a key sequence mapping in visual exclusive mode.
   * @param keys The key sequence to unmap
   */
  suspend fun xunmap(keys: String)

  /**
   * Maps a key sequence in visual exclusive mode to an action.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun xmap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in visual exclusive mode to an action with a label.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun xmap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in select mode.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun smap(from: String, to: String)

  /**
   * Removes a key sequence mapping in select mode.
   * @param keys The key sequence to unmap
   */
  suspend fun sunmap(keys: String)

  /**
   * Maps a key sequence in select mode to an action.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun smap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in select mode to an action with a label.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun smap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in operator pending mode.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun omap(from: String, to: String)

  /**
   * Removes a key sequence mapping in operator pending mode.
   * @param keys The key sequence to unmap
   */
  suspend fun ounmap(keys: String)

  /**
   * Maps a key sequence in operator pending mode to an action.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun omap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in operator pending mode to an action with a label.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun omap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in insert mode.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun imap(from: String, to: String)

  /**
   * Removes a key sequence mapping in insert mode.
   * @param keys The key sequence to unmap
   */
  suspend fun iunmap(keys: String)

  /**
   * Maps a key sequence in insert mode to an action.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun imap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in insert mode to an action with a label.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun imap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in command line mode.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun cmap(from: String, to: String)

  /**
   * Removes a key sequence mapping in command line mode.
   * @param keys The key sequence to unmap
   */
  suspend fun cunmap(keys: String)

  /**
   * Maps a key sequence in command line mode to an action.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun cmap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in command line mode to an action with a label.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun cmap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in normal mode non-recursively.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun nnoremap(from: String, to: String)

  /**
   * Maps a key sequence in normal mode to an action non-recursively.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun nnoremap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in normal mode to an action with a label non-recursively.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun nnoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in visual mode non-recursively.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun vnoremap(from: String, to: String)

  /**
   * Maps a key sequence in visual mode to an action non-recursively.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun vnoremap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in visual mode to an action with a label non-recursively.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun vnoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in all modes non-recursively.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun noremap(from: String, to: String)

  /**
   * Maps a key sequence in all modes to an action non-recursively.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun noremap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in all modes to an action with a label non-recursively.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun noremap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in visual exclusive mode non-recursively.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun xnoremap(from: String, to: String)

  /**
   * Maps a key sequence in visual exclusive mode to an action non-recursively.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun xnoremap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in visual exclusive mode to an action with a label non-recursively.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun xnoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in select mode non-recursively.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun snoremap(from: String, to: String)

  /**
   * Maps a key sequence in select mode to an action non-recursively.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun snoremap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in select mode to an action with a label non-recursively.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun snoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in operator pending mode non-recursively.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun onoremap(from: String, to: String)

  /**
   * Maps a key sequence in operator pending mode to an action non-recursively.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun onoremap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in operator pending mode to an action with a label non-recursively.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun onoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in insert mode non-recursively.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun inoremap(from: String, to: String)

  /**
   * Maps a key sequence in insert mode to an action non-recursively.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun inoremap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in insert mode to an action with a label non-recursively.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun inoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )

  /**
   * Maps a key sequence in command line mode non-recursively.
   * @param from The key sequence to map from
   * @param to The key sequence to map to
   */
  suspend fun cnoremap(from: String, to: String)

  /**
   * Maps a key sequence in command line mode to an action non-recursively.
   * @param from The key sequence to map from
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun cnoremap(from: String, isRepeatable: Boolean = false, action: suspend VimScope.() -> Unit)

  /**
   * Maps a key sequence in command line mode to an action with a label non-recursively.
   * @param keys The key sequence to map from
   * @param label A label for the mapping
   * @param isRepeatable Whether the mapping is repeatable with the dot command
   * @param action The action to execute when the key sequence is pressed
   */
  suspend fun cnoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean = false,
    action: suspend VimScope.() -> Unit,
  )
}
