/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import javax.swing.KeyStroke

public class RequiredShortcut(public val keyStroke: KeyStroke, public val owner: MappingOwner)

public sealed class MappingOwner {
  public interface IdeaVim {
    public object InitScript : MappingOwner()
    public object Other : MappingOwner()
    public object System : MappingOwner()
  }

  @Suppress("DataClassPrivateConstructor")
  public data class Plugin private constructor(val name: String) : MappingOwner() {
    public companion object {
      public fun get(name: String): Plugin = allOwners.computeIfAbsent(name) { Plugin(it) }

      public fun remove(name: String): Plugin? = allOwners.remove(name)

      private val allOwners: MutableMap<String, Plugin> = mutableMapOf()
    }
  }
}
