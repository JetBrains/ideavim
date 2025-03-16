/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.ExException

abstract class VimDataType {

  /**
   * Deprecated. Returns the current object as a string value, throwing if there is no conversion available
   *
   * This function is unclear on its intended usage, as there are several reasons for getting a string or textual
   * representation of a Vim expression result.
   *
   * If the caller requires a String value from an expression result, it is better to be explicit and use [toVimString]
   * and then use the accessors to get the underlying value. This will apply the correct automatic conversion from
   * Number, and throw for other datatypes.
   *
   * This function is used by external plugins.
   */
  @Deprecated("Use toVimString().value instead", ReplaceWith("toVimString().value"))
  fun asString(): String = toVimString().value

  /**
   * Deprecated. Returns the current object as a boolean value, throwing if there is no conversion available
   *
   * The original implementation of this would incorrectly convert Float to a boolean value, which is not part of Vim's
   * conversion rules.
   *
   * If the caller requires a boolean value from an expression result, it is better to be explicit that the expression
   * is expected to be a Vim Number, by calling [toVimNumber]. This function will apply the correct conversion rules
   * (only String can convert to Number) and will throw otherwise.
   *
   * This function is used by external plugins.
   */
  @Deprecated("Use toVimNumber().booleanValue instead", ReplaceWith("toVimNumber().booleanValue"))
  fun asBoolean(): Boolean = toVimNumber().booleanValue

  /**
   * Returns this object as a Vim Float. If the object is not a Float, this function throws
   */
  abstract fun toVimFloat(): VimFloat

  /**
   * Returns this object as a Vim Number (integral number value), converting if necessary/appropriate
   *
   * Note that Vim will only automatically convert a String to a Number. Anything else will throw an [ExException] in
   * the form "Using a XXX as a Number".
   *
   * Use this function to get a Number that can be used as a boolean value.
   */
  abstract fun toVimNumber(): VimInt

  /**
   * Returns this object as a Vim String, converting if necessary and throwing if no conversion is allowed
   *
   * Use this function to get the value of a String expression, after evaluation. It will throw an [ExException] if the
   * value is not a String and cannot be automatically converted to a String, according to Vim's rules (only Numbers
   * can be automatically converted to String).
   *
   * To get the text of an arbitrary expression result (e.g. a Float, List or Dictionary), use [toOutputString] to
   * report the textual representation to the user in output and error messages, and [toInsertableString] to get a
   * textual representation that can be inserted into a document.
   *
   * @see toOutputString
   * @see toInsertableString
   */
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
