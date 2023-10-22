/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.StringListOption
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Base class to provide mechanisms to delegate properties to get/set option values
 */
public abstract class OptionsPropertiesBase(private val scope: OptionAccessScope) {
  /**
   * Provide a delegate property to get/set boolean option values
   */
  private inner class ToggleOptionProperty(private val option: ToggleOption) :
    ReadWriteProperty<OptionsPropertiesBase, Boolean> {

    override fun getValue(thisRef: OptionsPropertiesBase, property: KProperty<*>) = getOptionValue(option).asBoolean()
    override fun setValue(thisRef: OptionsPropertiesBase, property: KProperty<*>, value: Boolean) =
      setOptionValue(option, value.asVimInt())
  }

  /**
   * Provide a delegate property to get/set int option values
   */
  private inner class NumberOptionProperty(private val option: NumberOption) :
    ReadWriteProperty<OptionsPropertiesBase, Int> {

    override fun getValue(thisRef: OptionsPropertiesBase, property: KProperty<*>) = getOptionValue(option).value
    override fun setValue(thisRef: OptionsPropertiesBase, property: KProperty<*>, value: Int) =
      setOptionValue(option, VimInt(value))
  }

  /**
   * Provide a delegate property to get/set string option values
   */
  private inner class StringOptionProperty(private val option: StringOption) :
    ReadWriteProperty<OptionsPropertiesBase, String> {

    override fun getValue(thisRef: OptionsPropertiesBase, property: KProperty<*>) = getOptionValue(option).value
    override fun setValue(thisRef: OptionsPropertiesBase, property: KProperty<*>, value: String) =
      setOptionValue(option, VimString(value))
  }

  /**
   * Provide a delegate property to get, query and modify string list options
   *
   * Note that this is a read only property delegate. It does not return a plain string, but a [StringListOptionValue]
   * which has functions to append or remove values, as well as check for the existence of a value.
   */
  private inner class StringListOptionProperty(private val option: StringListOption) :
    ReadOnlyProperty<OptionsPropertiesBase, StringListOptionValue> {

    override fun getValue(thisRef: OptionsPropertiesBase, property: KProperty<*>) =
      StringListOptionValue(option, scope)
  }

  private fun <T : VimDataType> getOptionValue(option: Option<T>) =
    injector.optionGroup.getOptionValue(option, scope)

  private fun <T : VimDataType> setOptionValue(option: Option<T>, value: T) =
    injector.optionGroup.setOptionValue(option, scope, value)

  // Note that if StringOption and StringListOption were combined, we'd lose the simple overloaded API here, and we'd
  // have to create delegated properties directly:
  // val foo: String by optionProperty(myStringOption)
  // val bar: StringListOptionValue by optionProperty(myStringListOption)
  // vs
  // val foo: String = StringOptionProperty(myStringOption, scope)
  // val bar: StringListOptionValue = StringListOptionProperty(myStringListOption, scope)
  // This is arguably simpler, and StringListOption gives us more type safety about using a string vs a list
  protected fun optionProperty(option: ToggleOption): ReadWriteProperty<OptionsPropertiesBase, Boolean> =
    ToggleOptionProperty(option)

  protected fun optionProperty(option: NumberOption): ReadWriteProperty<OptionsPropertiesBase, Int> =
    NumberOptionProperty(option)

  protected fun optionProperty(option: StringOption): ReadWriteProperty<OptionsPropertiesBase, String> =
    StringOptionProperty(option)

  protected fun optionProperty(option: StringListOption): ReadOnlyProperty<OptionsPropertiesBase, StringListOptionValue> =
    StringListOptionProperty(option)
}

/**
 * Provides a class to work with a string list option
 *
 * It provides functions to modify the list as well as get the actual string value. It also implements `List<String>`,
 * so can be iterated, and [List.contains] can be used to check for existence of an item.
 *
 * Note that this class should be short-lived and not cached. It assumes the underlying option value is not changed
 * except via its own methods!
 */
public class StringListOptionValue(
  private val option: StringListOption,
  private val scope: OptionAccessScope
) : AbstractList<String>() {

  // We cache the value at creation time, and update whenever it's changed via one of its own methods. We lazily fetch
  // and cache the string list. This class does not expect the value to be changed behind its back!
  private var currentValue: VimString = injector.optionGroup.getOptionValue(option, scope)
  private var stringListValues: List<String>? = null

  public val value: String
    get() = currentValue.value

  public fun appendValue(value: String) {
    val parsedValue = option.parseValue(value, value)
    val newValue = option.appendValue(currentValue, parsedValue)
    injector.optionGroup.setOptionValue(option, scope, newValue)
    currentValue = newValue
    stringListValues = null
  }

  public fun prependValue(value: String) {
    val parsedValue = option.parseValue(value, value)
    val newValue = option.prependValue(currentValue, parsedValue)
    injector.optionGroup.setOptionValue(option, scope, newValue)
    currentValue = newValue
    stringListValues = null
  }

  public fun removeValue(value: String) {
    val parsedValue = option.parseValue(value, value)
    val newValue = option.removeValue(currentValue, parsedValue)
    injector.optionGroup.setOptionValue(option, scope, newValue)
    currentValue = newValue
    stringListValues = null
  }

  override val size: Int
    get() = getStringListValues().size

  override fun get(index: Int): String = getStringListValues()[index]

  private fun getStringListValues() =
    stringListValues ?: injector.optionGroup.getStringListValues(option, scope).also { stringListValues = it }
}
