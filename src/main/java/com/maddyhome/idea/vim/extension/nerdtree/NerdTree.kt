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
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.speedSearch.SpeedSearchSupply
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.CommandAlias
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.group.KeyGroup
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.runAfterGotFocus
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.RequiredShortcut
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import java.awt.event.KeyEvent
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

    registerMappings()

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

  @Service(Service.Level.PROJECT)
  class NerdDispatcher : AbstractDispatcher(mappings) {
    internal var waitForSearch = false
    internal var speedSearchListenerInstalled = false

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
        return project.service<NerdDispatcher>()
      }

      private const val ESCAPE_KEY_CODE = 27
    }
  }

  private fun registerMappings() {
    mappings.registerNavigationMappings()

    mappings.register(
      "NERDTreeMapActivateNode",
      "o",
      Mappings.Action { event, tree ->
        val array = CommonDataKeys.NAVIGATABLE_ARRAY.getData(event.dataContext)?.filter { it.canNavigateToSource() }
        if (array.isNullOrEmpty()) {
          val row = tree.selectionRows?.getOrNull(0) ?: return@Action
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
    mappings.register(
      "NERDTreeMapPreview",
      "go",
      Mappings.Action { event, _ ->
        CommonDataKeys.NAVIGATABLE_ARRAY.getData(event.dataContext)?.filter { it.canNavigateToSource() }
          ?.forEach { it.navigate(false) }
      },
    )
    mappings.register(
      "NERDTreeMapOpenInTab",
      "t",
      Mappings.Action { event, _ ->
        // FIXME: 22.01.2021 Doesn't work correct
        CommonDataKeys.NAVIGATABLE_ARRAY.getData(event.dataContext)?.filter { it.canNavigateToSource() }
          ?.forEach { it.navigate(true) }
      },
    )
    mappings.register(
      "NERDTreeMapOpenInTabSilent",
      "T",
      Mappings.Action { event, _ ->
        // FIXME: 22.01.2021 Doesn't work correct
        CommonDataKeys.NAVIGATABLE_ARRAY.getData(event.dataContext)?.filter { it.canNavigateToSource() }
          ?.forEach { it.navigate(true) }
      },
    )

    // TODO: 21.01.2021 Should option in left split
    mappings.register("NERDTreeMapOpenVSplit", "s", Mappings.Action.ij("OpenInRightSplit"))
    // TODO: 21.01.2021 Should option in above split
    mappings.register(
      "NERDTreeMapOpenSplit",
      "i",
      Mappings.Action { event, _ ->
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@Action
        if (file.isDirectory) return@Action
        val currentWindow = Util.getSplittersCurrentWindow(event)
        currentWindow?.split(SwingConstants.HORIZONTAL, true, file, true)
      },
    )
    mappings.register(
      "NERDTreeMapPreviewVSplit",
      "gs",
      Mappings.Action { event, _ ->
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@Action
        val currentWindow = Util.getSplittersCurrentWindow(event)
        currentWindow?.split(SwingConstants.VERTICAL, true, file, true)

        // FIXME: 22.01.2021 This solution bouncing a bit
        Util.callAction(null, "ActivateProjectToolWindow", event.dataContext.vim)
      },
    )
    mappings.register(
      "NERDTreeMapPreviewSplit",
      "gi",
      Mappings.Action { event, _ ->
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@Action
        val currentWindow = Util.getSplittersCurrentWindow(event)
        currentWindow?.split(SwingConstants.HORIZONTAL, true, file, true)

        Util.callAction(null, "ActivateProjectToolWindow", event.dataContext.vim)
      },
    )
    mappings.register(
      "NERDTreeMapRefresh",
      "r",
      Mappings.Action.ij("SynchronizeCurrentFile"),
    )
    mappings.register("NERDTreeMapToggleHidden", "I", Mappings.Action.ij("ProjectView.ShowExcludedFiles"))
    mappings.register("NERDTreeMapNewFile", "n", Mappings.Action.ij("NewFile"))
    mappings.register("NERDTreeMapNewDir", "N", Mappings.Action.ij("NewDir"))
    mappings.register("NERDTreeMapDelete", "d", Mappings.Action.ij("\$Delete"))
    mappings.register("NERDTreeMapCopy", "y", Mappings.Action.ij("\$Copy"))
    mappings.register("NERDTreeMapPaste", "v", Mappings.Action.ij("\$Paste"))
    mappings.register("NERDTreeMapRename", "<C-r>", Mappings.Action.ij("RenameElement"))
    mappings.register("NERDTreeMapRefreshRoot", "R", Mappings.Action.ij("Synchronize"))
    mappings.register("NERDTreeMapMenu", "m", Mappings.Action.ij("ShowPopupMenu"))
    mappings.register("NERDTreeMapQuit", "q", Mappings.Action.ij("HideActiveWindow"))
    mappings.register(
      "NERDTreeMapToggleZoom",
      "A",
      Mappings.Action.ij("MaximizeToolWindow"),
    )

    mappings.register(
      "/",
      Mappings.Action { event, _ ->
        NerdDispatcher.getInstance(event.project ?: return@Action).waitForSearch = true
      },
    )

    mappings.register(
      "<ESC>",
      Mappings.Action { event, _ ->
        val instance = NerdDispatcher.getInstance(event.project ?: return@Action)
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

    fun getSplittersCurrentWindow(event: AnActionEvent): EditorWindow? {
      val splitters = FileEditorManagerEx.getInstanceEx(event.project ?: return null).splitters
      return splitters.currentWindow
    }
  }

  companion object {
    const val pluginName = "NERDTree"
    private val LOG = vimLogger<NerdTree>()
  }
}

private fun addCommand(alias: String, handler: CommandAliasHandler) {
  VimPlugin.getCommand().setAlias(alias, CommandAlias.Call(0, -1, alias, handler))
}

private val mappings = Mappings("NERDTree")

private fun installDispatcher(project: Project) {
  val dispatcher = NerdTree.NerdDispatcher.getInstance(project)
  val shortcuts = mappings.keyStrokes.map { RequiredShortcut(it, MappingOwner.Plugin.get(NerdTree.pluginName)) }
  dispatcher.registerCustomShortcutSet(
    KeyGroup.toShortcutSet(shortcuts),
    (ProjectView.getInstance(project) as ProjectViewImpl).component,
  )
}
