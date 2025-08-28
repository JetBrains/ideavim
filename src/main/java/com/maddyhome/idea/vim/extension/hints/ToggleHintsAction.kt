/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.hints

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.ui.treeStructure.Tree
import java.awt.Color
import javax.swing.JLabel
import javax.swing.JPanel

class ToggleHintsAction : DumbAwareToggleAction() {
  private var enabled = false

  /** The mask layer container for placing all hints */
  private val cover = JPanel().apply {
    layout = null // no layout manager (absolute positioning)
    isOpaque = false
    isVisible = false
  }

  private val highlight = HighlightComponent()

  private val generator = HintGenerator.Permutation(alphabet)

  override fun isSelected(e: AnActionEvent): Boolean = enabled

  override fun setSelected(e: AnActionEvent, selected: Boolean) = if (selected) {
    enable()
  } else {
    disable()
  }

  private fun enable() {
    val frame = WindowManager.getInstance().findVisibleFrame() ?: return
    val rootPane = frame.rootPane
    val glassPane = frame.glassPane as IdeGlassPaneImpl

    val targets = generator.generate(rootPane, glassPane)

    cover.removeAll() // clear existing covers
    targets.map(HintTarget::createCover).forEach(cover::add)
    cover.size = glassPane.size

    if (highlight !in glassPane.components) glassPane.add(highlight)
    if (cover !in glassPane.components) glassPane.add(cover)
    glassPane.isVisible = true
    cover.isVisible = true
    val popup =
      JBPopupFactory.getInstance().createListPopup(object : BaseListPopupStep<HintTarget>("Type to Filter", targets) {
        override fun getTextFor(value: HintTarget) = value.hint
        override fun isSpeedSearchEnabled(): Boolean = true
        override fun onChosen(selectedValue: HintTarget?, finalChoice: Boolean): PopupStep<*>? {
          selectedValue?.component?.accessibleContext?.accessibleAction?.doAccessibleAction(0)
          return FINAL_CHOICE
        }
      })
    popup.addListener(object : JBPopupListener {
      override fun onClosed(event: LightweightWindowEvent) {
        disable()
      }
    })
    popup.addListSelectionListener {
      val current = ((it.source as JBList<*>).selectedValue as? HintTarget)
      highlight.setTarget(current)
    }
    popup.showInCenterOf(rootPane)

    enabled = true
  }

  private fun disable() {
    cover.isVisible = false
    highlight.setTarget(null)

    enabled = false
  }
}

private fun HintTarget.createCover() = JPanel().apply {
  background = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))
  bounds = this@createCover.bounds
  border = javax.swing.border.LineBorder(JBColor.BLUE, 1)
  add(JLabel().apply {
    text = hint
    foreground = JBColor.RED
  })
  if (component is Tree) {
    border = javax.swing.border.LineBorder(JBColor.RED, 1)
  }
  isVisible = true
}

private class HighlightComponent : JPanel() {
  init {
    background = JBColor(Color(0, 255, 0, 50), Color(0, 255, 0, 50))
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

private val alphabet = "ASDFGHJKL".toList()
