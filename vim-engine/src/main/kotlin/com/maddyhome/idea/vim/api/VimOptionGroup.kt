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
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import com.maddyhome.idea.vim.options.StringListOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import org.jetbrains.annotations.TestOnly

interface VimOptionGroup {
  /**
   * Called to initialise the list of available options
   *
   * This function must be idempotent, as it is called each time the plugin is enabled.
   */
  fun initialiseOptions()

  /**
   * Initialise the local to buffer and local to window option values for this editor
   *
   * Depending on the initialisation scenario, the local-to-buffer, local-to-window and/or global-local options are
   * initialised. The scenario dictates where the local options get their values from. Typically, local-to-buffer
   * options are copied from the global values. Local-to-window options are either initialised from the per-window
   * "global" value or copied directly from the opening window. Global-local options are usually initialised to the
   * option's "unset" marker value.
   *
   * Note that listeners are not notified. This method is called when a new editor is opened, and also very early in the
   * process of enabling IdeaVim (to ensure that options are available for the rest of the process). Depending on when a
   * listener is registered, it might get called or not. To ensure consistency, this method does not call any listeners.
   *
   * @param editor  The editor to initialise
   * @param sourceEditor  The editor which is opening the new editor. This source editor is used to get the per-window
   * "global" values to initialise the new editor. It can only be null when [scenario] is
   * [LocalOptionInitialisationScenario.DEFAULTS].
   * @param scenario  The scenario for initialising the local options
   */
  fun initialiseLocalOptions(editor: VimEditor, sourceEditor: VimEditor?, scenario: LocalOptionInitialisationScenario)

  /**
   * Start tracking when the `~/.ideavimrc` file is being evaluated as part of IdeaVim startup.
   *
   * This is used to track when options are explicitly set as part of IdeaVim initialisation. These options will be used
   * to initialise all subsequently opened windows, so can kind of be considered as "global" (but not in the same way as
   * [OptionDeclaredScope.GLOBAL]). This is useful for externally mapped options. Typically, the external value is local
   * to the editor, and setting the external global value doesn't update the local value. By tracking when the option is
   * initialised during startup, we can reset these external local values when the external global value changes. Any
   * option that is explicitly set by the user is not reset.
   */
  fun startInitVimRc()

  /**
   * Stop tracking when the `~/.ideavimrc` file is being evaluated as part of IdeaVim startup.
   */
  fun endInitVimRc()

  /**
   * Get the [Option] by its name or abbreviation
   */
  fun getOption(key: String): Option<VimDataType>?

  /**
   * @return list of all options
   */
  fun getAllOptions(): Set<Option<VimDataType>>

  /**
   * Get the value for the option in the given scope
   */
  fun <T : VimDataType> getOptionValue(option: Option<T>, scope: OptionAccessScope): T

  /**
   * Set the value for the option in the given scope
   */
  fun <T : VimDataType> setOptionValue(option: Option<T>, scope: OptionAccessScope, value: T)

  /**
   * Resets the option's target scope's value back to its default value
   *
   * This is the equivalent of `:set {option}&`, `:setglobal {option}&` and `:setlocal {option}&`.
   *
   * When called at global scope, it will reset the global value to the option's default value. Similarly for local
   * scope. When called at effective scope for local options, it will reset both the local and global values. For
   * global-local options, the local value is reset to the default value, rather than unset. This matches Vim behaviour.
   */
  fun <T : VimDataType> resetToDefaultValue(option: Option<T>, scope: OptionAccessScope)

  /**
   * Resets the option's target scope's value back to its global value
   *
   * This is the equivalent of `:set {option}<`, `:setglobal {option}<` and `:setlocal {option}<`.
   *
   * For local options, this will copy the global value to the local value. For global options, or called at global
   * scope (`:setglobal {option}<`), this is a no-op, as copying the global value to the global value obviously does
   * nothing. For global-local options called at effective scope, this will also copy the current global value to the
   * local value, but when called at local scope (`:setlocal {option}<`) then number-based options are unset,
   * effectively resetting the local value to the global value. This is the only way to unset global-local toggle
   * options.
   */
  fun <T : VimDataType> resetToGlobalValue(option: Option<T>, scope: OptionAccessScope, editor: VimEditor)

  /**
   * Get or create cached, parsed data for the option value effective for the editor
   *
   * The parsed data is created by the given [provider], based on the effective value of the option in the given
   * [editor] (there is no reason to parse global/local data unless it is the effective value). The parsed data is then
   * cached, and the cache is cleared when the effective option value is changed.
   *
   * It is not expected for this function to be used by general purpose use code, but by helper objects that will parse
   * complex options and provide a user facing API for the data. E.g. for `'guicursor'` and `'iskeyword'` options.
   *
   * @param option  The option to return parsed data for
   * @param editor  The editor to get the option value for. This must be specified for local or global-local options
   * @param provider  If the parsed value does not exist, the effective option value is retrieved and passed to the
   *                  provider. The resulting value is cached.
   * @return The cached, parsed option value, ready to be used by code.
   */
  fun <T : VimDataType, TData : Any> getParsedEffectiveOptionValue(
    option: Option<T>,
    editor: VimEditor?,
    provider: (T) -> TData,
  ): TData

  /**
   * Resets all options for the given editor, including global options, back to default values.
   *
   * In line with `:set all&`, this will reset all option for the given editor. This means resetting global,
   * global-local and local-to-buffer options, which will affect other editors/windows. It resets the local-to-window
   * options for the current editor; this does not affect other editors.
   */
  fun resetAllOptions(editor: VimEditor)

  /**
   * Resets all options across all editors, to reset state for testing
   *
   * This is required to reset global options set for tests that don't create an editor
   */
  @TestOnly
  fun resetAllOptionsForTesting()

  /**
   * Adds the option.
   *
   * Note that this function accepts a covariant version of [Option] so it can accept derived instances that are
   * specialised by a type derived from [VimDataType].
   *
   * This function will initialise the option to default values across all currently open editors, but it does not
   * notify listeners. This mirrors the behaviour of [initialiseLocalOptions].
   *
   * @param option option
   */
  fun addOption(option: Option<out VimDataType>)

  /**
   * Removes the option.
   * @param optionName option name or alias
   */
  fun removeOption(optionName: String)

  /**
   * Add a listener for when a global option value changes
   *
   * This listener will get called once when a global option's value changes. It is intended for non-editor features,
   * such as updating the status bar widget for `'showcmd'` or updating the default register when `'clipboard'` changes.
   * It can only be used for global options, and will not be called when the global value of a local-to-buffer or
   * local-to-window option is changed. It is also not called when a global-local option is changed.
   *
   * @param option  The option to listen to for changes. It must be a [OptionDeclaredScope.GLOBAL] option
   * @param listener  The listener that will be invoked when the global option changes
   */
  fun <T : VimDataType> addGlobalOptionChangeListener(option: Option<T>, listener: GlobalOptionChangeListener)

  /**
   * Remove a global option change listener
   *
   * @param option  The global option that has previously been subscribed to
   * @param listener  The listener to remove
   */
  fun <T : VimDataType> removeGlobalOptionChangeListener(option: Option<T>, listener: GlobalOptionChangeListener)

  /**
   * Add a listener for when the effective value of an option is changed
   *
   * This listener will be called for all editors that are affected by the value change. For global options, this is all
   * open editors. For local-to-buffer options, this is all editors for the buffer, and for local-to-window options,
   * this will be the single editor for the window.
   *
   * Global-local options are slightly more complicated. If the global value is changed, all editors that are using the
   * global value are notified - any editor that has an overriding local value is not notified. If the local value is
   * changed, then all editors for the buffer or window will be notified. When the effective value of a global-local
   * option is changed with `:set`, both the global and local values are updated. In this case, all editors that are
   * unset are notified, as are the editors affected by the local value update (the editors associated with the buffer
   * or window)
   *
   * Note that the listener is not called for global value changes to local options.
   *
   * @param option  The option to listen to for changes
   * @param listener  The listener to call when the effective value chagnse.
   */
  fun <T : VimDataType> addEffectiveOptionValueChangeListener(
    option: Option<T>,
    listener: EffectiveOptionValueChangeListener,
  )

  /**
   * Remove an effective option value change listener
   *
   * @param option  The option that has previously been subscribed to
   * @param listener  The listener to remove
   */
  fun <T : VimDataType> removeEffectiveOptionValueChangeListener(
    option: Option<T>,
    listener: EffectiveOptionValueChangeListener,
  )

  /**
   * Override the original default value of the option with an implementation specific value
   *
   * This is added specifically for `'clipboard'` to support the `ideaput` value in the IntelliJ implementation.
   * This function should be used with care!
   */
  fun <T : VimDataType> overrideDefaultValue(option: Option<T>, newDefaultValue: T)

  /**
   * Return an accessor for options that only have a global value
   */
  fun getGlobalOptions(): GlobalOptions

  /**
   * Return an accessor for the effective value of local options
   */
  fun getEffectiveOptions(editor: VimEditor): EffectiveOptions
}

/**
 * Checks if option is set to its default value
 */
fun <T : VimDataType> VimOptionGroup.isDefaultValue(option: Option<T>, scope: OptionAccessScope): Boolean =
  getOptionValue(option, scope) == option.defaultValue

fun <T : VimDataType> VimOptionGroup.isUnsetValue(option: Option<T>, editor: VimEditor): Boolean {
  check(option.declaredScope.isGlobalLocal())
  return getOptionValue(option, OptionAccessScope.LOCAL(editor)) == option.unsetValue
}

/**
 * Splits a string list option into flags, or returns a list with a single string value
 *
 * E.g. the `fileencodings` option with value "ucs-bom,utf-8,default,latin1" will result listOf("ucs-bom", "utf-8", "default", "latin1")
 */
fun VimOptionGroup.getStringListValues(option: StringListOption, scope: OptionAccessScope): List<String> {
  return option.split(getOptionValue(option, scope).asString())
}

/**
 * Sets the toggle option on
 */
fun VimOptionGroup.setToggleOption(option: ToggleOption, scope: OptionAccessScope) {
  setOptionValue(option, scope, VimInt.ONE)
}

/**
 * Unsets a toggle option
 */
fun VimOptionGroup.unsetToggleOption(option: ToggleOption, scope: OptionAccessScope) {
  setOptionValue(option, scope, VimInt.ZERO)
}

/**
 * Inverts toggle option value, setting it on if off, or off if on.
 */
fun VimOptionGroup.invertToggleOption(option: ToggleOption, scope: OptionAccessScope) {
  val optionValue = getOptionValue(option, scope)
  setOptionValue(option, scope, (!optionValue.booleanValue).asVimInt())
}

/**
 * Checks a string list option to see if it contains a specific value
 */
fun VimOptionGroup.hasValue(option: StringListOption, scope: OptionAccessScope, value: String): Boolean {
  val optionValue = getOptionValue(option, scope)
  return option.split(optionValue.asString()).contains(value)
}

/**
 * The scenario for initialising local options
 */
enum class LocalOptionInitialisationScenario {
  /**
   * Set the local options to default (global) values.
   */
  DEFAULTS,

  /**
   * The new window is being initialised with the values of the fallback window
   *
   * Vim always has at least one buffer and window open, and the `vimrc` files are evaluated in this context. Any
   * options set during evaluation are applied to the first open window and buffer, as if the user had interactively
   * typed them in. IdeaVim does not always have an open window (and therefore buffer), so we evaluate `~/.ideavimrc` in
   * a special, hidden "fallback" window, that is always available even if there are no editor windows. This fallback
   * window is used to initialise the first editor window.
   *
   * Since Vim will evaluate `vimrc` in the context of the first window, any local-to-buffer options are set against the
   * first window's buffer. Therefore, this scenario will copy buffer and window local values, including global-local
   * values, and the per-window "global" values of local-to-window options.
   */
  FALLBACK,

  /**
   * The new window is a split of the opening/current window
   *
   * In this scenario, Vim is trying to make the new window behave exactly like the opening window, so will copy both
   * local and per-window "global" values of local-to-window and global-local (to window) options from the opening
   * window to the new window. Local-to-buffer windows are obviously already initialised and not modified.
   */
  SPLIT,

  /**
   * The user has opened a new buffer in the current window
   *
   * This is the `:edit {file}` command, where the current window is reused to edit a new or previously edited buffer.
   * Vim will reset any explicitly set local-to-window values. The local-to-buffer options are initialised for a new
   * buffer, by copying from the global values. Local-to-window values are reset to the existing per-window "global"
   * values.
   *
   * Note that IdeaVim currently implements `:edit {file}` to behave like `:new {file}`, and therefore uses the [NEW]
   * scenario when opening a file with `:edit`. However, it does use the [EDIT] scenario with preview tabs, or when an
   * unmodified tab is reused.
   */
  EDIT,

  /**
   * The user has opened a new window
   *
   * This is Vim's `:new {file}` command, which will open a new or existing buffer in a new window. Vim treats this as
   * a split followed by `:edit`, which means copying local and per-window "global" local-to-window option values from
   * the opening window and then resetting any explicitly set local-to-window options to the per-window "global" values.
   *
   * Note that this scenario is used for IdeaVim's current implementation of the `:edit {file}` command.
   */
  NEW,

  /**
   * Initialise the [VimEditor] used for the `ex` command line text field
   *
   * Vim doesn't really have the concept of "editor". It has a window, which is a view on a buffer, and which can edit
   * the text of the buffer. The `ex` command line and search text entry are implemented as part of this window, and
   * therefore automatically uses the window's local options (e.g. search requires `'iskeyword'`)
   *
   * For IdeaVim, the `ex`/search text entry is a separate UI component to the main editor and implements [VimEditor].
   * As such, it needs its own copy of the local options. This scenario makes a full copy of the local to buffer and
   * local to window options, so has the same effect as [FALLBACK].
   *
   * We need to migrate more of the command line text handling to work with a [VimEditor]-based implementation (it's
   * currently very heavily based on Swing). As part of the implementation detail, we could look at sharing options
   * instead of copying them.
   */
  CMD_LINE
}
