/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change

import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.vimscript.model.LazyInstance
import javax.swing.KeyStroke

open class LazyVimCommand(
  val keys: Set<List<KeyStroke>>,
  val modes: Set<MappingMode>,
  className: String,
  classLoader: ClassLoader,
  /** Declared with `@CommandOrMotion(lookup = true)`: the variant for when a completion popup is open. */
  val isLookupAction: Boolean = false,
) : LazyInstance<EditorActionHandlerBase>(className, classLoader) {
  val actionId: String = EditorActionHandlerBase.getActionId(className)

  /**
   * The handler to use instead of this one while a completion popup is open, paired up by [CommandProvider].
   *
   * Only the normal command of a pair is registered in the keystroke trie - one key resolves to one node - and it
   * points here for the other half.
   */
  var lookupVariant: LazyVimCommand? = null
    internal set

  /** This command, or its [lookupVariant] when one is declared and [hasActiveLookup]. */
  fun resolve(hasActiveLookup: Boolean): LazyVimCommand =
    if (hasActiveLookup) lookupVariant ?: this else this

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as LazyVimCommand

    if (keys != other.keys) return false
    if (modes != other.modes) return false
    if (actionId != other.actionId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = keys.hashCode()
    result = 31 * result + modes.hashCode()
    result = 31 * result + actionId.hashCode()
    return result
  }

  override fun toString(): String {
    return actionId
  }
}