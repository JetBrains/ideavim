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
import org.jetbrains.annotations.TestOnly

public interface VimOptionGroup {
  /**
   * Called to initialise the options
   *
   * This function must be idempotent, as it is called each time the plugin is enabled.
   */
  public fun initialiseOptions()

  /**
   * Initialise the local to buffer and local to window options for this editor
   *
   * Depending on the initialisation scenario, the local-to-buffer, local-to-window and/or global-local options are
   * initialised. The scenario dictates where the local options get their values from. Typically, local-to-buffer
   * options are copied from the global values. Local-to-window options are either initialised from the per-window
   * "global" value or copied directly from the opening window. Global-local options are usually initialised to the
   * option's "unset" marker value.
   *
   * @param editor  The editor to initialise
   * @param sourceEditor  The editor which is opening the new editor. This source editor is used to get the per-window
   * "global" values to initialise the new editor. It can only be null when [scenario] is
   * [LocalOptionInitialisationScenario.DEFAULTS].
   * @param scenario  The scenario for initialising the local options
   */
  public fun initialiseLocalOptions(editor: VimEditor, sourceEditor: VimEditor?, scenario: LocalOptionInitialisationScenario)

  /**
   * Get the [Option] by its name or abbreviation
   */
  public fun getOption(key: String): Option<VimDataType>?

  /**
   * @return list of all options
   */
  public fun getAllOptions(): Set<Option<VimDataType>>

  /**
   * Get the value for the option in the given scope
   */
  public fun <T : VimDataType> getOptionValue(option: Option<T>, scope: OptionAccessScope): T

  /**
   * Set the value for the option in the given scope
   */
  public fun <T : VimDataType> setOptionValue(option: Option<T>, scope: OptionAccessScope, value: T)

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
  public fun <T : VimDataType, TData : Any> getParsedEffectiveOptionValue(
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
  public fun resetAllOptions(editor: VimEditor)

  /**
   * Resets all options across all editors, to reset state for testing
   *
   * This is required to reset global options set for tests that don't create an editor
   */
  @TestOnly
  public fun resetAllOptionsForTesting()

  /**
   * Adds the option.
   *
   * Note that this function accepts a covariant version of [Option] so it can accept derived instances that are
   * specialised by a type derived from [VimDataType].
   *
   * @param option option
   */
  public fun addOption(option: Option<out VimDataType>)

  /**
   * Removes the option.
   * @param optionName option name or alias
   */
  public fun removeOption(optionName: String)

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
  public fun <T : VimDataType> addGlobalOptionChangeListener(option: Option<T>, listener: GlobalOptionChangeListener)

  /**
   * Remove a global option change listener
   *
   * @param option  The global option that has previously been subscribed to
   * @param listener  The listener to remove
   */
  public fun <T : VimDataType> removeGlobalOptionChangeListener(option: Option<T>, listener: GlobalOptionChangeListener)

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
  public fun <T : VimDataType> addEffectiveOptionValueChangeListener(
    option: Option<T>,
    listener: EffectiveOptionValueChangeListener,
  )

  /**
   * Remove an effective option value change listener
   *
   * @param option  The option that has previously been subscribed to
   * @param listener  The listener to remove
   */
  public fun <T : VimDataType> removeEffectiveOptionValueChangeListener(
    option: Option<T>,
    listener: EffectiveOptionValueChangeListener
  )

  /**
   * Override the original default value of the option with an implementation specific value
   *
   * This is added specifically for `'clipboard'` to support the `ideaput` value in the IntelliJ implementation.
   * This function should be used with care!
   */
  public fun <T : VimDataType> overrideDefaultValue(option: Option<T>, newDefaultValue: T)

  /**
   * Return an accessor for options that only have a global value
   */
  public fun getGlobalOptions(): GlobalOptions

  /**
   * Return an accessor for the effective value of local options
   */
  public fun getEffectiveOptions(editor: VimEditor): EffectiveOptions
}

/**
 * Checks if option is set to its default value
 */
public fun <T : VimDataType> VimOptionGroup.isDefaultValue(option: Option<T>, scope: OptionAccessScope): Boolean =
  getOptionValue(option, scope) == option.defaultValue

/**
 * Resets the option back to its default value
 *
 * Resetting a global-local value at local scope will set it to the default value, rather than set it to its unset
 * value. This matches Vim behaviour.
 */
public fun <T : VimDataType> VimOptionGroup.resetDefaultValue(option: Option<T>, scope: OptionAccessScope) {
  setOptionValue(option, scope, option.defaultValue)
}

/**
 *
 */
public fun <T : VimDataType> VimOptionGroup.isUnsetValue(option: Option<T>, editor: VimEditor): Boolean {
  check(option.declaredScope == OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER
    || option.declaredScope == OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW)
  return getOptionValue(option, OptionAccessScope.LOCAL(editor)) == option.unsetValue
}

/**
 * Splits a string list option into flags, or returns a list with a single string value
 *
 * E.g. the `fileencodings` option with value "ucs-bom,utf-8,default,latin1" will result listOf("ucs-bom", "utf-8", "default", "latin1")
 */
public fun VimOptionGroup.getStringListValues(option: StringListOption, scope: OptionAccessScope): List<String> {
  return option.split(getOptionValue(option, scope).asString())
}

/**
 * Sets the toggle option on
 */
public fun VimOptionGroup.setToggleOption(option: ToggleOption, scope: OptionAccessScope) {
  setOptionValue(option, scope, VimInt.ONE)
}

/**
 * Unsets a toggle option
 */
public fun VimOptionGroup.unsetToggleOption(option: ToggleOption, scope: OptionAccessScope) {
  setOptionValue(option, scope, VimInt.ZERO)
}

/**
 * Inverts toggle option value, setting it on if off, or off if on.
 */
public fun VimOptionGroup.invertToggleOption(option: ToggleOption, scope: OptionAccessScope) {
  val optionValue = getOptionValue(option, scope)
  setOptionValue(option, scope, if (optionValue.asBoolean()) VimInt.ZERO else VimInt.ONE)
}

/**
 * Checks a string list option to see if it contains a specific value
 */
public fun VimOptionGroup.hasValue(option: StringListOption, scope: OptionAccessScope, value: String): Boolean {
  val optionValue = getOptionValue(option, scope)
  return option.split(optionValue.asString()).contains(value)
}

/**
 * The scenario for initialising local options
 */
public enum class LocalOptionInitialisationScenario {
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
