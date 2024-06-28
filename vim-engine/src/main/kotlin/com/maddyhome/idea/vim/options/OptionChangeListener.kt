/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

import com.maddyhome.idea.vim.api.VimEditor

/**
 * Listener for changes to the value of a global option
 *
 * This listener will only be called when a global option's value is changed. It is intended for non-editor related
 * options. That is, options that don't need to refresh editor(s) when the global value changes. For example, updating
 * the status bar widget when `'showcmd'` changes, or updating the default register when `'clipboard'` changes, or
 * enabling/disabling extensions.
 *
 * This function will always be called before [EffectiveOptionValueChangeListener.onEffectiveValueChanged].
 */
fun interface GlobalOptionChangeListener {
  fun onGlobalOptionChanged()
}

/**
 * Listener for changes to the effective value of an option
 *
 * The effective value of an option is the value that is in effect for a given editor. When an option changes, this
 * interface function will be called for all affected options. For global options, all open editors will be notified
 * for all changes in all scopes. For local-to-buffer options, all editors for the buffer are notified when the local
 * or [OptionAccessScope.EFFECTIVE] scope changes. For local-to-window options, the editor specified by [OptionAccessScope.LOCAL] or
 * [OptionAccessScope.EFFECTIVE] will be notified. There will be no notifications to changes to the global scope of local
 * options. If a global-local option's local value is changed, the affected buffer or window editor(s) will be notified.
 * If its global value is changed, all editors where the value is unset will be notified (this includes the current
 * buffer or window's editor(s)).
 */
fun interface EffectiveOptionValueChangeListener {
  fun onEffectiveValueChanged(editor: VimEditor)
}
