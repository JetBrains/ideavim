/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.changelist

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.project.projectId
import com.intellij.platform.rpc.topics.ProjectRemoteTopic
import com.intellij.platform.rpc.topics.ProjectRemoteTopicListener

internal class ChangeListRemoteTopicListener : ProjectRemoteTopicListener<ChangeListInfo> {
  override val topic: ProjectRemoteTopic<ChangeListInfo> = CHANGE_LIST_REMOTE_TOPIC

  override fun handleEvent(project: Project, event: ChangeListInfo) {
    service<ChangeListService>().addChange(
      project.projectId().serializeToString(),
      ChangeListService.Change(event.line, event.col, event.filepath, event.protocol),
    )
  }
}
