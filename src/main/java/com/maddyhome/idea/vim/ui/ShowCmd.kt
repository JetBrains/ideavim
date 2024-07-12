/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.util.Consumer
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.EditorListener
import com.maddyhome.idea.vim.helper.EngineStringHelper
import com.maddyhome.idea.vim.helper.VimNlsSafe
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.initInjector
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import org.jetbrains.annotations.NonNls
import java.awt.Component
import java.awt.event.MouseEvent

internal object ShowCmd {
  // https://github.com/vim/vim/blob/b376ace1aeaa7614debc725487d75c8f756dd773/src/vim.h#L1721
  private const val SHOWCMD_COLS = 10

  @NonNls
  internal const val ID = "IdeaVimShowCmd"

  @NlsSafe
  internal const val displayName = "IdeaVim showcmd"

  fun update() {
    val windowManager = WindowManager.getInstance()
    ProjectManager.getInstance().openProjects.forEach {
      val statusBar = windowManager.getStatusBar(it)
      statusBar.updateWidget(ID)
    }
  }

  fun getWidgetText(editor: Editor?): String {
    // Vim only shows the last 10 characters. See normal.c:add_to_showcmd
    // https://github.com/vim/vim/blob/b376ace1aeaa7614debc725487d75c8f756dd773/src/normal.c#L1885-L1890
    return getFullText(editor).takeLast(SHOWCMD_COLS)
  }

  fun getFullText(editor: Editor?): String {
    if (!injector.globalOptions().showcmd || editor == null || editor.isDisposed) return ""

    val keyState = KeyHandler.getInstance().keyHandlerState
    return EngineStringHelper.toPrintableCharacters(keyState.commandBuilder.keys + keyState.mappingState.keys)
  }
}

internal object ShowCmdOptionChangeListener : GlobalOptionChangeListener {
  override fun onGlobalOptionChanged() {
    ShowCmd.update()
  }
}

internal class ShowCmdStatusBarWidgetFactory : StatusBarWidgetFactory/*, LightEditCompatible*/ {

  init {
    initInjector()
  }

  override fun getId() = ShowCmd.ID

  override fun getDisplayName(): String = ShowCmd.displayName

  override fun disposeWidget(widget: StatusBarWidget) {
    // Nothing
  }

  override fun isAvailable(project: Project): Boolean {
    return injector.globalOptions().showcmd
  }

  override fun createWidget(project: Project): StatusBarWidget = Widget(project)

  override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

  // Should be configured via `set showcmd`
  override fun isConfigurable(): Boolean = false
}

// `:help 'showcmd'`
// Widget shows:
// * Partial command, as it's being typed
// * When selecting characters within a line, the number of characters
//   * Tabs are shown as one char
//   * If the number of bytes is different, this is also shown: "2-6"
// * When selecting more than one line, the number of lines
// * When selecting a block, the size in screen characters: {lines}x{columns}
//
// We only need to show partial commands, since the standard PositionPanel shows the other information already, with
// the exception of "{lines}x{columns}" (it shows "x carets" instead)
internal class Widget(project: Project) :
  EditorBasedWidget(project),
  StatusBarWidget.Multiframe,
  StatusBarWidget.TextPresentation,
  FileEditorManagerListener {

  override fun ID() = ShowCmd.ID

  override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

  override fun getClickConsumer(): Consumer<MouseEvent>? = null

  @VimNlsSafe
  override fun getTooltipText(): String {
    var count = ShowCmd.getFullText(getEditor())
    if (count.isNotBlank()) count = ": $count"
    return "${ShowCmd.displayName}$count"
  }

  override fun getText(): String = ShowCmd.getWidgetText(getEditor())

  override fun getAlignment() = Component.CENTER_ALIGNMENT

  // Multiframe#copy to show the widget on popped out editors
  override fun copy(): StatusBarWidget = Widget(project)
}

internal class ShowCmdWidgetUpdater : EditorListener {
  override fun focusGained(editor: VimEditor) {
    editor.ij.project?.let { selectionChanged(it) }
  }

  override fun focusLost(editor: VimEditor) {
    editor.ij.project?.let { selectionChanged(it) }
  }

  private fun selectionChanged(project: Project) {
    val windowManager = WindowManager.getInstance()
    val statusBar = windowManager.getStatusBar(project)
    statusBar.updateWidget(ShowCmd.ID)
  }
}
