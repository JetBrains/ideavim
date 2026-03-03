/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.jump

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.text.StringUtil
import com.maddyhome.idea.vim.api.VimJumpServiceBase
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.mark.Jump
import com.maddyhome.idea.vim.newapi.initInjector
import org.jdom.Element

/**
 * Backend-only storage for jumps collected by [JumpsListener].
 *
 * This is **not** a [VimJumpService] — it is a standalone application service registered
 * only in `ideavim-backend.xml`. It extends [VimJumpServiceBase] for its in-memory jump
 * list management and adds [PersistentStateComponent] for disk persistence.
 *
 * In **split mode**, [JumpsListener] writes IDE navigation events here, and the frontend
 * fetches them via [JumpRemoteApi.getListenerJumps] → [JumpRemoteApiImpl].
 *
 * In **monolith mode**, [JumpsListener] adds jumps directly to [VimJumpServiceImpl]
 * (via `injector.jumpService`) and does NOT use this storage. This service is still
 * instantiated but unused in monolith mode.
 */
@State(
  name = "VimBackendJumpsSettings",
  storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)]
)
internal class BackendJumpStorage : VimJumpServiceBase(), PersistentStateComponent<Element?> {
  companion object {
    private val logger = vimLogger<BackendJumpStorage>()
  }

  override var lastJumpTimeStamp: Long = 0

  override fun includeCurrentCommandAsNavigation(editor: com.maddyhome.idea.vim.api.VimEditor) {
    // Not used — this is storage only, not a full VimJumpService.
  }

  override fun getState(): Element {
    val projectsElem = Element("projects")
    for ((project, jumps) in projectToJumps) {
      val projectElement = Element("project").setAttribute("id", project)
      for (jump in jumps) {
        val jumpElem = Element("jump")
        jumpElem.setAttribute("line", jump.line.toString())
        jumpElem.setAttribute("column", jump.col.toString())
        jumpElem.setAttribute("filename", StringUtil.notNullize(jump.filepath))
        jumpElem.setAttribute("protocol", StringUtil.notNullize(jump.protocol))
        projectElement.addContent(jumpElem)
        if (logger.isDebug()) {
          logger.debug("saved jump = $jump")
        }
      }
      projectsElem.addContent(projectElement)
    }
    return projectsElem
  }

  override fun loadState(state: Element) {
    initInjector()
    val projectElements = state.getChildren("project")
    for (projectElement in projectElements) {
      val jumps = mutableListOf<Jump>()
      val jumpElements = projectElement.getChildren("jump")
      for (jumpElement in jumpElements) {
        val jump = Jump(
          Integer.parseInt(jumpElement.getAttributeValue("line")),
          Integer.parseInt(jumpElement.getAttributeValue("column")),
          jumpElement.getAttributeValue("filename"),
          jumpElement.getAttributeValue("protocol", "file"),
        )
        jumps.add(jump)
      }
      if (logger.isDebug()) {
        logger.debug("jumps=$jumps")
      }
      val projectId = projectElement.getAttributeValue("id")
      projectToJumps[projectId] = jumps
    }
  }
}
