/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.ExException

abstract class VimDataType(val typeName: String) {
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
   * Formats the object so that it can be used when outputting the value of an object in `:echo` or `:throw`, `join()`,
   * when option values are formatted and other user-facing output and error messages.
   *
   * String is used as is, with no quotation marks. Number and Float are converted to string values. A List is shown in
   * square brackets, like `[1, 2, 3]` and a Dictionary is shown in curly braces, like `{key1: 2, key2: 4}`. Funcref
   * depends on the kind of function reference, for example, a built-in function shows the name, such as `abs`, while a
   * user-defined function will be formatted such as `function('s:Func')`, amongst others.
   */
  abstract fun toOutputString(): String

  /**
   * Implementation of [toOutputString] for recursive lists and dictionaries
   *
   * DO NOT USE!
   */
  internal open fun buildOutputString(builder: StringBuilder, visited: MutableSet<VimDataType>) {
    builder.append(toOutputString())
  }

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
   * Implementation of [toInsertableString] for recursive lists and dictionaries
   *
   * DO NOT USE!
   */
  internal open fun buildInsertableString(builder: StringBuilder, depth: Int): Boolean {
    builder.append(toInsertableString())
    return true
  }

  /**
   * Returns true if this object is equal to another object, based on value semantics
   *
   * Not all Vim types can be represented as data classes, with value semantics. For example, a list or dictionary can
   * be a recursive data structure, and that would cause problems with implementation of [equals] and [hashCode].
   *
   * This function can be used to compare the values of two objects are correct. It does no type coercion, unlike Vim's
   * `==` operator.
   */
  open fun valueEquals(other: VimDataType, ignoreCase: Boolean, depth: Int = 0) = this == other

  /**
   * Create a shallow copy of this object
   *
   * Creates a new object with the same value as this one. The new object will be a new instance, but the values will
   * be the same. For example, a [VimFloat] will have the same value, but will be a new [VimFloat] instance. When
   * copying a more complex object like [VimList] or [VimDictionary], a new list or dictionary instance is created,
   * but the existing list or dictionary items are reused.
   *
   * Use [deepCopy] to create a completely new instance.
   */
  abstract fun copy(): VimDataType

  abstract fun deepCopy(level: Int = 100): VimDataType

  var lockOwner: Any? = null
  var isLocked: Boolean = false
    protected set

  abstract fun lockVar(depth: Int)
  abstract fun unlockVar(depth: Int)

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
   * Provides a diagnostic string representation of an object, useful while debugging
   *
   * To avoid confusion with different data conversion functions and requirements, [toString] should only be used for
   * debugging. Use one of the semantically named conversion functions to convert to a string, or to get a string
   * representation for `:echo` output or error messages.
   */
  @Suppress("POTENTIALLY_NON_REPORTED_ANNOTATION")
  @Deprecated("Use toOutputString instead", ReplaceWith("toOutputString()"))
  final override fun toString() = "${this.javaClass.simpleName}(${toOutputString()})"
}
