package com.maddyhome.idea.vim.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetProvider
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.util.Consumer
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.vimCommandState
import com.maddyhome.idea.vim.option.OptionChangeListener
import com.maddyhome.idea.vim.option.OptionsManager
import java.awt.Component
import java.awt.event.MouseEvent

object ShowCmd {
  // https://github.com/vim/vim/blob/b376ace1aeaa7614debc725487d75c8f756dd773/src/vim.h#L1721
  private const val SHOWCMD_COLS = 10

  fun update() {
    val windowManager = WindowManager.getInstance()
    ProjectManager.getInstance().openProjects.forEach {
      val statusBar = windowManager.getStatusBar(it)
      statusBar.updateWidget(ShowCmdStatusBarWidget.ID)
    }
  }

  fun getWidgetText(editor: Editor?): String {
    // Vim only shows the last 10 characters. See normal.c:add_to_showcmd
    // https://github.com/vim/vim/blob/b376ace1aeaa7614debc725487d75c8f756dd773/src/normal.c#L1885-L1890
    return getFullText(editor).takeLast(SHOWCMD_COLS)
  }

  fun getFullText(editor: Editor?): String {
    if (!OptionsManager.showcmd.isSet || editor == null || editor.isDisposed) return ""

    val editorState = editor.vimCommandState ?: return ""
    return StringHelper.toPrintableCharacters(editorState.commandBuilder.keys + editorState.mappingState.keys)
  }
}

object ShowCmdOptionChangeListener: OptionChangeListener<Boolean> {
  override fun valueChange(oldValue: Boolean?, newValue: Boolean?) {
    ShowCmd.update()
  }
}

class ShowCmdStatusBarWidget: StatusBarWidgetProvider {
  companion object {
    const val ID = "IdeaVim::ShowCmd"
  }

  override fun getWidget(project: Project): StatusBarWidget = Widget(project)
  override fun getAnchor(): String = StatusBar.Anchors.before(StatusBar.StandardWidgets.POSITION_PANEL)

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
  class Widget(project: Project)
    : EditorBasedWidget(project), StatusBarWidget.Multiframe, StatusBarWidget.TextPresentation {

    override fun ID() = ID

    // TODO [VERSION UPDATE] After 193 use `getPresentation()`
    @Suppress("UnstableApiUsage", "DEPRECATION")
    override fun getPresentation(type: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation? = this

    override fun getClickConsumer(): Consumer<MouseEvent>? = null
    override fun getTooltipText() = "IdeaVim: " + ShowCmd.getFullText(this.editor)
    override fun getText(): String = ShowCmd.getWidgetText(editor)
    override fun getAlignment() = Component.CENTER_ALIGNMENT

    // Multiframe#copy to show the widget on popped out editors
    override fun copy(): StatusBarWidget = Widget(project)

    override fun selectionChanged(event: FileEditorManagerEvent) {
      // Update when changing selected editor
      myStatusBar?.updateWidget(ID)
    }
  }
}
