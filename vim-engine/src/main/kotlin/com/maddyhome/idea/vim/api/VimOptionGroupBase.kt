/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.helper.StrictMode
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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

abstract class VimOptionGroupBase : VimOptionGroup {
  private val storage = OptionStorage()
  private val listeners = OptionListenersImpl(storage, injector.editorGroup)
  private val parsedValuesCache = ParsedValuesCache(storage, injector.vimStorageService)
  private var inInitVimRc = false

  init {
    addOptionValueOverride(Options.hlsearch, object : GlobalOptionValueOverride<VimInt> {
      override fun getGlobalValue(storedValue: OptionValue<VimInt>, editor: VimEditor?) = storedValue

      override fun setGlobalValue(
        storedValue: OptionValue<VimInt>,
        newValue: OptionValue<VimInt>,
        editor: VimEditor?,
      ): Boolean {
        // We normally only notify of changes if the option value has actually changed, but we want to refresh search
        // highlights if we search, call `:nohlsearch` and then `:set hlsearch`. The value of 'hlsearch' hasn't changed,
        // but we still want to notify
        return true
      }
    })
  }

  override fun initialiseOptions() {
    Options.initialise()
  }

  override fun initialiseLocalOptions(
    editor: VimEditor,
    sourceEditor: VimEditor?,
    scenario: LocalOptionInitialisationScenario,
  ) {
    // Called for all editors. Ensures that we eagerly initialise all local values, including the per-window "global"
    // values of local-to-window options. Note that global options are not eagerly initialised - the value is the
    // default value unless explicitly set.

    // Don't do anything if we're previously initialised the editor. Otherwise, we'll reset options back to defaults
    if (storage.isOptionStorageInitialised(editor)) {
      return
    }

    val strategy = OptionInitialisationStrategy(storage)
    if (scenario == LocalOptionInitialisationScenario.DEFAULTS) {
      check(sourceEditor == null) { "sourceEditor must be null when initialising the default options" }
      strategy.initialiseToDefaults(editor)
      return
    }

    check(sourceEditor != null) { "sourceEditor must not be null for scenario: $scenario" }
    when (scenario) {
      LocalOptionInitialisationScenario.FALLBACK,
      LocalOptionInitialisationScenario.CMD_LINE,
        -> strategy.initialiseCloneCurrentState(sourceEditor, editor)

      LocalOptionInitialisationScenario.SPLIT -> strategy.initialiseForSplitCurrentWindow(sourceEditor, editor)
      LocalOptionInitialisationScenario.EDIT -> strategy.initialiseForEditingNewBuffer(sourceEditor, editor)
      LocalOptionInitialisationScenario.NEW -> strategy.initialiseForNewBufferInNewWindow(sourceEditor, editor)
      else -> {}
    }
  }

  override fun startInitVimRc() {
    inInitVimRc = true
  }

  override fun endInitVimRc() {
    inInitVimRc = false
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

  protected open fun <T : VimDataType> addOptionValueOverride(
    option: Option<T>,
    override: OptionValueOverride<T>,
  ): Unit =
    storage.addOptionValueOverride(option, override)

  override fun <T : VimDataType> getOptionValue(option: Option<T>, scope: OptionAccessScope): T =
    getOptionValueInternal(option, scope).value

  protected open fun <T : VimDataType> getOptionValueInternal(
    option: Option<T>,
    scope: OptionAccessScope,
  ): OptionValue<T> =
    storage.getOptionValue(option, scope)

  override fun <T : VimDataType> setOptionValue(option: Option<T>, scope: OptionAccessScope, value: T) {
    option.checkIfValueValid(value, value.toOutputString())
    // The value is being explicitly set. [resetDefaultValue] is used to set the default value
    // Track if this option was explicitly set from ~/.ideavimrc during IdeaVim startup
    val optionValue = if (inInitVimRc) OptionValue.InitVimRc(value) else OptionValue.User(value)
    doSetOptionValue(option, scope, optionValue)
  }

  protected open fun <T : VimDataType> setOptionValueInternal(
    option: Option<T>,
    scope: OptionAccessScope,
    value: OptionValue<T>,
  ) {
    option.checkIfValueValid(value.value, value.value.toOutputString())
    doSetOptionValue(option, scope, value)
  }

  override fun <T : VimDataType> resetToDefaultValue(option: Option<T>, scope: OptionAccessScope) {
    doSetOptionValue(option, scope, OptionValue.Default(option.defaultValue))
  }

  override fun <T : VimDataType> resetToGlobalValue(option: Option<T>, scope: OptionAccessScope, editor: VimEditor) {
    val newValue: OptionValue<T> = if (scope is OptionAccessScope.LOCAL && option.declaredScope.isGlobalLocal()
      && (option is NumberOption || option is ToggleOption)
    ) {
      OptionValue.Default(option.unsetValue)
    } else {
      storage.getOptionValue(option, OptionAccessScope.GLOBAL(editor))
    }
    doSetOptionValue(option, scope, newValue)
  }

  private fun <T : VimDataType> doSetOptionValue(
    option: Option<T>,
    scope: OptionAccessScope,
    optionValue: OptionValue<T>,
  ) {
    if (storage.setOptionValue(option, scope, optionValue)) {
      when (scope) {
        is OptionAccessScope.EFFECTIVE -> {
          parsedValuesCache.reset(option, scope.editor)
          listeners.onEffectiveValueChanged(option, scope.editor)
        }

        is OptionAccessScope.LOCAL -> {
          parsedValuesCache.reset(option, scope.editor)
          listeners.onLocalValueChanged(option, scope.editor)
        }

        is OptionAccessScope.GLOBAL -> {
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
    // Reset all options to default values at effective scope. This will fire any listeners and clear any caches
    // Note that this is NOT the equivalent of calling `:set {option}&` on each option in turn. For number-based
    // global-local options that have previously set the local value, `:set {option}&` will copy the global value to the
    // local value. `:set all&` resets back to the original defaults, and global-local options will have a local value
    // of `-1`. We have to implement this manually
    val effectiveScope = OptionAccessScope.EFFECTIVE(editor)
    val localScope = OptionAccessScope.LOCAL(editor)
    Options.getAllOptions().forEach { option ->
      resetToDefaultValue(option, effectiveScope)
      if (option.declaredScope.isGlobalLocal()) {
        setOptionValue(option, localScope, option.unsetValue)
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
    listener: GlobalOptionChangeListener,
  ) {
    check(option.declaredScope == GLOBAL)
    listeners.addGlobalOptionChangeListener(option.name, listener)
  }

  override fun <T : VimDataType> removeGlobalOptionChangeListener(
    option: Option<T>,
    listener: GlobalOptionChangeListener,
  ) {
    listeners.removeGlobalOptionChangeListener(option.name, listener)
  }

  override fun <T : VimDataType> addEffectiveOptionValueChangeListener(
    option: Option<T>,
    listener: EffectiveOptionValueChangeListener,
  ) {
    listeners.addEffectiveOptionValueChangeListener(option.name, listener)
  }

  override fun <T : VimDataType> removeEffectiveOptionValueChangeListener(
    option: Option<T>,
    listener: EffectiveOptionValueChangeListener,
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
    fun initialiseNewOptionDefaultValuesForWindow(editor: VimEditor) {
      when (option.declaredScope) {
        GLOBAL -> {}
        LOCAL_TO_BUFFER -> {
          storage.setOptionValue(option, OptionAccessScope.LOCAL(editor), OptionValue.Default(option.defaultValue))
        }

        LOCAL_TO_WINDOW -> {
          storage.setOptionValue(option, OptionAccessScope.GLOBAL(editor), OptionValue.Default(option.defaultValue))
          storage.setOptionValue(option, OptionAccessScope.LOCAL(editor), OptionValue.Default(option.defaultValue))
        }

        GLOBAL_OR_LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_WINDOW -> {
          storage.setOptionValue(option, OptionAccessScope.LOCAL(editor), OptionValue.Default(option.unsetValue))
        }
      }
    }

    if (option.declaredScope != LOCAL_TO_WINDOW) {
      storage.setOptionValue(option, OptionAccessScope.GLOBAL(null), OptionValue.Default(option.defaultValue))
    }
    initialiseNewOptionDefaultValuesForWindow(injector.fallbackWindow)
    injector.editorGroup.getEditors().forEach(::initialiseNewOptionDefaultValuesForWindow)
  }
}

interface OptionValueOverride<T : VimDataType>

/**
 * Used to override the effective value of a global Vim option, typically with the value of an IDE setting
 *
 * The sweet spot for mapping Vim options and IDE settings is local Vim options and local (or global-local) IDE
 * settings. However, some Vim options are global, which can make sensible behaviour tricky. The important rule is that
 * we don't want to write to persistent IDE settings, so typically, the behaviour will (probably) be like this:
 *
 * * If the IDE setting is local or global-local, then get the local IDE value. Set should set the local value of ALL
 *   editors (if different)
 * * If the IDE setting is global, then get the value. Set will be on a case-by-case basis. Ideally, we don't set at
 *   all - we don't want to write to persistent settings. If the Vim option's behaviour is implemented by IdeaVim, then
 *   this just works. If it's not, then we have to figure out what's the best way.
 */
interface GlobalOptionValueOverride<T : VimDataType> : OptionValueOverride<T> {

  /**
   * Gets an overridden value of a global Vim option
   *
   * @param storedValue The current value of the Vim option. This will always be valid, possibly the default value.
   * @param editor      The current editor. Can be null as global options don't require an editor
   * @return Return the overridden value of the option, or [storedValue] if there are no changes
   */
  fun getGlobalValue(storedValue: OptionValue<T>, editor: VimEditor?): OptionValue<T>

  /**
   * Sets the overridden value of a global Vim option
   *
   * The behaviour of this method is heavily dependent on the scope of the related IDE setting.
   *
   * Note that this method does not get called during initialisation, since we don't explicitly or eagerly initialise
   * global values. This doesn't cause problems with current implementations because we always initialise to defaults,
   * and we never need to do anything with the initial default value, but we might want to address this at some point.
   *
   * @param storedValue The current value of the Vim option. This will always be valid, possibly the default value.
   * @param newValue    The new value set by IdeaVim
   * @param editor      The current editor. Can be null as global options don't require an editor
   * @return Returns `true` if the applied new value is different to the current stored value. If the function does
   *         nothing else, it needs to return this value.
   */
  fun setGlobalValue(storedValue: OptionValue<T>, newValue: OptionValue<T>, editor: VimEditor?): Boolean
}

/**
 * Used to override the local/effective value of an option in order to allow IDE backed option values
 *
 * This allows a derived instance of [VimOptionGroupBase] to register providers that can override the local/effective
 * value of a stored Vim option. When getting the local value, an override provider can return the current state of an
 * IDE setting, and when setting the value, it can change the IDE setting.
 *
 * Ideally, this class would be a protected nested class of [VimOptionGroupBase], since it should only be applicable to
 * implementors, but it's used by private helper classes, so needs to be public.
 */
interface LocalOptionValueOverride<T : VimDataType> : OptionValueOverride<T> {
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
  fun getLocalValue(storedValue: OptionValue<T>?, editor: VimEditor): OptionValue<T>

  /**
   * Sets the local/effective value for the current option.
   *
   * This method is called when the currently overridden option's local (and therefore effective) value is set, either
   * to a default value or to a new value. It can be used to map Vim options to equivalent IDE settings. For example,
   * if the new value is [OptionValue.User] or [OptionValue.External], the user is explicitly setting a value (or the
   * option is being initialised from a window where the option has been overridden and set externally to IdeaVim). In
   * this case, an implementation would want to update IDE settings.
   *
   * If the new value is [OptionValue.Default], an implementation could reset the current IDE setting to a default
   * value, likely also from the IDE. However, an implementation shouldn't reset IDE settings during initialisation.
   * The method is passed what IdeaVim thinks the current value is. This value will be null during initialisation
   * (because there isn't a previous value yet!), and this fact can be used to avoid resetting to default during
   * initialisation.
   *
   * @param storedValue The current stored value of the Vim option. This will only be `null` during initialisation. The
   * stored value will be the last value set either explicitly with `:set` commands, or defaults. It will not be the
   * overridden result of previous calls to [getLocalValue].
   * @param newValue The new value being set for the Vim option.
   * @param editor The [VimEditor] instance in which the option should be set.
   * @return `true` if the new, overridden local value is different to [storedValue]. Note that this should be the
   * result of comparing [OptionValue.value] instances, not [OptionValue] instances - we care about the value being
   * changed, not if the value has changed from [OptionValue.Default] to [OptionValue.User].
   */
  fun setLocalValue(storedValue: OptionValue<T>?, newValue: OptionValue<T>, editor: VimEditor): Boolean

  /**
   * Allows the override to veto copying the option value from the source editor to the target editor
   *
   * This is required for the `'wrap'` option in IntelliJ IDEs, because IntelliJ has different options for different
   * editor kinds which can lead to unexpected results when trying to initialise a console editor from a main editor.
   */
  fun canInitialiseOptionFrom(sourceEditor: VimEditor, targetEditor: VimEditor): Boolean = true
}

/**
 * Provides a base implementation to map a local Vim option to a global-local external setting
 *
 * Most editor settings in IntelliJ are global-local; they have a persistent global value that can be overridden by a
 * value local to the current editor. This base class assumes we never want to set the global external setting, and will
 * set the effective/local external value instead.
 *
 * It is not possible to remove the local value in IntelliJ's global-local setting. The best we can do is to set the
 * value either to a copy of the global external setting value when resetting the option (`:set {option}&`). But if the
 * user changes that external global value, it won't be reflected in the effective value.
 *
 * Setting the global value of the Vim option does not modify the external setting at all - the global value is a
 * Vim-only value used to initialise new windows.
 */
abstract class LocalOptionToGlobalLocalExternalSettingMapper<T : VimDataType>(protected val option: Option<T>) :
  LocalOptionValueOverride<T> {

  /**
   * True if the user can modify the local value of the external global-local setting.
   *
   * If this value is false, there is no user facing UI to set the local value of the external global-local setting.
   * In this case, the setting is in practice global, from the user's perspective. If the user changes the global value
   * of the external setting, it would be reasonable to override the IdeaVim value. Conversely, it would be confusing if
   * the global external value is changed and the editor doesn't update, like it would with IdeaVim disabled (even
   * though it's also not possible to have different values in different editors without IdeaVim)
   *
   * For example, in IntelliJ, `'breakindent'` would return `false`, as there is no way to modify the local value of
   * `EditorSettings.isUseCustomSoftWrapIndent`. However, `'list'` would return `true`, because it's possible to show or
   * hide whitespace locally with _View | Active Editor | Show Whitespaces_.
   */
  protected abstract val canUserModifyExternalLocalValue: Boolean

  override fun getLocalValue(storedValue: OptionValue<T>?, editor: VimEditor): OptionValue<T> {
    // Always return the current effective IntelliJ editor setting, regardless of the current IdeaVim value - the user
    // might have changed the value through the IDE. This means `:setlocal wrap?` will show the current value
    val ideValue = getEffectiveExternalValue(editor)

    // Tell the caller how the value was set as well as what the value is. This is used when copying values to a new
    // window and deciding if the IntelliJ value should be set - we don't want to set the IntelliJ value if the current
    // value is a default. We do want to set it when the user has explicitly set the value, either through the IDE or
    // with Vim commands.
    return when (storedValue) {
      is OptionValue.Default -> if (ideValue != getGlobalExternalValue(editor)) {
        OptionValue.External(ideValue)
      } else {
        OptionValue.Default(ideValue)
      }

      is OptionValue.External,
      is OptionValue.InitVimRc,
      is OptionValue.User,
      null,
        -> {
        // If the stored value matches the IDE value, return the stored value. If it has changed, it's been changed
        // externally. Note that stored value might be external. IdeaVim will never set that, but can copy it when
        // initialising a new window
        storedValue.takeUnless { it?.value != ideValue } ?: OptionValue.External(ideValue)
      }
    }
  }

  override fun setLocalValue(storedValue: OptionValue<T>?, newValue: OptionValue<T>, editor: VimEditor): Boolean {
    when (newValue) {
      is OptionValue.Default -> {
        // storedValue will only be null during initialisation, when we're setting the value for the first time and
        // therefore don't have a previous value. This only matters if we're setting the default, in which case we do
        // nothing, as we want to treat the current IntelliJ value as default.
        if (storedValue != null) {
          doResetLocalExternalValueToGlobal(editor)
        }
      }

      is OptionValue.InitVimRc -> {
        // Externally mapped options typically set the equivalent IDE setting's local value, rather than the persistent
        // global value. However, this means that modifying the global IDE value does not update/override currently open
        // editors, which can lead to user confusion. But IdeaVim can't simply reset all local values when a global
        // value changes - this isn't normal behaviour for the IDE. It would also reset values that were explicitly set
        // by the user, either through the IDE or through Vim commands.
        // This option value type means that the option has been set during initialisation, while evaluating the
        // `~/.ideavimrc` file. Since this value will be used to initialise all subsequent windows, it can be considered
        // to be a kind of "global" value (not to be confused with OptionDeclaredScope.GLOBAL). It is not unreasonable
        // to reset this "global" value when the IDE setting's global value changes.
        // While setting the local value, behave just like OptionValue.User
        if (getEffectiveExternalValue(editor) != newValue.value) {
          doSetLocalExternalValue(editor, newValue.value)
        }
      }

      is OptionValue.External -> {
        // The new value has been explicitly set by the user through the IDE, rather than using Vim commands. The only
        // way to get an External instance is through the getter for this option, which means we know this was copied
        // from an existing window/buffer and is being applied as part of initialisation.
        // It's been explicitly set by a user, so we can explicitly set the IntelliJ value. However, only set it if the
        // current value is different. Since IntelliJ settings are global-local, setting the value will prevent us from
        // setting it from the UI (unless there's a UI for the local value). This isn't foolproof, but it helps.
        if (getEffectiveExternalValue(editor) != newValue.value) {
          doSetLocalExternalValue(editor, newValue.value)
        }
      }

      is OptionValue.User -> {
        // The user is explicitly setting a value, so update the IntelliJ value
        if (getEffectiveExternalValue(editor) != newValue.value) {
          doSetLocalExternalValue(editor, newValue.value)
        }
      }
    }

    return storedValue?.value != newValue.value
  }

  private fun doSetLocalExternalValue(editor: VimEditor, value: T) {
    when (option.declaredScope) {
      LOCAL_TO_BUFFER -> setBufferLocalExternalValue(editor, value)
      LOCAL_TO_WINDOW -> setLocalExternalValue(editor, value)
      else -> error("Invalid declared option scope")
    }
  }

  /**
   * Set the IDE value for all open editors for the given document/buffer
   *
   * This function will set the IDE value for all open editors (windows) for the given document (buffer). An
   * implementer can override this if it is easier to set the IDE setting per-buffer.
   */
  protected open fun setBufferLocalExternalValue(editor: VimEditor, value: T) {
    // Set the value for the current editor, then set it for all other editors with the same buffer. During
    // initialisation, getEditors won't return the current editor (because it's not initialised) so set it explicitly.
    // This also means that the value might be set twice because VimEditor doesn't support equality
    setLocalExternalValue(editor, value)
    injector.editorGroup.getEditors(editor.document).forEach { setLocalExternalValue(it, value) }
  }

  private fun doResetLocalExternalValueToGlobal(editor: VimEditor) {
    when (option.declaredScope) {
      LOCAL_TO_BUFFER -> resetBufferLocalExternalValueToGlobal(editor.document)
      LOCAL_TO_WINDOW -> resetLocalExternalValueToGlobal(editor)
      else -> error("Invalid declared option scope")
    }
  }

  /**
   * Reset the external setting value for the given document/buffer to the global external value
   *
   * This function will reset the local value for all open editors/windows for the given document/buffer. An implementer
   * can override this if it is easier to reset the external setting per-buffer.
   */
  protected open fun resetBufferLocalExternalValueToGlobal(document: VimDocument) {
    injector.editorGroup.getEditors(document).forEach { resetLocalExternalValueToGlobal(it) }
  }

  /**
   * Reset the current external setting value to the global external value, if different
   *
   * Implementers can override this function if they need to do more complex reset, such as resetting two IDE values.
   * The overridden value should only update the local setting if the value has changed. This is especially important
   * if the IDE values are global-local - updating the IDE value might set the local value to a copy of the default
   * value, rather than leaving the local value "unset".
   */
  protected open fun resetLocalExternalValueToGlobal(editor: VimEditor) {
    val global = getGlobalExternalValue(editor)
    if (getEffectiveExternalValue(editor) != global) {
      doSetLocalExternalValue(editor, global)
    }
  }

  /**
   * Gets the global persistent value for the external setting.
   *
   * @param editor The current editor. Some external settings might have per-editor or per-file type global settings.
   * @return The global external value for the specified editor.
   */
  protected abstract fun getGlobalExternalValue(editor: VimEditor): T

  /**
   * Gets the current effective value external of the external setting.
   *
   * This will return the local value of the external setting, if set, and the global persistent value if not set.
   *
   * @param editor The editor to get the effective external value for.
   * @return The effective external value for the specified editor.
   */
  protected abstract fun getEffectiveExternalValue(editor: VimEditor): T

  /**
   * Sets the local external value for the given editor.
   *
   * @param editor The editor to set the effective external value for.
   * @param value The new value to set as the effective external value.
   */
  protected abstract fun setLocalExternalValue(editor: VimEditor, value: T)
}

/**
 * A base class for a global Vim option that is mapped to an IntelliJ global-local setting
 *
 * If we can't rely on IdeaVim's implementation for this option, we need to make sure that the IDE setting is correctly
 * updated to reflect the global value. We don't want to modify the global, persistent IDE setting, so we instead update
 * the local value of all editors (new editors are also correctly modified when they're initialised). By setting the
 * local value of all editors, we effectively mimic the behaviour of a global option. If we reset the option back to
 * default, we can reset all editors back to default too, either by copying the global IDE value, or by clearing the
 * global-local IDE setting override (if possible).
 */
abstract class GlobalOptionToGlobalLocalExternalSettingMapper<T : VimDataType>
  : GlobalOptionValueOverride<T> {

  /**
   * True if the user can modify the local value of the external global-local setting.
   *
   * If this value is false, there is no user facing UI to set the local value of the external global-local setting.
   * In this case, the setting is in practice global, from the user's perspective. If the user changes the global value
   * of the external setting, it would be reasonable to override the IdeaVim value. Conversely, it would be confusing if
   * the global external value is changed and the editor doesn't update, like it would with IdeaVim disabled (even
   * though it's also not possible to have different values in different editors without IdeaVim)
   *
   * This flag is mostly redundant for global Vim options. Both Vim option and external setting are treated as global.
   */
  protected abstract val canUserModifyExternalLocalValue: Boolean

  final override fun getGlobalValue(storedValue: OptionValue<T>, editor: VimEditor?): OptionValue<T> {
    return if (storedValue is OptionValue.Default) {
      // If we have an editor, return the local value. Since the IDE setting is global-local, this local value will
      // either be unset, and therefore the global value, or will be the locally set value, which we set when the user
      // explicitly sets the IdeaVim option
      val ideValue = editor?.let { getEffectiveExternalValue(it) } ?: getGlobalExternalValue()
      OptionValue.Default(ideValue)
    } else {
      storedValue
    }
  }

  final override fun setGlobalValue(
    storedValue: OptionValue<T>,
    newValue: OptionValue<T>,
    editor: VimEditor?,
  ): Boolean {
    if (newValue is OptionValue.Default) {
      val globalValue = getGlobalValue(storedValue, null)
      injector.editorGroup.getEditors().forEach { resetLocalExternalValue(it, globalValue.value) }
    } else {
      val globalValue = getGlobalValue(storedValue, null)
      if (globalValue.value != newValue.value) {
        injector.editorGroup.getEditors().forEach { setLocalExternalValue(it, newValue.value) }
      } else {
        injector.editorGroup.getEditors().forEach { resetLocalExternalValue(it, globalValue.value) }
      }
    }

    return storedValue.value != newValue.value
  }

  /**
   * Return the global persistent value for the external setting
   */
  protected abstract fun getGlobalExternalValue(): T

  /**
   * Return the current effective value of the external setting
   *
   * For a global-local external setting, this will be the local value if explicitly set, otherwise the global value.
   */
  protected abstract fun getEffectiveExternalValue(editor: VimEditor): T

  /**
   * Set the local value of the external setting
   */
  protected abstract fun setLocalExternalValue(editor: VimEditor, value: T)

  /**
   * Reset the local value of the external setting, either by removing the local setting, or setting to the given
   * default value
   */
  protected abstract fun resetLocalExternalValue(editor: VimEditor, defaultValue: T)
}


/**
 * Provides a base implementation to map a global-local Vim option to a global-local external setting
 *
 * This class will map a global-local Vim option to a global-local external setting. This isn't as easy as it sounds,
 * because we don't want to modify the global external value, as it is a persistent setting. Instead, we fake it by
 * setting the local external value for all editors, unless the editor has overridden the local Vim value.
 */
abstract class GlobalLocalOptionToGlobalLocalExternalSettingMapper<T : VimDataType>(protected val option: Option<T>) :
  GlobalOptionValueOverride<T>, LocalOptionValueOverride<T> {

  private var storedGlobalValue: OptionValue<T>? = null

  /**
   * True if the user can modify the local value of the external global-local setting.
   *
   * If this value is false, there is no user facing UI to set the local value of the external global-local setting.
   * In this case, the setting is in practice global, from the user's perspective. If the user changes the global value
   * of the external setting, it would be reasonable to override the IdeaVim value. Conversely, it would be confusing if
   * the global external value is changed and the editor doesn't update, like it would with IdeaVim disabled (even
   * though it's also not possible to have different values in different editors without IdeaVim)
   *
   * This flag is mostly redundant for global Vim options. Both Vim option and external setting are treated as global.
   */
  protected abstract val canUserModifyExternalLocalValue: Boolean

  override fun getGlobalValue(storedValue: OptionValue<T>, editor: VimEditor?): OptionValue<T> {
    // If we assume that the user cannot change the local value, it makes it a lot easier to know if the current
    // effective value is global or global-local (set by IdeaVim)
    assert(!canUserModifyExternalLocalValue)

    if (editor != null && storedValue is OptionValue.Default) {
      // IdeaVim thinks the global value is default, so return the global external value
      return OptionValue.Default(getGlobalExternalValue())
    }

    // Return the stored Vim value. Since this is a global-local value, we can't just return the current effective
    // external value, as it might represent the global or local value. Fortunately, we assume/know that the user cannot
    // change the local external value through the UI, so we don't need to worry about what the external value is. We
    // can just return what IdeaVim _thinks_ it is, and be confident that it's correct.
    return storedValue
  }

  override fun setGlobalValue(storedValue: OptionValue<T>, newValue: OptionValue<T>, editor: VimEditor?): Boolean {
    // Set the external value to reflect the new global Vim value. We don't want to set the global external value, as
    // this is a persistent setting, so we fake it by setting the local external setting of all editors. However, we
    // want to skip editors that have a local Vim value, which is also copied to the local external value when set.
    // Fortunately, the stored global Vim value tells us what the local external value should be. If the stored value is
    // default, then the local external value should match the current global external value, unless it's been
    // overridden locally. If the stored value isn't a default, then the local external value should match the stored
    // value. If the editor doesn't match either of these, then it's been set as a local value.
    val globalVimValue = if (storedValue is OptionValue.Default) getGlobalExternalValue() else storedValue.value
    injector.editorGroup.getEditors().forEach {
      if (getEffectiveExternalValue(it) == globalVimValue) {
        // This editor has an external value that matches the existing global value. Update the external value to
        // reflect the new external value, removing the local external value if we're resetting to default.
        if (newValue is OptionValue.Default) {
          removeLocalExternalValue(it)
        } else {
          setLocalExternalValue(it, newValue.value)
        }
      }
    }

    // Remember the new global Vim value - we need this when resetting the local Vim value back to global
    storedGlobalValue = newValue
    return storedValue.value != newValue.value
  }

  override fun getLocalValue(storedValue: OptionValue<T>?, editor: VimEditor): OptionValue<T> {
    // If we assume that the user cannot change the local value, it makes it a lot easier to know if the current
    // effective value is global or global-local (set by IdeaVim)
    assert(!canUserModifyExternalLocalValue)

    if (storedValue == null) {
      // The local IdeaVim value hasn't been initialised yet. All we can do is use the local/effective external value
      return OptionValue.External(getEffectiveExternalValue(editor))
    }

    if (storedValue is OptionValue.Default && storedValue.value != option.unsetValue) {
      // If the stored value is default, but not the initial "unset" value, that means the user has reset the option to
      // the global value with `:set[local] {option}<`, and the global value was a default value. The local external
      // value will already have been set to reflect this change, so we can just return the effective (local) external
      // value.
      return OptionValue.Default(getEffectiveExternalValue(editor))
    }

    // The stored value is either explicitly set by the user, or the unset value. Since we assume that the user cannot
    // modify the local external value, the only way it can be modified is by setting the IdeaVim value; then we can
    // also assume that the stored IdeaVim value correctly reflects the local external value. (And when unset, it will
    // correctly reflect the magic unset value rather than the effective value of the global value)
    return storedValue
  }

  override fun setLocalValue(storedValue: OptionValue<T>?, newValue: OptionValue<T>, editor: VimEditor): Boolean {
    if (newValue is OptionValue.Default) {
      // The option is being set to a default value. During initialisation, this will be unsetValue, and we don't want
      // to modify the external value (storedValue will be null only during initialisation).
      // But we do want to modify the external value if the option is being reset either to default or unset due to
      // `:set[local] {option}&` or `:set[local] {option}<`. Unfortunately, we don't always know what the external value
      // should be.
      // Specifically, if the user first explicitly sets the global Vim value, the local external value of all editors
      // is set to the new global value, to avoid setting the persistent global external value. Then, if the user
      // explicitly sets the local Vim value, the local external value of the current editor is updated.
      // Finally, if the user then tries to unset the local Vim value, the local external value should be reset back to
      // the global Vim value, which we don't have. Therefore, we must keep track of the stored Vim global value.
      if (storedValue != null) {
        val storedGlobalValue = this.storedGlobalValue
        if (newValue.value == option.unsetValue && storedGlobalValue != null && storedGlobalValue !is OptionValue.Default) {
          doForAllAffectedEditors(option, editor) { setLocalExternalValue(it, storedGlobalValue.value) }
        } else {
          doForAllAffectedEditors(option, editor) { removeLocalExternalValue(it) }
        }
      }
    } else {
      // The new value isn't default. It's been explicitly set by the user, so we should update the local external
      // value. We expect it to be OptionValue.User, rather than OptionValue.External, because global-local options do
      // not initialise values from the current value. But even so, just set the local external value.
      doForAllAffectedEditors(option, editor) { setLocalExternalValue(it, newValue.value) }
    }

    return storedValue?.value != newValue.value
  }

  private fun doForAllAffectedEditors(option: Option<T>, editor: VimEditor, action: (editor: VimEditor) -> Unit) {
    when (option.declaredScope) {
      GLOBAL_OR_LOCAL_TO_BUFFER -> injector.editorGroup.getEditors(editor.document).forEach { action(it) }
      GLOBAL_OR_LOCAL_TO_WINDOW -> action(editor)
      else -> StrictMode.fail("IdeaVim option must be global-local")
    }
  }

  /**
   * Get the current global external value
   */
  protected abstract fun getGlobalExternalValue(): T

  /**
   * Get the effective external value
   *
   * This value is the global external value, unless the local external value has been set as overriding the global
   * value.
   */
  protected abstract fun getEffectiveExternalValue(editor: VimEditor): T

  /**
   * Set the local external value
   *
   * Sets the local external value to the given value for the given editor
   *
   * Note that there is no global external value; we don't modify this value because it's a persistent value.
   */
  protected abstract fun setLocalExternalValue(editor: VimEditor, value: T)

  /**
   * Remove the local external value, leaving the global value as the effective value
   *
   * This method assumes that the implementation is able to remove the local value. No provision is made for a
   * workaround.
   */
  protected abstract fun removeLocalExternalValue(editor: VimEditor)
}

/**
 * A wrapper class for an option value that also tracks how it was set
 *
 * This is required in order to implement Vim options that are either completely or partially backed by IDE settings.
 * For example, the `'wrap'` Vim option is not implemented by IdeaVim at all. Soft wraps must be implemented by the host
 * editor. Similarly, IntelliJ has options that correspond to `'scrolloff'`, although the implementation is different to
 * Vim's. IdeaVim might be able to use the same values while providing a different implementation.
 *
 * Unless the Vim value is explicitly set, the IDE value should take precedence. This allows users to opt in to Vim
 * behaviour (`:set` or `~/.ideavimrc`), while still using the IDE to change settings. If the option has not been
 * explicitly set, then it has a default Vim value, but the effective value comes from the IDE. When set via the Vim
 * `:set` commands, the IDE value is updated to match. The user is free to update the value in the IDE, and this is
 * still reflected in the Vim option value, but treated internally as an external changed.
 *
 * When setting the effective value of local options, the global value is also updated. If a user opts in to modifying a
 * Vim option, the global value is also considered explicitly set and this is copied to any new windows during
 * initialisation, meaning new windows match the behaviour of the current window.
 *
 * Note that this class is an implementation detail of [VimOptionGroupBase] and derived instances, but cannot be
 * made into a protected nested class because it is used by private helper classes.
 */
sealed class OptionValue<T : VimDataType>(open val value: T) {
  /**
   * The option value has been set as a default value by IdeaVim
   *
   * When setting an option, the value is a Vim default value. When getting a default option, the value might come from
   * an IDE setting, but still uses the [Default] wrapper type.
   */
  class Default<T : VimDataType>(override val value: T) : OptionValue<T>(value)

  /**
   * The option has been set explicitly, by the user as part of the initial evaluation of `~/.ideavimrc`.
   *
   * This is very similar to [User], in that the value has been explicitly set, but it indicates that it was set from
   * the `~/.ideavimrc` script during plugin startup. This usually means that the option will propagate to subsequently
   * opened windows, which can make the option value feel like a "global" value (not to be confused with Vim's own
   * global option values).
   *
   * Since externally mapped options are typically mapped to an IDE setting's local value, changing the IDE's global
   * value can leave a user confused - why hasn't the setting updated in open windows? If the option was set during
   * plugin startup, IdeaVim can now identify values that should be considered "global" and update them when the IDE
   * setting's global value changes.
   *
   * This value is only used during plugin initialisation. If `~/.ideavimrc` is sourced or reloaded interactively, it is
   * evaluated in the context of the current window, and existing window/buffer options are not updated (as per Vim).
   * Therefore, any options set during this subsequent evaluation are considered to be [User].
   */
  class InitVimRc<T : VimDataType>(override val value: T) : OptionValue<T>(value)

  /**
   * The option value has been explicitly set by the user, by Vim commands
   *
   * The value has been set using the `:set` commands. When getting a value, this type is used if the value has been
   * explicitly set by the user and the corresponding IDE setting (if any) still has the same value.
   */
  class User<T : VimDataType>(override val value: T) : OptionValue<T>(value)

  /**
   * The option value has been explicitly set by the user, but changed through the IDE
   *
   * This type is only used if the option has previously been set by the user using Vim's `:set` commands, but the
   * current corresponding IDE setting no longer has the same value. This means that the user has explicitly set the
   * option via Vim, but changed it in the IDE. If this value is used to initialise an option in a new window, it is
   * treated as though the user explicitly set the option using Vim's `:set` commands.
   *
   * Note that the typical behaviour for externally mapped options is to modify the IDE setting's local value. In this
   * case, only the local value of the IDE setting is considered. Changes to the IDE setting's global value do not
   * override the local value unless some other mechanism resets the IDE setting's local value.
   */
  class External<T : VimDataType>(override val value: T) : OptionValue<T>(value)

  override fun equals(other: Any?): Boolean {
    // For equality, we don't care about how the value is set. We're only interested in the wrapped value.
    // Ideally, callers will compare `oldValue.value` with `newValue.value`. However, there is a bug in the IDE/compiler
    // that allows code like `if (optionValue<T> == T)` without showing a warning, but which will always return false.
    // See KTIJ-26930
    if (other is OptionValue<*>) {
      return other.value == value
    }
    return other == value
  }

  override fun hashCode(): Int = value.hashCode()
  override fun toString(): String = "OptionValue.${this::class.simpleName}($value)"
}


/**
 * Maintains storage of, and provides accessors to option values
 *
 * This class does not notify any listeners of changes, but provides enough information for a caller to handle this.
 */
private class OptionStorage {
  private val globalValues = mutableMapOf<String, OptionValue<out VimDataType>>()
  private val perWindowGlobalOptionsKey =
    Key<MutableMap<String, OptionValue<out VimDataType>>>("vimPerWindowGlobalOptions")
  private val localOptionsKey = Key<MutableMap<String, OptionValue<out VimDataType>>>("vimLocalOptions")
  private val overrides = mutableMapOf<String, OptionValueOverride<out VimDataType>>()

  fun <T : VimDataType> addOptionValueOverride(option: Option<T>, override: OptionValueOverride<T>) {
    overrides[option.name] = override
  }

  fun <T : VimDataType> getOptionValue(option: Option<T>, scope: OptionAccessScope): OptionValue<T> = when (scope) {
    is OptionAccessScope.EFFECTIVE -> getEffectiveValue(option, scope.editor)
    is OptionAccessScope.GLOBAL -> getGlobalValue(option, scope.editor)
    is OptionAccessScope.LOCAL -> getLocalValue(option, scope.editor)
  }

  fun <T : VimDataType> setOptionValue(option: Option<T>, scope: OptionAccessScope, value: OptionValue<T>): Boolean {
    return when (scope) {
      is OptionAccessScope.EFFECTIVE -> setEffectiveValue(option, scope.editor, value)
      is OptionAccessScope.GLOBAL -> setGlobalValue(option, scope.editor, value)
      is OptionAccessScope.LOCAL -> setLocalValue(option, scope.editor, value)
    }
  }

  fun <T : VimDataType> getOptionValueForInitialisation(
    option: Option<T>,
    sourceScope: OptionAccessScope,
    targetScope: OptionAccessScope,
  ): OptionValue<T> {
    fun getScopeEditor(scope: OptionAccessScope) = when (scope) {
      is OptionAccessScope.EFFECTIVE -> null  // Should never happen
      is OptionAccessScope.GLOBAL -> scope.editor
      is OptionAccessScope.LOCAL -> scope.editor
    }

    // If this option is a local option, and has an override, give it the chance to veto initialising the option from
    // the source editor. If it's not a local option, getLocalOptionValueOverride will return null.
    // This is currently required to handle 'wrap' in IntelliJ, which stores different values based on editor kind, so
    // we don't initialise a console editor with defaults/current values from a main editor
    val sourceEditor = getScopeEditor(sourceScope)
    val targetEditor = getScopeEditor(targetScope)
    return if (sourceEditor != null && targetEditor != null
      && getLocalOptionValueOverride(option)?.canInitialiseOptionFrom(sourceEditor, targetEditor) == false
    ) {
      OptionValue.Default(option.defaultValue)
    } else {
      getOptionValue(option, sourceScope)
    }
  }

  fun isOptionStorageInitialised(editor: VimEditor): Boolean {
    // Local window option storage will exist if we've previously initialised this editor
    return injector.vimStorageService.getDataFromWindow(editor, localOptionsKey) != null
  }

  fun isLocalToBufferOptionStorageInitialised(editor: VimEditor) =
    injector.vimStorageService.getDataFromBuffer(editor, localOptionsKey) != null

  private fun <T : VimDataType> getGlobalOptionValueOverride(option: Option<T>): GlobalOptionValueOverride<T>? {
    @Suppress("UNCHECKED_CAST")
    return overrides[option.name] as? GlobalOptionValueOverride<T>
  }

  private fun <T : VimDataType> getLocalOptionValueOverride(option: Option<T>): LocalOptionValueOverride<T>? {
    @Suppress("UNCHECKED_CAST")
    return overrides[option.name] as? LocalOptionValueOverride<T>
  }

  private fun <T : VimDataType> getEffectiveValue(option: Option<T>, editor: VimEditor): OptionValue<T> {
    return when (option.declaredScope) {
      GLOBAL -> getGlobalValue(option, editor)
      LOCAL_TO_BUFFER -> getLocalValue(option, editor)
      LOCAL_TO_WINDOW -> getLocalValue(option, editor)
      GLOBAL_OR_LOCAL_TO_BUFFER -> {
        getLocalValue(option, editor).takeUnless { it.value == option.unsetValue }
          ?: getGlobalValue(option, editor)
      }

      GLOBAL_OR_LOCAL_TO_WINDOW -> {
        getLocalValue(option, editor).takeUnless { it.value == option.unsetValue }
          ?: getGlobalValue(option, editor)
      }
    }
  }

  private fun <T : VimDataType> getGlobalValue(option: Option<T>, editor: VimEditor?): OptionValue<T> {
    val values = if (option.declaredScope == LOCAL_TO_WINDOW) {
      check(editor != null) { "Editor must be provided for local options" }
      getPerWindowGlobalOptionStorage(editor)
    } else {
      globalValues
    }
    return getOverriddenGlobalValue(
      option,
      getStoredValue(values, option) ?: OptionValue.Default(option.defaultValue),
      editor
    )
  }

  private fun <T : VimDataType> getLocalValue(option: Option<T>, editor: VimEditor): OptionValue<T> {
    return when (option.declaredScope) {
      GLOBAL -> getGlobalValue(option, editor)
      LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_BUFFER -> getBufferLocalValue(option, editor)
      LOCAL_TO_WINDOW, GLOBAL_OR_LOCAL_TO_WINDOW -> getWindowLocalValue(option, editor)
    }
  }

  private fun <T : VimDataType> getBufferLocalValue(option: Option<T>, editor: VimEditor): OptionValue<T> {
    val values = getBufferLocalOptionStorage(editor)
    val value = getOverriddenLocalValue(option, getStoredValue(values, option), editor)
    strictModeAssert(value != null) { "Unexpected uninitialised buffer local value: ${option.name}" }
    return value ?: getEmergencyFallbackLocalValue(option, editor)
  }

  private fun <T : VimDataType> getWindowLocalValue(option: Option<T>, editor: VimEditor): OptionValue<T> {
    val values = getWindowLocalOptionStorage(editor)
    val value = getOverriddenLocalValue(option, getStoredValue(values, option), editor)
    strictModeAssert(value != null) { "Unexpected uninitialised window local value: ${option.name}" }
    return value ?: getEmergencyFallbackLocalValue(option, editor)
  }

  private fun <T : VimDataType> getOverriddenGlobalValue(
    option: Option<T>,
    storedValue: OptionValue<T>,
    editor: VimEditor?,
  ): OptionValue<T> {
    getGlobalOptionValueOverride(option)?.let {
      return it.getGlobalValue(storedValue, editor)
    }
    return storedValue
  }

  private fun <T : VimDataType> getOverriddenLocalValue(
    option: Option<T>,
    storedValue: OptionValue<T>?,
    editor: VimEditor,
  ): OptionValue<T>? {
    getLocalOptionValueOverride(option)?.let {
      return it.getLocalValue(storedValue, editor)
    }
    return storedValue
  }

  private fun <T : VimDataType> setEffectiveValue(
    option: Option<T>,
    editor: VimEditor,
    value: OptionValue<T>,
  ): Boolean {
    return when (option.declaredScope) {
      GLOBAL -> setGlobalValue(option, editor, value)
      LOCAL_TO_BUFFER, LOCAL_TO_WINDOW -> setLocalValue(option, editor, value).also {
        setGlobalValue(option, editor, value)
      }

      GLOBAL_OR_LOCAL_TO_BUFFER, GLOBAL_OR_LOCAL_TO_WINDOW -> {
        var changed = false
        if (getLocalValue(option, editor).value != option.unsetValue) {
          changed = setLocalValue(
            option, editor,
            if (option is NumberOption || option is ToggleOption) value else OptionValue.Default(option.unsetValue)
          )
        }
        setGlobalValue(option, editor, value) || changed
      }
    }
  }

  private fun <T : VimDataType> setGlobalValue(option: Option<T>, editor: VimEditor?, value: OptionValue<T>): Boolean {
    val values = if (option.declaredScope == LOCAL_TO_WINDOW) {
      check(editor != null) { "Editor must be provided for local options" }
      getPerWindowGlobalOptionStorage(editor)
    } else {
      globalValues
    }
    getGlobalOptionValueOverride(option)?.let {
      val storedValue = getStoredValue(values, option) ?: OptionValue.Default(option.defaultValue)
      val changed = it.setGlobalValue(storedValue, value, editor)
      setStoredValue(values, option.name, value)
      return changed
    }
    return setStoredValue(values, option.name, value)
  }

  private fun <T : VimDataType> setLocalValue(option: Option<T>, editor: VimEditor, value: OptionValue<T>): Boolean {
    return when (option.declaredScope) {
      GLOBAL -> setGlobalValue(option, editor, value)
      LOCAL_TO_BUFFER,
      GLOBAL_OR_LOCAL_TO_BUFFER,
        -> setLocalValue(getBufferLocalOptionStorage(editor), option, editor, value)

      LOCAL_TO_WINDOW,
      GLOBAL_OR_LOCAL_TO_WINDOW,
        -> setLocalValue(getWindowLocalOptionStorage(editor), option, editor, value)
    }
  }

  private fun <T : VimDataType> setLocalValue(
    values: MutableMap<String, OptionValue<out VimDataType>>,
    option: Option<T>,
    editor: VimEditor,
    value: OptionValue<T>,
  ): Boolean {
    getLocalOptionValueOverride(option)?.let {
      val storedValue = getStoredValue(values, option) // Will be null during initialisation!
      val changed = it.setLocalValue(storedValue, value, editor)
      setStoredValue(values, option.name, value)
      return changed
    }
    return setStoredValue(values, option.name, value)
  }

  private fun <T : VimDataType> getStoredValue(
    values: MutableMap<String, OptionValue<out VimDataType>>,
    option: Option<T>,
  ): OptionValue<T>? {
    // We can safely suppress this because we know we only set it with a strongly typed option and only get it with a
    // strongly typed option
    @Suppress("UNCHECKED_CAST")
    return values[option.name] as? OptionValue<T>
  }

  private fun <T : VimDataType> setStoredValue(
    values: MutableMap<String, OptionValue<out VimDataType>>,
    key: String,
    value: OptionValue<T>,
  ): Boolean {
    // We need to notify listeners if the actual value changes, so we don't care if it's changed from being default to
    // now being explicitly set, only if the value is different. However, we will always update the value - we want to
    // know if we've gone from default to explicit, even if the value is the same
    val oldValue = values[key]
    values[key] = value
    return oldValue?.value != value.value
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
  private fun <T : VimDataType> getEmergencyFallbackLocalValue(option: Option<T>, editor: VimEditor?): OptionValue<T> {
    return if (option.declaredScope.isGlobalLocal()) {
      OptionValue.Default(option.unsetValue)
    } else {
      getGlobalValue(option, editor)
    }
  }

  // We can't use StrictMode.assert because it checks an option, which calls into VimOptionGroupBase...
  private inline fun strictModeAssert(condition: Boolean, lazyMessage: () -> String) {
    if (globalValues[Options.ideastrictmode.name]?.value?.toVimNumber()?.booleanValue == true && !condition) {
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
   * copied, both the local and per-window "global" values. Buffer local options are already initialised.
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
      storage.setOptionValue(option, scope, OptionValue.Default(option.defaultValue))
    }
  }

  private fun copyPerWindowGlobalValues(sourceEditor: VimEditor, targetEditor: VimEditor) {
    val sourceGlobalScope = OptionAccessScope.GLOBAL(sourceEditor)
    val targetGlobalScope = OptionAccessScope.GLOBAL(targetEditor)
    forEachOption(LOCAL_TO_WINDOW) { option ->
      val value = getOptionValueForInitialisation(option, sourceGlobalScope, targetGlobalScope)
      storage.setOptionValue(option, targetGlobalScope, value)
    }
  }

  private fun initialiseLocalToBufferOptions(editor: VimEditor) {
    if (!storage.isLocalToBufferOptionStorageInitialised(editor)) {
      val globalScope = OptionAccessScope.GLOBAL(editor)
      val localScope = OptionAccessScope.LOCAL(editor)
      forEachOption(LOCAL_TO_BUFFER) { option ->
        val value = getOptionValueForInitialisation(option, globalScope, localScope)
        storage.setOptionValue(option, localScope, value)
      }
      forEachOption(GLOBAL_OR_LOCAL_TO_BUFFER) { option ->
        storage.setOptionValue(option, localScope, OptionValue.Default(option.unsetValue))
      }
    }
  }

  private fun copyLocalToBufferLocalValues(sourceEditor: VimEditor, targetEditor: VimEditor) {
    val sourceLocalScope = OptionAccessScope.LOCAL(sourceEditor)
    val targetLocalScope = OptionAccessScope.LOCAL(targetEditor)
    forEachOption(LOCAL_TO_BUFFER) { option ->
      val value = getOptionValueForInitialisation(option, sourceLocalScope, targetLocalScope)
      storage.setOptionValue(option, targetLocalScope, value)
    }
    forEachOption(GLOBAL_OR_LOCAL_TO_BUFFER) { option ->
      val value = getOptionValueForInitialisation(option, sourceLocalScope, targetLocalScope)
      storage.setOptionValue(option, targetLocalScope, value)
    }
  }

  private fun initialiseLocalToWindowOptions(editor: VimEditor) {
    val globalScope = OptionAccessScope.GLOBAL(editor)
    val localScope = OptionAccessScope.LOCAL(editor)
    forEachOption(LOCAL_TO_WINDOW) { option ->
      val value = getOptionValueForInitialisation(option, globalScope, localScope)
      storage.setOptionValue(option, localScope, value)
    }
    forEachOption(GLOBAL_OR_LOCAL_TO_WINDOW) { option ->
      storage.setOptionValue(option, localScope, OptionValue.Default(option.unsetValue))
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
      val value = getOptionValueForInitialisation(option, sourceLocalScope, targetLocalScope)
      storage.setOptionValue(option, targetLocalScope, value)
    }
    forEachOption(GLOBAL_OR_LOCAL_TO_WINDOW) { option ->
      val value = getOptionValueForInitialisation(option, sourceLocalScope, targetLocalScope)
      storage.setOptionValue(option, targetLocalScope, value)
    }
  }

  private fun forEachOption(scope: OptionDeclaredScope, action: (Option<VimDataType>) -> Unit) {
    Options.getAllOptions().forEach { option -> if (option.declaredScope == scope) action(option) }
  }

  /**
   * Get the option value for initialisation. Either the value from scope, or default, if the option is local-noglobal
   *
   * Some options should not be copied from global, or from another source, during initialisation.
   * For example, Vim doesn't copy 'scroll' from one window to another, or 'filetype' from one buffer to another.
   * IntelliJ extends this with 'bomb', 'fileencoding' and 'fileformat', because they don't behave the same as Vim.
   * Vim will use these options when saving. IntelliJ doesn't work like that - it will convert the file immediately
   * (and saving is a little non-deterministic). So if we copy the value, and it's different to what the IDE detects,
   * we will erroneously try to modify the file.
   * Therefore, we treat these additional options as local-noglobal, and don't copy them during initialisation. The
   * value is always set to default, which will use the current value detected by the IDE.
   * See `:help local-noglobal`
   */
  private fun getOptionValueForInitialisation(
    option: Option<VimDataType>,
    sourceScope: OptionAccessScope,
    targetScope: OptionAccessScope,
  ): OptionValue<VimDataType> {
    return if (option.isLocalNoGlobal) {
      OptionValue.Default(option.defaultValue)
    } else {
      storage.getOptionValueForInitialisation(option, sourceScope, targetScope)
    }
  }
}


private fun <T : VimDataType> OptionStorage.isUnsetValue(option: Option<T>, editor: VimEditor): Boolean {
  return this.getOptionValue(option, OptionAccessScope.LOCAL(editor)).value == option.unsetValue
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
      LOCAL_TO_BUFFER, LOCAL_TO_WINDOW -> { /* Setting global value of a local option. No need to notify anyone */
      }

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
    } else if (option.declaredScope == GLOBAL_OR_LOCAL_TO_BUFFER) {
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
    check(!option.declaredScope.isGlobalLocal()) { "Global-local options cannot currently be cached" }

    val cachedValues = getStorage(option, editor)

    // Unless the user is calling this method multiple times with different providers, we can be confident this cast
    // will succeed. Editor will only be null with global options, so it's safe to use null
    @Suppress("UNCHECKED_CAST")
    return cachedValues.getOrPut(option.name) {
      val scope = if (editor == null) OptionAccessScope.GLOBAL(null) else OptionAccessScope.EFFECTIVE(editor)
      provider(optionStorage.getOptionValue(option, scope).value)
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
