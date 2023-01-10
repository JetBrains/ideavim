/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import javax.swing.KeyStroke

class RequiredShortcut(val keyStroke: KeyStroke, val owner: MappingOwner)

sealed class MappingOwner {
  interface IdeaVim {
    object InitScript : MappingOwner()
    object Other : MappingOwner()
    object System : MappingOwner()
  }

  @Suppress("DataClassPrivateConstructor")
  data class Plugin private constructor(val name: String) : MappingOwner() {
    companion object {
      fun get(name: String): Plugin = allOwners.computeIfAbsent(name) { Plugin(it) }

      fun remove(name: String) = allOwners.remove(name)

      private val allOwners: MutableMap<String, Plugin> = mutableMapOf()
    }
  }
}
