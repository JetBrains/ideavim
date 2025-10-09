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
import com.maddyhome.idea.vim.key.KeyStrokeTrie.TrieNode
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

data class KeyMappingEntry(private val node: TrieNode<MappingInfo>) {
  val mappingInfo = node.data!!

  fun collectPath(path: MutableList<KeyStroke>): List<KeyStroke> {
    path.clear()
    var current: TrieNode<MappingInfo>? = node
    while (current != null && current.parent != null) {
      path.add(current.key)
      current = current.parent
    }
    path.reverse()
    return path
  }

  fun getPath() = buildList { collectPath(this) }
}

/**
 * Container for key mappings for some mode
 * Iterable by "from" keys
 *
 * @author vlan
 */
class KeyMapping(private val mode: MappingMode) : Iterable<List<KeyStroke>>, KeyMappingLayer {
  private val keysTrie = KeyStrokeTrie<MappingInfo>(mode.name)

  /**
   * Returns mapping info for the given key sequence, if any
   */
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

  // TODO: Do we need this as well as get()?
  override fun getLayer(keys: List<KeyStroke>): MappingInfoLayer? = get(keys)

  @Deprecated("Use get(List<KeyStroke>) to maintain the same lookup key type and avoid unnecessary wrapping")
  operator fun get(keys: Iterable<KeyStroke>): MappingInfo? =
    get(keys as? List<KeyStroke> ?: keys.toList())

  private fun getActionNameFromActionMapping(keys: List<KeyStroke>): String? {
    if (keys.size > 3
      && keys[0].keyCode == injector.parser.actionKeyStroke.keyCode
      && keys[1].keyChar == '(' && keys.last().keyChar == ')'
    ) {
      return buildString {
        for (i in 2 until keys.size - 1) {
          append(keys[i].keyChar)
        }
      }
    }
    return null
  }

  // Used by idea-which-key
  @Deprecated(
    "Use getAll to return a sequence that does not allocate a list of for all entries and a list for the keystrokes of each entry",
    ReplaceWith("getAll()")
  )
  override fun iterator(): Iterator<List<KeyStroke>> =
    keysTrie.getEntries().map { KeyMappingEntry(it).getPath() }.iterator()

  /**
   * Returns a sequence of all valid key sequences for the given prefix, optionally including mappings for the prefixes
   *
   * If [prefix] is an empty list, returns all valid key sequences. If [includePrefixMappings] is true, also returns
   * mapping sequences for each key in the prefix. Otherwise, skips the sequences for the prefixes.
   */
  fun getAll(prefix: List<KeyStroke>, includePrefixMappings: Boolean = true): Sequence<KeyMappingEntry> =
    keysTrie.getEntries(prefix, includePrefixNodes = includePrefixMappings).map { KeyMappingEntry(it) }

  /**
   * Return a sequence of all valid key sequences belonging to the given owner
   */
  fun getAllByOwner(owner: MappingOwner) =
    keysTrie.getEntries().filter { it.data?.owner == owner }.map { KeyMappingEntry(it) }

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

  /**
   * Delete mapping info for the given key sequence
   *
   * If the key sequence is also a prefix, all child sequences are not modified
   */
  fun removeKeyMapping(keys: List<KeyStroke>) {
    keysTrie.remove(keys)
  }

  /**
   * Delete all key sequences owned by the given owner
   */
  fun removeKeyMappingsByOwner(owner: MappingOwner) {
    // Make a copy of the sequence, so we can modify it without exceptions
    val toRemove = getAllByOwner(owner).toList()
    val keys = mutableListOf<KeyStroke>()
    toRemove.forEach { keysTrie.remove(it.collectPath(keys)) }
  }

  /**
   * Clears all maps for this mapping mode
   */
  fun clear() {
    keysTrie.clear()
  }

  /**
   * Returns true if the given list of keystrokes is a prefix
   *
   * Used while handling unfinished mapping sequences. Note that a list of keystrokes that is both a key sequence and a
   * prefix is treated as a prefix.
   */
  override fun isPrefix(keys: List<KeyStroke>): Boolean {
    if (keys.isEmpty()) return false
    if (keysTrie.isPrefix(keys)) return true

    // Is this an incomplete RHS on-demand <Action>(...)?
    return keys.first().keyCode == injector.parser.actionKeyStroke.keyCode
      && (keys.size < 2 || keys[1].keyChar == '(')
      && (keys.size < 3 || !Character.isWhitespace(keys[2].keyChar))
      && keys.last().keyChar != ')'
  }

  /**
   * Returns true if there exists a mapping to keys that match the given key sequence
   */
  fun hasmapto(toKeys: List<KeyStroke>) =
    keysTrie.getEntries().any { (it.data as? ToKeysMappingInfo)?.toKeys == toKeys }


  // Currently used externally by peekaboo plugin
  @Deprecated("Use different approach")
  @TestOnly
  fun getMapTo(toKeys: List<KeyStroke?>): List<Pair<List<KeyStroke>, MappingInfo>> {
    return keysTrie.getEntries().filter { node ->
      if (node.data == null) return@filter false
      val mappingInfo = node.data
      KeyMappingEntry(node).getPath()
      mappingInfo is ToKeysMappingInfo && mappingInfo.toKeys == toKeys
    }.map { KeyMappingEntry(it).getPath() to it.data!! }.toList()
  }
}
