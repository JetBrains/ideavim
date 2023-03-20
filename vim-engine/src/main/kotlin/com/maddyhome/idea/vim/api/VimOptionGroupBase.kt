/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.helper.StrictMode
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionChangeListener
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.OptionValueAccessor
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

public abstract class VimOptionGroupBase : VimOptionGroup {
  private val globalValues = mutableMapOf<String, VimDataType>()
  private val localOptionsKey = Key<MutableMap<String, VimDataType>>("localOptions")
  private val globalOptionValueAccessor by lazy { OptionValueAccessor(this, OptionScope.GLOBAL) }

  override fun <T : VimDataType> getOptionValue(option: Option<T>, scope: OptionScope): T {
    return when (scope) {
      is OptionScope.LOCAL -> getLocalOptionValue(option, scope.editor)
      is OptionScope.GLOBAL -> getGlobalOptionValue(option)
    }
  }

  override fun setOptionValue(option: Option<out VimDataType>, scope: OptionScope, value: VimDataType) {
    // Should always be called with the correct value type, either because code already knows the option, or because
    // the :set command has already parsed the incoming string into the correct type
    StrictMode.assert(option.defaultValue::class == value::class, "Incorrect datatype! Expected ${option.defaultValue::class} got ${value::class}")

    // TODO: Convert this to an assert. The value should already be a valid value
    option.checkIfValueValid(value, value.asString())

    val oldValue = getOptionValue(option, scope)
    when (scope) {
      is OptionScope.LOCAL -> setLocalOptionValue(option.name, value, scope.editor)
      is OptionScope.GLOBAL -> setGlobalOptionValue(option.name, value)
    }
    option.onChanged(scope, oldValue)
  }

  override fun getOption(key: String): Option<out VimDataType>? = Options.getOption(key)
  override fun getAllOptions(): Set<Option<out VimDataType>> = Options.getAllOptions()

  private fun setGlobalOptionValue(optionName: String, value: VimDataType) {
    globalValues[optionName] = value
  }

  private fun getLocalOptions(editor: VimEditor): MutableMap<String, VimDataType> {
    val storageService = injector.vimStorageService
    val storedData = storageService.getDataFromEditor(editor, localOptionsKey)
    if (storedData != null) {
      return storedData
    }
    val localOptions = mutableMapOf<String, VimDataType>()
    storageService.putDataToEditor(editor, localOptionsKey, localOptions)
    return localOptions
  }

  private fun setLocalOptionValue(optionName: String, value: VimDataType, editor: VimEditor) {
    val localOptions = getLocalOptions(editor)
    localOptions[optionName] = value
  }

  private fun <T : VimDataType> getGlobalOptionValue(option: Option<T>): T {
    // We know that the datatype is correct, because we added it via the same strongly typed option
    @Suppress("UNCHECKED_CAST")
    return globalValues[option.name] as? T ?: option.defaultValue
  }

  private fun <T : VimDataType> getLocalOptionValue(option: Option<T>, editor: VimEditor): T {
    val localOptions = getLocalOptions(editor)
    // Again, we know this cast is safe because we added it with the same strongly typed option
    @Suppress("UNCHECKED_CAST")
    return localOptions[option.name] as? T ?: getGlobalOptionValue(option)
  }

  override fun resetAllOptions() {
    globalValues.clear()
    injector.editorGroup.localEditors()
      .forEach { injector.vimStorageService.getDataFromEditor(it, localOptionsKey)?.clear() }
  }

  override fun addOption(option: Option<out VimDataType>) {
    Options.addOption(option)
  }

  override fun removeOption(optionName: String) {
    Options.removeOption(optionName)
  }

  override fun addListener(optionName: String, listener: OptionChangeListener<VimDataType>, executeOnAdd: Boolean) {
    val option = Options.getOption(optionName)!!
    option.addOptionChangeListener(listener)
    if (executeOnAdd) {
      listener.processGlobalValueChange(getGlobalOptionValue(option))
    }
  }

  override fun removeListener(optionName: String, listener: OptionChangeListener<VimDataType>) {
    Options.getOption(optionName)!!.removeOptionChangeListener(listener)
  }

  override fun getValueAccessor(editor: VimEditor?): OptionValueAccessor =
    if (editor == null) globalOptionValueAccessor else OptionValueAccessor(this, OptionScope.LOCAL(editor))
}
