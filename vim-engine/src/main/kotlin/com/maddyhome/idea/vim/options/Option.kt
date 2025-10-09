/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.util.*

/**
 * The base class of option types
 *
 * This class is generic on the datatype of the option, which must be a type derived from [VimDataType], such as
 * [VimInt] or [VimString]. Vim data types are used so that we can easily use options as VimScript variables.
 *
 * A note on variance: derived classes will also use a derived type of [VimDataType], which means that e.g.
 * `StringOption` would derive from `Option<VimString>`, which is not assignable to `Option<VimDataType>`. This can work
 * if we make the type covariant (e.g. `Option<out T : VimDataType>`) however, the type is not covariant - it's not
 * solely a producer ([checkIfValueValid] is a consumer, for example), so we must keep [T] as invariant. Furthermore,
 * if we make it covariant, then we also lose some type safety, with something like
 * `setValue(numberOption, VimString("foo"))` not treated as an error.
 *
 * We also want to avoid a sealed hierarchy, since we create object instances with custom validation for some options.
 *
 * @param name  The name of the option
 * @param declaredScope The declared scope of the option - global, global-local, local-to-buffer, local-to-window
 * @param abbrev  An abbreviated name for the option, recognised by `:set`
 * @param defaultValue  The default value of the option, if not set by the user
 * @param unsetValue    The value of the local part of a global-local option if the local part has not been set
 * @param isLocalNoGlobal Most local options are initialised by copying the global value from the opening window. If
 *                        this value is true, this value is not copied and the local option is set to default.
 *                        See `:help local-noglobal`
 * @param isHidden   True for feature-toggle options that will be reviewed in future releases.
 *                      Such options won't be printed in the output to `:set`
 */
abstract class Option<T : VimDataType>(
  val name: String,
  val declaredScope: OptionDeclaredScope,
  val abbrev: String,
  defaultValue: T,
  val unsetValue: T,
  val isLocalNoGlobal: Boolean = false,
  val isHidden: Boolean = false,
) {
  private var defaultValueField = defaultValue

  open val defaultValue: T
    get() = defaultValueField

  internal fun overrideDefaultValue(newDefaultValue: T) {
    defaultValueField = newDefaultValue
  }

  // todo 1.9 should return Result with exceptions
  abstract fun checkIfValueValid(value: VimDataType, token: String)
  abstract fun parseValue(value: String, token: String): VimDataType
}

/**
 * Represents a string option
 *
 * If the string option is global-local, the [unsetValue] will be used as a sentinel value to recognise that a local
 * value is not set. Vim's help pages do not explicitly state that string global-local options are an empty string, but
 * it is implied by the use of `set {option}=` to reset back to an unset state. This can also be seen interactively - an
 * unset global-local string option will be reported as `{option}=`, meaning it does not have a string value. See also
 * `:help global-local`.
 *
 * @constructor Creates a new [StringOption] instance
 * @param name The name of the option
 * @param declaredScope The declared scope of the option - global, global-local, local-to-buffer, local-to-window
 * @param abbrev  An abbreviated name for the option, recognised by `:set`
 * @param defaultValue The option's default value of the option
 * @param unsetValue The unset value of the option if the [declaredScope] is [OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER]
 * or [OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW]. The default value is an empty string.
 * @param boundedValues The collection of bounded values for the option.
 */
open class StringOption(
  name: String,
  declaredScope: OptionDeclaredScope,
  abbrev: String,
  defaultValue: VimString,
  unsetValue: VimString = VimString.EMPTY,
  val boundedValues: Collection<String>? = null,
  isLocalNoGlobal: Boolean = false,
) : Option<VimString>(name, declaredScope, abbrev, defaultValue, unsetValue, isLocalNoGlobal = isLocalNoGlobal) {

  constructor(
    name: String,
    declaredScope: OptionDeclaredScope,
    abbrev: String,
    defaultValue: String,
    boundedValues: Collection<String>? = null,
    isLocalNoGlobal: Boolean = false,
  ) : this(
    name,
    declaredScope,
    abbrev,
    VimString(defaultValue),
    boundedValues = boundedValues,
    isLocalNoGlobal = isLocalNoGlobal
  )

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimString) {
      throw exExceptionMessage("E474.arg", token)
    }

    if (value.value.isEmpty()) {
      return
    }

    if (boundedValues != null && !boundedValues.contains(value.value)) {
      throw exExceptionMessage("E474.arg", token)
    }
  }

  override fun parseValue(value: String, token: String): VimString =
    VimString(value).also { checkIfValueValid(it, token) }

  fun appendValue(currentValue: VimString, value: VimString): VimString =
    VimString(currentValue.value + value.value)

  fun prependValue(currentValue: VimString, value: VimString): VimString =
    VimString(value.value + currentValue.value)

  fun removeValue(currentValue: VimString, value: VimString): VimString =
    VimString(currentValue.value.replaceFirst(value.value, ""))
}

/**
 * Represents a string option that is a comma-separated list of values
 *
 * Note that we have tried multiple ways to represent a string list option, from a separate class similar to
 * [StringListOption] or a combined string option. While a string list option "is-a" string option, its operations
 * (append, prepend and remove) are implemented very differently to the string option. Unless there is a good reason to
 * do so, we do not expect this to change again.
 *
 * Some Vim options are a sequence of character flags, such as `'guioptions'`. These are not comma separated, and are
 * not supported by [StringListOption]. Verify the behaviour of modifying sublists if/when flags are required.
 * See `:help set-args` and `:help add-option-flags`.
 */
open class StringListOption(
  name: String,
  declaredScope: OptionDeclaredScope,
  abbrev: String,
  defaultValue: VimString,
  val boundedValues: Collection<String>? = null,
) : Option<VimString>(name, declaredScope, abbrev, defaultValue, VimString.EMPTY) {

  constructor(
    name: String,
    declaredScope: OptionDeclaredScope,
    abbrev: String,
    defaultValue: String,
    boundedValues: Collection<String>? = null,
  ) : this(name, declaredScope, abbrev, VimString(defaultValue), boundedValues)

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimString) {
      throw exExceptionMessage("E474.arg", token)
    }

    if (value.value.isEmpty()) {
      return
    }

    if (boundedValues != null && split(value.value).any { !boundedValues.contains(it) }) {
      throw exExceptionMessage("E474.arg", token)
    }
  }

  override fun parseValue(value: String, token: String): VimString =
    VimString(value).also { checkIfValueValid(it, token) }

  fun appendValue(currentValue: VimString, value: VimString): VimString {
    val valuesToAppend = split(value.value)
    val elements = split(currentValue.value).toMutableList()
    if (Collections.indexOfSubList(elements, valuesToAppend) != -1) {
      return currentValue
    }
    return VimString(joinValues(currentValue.value, value.value))
  }

  fun prependValue(currentValue: VimString, value: VimString): VimString {
    val valuesToPrepend = split(value.value)
    val elements = split(currentValue.value).toMutableList()
    if (Collections.indexOfSubList(elements, valuesToPrepend) != -1) {
      return currentValue
    }
    return VimString(joinValues(value.value, currentValue.value))
  }

  fun removeValue(currentValue: VimString, value: VimString): VimString {
    val valuesToRemove = split(value.value)
    val elements = split(currentValue.value).toMutableList()
    if (Collections.indexOfSubList(elements, valuesToRemove) != -1) {
      elements.removeAll(valuesToRemove)
    }
    return VimString(elements.joinToString(separator = ","))
  }

  open fun split(value: String): List<String> = value.split(",")

  private fun joinValues(first: String, second: String): String {
    val separator = if (first.isNotEmpty()) "," else ""
    return first + separator + second
  }
}

/**
 * Represents a number option
 *
 * If a number option is global-local, the [unsetValue] will be used as a sentinel value to recognise that a local value
 * is not set. Vim's help pages do not specify that a number option's unset value is `-1`, but this can be seen in the
 * output for `:setlocal {option}?` for a number global-local option.
 *
 * @constructor Creates a new [NumberOption] instance
 * @param name The name of the option
 * @param declaredScope The declared scope of the option - global, global-local, local-to-buffer, local-to-window
 * @param abbrev  An abbreviated name for the option, recognised by `:set`
 * @param defaultValue The option's default value of the option
 * @param unsetValue The unset value of the option if the [declaredScope] is [OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER]
 * or [OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW]. The default value is `-1`.
 */
open class NumberOption(
  name: String,
  declaredScope: OptionDeclaredScope,
  abbrev: String,
  defaultValue: VimInt,
  unsetValue: VimInt = VimInt.MINUS_ONE,
) :
  Option<VimInt>(name, declaredScope, abbrev, defaultValue, unsetValue) {

  constructor(
    name: String,
    declaredScope: OptionDeclaredScope,
    abbrev: String,
    defaultValue: Int,
    unsetValue: Int = -1,
  ) : this(
    name,
    declaredScope,
    abbrev,
    VimInt(defaultValue),
    if (unsetValue == -1) VimInt.MINUS_ONE else VimInt(unsetValue)
  )

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimInt) throw exExceptionMessage("E521", token)
  }

  override fun parseValue(value: String, token: String): VimInt {
    val number = VimInt.parseNumber(value) ?: throw exExceptionMessage("E521", token)
    checkIfValueValid(number, token)
    return number
  }

  fun addValues(value1: VimInt, value2: VimInt): VimInt = VimInt(value1.value + value2.value)
  fun multiplyValues(value1: VimInt, value2: VimInt): VimInt = VimInt(value1.value * value2.value)
  fun subtractValues(value1: VimInt, value2: VimInt): VimInt = VimInt(value1.value - value2.value)
}

/**
 * Represents a number option that always has a positive value
 *
 * If an unsigned number option is global-local, then its [unsetValue] is inherited from [NumberOption] and will be
 * `-1`. While this value is invalid, it is used as a sentinel value to know that the local value is not set. Consumers
 * of the options API will not use the raw local value but will get the effective value (if unset, the global value is
 * used). Only the `:setlocal {option}?` command needs the raw local value, but uses it for output purposes only.
 *
 * @constructor Creates a new [UnsignedNumberOption] instance
 * @param name The name of the option
 * @param declaredScope The declared scope of the option - global, global-local, local-to-buffer, local-to-window
 * @param abbrev  An abbreviated name for the option, recognised by `:set`
 * @param defaultValue The option's default value of the option
 */
open class UnsignedNumberOption(
  name: String,
  declaredScope: OptionDeclaredScope,
  abbrev: String,
  defaultValue: VimInt,
) : NumberOption(name, declaredScope, abbrev, defaultValue) {

  constructor(name: String, declaredScope: OptionDeclaredScope, abbrev: String, defaultValue: Int) : this(
    name,
    declaredScope,
    abbrev,
    VimInt(defaultValue)
  )

  override fun checkIfValueValid(value: VimDataType, token: String) {
    super.checkIfValueValid(value, token)
    if ((value as VimInt).value < 0) {
      throw exExceptionMessage("E487", token)
    }
  }
}

/**
 * Represents a boolean option
 *
 * Boolean options are represented as a number, using the [VimInt] datatype. A value of zero is treated as false, and
 * any other value is treated as true. If a boolean option is global-local, the [unsetValue] is set to the `-1` sentinel
 * value. Vim does not document this anywhere; however, it can be observed using `:echo &l:autoread` which will output
 * the unset value of the local boolean option `'autoread'` as `-1`.
 *
 * @constructor Creates a new [UnsignedNumberOption] instance
 * @param name The name of the option
 * @param declaredScope The declared scope of the option - global, global-local, local-to-buffer, local-to-window
 * @param abbrev  An abbreviated name for the option, recognised by `:set`
 * @param defaultValue The option's default value of the option
 * @param isHidden   True for feature-toggle options that will be reviewed in future releases.
 *                   Such options won't be printed in the output to `:set`
 */
class ToggleOption(
  name: String,
  declaredScope: OptionDeclaredScope,
  abbrev: String,
  defaultValue: VimInt,
  isLocalNoGlobal: Boolean = false,
  isHidden: Boolean = false,
) :
  Option<VimInt>(
    name,
    declaredScope,
    abbrev,
    defaultValue,
    VimInt.MINUS_ONE,
    isLocalNoGlobal = isLocalNoGlobal,
    isHidden = isHidden
  ) {

  constructor(
    name: String,
    declaredScope: OptionDeclaredScope,
    abbrev: String,
    defaultValue: Boolean,
    isLocalNoGlobal: Boolean = false,
    isHidden: Boolean = false,
  ) : this(
    name,
    declaredScope,
    abbrev,
    if (defaultValue) VimInt.ONE else VimInt.ZERO,
    isLocalNoGlobal = isLocalNoGlobal,
    isHidden = isHidden
  )

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimInt) throw exExceptionMessage("E474.arg", token)
  }

  override fun parseValue(value: String, token: String): Nothing = throw exExceptionMessage("E474.arg", token)
}
