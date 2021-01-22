/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.ProjectViewImpl
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.KeyStrokeAdapter
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.common.CommandAlias
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.group.KeyGroup
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.runAfterGotFocus
import com.maddyhome.idea.vim.key.CommandNode
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.key.Node
import com.maddyhome.idea.vim.key.RequiredShortcut
import com.maddyhome.idea.vim.key.RootNode
import com.maddyhome.idea.vim.key.addLeafs
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import javax.swing.SwingConstants

class NerdTree : VimExtension {
  override fun getName(): String = "NERDTree"

  override fun init() {
    addCommand("NERDTreeFocus", FocusHandler())

    ProjectManager.getInstance().openProjects.forEach { project -> installDispatcher(project) }
  }

  class FocusHandler : CommandAliasHandler {
    override fun execute(editor: Editor, context: DataContext) {
      callAction("ActivateProjectToolWindow", context)
    }
  }

  private fun installDispatcher(project: Project) {
    val action = NerdDispatcher.instance
    val shortcuts = collectShortcuts(actionsRoot).map { RequiredShortcut(it, owner) }
    action.registerCustomShortcutSet(
      KeyGroup.toShortcutSet(shortcuts),
      (ProjectView.getInstance(project) as ProjectViewImpl).component
    )
  }

  class NerdDispatcher : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val keyStroke = getKeyStroke(e) ?: return

      val nextNode = currentNode[keyStroke]

      when (nextNode) {
        null -> currentNode = actionsRoot
        is CommandNode<NerdAction> -> {
          currentNode = actionsRoot

          val action = nextNode.actionHolder
          when (action) {
            is NerdAction.ToIj -> callAction(action.name, e.dataContext)
            is NerdAction.Code -> e.project?.let { action.action(it, e.dataContext, e) }
          }
        }
        is CommandPartNode<NerdAction> -> currentNode = nextNode
      }
    }

    companion object {
      val instance = NerdDispatcher()
    }

    /**
     * getDefaultKeyStroke is needed for NEO layout keyboard VIM-987
     * but we should cache the value because on the second call (isEnabled -> actionPerformed)
     * the event is already consumed
     */
    private var keyStrokeCache: Pair<KeyEvent?, KeyStroke?> = null to null

    private fun getKeyStroke(e: AnActionEvent): KeyStroke? {
      val inputEvent = e.inputEvent
      if (inputEvent is KeyEvent) {
        val defaultKeyStroke = KeyStrokeAdapter.getDefaultKeyStroke(inputEvent)
        val strokeCache = keyStrokeCache
        if (defaultKeyStroke != null) {
          keyStrokeCache = inputEvent to defaultKeyStroke
          return defaultKeyStroke
        } else if (strokeCache.first === inputEvent) {
          keyStrokeCache = null to null
          return strokeCache.second
        }
        return KeyStroke.getKeyStrokeForEvent(inputEvent)
      }
      return null
    }
  }

  companion object {
    fun callAction(name: String, context: DataContext) {
      val action = ActionManager.getInstance().getAction(name) ?: run {
        VimPlugin.showMessage(MessageHelper.message("action.not.found.0", name))
        return
      }
      val application = ApplicationManager.getApplication()
      if (application.isUnitTestMode) {
        KeyHandler.executeAction(action, context)
      } else {
        runAfterGotFocus(Runnable { KeyHandler.executeAction(action, context) })
      }
    }

    private fun addCommand(alias: String, handler: CommandAliasHandler) {
      VimPlugin.getCommand().setAlias(alias, CommandAlias.Call(0, -1, alias, handler))
    }

    private val actionsRoot: RootNode<NerdAction> = RootNode<NerdAction>().apply {
      // TODO: 22.01.2021 Should not just to the last line after the first
      addLeafs("j", NerdAction.ToIj("Tree-selectNext"))
      addLeafs("k", NerdAction.ToIj("Tree-selectPrevious"))
      addLeafs("o", NerdAction.Code { project, dataContext, _ ->
        val tree = ProjectView.getInstance(project).currentProjectViewPane.tree

        val array = CommonDataKeys.NAVIGATABLE_ARRAY.getData(dataContext)?.filter { it.canNavigateToSource() }
        if (array.isNullOrEmpty()) {
          val row = tree.selectionRows?.getOrNull(0) ?: return@Code
          if (tree.isExpanded(row)) {
            tree.collapseRow(row)
          } else {
            tree.expandRow(row)
          }
        } else {
          array.forEach { it.navigate(true) }
        }
      })
      addLeafs("go", NerdAction.Code { _, dataContext, _ ->
        CommonDataKeys.NAVIGATABLE_ARRAY
          .getData(dataContext)
          ?.filter { it.canNavigateToSource() }
          ?.forEach { it.navigate(false) }
      })

      // TODO: 21.01.2021 Should option in left split
      addLeafs("s", NerdAction.ToIj("OpenInRightSplit"))
      // TODO: 21.01.2021 Should option in above split
      addLeafs("i", NerdAction.Code { project, _, event ->
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@Code
        val splitters = FileEditorManagerEx.getInstanceEx(project).splitters
        val currentWindow = splitters.currentWindow
        currentWindow.split(SwingConstants.HORIZONTAL, true, file, true)
      })
      addLeafs("gs", NerdAction.Code { project, context, event ->
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@Code
        val splitters = FileEditorManagerEx.getInstanceEx(project).splitters
        val currentWindow = splitters.currentWindow
        currentWindow.split(SwingConstants.VERTICAL, true, file, true)

        // FIXME: 22.01.2021 This solution bouncing a bit
        callAction("ActivateProjectToolWindow", context)
      })
      addLeafs("gi", NerdAction.Code { project, context, event ->
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@Code
        val splitters = FileEditorManagerEx.getInstanceEx(project).splitters
        val currentWindow = splitters.currentWindow
        currentWindow.split(SwingConstants.HORIZONTAL, true, file, true)

        callAction("ActivateProjectToolWindow", context)
      })
    }

    private var currentNode: CommandPartNode<NerdAction> = actionsRoot

    private fun collectShortcuts(node: Node<NerdAction>): Set<KeyStroke> {
      return if (node is CommandPartNode<NerdAction>) {
        val res = node.keys.toMutableSet()
        res += node.values.map { collectShortcuts(it) }.flatten()
        res
      } else emptySet()
    }
  }
}
