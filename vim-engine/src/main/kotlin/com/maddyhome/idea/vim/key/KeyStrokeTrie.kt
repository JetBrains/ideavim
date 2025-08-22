/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.api.injector
import java.awt.event.KeyEvent
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
    val key: KeyStroke
    val data: T?
    val parent: TrieNode<T>?
    val hasChildren: Boolean

    fun visit(visitor: (KeyStroke, TrieNode<T>) -> Unit)

    val debugString: String
  }

  private class TrieNodeImpl<T>(
    val name: String,
    override val key: KeyStroke,
    override val parent: TrieNodeImpl<T>?,
    override var data: T?,
  ) : TrieNode<T> {

    // Only initialised when we're adding children. If we don't add children, it will be uninitialised.
    // Avoid unnecessary access, to avoid allocating empty maps for leaf nodes.
    val children = lazy { mutableMapOf<KeyStroke, TrieNodeImpl<T>>() }

    override val hasChildren
      get() = children.isInitialized() && children.value.isNotEmpty()

    val depth: Int
      get() = parent?.let { it.depth + 1 } ?: 0

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

  private val root = TrieNodeImpl<T>(
    name = "",
    key = KeyStroke.getKeyStroke(KeyEvent.CHAR_UNDEFINED),
    parent = null,
    data = null
  )

  fun add(keyStrokes: List<KeyStroke>, data: T) {
    var current = root
    keyStrokes.forEachIndexed { i, stroke ->
      current = current.children.value.getOrPut(stroke) {
        val name = current.name + injector.parser.toKeyNotation(stroke)
        TrieNodeImpl(name, stroke, current, if (i == keyStrokes.lastIndex) data else null)
      }
    }

    // Last write wins (also means we can't cache results)
    current.data = data
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

  /**
   * Returns a sequence of nodes that contain data
   *
   * Prefixes are skipped
   */
  fun getEntries(prefix: List<KeyStroke>? = null): Sequence<TrieNode<T>> {
    suspend fun SequenceScope<TrieNode<T>>.yieldTrieNode(node: TrieNodeImpl<T>) {
      if (node.data != null) yield(node)
      if (node.children.isInitialized()) {
        node.children.value.forEach { yieldTrieNode(it.value) }
      }
    }

    val node = prefix?.let { getTrieNode(it) as TrieNodeImpl<T> } ?: root
    return sequence { yieldTrieNode(node) }
  }

  /**
   * Returns true if the given keys are a prefix to a longer sequence of keys
   *
   * Will return true even if the current keys map to a node with data.
   */
  fun isPrefix(keyStrokes: List<KeyStroke>): Boolean {
    val node = getTrieNode(keyStrokes) as? TrieNodeImpl<T> ?: return false
    return node.children.isInitialized() && node.children.value.isNotEmpty()
  }

  /**
   * Removes the given key sequence from the trie
   *
   * If the key sequence is also a prefix, removes the associated data, but does not modify any child sequences.
   */
  fun remove(keys: List<KeyStroke>) {
    val path = buildList {
      var current = root
      keys.forEach { key ->
        if (!current.children.isInitialized()) return
        val next = current.children.value[key] ?: return
        add(Pair(current, key))
        current = next
      }
    }

    path.asReversed().forEach { (parent, key) ->
      val child = parent.children.value[key] ?: return
      child.data = null
      if (child.children.isInitialized() && child.children.value.isNotEmpty()) return
      parent.children.value.remove(key)
      if (parent.children.value.isNotEmpty() || parent.data != null) return
    }
  }

  fun clear() {
    if (root.children.isInitialized()) {
      root.children.value.clear()
    }
  }

  override fun toString(): String {
    val children = if (root.children.isInitialized()) {
      "${root.children.value.size} children"
    } else {
      "0 children (not initialized)"
    }
    return "KeyStrokeTrie - '$name', $children"
  }
}

fun <T> KeyStrokeTrie<T>.add(keys: String, data: T) {
  add(injector.parser.parseKeys(keys), data)
}
