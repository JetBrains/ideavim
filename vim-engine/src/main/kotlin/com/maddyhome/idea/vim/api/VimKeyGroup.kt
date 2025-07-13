/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.action.change.LazyVimCommand
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.key.KeyMapping
import com.maddyhome.idea.vim.key.KeyMappingLayer
import com.maddyhome.idea.vim.key.KeyStrokeTrie
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.key.VimKeyStroke

interface VimKeyGroup {
  @Suppress("DEPRECATION")
  @Deprecated("Use getBuiltinCommandTrie", ReplaceWith("getBuiltinCommandsTrie(mappingMode)"))
  fun getKeyRoot(mappingMode: MappingMode): com.maddyhome.idea.vim.key.CommandPartNode<LazyVimCommand>

  fun getBuiltinCommandsTrie(mappingMode: MappingMode): KeyStrokeTrie<LazyVimCommand>
  fun getKeyMappingLayer(mode: MappingMode): KeyMappingLayer
  fun getActions(editor: VimEditor, keyStroke: VimKeyStroke): List<NativeAction>
  fun getKeymapConflicts(keyStroke: VimKeyStroke): List<NativeAction>

  fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<VimKeyStroke>,
    owner: MappingOwner,
    extensionHandler: ExtensionHandler,
    recursive: Boolean,
  )

  fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<VimKeyStroke>,
    owner: MappingOwner,
    toKeys: List<VimKeyStroke>,
    recursive: Boolean,
  )

  fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<VimKeyStroke>,
    owner: MappingOwner,
    toExpr: Expression,
    originalString: String,
    recursive: Boolean,
  )

  fun removeKeyMapping(owner: MappingOwner)
  fun removeKeyMapping(modes: Set<MappingMode>)
  fun removeKeyMapping(modes: Set<MappingMode>, keys: List<VimKeyStroke>)
  fun showKeyMappings(modes: Set<MappingMode>, prefix: List<VimKeyStroke>, editor: VimEditor): Boolean
  fun getKeyMapping(mode: MappingMode): KeyMapping
  fun updateShortcutKeysRegistration()
  fun unregisterCommandActions()
  fun resetKeyMappings()

  /**
   * Returns true if there exists a mapping to the given left-hand side keystrokes
   *
   * Note that the Vim function `hasmapto()` can accept a set of modes, and checks if any mapping _contains_ the given
   * left-hand side mapping, rather than is a direct map. (It also handles abbreviations)
   */
  fun hasmapto(mode: MappingMode, toKeys: List<VimKeyStroke>): Boolean

  val shortcutConflicts: MutableMap<VimKeyStroke, ShortcutOwnerInfo>
  val savedShortcutConflicts: MutableMap<VimKeyStroke, ShortcutOwnerInfo>
}
