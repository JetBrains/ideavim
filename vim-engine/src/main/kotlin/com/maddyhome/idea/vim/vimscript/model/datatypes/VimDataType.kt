/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

abstract class VimDataType {

  abstract fun asDouble(): Double

  // string value that is used in arithmetic expressions (concatenation etc.)
  abstract fun asString(): String

  open fun asBoolean(): Boolean {
    return asDouble() != 0.0
  }

  abstract fun toVimNumber(): VimInt
  abstract fun toVimString(): VimString

  /**
   * Returns a textual representation of the object, suitable for use in output messages
   *
   * Will return a textual representation of an object that isn't a String, such as a List, Dictionary or Funcref.
   *
   * Used by `:echo` and `:throw`, `join()`, formatting option values, as well as user facing error messages.
   */
  abstract fun toOutputString(): String

  /**
   * Returns a textual representation of the object, suitable for inserting into the editor's text
   *
   * Used when evaluating the expression register (e.g. `i_CTRL-R_=` and `c_CTRL-R_=`), or when using an expression with
   * the substitute command (`s/\=`).
   *
   * The output is very similar to [toOutputString], but datatypes are allowed to override for different formatting.
   * E.g. [VimList] will treat items as separate lines (even in a command prompt!)
   */
  open fun toInsertableString() = toOutputString()

  /**
   * Provides a diagnostic string representation of an object, useful while debugging
   *
   * To avoid confusion with different data conversion functions and requirements, [toString] should only be used for
   * debugging. Use one of the semantically named conversion functions to convert to a string, or to get a string
   * representation for `:echo` output or error messages.
   */
  @Suppress("POTENTIALLY_NON_REPORTED_ANNOTATION")
  @Deprecated("Use toOutputString instead", ReplaceWith("toOutputString()"))
  final override fun toString() = "${this.javaClass.simpleName}(${toOutputString()})"

  abstract fun deepCopy(level: Int = 100): VimDataType

  var lockOwner: Any? = null
  var isLocked: Boolean = false
    protected set

  abstract fun lockVar(depth: Int)
  abstract fun unlockVar(depth: Int)
}
