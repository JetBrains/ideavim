/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope

/**
 * Application-level service that provides a platform-managed [CoroutineScope].
 *
 * The IntelliJ Platform injects a properly configured [CoroutineScope] into the
 * constructor. This scope is tied to the application lifecycle and is cancelled
 * when the application shuts down.
 *
 * Used by split-mode (Remote Development) RPC clients to obtain a coroutine context
 * for `runBlocking` calls, replacing the previous `KernelService.kernelCoroutineScope` approach.
 */
@Service(Service.Level.APP)
class CoroutineScopeProvider(val coroutineScope: CoroutineScope)
