/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

import com.maddyhome.idea.vim.options.OptionDeclaredScope.LOCAL_TO_WINDOW


/**
 * Represents the declared scope for an option, e.g. global, global-local or local to window or buffer.
 *
 * Vim has a lot of options. Some are global, with a single value shared across all buffers and windows. Others are
 * local to a buffer, and shared across all windows for that buffer, or local to a single window. There are also
 * global-local options that are usually global, but can be overridden and made local to a buffer or window.
 *
 * The `:set`, `:setglobal` and `:setlocal` commands set or show an option's value at a specific scope - explicitly
 * global, explicitly local, or the "effective" value, which depends on the option's declared scope. The declared scope
 * should not be confused with the accessor scope (see [OptionAccessScope]).
 *
 * Vim tries to do what the user expects when editing a new buffer or opening a new window, with the effect roughly
 * being that the new buffer or window looks and acts like the current window, ignoring locally set options. To avoid
 * copying these locally set options, Vim will maintain two copies of local options, the effective local value, and a
 * "global" value. The `:set` command will set both values, but they can also be set explicitly with `:setglobal` and
 * `:setlocal`. These "global" values are used to initialise the new window or buffer.
 *
 * Local-to-buffer options maintain a global value that is shared across all buffers and windows, so a new window will
 * always use the last set value. Local-to-window options (typically related to appearance) have a per-window "global"
 * value, so a new window will look like the opening window, not the last set value. See [LOCAL_TO_WINDOW] for more
 * details on how the per-window "global" values are updated and used.
 *
 * Note that Vim always has at least one window, so opening a new window always has a value to copy from. IntelliJ, as
 * an IDE, can close all windows, so there is a scenario where a new window needs to be opened without a window to copy
 * options from. Also, Vim always has an opening window, such as `:e {file}` from the ex command line. This isn't true
 * for an IDE, which might open a window or buffer from a Project view, a search palette, ctrl+click on an element, etc.
 */
enum class OptionDeclaredScope {
  /**
   * Option is global and applies to all buffers and windows
   *
   * `:set`, `:setglobal` and `:setlocal` all have the same result of setting the global value.
   */
  GLOBAL,

  /**
   * Each Vim buffer has a copy of this option, which is shared across all windows for the buffer.
   *
   * The local value is the effective value of this option, and used to affect appearance and behaviour in the editor.
   * Setting the option with `:set` will set both the global and local values, while `:setlocal` and `:setglobal` will
   * set just the global or local value.
   *
   * When editing a new buffer, the local values of its local-to-buffer options are initialised to the current global
   * values. This means it will use the most recent, non-explicitly locally set option value.
   *
   * `'shiftwidth'` and `'matchpairs'` are examples of "local to buffer" options.
   *
   * See `:help local-option` and `:help option-summary`.
   */
  LOCAL_TO_BUFFER,

  /**
   * This option is local to a single Vim window
   *
   * As with [LOCAL_TO_BUFFER], the local value is the effective value used by the window. The `:set` command sets both
   * the local and "global" value, while `:setlocal` and `:setglobal` will set the local and "global" values explicitly.
   *
   * The "global" value isn't global, but is a per-window copy. This is so that when initialising a new window's options
   * the current window's "global" value is used, and the new window looks like the current window, rather than looking
   * like whichever window set the value last.
   *
   * When splitting a window, the new window is treated as a duplicate of the current window, and both the local and
   * "global" values are copied to the new window. When editing a new buffer in the current window, the current window's
   * "global" values are copied to the new window, overwriting previously set local values. If the buffer has been
   * edited before, Vim maintains a copy of the previously used local-to-window options, and reapplies them as local
   * values, without changing the "global" values. Finally, if editing a buffer in a new window (i.e. `:new {file}`),
   * Vim first splits the window (copying the "global" and local values) and then edits the buffer (reapplying any saved
   * values, or copying the new current window's "global" values over the local values).
   *
   * This means a couple of things:
   * * A local-to-window option with an explicitly local value will be copied to the new window if the window is split.
   *   It will be reset to the "global" value if editing a new buffer, or editing a new buffer in a new window. It will
   *   be reset to a previously saved value if editing a previously edited buffer.
   * * It is possible to get two windows with different "global" values
   * * Any newly created window will look and behave like the window that opened it, or like the window it was last
   *   opened in.
   *
   * `'list'` and `'number'` are examples of "local to window" options.
   *
   * See `:help local-option` and `:help option-summary`.
   */
  LOCAL_TO_WINDOW,

  /**
   * This option is global but can be optionally overridden for all windows for a buffer
   *
   * Like [LOCAL_TO_BUFFER], except the local value is not initialised when editing a new buffer. It must be explicitly
   * set locally with `:setlocal`. Using `:set` will only set the global value, just like `:setglobal`.
   *
   * The local value is initialised to a sentinel value to show it's unset, such as empty string or `-1`. Boolean values
   * are also set to `-1`, as they are internally stored as integers. If `-1` is a valid value for an option, then a
   * specific marker value is used instead (e.g. `'undolevels'` uses `-123456`).
   *
   * The global value is used to initialise new buffers or windows.
   *
   * `'undolevels'` is an example of a "global or local to buffer" option.
   *
   * See `:help global-local`.
   */
  GLOBAL_OR_LOCAL_TO_BUFFER,

  /**
   * This option is global but can be optionally overridden for a single window
   *
   * Like [LOCAL_TO_WINDOW], except the local value is not initialised when creating a new window. It must be explicitly
   * set locally with `:setlocal`. Using `:set` will only set the global value, like with `:setglobal`.
   *
   * The local value is initialised to a sentinel value to show it's unset, such as empty string or `-1`. Boolean values
   * are also set to `-1`, as they are internally stored as integers. If `-1` is a valid value for an option, then a
   * specific marker value is used instead.
   *
   * The global value is used to initialise new buffers or windows.
   *
   * `'scrolloff'` is an example of a "global or local to window" option.
   *
   * See `:help global-local`
   */
  GLOBAL_OR_LOCAL_TO_WINDOW;

  fun isGlobalLocal(): Boolean =
    this == GLOBAL_OR_LOCAL_TO_BUFFER || this == GLOBAL_OR_LOCAL_TO_WINDOW
}
