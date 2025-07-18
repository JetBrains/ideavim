/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.AbstractProjectViewPane
import com.intellij.ide.projectView.impl.ProjectViewImpl
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.KeyStrokeAdapter
import com.intellij.ui.TreeExpandCollapse
import com.intellij.ui.speedSearch.SpeedSearchSupply
import com.intellij.util.ui.tree.TreeUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.CommandAlias
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.group.KeyGroup
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.runAfterGotFocus
import com.maddyhome.idea.vim.helper.keyStroke
import com.maddyhome.idea.vim.helper.vimKeyStroke
import com.maddyhome.idea.vim.key.KeyStrokeTrie
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.RequiredShortcut
import com.maddyhome.idea.vim.key.add
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import javax.swing.SwingConstants

/**
 * Features and issues:
 * - Files are opened with "open with a single click"
 * - Enable mappings in project view for regilar commands "j", "k", etc.
 * - Support more regular commands (gg, G, etc.)
 * - Support ex commands in project view (https://youtrack.jetbrains.com/issue/VIM-1042#focus=Comments-27-4654338.0-0)
 * - Add label after pressing `/`
 * - Write UI tests
 */

/**
 * Key      Description                                                  help-tag~
 * '+' means supported
 *
 * + o........Open files, directories and bookmarks......................|NERDTree-o|
 * + go.......Open selected file, but leave cursor in the NERDTree......|NERDTree-go|
 *            Open selected bookmark dir in current NERDTree
 * + t........Open selected node/bookmark in a new tab...................|NERDTree-t|
 * + T........Same as 't' but keep the focus on the current tab..........|NERDTree-T|
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
 * + K........Jump up inside directories at the current tree depth.......|NERDTree-K|
 * + J........Jump down inside directories at the current tree depth.....|NERDTree-J|
 * + <C-J>....Jump down to next sibling of the current directory.......|NERDTree-C-J|
 * + <C-K>....Jump up to previous sibling of the current directory.....|NERDTree-C-K|
 *
 * C........Change the tree root to the selected dir...................|NERDTree-C|
 * u........Move the tree root up one directory........................|NERDTree-u|
 * U........Same as 'u' except the old root node is left open..........|NERDTree-U|
 * + r........Recursively refresh the current directory..................|NERDTree-r|
 * + R........Recursively refresh the current root.......................|NERDTree-R|
 * + m........Display the NERDTree menu..................................|NERDTree-m|
 * cd.......Change the CWD to the dir of the selected node............|NERDTree-cd|
 * CD.......Change tree root to the CWD...............................|NERDTree-CD|
 *
 * I........Toggle whether hidden files displayed......................|NERDTree-I|
 * f........Toggle whether the file filters are used...................|NERDTree-f|
 * F........Toggle whether files are displayed.........................|NERDTree-F|
 * B........Toggle whether the bookmark table is displayed.............|NERDTree-B|
 *
 * + q........Close the NERDTree window..................................|NERDTree-q|
 * + A........Zoom (maximize/minimize) the NERDTree window...............|NERDTree-A|
 * ?........Toggle the display of the quick help.......................|NERDTree-?|
 */
internal class NerdTree : VimExtension {
  override fun getName(): String = pluginName

  override fun init() {
    LOG.info("IdeaVim: Initializing NERDTree extension. Disable this extension if you observe a strange behaviour of the project tree. E.g. moving down on 'j'")

    registerCommands()

    addCommand("NERDTreeFocus", IjCommandHandler("ActivateProjectToolWindow"))
    addCommand("NERDTree", IjCommandHandler("ActivateProjectToolWindow"))
    addCommand("NERDTreeToggle", ToggleHandler())
    addCommand("NERDTreeClose", CloseHandler())
    addCommand("NERDTreeFind", IjCommandHandler("SelectInProjectView"))
    addCommand("NERDTreeRefreshRoot", IjCommandHandler("Synchronize"))

    synchronized(Util.monitor) {
      Util.commandsRegistered = true
      ProjectManager.getInstance().openProjects.forEach { project -> installDispatcher(project) }
    }
  }

  class IjCommandHandler(private val actionId: String) : CommandAliasHandler {
    override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
      Util.callAction(editor, actionId, context)
    }
  }

  class ToggleHandler : CommandAliasHandler {
    override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
      val project = editor.ij.project ?: return
      val toolWindow = ToolWindowManagerEx.getInstanceEx(project).getToolWindow(ToolWindowId.PROJECT_VIEW) ?: return
      if (toolWindow.isVisible) {
        toolWindow.hide()
      } else {
        Util.callAction(editor, "ActivateProjectToolWindow", context)
      }
    }
  }

  class CloseHandler : CommandAliasHandler {
    override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
      val project = editor.ij.project ?: return
      val toolWindow = ToolWindowManagerEx.getInstanceEx(project).getToolWindow(ToolWindowId.PROJECT_VIEW) ?: return
      if (toolWindow.isVisible) {
        toolWindow.hide()
      }
    }
  }

  class ProjectViewListener(private val project: Project) : ToolWindowManagerListener {
    override fun toolWindowShown(toolWindow: ToolWindow) {
      if (ToolWindowId.PROJECT_VIEW != toolWindow.id) return

      val dispatcher = NerdDispatcher.getInstance(project)
      if (dispatcher.speedSearchListenerInstalled) return

      // I specify nullability explicitly as we've got a lot of exceptions saying this property is null
      val currentProjectViewPane: AbstractProjectViewPane? = ProjectView.getInstance(project).currentProjectViewPane
      val tree = currentProjectViewPane?.tree ?: return
      val supply = SpeedSearchSupply.getSupply(tree, true) ?: return

      // NB: Here might be some issues with concurrency, but it's not really bad, I think
      dispatcher.speedSearchListenerInstalled = true
      supply.addChangeListener {
        dispatcher.waitForSearch = false
      }
    }
  }

  // TODO I'm not sure is this activity runs at all? Should we use [RunOnceUtil] instead?
  class NerdStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
      synchronized(Util.monitor) {
        if (!Util.commandsRegistered) return
        installDispatcher(project)
      }
    }
  }

  class NerdDispatcher : DumbAwareAction() {
    internal var waitForSearch = false
    internal var speedSearchListenerInstalled = false

    private val keys = mutableListOf<KeyStroke>()

    override fun actionPerformed(e: AnActionEvent) {
      var keyStroke = getKeyStroke(e) ?: return
      val keyChar = keyStroke.keyChar
      if (keyChar != KeyEvent.CHAR_UNDEFINED) {
        keyStroke = KeyStroke.getKeyStroke(keyChar)
      }

      keys.add(keyStroke)
      actionsRoot.getData(keys.map { it.vimKeyStroke })?.let { action ->
        when (action) {
          is NerdAction.ToIj -> Util.callAction(null, action.name, e.dataContext.vim)
          is NerdAction.Code -> e.project?.let { action.action(it, e.dataContext, e) }
        }

        keys.clear()
      }
    }

    override fun update(e: AnActionEvent) {
      // Special processing of esc.
      if ((e.inputEvent as? KeyEvent)?.keyCode == ESCAPE_KEY_CODE) {
        e.presentation.isEnabled = waitForSearch
        return
      }

      if (waitForSearch) {
        e.presentation.isEnabled = false
        return
      }
      e.presentation.isEnabled = !speedSearchIsHere(e)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    private fun speedSearchIsHere(e: AnActionEvent): Boolean {
      val searchText = e.getData(PlatformDataKeys.SPEED_SEARCH_TEXT)
      return !searchText.isNullOrEmpty()
    }

    companion object {
      fun getInstance(project: Project): NerdDispatcher {
        return project.getService(NerdDispatcher::class.java)
      }

      private const val ESCAPE_KEY_CODE = 27
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
    registerCommand(
      "NERDTreeMapActivateNode",
      "o",
      NerdAction.Code { project, dataContext, _ ->
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
      },
    )
    registerCommand(
      "NERDTreeMapPreview",
      "go",
      NerdAction.Code { _, dataContext, _ ->
        CommonDataKeys.NAVIGATABLE_ARRAY
          .getData(dataContext)
          ?.filter { it.canNavigateToSource() }
          ?.forEach { it.navigate(false) }
      },
    )
    registerCommand(
      "NERDTreeMapOpenInTab",
      "t",
      NerdAction.Code { _, dataContext, _ ->
        // FIXME: 22.01.2021 Doesn't work correct
        CommonDataKeys.NAVIGATABLE_ARRAY
          .getData(dataContext)
          ?.filter { it.canNavigateToSource() }
          ?.forEach { it.navigate(true) }
      },
    )
    registerCommand(
      "NERDTreeMapOpenInTabSilent",
      "T",
      NerdAction.Code { _, dataContext, _ ->
        // FIXME: 22.01.2021 Doesn't work correct
        CommonDataKeys.NAVIGATABLE_ARRAY
          .getData(dataContext)
          ?.filter { it.canNavigateToSource() }
          ?.forEach { it.navigate(true) }
      },
    )

    // TODO: 21.01.2021 Should option in left split
    registerCommand("NERDTreeMapOpenVSplit", "s", NerdAction.ToIj("OpenInRightSplit"))
    // TODO: 21.01.2021 Should option in above split
    registerCommand(
      "NERDTreeMapOpenSplit",
      "i",
      NerdAction.Code { project, _, event ->
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@Code
        if (file.isDirectory) return@Code
        val splitters = FileEditorManagerEx.getInstanceEx(project).splitters
        val currentWindow = splitters.currentWindow
        currentWindow?.split(SwingConstants.HORIZONTAL, true, file, true)
      },
    )
    registerCommand(
      "NERDTreeMapPreviewVSplit",
      "gs",
      NerdAction.Code { project, context, event ->
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@Code
        val splitters = FileEditorManagerEx.getInstanceEx(project).splitters
        val currentWindow = splitters.currentWindow
        currentWindow?.split(SwingConstants.VERTICAL, true, file, true)

        // FIXME: 22.01.2021 This solution bouncing a bit
        Util.callAction(null, "ActivateProjectToolWindow", context.vim)
      },
    )
    registerCommand(
      "NERDTreeMapPreviewSplit",
      "gi",
      NerdAction.Code { project, context, event ->
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@Code
        val splitters = FileEditorManagerEx.getInstanceEx(project).splitters
        val currentWindow = splitters.currentWindow
        currentWindow?.split(SwingConstants.HORIZONTAL, true, file, true)

        Util.callAction(null, "ActivateProjectToolWindow", context.vim)
      },
    )
    registerCommand(
      "NERDTreeMapOpenRecursively",
      "O",
      NerdAction.Code { project, _, _ ->
        val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
        TreeExpandCollapse.expandAll(tree)
        tree.selectionPath?.let {
          TreeUtil.scrollToVisible(tree, it, false)
        }
      },
    )
    registerCommand(
      "NERDTreeMapCloseChildren",
      "X",
      NerdAction.Code { project, _, _ ->
        val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
        TreeExpandCollapse.collapse(tree)
        tree.selectionPath?.let {
          TreeUtil.scrollToVisible(tree, it, false)
        }
      },
    )
    registerCommand(
      "NERDTreeMapCloseDir",
      "x",
      NerdAction.Code { project, _, _ ->
        val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
        val currentPath = tree.selectionPath ?: return@Code
        if (tree.isExpanded(currentPath)) {
          tree.collapsePath(currentPath)
        } else {
          val parentPath = currentPath.parentPath ?: return@Code
          if (parentPath.parentPath != null) {
            // The real root of the project is not shown in the project view, so we check the grandparent of the node
            tree.collapsePath(parentPath)
            TreeUtil.scrollToVisible(tree, parentPath, false)
          }
        }
      },
    )
    registerCommand("NERDTreeMapJumpRoot", "P", NerdAction.ToIj("Tree-selectFirst"))
    registerCommand(
      "NERDTreeMapJumpParent",
      "p",
      NerdAction.Code { project, _, _ ->
        val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
        val currentPath = tree.selectionPath ?: return@Code
        val parentPath = currentPath.parentPath ?: return@Code
        if (parentPath.parentPath != null) {
          // The real root of the project is not shown in the project view, so we check the grandparent of the node
          tree.selectionPath = parentPath
          TreeUtil.scrollToVisible(tree, parentPath, false)
        }
      },
    )
    registerCommand(
      "NERDTreeMapJumpFirstChild",
      "K",
      NerdAction.Code { project, _, _ ->
        val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
        val currentPath = tree.selectionPath ?: return@Code
        val parent = currentPath.parentPath ?: return@Code
        val row = tree.getRowForPath(parent)
        tree.setSelectionRow(row + 1)

        tree.scrollRowToVisible(row + 1)
      },
    )
    registerCommand(
      "NERDTreeMapJumpLastChild",
      "J",
      NerdAction.Code { project, _, _ ->
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
      },
    )
    registerCommand("gg", NerdAction.Code { project, _, _ ->
      val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
      tree.setSelectionRow(0)
      tree.scrollRowToVisible(0)
    })
    registerCommand("G", NerdAction.Code { project, _, _ ->
      val tree = ProjectView.getInstance(project).currentProjectViewPane.tree
      val lastRowIndex = tree.rowCount -1
      tree.setSelectionRow(lastRowIndex)
      tree.scrollRowToVisible(lastRowIndex)
    })
    registerCommand(
      "NERDTreeMapJumpNextSibling",
      "<C-J>",
      NerdAction.ToIj("Tree-selectNextSibling"),
    )
    registerCommand(
      "NERDTreeMapJumpPrevSibling",
      "<C-K>",
      NerdAction.ToIj("Tree-selectPreviousSibling"),
    )
    registerCommand(
      "NERDTreeMapRefresh",
      "r",
      NerdAction.ToIj("SynchronizeCurrentFile"),
    )
    registerCommand("NERDTreeMapToggleHidden", "I", NerdAction.ToIj("ProjectView.ShowExcludedFiles"))
    registerCommand("NERDTreeMapNewFile", "n", NerdAction.ToIj("NewFile"))
    registerCommand("NERDTreeMapNewDir", "N", NerdAction.ToIj("NewDir"))
    registerCommand("NERDTreeMapDelete", "d", NerdAction.ToIj("\$Delete"))
    registerCommand("NERDTreeMapCopy", "y", NerdAction.ToIj("\$Copy"))
    registerCommand("NERDTreeMapPaste", "v", NerdAction.ToIj("\$Paste"))
    registerCommand("NERDTreeMapRename", "<C-r>", NerdAction.ToIj("RenameElement"))
    registerCommand("NERDTreeMapRefreshRoot", "R", NerdAction.ToIj("Synchronize"))
    registerCommand("NERDTreeMapMenu", "m", NerdAction.ToIj("ShowPopupMenu"))
    registerCommand("NERDTreeMapQuit", "q", NerdAction.ToIj("HideActiveWindow"))
    registerCommand(
      "NERDTreeMapToggleZoom",
      "A",
      NerdAction.ToIj("MaximizeToolWindow"),
    )

    registerCommand(
      "/",
      NerdAction.Code { project, _, _ ->
        NerdDispatcher.getInstance(project).waitForSearch = true
      },
    )

    registerCommand(
      "<ESC>",
      NerdAction.Code { project, _, _ ->
        val instance = NerdDispatcher.getInstance(project)
        if (instance.waitForSearch) {
          instance.waitForSearch = false
        }
      },
    )
  }

  object Util {
    internal val monitor = Any()
    internal var commandsRegistered = false
    fun callAction(editor: VimEditor?, name: String, context: ExecutionContext) {
      val action = ActionManager.getInstance().getAction(name) ?: run {
        VimPlugin.showMessage(MessageHelper.message("action.not.found.0", name))
        return
      }
      val application = ApplicationManager.getApplication()
      if (application.isUnitTestMode) {
        injector.actionExecutor.executeAction(editor, action.vim, context)
      } else {
        runAfterGotFocus {
          injector.actionExecutor.executeAction(editor, action.vim, context)
        }
      }
    }
  }

  companion object {
    const val pluginName = "NERDTree"
    private val LOG = logger<NerdTree>()
  }
}

private fun addCommand(alias: String, handler: CommandAliasHandler) {
  VimPlugin.getCommand().setAlias(alias, CommandAlias.Call(0, -1, alias, handler))
}

private fun registerCommand(variable: String, defaultMapping: String, action: NerdAction) {
  val variableValue = VimPlugin.getVariableService().getGlobalVariableValue(variable)
  val mapping = if (variableValue is VimString) {
    variableValue.value
  } else {
    defaultMapping
  }
  registerCommand(mapping, action)
}

private fun registerCommand(mapping: String, action: NerdAction) {
  actionsRoot.add(mapping, action)
  injector.parser.parseKeys(mapping).forEach {
    distinctShortcuts.add(it.keyStroke)
  }
}

private val actionsRoot: KeyStrokeTrie<NerdAction> = KeyStrokeTrie<NerdAction>("NERDTree")
private val distinctShortcuts = mutableSetOf<KeyStroke>()

private fun installDispatcher(project: Project) {
  val dispatcher = NerdTree.NerdDispatcher.getInstance(project)
  val shortcuts = distinctShortcuts.map { RequiredShortcut(it.vimKeyStroke, MappingOwner.Plugin.get(NerdTree.pluginName)) }
  dispatcher.registerCustomShortcutSet(
    KeyGroup.toShortcutSet(shortcuts),
    (ProjectView.getInstance(project) as ProjectViewImpl).component,
  )
}
