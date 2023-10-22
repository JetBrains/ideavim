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
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl.PlaceInfo
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl.RecentPlacesListener
import com.intellij.openapi.util.text.StringUtil
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimJumpServiceBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.mark.Jump
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.ij
import org.jdom.Element

@State(name = "VimJumpsSettings", storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)])
internal class VimJumpServiceImpl : VimJumpServiceBase(), PersistentStateComponent<Element?> {
  companion object {
    private val logger = vimLogger<VimJumpServiceImpl>()
  }

  override var lastJumpTimeStamp: Long = 0

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
        jumpElement.getAttributeValue("filename"),
      )
      jumps.add(jump)
    }

    if (logger.isDebug()) {
      logger.debug("jumps=$jumps")
    }
  }
}

internal class JumpsListener : RecentPlacesListener {
  override fun recentPlaceAdded(changePlace: PlaceInfo, isChanged: Boolean) {
    if (!injector.globalIjOptions().unifyjumps) return

    val jumpService = injector.jumpService
    if (!isChanged) {
      if (changePlace.timeStamp < jumpService.lastJumpTimeStamp) return // this listener is notified asynchronously, and
      // we do not want jumps that were processed before
      val jump = buildJump(changePlace) ?: return
      jumpService.addJump(jump, true)
    }
  }

  override fun recentPlaceRemoved(changePlace: PlaceInfo, isChanged: Boolean) {
    if (!injector.globalIjOptions().unifyjumps) return

    val jumpService = injector.jumpService
    if (!isChanged) {
      if (changePlace.timeStamp < jumpService.lastJumpTimeStamp) return // this listener is notified asynchronously, and
      // we do not want jumps that were processed before
      val jump = buildJump(changePlace) ?: return
      jumpService.removeJump(jump)
    }
  }

  private fun buildJump(place: PlaceInfo): Jump? {
    val editor = injector.editorGroup.localEditors().firstOrNull { it.ij.virtualFile == place.file } ?: return null
    val offset = place.caretPosition?.startOffset ?: return null

    val bufferPosition = editor.offsetToBufferPosition(offset)
    val line = bufferPosition.line
    val col = bufferPosition.column

    val path = place.file.path

    return Jump(line, col, path)
  }
}
