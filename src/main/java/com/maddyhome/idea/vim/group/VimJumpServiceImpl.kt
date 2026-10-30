/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.impl.editorIdOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.PlatformUtils
import com.intellij.platform.project.projectId
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimJumpServiceBase
import com.maddyhome.idea.vim.api.VimWindowIdService
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.group.diff.DiffJumps
import com.maddyhome.idea.vim.group.jump.DiffJump
import com.maddyhome.idea.vim.group.jump.JumpRemoteApi
import com.maddyhome.idea.vim.mark.Jump
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
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

  override fun jumpAtCaret(editor: VimEditor): Jump? {
    return DiffJumps.jumpAt(editor.ij) ?: diffJumpOnBackend(editor.ij) ?: super.jumpAtCaret(editor)
  }

  override fun showInDiff(editor: VimEditor, jump: Jump): Boolean {
    val project = editor.ij.project ?: return false
    return DiffJumps.show(project, jump) || showDiffJumpOnBackend(project, jump)
  }

  /**
   * In split mode, diff tabs live on the backend. The frontend shows them with editors that mirror the backend's ones
   *
   * A write action can't wait for the backend, so a jump recorded in one falls back to what the frontend knows.
   */
  private fun diffJumpOnBackend(editor: Editor): Jump? {
    if (!PlatformUtils.isJetBrainsClient() || editor.editorKind != EditorKind.DIFF) return null
    if (ApplicationManager.getApplication().isWriteAccessAllowed) return null
    val editorId = editor.editorIdOrNull() ?: return null
    val caret = editor.caretModel.logicalPosition
    return rpc(editor.project) { JumpRemoteApi.getInstance().diffJumpAt(editorId, caret.line, caret.column) }?.toJump()
  }

  private fun showDiffJumpOnBackend(project: Project, jump: Jump): Boolean {
    if (!PlatformUtils.isJetBrainsClient()) return false
    val diffJump = DiffJump.of(jump) ?: return false
    return rpc(project) { JumpRemoteApi.getInstance().showDiffJump(project.projectId(), diffJump) }
  }

  // We do not delete old project records.
  // Rationale: It's more likely that users will want to review their old projects and access their jump history
  // (e.g., recent files), than for the 100 jumps (max number of records) to consume enough space to be noticeable.
  override fun getState(): Element {
    val projectsElem = Element("projects")
    for ((project, jumps) in jumpsByProject()) {
      val projectElement = Element("project").setAttribute("id", project)
      // A jump to text that is not in a file, such as an older revision shown in a diff, has nowhere to go after a restart
      for (jump in jumps.filter { it.isInFile }) {
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
   * One jump list per project, taken from the most recently used window
   *
   * Window ids mean nothing after a restart, so saving a list per window would write records that can never be read
   * back. Vim also keeps a single list in `viminfo`.
   */
  private fun jumpsByProject(): Map<String, List<Jump>> {
    val result = LinkedHashMap<String, List<Jump>>()
    for ((scopeId, jumps) in scopeToJumps) {
      result[VimWindowIdService.projectIdOf(scopeId)] = jumps
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
