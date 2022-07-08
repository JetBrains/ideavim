/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import java.util.function.Consumer
import java.util.stream.Collectors
import javax.swing.KeyStroke

/**
 * Container for key mappings for some mode
 * Iterable by "from" keys
 *
 * @author vlan
 */
class KeyMapping : Iterable<List<KeyStroke?>?>, KeyMappingLayer {
  /**
   * Contains all key mapping for some mode.
   */
  private val myKeys: MutableMap<List<KeyStroke>, MappingInfo> = HashMap()

  /**
   * Set the contains all possible prefixes for mappings.
   * E.g. if there is mapping for "hello", this set will contain "h", "he", "hel", etc.
   * Multiset is used to correctly remove the mappings.
   */
  private val myPrefixes: MutableMap<List<KeyStroke>, Int> = HashMap()
  override fun iterator(): MutableIterator<List<KeyStroke>> {
    return ArrayList(myKeys.keys).iterator()
  }

  operator fun get(keys: Iterable<KeyStroke>): MappingInfo? {
    // Having a parameter of Iterable allows for a nicer API, because we know when a given list is immutable.
    // TODO: Should we change this to be a trie?
    assert(keys is List<*>) { "keys must be of type List<KeyStroke>" }
    val keyStrokes = keys as List<KeyStroke>
    val mappingInfo = myKeys[keys]
    if (mappingInfo != null) return mappingInfo
    if (keyStrokes.size > 3) {
      if (keyStrokes[0].keyCode == injector.parser.actionKeyStroke.keyCode && keyStrokes[1].keyChar == '(' && keyStrokes[keyStrokes.size - 1].keyChar == ')') {
        val builder = StringBuilder()
        for (i in 2 until keyStrokes.size - 1) {
          builder.append(keyStrokes[i].keyChar)
        }
        return ToActionMappingInfo(builder.toString(), keyStrokes, false, MappingOwner.IdeaVim.System)
      }
    }
    return null
  }

  fun put(
    fromKeys: List<KeyStroke>,
    owner: MappingOwner,
    extensionHandler: ExtensionHandler,
    recursive: Boolean
  ) {
    myKeys[ArrayList(fromKeys)] = ToHandlerMappingInfo(extensionHandler, fromKeys, recursive, owner)
    fillPrefixes(fromKeys)
  }

  fun put(
    fromKeys: List<KeyStroke>,
    toKeys: List<KeyStroke>,
    owner: MappingOwner,
    recursive: Boolean
  ) {
    myKeys[ArrayList(fromKeys)] = ToKeysMappingInfo(toKeys, fromKeys, recursive, owner)
    fillPrefixes(fromKeys)
  }

  fun put(
    fromKeys: List<KeyStroke>,
    toExpression: Expression,
    owner: MappingOwner,
    originalString: String,
    recursive: Boolean
  ) {
    myKeys[ArrayList(fromKeys)] =
      ToExpressionMappingInfo(toExpression, fromKeys, recursive, owner, originalString)
    fillPrefixes(fromKeys)
  }

  private fun fillPrefixes(fromKeys: List<KeyStroke>) {
    val prefix: MutableList<KeyStroke> = ArrayList()
    val prefixLength = fromKeys.size - 1
    for (i in 0 until prefixLength) {
      prefix.add(fromKeys[i])
      myPrefixes[ArrayList(prefix)] = (myPrefixes[ArrayList(prefix)] ?: 0) + 1
    }
  }

  fun delete(owner: MappingOwner) {
    val toRemove = myKeys.entries.stream()
      .filter { (_, value): Map.Entry<List<KeyStroke>, MappingInfo> -> value.owner == owner }
      .collect(Collectors.toList())
    toRemove.forEach(
      Consumer { (key, value): Map.Entry<List<KeyStroke>, MappingInfo> ->
        myKeys.remove(
          key, value
        )
      }
    )
    toRemove.map { it.key }.forEach(this::removePrefixes)
  }

  fun delete(keys: List<KeyStroke>) {
    myKeys.remove(keys) ?: return
    removePrefixes(keys)
  }

  fun delete() {
    myKeys.clear()
    myPrefixes.clear()
  }

  private fun removePrefixes(keys: List<KeyStroke>) {
    val prefix: MutableList<KeyStroke> = ArrayList()
    val prefixLength = keys.size - 1
    for (i in 0 until prefixLength) {
      prefix.add(keys[i])
      val existingCount = myPrefixes[prefix]
      if (existingCount == 1 || existingCount == null) {
        myPrefixes.remove(prefix)
      } else {
        myPrefixes[prefix] = existingCount - 1
      }
    }
  }

  fun getByOwner(owner: MappingOwner): List<Pair<List<KeyStroke>, MappingInfo>> {
    return myKeys.entries.stream()
      .filter { (_, value): Map.Entry<List<KeyStroke>, MappingInfo> -> value.owner == owner }
      .map { (key, value): Map.Entry<List<KeyStroke>, MappingInfo> ->
        Pair(
          key, value
        )
      }.collect(Collectors.toList())
  }

  override fun isPrefix(keys: Iterable<KeyStroke>): Boolean {
    // Having a parameter of Iterable allows for a nicer API, because we know when a given list is immutable.
    // Perhaps we should look at changing this to a trie or something?
    assert(keys is List<*>) { "keys must be of type List<KeyStroke>" }
    val keyList = keys as List<KeyStroke>
    if (keyList.isEmpty()) return false
    if (myPrefixes.contains(keys)) return true
    val firstChar = keyList[0].keyCode
    val lastChar = keyList[keyList.size - 1].keyChar
    return firstChar == injector.parser.actionKeyStroke.keyCode && lastChar != ')'
  }

  fun hasmapto(toKeys: List<KeyStroke?>): Boolean {
    return myKeys.values.stream()
      .anyMatch { o: MappingInfo? -> o is ToKeysMappingInfo && o.toKeys == toKeys }
  }

  fun getMapTo(toKeys: List<KeyStroke?>): List<Pair<List<KeyStroke>, MappingInfo>> {
    return myKeys.entries.stream()
      .filter { (_, value): Map.Entry<List<KeyStroke>, MappingInfo> -> value is ToKeysMappingInfo && value.toKeys == toKeys }
      .map { (key, value): Map.Entry<List<KeyStroke>, MappingInfo> ->
        Pair(
          key, value
        )
      }.collect(Collectors.toList())
  }

  override fun getLayer(keys: Iterable<KeyStroke>): MappingInfoLayer? {
    return get(keys)
  }
}
