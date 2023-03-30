/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionChangeListener
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

public abstract class VimOptionGroupBase : VimOptionGroup {
  private val globalOptionsAccessor = GlobalOptions()
  private val globalValues = mutableMapOf<String, VimDataType>()
  private val localOptionsKey = Key<MutableMap<String, VimDataType>>("localOptions")

  override fun initialiseOptions() {
    Options.initialise()
  }

  override fun <T : VimDataType> getOptionValue(option: Option<T>, scope: OptionScope): T {
    return when (scope) {
      is OptionScope.LOCAL -> getLocalOptionValue(option, scope.editor)
      is OptionScope.GLOBAL -> getGlobalOptionValue(option)
    }
  }

  override fun <T : VimDataType> setOptionValue(option: Option<T>, scope: OptionScope, value: T) {
    option.checkIfValueValid(value, value.asString())

    val oldValue = getOptionValue(option, scope)
    when (scope) {
      is OptionScope.LOCAL -> setLocalOptionValue(option.name, value, scope.editor)
      is OptionScope.GLOBAL -> setGlobalOptionValue(option.name, value)
    }
    option.onChanged(scope, oldValue)
  }

  override fun getOption(key: String): Option<VimDataType>? = Options.getOption(key)
  override fun getAllOptions(): Set<Option<VimDataType>> = Options.getAllOptions()

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

  override fun <T : VimDataType> addListener(
    option: Option<T>,
    listener: OptionChangeListener<T>,
    executeOnAdd: Boolean
  ) {
    option.addOptionChangeListener(listener)
    if (executeOnAdd) {
      listener.processGlobalValueChange(getGlobalOptionValue(option))
    }
  }

  override fun <T : VimDataType> removeListener(option: Option<T>, listener: OptionChangeListener<T>) {
    option.removeOptionChangeListener(listener)
  }

  final override fun <T : VimDataType> overrideDefaultValue(option: Option<T>, newDefaultValue: T) {
    option.overrideDefaultValue(newDefaultValue)
  }

  override fun getGlobalOptions(): GlobalOptions = globalOptionsAccessor

  override fun getEffectiveOptions(editor: VimEditor): EffectiveOptions = EffectiveOptions(OptionScope.LOCAL(editor))
}
