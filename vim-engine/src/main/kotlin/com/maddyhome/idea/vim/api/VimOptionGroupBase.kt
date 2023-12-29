/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.options.EffectiveOptionValueChangeListener
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW
import com.maddyhome.idea.vim.options.OptionDeclaredScope.LOCAL_TO_BUFFER
import com.maddyhome.idea.vim.options.OptionDeclaredScope.LOCAL_TO_WINDOW
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

public abstract class VimOptionGroupBase : VimOptionGroup {
  private val storage = OptionStorage()
  private val listeners by lazy { OptionListenersImpl(this, injector.editorGroup) }
  private val parsedValuesCache by lazy { ParsedValuesCache(this, injector.vimStorageService) }

  override fun initialiseOptions() {
    Options.initialise()
  }

  override fun initialiseLocalOptions(editor: VimEditor, sourceEditor: VimEditor?, scenario: LocalOptionInitialisationScenario) {
    // Called for all editors. Ensures that we eagerly initialise all local values, including the per-window "global"
    // values of local-to-window options. Note that global options are not eagerly initialised - the value is the
    // default value unless explicitly set.
    when (scenario) {
      // We're initialising the first (hidden) editor. Vim always has at least one window (and buffer); IdeaVim doesn't.
      // We fake this with a hidden editor that is created when the plugin first starts. It is used to capture state
      // when initially evaluating `~/.ideavimrc`, and then used to initialise subsequent windows. But first, we must
      // initialise all local options to the default (global) values
      LocalOptionInitialisationScenario.DEFAULTS -> {
        check(sourceEditor == null) { "sourceEditor must be null for DEFAULTS scenario" }
        initialisePerWindowGlobalValues(editor)
        initialiseLocalToBufferOptions(editor)
        initialiseLocalToWindowOptions(editor)
      }

      // The opening window is either:
      // a) the fallback window used to evaluate `~/.ideavimrc` during initialisation and potentially before any windows
      //    are open or:
      // b) the "ex" or search command line text field/editor associated with a main editor
      // Either way, the target window should be a clone of the source window, copying local to buffer and local to
      // window values
      LocalOptionInitialisationScenario.FALLBACK,
      LocalOptionInitialisationScenario.CMD_LINE -> {
        check(sourceEditor != null) { "sourceEditor must not be null for FALLBACK or CMD_LINE scenarios" }
        copyPerWindowGlobalValues(editor, sourceEditor)
        copyLocalToBufferLocalValues(editor, sourceEditor)
        copyLocalToWindowLocalValues(editor, sourceEditor)
      }

      // The opening/current window is being split. Clone the local-to-window options, both the local values and the
      // per-window "global" values. The buffer local options are obviously already initialised
      LocalOptionInitialisationScenario.SPLIT -> {
        check(sourceEditor != null) { "sourceEditor must not be null for SPLIT scenario" }
        copyPerWindowGlobalValues(editor, sourceEditor)
        initialiseLocalToBufferOptions(editor)  // It's a split, they should already be initialised
        copyLocalToWindowLocalValues(editor, sourceEditor)
      }

      // Editing a new buffer in the current window (`:edit {file}`). Remove explicitly set local values, which means to
      // copy the per-window "global" value of local-to-window options to the local value, and to reset all window
      // global-local options. Since it's a new buffer, we initialise buffer local options.
      // Note that IdeaVim does not use this scenario for `:edit {file}` because the current implementation will always
      // open a new window. It does use it for preview tabs and reusing unmodified tabs
      LocalOptionInitialisationScenario.EDIT -> {
        check(sourceEditor != null) { "sourceEditor must not be null for EDIT scenario" }
        copyPerWindowGlobalValues(editor, sourceEditor)
        initialiseLocalToBufferOptions(editor)
        resetLocalToWindowOptions(editor)
      }

      // Editing a new buffer in a new window (`:new {file}`). Vim treats this as a split followed by an edit. That
      // means, clone the window, then reset its local values to its global values
      LocalOptionInitialisationScenario.NEW -> {
        check(sourceEditor != null) { "sourceEditor must not be null for NEW scenario" }
        copyPerWindowGlobalValues(editor, sourceEditor)
        initialiseLocalToBufferOptions(editor)
        copyLocalToWindowLocalValues(editor, sourceEditor)  // Technically redundant
        resetLocalToWindowOptions(editor)
      }
    }
  }

  private fun copyLocalToBufferLocalValues(targetEditor: VimEditor, sourceEditor: VimEditor) {
    val sourceLocalScope = OptionAccessScope.LOCAL(sourceEditor)
    val targetLocalScope = OptionAccessScope.LOCAL(targetEditor)
    getAllOptions().forEach { option ->
      if (option.declaredScope == LOCAL_TO_BUFFER || option.declaredScope == GLOBAL_OR_LOCAL_TO_BUFFER) {
        val value = storage.getOptionValue(option, sourceLocalScope)
        storage.setOptionValue(option, targetLocalScope, value)
      }
    }
  }

  /**
   * Initialise local-to-buffer options by copying the global value
   *
   * Note that the buffer might have been previously initialised. The global value is the most recently set value,
   * across any buffer and any window. This makes most sense for non-visible options - the user always gets what they
   * last set, regardless of where it was set.
   *
   * Remember that `:set` on a buffer-local option will set both the local value and the global value.
   */
  private fun initialiseLocalToBufferOptions(editor: VimEditor) {
    if (!storage.isLocalToBufferOptionStorageInitialised(editor)) {
      val globalScope = OptionAccessScope.GLOBAL(editor)
      val localScope = OptionAccessScope.LOCAL(editor)
      getAllOptions().forEach { option ->
        if (option.declaredScope == LOCAL_TO_BUFFER) {
          val value = storage.getOptionValue(option, globalScope)
          storage.setOptionValue(option, localScope, value)
        } else if (option.declaredScope == GLOBAL_OR_LOCAL_TO_BUFFER) {
          storage.setOptionValue(option, localScope, option.unsetValue)
        }
      }
    }
  }

  protected fun copyPerWindowGlobalValues(targetEditor: VimEditor, sourceEditor: VimEditor) {
    val sourceGlobalScope = OptionAccessScope.GLOBAL(sourceEditor)
    val targetGlobalScope = OptionAccessScope.GLOBAL(targetEditor)
    getAllOptions().forEach { option ->
      if (option.declaredScope == LOCAL_TO_WINDOW) {
        val localValue = storage.getOptionValue(option, sourceGlobalScope)
        storage.setOptionValue(option, targetGlobalScope, localValue)
      }
    }
  }

  private fun initialisePerWindowGlobalValues(targetEditor: VimEditor) {
    val targetGlobalScope = OptionAccessScope.GLOBAL(targetEditor)
    getAllOptions().forEach { option ->
      if (option.declaredScope == LOCAL_TO_WINDOW) {
        storage.setOptionValue(option, targetGlobalScope, option.defaultValue)
      }
    }
  }

  private fun copyLocalToWindowLocalValues(targetEditor: VimEditor, sourceEditor: VimEditor) {
    val sourceLocalScope = OptionAccessScope.LOCAL(sourceEditor)
    val targetLocalScope = OptionAccessScope.LOCAL(targetEditor)
    getAllOptions().forEach { option ->
      if (option.declaredScope == LOCAL_TO_WINDOW || option.declaredScope == GLOBAL_OR_LOCAL_TO_WINDOW) {
        val value = storage.getOptionValue(option, sourceLocalScope)
        storage.setOptionValue(option, targetLocalScope, value)
      }
    }
  }

  private fun resetLocalToWindowOptions(editor: VimEditor) = initialiseLocalToWindowOptions(editor)

  private fun initialiseLocalToWindowOptions(editor: VimEditor) {
    val globalScope = OptionAccessScope.GLOBAL(editor)
    val localScope = OptionAccessScope.LOCAL(editor)
    getAllOptions().forEach { option ->
      if (option.declaredScope == LOCAL_TO_WINDOW) {
        val value = storage.getOptionValue(option, globalScope)
        storage.setOptionValue(option, localScope, value)
      }
      else if (option.declaredScope == GLOBAL_OR_LOCAL_TO_WINDOW) {
        storage.setOptionValue(option, localScope, option.unsetValue)
      }
    }
  }

  override fun <T : VimDataType> getOptionValue(option: Option<T>, scope: OptionAccessScope): T =
    storage.getOptionValue(option, scope)

  override fun <T : VimDataType> setOptionValue(option: Option<T>, scope: OptionAccessScope, value: T) {
    option.checkIfValueValid(value, value.asString())

    when (scope) {
      is OptionAccessScope.EFFECTIVE -> {
        if (storage.setOptionValue(option, scope, value)) {
          parsedValuesCache.reset(option, scope.editor)
          listeners.onEffectiveValueChanged(option, scope.editor)
        }
      }
      is OptionAccessScope.LOCAL -> {
        if (storage.setOptionValue(option, scope, value)) {
          parsedValuesCache.reset(option, scope.editor)
          listeners.onLocalValueChanged(option, scope.editor)
        }
      }
      is OptionAccessScope.GLOBAL -> {
        if (storage.setOptionValue(option, scope, value)) {
          if (option.declaredScope == GLOBAL) {
            // Don't reset the parsed effective value if we change the global value of local options
            parsedValuesCache.reset(option, scope.editor)
          }
          listeners.onGlobalValueChanged(option)
        }
      }
    }
  }

  override fun <T : VimDataType, TData : Any> getParsedEffectiveOptionValue(
    option: Option<T>,
    editor: VimEditor?,
    provider: (T) -> TData,
  ): TData {
    return parsedValuesCache.getParsedEffectiveOptionValue(option, editor, provider)
  }

  override fun getOption(key: String): Option<VimDataType>? = Options.getOption(key)
  override fun getAllOptions(): Set<Option<VimDataType>> = Options.getAllOptions()

  override fun resetAllOptions(editor: VimEditor) {
    // Reset all options to default values at global and local scope. This will fire any listeners and clear any caches
    Options.getAllOptions().forEach { option ->
      resetDefaultValue(option, OptionAccessScope.GLOBAL(editor))
      when (option.declaredScope) {
        GLOBAL -> {}
        LOCAL_TO_BUFFER, LOCAL_TO_WINDOW -> resetDefaultValue(option, OptionAccessScope.LOCAL(editor))
        GLOBAL_OR_LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_WINDOW -> {
          setOptionValue(option, OptionAccessScope.LOCAL(editor), option.unsetValue)
        }
      }
    }
  }

  override fun resetAllOptionsForTesting() {
    // Resets the global values of all options, including per-window global of the fallback window. Also resets the
    // local options of the fallback window. When combined with resetting all options for any open editors, this resets
    // all options everywhere.
    resetAllOptions(injector.fallbackWindow)

    // During testing, we do not expect to have any editors, so this collection is usually empty
    injector.editorGroup.localEditors().forEach { resetAllOptions(it) }
  }

  override fun addOption(option: Option<out VimDataType>) {
    Options.addOption(option)
    initialiseNewOptionDefaultValues(option)
  }

  override fun removeOption(optionName: String) {
    Options.removeOption(optionName)
    listeners.removeAllListeners(optionName)
  }

  override fun <T : VimDataType> addGlobalOptionChangeListener(
    option: Option<T>,
    listener: GlobalOptionChangeListener
  ) {
    check(option.declaredScope == GLOBAL)
    listeners.addGlobalOptionChangeListener(option.name, listener)
  }

  override fun <T : VimDataType> removeGlobalOptionChangeListener(
    option: Option<T>,
    listener: GlobalOptionChangeListener
  ) {
    listeners.removeGlobalOptionChangeListener(option.name, listener)
  }

  override fun <T : VimDataType> addEffectiveOptionValueChangeListener(
    option: Option<T>,
    listener: EffectiveOptionValueChangeListener
  ) {
    listeners.addEffectiveOptionValueChangeListener(option.name, listener)
  }

  override fun <T : VimDataType> removeEffectiveOptionValueChangeListener(
    option: Option<T>,
    listener: EffectiveOptionValueChangeListener
  ) {
    listeners.removeEffectiveOptionValueChangeListener(option.name, listener)
  }

  final override fun <T : VimDataType> overrideDefaultValue(option: Option<T>, newDefaultValue: T) {
    option.overrideDefaultValue(newDefaultValue)
  }

  // We can pass null as the editor because we are only accessing global options
  override fun getGlobalOptions(): GlobalOptions = GlobalOptions(OptionAccessScope.GLOBAL(null))

  override fun getEffectiveOptions(editor: VimEditor): EffectiveOptions = EffectiveOptions(OptionAccessScope.EFFECTIVE(editor))


  private fun <T : VimDataType> initialiseNewOptionDefaultValues(option: Option<T>) {
    if (option.declaredScope != LOCAL_TO_WINDOW) {
      storage.setOptionValue(option, OptionAccessScope.GLOBAL(null), option.defaultValue)
    }
    injector.editorGroup.localEditors().forEach { editor ->
      when (option.declaredScope) {
        GLOBAL -> { }
        LOCAL_TO_BUFFER,
        LOCAL_TO_WINDOW -> storage.setOptionValue(option, OptionAccessScope.LOCAL(editor), option.defaultValue)
        GLOBAL_OR_LOCAL_TO_BUFFER,
        GLOBAL_OR_LOCAL_TO_WINDOW -> storage.setOptionValue(option, OptionAccessScope.LOCAL(editor), option.unsetValue)
      }
    }
  }
}


/**
 * Maintains storage of, and provides accessors to option values
 *
 * This class does not notify any listeners of changes, but provides enough information for a caller to handle this.
 */
private class OptionStorage {
  private val globalValues = mutableMapOf<String, VimDataType>()
  private val perWindowGlobalOptionsKey = Key<MutableMap<String, VimDataType>>("perWindowGlobalOptions")
  private val localOptionsKey = Key<MutableMap<String, VimDataType>>("localOptions")

  fun <T : VimDataType> getOptionValue(option: Option<T>, scope: OptionAccessScope): T = when (scope) {
    is OptionAccessScope.EFFECTIVE -> getEffectiveValue(option, scope.editor)
    is OptionAccessScope.GLOBAL -> getGlobalValue(option, scope.editor)
    is OptionAccessScope.LOCAL -> getLocalValue(option, scope.editor)
  }

  fun <T : VimDataType> setOptionValue(option: Option<T>, scope: OptionAccessScope, value: T): Boolean {
    return when (scope) {
      is OptionAccessScope.EFFECTIVE -> setEffectiveValue(option, scope.editor, value)
      is OptionAccessScope.GLOBAL -> setGlobalValue(option, scope.editor, value)
      is OptionAccessScope.LOCAL -> setLocalValue(option, scope.editor, value)
    }
  }

  fun isLocalToBufferOptionStorageInitialised(editor: VimEditor) =
    injector.vimStorageService.getDataFromBuffer(editor, localOptionsKey) != null

  private fun <T : VimDataType> getEffectiveValue(option: Option<T>, editor: VimEditor): T {
    return when (option.declaredScope) {
      GLOBAL -> getGlobalValue(option, editor)
      LOCAL_TO_BUFFER -> getLocalValue(option, editor)
      LOCAL_TO_WINDOW -> getLocalValue(option, editor)
      GLOBAL_OR_LOCAL_TO_BUFFER -> {
        getLocalValue(option, editor).takeUnless { it == option.unsetValue }
          ?: getGlobalValue(option, editor)
      }
      GLOBAL_OR_LOCAL_TO_WINDOW -> {
        getLocalValue(option, editor).takeUnless { it == option.unsetValue }
          ?: getGlobalValue(option, editor)
      }
    }
  }

  private fun <T : VimDataType> getGlobalValue(option: Option<T>, editor: VimEditor?): T {
    val values = if (option.declaredScope == LOCAL_TO_WINDOW) {
      check(editor != null) { "Editor must be provided for local options" }
      getPerWindowGlobalOptionStorage(editor)
    }
    else {
      globalValues
    }
    return getValue(values, option) ?: option.defaultValue
  }

  private fun <T : VimDataType> getLocalValue(option: Option<T>, editor: VimEditor): T {
    return when (option.declaredScope) {
      GLOBAL -> getGlobalValue(option, editor)
      LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_BUFFER -> getBufferLocalValue(option, editor)
      LOCAL_TO_WINDOW, GLOBAL_OR_LOCAL_TO_WINDOW -> getWindowLocalValue(option, editor)
    }
  }

  private fun <T : VimDataType> getBufferLocalValue(option: Option<T>, editor: VimEditor): T {
    val values = getBufferLocalOptionStorage(editor)
    val value = getValue(values, option)
    strictModeAssert(value != null) { "Unexpected uninitialised buffer local value: ${option.name}" }
    return value ?: getEmergencyFallbackLocalValue(option, editor)
  }

  private fun <T : VimDataType> getWindowLocalValue(option: Option<T>, editor: VimEditor): T {
    val values = getWindowLocalOptionStorage(editor)
    val value = getValue(values, option)
    strictModeAssert(value != null) { "Unexpected uninitialised window local value: ${option.name}" }
    return value ?: getEmergencyFallbackLocalValue(option, editor)
  }

  private fun <T : VimDataType> setEffectiveValue(option: Option<T>, editor: VimEditor, value: T): Boolean {
    return when (option.declaredScope) {
      GLOBAL -> setGlobalValue(option, editor, value)
      LOCAL_TO_BUFFER, LOCAL_TO_WINDOW -> setLocalValue(option, editor, value).also {
        setGlobalValue(option, editor, value)
      }
      GLOBAL_OR_LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_WINDOW -> {
        var changed = false
        if (getLocalValue(option, editor) != option.unsetValue) {
          changed = setLocalValue(option, editor, if (option is NumberOption || option is ToggleOption) value else option.unsetValue)
        }
        setGlobalValue(option, editor, value) || changed
      }
    }
  }

  private fun <T : VimDataType> setGlobalValue(option: Option<T>, editor: VimEditor?, value: T): Boolean {
    val values = if (option.declaredScope == LOCAL_TO_WINDOW) {
      check(editor != null) { "Editor must be provided for local options" }
      getPerWindowGlobalOptionStorage(editor)
    }
    else {
      globalValues
    }
    return setValue(values, option.name, value)
  }

  private fun <T : VimDataType> setLocalValue(option: Option<T>, editor: VimEditor, value: T): Boolean {
    return when (option.declaredScope) {
      GLOBAL -> setGlobalValue(option, editor, value)
      LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_BUFFER -> setBufferLocalValue(option, editor, value)
      LOCAL_TO_WINDOW, GLOBAL_OR_LOCAL_TO_WINDOW -> setWindowLocalValue(option, editor, value)
    }
  }

  private fun <T : VimDataType> setBufferLocalValue(option: Option<T>, editor: VimEditor, value: T): Boolean {
    val values = getBufferLocalOptionStorage(editor)
    return setValue(values, option.name, value)
  }

  private fun <T : VimDataType> setWindowLocalValue(option: Option<T>, editor: VimEditor, value: T): Boolean {
    val values = getWindowLocalOptionStorage(editor)
    return setValue(values, option.name, value)
  }

  private fun <T : VimDataType> getValue(values: MutableMap<String, VimDataType>, option: Option<T>): T? {
    // We can safely suppress this because we know we only set it with a strongly typed option and only get it with a
    // strongly typed option
    @Suppress("UNCHECKED_CAST")
    return values[option.name] as? T
  }

  private fun <T : VimDataType> setValue(values: MutableMap<String, VimDataType>, key: String, value: T): Boolean {
    val oldValue = values[key]
    if (oldValue != value) {
      values[key] = value
      return true
    }
    return false
  }

  private fun getPerWindowGlobalOptionStorage(editor: VimEditor) =
    injector.vimStorageService.getOrPutWindowData(editor, perWindowGlobalOptionsKey) { mutableMapOf() }

  private fun getBufferLocalOptionStorage(editor: VimEditor) =
    injector.vimStorageService.getOrPutBufferData(editor, localOptionsKey) { mutableMapOf() }

  private fun getWindowLocalOptionStorage(editor: VimEditor) =
    injector.vimStorageService.getOrPutWindowData(editor, localOptionsKey) { mutableMapOf() }

  /**
   * Provides a fallback value if the values map is unexpectedly empty
   *
   * The local-to-window or local-to-buffer values maps should never have a missing option, as we eagerly initialise all
   * local option values for each editor, but the map returns a nullable value, so let's just make sure we always have
   * a sensible fallback.
   */
  private fun <T : VimDataType> getEmergencyFallbackLocalValue(option: Option<T>, editor: VimEditor?): T {
    return if (option.declaredScope == GLOBAL_OR_LOCAL_TO_BUFFER || option.declaredScope == GLOBAL_OR_LOCAL_TO_WINDOW) {
      option.unsetValue
    }
    else {
      getGlobalValue(option, editor)
    }
  }

  // We can't use StrictMode.assert because it checks an option, which calls into VimOptionGroupBase...
  private inline fun strictModeAssert(condition: Boolean, lazyMessage: () -> String) {
    if (globalValues[Options.ideastrictmode.name]?.asBoolean() == true && !condition) {
      error(lazyMessage())
    }
  }
}


private class OptionListenersImpl(private val optionGroup: VimOptionGroup, private val editorGroup: VimEditorGroup) {
  private val globalOptionListeners = MultiSet<String, GlobalOptionChangeListener>()
  private val effectiveOptionValueListeners = MultiSet<String, EffectiveOptionValueChangeListener>()

  fun addGlobalOptionChangeListener(optionName: String, listener: GlobalOptionChangeListener) {
    globalOptionListeners.add(optionName, listener)
  }

  fun removeGlobalOptionChangeListener(optionName: String, listener: GlobalOptionChangeListener) {
    globalOptionListeners.remove(optionName, listener)
  }

  fun addEffectiveOptionValueChangeListener(optionName: String, listener: EffectiveOptionValueChangeListener) {
    effectiveOptionValueListeners.add(optionName, listener)
  }

  fun removeEffectiveOptionValueChangeListener(optionName: String, listener: EffectiveOptionValueChangeListener) {
    effectiveOptionValueListeners.remove(optionName, listener)
  }

  fun removeAllListeners(optionName: String) {
    globalOptionListeners.removeAll(optionName)
    effectiveOptionValueListeners.removeAll(optionName)
  }

  /**
   * Notify listeners that a global value has changed
   *
   * This can be called for an option of any scope, and will notify affected editors if the effective value has changed.
   * In the case of a global option, it also notifies the non-editor based listeners.
   */
  fun onGlobalValueChanged(option: Option<out VimDataType>) {
    when (option.declaredScope) {
      GLOBAL -> onGlobalOptionChanged(option.name)
      LOCAL_TO_BUFFER, LOCAL_TO_WINDOW -> { /* Setting global value of a local option. No need to notify anyone */ }
      GLOBAL_OR_LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_WINDOW -> onGlobalLocalOptionGlobalValueChanged(option)
    }
  }

  /**
   * Notify listeners that a local value has changed.
   *
   * For global options, this is the same as setting a global value. For local-to-buffer and local-to-window options,
   * this is the effective value, so all affected editors are notified. For global-local options, the local value now
   * becomes the effective value, so all affected editors are notified too.
   */
  fun onLocalValueChanged(option: Option<out VimDataType>, editor: VimEditor) {
    // For all intents and purposes, global-local and local options have the same requirements when setting local value
    when (option.declaredScope) {
      GLOBAL -> onGlobalOptionChanged(option.name)
      LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_BUFFER -> onLocalToBufferOptionChanged(option, editor)
      LOCAL_TO_WINDOW, GLOBAL_OR_LOCAL_TO_WINDOW -> onLocalToWindowOptionChanged(option, editor)
    }
  }

  /**
   * Notify listeners that the effective value of an option has changed.
   *
   * For global options, this is the same as setting the global or local value. For local options, this is the same as
   * setting the local value (setting the global value requires no notifications). For global-local options, this means
   * setting the global value (and affecting all editors that do not have a local override) and resetting the local
   * value to either an unset marker, or the new global value.
   */
  fun onEffectiveValueChanged(option: Option<out VimDataType>, editor: VimEditor) {
    when (option.declaredScope) {
      GLOBAL -> onGlobalOptionChanged(option.name)
      LOCAL_TO_BUFFER -> onLocalToBufferOptionChanged(option, editor)
      LOCAL_TO_WINDOW -> onLocalToWindowOptionChanged(option, editor)
      GLOBAL_OR_LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_WINDOW -> onGlobalLocalOptionEffectiveValueChanged(option, editor)
    }
  }

  /**
   * Notify listeners that a global option has changed
   *
   * This will notify non-editor listeners that the global option value has changed. It also notifies all open editors
   * that the global (and therefore effective) value of a global option has changed.
   */
  private fun onGlobalOptionChanged(optionName: String) {
    globalOptionListeners[optionName]?.forEach { it.onGlobalOptionChanged() }
    fireEffectiveValueChanged(optionName, editorGroup.localEditors())
  }

  /**
   * Notify listeners that the local (and therefore effective) value of a local-to-buffer option has changed
   *
   * Notifies all open editors for the current buffer (document).
   */
  private fun onLocalToBufferOptionChanged(option: Option<out VimDataType>, editor: VimEditor) {
    fireEffectiveValueChanged(option.name, editorGroup.localEditors(editor.document))
  }

  /**
   * Notify listeners that the local/effective value of a local-to-window option has changed
   *
   * Notifies the current open editor only.
   */
  private fun onLocalToWindowOptionChanged(option: Option<out VimDataType>, editor: VimEditor) {
    fireEffectiveValueChanged(option.name, listOf(editor))
  }

  /**
   * Notify listeners that the global value of a global-local option has changed
   *
   * This will notify all open editors where the option is not locally set.
   */
  private fun onGlobalLocalOptionGlobalValueChanged(option: Option<out VimDataType>) {
    val affectedEditors = editorGroup.localEditors().filter { optionGroup.isUnsetValue(option, it) }
    fireEffectiveValueChanged(option.name, affectedEditors)
  }

  /**
   * Notify listeners that the effective value of a global-local option has changed
   *
   * When setting the effective value, the global value is set, and the local value is reset. For string based options,
   * the local value is unset, while for number based options (including toggle options), the value is set to a copy of
   * the new value.
   *
   * This function will notify all editors that do not override the option locally. It also notifies the local editor
   * for the current window or current document, if it's not already unset. The side effect of this is that number based
   * global-local options can never be unset, and setting the effective value of the same option in another window will
   * not update the effective (local) value in this window.
   */
  fun onGlobalLocalOptionEffectiveValueChanged(option: Option<out VimDataType>, editor: VimEditor) {
    // This is essentially the same as calling [onGlobalLocalOptionGlobalValueChanged] followed by
    // [onLocalOptionChanged], but ensures local editors are not notified twice in the case that a number based option
    // is reset rather than unset.
    val affectedEditors = mutableListOf<VimEditor>()
    affectedEditors.addAll(editorGroup.localEditors().filter { optionGroup.isUnsetValue(option, it) })

    if (option.declaredScope == GLOBAL_OR_LOCAL_TO_WINDOW) {
      if (!optionGroup.isUnsetValue(option, editor)) affectedEditors.add(editor)
    }
    else if (option.declaredScope == GLOBAL_OR_LOCAL_TO_BUFFER) {
      affectedEditors.addAll(editorGroup.localEditors().filter { !optionGroup.isUnsetValue(option, it) })
    }

    fireEffectiveValueChanged(option.name, affectedEditors)
  }

  private fun fireEffectiveValueChanged(optionName: String, editors: Collection<VimEditor>) {
    val listeners = effectiveOptionValueListeners[optionName] ?: return
    listeners.forEach { listener ->
      editors.forEach { listener.onEffectiveValueChanged(it) }
    }
  }

  private class MultiSet<K, V> : HashMap<K, MutableSet<V>>() {
    fun add(key: K, value: V) {
      getOrPut(key) { mutableSetOf() }.add(value)
    }

    fun remove(key: K, value: V) {
      this[key]?.remove(value)
    }

    fun removeAll(key: K) {
      remove(key)
    }
  }
}


private class ParsedValuesCache(
  private val optionGroup: VimOptionGroup,
  private val storageService: VimStorageService,
) {
  private val globalParsedValues = mutableMapOf<String, Any>()
  private val parsedEffectiveValueKey = Key<MutableMap<String, Any>>("parsedEffectiveOptionValues")

  fun <T : VimDataType, TData : Any> getParsedEffectiveOptionValue(
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

    val cachedValues = getStorage(option, editor)

    // Unless the user is calling this method multiple times with different providers, we can be confident this cast
    // will succeed. Editor will only be null with global options, so it's safe to use null
    @Suppress("UNCHECKED_CAST")
    return cachedValues.getOrPut(option.name) {
      val scope = if (editor == null) OptionAccessScope.GLOBAL(null) else OptionAccessScope.EFFECTIVE(editor)
      provider(optionGroup.getOptionValue(option, scope))
    } as TData
  }

  private fun getStorage(option: Option<out VimDataType>, editor: VimEditor?): MutableMap<String, Any> {
    return when (option.declaredScope) {
      GLOBAL -> globalParsedValues
      LOCAL_TO_WINDOW, GLOBAL_OR_LOCAL_TO_WINDOW -> {
        check(editor != null) { "Editor must be supplied for local options" }
        storageService.getOrPutWindowData(editor, parsedEffectiveValueKey) { mutableMapOf() }
      }
      LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_BUFFER -> {
        check(editor != null) { "Editor must be supplied for local options" }
        storageService.getOrPutBufferData(editor, parsedEffectiveValueKey) { mutableMapOf() }
      }
    }
  }

  fun reset(option: Option<out VimDataType>, editor: VimEditor?) {
    getStorage(option, editor).remove(option.name)
  }
}

