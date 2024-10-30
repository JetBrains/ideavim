/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.api.injector
import javax.swing.KeyStroke

/**
 * A trie data structure for storing and retrieving values associated with sequences of keystrokes
 *
 * All leaves will have data, but it is not a requirement for nodes with data to have no children.
 *
 * @param name The name of this KeyStrokeTrie instance (for debug purposes)
 */
class KeyStrokeTrie<T>(private val name: String) {
  interface TrieNode<T> {
    val data: T?

    fun visit(visitor: (KeyStroke, TrieNode<T>) -> Unit)

    val debugString: String
  }

  private class TrieNodeImpl<T>(val name: String, val depth: Int, override val data: T?)
    : TrieNode<T> {

    val children = lazy { mutableMapOf<KeyStroke, TrieNodeImpl<T>>() }

    override fun visit(visitor: (KeyStroke, TrieNode<T>) -> Unit) {
      if (!children.isInitialized()) return
      children.value.forEach { visitor(it.key, it.value) }
    }

    /**
     * Debug helpers to dump this node and its children
     */
    override val debugString
      get() = buildString { dump(this) }

    private fun dump(builder: StringBuilder) {
      builder.run {
        append("TrieNode('")
        append(name)
        append("'")
        if (data != null) {
          append(", ")
          append(data)
        }
        if (children.isInitialized() && children.value.isNotEmpty()) {
          appendLine()
          children.value.forEach {
            repeat(depth + 1) { append(" ") }
            append("'")
            append(injector.parser.toKeyNotation(it.key))
            append("' - ")
            it.value.dump(this)
            if (children.value.size > 1 || depth > 0) appendLine()
          }
          repeat(depth) { append(" ") }
        }
        append(")")
      }
    }

    override fun toString() = "TrieNode('$name', ${children.value.size} children): $data"
  }

  private val root = TrieNodeImpl<T>("", 0, null)

  fun visit(visitor: (KeyStroke, TrieNode<T>) -> Unit) {
    // Does not visit the (empty) root node
    root.visit(visitor)
  }

  fun add(keyStrokes: List<KeyStroke>, data: T) {
    var current = root
    keyStrokes.forEachIndexed { i, stroke ->
      current = current.children.value.getOrPut(stroke) {
        val name = current.name + injector.parser.toKeyNotation(stroke)
        TrieNodeImpl(name, current.depth + 1, if (i == keyStrokes.lastIndex) data else null)
      }
    }
  }

  /**
   * Get the data for the given key sequence if it exists
   *
   * @return Returns null if the key sequence does not exist, or if the data at the node is empty
   */
  fun getData(keyStrokes: List<KeyStroke>): T? {
    var current = root
    keyStrokes.forEach {
      if (!current.children.isInitialized()) return null
      current = current.children.value[it] ?: return null
    }
    return current.data
  }

  /**
   * Get the node for the given key sequence if it exists
   *
   * Like [getData] but will return a node even if that node's data is empty. Will return something useful in the case
   * of a matching sequence, or a matching prefix. If it's only a matching prefix, the [TrieNode.data] value will be
   * null.
   */
  fun getTrieNode(keyStrokes: List<KeyStroke>): TrieNode<T>? {
    var current = root
    keyStrokes.forEach {
      if (!current.children.isInitialized()) return null
      current = current.children.value[it] ?: return null
    }
    return current
  }

  override fun toString(): String {
    val children = if (root.children.isInitialized()) {
      "${root.children.value.size} children"
    }
    else {
      "0 children (not initialized)"
    }
    return "KeyStrokeTrie - '$name', $children"
  }
}

fun <T> KeyStrokeTrie<T>.add(keys: String, data: T) {
  add(injector.parser.parseKeys(keys), data)
}

/**
 * Returns a map containing all keystroke sequences that start with the given prefix
 *
 * This only returns keystroke sequences that have associated data. A keystroke sequence without data is considered a
 * prefix and not included in the map.
 */
fun <T> KeyStrokeTrie<T>.getPrefixed(prefix: List<KeyStroke>): Map<List<KeyStroke>, T> {
  fun visitor(prefix: List<KeyStroke>, map: MutableMap<List<KeyStroke>, T>) {
    getTrieNode(prefix)?.let { node ->
      node.data?.let { map[prefix] = it }
      node.visit { key, value -> visitor(prefix + key, map) }
    }
  }

  return buildMap { visitor(prefix, this) }
}

/**
 * Returns all keystroke sequences with associated data
 */
fun <T> KeyStrokeTrie<T>.getAll(): Map<List<KeyStroke>, T> = getPrefixed(emptyList())
