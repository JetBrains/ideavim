/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import java.lang.ref.WeakReference

/**
 * A [Key] owned by another module or plugin that we can only find by its name.
 *
 * [Key.findKeyByName] copies and walks every key in the application while holding a global lock. That is far too slow
 * for the checks that run on every keystroke, and on a big project it freezes the UI (VIM-4379).
 *
 * So the key is looked up once and remembered. While it doesn't exist yet, every lookup only checks the keys created
 * since the previous one. Key indices only grow, and the owner may create its key later, when its class is loaded.
 *
 * The key is held weakly, so it doesn't keep an unloaded plugin in memory. Once it's collected, newer keys are checked
 * again, which also finds the key of a reloaded plugin.
 */
class NamedKey(private val name: String) {
  private var key: WeakReference<Key<*>>? = null

  /**
   * The index of the first key that we haven't checked yet, or -1 if we haven't looked for the key at all
   */
  private var nextUncheckedIndex = -1

  @Synchronized
  fun find(): Key<*>? {
    key?.get()?.let { return it }

    // Key indices come from a counter, so a new key tells us the index of the newest one
    val probeIndex = Key.create<Any>(PROBE_KEY_NAME).hashCode()
    val found = if (nextUncheckedIndex < 0) {
      @Suppress("DEPRECATION")
      Key.findKeyByName(name)
    } else {
      (nextUncheckedIndex until probeIndex).firstNotNullOfOrNull { index ->
        Key.getKeyByIndex<Any>(index)?.takeIf { it.toString() == name }
      }
    }
    nextUncheckedIndex = probeIndex + 1
    key = found?.let { WeakReference(it) }
    return found
  }

  /**
   * Gets the value of this key from [holder], or null if the key doesn't exist
   */
  fun getUserData(holder: UserDataHolder): Any? {
    val key = find() ?: return null
    return holder.getUserData(key)
  }

  private companion object {
    const val PROBE_KEY_NAME = "IdeaVim.NamedKey.probe"
  }
}
