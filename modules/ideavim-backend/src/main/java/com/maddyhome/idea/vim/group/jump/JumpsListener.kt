/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.jump

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl.PlaceInfo
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl.RecentPlacesListener
import com.intellij.openapi.project.Project
import com.intellij.platform.project.projectId
import com.maddyhome.idea.vim.group.findEditorByFilePath
import com.maddyhome.idea.vim.mark.Jump

/**
 * Listens to IntelliJ's [RecentPlacesListener] to sync IDE navigation events
 * into IdeaVim's jump list via [BackendJumpStorage].
 *
 * The frontend fetches stored jumps via [JumpRemoteApi.getListenerJumps]
 * and checks `unifyjumps` before merging.
 *
 * This listener never reads options — the `unifyjumps` check is performed
 * on the frontend side.
 */
internal class JumpsListener(val project: Project) : RecentPlacesListener {

  private val storageKey: String by lazy { project.projectId().serializeToString() }

  private fun resolveJumpStorage(): BackendJumpStorage {
    return service<BackendJumpStorage>()
  }

  override fun recentPlaceAdded(changePlace: PlaceInfo, isChanged: Boolean) {
    val jumpStorage = resolveJumpStorage()
    if (!isChanged) {
      if (changePlace.timeStamp < jumpStorage.lastJumpTimeStamp) return // this listener is notified asynchronously, and
      // we do not want jumps that were processed before
      val jump = buildJump(changePlace) ?: return
      jumpStorage.addJump(storageKey, jump, true)
    }
  }

  override fun recentPlaceRemoved(changePlace: PlaceInfo, isChanged: Boolean) {
    val jumpStorage = resolveJumpStorage()
    if (!isChanged) {
      if (changePlace.timeStamp < jumpStorage.lastJumpTimeStamp) return // this listener is notified asynchronously, and
      // we do not want jumps that were processed before
      val jump = buildJump(changePlace) ?: return
      jumpStorage.removeJump(storageKey, jump)
    }
  }

  private fun buildJump(place: PlaceInfo): Jump? {
    val editor = findEditorByFilePath(project, place.file.path) ?: return null
    val offset = place.caretPosition?.startOffset ?: return null

    val bufferPosition = editor.offsetToLogicalPosition(offset)
    val line = bufferPosition.line
    val col = bufferPosition.column

    val path = place.file.path

    return Jump(line, col, path, place.file.fileSystem.protocol)
  }
}
