/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

/**
 * Container for key mappings for some mode
 * Iterable by "from" keys
 *
 * @author vlan
 */
class KeyMapping(private val mode: MappingMode) : Iterable<List<KeyStroke>>, KeyMappingLayer {
  private val keysTrie = KeyStrokeTrie<MappingInfo>(mode.name)

  override fun iterator(): Iterator<List<KeyStroke>> = ArrayList(keysTrie.getAll().keys).iterator()

  operator fun get(keys: List<KeyStroke>): MappingInfo? {
    keysTrie.getData(keys)?.let { return it }

    // Mapping a keystroke to an IDE action is a recursive mapping. The lhs is the keys, and the rhs is the
    // <Action>(...) key sequence. Like <Plug>, this needs to be mapped to a handler, but we have far too many IDE
    // actions to be able to pre-register them. We'll dynamically create the rhs mapping on demand.
    getActionNameFromActionMapping(keys)?.let {
      return ToActionMappingInfo(it, keys, false, MappingOwner.IdeaVim.System, enumSetOf(mode))
    }

    return null
  }

  @Deprecated("Use get(List<KeyStroke>)")
  operator fun get(keys: Iterable<KeyStroke>): MappingInfo? {
    if (keys is List<KeyStroke>) {
      return get(keys)
    }
    return get(keys.toList())
  }

  private fun getActionNameFromActionMapping(keys: List<KeyStroke>): String? {
    if (keys.size > 3
      && keys[0].keyCode == injector.parser.actionKeyStroke.keyCode
      && keys[1].keyChar == '(' && keys.last().keyChar == ')') {
      return buildString {
        for (i in 2 until keys.size - 1) {
          append(keys[i].keyChar)
        }
      }
    }
    return null
  }

  fun put(
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    originalModes: Set<MappingMode>,
    extensionHandler: ExtensionHandler,
    recursive: Boolean,
  ) {
    add(fromKeys, ToHandlerMappingInfo(extensionHandler, fromKeys, recursive, owner, originalModes))
  }

  fun put(
    fromKeys: List<KeyStroke>,
    toKeys: List<KeyStroke>,
    owner: MappingOwner,
    originalModes: Set<MappingMode>,
    recursive: Boolean,
  ) {
    add(fromKeys, ToKeysMappingInfo(toKeys, fromKeys, recursive, owner, originalModes))
  }

  fun put(
    fromKeys: List<KeyStroke>,
    toExpression: Expression,
    owner: MappingOwner,
    originalModes: Set<MappingMode>,
    originalString: String,
    recursive: Boolean,
  ) {
    add(fromKeys, ToExpressionMappingInfo(toExpression, fromKeys, recursive, owner, originalModes, originalString))
  }

  private fun add(keys: List<KeyStroke>, mappingInfo: MappingInfo) {
    keysTrie.add(keys, mappingInfo)
  }

  fun delete(owner: MappingOwner) {
    getByOwner(owner).forEach { (keys, _) ->
      keysTrie.remove(keys)
    }
  }

  fun delete(keys: List<KeyStroke>) {
    keysTrie.remove(keys)
  }

  fun delete() {
    keysTrie.clear()
  }

  fun getByOwner(owner: MappingOwner): List<Pair<List<KeyStroke>, MappingInfo>> =
    buildList {
      keysTrie.getAll().forEach { (keys, mappingInfo) ->
        if (mappingInfo.owner == owner) {
          add(Pair(keys, mappingInfo))
        }
      }
    }

  override fun isPrefix(keys: List<KeyStroke>): Boolean {
    if (keys.isEmpty()) return false

    if (keysTrie.isPrefix(keys)) return true

    val firstChar = keys.first().keyCode
    val lastChar = keys.last().keyChar
    return firstChar == injector.parser.actionKeyStroke.keyCode && lastChar != ')'
  }

  fun hasmapto(toKeys: List<KeyStroke>) = keysTrie.getAll().any { (_, mappingInfo) ->
    mappingInfo is ToKeysMappingInfo && mappingInfo.toKeys == toKeys
  }

  fun hasmapfrom(fromKeys: List<KeyStroke>) = keysTrie.getData(fromKeys) != null

  @TestOnly
  fun getMapTo(toKeys: List<KeyStroke?>) =
    keysTrie.getAll().filter { (_, mappingInfo) ->
      mappingInfo is ToKeysMappingInfo && mappingInfo.toKeys == toKeys
    }.map { it.toPair() }

  override fun getLayer(keys: List<KeyStroke>): MappingInfoLayer? = get(keys)
}
