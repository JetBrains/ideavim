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

/**
 * Toggle hints action
 */
class ToggleHintsAction : ToggleOptionAction(object : Option {
  /** Whether the hints are enabled */
  private var enabled = false

  /** Cover panel container */
  private val cover: JPanel = JPanel().apply {
    // no layout manager (absolute positioning)
    layout = null
    // semi-transparent background
    background = JBColor(0x1F000000, 0x1F000000)
  }

  /**
   * @return current enablement state
   */
  override fun isSelected(): Boolean = enabled

  /**
   * @param selected new enablement state
   */
  override fun setSelected(selected: Boolean) {
    this.enabled = selected

    val frame = WindowManager.getInstance().findVisibleFrame()!!
    val rootPane = frame.rootPane
    val glassPane = frame.glassPane as IdeGlassPaneImpl

    if (selected) { // enable
      updateCovers(rootPane, glassPane)
      if (cover !in glassPane.components) glassPane.add(cover)
      glassPane.isVisible = true
      cover.isVisible = true
    } else cover.isVisible = false // disable
  }

  /**
   * Update cover panels in the container
   *
   * @param rootComponent root component
   * @param glassPane glass pane for positioning
   */
  private fun updateCovers(rootComponent: Component, glassPane: Component) {
    cover.removeAll() // clear existing covers
    rootComponent.createCovers(glassPane).forEach(cover::add)
    cover.size = glassPane.size
  }
})

/**
 * Check if the component is clickable
 *
 * @return whether the component is clickable
 */
private fun Component.isClickable(): Boolean = when (this) {
  is AbstractButton, is ActionButton, is InplaceButton, is ToolbarComboButton, // buttons
  is JTextField, is JTextArea, // text inputs
  is LinkLabel<*>, // clickable links
  is TextPanel, is SimpleColoredComponent, // in IDE status bar
    -> isEnabled // exclude disabled components
  else -> false
}

private fun Component.createCover(glassPane: Component) = JPanel().apply {
  // transparent background
  background = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))
  // same bounds (location and size) as the original component
  bounds = SwingUtilities.convertRectangle(this@createCover.parent, this@createCover.bounds, glassPane)
  // green border
  border = javax.swing.border.LineBorder(JBColor.GREEN, 2)
  // tree structure
  if (this@createCover is Tree) {
    // red border
    border = javax.swing.border.LineBorder(JBColor.RED, 2)
    // add a label
    add(JLabel().apply {
      text = "TREE"
      foreground = JBColor.RED
    })
  }
  // visible
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
