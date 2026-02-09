/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.hints

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import com.intellij.ui.JBColor
import com.intellij.util.Alarm
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.ShortcutDispatcher
import com.maddyhome.idea.vim.newapi.globalIjOptions
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JRootPane
import javax.swing.SwingUtilities

class ToggleHintsAction : DumbAwareToggleAction() {
  /** The mask layer container for placing all hints */
  private var cover: JComponent? = null

  private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD)
  private val highlight = HighlightComponent()

  private val generator = HintGenerator(alphabet)

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun isSelected(e: AnActionEvent): Boolean = cover != null

  init {
    // By default, IntelliJ disables actions while a modal dialog is open.
    // We override this so hints can target components inside popups and dialogs (e.g., IdeaVim settings).
    isEnabledInModalContext = true
  }

  override fun setSelected(e: AnActionEvent, selected: Boolean) {
    val rootPane = SwingUtilities.getRootPane(e.getData(PlatformDataKeys.CONTEXT_COMPONENT)) ?: return
    if (!injector.globalIjOptions().vimHints) return
    val glassPane = rootPane.glassPane as IdeGlassPaneImpl
    if (selected) {
      enable(rootPane, glassPane)
    } else {
      disable(glassPane)
    }
  }

  private fun enable(rootPane: JRootPane, glassPane: IdeGlassPaneImpl) {
    val targets = generator.generateHints(rootPane, glassPane)

    val cover = JPanel().apply {
      cover = this
      layout = null // no layout manager (absolute positioning)
      isOpaque = false
      val containerSize = glassPane.size
      for (target in targets) add(target.createCover(containerSize))
      size = containerSize
    }

    if (highlight !in glassPane.components) glassPane.add(highlight)
    if (cover !in glassPane.components) glassPane.add(cover)
    glassPane.isVisible = true
    glassPane.revalidate()
    glassPane.repaint()

    val select = JPanel()
    val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(select, select).createPopup()
    popup.setRequestFocus(true)
    popup.addListener(object : JBPopupListener {
      override fun onClosed(event: LightweightWindowEvent) {
        disable(glassPane)
      }
    })
    ShortcutDispatcher("hints", targets.associateBy { it.hint.lowercase() }, { target ->
      popup.closeOk(null)
      alarm.cancelAllRequests()
      if (target.action()) {
        highlight.setTarget(target)
        alarm.addRequest({ highlight.setTarget(null) }, highlightDuration)
      }
    }, {
      popup.cancel()
      injector.messages.indicateError()
    }, { entries ->
      cover.removeAll()
      for (entry in entries) cover.add(entry.data!!.createCover(cover.size))
      cover.revalidate()
      cover.repaint()
    }).register(select, popup)
    popup.showInCenterOf(rootPane)
  }

  private fun disable(glassPane: IdeGlassPaneImpl) {
    cover?.let(glassPane::remove)
    glassPane.revalidate()
    glassPane.repaint()
    cover = null
  }
}

private class HighlightComponent : JPanel() {
  init {
    background = JBColor.GREEN.let { Color(it.red, it.green, it.blue, 100) }
    border = javax.swing.border.LineBorder(JBColor.GREEN, 1)
  }

  fun setTarget(target: HintTarget?) {
    if (target != null) {
      bounds = target.bounds
      isVisible = true
    } else {
      isVisible = false
    }
  }
}

private const val highlightDuration = 500

private val alphabet = "ASDFGHJKL".toList()
