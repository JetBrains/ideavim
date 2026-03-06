/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.project.Project
import com.intellij.platform.rpc.topics.ProjectRemoteTopic
import com.intellij.platform.rpc.topics.ProjectRemoteTopicListener
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.jump.JUMP_REMOTE_TOPIC
import com.maddyhome.idea.vim.group.jump.JumpInfo
import com.maddyhome.idea.vim.mark.Jump
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.initInjector

/**
 * Receives jump events from the backend [JumpsListener] via [JUMP_REMOTE_TOPIC]
 * and applies them to the active [com.maddyhome.idea.vim.api.VimJumpService].
 *
 * Checks the `unifyjumps` option and the event timestamp before recording —
 * this mirrors the checks that were in `JumpsListener` on master.
 * The timestamp check prevents processing events that were already handled
 * directly by [com.maddyhome.idea.vim.api.VimJumpServiceBase.saveJumpLocation].
 */
internal class JumpRemoteTopicListener : ProjectRemoteTopicListener<JumpInfo> {
  override val topic: ProjectRemoteTopic<JumpInfo> = JUMP_REMOTE_TOPIC

  override fun handleEvent(project: Project, event: JumpInfo) {
    initInjector()
    if (!injector.globalIjOptions().unifyjumps) return

    val jumpService = injector.jumpService
    if (event.timestamp < jumpService.lastJumpTimeStamp) return

    val projectId = injector.file.getProjectId(project)
    val jump = Jump(event.line, event.col, event.filepath, event.protocol)

    if (event.added) {
      jumpService.addJump(projectId, jump, true)
    } else {
      jumpService.removeJump(projectId, jump)
    }
  }
}
