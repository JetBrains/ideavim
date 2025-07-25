/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Mode
import com.intellij.vim.api.Range

/**
 * Scope that provides access to various listeners.
 */
@VimPluginDsl
interface ListenersScope {
  /**
   * Registers a callback that is invoked when the editor mode changes.
   *
   * The callback receives the previous mode as a parameter.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onModeChange { oldMode ->
   *     if (mode == Mode.INSERT) {
   *       // Do something when entering INSERT mode
   *     }
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when the mode changes
   */
  fun onModeChange(callback: suspend VimScope.(Mode) -> Unit)

  /**
   * Registers a callback that is invoked when text is yanked.
   *
   * The callback receives a map of caret IDs to the yanked text ranges.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onYank { caretRangeMap ->
   *     // Process yanked text ranges
   *     caretRangeMap.forEach { (caretId, range) ->
   *       // Highlight or process the yanked range
   *     }
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when text is yanked
   */
  fun onYank(callback: suspend VimScope.(Map<CaretId, Range.Simple>) -> Unit)

  /**
   * Registers a callback that is invoked when a new editor is created.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onEditorCreate {
   *     // Initialize resources for the new editor
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when an editor is created
   */
  fun onEditorCreate(callback: suspend VimScope.() -> Unit)

  /**
   * Registers a callback that is invoked when an editor is released.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onEditorRelease {
   *     // Clean up resources associated with the editor
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when an editor is released
   */
  fun onEditorRelease(callback: suspend VimScope.() -> Unit)

  /**
   * Registers a callback that is invoked when an editor gains focus.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onEditorFocusGain {
   *     // Perform actions when editor gains focus
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when an editor gains focus
   */
  fun onEditorFocusGain(callback: suspend VimScope.() -> Unit)

  /**
   * Registers a callback that is invoked when an editor loses focus.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onEditorFocusLost {
   *     // Perform actions when editor loses focus
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when an editor loses focus
   */
  fun onEditorFocusLost(callback: suspend VimScope.() -> Unit)

  /**
   * Registers a callback that is invoked when macro recording starts.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onMacroRecordingStart {
   *     // Perform actions when macro recording begins
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when macro recording starts
   */
  fun onMacroRecordingStart(callback: suspend VimScope.() -> Unit)

  /**
   * Registers a callback that is invoked when macro recording finishes.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onMacroRecordingFinish {
   *     // Perform actions when macro recording ends
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when macro recording finishes
   */
  fun onMacroRecordingFinish(callback: suspend VimScope.() -> Unit)

  /**
   * Registers a callback that is invoked when IdeaVim is enabled.
   *
   * Example usage:
   * ```kotlin
   * listeners {
   *   onIdeaVimEnabled {
   *     // Initialize plugin resources when IdeaVim is enabled
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when IdeaVim is enabled
   */
  fun onIdeaVimEnabled(callback: suspend VimScope.() -> Unit)

  /**
   * Registers a callback that is invoked when IdeaVim is disabled.
   *
   * Example usage:
   * ```kotlin
   * listeners {
   *   onIdeaVimDisabled {
   *     // Clean up plugin resources when IdeaVim is disabled
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when IdeaVim is disabled
   */
  fun onIdeaVimDisabled(callback: suspend VimScope.() -> Unit)
}
