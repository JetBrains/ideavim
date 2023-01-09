/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.option.NumberOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.util.*

/**
 * COMPATIBILITY-LAYER: switched from sealed to abstract
 * Please see: https://jb.gg/zo8n0r
 */
/*sealed*/abstract class Option<T : VimDataType>(val name: String, val abbrev: String, private val defaultValue: T) {

  open fun getDefaultValue(): T {
    return defaultValue
  }

  private val listeners = mutableSetOf<OptionChangeListener<VimDataType>>()

  open fun addOptionChangeListener(listener: OptionChangeListener<VimDataType>) {
    listeners.add(listener)
  }

  open fun removeOptionChangeListener(listener: OptionChangeListener<VimDataType>) {
    listeners.remove(listener)
  }

  fun onChanged(scope: OptionScope, oldValue: VimDataType) {
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

  /**
   * COMPATIBILITY-LAYER: Method added
   * Please see: https://jb.gg/zo8n0r
   */
  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  open val value: java.lang.Boolean
    get() = TODO()

  // todo 1.9 should return Result with exceptions
  abstract fun checkIfValueValid(value: VimDataType, token: String)

  abstract fun getValueIfAppend(currentValue: VimDataType, value: String, token: String): T
  abstract fun getValueIfPrepend(currentValue: VimDataType, value: String, token: String): T
  abstract fun getValueIfRemove(currentValue: VimDataType, value: String, token: String): T
}

open class StringOption(name: String, abbrev: String, defaultValue: VimString, private val isList: Boolean = false, private val boundedValues: Collection<String>? = null) : Option<VimString>(name, abbrev, defaultValue) {
  constructor(name: String, abbrev: String, defaultValue: String, isList: Boolean = false, boundedValues: Collection<String>? = null) : this(name, abbrev, VimString(defaultValue), isList, boundedValues)

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimString) {
      throw ExException("E474: Invalid argument: $token")
    }

    if (value.value.isEmpty()) {
      return
    }

    if (boundedValues != null && split(value.value).any { !boundedValues.contains(it) }) {
      throw ExException("E474: Invalid argument: $token")
    }
  }

  override fun getValueIfAppend(currentValue: VimDataType, value: String, token: String): VimString {
    val currentString = (currentValue as VimString).value
    if (split(currentString).contains(value)) return currentValue
    return VimString(joinValues(currentString, value))
  }

  override fun getValueIfPrepend(currentValue: VimDataType, value: String, token: String): VimString {
    val currentString = (currentValue as VimString).value
    if (split(currentString).contains(value)) return currentValue
    return VimString(joinValues(value, currentString))
  }

  override fun getValueIfRemove(currentValue: VimDataType, value: String, token: String): VimString {
    val currentValueAsString = (currentValue as VimString).value
    val newValue = if (isList) {
      val valuesToRemove = split(value)
      val elements = split(currentValueAsString).toMutableList()
      if (Collections.indexOfSubList(elements, valuesToRemove) != -1) {
        // see `:help set`
        // When the option is a list of flags, {value} must be
        // exactly as they appear in the option.  Remove flags
        // one by one to avoid problems.
        elements.removeAll(valuesToRemove)
      }
      elements.joinToString(separator = ",")
    } else {
      currentValueAsString.replace(value, "")
    }
    return VimString(newValue)
  }

  open fun split(value: String): List<String> {
    return if (isList) {
      value.split(",")
    } else {
      listOf(value)
    }
  }

  private fun joinValues(first: String, second: String): String {
    val separator = if (isList && first.isNotEmpty()) "," else ""
    return first + separator + second
  }
}

open class UnsignedNumberOption(name: String, abbrev: String, defaultValue: VimInt) :
  NumberOption(name, abbrev, defaultValue) {

  constructor(name: String, abbrev: String, defaultValue: Int) : this(name, abbrev, VimInt(defaultValue))

  override fun checkIfValueValid(value: VimDataType, token: String) {
    super.checkIfValueValid(value, token)
    if ((value as VimInt).value < 0) {
      throw ExException("E487: Argument must be positive: $token")
    }
  }
}
