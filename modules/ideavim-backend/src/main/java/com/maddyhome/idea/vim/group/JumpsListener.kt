/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl.PlaceInfo
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl.RecentPlacesListener
import com.intellij.openapi.project.Project
import com.maddyhome.idea.vim.api.VimJumpService
import com.maddyhome.idea.vim.api.VimJumpServiceBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.mark.Jump
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.initInjector

/**
 * Listens to IntelliJ's [RecentPlacesListener] to sync IDE navigation events
 * into IdeaVim's jump list.
 *
 * In **monolith mode**, [injector.jumpService] resolves to [VimJumpServiceImpl] (frontend),
 * so jumps go directly into the main jump list.
 *
 * In **split-backend mode**, [VimJumpService] is not registered (it lives in the frontend),
 * so this listener falls back to [BackendJumpStorage]. The frontend fetches those jumps
 * via [JumpRemoteApi.getListenerJumps].
 */
internal class JumpsListener(val project: Project) : RecentPlacesListener {

  /**
   * Returns the jump storage to use.
   *
   * In monolith mode, [VimJumpService] is registered (as [VimJumpServiceImpl]) and
   * [injector.jumpService] returns it. In split-backend mode, no [VimJumpService] is
   * registered, so we fall back to [BackendJumpStorage].
   */
  private fun resolveJumpStorage(): VimJumpServiceBase {
    return try {
      injector.jumpService as VimJumpServiceBase
    } catch (_: Exception) {
      service<BackendJumpStorage>()
    }
  }

  override fun recentPlaceAdded(changePlace: PlaceInfo, isChanged: Boolean) {
    initInjector()
    if (!injector.globalIjOptions().unifyjumps) return

    val jumpStorage = resolveJumpStorage()
    if (!isChanged) {
      if (changePlace.timeStamp < jumpStorage.lastJumpTimeStamp) return // this listener is notified asynchronously, and
      // we do not want jumps that were processed before
      val jump = buildJump(changePlace) ?: return
      jumpStorage.addJump(injector.file.getProjectId(project), jump, true)
    }
  }

  override fun recentPlaceRemoved(changePlace: PlaceInfo, isChanged: Boolean) {
    initInjector()
    if (!injector.globalIjOptions().unifyjumps) return

    val jumpStorage = resolveJumpStorage()
    if (!isChanged) {
      if (changePlace.timeStamp < jumpStorage.lastJumpTimeStamp) return // this listener is notified asynchronously, and
      // we do not want jumps that were processed before
      val jump = buildJump(changePlace) ?: return
      jumpStorage.removeJump(injector.file.getProjectId(project), jump)
    }
  }

  private fun buildJump(place: PlaceInfo): Jump? {
    val project = findProjectById(injector.file.getProjectId(project)) ?: return null
    val editor =
      findEditorByFilePath(project, place.file.path) ?: return null
    val offset = place.caretPosition?.startOffset ?: return null

    val bufferPosition = editor.offsetToLogicalPosition(offset)
    val line = bufferPosition.line
    val col = bufferPosition.column

    val path = place.file.path

    return Jump(line, col, path, place.file.fileSystem.protocol)
  }
}
