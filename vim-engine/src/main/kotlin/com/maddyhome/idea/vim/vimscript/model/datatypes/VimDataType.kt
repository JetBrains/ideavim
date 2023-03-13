/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

public abstract class VimDataType {

  public abstract fun asDouble(): Double

  // string value that is used in arithmetic expressions (concatenation etc.)
  public abstract fun asString(): String

  public abstract fun toVimNumber(): VimInt

  // string value that is used in echo-like commands
  override fun toString(): String {
    throw NotImplementedError("implement me :(")
  }

  public open fun asBoolean(): Boolean {
    return asDouble() != 0.0
  }

  public abstract fun deepCopy(level: Int = 100): VimDataType

  public var lockOwner: Any? = null
  public var isLocked: Boolean = false

  public abstract fun lockVar(depth: Int)
  public abstract fun unlockVar(depth: Int)

  // use in cases when VimDataType's value should be inserted into document
  // e.g. expression register or substitute with expression
  public fun toInsertableString(): String {
    return when (this) {
      is VimList -> {
        this.values.joinToString(separator = "") { it.toString() + "\n" }
      }
      is VimDictionary -> this.asString()
      else -> this.toString()
    }
  }
}
