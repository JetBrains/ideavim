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
import com.intellij.openapi.components.service
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.project.projectId
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimJumpServiceBase
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.group.jump.JumpRemoteApi
import com.maddyhome.idea.vim.mark.Jump
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.initInjector
import kotlinx.coroutines.launch
import org.jdom.Element

/**
 * Jump service with persistence and navigation integration via RPC.
 *
 * [includeCurrentCommandAsNavigation] delegates to the backend via [JumpRemoteApi]
 * where [IdeDocumentHistory] is available. Works in both monolith and split mode.
 */
@State(
  name = "VimJumpsSettings",
  storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)]
)
internal class VimJumpServiceImpl : VimJumpServiceBase(), PersistentStateComponent<Element?> {
  companion object {
    private val logger = vimLogger<VimJumpServiceImpl>()

    /** Separates the project id from the window id in a window scoped jump list's scope id */
    private const val WINDOW_SCOPE_SEPARATOR = '/'
  }

  override var lastJumpTimeStamp: Long = 0

  override fun includeCurrentCommandAsNavigation(editor: VimEditor) {
    val project = (editor as IjVimEditor).editor.project ?: return
    val projectId = project.projectId()
    // Fire-and-forget: this is called from write actions (motion handlers),
    // so we can't use blocking rpc(). Launch async instead.
    service<CoroutineScopeProvider>().coroutineScope.launch {
      JumpRemoteApi.getInstance().includeCurrentCommandAsNavigation(projectId)
    }
  }

  // We do not delete old project records.
  // Rationale: It's more likely that users will want to review their old projects and access their jump history
  // (e.g., recent files), than for the 100 jumps (max number of records) to consume enough space to be noticeable.
  override fun getState(): Element {
    val projectsElem = Element("projects")
    for ((project, jumps) in jumpsByProject()) {
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

  /**
   * The jump lists to persist, keyed by project id
   *
   * With the 'windowjumps' option set, a jump list is scoped to a window and its scope id is `"<projectId>/<windowId>"`
   * (see [com.maddyhome.idea.vim.newapi.IjVimEditor.jumpListId]). Window ids only mean something within a session, so
   * persisting them would write records that can never be read back, and which would accumulate over time. Instead we
   * save one list per project, taken from the most recently used window - the closest thing to Vim's single `viminfo`
   * jump list. [scopeToJumps] iterates from least to most recently updated, so later entries win.
   *
   * Without 'windowjumps' the scope id *is* the project id, and this grouping is a no-op.
   */
  private fun jumpsByProject(): Map<String, List<Jump>> {
    val result = LinkedHashMap<String, List<Jump>>()
    for ((scopeId, jumps) in scopeToJumps) {
      result[scopeId.substringBefore(WINDOW_SCOPE_SEPARATOR)] = jumps
    }
    return result
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
      scopeToJumps[projectId] = jumps
    }
  }
}
