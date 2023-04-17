/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.parseNumber
import java.util.*

/**
 * The base class of option types
 *
 * This class is generic on the datatype of the option, which must be a type derived from [VimDataType], such as
 * [VimInt] or [VimString]. Vim data types are used so that we can easily use options as VimScript variables.
 *
 * A note on variance: derived classes will also use a derived type of [VimDataType], which means that e.g.
 * `StringOption` would derive from `Option<VimString>`, which is not assignable to `Option<VimDataType>`. This can work
 * if we make the type covariant (e.g. `Option<out T : VimDataType>`) however the type is not covariant - it's not
 * solely a producer ([onChanged] is a consumer, for example), so we must keep [T] as invariant. Furthermore, if we make
 * it covariant, then we also lose some type safety, with something like `setValue(numberOption, VimString("foo"))` not
 * treated as an error.
 *
 * We also want to avoid a sealed hierarchy, since we create object instances with custom validation for some options.
 */
public abstract class Option<T : VimDataType>(public val name: String, public val abbrev: String, defaultValue: T) {
  private val listeners = mutableSetOf<OptionChangeListener<T>>()

  private var defaultValueField = defaultValue

  public open val defaultValue: T
    get() = defaultValueField

  internal fun overrideDefaultValue(newDefaultValue: T) {
    defaultValueField = newDefaultValue
  }

  public open fun addOptionChangeListener(listener: OptionChangeListener<T>) {
    listeners.add(listener)
  }

  public open fun removeOptionChangeListener(listener: OptionChangeListener<T>) {
    listeners.remove(listener)
  }

  public fun onChanged(scope: OptionScope, oldValue: T) {
    for (listener in listeners) {
      when (scope) {
        is OptionScope.GLOBAL -> listener.processGlobalValueChange(oldValue)
        is OptionScope.LOCAL -> {
          if (listener is LocalOptionChangeListener) {
            listener.processLocalValueChange(oldValue, scope.editor)
          } else {
            listener.processGlobalValueChange(oldValue)
          }
        }
      }
    }
  }

  // todo 1.9 should return Result with exceptions
  public abstract fun checkIfValueValid(value: VimDataType, token: String)
  public abstract fun parseValue(value: String, token: String): VimDataType
}

public open class StringOption(name: String, abbrev: String, defaultValue: VimString, public val boundedValues: Collection<String>? = null) : Option<VimString>(name, abbrev, defaultValue) {
  public constructor(name: String, abbrev: String, defaultValue: String, boundedValues: Collection<String>? = null) : this(name, abbrev, VimString(defaultValue), boundedValues)

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimString) {
      throw exExceptionMessage("E474", token)
    }

    if (value.value.isEmpty()) {
      return
    }

    if (boundedValues != null && !boundedValues.contains(value.value)) {
      throw exExceptionMessage("E474", token)
    }
  }

  override fun parseValue(value: String, token: String): VimString =
    VimString(value).also { checkIfValueValid(it, token) }

  public fun appendValue(currentValue: VimString, value: VimString): VimString =
    VimString(currentValue.value + value.value)

  public fun prependValue(currentValue: VimString, value: VimString): VimString =
    VimString(value.value + currentValue.value)

  public fun removeValue(currentValue: VimString, value: VimString): VimString {
    // TODO: Not sure this is correct. Should replace just the first occurrence?
    return VimString(currentValue.value.replace(value.value, ""))
  }
}

/**
 * Represents a string that is a comma-separated list of values
 *
 * Note that we have tried multiple ways to represent a string list option, from a separate class similar to
 * [StringListOption] or a combined string option. While a string list option "is-a" string option, its operations
 * (append, prepend and remove) are implemented very differently to the string option. Unless there is a good reason to
 * do so, we do not expect this to change again.
 */
public open class StringListOption(
  name: String,
  abbrev: String,
  defaultValue: VimString,
  public val boundedValues: Collection<String>? = null,
) : Option<VimString>(name, abbrev, defaultValue) {

  public constructor(name: String, abbrev: String, defaultValue: String, boundedValues: Collection<String>? = null)
    : this(name, abbrev, VimString(defaultValue), boundedValues)

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimString) {
      throw exExceptionMessage("E474", token)
    }

    if (value.value.isEmpty()) {
      return
    }

    if (boundedValues != null && split(value.value).any { !boundedValues.contains(it) }) {
      throw exExceptionMessage("E474", token)
    }
  }

  override fun parseValue(value: String, token: String): VimString =
    VimString(value).also { checkIfValueValid(it, token) }

  public fun appendValue(currentValue: VimString, value: VimString): VimString {
    // TODO: What happens if we're trying to add a sublist that already exists?
    if (split(currentValue.value).contains(value.value)) return currentValue
    return VimString(joinValues(currentValue.value, value.value))
  }

  public fun prependValue(currentValue: VimString, value: VimString): VimString {
    // TODO: What happens if we're trying to add a sublist that already exists?
    if (split(currentValue.value).contains(value.value)) return currentValue
    return VimString(joinValues(value.value, currentValue.value))
  }

  public fun removeValue(currentValue: VimString, value: VimString): VimString {
    val valuesToRemove = split(value.value)
    val elements = split(currentValue.value).toMutableList()
    if (Collections.indexOfSubList(elements, valuesToRemove) != -1) {
      // see `:help set`
      // When the option is a list of flags, {value} must be
      // exactly as they appear in the option.  Remove flags
      // one by one to avoid problems.
      elements.removeAll(valuesToRemove)
    }
    return VimString(elements.joinToString(separator = ","))
  }

  public open fun split(value: String): List<String> = value.split(",")

  private fun joinValues(first: String, second: String): String {
    val separator = if (first.isNotEmpty()) "," else ""
    return first + separator + second
  }
}

public open class NumberOption(name: String, abbrev: String, defaultValue: VimInt) :
  Option<VimInt>(name, abbrev, defaultValue) {
  public constructor(name: String, abbrev: String, defaultValue: Int) : this(name, abbrev, VimInt(defaultValue))

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimInt) throw exExceptionMessage("E521", token)
  }

  override fun parseValue(value: String, token: String): VimInt =
    VimInt(parseNumber(value) ?: throw exExceptionMessage("E521", token)).also { checkIfValueValid(it, token) }

  public fun addValues(value1: VimInt, value2: VimInt): VimInt = VimInt(value1.value + value2.value)
  public fun multiplyValues(value1: VimInt, value2: VimInt): VimInt = VimInt(value1.value * value2.value)
  public fun subtractValues(value1: VimInt, value2: VimInt): VimInt = VimInt(value1.value - value2.value)
}

public open class UnsignedNumberOption(name: String, abbrev: String, defaultValue: VimInt) :
  NumberOption(name, abbrev, defaultValue) {

  public constructor(name: String, abbrev: String, defaultValue: Int) : this(name, abbrev, VimInt(defaultValue))

  override fun checkIfValueValid(value: VimDataType, token: String) {
    super.checkIfValueValid(value, token)
    if ((value as VimInt).value < 0) {
      throw ExException("E487: Argument must be positive: $token")
    }
  }
}

public class ToggleOption(name: String, abbrev: String, defaultValue: VimInt) : Option<VimInt>(name, abbrev, defaultValue) {
  public constructor(name: String, abbrev: String, defaultValue: Boolean) : this(name, abbrev, if (defaultValue) VimInt.ONE else VimInt.ZERO)

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimInt) throw exExceptionMessage("E474", token)
  }

  override fun parseValue(value: String, token: String): Nothing = throw exExceptionMessage("E474", token)
}
