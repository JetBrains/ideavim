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
  private val globalValues = mutableMapOf<String, VimDataType>()
  private val globalParsedValues = mutableMapOf<String, Any>()
  private val globalOptionListeners = MultiSet<String, GlobalOptionChangeListener>()
  private val effectiveOptionValueListeners = MultiSet<String, EffectiveOptionValueChangeListener>()
  private val localOptionsKey = Key<MutableMap<String, VimDataType>>("localOptions")
  private val perWindowGlobalOptionsKey = Key<MutableMap<String, VimDataType>>("perWindowGlobalOptions")
  private val parsedEffectiveValueKey = Key<MutableMap<String, Any>>("parsedEffectiveOptionValues")

  override fun initialiseOptions() {
    Options.initialise()
  }

  override fun initialiseLocalOptions(editor: VimEditor, sourceEditor: VimEditor?, scenario: LocalOptionInitialisationScenario) {
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
        initialiseLocalToBufferOptions(editor)  // Should be a no-op
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
    val localValues = getBufferLocalOptionStorage(targetEditor)
    getAllOptions().forEach { option ->
      if (option.declaredScope == LOCAL_TO_BUFFER || option.declaredScope == GLOBAL_OR_LOCAL_TO_BUFFER) {
        localValues[option.name] = getBufferLocalOptionValue(option, sourceEditor)
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
    injector.vimStorageService.getOrPutBufferData(editor, localOptionsKey) {
      mutableMapOf<String, VimDataType>().also { bufferOptions ->
        getAllOptions().forEach { option ->
          if (option.declaredScope == LOCAL_TO_BUFFER) {
            bufferOptions[option.name] = getGlobalOptionValue(option, editor)
          } else if (option.declaredScope == GLOBAL_OR_LOCAL_TO_BUFFER) {
            bufferOptions[option.name] = option.unsetValue
          }
        }
      }
    }
  }

  protected fun copyPerWindowGlobalValues(targetEditor: VimEditor, sourceEditor: VimEditor) {
    getAllOptions().forEach { option ->
      if (option.declaredScope == LOCAL_TO_WINDOW) {
        val localValue = getGlobalOptionValue(option, sourceEditor)
        setGlobalOptionValue(option, targetEditor, localValue)
      }
    }
  }

  private fun initialisePerWindowGlobalValues(targetEditor: VimEditor) {
    getAllOptions().forEach { option ->
      if (option.declaredScope == LOCAL_TO_WINDOW) {
        setGlobalOptionValue(option, targetEditor, option.defaultValue)
      }
    }
  }

  private fun copyLocalToWindowLocalValues(targetEditor: VimEditor, sourceEditor: VimEditor) {
    val localValues = getWindowLocalOptionStorage(targetEditor)
    getAllOptions().forEach { option ->
      if (option.declaredScope == LOCAL_TO_WINDOW || option.declaredScope == GLOBAL_OR_LOCAL_TO_WINDOW) {
        localValues[option.name] = getWindowLocalOptionValue(option, sourceEditor)
      }
    }
  }

  private fun resetLocalToWindowOptions(editor: VimEditor) = initialiseLocalToWindowOptions(editor)

  private fun initialiseLocalToWindowOptions(editor: VimEditor) {
    val localValues = getWindowLocalOptionStorage(editor)
    getAllOptions().forEach { option ->
      if (option.declaredScope == LOCAL_TO_WINDOW) {
        // Remember that this global value is per-window and should be initialised first
        localValues[option.name] = getGlobalOptionValue(option, editor)
      } else if (option.declaredScope == GLOBAL_OR_LOCAL_TO_WINDOW) {
        localValues[option.name] = option.unsetValue
      }
    }
  }

  override fun <T : VimDataType> getOptionValue(option: Option<T>, scope: OptionAccessScope): T = when (scope) {
    is OptionAccessScope.EFFECTIVE -> getEffectiveOptionValue(option, scope.editor)
    is OptionAccessScope.LOCAL -> getLocalOptionValue(option, scope.editor)
    is OptionAccessScope.GLOBAL -> getGlobalOptionValue(option, scope.editor)
  }

  override fun <T : VimDataType> setOptionValue(option: Option<T>, scope: OptionAccessScope, value: T) {
    option.checkIfValueValid(value, value.asString())

    when (scope) {
      is OptionAccessScope.EFFECTIVE -> setEffectiveOptionValue(option, scope.editor, value)
      is OptionAccessScope.LOCAL -> setLocalOptionValue(option, scope.editor, value)
      is OptionAccessScope.GLOBAL -> setGlobalOptionValue(option, scope.editor, value)
    }
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
    } else {
      check(editor != null) { "Editor must be supplied for local options" }
      getParsedEffectiveOptionStorage(option, editor)
    }

    // Unless the user is calling this method multiple times with different providers, we can be confident this cast
    // will succeed. Editor will only be null with global options, so it's safe to use null
    @Suppress("UNCHECKED_CAST")
    return cachedValues.getOrPut(option.name) {
      provider(getOptionValue(option, if (editor == null) OptionAccessScope.GLOBAL(null) else OptionAccessScope.EFFECTIVE(editor)))
    } as TData
  }

  override fun getOption(key: String): Option<VimDataType>? = Options.getOption(key)
  override fun getAllOptions(): Set<Option<VimDataType>> = Options.getAllOptions()

  override fun resetAllOptions(editor: VimEditor) {
    // Reset all options to default values at global and local scope. This will fire any listeners and clear any caches
    Options.getAllOptions().forEach { option ->
      resetDefaultValue(option, OptionAccessScope.GLOBAL(editor))
      when (option.declaredScope) {
        GLOBAL -> {
        }
        LOCAL_TO_BUFFER, LOCAL_TO_WINDOW -> resetDefaultValue(option, OptionAccessScope.LOCAL(editor))
        GLOBAL_OR_LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_WINDOW -> {
          setOptionValue(option, OptionAccessScope.LOCAL(editor), option.unsetValue)
        }
      }
    }
  }

  override fun resetAllOptionsForTesting() {
    // During testing, this collection is usually empty. Just in case, make sure all editors have default options
    injector.editorGroup.localEditors().forEach { resetAllOptions(it) }
    resetAllOptions(injector.fallbackWindow)

    // Make sure we reset global options even if we don't have any editors. This fires listeners and clears caches
    Options.getAllOptions().filter { it.declaredScope == GLOBAL }.forEach { resetDefaultValue(it, OptionAccessScope.GLOBAL(null)) }

    // Reset global value of other options manually, without firing listeners or clearing caches. This is safe because
    // we only cache values or listen to changes for the effective values of local options (and not global-local). But
    // local-to-window options will store global values per-window (which we don't have). So this will have the same
    // result as resetDefaultValue but without the asserts for setting a local option without a window.
    Options.getAllOptions().filter { it.declaredScope != GLOBAL }.forEach { globalValues[it.name] = it.defaultValue }
  }

  override fun addOption(option: Option<out VimDataType>) {
    Options.addOption(option)

    // Initialise the values. Cast away the covariance, because it gets in the way of type inference. We want functions
    // that are generic on Option<T> to get `VimDataType` and not `out VimDataType`, which we can't use
    @Suppress("UNCHECKED_CAST")
    initialiseOptionValues(option as Option<VimDataType>)
  }

  override fun removeOption(optionName: String) {
    Options.removeOption(optionName)
    globalOptionListeners.removeAll(optionName)
    effectiveOptionValueListeners.removeAll(optionName)
  }

  override fun <T : VimDataType> addGlobalOptionChangeListener(
    option: Option<T>,
    listener: GlobalOptionChangeListener
  ) {
    check(option.declaredScope == GLOBAL)
    globalOptionListeners.add(option.name, listener)
  }

  override fun <T : VimDataType> removeGlobalOptionChangeListener(
    option: Option<T>,
    listener: GlobalOptionChangeListener
  ) {
    globalOptionListeners.remove(option.name, listener)
  }

  override fun <T : VimDataType> addEffectiveOptionValueChangeListener(
    option: Option<T>,
    listener: EffectiveOptionValueChangeListener
  ) {
    effectiveOptionValueListeners.add(option.name, listener)
  }

  override fun <T : VimDataType> removeEffectiveOptionValueChangeListener(
    option: Option<T>,
    listener: EffectiveOptionValueChangeListener
  ) {
    effectiveOptionValueListeners.remove(option.name, listener)
  }

  final override fun <T : VimDataType> overrideDefaultValue(option: Option<T>, newDefaultValue: T) {
    option.overrideDefaultValue(newDefaultValue)
  }

  // We can pass null as the editor because we are only accessing global options
  override fun getGlobalOptions(): GlobalOptions = GlobalOptions(OptionAccessScope.GLOBAL(null))

  override fun getEffectiveOptions(editor: VimEditor): EffectiveOptions = EffectiveOptions(OptionAccessScope.EFFECTIVE(editor))


  // We can't use StrictMode.assert because it checks an option, which calls into VimOptionGroupBase...
  private fun strictModeAssert(condition: Boolean, message: String) {
    if (globalValues[Options.ideastrictmode.name]?.asBoolean() == true && !condition) {
      error(message)
    }
  }


  private fun initialiseOptionValues(option: Option<VimDataType>) {
    // Initialise the values for a new option. Note that we don't call setOptionValue to avoid calling getOptionValue
    // in get a non-existent old value to pass to any listeners
    globalValues[option.name] = option.defaultValue
    injector.editorGroup.localEditors().forEach { editor ->
      when (option.declaredScope) {
        GLOBAL -> doSetGlobalOptionValue(option, option.defaultValue)
        LOCAL_TO_BUFFER -> doSetBufferLocalOptionValue(option, editor, option.defaultValue)
        LOCAL_TO_WINDOW -> doSetWindowLocalOptionValue(option, editor, option.defaultValue)
        GLOBAL_OR_LOCAL_TO_BUFFER -> doSetBufferLocalOptionValue(option, editor, option.unsetValue)
        GLOBAL_OR_LOCAL_TO_WINDOW -> doSetWindowLocalOptionValue(option, editor, option.unsetValue)
      }
    }
  }


  private fun getPerWindowGlobalOptionStorage(editor: VimEditor) =
    injector.vimStorageService.getOrPutWindowData(editor, perWindowGlobalOptionsKey) { mutableMapOf() }

  private fun <T : VimDataType> getGlobalOptionValue(option: Option<T>, editor: VimEditor?): T {
    val values = if (option.declaredScope == LOCAL_TO_WINDOW) {
      check(editor != null) { "Editor must be provided for local options" }
      getPerWindowGlobalOptionStorage(editor)
    } else {
      globalValues
    }

    // We set the value via Option<T> so it's safe to cast to T. But note that the value might be null because we don't
    // explicitly populate global option values in the same way we do local options
    @Suppress("UNCHECKED_CAST")
    return values[option.name] as? T ?: option.defaultValue
  }

  private fun <T : VimDataType> setGlobalOptionValue(option: Option<T>, editor: VimEditor?, value: T) {
    val changed = if (option.declaredScope == LOCAL_TO_WINDOW) {
      check(editor != null) { "Editor must be provided for local options" }
      doSetPerWindowGlobalOptionValue(option, editor, value)
    } else {
      doSetGlobalOptionValue(option, value)
    }

    when (option.declaredScope) {
      GLOBAL -> if (changed) {
        onGlobalOptionValueChanged(option)
        onGlobalOptionEffectiveValueChanged(option)
      }
      LOCAL_TO_BUFFER, LOCAL_TO_WINDOW -> { /* Setting global value of a local option. No need to notify anyone */
      }
      GLOBAL_OR_LOCAL_TO_BUFFER -> onGlobalLocalToBufferOptionGlobalValueChanged(option)
      GLOBAL_OR_LOCAL_TO_WINDOW -> onGlobalLocalToWindowOptionGlobalValueChanged(option)
    }
  }

  private fun <T : VimDataType> doSetGlobalOptionValue(option: Option<T>, value: T): Boolean {
    val oldValue = globalValues[option.name]
    if (oldValue != value) {
      globalValues[option.name] = value
      globalParsedValues.remove(option.name)
      return true
    }
    return false
  }

  private fun <T : VimDataType> doSetPerWindowGlobalOptionValue(option: Option<T>, editor: VimEditor, value: T): Boolean {
    val values = getPerWindowGlobalOptionStorage(editor)
    val oldValue = values[option.name]
    if (oldValue != value) {
      values[option.name] = value
      return true
    }
    return false
  }


  /**
   * Get the effective value of the option for the given editor. This is the equivalent of `:set {option}?`
   *
   * For local-to-window and local-to-buffer options, this returns the local value. For global options, it returns the
   * global value. For global-local options, it returns the local value if set, and the global value if not.
   */
  private fun <T : VimDataType> getEffectiveOptionValue(option: Option<T>, editor: VimEditor) =
    when (option.declaredScope) {
      GLOBAL -> getGlobalOptionValue(option, editor)
      LOCAL_TO_BUFFER -> getBufferLocalOptionValue(option, editor)
      LOCAL_TO_WINDOW -> getWindowLocalOptionValue(option, editor)
      GLOBAL_OR_LOCAL_TO_BUFFER -> {
        tryGetBufferLocalOptionValue(option, editor).takeUnless { it == option.unsetValue }
          ?: getGlobalOptionValue(option, editor)
      }
      GLOBAL_OR_LOCAL_TO_WINDOW -> {
        tryGetWindowLocalOptionValue(option, editor).takeUnless { it == option.unsetValue }
          ?: getGlobalOptionValue(option, editor)
      }
    }

  /**
   * Set the effective value of the option. This is the equivalent of `:set`
   *
   * For local-to-buffer and local-to-window options, this function will set the local value of the option. It also sets
   * the global value (so that we remember the most recently set value for initialising new windows). For global
   * options, this function will also set the global value. For global-local, this function will always set the global
   * value, and if the local value is set, it will unset it for string values, and copy the global value for
   * number-based options (including toggle options).
   */
  private fun <T : VimDataType> setEffectiveOptionValue(option: Option<T>, editor: VimEditor, value: T) {
    when (option.declaredScope) {
      GLOBAL -> if (doSetGlobalOptionValue(option, value)) {
        onGlobalOptionValueChanged(option)
        onGlobalOptionEffectiveValueChanged(option)
      }
      LOCAL_TO_BUFFER -> if (doSetBufferLocalOptionValue(option, editor, value)) {
        doSetGlobalOptionValue(option, value)
        onBufferLocalOptionEffectiveValueChanged(option, editor)
      }
      LOCAL_TO_WINDOW -> if (doSetWindowLocalOptionValue(option, editor, value)) {
        doSetPerWindowGlobalOptionValue(option, editor, value)
        onWindowLocalOptionEffectiveValueChanged(option, editor)
      }
      GLOBAL_OR_LOCAL_TO_BUFFER -> {
        if (tryGetBufferLocalOptionValue(option, editor) != option.unsetValue) {
          // Number based options (including boolean) get a copy of the global value. String based options get unset
          doSetBufferLocalOptionValue(
            option,
            editor,
            if (option is NumberOption || option is ToggleOption) value else option.unsetValue
          )
        }
        doSetGlobalOptionValue(option, value)
        onGlobalLocalToBufferOptionEffectiveValueChanged(option, editor)
      }
      GLOBAL_OR_LOCAL_TO_WINDOW -> {
        if (tryGetWindowLocalOptionValue(option, editor) != option.unsetValue) {
          // Number based options (including boolean) get a copy of the global value. String based options get unset
          doSetWindowLocalOptionValue(
            option,
            editor,
            if (option is NumberOption || option is ToggleOption) value else option.unsetValue
          )
        }
        doSetGlobalOptionValue(option, value)
        onGlobalLocalToWindowOptionEffectiveValueChanged(option, editor)
      }
    }
  }


  private fun <T : VimDataType> getLocalOptionValue(option: Option<T>, editor: VimEditor) =
    when (option.declaredScope) {
      GLOBAL -> getGlobalOptionValue(option, editor)
      LOCAL_TO_BUFFER -> getBufferLocalOptionValue(option, editor)
      LOCAL_TO_WINDOW -> getWindowLocalOptionValue(option, editor)
      GLOBAL_OR_LOCAL_TO_BUFFER -> {
        tryGetBufferLocalOptionValue(option, editor).also {
          strictModeAssert(it != null, "Global local value is missing: ${option.name}")
        } ?: option.unsetValue
      }
      GLOBAL_OR_LOCAL_TO_WINDOW -> {
        tryGetWindowLocalOptionValue(option, editor).also {
          strictModeAssert(it != null, "Global local value is missing: ${option.name}")
        } ?: option.unsetValue
      }
    }

  /**
   * Set the option explicitly at local scope. This is the equivalent of `:setlocal`
   *
   * This will always set the local option value, even for global-local options (global options obviously only have a
   * global value).
   */
  private fun <T : VimDataType> setLocalOptionValue(option: Option<T>, editor: VimEditor, value: T) {
    when (option.declaredScope) {
      GLOBAL -> if (doSetGlobalOptionValue(option, value)) {
        onGlobalOptionValueChanged(option)
        onGlobalOptionEffectiveValueChanged(option)
      }
      LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_BUFFER -> if (doSetBufferLocalOptionValue(option, editor, value)) {
        onBufferLocalOptionEffectiveValueChanged(option, editor)
      }
      LOCAL_TO_WINDOW, GLOBAL_OR_LOCAL_TO_WINDOW -> if (doSetWindowLocalOptionValue(option, editor, value)) {
        onWindowLocalOptionEffectiveValueChanged(option, editor)
      }
    }
  }


  private fun getBufferLocalOptionStorage(editor: VimEditor) =
    injector.vimStorageService.getOrPutBufferData(editor, localOptionsKey) { mutableMapOf() }

  private fun <T : VimDataType> doSetBufferLocalOptionValue(option: Option<T>, editor: VimEditor, value: T): Boolean {
    val values = getBufferLocalOptionStorage(editor)
    val oldValue = values[option.name]
    if (oldValue != value) {
      values[option.name] = value
      getParsedEffectiveOptionStorage(option, editor).remove(option.name)
      return true
    }
    return false
  }

  private fun <T : VimDataType> getBufferLocalOptionValue(option: Option<T>, editor: VimEditor): T {
    check(option.declaredScope == LOCAL_TO_BUFFER || option.declaredScope == GLOBAL_OR_LOCAL_TO_BUFFER)

    // This should never return null, because we initialise local options when initialising the editor, even when adding
    // options dynamically, e.g. registering an extension. We fall back to global option, just in case
    return tryGetBufferLocalOptionValue(option, editor).also {
      strictModeAssert(it != null, "Buffer local option value is missing: ${option.name}")
    } ?: getGlobalOptionValue(option, editor)
  }

  private fun <T : VimDataType> tryGetBufferLocalOptionValue(option: Option<T>, editor: VimEditor): T? {
    val values = getBufferLocalOptionStorage(editor)

    // We set the value via Option<T> so it's safe to cast to T
    @Suppress("UNCHECKED_CAST")
    return values[option.name] as? T
  }


  private fun getWindowLocalOptionStorage(editor: VimEditor) =
    injector.vimStorageService.getOrPutWindowData(editor, localOptionsKey) { mutableMapOf() }

  private fun <T : VimDataType> doSetWindowLocalOptionValue(option: Option<T>, editor: VimEditor, value: T): Boolean {
    val values = getWindowLocalOptionStorage(editor)
    val oldValue = values[option.name]
    if (oldValue != value) {
      values[option.name] = value
      getParsedEffectiveOptionStorage(option, editor).remove(option.name)
      return true
    }
    return false
  }

  private fun <T : VimDataType> getWindowLocalOptionValue(option: Option<T>, editor: VimEditor): T {
    check(option.declaredScope == LOCAL_TO_WINDOW || option.declaredScope == GLOBAL_OR_LOCAL_TO_WINDOW)

    // This should never return null, because we initialise local options when initialising the editor, even when adding
    // options dynamically, e.g. registering an extension. We fall back to global option, just in case
    val value = tryGetWindowLocalOptionValue(option, editor)
    strictModeAssert(value != null, "Window local option value is missing: ${option.name}")
    return value ?: getGlobalOptionValue(option, editor)
  }

  private fun <T : VimDataType> tryGetWindowLocalOptionValue(option: Option<T>, editor: VimEditor): T? {
    val values = getWindowLocalOptionStorage(editor)

    // We set the value via Option<T> so it's safe to cast to T
    @Suppress("UNCHECKED_CAST")
    return values[option.name] as? T
  }

  private fun <T : VimDataType> onGlobalOptionValueChanged(option: Option<T>) {
    globalOptionListeners[option.name]?.forEach {
      it.onGlobalOptionChanged()
    }
  }

  private inline fun <T : VimDataType> onEffectiveValueChanged(
    option: Option<T>,
    editorsProvider: () -> Collection<VimEditor>,
  ) {
    val listeners = effectiveOptionValueListeners[option.name] ?: return
    val editors = editorsProvider()
    listeners.forEach { listener ->
      editors.forEach { listener.onEffectiveValueChanged(it) }
    }
  }

  /**
   * Notify all editors that a global option value has changed
   *
   * This will call the listener for all open editors. It will also call the listener with a `null` editor, to provide
   * for non-editor related callbacks. E.g. updating `'showcmd'`, tracking default register for `'clipboard'`, etc.
   */
  private fun <T : VimDataType> onGlobalOptionEffectiveValueChanged(option: Option<T>) {
    onEffectiveValueChanged(option) { injector.editorGroup.localEditors() }
  }

  /**
   * Notify all editors for the current buffer that a local-to-buffer option's effective value has changed
   */
  private fun <T : VimDataType> onBufferLocalOptionEffectiveValueChanged(option: Option<T>, editor: VimEditor) {
    onEffectiveValueChanged(option) { injector.editorGroup.localEditors(editor.document) }
  }

  /**
   * Notify the given editor that a local-to-window option's effective value has changed
   */
  private fun <T : VimDataType> onWindowLocalOptionEffectiveValueChanged(option: Option<T>, editor: VimEditor) {
    onEffectiveValueChanged(option) { listOf(editor) }
  }

  /**
   * Notify the affected editors that a global-local local to buffer option's effective value has changed.
   *
   * When a global-local option's effective value is changed with `:set`, the global value is updated, and copied to the
   * local value. This means we need to notify all editors that are using the global value that it has changed, and skip
   * any editors that have the value overridden. The editors associated with the current buffer are also notified (their
   * local value has been updated).
   *
   * Note that we make no effort to minimise change notifications for global-local, as the logic is complex enough.
   */
  private fun <T : VimDataType> onGlobalLocalToBufferOptionEffectiveValueChanged(option: Option<T>, editor: VimEditor) {
    onEffectiveValueChanged(option) {
      mutableListOf<VimEditor>().also { editors ->
        editors.addAll(injector.editorGroup.localEditors().filter { isUnsetValue(option, it) })
        editors.addAll(injector.editorGroup.localEditors(editor.document).filter { !isUnsetValue(option, it) })
      }
    }
  }

  private fun <T : VimDataType> onGlobalLocalToBufferOptionGlobalValueChanged(option: Option<T>) {
    onEffectiveValueChanged(option) { injector.editorGroup.localEditors().filter { isUnsetValue(option, it) } }
  }

  /**
   * Notify the affected editors that a global-local local to window option's effective value has changed.
   *
   * When a global-local option's effective value is changed with `:set`, the global value is updated, and copied to the
   * local value. This means we need to notify all editors that are using the global value that it has changed, and skip
   * any editors that have the value overridden. The editors associated with the current buffer are also notified (their
   * local value has been updated).
   *
   * Note that we make no effort to minimise change notifications for global-local, as the logic is complex enough.
   */
  private fun <T : VimDataType> onGlobalLocalToWindowOptionEffectiveValueChanged(option: Option<T>, editor: VimEditor) {
    onEffectiveValueChanged(option) {
      mutableListOf<VimEditor>().also { editors ->
        editors.addAll(injector.editorGroup.localEditors().filter { isUnsetValue(option, it) })
        if (!isUnsetValue(option, editor)) {
          editors.add(editor)
        }
      }
    }
  }

  private fun <T : VimDataType> onGlobalLocalToWindowOptionGlobalValueChanged(option: Option<T>) {
    onEffectiveValueChanged(option) { injector.editorGroup.localEditors().filter { isUnsetValue(option, it) } }
  }


  private fun getParsedEffectiveOptionStorage(option: Option<out VimDataType>, editor: VimEditor): MutableMap<String, Any> {
    return when (option.declaredScope) {
      LOCAL_TO_WINDOW, GLOBAL_OR_LOCAL_TO_WINDOW -> {
        injector.vimStorageService.getOrPutWindowData(editor, parsedEffectiveValueKey) { mutableMapOf() }
      }
      LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_BUFFER -> {
        injector.vimStorageService.getOrPutBufferData(editor, parsedEffectiveValueKey) { mutableMapOf() }
      }
      else -> {
        throw IllegalStateException("Unexpected option declared scope for parsed effective storage: ${option.name}")
      }
    }
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