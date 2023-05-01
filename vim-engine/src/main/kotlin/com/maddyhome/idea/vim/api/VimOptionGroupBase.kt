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
import com.maddyhome.idea.vim.options.OptionChangeListener
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW
import com.maddyhome.idea.vim.options.OptionDeclaredScope.LOCAL_TO_BUFFER
import com.maddyhome.idea.vim.options.OptionDeclaredScope.LOCAL_TO_WINDOW
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

public abstract class VimOptionGroupBase : VimOptionGroup {
  private val globalOptionsAccessor = GlobalOptions()
  private val globalValues = mutableMapOf<String, VimDataType>()
  private val globalParsedValues = mutableMapOf<String, Any>()
  private val globalOptionListeners = MultiSet<String, GlobalOptionChangeListener>()
  private val effectiveOptionValueListeners = MultiSet<String, EffectiveOptionValueChangeListener>()
  private val localOptionsKey = Key<MutableMap<String, VimDataType>>("localOptions")
  private val parsedEffectiveValueKey = Key<MutableMap<String, Any>>("parsedEffectiveOptionValues")

  override fun initialiseOptions() {
    Options.initialise()
  }

  override fun initialiseLocalOptions(editor: VimEditor, sourceEditor: VimEditor?, isSplit: Boolean) {
    // Initialise local-to-buffer options
    // They are stored per-buffer, so shared across all editors for the buffer. If the key exists, they've previously
    // been initialised, else initialise the options from the global values, which is always the most recently set value
    // (`:set` on a buffer-local option will set the local value, but it also sets the global value, for exactly this
    // reason)
    injector.vimStorageService.getOrPutBufferData(editor, localOptionsKey) {
      mutableMapOf<String, VimDataType>().also { bufferOptions ->
        getAllOptions().forEach { option ->
          if (option.declaredScope == LOCAL_TO_BUFFER) {
            bufferOptions[option.name] = getGlobalOptionValue(option)
          } else if (option.declaredScope == GLOBAL_OR_LOCAL_TO_BUFFER) {
            bufferOptions[option.name] = option.unsetValue
          }
        }
      }
    }

    // TODO: We don't support per-window "global" values right now
    // These functions are here so we know what the semantics should be when it comes time to implement.
    // Default to getting the per-instance global value for now (per-instance meaning per VimOptionGroup service instance)
    // Set does nothing, because it's called with the current "global" value, which would be a no-op
    fun getPerWindowGlobalOptionValue(option: Option<VimDataType>, editor: VimEditor?) = getGlobalOptionValue(option)
    fun setPerWindowGlobalOptionValue(option: Option<VimDataType>, editor: VimEditor, value: VimDataType) {}

    // Initialising local-to-window options is a little more involved (see [OptionDeclaredScope])
    // Assumptions:
    // * Vim always has at least one open window. A new window or buffer is initialised from this source window
    //   IdeaVim does not always have an open window. The passed source window might be null.
    //   TODO: How does this handle `:setlocal` in `~/.ideavimrc`?
    //   We might need to create a dummy "root" window that evaluates `~/.ideavimrc` and is used to initialise the local
    //   options of other windows.
    // * Vim's local-to-window options store "global" values as per-window global values.
    //   TODO: IdeaVim does not currently support per-window global values
    // Scenarios:
    // 1. Split the current window
    //    Vim tries to make the split an exact clone. Copy the source window's local and per-window global values to the
    //    new window. This applies to local-to-window and "global or local to window" options
    // 2. Edit a new buffer in the current window (`:edit {file}`)
    //    Reapply the current window's per-window global values as local values, to get rid of explicitly local values
    //    IdeaVim does not currently support this scenario, because IdeaVim's implementation of `:edit` does not open
    //    a file in the current window. It instead behaves like `:new {file}`.
    //    We could implement it like the platform implements preview tabs, or reusing unmodified tabs - by opening a new
    //    editor and immediately closing the old one. This would still behave like `:new` - copying the per-window
    //    global values and applying them as local values, which would be the correct behaviour
    // 3. Open a new buffer in a new window (`:new {file}`)
    //    Vim implements this as a split, then editing a new buffer in the new current window. This will copy the source
    //    window's local and per-window global values to the new split window, then reapply the new window's per-window
    //    global values to the new window's local options. In effect, copying the source window's per-window global
    //    values to the new window
    // 3. Edit a previously edited buffer (in the current window)
    //    Vim will reapply options saved from the last window used to edit this buffer. Details are a bit sketchy - when
    //    are the options saved, when are the released, etc. so this scenario is not currently supported.
    //    IdeaVim does not support this
    injector.vimStorageService.getOrPutWindowData(editor, localOptionsKey) {
      mutableMapOf<String, VimDataType>().also { windowOptions ->
        getAllOptions()
          .filter { it.declaredScope == LOCAL_TO_WINDOW || it.declaredScope == GLOBAL_OR_LOCAL_TO_WINDOW }
          .forEach { option ->
            if (isSplit && sourceEditor != null) {
              // Splitting the current window, make it look and behave the same as the source editor
              windowOptions[option.name] = getWindowLocalOptionValue(option, sourceEditor)
              setPerWindowGlobalOptionValue(option, editor, getPerWindowGlobalOptionValue(option, sourceEditor))
            }
            else {
              // All other scenarios (open new buffer in new or current window)
              windowOptions[option.name] = if (option.declaredScope == GLOBAL_OR_LOCAL_TO_WINDOW) {
                option.unsetValue
              }
              else {
                getPerWindowGlobalOptionValue(option, sourceEditor)
              }
            }
          }
      }
    }
  }

  override fun <T : VimDataType> getOptionValue(option: Option<T>, scope: OptionScope): T = when (scope) {
    is OptionScope.AUTO -> getEffectiveOptionValue(option, scope.editor)
    is OptionScope.LOCAL -> getLocalOptionValue(option, scope.editor)
    OptionScope.GLOBAL -> getGlobalOptionValue(option)
  }

  override fun <T : VimDataType> setOptionValue(option: Option<T>, scope: OptionScope, value: T) {
    option.checkIfValueValid(value, value.asString())

    val oldValue = getOptionValue(option, scope)

    when (scope) {
      is OptionScope.AUTO -> setEffectiveOptionValue(option, scope.editor, value)
      is OptionScope.LOCAL -> setLocalOptionValue(option, scope.editor, value)
      OptionScope.GLOBAL -> setGlobalOptionValue(option, value)
    }

    if (oldValue != value) {
      option.onChanged(scope, oldValue)
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
    }
    else {
      check(editor != null) { "Editor must be supplied for local options" }
      getParsedEffectiveOptionStorage(option, editor)
    }

    // Unless the user is calling this method multiple times with different providers, we can be confident this cast
    // will succeed
    @Suppress("UNCHECKED_CAST")
    return cachedValues.getOrPut(option.name) {
      provider(getOptionValue(option, if (editor == null) OptionScope.GLOBAL else OptionScope.AUTO(editor)))
    } as TData
  }

  override fun getOption(key: String): Option<VimDataType>? = Options.getOption(key)
  override fun getAllOptions(): Set<Option<VimDataType>> = Options.getAllOptions()

  override fun resetAllOptions(editor: VimEditor) {
    // Reset all options to default values at global and local scope. This will fire any listeners and clear any caches
    Options.getAllOptions().forEach { option ->
      resetDefaultValue(option, OptionScope.GLOBAL)
      when (option.declaredScope) {
        GLOBAL -> {}
        LOCAL_TO_BUFFER, LOCAL_TO_WINDOW -> resetDefaultValue(option, OptionScope.LOCAL(editor))
        GLOBAL_OR_LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_WINDOW -> {
          setOptionValue(option, OptionScope.LOCAL(editor), option.unsetValue)
        }
      }
    }
  }

  override fun resetAllOptionsForTesting() {
    Options.getAllOptions().forEach {
      resetDefaultValue(it, OptionScope.GLOBAL)
    }
    // During testing, this collection is usually empty
    injector.editorGroup.localEditors().forEach { resetAllOptions(it) }
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

  override fun getEffectiveOptions(editor: VimEditor): EffectiveOptions = EffectiveOptions(OptionScope.AUTO(editor))


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
        GLOBAL -> {}
        LOCAL_TO_BUFFER -> doSetBufferLocalOptionValue(option, editor, option.defaultValue)
        LOCAL_TO_WINDOW -> doSetWindowLocalOptionValue(option, editor, option.defaultValue)
        GLOBAL_OR_LOCAL_TO_BUFFER -> doSetBufferLocalOptionValue(option, editor, option.unsetValue)
        GLOBAL_OR_LOCAL_TO_WINDOW -> doSetWindowLocalOptionValue(option, editor, option.unsetValue)
      }
    }
  }


  private fun <T : VimDataType> getGlobalOptionValue(option: Option<T>): T {
    // We set the value via Option<T> so it's safe to cast to T. But note that the value might be null because we don't
    // explicitly populate global option values in the same way we do local options
    @Suppress("UNCHECKED_CAST")
    return globalValues[option.name] as? T ?: option.defaultValue
  }

  private fun <T : VimDataType> setGlobalOptionValue(option: Option<T>, value: T) {
    doSetGlobalOptionValue(option, value)
    when (option.declaredScope) {
      GLOBAL -> {
        onGlobalOptionValueChanged(option)
        onGlobalOptionEffectiveValueChanged(option)
      }
      LOCAL_TO_BUFFER, LOCAL_TO_WINDOW -> {}
      GLOBAL_OR_LOCAL_TO_BUFFER -> onGlobalLocalToBufferOptionGlobalValueChanged(option)
      GLOBAL_OR_LOCAL_TO_WINDOW -> onGlobalLocalToWindowOptionGlobalValueChanged(option)
    }
  }

  private fun <T : VimDataType> doSetGlobalOptionValue(option: Option<T>, value: T) {
    globalValues[option.name] = value
    globalParsedValues.remove(option.name)
  }


  /**
   * Get the effective value of the option for the given editor. This is the equivalent of `:set {option}?`
   *
   * For local-to-window and local-to-buffer options, this returns the local value. For global options, it returns the
   * global value. For global-local options, it returns the local value if set, and the global value if not.
   */
  private fun <T : VimDataType> getEffectiveOptionValue(option: Option<T>, editor: VimEditor) =
    when (option.declaredScope) {
      GLOBAL -> getGlobalOptionValue(option)
      LOCAL_TO_BUFFER -> getBufferLocalOptionValue(option, editor)
      LOCAL_TO_WINDOW -> getWindowLocalOptionValue(option, editor)
      GLOBAL_OR_LOCAL_TO_BUFFER -> {
        tryGetBufferLocalOptionValue(option, editor).takeUnless { it == option.unsetValue }
          ?: getGlobalOptionValue(option)
      }
      GLOBAL_OR_LOCAL_TO_WINDOW -> {
        tryGetWindowLocalOptionValue(option, editor).takeUnless { it == option.unsetValue }
          ?: getGlobalOptionValue(option)
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
      GLOBAL -> {
        doSetGlobalOptionValue(option, value)
        onGlobalOptionValueChanged(option)
        onGlobalOptionEffectiveValueChanged(option)
      }
      LOCAL_TO_BUFFER -> {
        doSetBufferLocalOptionValue(option, editor, value)
        doSetGlobalOptionValue(option, value)
        onBufferLocalOptionEffectiveValueChanged(option, editor)
      }
      LOCAL_TO_WINDOW -> {
        doSetWindowLocalOptionValue(option, editor, value)
        doSetGlobalOptionValue(option, value)
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
      GLOBAL -> getGlobalOptionValue(option)
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
      GLOBAL -> {
        doSetGlobalOptionValue(option, value)
        onGlobalOptionValueChanged(option)
        onGlobalOptionEffectiveValueChanged(option)
      }
      LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_BUFFER -> {
        doSetBufferLocalOptionValue(option, editor, value)
        onBufferLocalOptionEffectiveValueChanged(option, editor)
      }
      LOCAL_TO_WINDOW, GLOBAL_OR_LOCAL_TO_WINDOW -> {
        doSetWindowLocalOptionValue(option, editor, value)
        onWindowLocalOptionEffectiveValueChanged(option, editor)
      }
    }
  }


  private fun getBufferLocalOptionStorage(editor: VimEditor) =
    injector.vimStorageService.getOrPutBufferData(editor, localOptionsKey) { mutableMapOf() }

  private fun <T : VimDataType> doSetBufferLocalOptionValue(option: Option<T>, editor: VimEditor, value: T) {
    val options = getBufferLocalOptionStorage(editor)
    options[option.name] = value

    getParsedEffectiveOptionStorage(option, editor).remove(option.name)
  }

  private fun <T : VimDataType> getBufferLocalOptionValue(option: Option<T>, editor: VimEditor): T {
    check(option.declaredScope == LOCAL_TO_BUFFER || option.declaredScope == GLOBAL_OR_LOCAL_TO_BUFFER)

    // This should never return null, because we initialise local options when initialising the editor, even when adding
    // options dynamically, e.g. registering an extension. We fall back to global option, just in case
    return tryGetBufferLocalOptionValue(option, editor).also {
      strictModeAssert(it != null, "Buffer local option value is missing: ${option.name}")
    } ?: getGlobalOptionValue(option)
  }

  private fun <T : VimDataType> tryGetBufferLocalOptionValue(option: Option<T>, editor: VimEditor): T? {
    val options = getBufferLocalOptionStorage(editor)

    // We set the value via Option<T> so it's safe to cast to T
    @Suppress("UNCHECKED_CAST")
    return options[option.name] as? T
  }


  private fun getWindowLocalOptionStorage(editor: VimEditor) =
    injector.vimStorageService.getOrPutWindowData(editor, localOptionsKey) { mutableMapOf() }

  private fun <T : VimDataType> doSetWindowLocalOptionValue(option: Option<T>, editor: VimEditor, value: T) {
    val options = getWindowLocalOptionStorage(editor)
    options[option.name] = value

    getParsedEffectiveOptionStorage(option, editor).remove(option.name)
  }

  private fun <T : VimDataType> getWindowLocalOptionValue(option: Option<T>, editor: VimEditor): T {
    check(option.declaredScope == LOCAL_TO_WINDOW || option.declaredScope == GLOBAL_OR_LOCAL_TO_WINDOW)

    // This should never return null, because we initialise local options when initialising the editor, even when adding
    // options dynamically, e.g. registering an extension. We fall back to global option, just in case
    val value = tryGetWindowLocalOptionValue(option, editor)
    strictModeAssert(value != null, "Window local option value is missing: ${option.name}")
    return value ?: getGlobalOptionValue(option)
  }

  private fun <T : VimDataType> tryGetWindowLocalOptionValue(option: Option<T>, editor: VimEditor): T? {
    val options = getWindowLocalOptionStorage(editor)

    // We set the value via Option<T> so it's safe to cast to T
    @Suppress("UNCHECKED_CAST")
    return options[option.name] as? T
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