/*
 * Copyright 2003-2022 The IdeaVim authors
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
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.mark.Jump
import com.maddyhome.idea.vim.newapi.IjVimEditor
import org.jdom.Element

@State(name = "VimJumpsSettings", storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)])
class VimJumpServiceImpl : VimJumpServiceBase(), PersistentStateComponent<Element?> {
  companion object {
    private val logger = vimLogger<VimJumpServiceImpl>()
  }

  override fun includeCurrentCommandAsNavigation(editor: VimEditor) {
    val project = (editor as IjVimEditor).editor.project
    if (project != null) {
      IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation()
    }
  }

  override fun getState(): Element {
    val jumpsElem = Element("jumps")
    for (jump in jumps) {
      val jumpElem = Element("jump")
      jumpElem.setAttribute("line", jump.line.toString())
      jumpElem.setAttribute("column", jump.col.toString())
      jumpElem.setAttribute("filename", StringUtil.notNullize(jump.filepath))
      jumpsElem.addContent(jumpElem)
      if (logger.isDebug()) {
        logger.debug("saved jump = $jump")
      }
    }
    return jumpsElem
  }

  override fun loadState(state: Element) {
    val jumpList = state.getChildren("jump")
    for (jumpElement in jumpList) {
      val jump = Jump(
        Integer.parseInt(jumpElement.getAttributeValue("line")),
        Integer.parseInt(jumpElement.getAttributeValue("column")),
        jumpElement.getAttributeValue("filename")
      )
      jumps.add(jump)
    }

    if (logger.isDebug()) {
      logger.debug("jumps=$jumps")
    }
  }
}
