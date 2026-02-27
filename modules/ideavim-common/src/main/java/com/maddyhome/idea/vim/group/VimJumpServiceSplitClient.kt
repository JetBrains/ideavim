/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimJumpServiceBase
import com.maddyhome.idea.vim.newapi.IjVimEditor
import kotlinx.coroutines.runBlocking

/**
 * Thin-client jump service for split (Remote Development) mode.
 *
 * Extends [VimJumpServiceBase] which provides all pure in-memory jump list management
 * (addJump, getJump, removeJump, etc.). Only [includeCurrentCommandAsNavigation] needs
 * the backend — it calls [IdeDocumentHistory] which is a backend-only API.
 *
 * In monolith mode this service is overridden by [VimJumpServiceImpl] (registered
 * with `overrides="true"` in ideavim-backend.xml).
 */
internal class VimJumpServiceSplitClient : VimJumpServiceBase() {
  override var lastJumpTimeStamp: Long = 0

  override fun includeCurrentCommandAsNavigation(editor: VimEditor) {
    val project = (editor as IjVimEditor).editor.project ?: return
    val coroutineScope = ApplicationManager.getApplication().service<CoroutineScopeProvider>().coroutineScope
    runBlocking(coroutineScope.coroutineContext) {
      JumpRemoteApi.getInstance().includeCurrentCommandAsNavigation(project.basePath)
    }
  }
}
