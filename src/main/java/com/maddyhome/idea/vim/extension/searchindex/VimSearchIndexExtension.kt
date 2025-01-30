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
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import java.awt.Component
import java.awt.event.MouseEvent
import java.util.regex.Pattern

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

  override fun modeChanged(editor: VimEditor, oldMode: Mode) {
    LOG.info("Mode changed from $oldMode to ${editor.mode}")

    if (oldMode is Mode.CMD_LINE && editor.mode !is Mode.CMD_LINE) {
      val label = ExEntryPanel.getInstance().label
      LOG.info("search label: $label")

      if (label == "/" || label == "?") {
        ApplicationManager.getApplication().invokeLater {
          val searchGroup = VimPlugin.getSearch()
          val pattern = searchGroup.lastSearchPattern
          LOG.info("Last search pattern: $pattern")

          if (!pattern.isNullOrEmpty()) {
            updateSearchIndex(editor.ij)
          }
        }
      }
    }
  }

  private fun updateSearchIndex(editor: Editor) {
    LOG.info("Updating search index")

    val searchGroup = VimPlugin.getSearch()
    val pattern = searchGroup.lastSearchPattern ?: return
    if (pattern.isEmpty()) return

    LOG.info("Processing pattern: $pattern")

    val document = editor.document
    val text = document.text
    val caretOffset = editor.caretModel.offset

    val compiledPattern = try {
      val ignoreCase = true
      LOG.info("Ignore case: $ignoreCase")
      val flags = if (ignoreCase) Pattern.CASE_INSENSITIVE else 0
      Pattern.compile(pattern, flags)
    } catch (e: Exception) {
      LOG.error("Failed to compile pattern: $pattern", e)
      return
    }

    val matcher = compiledPattern.matcher(text)
    var total = 0
    var current = 0

    while (matcher.find()) {
      total++
      if (matcher.start() <= caretOffset && caretOffset <= matcher.end()) {
        current = total
      }
    }
    val label = ExEntryPanel.getInstance().label
    if (total > 0) {
      LOG.info("Found $total matches, current position: $current")
      ApplicationManager.getApplication().invokeLater {
        SearchIndex.update("[${current}/${total}] $label$pattern")
      }
    } else {
      LOG.info("No matches found")
      ApplicationManager.getApplication().invokeLater {
        SearchIndex.update("[0/0] $label$pattern")
      }
    }
  }
}
