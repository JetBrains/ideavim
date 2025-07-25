/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

sealed class ListenerOwner {

  interface IdeaVim {
    object System : ListenerOwner()
  }

  @ConsistentCopyVisibility
  data class Plugin private constructor(val name: String) : ListenerOwner() {
    companion object {
      fun get(name: String): Plugin = allOwners.computeIfAbsent(name) { Plugin(it) }

      fun remove(name: String): Plugin? = allOwners.remove(name)

      private val allOwners: MutableMap<String, Plugin> = mutableMapOf()
    }
  }
}