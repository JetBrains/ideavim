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
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW
import com.maddyhome.idea.vim.options.OptionDeclaredScope.LOCAL_TO_BUFFER
import com.maddyhome.idea.vim.options.OptionDeclaredScope.LOCAL_TO_WINDOW
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
    val editor = when (scope) {
      is OptionScope.LOCAL -> scope.editor
      OptionScope.GLOBAL -> return getGlobalOptionValue(option)
    }

    return when (option.declaredScope) {
      GLOBAL -> getGlobalOptionValue(option)
      LOCAL_TO_BUFFER -> getBufferLocalOptionValue(option, editor)
      LOCAL_TO_WINDOW -> getWindowLocalOptionValue(option, editor)
      GLOBAL_OR_LOCAL_TO_BUFFER -> tryGetBufferLocalOptionValue(option, editor) ?: getGlobalOptionValue(option)
      GLOBAL_OR_LOCAL_TO_WINDOW -> tryGetWindowLocalOptionValue(option, editor) ?: getGlobalOptionValue(option)
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
        when (option.declaredScope) {
          GLOBAL -> {
            setGlobalOptionValue(option, value)
            globalParsedValues.remove(option.name)
          }
          LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_BUFFER -> {
            setBufferLocalOptionValue(option, scope.editor, value)
            injector.vimStorageService.getDataFromBuffer(scope.editor, parsedEffectiveValueKey)?.remove(option.name)
          }
          LOCAL_TO_WINDOW, GLOBAL_OR_LOCAL_TO_WINDOW -> {
            setWindowLocalOptionValue(option, scope.editor, value)
            injector.vimStorageService.getDataFromEditor(scope.editor, parsedEffectiveValueKey)?.remove(option.name)
          }
        }
      }
      is OptionScope.GLOBAL -> {
        setGlobalOptionValue(option, value)
        globalParsedValues.remove(option.name)
      }
    }

    option.onChanged(scope, oldValue)
  }

  override fun <T : VimDataType, TData : Any> getParsedEffectiveOptionValue(
    option: Option<T>,
    editor: VimEditor?,
    provider: (T) -> TData,
  ): TData {
    // TODO: We can't correctly clear global-local options
    // We have to cache global-local values locally, because they can be set locally. But if they're not overridden
    // locally, we would cache a global value per-window. When the global value is changed with OptionScope.GLOBAL, we
    // are unable to clear the per-window cached value, so windows would end up with stale cached (global) values.
    check(option.declaredScope != GLOBAL_OR_LOCAL_TO_WINDOW
      && option.declaredScope != GLOBAL_OR_LOCAL_TO_BUFFER
    ) { "Global-local options cannot currently be cached" }

    val cachedValues = if (option.declaredScope == GLOBAL) {
      globalParsedValues
    }
    else {
      // Note that for simplicity, we cache all local values per window, even local-to-buffer values
      check(editor != null) { "Editor must be supplied for local options" }
      injector.vimStorageService.getOrPutEditorData(editor, parsedEffectiveValueKey) { mutableMapOf() }
    }

    // Unless the user is calling this method multiple times with different providers, we can be confident this cast
    // will succeed
    @Suppress("UNCHECKED_CAST")
    return cachedValues.getOrPut(option.name) {
      provider(getOptionValue(option, if (editor == null) OptionScope.GLOBAL else OptionScope.LOCAL(editor)))
    } as TData
  }

  override fun getOption(key: String): Option<VimDataType>? = Options.getOption(key)
  override fun getAllOptions(): Set<Option<VimDataType>> = Options.getAllOptions()

  override fun resetAllOptions() {
    // TODO: This should be split into two functions. One for testing that resets everything, and one for `:set alL&`
    // which should only reset the global options, and the options for the current editor
    globalValues.clear()
    injector.editorGroup.localEditors().forEach {
      injector.vimStorageService.getDataFromBuffer(it, localOptionsKey)?.clear()
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


  private fun <T : VimDataType> getGlobalOptionValue(option: Option<T>): T {
    // We set the value via Option<T> so it's safe to cast to T
    @Suppress("UNCHECKED_CAST")
    return globalValues[option.name] as? T ?: option.defaultValue
  }

  private fun <T : VimDataType> setGlobalOptionValue(option: Option<T>, value: T) {
    globalValues[option.name] = value
  }


  private fun getBufferLocalOptionStorage(editor: VimEditor) =
    injector.vimStorageService.getOrPutBufferData(editor, localOptionsKey) { mutableMapOf() }

  private fun <T : VimDataType> setBufferLocalOptionValue(option: Option<T>, editor: VimEditor, value: T) {
    val options = getBufferLocalOptionStorage(editor)
    options[option.name] = value
  }

  private fun <T : VimDataType> getBufferLocalOptionValue(option: Option<T>, editor: VimEditor): T {
    check(option.declaredScope == LOCAL_TO_BUFFER || option.declaredScope == GLOBAL_OR_LOCAL_TO_BUFFER)
    // TODO: Once we initialise the options during buffer creation, we should never get null here
    // What about options that are added dynamically, e.g. extensions?
    return tryGetBufferLocalOptionValue(option, editor) ?: getGlobalOptionValue(option)
  }

  private fun <T : VimDataType> tryGetBufferLocalOptionValue(option: Option<T>, editor: VimEditor): T? {
    val options = getBufferLocalOptionStorage(editor)

    // We set the value via Option<T> so it's safe to cast to T
    @Suppress("UNCHECKED_CAST")
    return options[option.name] as? T
  }


  private fun getWindowLocalOptionStorage(editor: VimEditor) =
    injector.vimStorageService.getOrPutEditorData(editor, localOptionsKey) { mutableMapOf() }

  private fun <T : VimDataType> setWindowLocalOptionValue(option: Option<T>, editor: VimEditor, value: T) {
    val options = getWindowLocalOptionStorage(editor)
    options[option.name] = value
  }

  private fun <T : VimDataType> getWindowLocalOptionValue(option: Option<T>, editor: VimEditor): T {
    check(option.declaredScope == LOCAL_TO_WINDOW || option.declaredScope == GLOBAL_OR_LOCAL_TO_WINDOW)
    // TODO: Once we initialise the options during window creation, we should never get null here
    return tryGetWindowLocalOptionValue(option, editor) ?: getGlobalOptionValue(option)
  }

  private fun <T : VimDataType> tryGetWindowLocalOptionValue(option: Option<T>, editor: VimEditor): T? {
    val options = getWindowLocalOptionStorage(editor)

    // We set the value via Option<T> so it's safe to cast to T
    @Suppress("UNCHECKED_CAST")
    return options[option.name] as? T
  }
}
