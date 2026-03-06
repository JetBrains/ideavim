/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.jump

import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl.PlaceInfo
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl.RecentPlacesListener
import com.intellij.openapi.project.Project
import com.intellij.platform.rpc.topics.broadcast
import com.maddyhome.idea.vim.group.findEditorByFilePath

/**
 * Listens to IntelliJ's [RecentPlacesListener] to capture IDE navigation events
 * and broadcast them to the frontend via [JUMP_REMOTE_TOPIC].
 *
 * The frontend [JumpRemoteTopicListener] receives these events and adds/removes
 * jumps from the active [com.maddyhome.idea.vim.api.VimJumpService].
 *
 * This listener never reads options — the `unifyjumps` check is performed
 * on the frontend side.
 */
internal class JumpsListener(val project: Project) : RecentPlacesListener {

  @Suppress("OVERRIDE_DEPRECATION")
  override fun recentPlaceAdded(changePlace: PlaceInfo, isChanged: Boolean) {
    if (!isChanged) {
      val jumpInfo = buildJumpInfo(changePlace, added = true) ?: return
      JUMP_REMOTE_TOPIC.broadcast(project, jumpInfo)
    }
  }

  @Suppress("OVERRIDE_DEPRECATION")
  override fun recentPlaceRemoved(changePlace: PlaceInfo, isChanged: Boolean) {
    if (!isChanged) {
      val jumpInfo = buildJumpInfo(changePlace, added = false) ?: return
      JUMP_REMOTE_TOPIC.broadcast(project, jumpInfo)
    }
  }

  private fun buildJumpInfo(place: PlaceInfo, added: Boolean): JumpInfo? {
    val editor = findEditorByFilePath(project, place.file.path) ?: return null
    val offset = place.caretPosition?.startOffset ?: return null

    val bufferPosition = editor.offsetToLogicalPosition(offset)
    val line = bufferPosition.line
    val col = bufferPosition.column

    val path = place.file.path

    return JumpInfo(line, col, path, place.file.fileSystem.protocol, added, place.timeStamp)
  }
}
