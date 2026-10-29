/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.ex.ActionUtil

// Rider and remote development frontend classes are not available at compile time, so they are recognized by name

/**
 * True for frontend actions that only delegate to the backend (the ReSharper backend in Rider), e.g. `ReformatCode` or
 * `MoveElementRight` for C#. Only the backend knows whether such an action is enabled.
 */
internal fun AnAction.isBackendAction(): Boolean =
  ActionUtil.getDelegateChainRootAction(this).javaClass.implementsInterface("com.jetbrains.rdclient.actions.base.BackendActionTrait")

/**
 * True for an action that might send a request to the backend, whose changes arrive later
 */
internal fun AnAction.isPossiblyDelegatingToBackend(): Boolean =
  javaClass.implementsInterface("com.jetbrains.rd.ui.actions.PossiblyDelegatingToBackendAction") ||
    isRiderBackendCompositeAction()

/**
 * For a hotkey, Rider replaces its backend actions with a single composite action that asks the backend which of them
 * to run, and doesn't wait for the result
 */
private fun AnAction.isRiderBackendCompositeAction(): Boolean =
  javaClass.name == "com.jetbrains.rider.actions.RiderActionUpdateInterceptor\$RiderBackendCompositeAction"

internal fun Class<*>.implementsInterface(name: String): Boolean =
  generateSequence(this) { it.superclass }.any { cls ->
    cls.interfaces.any { it.name == name || it.implementsInterface(name) }
  }
