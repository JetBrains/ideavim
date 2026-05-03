/*
 * Copyright 2003-2026 The IdeaVim authors
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
import javax.swing.KeyStroke

interface VimKeyGroup {
  @Suppress("DEPRECATION")
  @Deprecated("Use getBuiltinCommandTrie", ReplaceWith("getBuiltinCommandsTrie(mappingMode)"))
  fun getKeyRoot(mappingMode: MappingMode): com.maddyhome.idea.vim.key.CommandPartNode<LazyVimCommand>

  fun getBuiltinCommandsTrie(mappingMode: MappingMode): KeyStrokeTrie<LazyVimCommand>
  fun getKeyMappingLayer(mode: MappingMode): KeyMappingLayer
  fun getActions(editor: VimEditor, keyStroke: KeyStroke): List<NativeAction>
  fun getKeymapConflicts(keyStroke: KeyStroke): List<NativeAction>

  fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    extensionHandler: ExtensionHandler,
    recursive: Boolean,
  )

  fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    toKeys: List<KeyStroke>,
    recursive: Boolean,
  )

  fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    toExpr: Expression,
    originalString: String,
    recursive: Boolean,
  )

  fun removeKeyMapping(owner: MappingOwner)
  fun removeKeyMapping(modes: Set<MappingMode>)
  fun removeKeyMapping(modes: Set<MappingMode>, keys: List<KeyStroke>)
  fun showKeyMappings(modes: Set<MappingMode>, prefix: List<KeyStroke>, editor: VimEditor): Boolean
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
  fun hasmapto(mode: MappingMode, toKeys: List<KeyStroke>): Boolean

  val shortcutConflicts: MutableMap<KeyStroke, ShortcutOwnerInfo>
  val savedShortcutConflicts: MutableMap<KeyStroke, ShortcutOwnerInfo>

  fun getChar(editor: VimEditor): Char?

  /** Registers a command action and its shortcut keys. */
  fun registerCommandAction(command: LazyVimCommand) {}

  /** Registers a shortcut that is handled directly by KeyHandler, rather than by an action. */
  fun registerShortcutWithoutAction(keyStroke: KeyStroke, owner: MappingOwner) {}
}

/**
 * Retrieve the first mapping that is an exact match for the given LHS keystrokes in the given modes
 *
 * This function is essentially equivalent to the Vim function `maparg()`.
 *
 * Typically, this function will be called with a single mode, in which case there will be a single result, or null.
 * However, for `maparg()` support, it can also be called with [MappingMode.NVO], which might have the same mapping in
 * all modes, or a different mapping in any one of the modes. This function, like `maparg()`, will return an arbitrary
 * (undocumented) mapping in this scenario.
 */
fun VimKeyGroup.getFirstMappingInfoMatch(name: List<KeyStroke>, mode: Set<MappingMode>) =
  mode.map { getKeyMapping(it) }.map { it[name] }.firstNotNullOfOrNull { it }

/**
 * Retrieve the first mapping that is a prefix for the given LHS keystrokes, or has those keystrokes as a prefix, in any
 * of the given modes.
 *
 * This function is essentially equivalent to the Vim function `mapcheck()`.
 *
 * There can be multiple mappings that match, but like `mapcheck()`, this function will return an arbitrary
 * (undocumented) mapping.
 */
fun VimKeyGroup.getFirstMappingInfoPrefix(name: List<KeyStroke>, mode: Set<MappingMode>) =
  mode.map { getKeyMapping(it) }.flatMap { it.getAll(name) }.map { it.mappingInfo }.firstNotNullOfOrNull { it }
