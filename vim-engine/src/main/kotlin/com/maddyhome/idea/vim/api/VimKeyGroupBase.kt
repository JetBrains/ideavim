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
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.key.KeyMapping
import com.maddyhome.idea.vim.key.KeyMappingLayer
import com.maddyhome.idea.vim.key.KeyStrokeTrie
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.RequiredShortcut
import com.maddyhome.idea.vim.key.RootNode
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import java.util.*
import com.maddyhome.idea.vim.key.VimKeyStroke
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.CHAR_UNDEFINED
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_ENTER
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_ESCAPE
import kotlin.math.min

abstract class VimKeyGroupBase : VimKeyGroup {
  @JvmField
  val myShortcutConflicts: MutableMap<VimKeyStroke, ShortcutOwnerInfo> = LinkedHashMap()
  val requiredShortcutKeys: MutableSet<RequiredShortcut> = HashSet(300)
  val builtinCommands: MutableMap<MappingMode, KeyStrokeTrie<LazyVimCommand>> = EnumMap(MappingMode::class.java)
  val keyMappings: MutableMap<MappingMode, KeyMapping> = EnumMap(MappingMode::class.java)

  override fun removeKeyMapping(modes: Set<MappingMode>, keys: List<VimKeyStroke>) {
    modes.map { getKeyMapping(it) }.forEach { it.removeKeyMapping(keys) }
  }

  override fun removeKeyMapping(modes: Set<MappingMode>) {
    modes.map { getKeyMapping(it) }.forEach { it.clear() }
  }

  override fun hasmapto(mode: MappingMode, toKeys: List<VimKeyStroke>): Boolean {
    return this.getKeyMapping(mode).hasmapto(toKeys)
  }

  override fun getKeyMapping(mode: MappingMode): KeyMapping {
    return keyMappings.getOrPut(mode) { KeyMapping(mode) }
  }

  override fun resetKeyMappings() {
    keyMappings.clear()
  }

  @Suppress("DEPRECATION")
  @Deprecated("Use getBuiltinCommandTrie", ReplaceWith("getBuiltinCommandsTrie(mappingMode)"))
  override fun getKeyRoot(mappingMode: MappingMode): com.maddyhome.idea.vim.key.CommandPartNode<LazyVimCommand> =
    RootNode(getBuiltinCommandsTrie(mappingMode))

  /**
   * Returns the root node of the builtin command keystroke trie
   *
   * @param mappingMode The mapping mode
   * @return The root node of the builtin command trie
   */
  override fun getBuiltinCommandsTrie(mappingMode: MappingMode): KeyStrokeTrie<LazyVimCommand> =
    builtinCommands.getOrPut(mappingMode) { KeyStrokeTrie<LazyVimCommand>(mappingMode.name[0].lowercase()) }

  override fun getKeyMappingLayer(mode: MappingMode): KeyMappingLayer = getKeyMapping(mode)

  @Deprecated("Initialization EditorActionHandlerBase for this method breaks the point of lazy initialization")
  protected fun checkCommand(
    mappingModes: Set<MappingMode>,
    action: EditorActionHandlerBase,
    keys: List<VimKeyStroke>,
  ) {
    for (mappingMode in mappingModes) {
      checkIdentity(mappingMode, action.id, keys)
    }
    @Suppress("DEPRECATION")
    checkCorrectCombination(action, keys)
  }

  protected fun checkCommand(mappingModes: Set<MappingMode>, command: LazyVimCommand, keys: List<VimKeyStroke>) {
    for (mappingMode in mappingModes) {
      checkIdentity(mappingMode, command.actionId, keys)
    }
    checkCorrectCombination(command, keys)
  }

  private fun checkIdentity(mappingMode: MappingMode, actName: String, keys: List<VimKeyStroke>) {
    val keySets = identityChecker!!.getOrPut(mappingMode) { HashSet() }
    if (keys in keySets) {
      throw RuntimeException("This keymap already exists: $mappingMode keys: $keys action:$actName")
    }
    keySets.add(keys.toMutableList())
  }

  @Deprecated("Initialization EditorActionHandlerBase for this method breaks the point of lazy initialization")
  private fun checkCorrectCombination(action: EditorActionHandlerBase, keys: List<VimKeyStroke>) {
    for (entry in prefixes!!.entries) {
      val prefix = entry.key
      if (prefix.size == keys.size) continue
      val shortOne = min(prefix.size, keys.size)
      var i = 0
      while (i < shortOne) {
        if (prefix[i] != keys[i]) break
        i++
      }

      val actionExceptions = listOf(
        "VimInsertDeletePreviousWordAction",
        "VimInsertAfterCursorAction",
        "VimInsertBeforeCursorAction",
        "VimFilterVisualLinesAction",
        "VimAutoIndentMotionAction",
      )
      if (i == shortOne && action.id !in actionExceptions && entry.value !in actionExceptions) {
        throw RuntimeException(
          "Prefix found! $keys in command ${action.id} is the same as ${prefix.joinToString(", ") { it.toString() }} in ${entry.value}",
        )
      }
    }
    prefixes!![keys.toMutableList()] = action.id
  }

  private fun checkCorrectCombination(command: LazyVimCommand, keys: List<VimKeyStroke>) {
    for (entry in prefixes!!.entries) {
      val prefix = entry.key
      if (prefix.size == keys.size) continue
      val shortOne = min(prefix.size, keys.size)
      var i = 0
      while (i < shortOne) {
        if (prefix[i] != keys[i]) break
        i++
      }

      val actionExceptions = listOf(
        "VimInsertDeletePreviousWordAction",
        "VimInsertAfterCursorAction",
        "VimInsertBeforeCursorAction",
        "VimFilterVisualLinesAction",
        "VimAutoIndentMotionAction",
      )
      if (i == shortOne && command.actionId !in actionExceptions && entry.value !in actionExceptions) {
        throw RuntimeException(
          "Prefix found! $keys in command ${command.actionId} is the same as ${prefix.joinToString(", ") { it.toString() }} in ${entry.value}",
        )
      }
    }
    prefixes!![keys.toMutableList()] = command.actionId
  }

  override val savedShortcutConflicts: MutableMap<VimKeyStroke, ShortcutOwnerInfo>
    get() = myShortcutConflicts

  protected fun initIdentityChecker() {
    identityChecker = EnumMap(MappingMode::class.java)
    prefixes = HashMap()
  }

  // Internal structures that used in tests to make sure shortcuts are initialized properly and,
  //  for example, we didn't make two similar shortcuts for two different actions
  //  These structures are not initialized during production
  private var identityChecker: MutableMap<MappingMode, MutableSet<MutableList<VimKeyStroke>>>? = null
  private var prefixes: MutableMap<MutableList<VimKeyStroke>, String>? = null

  private fun registerKeyMapping(fromKeys: List<VimKeyStroke>, owner: MappingOwner) {
    val oldSize = requiredShortcutKeys.size
    for (key in fromKeys) {
      if (key.keyChar == CHAR_UNDEFINED) {
        if (
          !injector.application.isOctopusEnabled() ||
          !(key.keyCode == VK_ESCAPE && key.modifiers == 0) &&
          !(key.keyCode == VK_ENTER && key.modifiers == 0)
        ) {
          requiredShortcutKeys.add(RequiredShortcut(key, owner))
        }
      }
    }
    if (requiredShortcutKeys.size != oldSize) {
      updateShortcutKeysRegistration()
    }
  }

  private fun unregisterKeyMapping(owner: MappingOwner) {
    val oldSize = requiredShortcutKeys.size
    requiredShortcutKeys.removeIf { it.owner == owner }
    if (requiredShortcutKeys.size != oldSize) {
      updateShortcutKeysRegistration()
    }
  }

  override fun removeKeyMapping(owner: MappingOwner) {
    MappingMode.entries.map { getKeyMapping(it) }.forEach { it.removeKeyMappingsByOwner(owner) }
    unregisterKeyMapping(owner)
  }

  override fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<VimKeyStroke>,
    owner: MappingOwner,
    toKeys: List<VimKeyStroke>,
    recursive: Boolean,
  ) {
    modes.map { getKeyMapping(it) }.forEach { it.put(fromKeys, toKeys, owner, modes, recursive) }
    registerKeyMapping(fromKeys, owner)
  }

  override fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<VimKeyStroke>,
    owner: MappingOwner,
    toExpr: Expression,
    originalString: String,
    recursive: Boolean,
  ) {
    modes.map { getKeyMapping(it) }.forEach { it.put(fromKeys, toExpr, owner, modes, originalString, recursive) }
    registerKeyMapping(fromKeys, owner)
  }

  override fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<VimKeyStroke>,
    owner: MappingOwner,
    extensionHandler: ExtensionHandler,
    recursive: Boolean,
  ) {
    modes.map { getKeyMapping(it) }.forEach { it.put(fromKeys, owner, modes, extensionHandler, recursive) }
    registerKeyMapping(fromKeys, owner)
  }

  override fun unregisterCommandActions() {
    builtinCommands.clear()
  }
}
