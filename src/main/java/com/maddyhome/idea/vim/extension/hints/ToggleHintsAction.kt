/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.hints

import com.intellij.openapi.actionSystem.ToggleOptionAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import com.intellij.openapi.wm.impl.ToolbarComboButton
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.ui.InplaceButton
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.treeStructure.Tree
import java.awt.Color
import java.awt.Component
import java.awt.Container
import javax.swing.AbstractButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingUtilities

class ToggleHintsAction : ToggleOptionAction(object : Option {
  private var enabled = false

  /** The mask layer container for placing all hints */
  private val cover = JPanel().apply {
    layout = null // no layout manager (absolute positioning)
    background = JBColor(0x1F000000, 0x1F000000)
  }

  override fun isSelected(): Boolean = enabled

  override fun setSelected(selected: Boolean) {
    if (selected) { // enable
      val frame = WindowManager.getInstance().findVisibleFrame() ?: return
      val rootPane = frame.rootPane
      val glassPane = frame.glassPane as IdeGlassPaneImpl

      updateCovers(rootPane, glassPane)
      if (cover !in glassPane.components) glassPane.add(cover)
      glassPane.isVisible = true
      cover.isVisible = true
    } else { // disable
      cover.isVisible = false
    }
    this.enabled = selected
  }

  private fun updateCovers(rootComponent: Component, glassPane: Component) {
    cover.removeAll() // clear existing covers
    rootComponent.createCovers(glassPane).forEach(cover::add)
    cover.size = glassPane.size
  }
})

private fun Component.isClickable(): Boolean = when (this) {
  is AbstractButton, is ActionButton, is InplaceButton, is ToolbarComboButton, // buttons
  is JTextField, is JTextArea, // text inputs
  is LinkLabel<*>, // clickable links
  is TextPanel, is SimpleColoredComponent, // in IDE status bar
    -> isEnabled // exclude disabled components
  else -> false
}

private fun Component.createCover(glassPane: Component) = JPanel().apply {
  background = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))
  bounds = SwingUtilities.convertRectangle(this@createCover.parent, this@createCover.bounds, glassPane)
  border = javax.swing.border.LineBorder(JBColor.GREEN, 2)
  if (this@createCover is Tree) {
    border = javax.swing.border.LineBorder(JBColor.RED, 2)
    add(JLabel().apply {
      text = "TREE"
      foreground = JBColor.RED
    })
  }
  isVisible = true
}

/**
 * Create covers for the component and its children recursively
 *
 * @param this the ancestor of components to be highlighted
 * @return list of cover panels
 */
private fun Component.createCovers(glassPane: Component): List<JPanel> = if (isVisible) {
  val hints = mutableListOf<JPanel>()
  // recursively create covers for children
  if (this is Container) hints += components.flatMap { it.createCovers(glassPane) }
  if (this.isClickable() || this is Tree) hints.add(this.createCover(glassPane))
  hints
} else emptyList()
