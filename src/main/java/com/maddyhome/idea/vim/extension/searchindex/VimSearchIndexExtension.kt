/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.searchindex

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.util.Consumer
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.newapi.IjVimSearchGroup
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import java.awt.Component
import java.awt.event.MouseEvent

internal object SearchIndex {
  internal const val ID = "IdeaVimSearchIndex"
  internal const val DISPLAY_NAME = "IdeaVim searchindex"
  private var currentText = ""

  fun update(text: String) {
    currentText = text
    val windowManager = WindowManager.getInstance()
    ProjectManager.getInstance().openProjects.forEach {
      val statusBar = windowManager.getStatusBar(it)
      statusBar.updateWidget(ID)
    }
  }

  fun getCurrentText(): String = currentText
}

internal class SearchIndexWidgetFactory : StatusBarWidgetFactory {
  override fun getId() = SearchIndex.ID
  override fun getDisplayName() = SearchIndex.DISPLAY_NAME
  override fun createWidget(project: Project) = SearchIndexWidget(project)
  override fun disposeWidget(widget: StatusBarWidget) {}
  override fun canBeEnabledOn(statusBar: StatusBar) = true
  override fun isAvailable(project: Project) = true
}

internal class SearchIndexWidget(project: Project) :
  EditorBasedWidget(project),
  StatusBarWidget.TextPresentation {

  override fun ID() = SearchIndex.ID
  override fun getPresentation() = this
  override fun getClickConsumer(): Consumer<MouseEvent>? = null
  override fun getText() = SearchIndex.getCurrentText()
  override fun getTooltipText() = null
  override fun getAlignment() = Component.RIGHT_ALIGNMENT
}

internal class VimSearchIndexExtension : VimExtension, ModeChangeListener {
  init {
    LOG.info("Looking at SearchGroup implementation: ${VimPlugin.getSearch().javaClass}")
  }

  companion object {
    private val LOG = logger<VimSearchIndexExtension>()
  }

  override fun getName(): String = "searchindex"

  override fun init() {
    LOG.info("Initializing SearchIndex extension")
    injector.listenersNotifier.modeChangeListeners.add(this)
  }

  override fun dispose() {
    LOG.info("Disposing SearchIndex extension")
    injector.listenersNotifier.modeChangeListeners.remove(this)
  }

  // TODO: should support * , #
  override fun modeChanged(editor: VimEditor, oldMode: Mode) {
    LOG.info("Mode changed from $oldMode to ${editor.mode}")

    if (oldMode is Mode.CMD_LINE && editor.mode !is Mode.CMD_LINE) {
      val label = ExEntryPanel.getInstance().label
      LOG.info("search label: $label")

      if (label == "/" || label == "?") {
        ApplicationManager.getApplication().invokeLater {
          updateSearchIndex(editor = editor.ij, searchGroup = VimPlugin.getSearch())
        }
      }
    }
  }

  private fun updateSearchIndex(editor: Editor, searchGroup: IjVimSearchGroup) {
    if (searchGroup.lastSearchPattern.isNullOrEmpty()) {
      return
    }
    val pattern = searchGroup.lastSearchPattern
    if (pattern.isNullOrEmpty()) {
      return
    }

    LOG.info("search index - Processing pattern: $pattern")

    // FIXME: global search option -> previous search option
    val ignoreCase = injector.options(editor = editor.vim).ignorecase
    LOG.info("Ignore case: $ignoreCase")

    val results = injector.searchHelper.findAll(
      editor = editor.vim,
      pattern = pattern,
      startLine = 0,
      endLine = -1,
      ignoreCase = ignoreCase,
    )

    val label = ExEntryPanel.getInstance().label
    val (currentIndex: Int, resultSize: Int) = if (results.isNotEmpty()) {
      Pair(first = getCurrentIndex(editor = editor, results = results), second = results.size)
    } else {
      Pair(first = 0, second = 0)
    }

    ApplicationManager.getApplication().invokeLater {
      LOG.info("searchindex - [${currentIndex}/${resultSize}] $label$pattern")
      SearchIndex.update("[${currentIndex}/${resultSize}] $label$pattern")
    }
  }

  private fun getCurrentIndex(
    editor: Editor,
    results: List<TextRange>,
  ): Int {
    val caretOffset = editor.caretModel.offset
    var current = 0
    results.forEachIndexed { index, range ->
      if (range.startOffset <= caretOffset && caretOffset <= range.endOffset) {
        current = index + 1
      }
    }
    return current
  }
}
