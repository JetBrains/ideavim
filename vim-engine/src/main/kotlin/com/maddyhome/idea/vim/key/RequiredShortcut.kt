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

/**
 * Every mapping in IdeaVim (like `map jk <esc>`) has an owner. That is needed to understand where this mapping comes
 *   from. With that, we can, for example, remove all mappings from .ideavimrc when this file is reloaded.
 */
public sealed class MappingOwner {
  public interface IdeaVim {
    /**
     * Mapping is defined vim .ideavimrc configuration file. This doesn't always mean that the mapping is located
     *   in this particular file, but it can be defined in the file that is loaded via source command in .ideavimrc
     *   like `source ~/.vimrc`.
     */
    public object InitScript : MappingOwner()

    /**
     * Mappings created during runtime. For example, when created via `:map jk <esc>` command.
     * Also, this includes mappings that were loaded from scripts that are not .ideavimrc.
     */
    public object Other : MappingOwner()

    /**
     * Mappings that relate to IdeaVim core. Not defined by user
     */
    public object System : MappingOwner()
  }

  /**
   * Mappings registered from plugins
   */
  @Suppress("DataClassPrivateConstructor")
  public data class Plugin private constructor(val name: String) : MappingOwner() {
    public companion object {
      public fun get(name: String): Plugin = allOwners.computeIfAbsent(name) { Plugin(it) }

      public fun remove(name: String): Plugin? = allOwners.remove(name)

      private val allOwners: MutableMap<String, Plugin> = mutableMapOf()
    }
  }
}
