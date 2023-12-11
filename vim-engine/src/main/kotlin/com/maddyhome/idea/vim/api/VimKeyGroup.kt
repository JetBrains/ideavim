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
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.key.KeyMapping
import com.maddyhome.idea.vim.key.KeyMappingLayer
import com.maddyhome.idea.vim.key.MappingInfo
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import javax.swing.KeyStroke

public interface VimKeyGroup {
  public fun getKeyRoot(mappingMode: MappingMode): CommandPartNode<LazyVimCommand>
  public fun getKeyMappingLayer(mode: MappingMode): KeyMappingLayer
  public fun getActions(editor: VimEditor, keyStroke: KeyStroke): List<NativeAction>
  public fun getKeymapConflicts(keyStroke: KeyStroke): List<NativeAction>

  public fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    extensionHandler: ExtensionHandler,
    recursive: Boolean,
  )

  public fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    toKeys: List<KeyStroke>,
    recursive: Boolean,
  )

  public fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    toExpr: Expression,
    originalString: String,
    recursive: Boolean,
  )

  public fun removeKeyMapping(owner: MappingOwner)
  public fun removeKeyMapping(modes: Set<MappingMode>)
  public fun removeKeyMapping(modes: Set<MappingMode>, keys: List<KeyStroke>)
  public fun showKeyMappings(modes: Set<MappingMode>, editor: VimEditor): Boolean
  public fun getKeyMapping(mode: MappingMode): KeyMapping
  public fun getKeyMappingByOwner(owner: MappingOwner): List<Pair<List<KeyStroke>, MappingInfo>>
  public fun updateShortcutKeysRegistration()
  public fun getMapTo(mode: MappingMode, toKeys: List<KeyStroke>): List<Pair<List<KeyStroke>, MappingInfo>>
  public fun unregisterCommandActions()
  public fun resetKeyMappings()
  public fun hasmapto(mode: MappingMode, toKeys: List<KeyStroke>): Boolean
  public fun hasmapfrom(mode: MappingMode, fromKeys: List<KeyStroke>): Boolean

  public val shortcutConflicts: MutableMap<KeyStroke, ShortcutOwnerInfo>
  public val savedShortcutConflicts: MutableMap<KeyStroke, ShortcutOwnerInfo>
}
