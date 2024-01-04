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
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW
import com.maddyhome.idea.vim.options.OptionDeclaredScope.LOCAL_TO_BUFFER
import com.maddyhome.idea.vim.options.OptionDeclaredScope.LOCAL_TO_WINDOW
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

public abstract class VimOptionGroupBase : VimOptionGroup {
  private val storage = OptionStorage()
  private val listeners = OptionListenersImpl(storage, injector.editorGroup)
  private val parsedValuesCache = ParsedValuesCache(storage, injector.vimStorageService)

  override fun initialiseOptions() {
    Options.initialise()
  }

  override fun initialiseLocalOptions(editor: VimEditor, sourceEditor: VimEditor?, scenario: LocalOptionInitialisationScenario) {
    // Called for all editors. Ensures that we eagerly initialise all local values, including the per-window "global"
    // values of local-to-window options. Note that global options are not eagerly initialised - the value is the
    // default value unless explicitly set.

    val strategy = OptionInitialisationStrategy(storage)
    if (scenario == LocalOptionInitialisationScenario.DEFAULTS) {
      check(sourceEditor == null) { "sourceEditor must be null when initialising the default options" }
      strategy.initialiseToDefaults(editor)
      return
    }

    check(sourceEditor != null) { "sourceEditor must not be null for scenario: $scenario" }
    when (scenario) {
      LocalOptionInitialisationScenario.FALLBACK,
      LocalOptionInitialisationScenario.CMD_LINE -> strategy.initialiseCloneCurrentState(sourceEditor, editor)
      LocalOptionInitialisationScenario.SPLIT -> strategy.initialiseForSplitCurrentWindow(sourceEditor, editor)
      LocalOptionInitialisationScenario.EDIT -> strategy.initialiseForEditingNewBuffer(sourceEditor, editor)
      LocalOptionInitialisationScenario.NEW -> strategy.initialiseForNewBufferInNewWindow(sourceEditor, editor)
      else -> { }
    }
  }

  /**
   * Update the fallback window to reflect the state of the currently closing window
   *
   * The fallback window is used to provide state to the next window to be opened. When all windows are closed, it must
   * be updated to maintain the state of the last closed window so that the next window has the user's expected state.
   */
  protected fun updateFallbackWindow(fallbackWindow: VimEditor, sourceEditor: VimEditor) {
    // We simply clone all options from the closing window. The next window to open will be initialised with the EDIT
    // scenario, which is like pretending we didn't close the last window and using that window to edit a new buffer.
    // This is the closest approximation to Vim always having at least one window open.
    // The EDIT scenario will initialise the new window by copying per-window global values, initialising default
    // local-to-buffer values and resetting the local-to-window values to the per-window global values. Technically, we
    // could get away with just copying the per-window local values, but cloning state is equivalent to keeping that
    // last window available.
    val strategy = OptionInitialisationStrategy(storage)
    strategy.initialiseCloneCurrentState(sourceEditor, fallbackWindow)
  }

  protected fun <T : VimDataType> addOptionValueOverride(option: Option<T>, override: OptionValueOverride<T>): Unit =
    storage.addOptionValueOverride(option, override)

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
    injector.editorGroup.getEditors().forEach { resetAllOptions(it) }
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

  override fun getEffectiveOptions(editor: VimEditor): EffectiveOptions =
    EffectiveOptions(OptionAccessScope.EFFECTIVE(editor))


  private fun <T : VimDataType> initialiseNewOptionDefaultValues(option: Option<T>) {
    if (option.declaredScope != LOCAL_TO_WINDOW) {
      storage.setOptionValue(option, OptionAccessScope.GLOBAL(null), option.defaultValue)
    }
    injector.editorGroup.getEditors().forEach { editor ->
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
 * Used to override the local/effective value of an option in order to allow IDE backed option values
 *
 * This allows a derived instance of [VimOptionGroupBase] to register providers that can override the local/effective
 * value of a stored Vim option. When getting the local value, an override provider can return the current state of an
 * IDE setting, and when setting the value, it can change the IDE setting.
 *
 * Note that this interface doesn't currently support global option values. It is not clear if this is necessary, but
 * can be added easily.
 *
 * Ideally, this class would be a protected nested class of [VimOptionGroupBase], since it should only be applicable to
 * implementors, but it's used by private helper classes, so needs to be public.
 */
public interface OptionValueOverride<T : VimDataType> {
  /**
   * Gets an overridden local/effective value for the current option
   *
   * It can return a value from a setting in the local editor that matches the current option.
   *
   * @param storedValue The current value of the stored Vim option, if set. This can be `null` during initialisation.
   * The stored value will be the last value set either explicitly with `:set` commands, or defaults. It will not be the
   * result of previous calls to [getLocalValue].
   * @param editor The Vim editor instance to use for retrieving the local value.
   * @return The local value of the option as decided by the override provider. There should always be a value returned,
   * even if there isn't a current stored value, as the point of overriding the option value is to provide a different
   * value. This return value is used as the value of the Vim option, but is not stored.
   */
  public fun getLocalValue(storedValue: T?, editor: VimEditor): T

  /**
   * Sets the local/effective value for the current option.
   *
   * The implementation can use the new value to set a setting in the local editor that matches the current option.
   *
   * @param storedValue The current stored value of the Vim option, if set. This will be `null` during initialisation.
   * The stored value will be the last value set either explicitly with `:set` commands, or defaults. It will not be the
   * result of previous calls to [getLocalValue].
   * @param newValue The new value being set for the Vim option.
   * @param editor The [VimEditor] instance in which the option should be set.
   * @return `true` if the new, overridden local value is different to [storedValue].
   */
  public fun setLocalValue(storedValue: T?, newValue: T, editor: VimEditor): Boolean
}


/**
 * Maintains storage of, and provides accessors to option values
 *
 * This class does not notify any listeners of changes, but provides enough information for a caller to handle this.
 */
private class OptionStorage {
  private val globalValues = mutableMapOf<String, VimDataType>()
  private val perWindowGlobalOptionsKey = Key<MutableMap<String, VimDataType>>("vimPerWindowGlobalOptions")
  private val localOptionsKey = Key<MutableMap<String, VimDataType>>("vimLocalOptions")
  private val overrides = mutableMapOf<String, OptionValueOverride<out VimDataType>>()

  fun <T : VimDataType> addOptionValueOverride(option: Option<T>, override: OptionValueOverride<T>) {
    overrides[option.name] = override
  }

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

  private fun <T : VimDataType> getOptionValueOverride(option: Option<T>): OptionValueOverride<T>? {
    @Suppress("UNCHECKED_CAST")
    return overrides[option.name] as? OptionValueOverride<T>
  }

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
    val value = getOverriddenLocalValue(option, getValue(values, option), editor)
    strictModeAssert(value != null) { "Unexpected uninitialised buffer local value: ${option.name}" }
    return value ?: getEmergencyFallbackLocalValue(option, editor)
  }

  private fun <T : VimDataType> getWindowLocalValue(option: Option<T>, editor: VimEditor): T {
    val values = getWindowLocalOptionStorage(editor)
    val value = getOverriddenLocalValue(option, getValue(values, option), editor)
    strictModeAssert(value != null) { "Unexpected uninitialised window local value: ${option.name}" }
    return value ?: getEmergencyFallbackLocalValue(option, editor)
  }

  private fun <T : VimDataType> getOverriddenLocalValue(option: Option<T>, storedValue: T?, editor: VimEditor): T? {
    getOptionValueOverride(option)?.let {
      return it.getLocalValue(storedValue, editor)
    }
    return storedValue
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
      LOCAL_TO_BUFFER,
      GLOBAL_OR_LOCAL_TO_BUFFER -> setLocalValue(getBufferLocalOptionStorage(editor), option, editor, value)
      LOCAL_TO_WINDOW,
      GLOBAL_OR_LOCAL_TO_WINDOW -> setLocalValue(getWindowLocalOptionStorage(editor) ,option, editor, value)
    }
  }

  private fun <T : VimDataType> setLocalValue(
    values: MutableMap<String, VimDataType>,
    option: Option<T>,
    editor: VimEditor,
    value: T,
  ): Boolean {
    getOptionValueOverride(option)?.let {
      val storedValue = getValue(values, option)
      setValue(values, option.name, value)
      return it.setLocalValue(storedValue, value, editor)
    }
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


private class OptionInitialisationStrategy(private val storage: OptionStorage) {
  /**
   * Initialise the target editor to default values
   *
   * Only used to initialise the very first, hidden, "fallback" window. Vim always has at least one window (and buffer);
   * IdeaVim doesn't. We need to maintain state of options, so we create a simple hidden [VimEditor] when the plugin
   * first starts, and use it to evaluate `~/.ideavimrc`. We use this fallback editor to provide state when initialising
   * the first real editor. Before all that, we need to make sure that all options have default values.
   */
  fun initialiseToDefaults(targetEditor: VimEditor) {
    initialisePerWindowGlobalValues(targetEditor)
    initialiseLocalToBufferOptions(targetEditor)
    initialiseLocalToWindowOptions(targetEditor)
  }

  /**
   * Initialise the target editor by cloning the state of the source editor.
   *
   * This is used for two scenarios:
   * 1. Initialising the first "real" editor from the "fallback" editor. The fallback editor is used to capture options
   *    state when there are no other windows - evaluating `~/.ideavimrc` during startup or when all editors are closed.
   * 2. Initialising the "ex" command line or search input text field/editor associated with a buffer editor. This
   *    allows the command line to use the same options as the main editor.
   */
  fun initialiseCloneCurrentState(sourceEditor: VimEditor, targetEditor: VimEditor) {
    copyPerWindowGlobalValues(sourceEditor, targetEditor)
    copyLocalToBufferLocalValues(sourceEditor, targetEditor)
    copyLocalToWindowLocalValues(sourceEditor, targetEditor)
  }

  /**
   * Initialise the target editor as a split of the source editor.
   *
   * When splitting the current window, the new window is a clone of the current window. Local-to-window options are
   * copied, both the local and per-window "global" values. Buffer local options are of course already initialised.
   */
  fun initialiseForSplitCurrentWindow(sourceEditor: VimEditor, targetEditor: VimEditor) {
    copyPerWindowGlobalValues(sourceEditor, targetEditor)
    initialiseLocalToBufferOptions(targetEditor)  // When splitting the current window, the buffer will be the same
    copyLocalToWindowLocalValues(sourceEditor, targetEditor)
  }

  /**
   * Initialise options for the target editor editing a new buffer (`:edit {file}`).
   *
   * When editing a new buffer in the current window, the per-window global values are untouched. Buffer local options
   * are initialised for the new buffer (if not already initialised) and window local options are reset; local-to-window
   * options are reset to the per-window "global" value, and global-local window options are unset.
   *
   * Theoretically, the source and target editor should be the same editor, however, IntelliJ fakes reusing editors by
   * opening a new editor and closing the old one, so there is still a source and target editor. Note also that IntelliJ
   * does not use this scenario for `:edit {file}`, because the (current) implementation of `:edit` is more like `:new`
   * and always opens a new file. This scenario is used for preview tabs, and reusing unmodified tabs.
   */
  fun initialiseForEditingNewBuffer(sourceEditor: VimEditor, targetEditor: VimEditor) {
    copyPerWindowGlobalValues(sourceEditor, targetEditor)
    initialiseLocalToBufferOptions(targetEditor)
    resetLocalToWindowOptions(targetEditor)
  }

  /**
   * Initialise the target editor as a new buffer opening in a new window.
   *
   * Vim treats this like a split followed by editing a new buffer in the just created window. This means clone the
   * window local options, initialise the buffer local options and then reset the window local options to per-window
   * "global" values, or unset global-local values.
   */
  fun initialiseForNewBufferInNewWindow(sourceEditor: VimEditor, targetEditor: VimEditor) {
    initialiseForSplitCurrentWindow(sourceEditor, targetEditor)
    initialiseForEditingNewBuffer(sourceEditor, targetEditor)
  }

  private fun initialisePerWindowGlobalValues(editor: VimEditor) {
    val scope = OptionAccessScope.GLOBAL(editor)
    forEachOption(LOCAL_TO_WINDOW) { option ->
      storage.setOptionValue(option, scope, option.defaultValue)
    }
  }

  private fun copyPerWindowGlobalValues(sourceEditor: VimEditor, targetEditor: VimEditor) {
    val sourceGlobalScope = OptionAccessScope.GLOBAL(sourceEditor)
    val targetGlobalScope = OptionAccessScope.GLOBAL(targetEditor)
    forEachOption(LOCAL_TO_WINDOW) { option ->
      val value = storage.getOptionValue(option, sourceGlobalScope)
      storage.setOptionValue(option, targetGlobalScope, value)
    }
  }

  private fun initialiseLocalToBufferOptions(editor: VimEditor) {
    val globalScope = OptionAccessScope.GLOBAL(editor)
    val localScope = OptionAccessScope.LOCAL(editor)
    if (!storage.isLocalToBufferOptionStorageInitialised(editor)) {
      forEachOption(LOCAL_TO_BUFFER) { option ->
        val value = storage.getOptionValue(option, globalScope)
        storage.setOptionValue(option, localScope, value)
      }
      forEachOption(GLOBAL_OR_LOCAL_TO_BUFFER) { option ->
        storage.setOptionValue(option, localScope, option.unsetValue)
      }
    }
  }

  private fun copyLocalToBufferLocalValues(sourceEditor: VimEditor, targetEditor: VimEditor) {
    val sourceLocalScope = OptionAccessScope.LOCAL(sourceEditor)
    val targetLocalScope = OptionAccessScope.LOCAL(targetEditor)
    forEachOption(LOCAL_TO_BUFFER) { option ->
      val value = storage.getOptionValue(option, sourceLocalScope)
      storage.setOptionValue(option, targetLocalScope, value)
    }
    forEachOption(GLOBAL_OR_LOCAL_TO_BUFFER) { option ->
      val value = storage.getOptionValue(option, sourceLocalScope)
      storage.setOptionValue(option, targetLocalScope, value)
    }
  }

  private fun initialiseLocalToWindowOptions(editor: VimEditor) {
    val globalScope = OptionAccessScope.GLOBAL(editor)
    val localScope = OptionAccessScope.LOCAL(editor)
    forEachOption(LOCAL_TO_WINDOW) { option ->
      val value = storage.getOptionValue(option, globalScope)
      storage.setOptionValue(option, localScope, value)
    }
    forEachOption(GLOBAL_OR_LOCAL_TO_WINDOW) { option ->
      storage.setOptionValue(option, localScope, option.unsetValue)
    }
  }

  private fun resetLocalToWindowOptions(editor: VimEditor) {
    // This will copy the per-window global to the local value. This is the equivalent of `:set {option}<`
    initialiseLocalToWindowOptions(editor)
  }

  private fun copyLocalToWindowLocalValues(sourceEditor: VimEditor, targetEditor: VimEditor) {
    val sourceLocalScope = OptionAccessScope.LOCAL(sourceEditor)
    val targetLocalScope = OptionAccessScope.LOCAL(targetEditor)
    forEachOption(LOCAL_TO_WINDOW) { option ->
      val value = storage.getOptionValue(option, sourceLocalScope)
      storage.setOptionValue(option, targetLocalScope, value)
    }
    forEachOption(GLOBAL_OR_LOCAL_TO_WINDOW) { option ->
      val value = storage.getOptionValue(option, sourceLocalScope)
      storage.setOptionValue(option, targetLocalScope, value)
    }
  }

  private fun forEachOption(scope: OptionDeclaredScope, action: (Option<VimDataType>) -> Unit) {
    Options.getAllOptions().forEach { option -> if (option.declaredScope == scope) action(option) }
  }
}



private fun <T : VimDataType> OptionStorage.isUnsetValue(option: Option<T>, editor: VimEditor): Boolean {
  return this.getOptionValue(option, OptionAccessScope.LOCAL(editor)) == option.unsetValue
}

private class OptionListenersImpl(private val optionStorage: OptionStorage, private val editorGroup: VimEditorGroup) {
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
    fireEffectiveValueChanged(optionName, editorGroup.getEditors())
  }

  /**
   * Notify listeners that the local (and therefore effective) value of a local-to-buffer option has changed
   *
   * Notifies all open editors for the current buffer (document).
   */
  private fun onLocalToBufferOptionChanged(option: Option<out VimDataType>, editor: VimEditor) {
    fireEffectiveValueChanged(option.name, editorGroup.getEditors(editor.document))
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
    val affectedEditors = editorGroup.getEditors().filter { optionStorage.isUnsetValue(option, it) }
    fireEffectiveValueChanged(option.name, affectedEditors)
  }

  /**
   * Notify listeners that the effective value of a global-local option has changed
   *
   * When setting the effective value (i.e., by calling `:set`), the global value is set, and the local value is reset.
   * For string-based options, the local value is unset, while for number-based options (including toggle options), the
   * value is set to a copy of the new value.
   *
   * This function will notify all editors that do not override the option locally (including editors that have just
   * reset the string-based option to its unset value). It also notifies the editor for the current window, or all
   * editors for the current document where the value is not unset (current editors affected by resetting the
   * number-based option to the new value).
   */
  fun onGlobalLocalOptionEffectiveValueChanged(option: Option<out VimDataType>, editor: VimEditor) {
    // We could get essentially the same behaviour by calling onGlobalLocalOptionGlobalValueChanged followed by
    // onLocalXxxOptionChanged, but that would notify editors for string-based options twice.
    val affectedEditors = mutableListOf<VimEditor>()
    affectedEditors.addAll(editorGroup.getEditors().filter { optionStorage.isUnsetValue(option, it) })

    if (option.declaredScope == GLOBAL_OR_LOCAL_TO_WINDOW) {
      if (!optionStorage.isUnsetValue(option, editor)) affectedEditors.add(editor)
    }
    else if (option.declaredScope == GLOBAL_OR_LOCAL_TO_BUFFER) {
      affectedEditors.addAll(editorGroup.getEditors(editor.document).filter { !optionStorage.isUnsetValue(option, it) })
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
  private val optionStorage: OptionStorage,
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
      provider(optionStorage.getOptionValue(option, scope))
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
