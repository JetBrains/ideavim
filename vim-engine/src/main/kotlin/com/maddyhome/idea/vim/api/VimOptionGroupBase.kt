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
  private val globalParsedValues = mutableMapOf<String, Any>()
  private val localOptionsKey = Key<MutableMap<String, VimDataType>>("localOptions")
  private val parsedEffectiveValueKey = Key<MutableMap<String, Any>>("parsedEffectiveOptionValues")

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

    // TODO: We should only be storing, and therefore clearing, the cached data for the effective value
    // We should introduce OptionScope.AUTO, that sets the option value at the appropriate scope - locally for local
    // options and globally for global options. We would clear (and create) the parsed value only for AUTO
    val oldValue = getOptionValue(option, scope)
    when (scope) {
      is OptionScope.LOCAL -> {
        setLocalOptionValue(option.name, value, scope.editor)
        injector.vimStorageService.getDataFromEditor(scope.editor, parsedEffectiveValueKey)?.remove(option.name)
      }
      is OptionScope.GLOBAL -> {
        setGlobalOptionValue(option.name, value)
        globalParsedValues.remove(option.name)
      }
    }

    option.onChanged(scope, oldValue)
  }

  override fun <T : VimDataType, TData : Any> getParsedEffectiveOptionValue(
    option: Option<T>,
    scope: OptionScope,
    provider: (T) -> TData,
  ): TData {
    // TODO: Introduce OptionDeclaredScope so that we know what each option's effective scope is
    // This will allow us to set values to the correct scope via OptionScope.AUTO
//    StrictMode.assert(
//      option.declaredScope != OptionDeclaredScope.GLOBAL && editor != null,
//      "Editor must be supplied unless option's declared scope is global"
//    )
    val cachedValues = when (scope) {
      is OptionScope.GLOBAL -> globalParsedValues
      is OptionScope.LOCAL -> {
        injector.vimStorageService.getDataFromEditor(scope.editor, parsedEffectiveValueKey)
          ?: mutableMapOf<String, Any>().also {
            injector.vimStorageService.putDataToEditor(scope.editor, parsedEffectiveValueKey, it)
          }
      }
    }

    // Unless the user is calling this method multiple times with different providers, we can be confident this cast
    // will succeed
    @Suppress("UNCHECKED_CAST")
    return cachedValues.getOrPut(option.name) { provider(getOptionValue(option, scope)) } as TData
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
    // TODO: This should be split into two functions. One for testing that resets everything, and one for `:set alL&`
    // which should only reset the global options, and the options for the current editor
    globalValues.clear()
    injector.editorGroup.localEditors().forEach {
      injector.vimStorageService.getDataFromEditor(it, localOptionsKey)?.clear()
      injector.vimStorageService.getDataFromEditor(it, parsedEffectiveValueKey)?.clear()
    }
    globalParsedValues.clear()
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
