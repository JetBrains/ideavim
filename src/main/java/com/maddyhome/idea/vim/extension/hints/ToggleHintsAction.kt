/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.hints

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import com.intellij.ui.JBColor
import com.intellij.ui.treeStructure.Tree
import java.awt.Color
import java.awt.Component
import java.awt.Container
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class ToggleHintsAction : DumbAwareToggleAction() {
  private var enabled = false

  /** The mask layer container for placing all hints */
  private val cover = JPanel().apply {
    layout = null // no layout manager (absolute positioning)
    background = JBColor(0x1F000000, 0x1F000000)
  }

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

    updateCovers(rootPane, glassPane)
    if (cover !in glassPane.components) glassPane.add(cover)
    glassPane.isVisible = true
    cover.isVisible = true

    enabled = true
  }

  private fun disable() {
    cover.isVisible = false

    enabled = false
  }

  private fun updateCovers(rootComponent: Component, glassPane: Component) {
    cover.removeAll() // clear existing covers
    rootComponent.createCovers(glassPane).forEach(cover::add)
    cover.size = glassPane.size
  }
}

private fun Component.isClickable(): Boolean = (accessibleContext?.accessibleAction?.accessibleActionCount ?: 0) > 0

private class Hint(val component: Component, glassPane: Component) : JPanel() {
  val label: String?
    get() = component.accessibleContext?.accessibleName

  init {
    background = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))
    bounds = SwingUtilities.convertRectangle(component.parent, component.bounds, glassPane)
    border = javax.swing.border.LineBorder(JBColor.GREEN, 2)
    if (ApplicationManager.getApplication().isInternal) {
      if (label != null) {
        add(JLabel().apply {
          text = this@Hint.label
          foreground = JBColor.RED
        })
      }
    }
    if (component is Tree) {
      border = javax.swing.border.LineBorder(JBColor.RED, 2)
    }
    isVisible = true
  }
}

/**
 * Create covers for the component and its children recursively
 *
 * @param this the ancestor of components to be highlighted
 * @return list of cover panels
 */
private fun Component.createCovers(glassPane: Component): List<Hint> = if (isVisible) {
  val hints = mutableListOf<Hint>()
  // recursively create covers for children
  if (this is Container) hints += components.flatMap { it.createCovers(glassPane) }
  if (this.isClickable() || this is Tree) hints.add(Hint(this, glassPane))
  hints
} else emptyList()
