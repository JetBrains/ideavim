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
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.key.KeyMapping
import com.maddyhome.idea.vim.key.KeyMappingLayer
import com.maddyhome.idea.vim.key.MappingInfo
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.RequiredShortcut
import com.maddyhome.idea.vim.key.RootNode
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke
import kotlin.math.min

public abstract class VimKeyGroupBase : VimKeyGroup {
  @JvmField
  public val myShortcutConflicts: MutableMap<KeyStroke, ShortcutOwnerInfo> = LinkedHashMap()
  public val requiredShortcutKeys: MutableSet<RequiredShortcut> = HashSet(300)
  public val keyRoots: MutableMap<MappingMode, CommandPartNode<LazyVimCommand>> = EnumMap(MappingMode::class.java)
  public val keyMappings: MutableMap<MappingMode, KeyMapping> = EnumMap(MappingMode::class.java)

  override fun removeKeyMapping(modes: Set<MappingMode>, keys: List<KeyStroke>) {
    modes.map { getKeyMapping(it) }.forEach { it.delete(keys) }
  }

  override fun removeKeyMapping(modes: Set<MappingMode>) {
    modes.map { getKeyMapping(it) }.forEach { it.delete() }
  }

  override fun hasmapto(mode: MappingMode, toKeys: List<KeyStroke>): Boolean {
    return this.getKeyMapping(mode).hasmapto(toKeys)
  }

  override fun hasmapfrom(mode: MappingMode, fromKeys: List<KeyStroke>): Boolean {
    return this.getKeyMapping(mode).hasmapfrom(fromKeys)
  }

  override fun getKeyMapping(mode: MappingMode): KeyMapping {
    return keyMappings.getOrPut(mode) { KeyMapping() }
  }

  override fun resetKeyMappings() {
    keyMappings.clear()
  }

  /**
   * Returns the root of the key mapping for the given mapping mode
   *
   * @param mappingMode The mapping mode
   * @return The key mapping tree root
   */
  override fun getKeyRoot(mappingMode: MappingMode): CommandPartNode<LazyVimCommand> = keyRoots.getOrPut(mappingMode) { RootNode() }

  override fun getKeyMappingLayer(mode: MappingMode): KeyMappingLayer = getKeyMapping(mode)

  @Deprecated("Initialization EditorActionHandlerBase for this method breaks the point of lazy initialization")
  protected fun checkCommand(
    mappingModes: Set<MappingMode>,
    action: EditorActionHandlerBase,
    keys: List<KeyStroke>,
  ) {
    for (mappingMode in mappingModes) {
      checkIdentity(mappingMode, action.id, keys)
    }
    checkCorrectCombination(action, keys)
  }

  protected fun checkCommand(mappingModes: Set<MappingMode>, command: LazyVimCommand, keys: List<KeyStroke>) {
    for (mappingMode in mappingModes) {
      checkIdentity(mappingMode, command.actionId, keys)
    }
    checkCorrectCombination(command, keys)
  }

  private fun checkIdentity(mappingMode: MappingMode, actName: String, keys: List<KeyStroke>) {
    val keySets = identityChecker!!.getOrPut(mappingMode) { HashSet() }
    if (keys in keySets) {
      throw RuntimeException("This keymap already exists: $mappingMode keys: $keys action:$actName")
    }
    keySets.add(keys.toMutableList())
  }

  @Deprecated("Initialization EditorActionHandlerBase for this method breaks the point of lazy initialization")
  private fun checkCorrectCombination(action: EditorActionHandlerBase, keys: List<KeyStroke>) {
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

  private fun checkCorrectCombination(command: LazyVimCommand, keys: List<KeyStroke>) {
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

  override val savedShortcutConflicts: MutableMap<KeyStroke, ShortcutOwnerInfo>
    get() = myShortcutConflicts

  protected fun initIdentityChecker() {
    identityChecker = EnumMap(MappingMode::class.java)
    prefixes = HashMap()
  }

  private var identityChecker: MutableMap<MappingMode, MutableSet<MutableList<KeyStroke>>>? = null
  private var prefixes: MutableMap<MutableList<KeyStroke>, String>? = null

  override fun getKeyMappingByOwner(owner: MappingOwner): List<Pair<List<KeyStroke>, MappingInfo>> {
    return MappingMode.entries.map { getKeyMapping(it) }.flatMap { it.getByOwner(owner) }
  }

  private fun registerKeyMapping(fromKeys: List<KeyStroke>, owner: MappingOwner) {
    val oldSize = requiredShortcutKeys.size
    for (key in fromKeys) {
      if (key.keyChar == KeyEvent.CHAR_UNDEFINED) {
        if (
          !injector.globalOptions().octopushandler ||
          !(key.keyCode == KeyEvent.VK_ESCAPE && key.modifiers == 0) &&
          !(key.keyCode == KeyEvent.VK_ENTER && key.modifiers == 0)
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
    MappingMode.entries.map { getKeyMapping(it) }.forEach { it.delete(owner) }
    unregisterKeyMapping(owner)
  }

  override fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    toKeys: List<KeyStroke>,
    recursive: Boolean,
  ) {
    modes.map { getKeyMapping(it) }.forEach { it.put(fromKeys, toKeys, owner, recursive) }
    registerKeyMapping(fromKeys, owner)
  }

  override fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    toExpr: Expression,
    originalString: String,
    recursive: Boolean,
  ) {
    modes.map { getKeyMapping(it) }.forEach { it.put(fromKeys, toExpr, owner, originalString, recursive) }
    registerKeyMapping(fromKeys, owner)
  }

  override fun putKeyMapping(
    modes: Set<MappingMode>,
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    extensionHandler: ExtensionHandler,
    recursive: Boolean,
  ) {
    modes.map { getKeyMapping(it) }.forEach { it.put(fromKeys, owner, extensionHandler, recursive) }
    registerKeyMapping(fromKeys, owner)
  }

  override fun getMapTo(mode: MappingMode, toKeys: List<KeyStroke>): List<Pair<List<KeyStroke>, MappingInfo>> {
    return getKeyMapping(mode).getMapTo(toKeys)
  }

  override fun unregisterCommandActions() {
    requiredShortcutKeys.clear()
    keyRoots.clear()
    identityChecker?.clear()
    prefixes?.clear()
  }
}
