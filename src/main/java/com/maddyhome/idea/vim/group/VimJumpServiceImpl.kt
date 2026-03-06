/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.util.text.StringUtil
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimJumpServiceBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.mark.Jump
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.initInjector
import org.jdom.Element

/**
 * Full jump service with persistence and [IdeDocumentHistory] integration.
 *
 * Registered in `ideavim-frontend.xml` with `overrides="true"`, so in **monolith mode**
 * it overrides the common base [VimJumpServiceCommonImpl] and is the active [VimJumpService].
 * In **split mode** (thin client), it is further overridden by [VimJumpServiceSplitClient]
 * from `ideavim-frontend-split.xml`.
 *
 * Handles state serialization to `vim_settings_local.xml` and provides
 * [includeCurrentCommandAsNavigation] which integrates with IntelliJ's
 * Recent Places navigation.
 */
@State(
  name = "VimJumpsSettings",
  storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)]
)
internal class VimJumpServiceImpl : VimJumpServiceBase(), PersistentStateComponent<Element?> {
  companion object {
    private val logger = vimLogger<VimJumpServiceImpl>()
  }

  override var lastJumpTimeStamp: Long = 0

  /**
   * Checks `unifyjumps` before recording a jump.
   * This is the frontend-side gate for IDE navigation events that [JumpsListener]
   * (on the backend) forwards unconditionally.
   */
  override fun addJump(projectId: String, jump: Jump, reset: Boolean) {
    if (!injector.globalIjOptions().unifyjumps) return
    super.addJump(projectId, jump, reset)
  }

  override fun removeJump(projectId: String, jump: Jump) {
    if (!injector.globalIjOptions().unifyjumps) return
    super.removeJump(projectId, jump)
  }

  override fun includeCurrentCommandAsNavigation(editor: VimEditor) {
    val project = (editor as IjVimEditor).editor.project ?: return
    IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation()
  }

  // We do not delete old project records.
  // Rationale: It's more likely that users will want to review their old projects and access their jump history
  // (e.g., recent files), than for the 100 jumps (max number of records) to consume enough space to be noticeable.
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

  override fun loadLegacyState(element: Any) {
    loadState(element as Element)
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
