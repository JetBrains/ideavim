/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * Handles plugin activation/deactivation lifecycle.
 *
 * This is the frontend-facing part of the plugin lifecycle. It manages things like:
 * - Registering/unregistering Vim actions
 * - Setting up/tearing down editor listeners
 * - Executing ~/.ideavimrc
 * - Managing search highlights
 * - Updating status bar icons
 *
 * The implementation lives in ideavim-common but is designed to be movable to ideavim-frontend
 * once VimPlugin.java no longer directly depends on it.
 */
interface VimPluginActivator {
  /**
   * Activates the IdeaVim plugin. Called when the plugin is first initialized or re-enabled.
   * Sets up action registrations, extension registrations, option initialisation,
   * ideavimrc execution, search highlights, and editor listeners.
   */
  fun activate()

  /**
   * Deactivates the IdeaVim plugin. Called when the plugin is disabled or disposed.
   *
   * @param unsubscribe whether to unsubscribe editor listeners (false during dispose)
   */
  fun deactivate(unsubscribe: Boolean)

  /**
   * Updates the status bar icon to reflect the current plugin state.
   */
  fun updateStatusBarIcon()
}
