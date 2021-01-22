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
import com.intellij.ui.TreeExpandCollapse
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.common.CommandAlias
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
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


/**
 * Key      Description                                                  help-tag~
 *
 * + o........Open files, directories and bookmarks......................|NERDTree-o|
 * + go.......Open selected file, but leave cursor in the NERDTree......|NERDTree-go|
 * Open selected bookmark dir in current NERDTree
 * t........Open selected node/bookmark in a new tab...................|NERDTree-t|
 * T........Same as 't' but keep the focus on the current tab..........|NERDTree-T|
 * + i........Open selected file in a split window.......................|NERDTree-i|
 * + gi.......Same as i, but leave the cursor on the NERDTree...........|NERDTree-gi|
 * + s........Open selected file in a new vsplit.........................|NERDTree-s|
 * + gs.......Same as s, but leave the cursor on the NERDTree...........|NERDTree-gs|
 * <CR>.....User-definable custom open action.......................|NERDTree-<CR>|
 * + O........Recursively open the selected directory....................|NERDTree-O|
 * + x........Close the current nodes parent.............................|NERDTree-x|
 * + X........Recursively close all children of the current node.........|NERDTree-X|
 * e........Edit the current dir.......................................|NERDTree-e|
 *
 * double-click....same as |NERDTree-o|.
 * middle-click....same as |NERDTree-i| for files, and |NERDTree-e| for dirs.
 *
 * D........Delete the current bookmark ...............................|NERDTree-D|
 *
 * + P........Jump to the root node......................................|NERDTree-P|
 * + p........Jump to current nodes parent...............................|NERDTree-p|
 * K........Jump up inside directories at the current tree depth.......|NERDTree-K|
 * J........Jump down inside directories at the current tree depth.....|NERDTree-J|
 * <C-J>....Jump down to next sibling of the current directory.......|NERDTree-C-J|
 * <C-K>....Jump up to previous sibling of the current directory.....|NERDTree-C-K|
 *
 * C........Change the tree root to the selected dir...................|NERDTree-C|
 * u........Move the tree root up one directory........................|NERDTree-u|
 * U........Same as 'u' except the old root node is left open..........|NERDTree-U|
 * r........Recursively refresh the current directory..................|NERDTree-r|
 * R........Recursively refresh the current root.......................|NERDTree-R|
 * m........Display the NERDTree menu..................................|NERDTree-m|
 * cd.......Change the CWD to the dir of the selected node............|NERDTree-cd|
 * CD.......Change tree root to the CWD...............................|NERDTree-CD|
 *
 * I........Toggle whether hidden files displayed......................|NERDTree-I|
 * f........Toggle whether the file filters are used...................|NERDTree-f|
 * F........Toggle whether files are displayed.........................|NERDTree-F|
 * B........Toggle whether the bookmark table is displayed.............|NERDTree-B|
 *
 * q........Close the NERDTree window..................................|NERDTree-q|
 * A........Zoom (maximize/minimize) the NERDTree window...............|NERDTree-A|
 * ?........Toggle the display of the quick help.......................|NERDTree-?|
 */
class NerdTree : VimExtension {
  override fun getName(): String = "NERDTree"

  override fun init() {
    registerCommands()

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
      var keyStroke = getKeyStroke(e) ?: return
      val keyChar = keyStroke.keyChar
      if (keyChar != KeyEvent.CHAR_UNDEFINED) {
        keyStroke = KeyStroke.getKeyStroke(keyChar)
      }

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

  private fun registerCommands() {
    // TODO: 22.01.2021 Should not just to the last line after the first
    registerCommand("j", NerdAction.ToIj("Tree-selectNext"))
    registerCommand("k", NerdAction.ToIj("Tree-selectPrevious"))
    registerCommand("g:NERDTreeMapActivateNode", "o", NerdAction.Code { project, dataContext, _ ->
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
    registerCommand("g:NERDTreeMapPreview", "go", NerdAction.Code { _, dataContext, _ ->
      CommonDataKeys.NAVIGATABLE_ARRAY
        .getData(dataContext)
        ?.filter { it.canNavigateToSource() }
        ?.forEach { it.navigate(false) }
    })

    // TODO: 21.01.2021 Should option in left split
    registerCommand("g:NERDTreeMapOpenVSplit", "s", NerdAction.ToIj("OpenInRightSplit"))
    // TODO: 21.01.2021 Should option in above split
    registerCommand("g:NERDTreeMapActivateNode", "i", NerdAction.Code { project, _, event ->
      val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@Code
      val splitters = FileEditorManagerEx.getInstanceEx(project).splitters
      val currentWindow = splitters.currentWindow
      currentWindow.split(SwingConstants.HORIZONTAL, true, file, true)
    })
    registerCommand("g:NERDTreeMapPreviewVSplit", "gs", NerdAction.Code { project, context, event ->
      val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@Code
      val splitters = FileEditorManagerEx.getInstanceEx(project).splitters
      val currentWindow = splitters.currentWindow
      currentWindow.split(SwingConstants.VERTICAL, true, file, true)

      // FIXME: 22.01.2021 This solution bouncing a bit
      callAction("ActivateProjectToolWindow", context)
    })
    registerCommand("g:NERDTreeMapPreviewSplit", "gi", NerdAction.Code { project, context, event ->
      val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@Code
      val splitters = FileEditorManagerEx.getInstanceEx(project).splitters
      val currentWindow = splitters.currentWindow
      currentWindow.split(SwingConstants.HORIZONTAL, true, file, true)

      callAction("ActivateProjectToolWindow", context)
    })
    registerCommand("g:NERDTreeMapOpenRecursively", "O", NerdAction.Code { project, _, _ ->
      val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
      TreeExpandCollapse.expandAll(tree)
    })
    registerCommand("g:NERDTreeMapCloseChildren", "X", NerdAction.Code { project, _, _ ->
      val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
      TreeExpandCollapse.collapse(tree)
    })
    registerCommand("g:NERDTreeMapCloseDir", "x", NerdAction.Code { project, _, _ ->
      val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
      val currentPath = tree.selectionPath ?: return@Code
      if (tree.isExpanded(currentPath)) {
        tree.collapsePath(currentPath)
      } else {
        val parentPath = currentPath.parentPath ?: return@Code
        if (parentPath.parentPath != null) {
          // The real root of the project is not shown in the project view, so we check the grandparent of the node
          tree.collapsePath(parentPath)
        }
      }
    })
    registerCommand("g:NERDTreeMapJumpRoot", "P", NerdAction.Code { project, _, _ ->
      val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
      var currentPath = tree.selectionPath ?: return@Code
      while (currentPath.parentPath != null && currentPath.parentPath.parentPath != null) {
        // The real root of the project is not shown in the project view, so we check the grandparent of the node
        currentPath = currentPath.parentPath
      }
      tree.selectionPath = currentPath
    })
    registerCommand("g:NERDTreeMapJumpParent", "p", NerdAction.Code { project, _, _ ->
      val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
      val currentPath = tree.selectionPath ?: return@Code
      val parentPath = currentPath.parentPath ?:return@Code
      if (parentPath.parentPath != null) {
        // The real root of the project is not shown in the project view, so we check the grandparent of the node
        tree.selectionPath = parentPath
      }
    })
    registerCommand("g:NERDTreeMapJumpFirstChild", "K", NerdAction.Code { project, _, _ ->
      val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
      val currentPath = tree.selectionPath ?: return@Code
      val parent = currentPath.parentPath ?: return@Code
      val row = tree.getRowForPath(parent)
      tree.setSelectionRow(row + 1)

      tree.scrollRowToVisible(row + 1)
    })
    registerCommand("g:NERDTreeMapJumpLastChild", "J", NerdAction.Code { project, _, _ ->
      val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
      val currentPath = tree.selectionPath ?: return@Code

      val currentPathCount = currentPath.pathCount
      var row = tree.getRowForPath(currentPath)

      var expectedRow = row
      while (true) {
        row++
        val nextPath = tree.getPathForRow(row) ?: break
        val pathCount = nextPath.pathCount
        if (pathCount == currentPathCount) expectedRow = row
        if (pathCount < currentPathCount) break
      }
      tree.setSelectionRow(expectedRow)

      tree.scrollRowToVisible(expectedRow)
    })
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

    private fun registerCommand(variable: String, default: String, action: NerdAction) {
      val mappings = VimScriptGlobalEnvironment.getInstance().variables.getOrDefault(variable, default).toString()
      actionsRoot.addLeafs(mappings, action)
    }

    private fun registerCommand(default: String, action: NerdAction) {
      actionsRoot.addLeafs(default, action)
    }

    private val actionsRoot: RootNode<NerdAction> = RootNode()
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
