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

/**
 * Toggle hints action
 */
class ToggleHintsAction : DumbAwareToggleAction() {
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
  override fun isSelected(e: AnActionEvent): Boolean = enabled

  override fun setSelected(e: AnActionEvent, selected: Boolean) {
    setSelected(selected)
  }

  /**
   * @param selected new enablement state
   */
  private fun setSelected(selected: Boolean) {
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
}

/**
 * Check if the component is clickable
 *
 * @return whether the component is clickable
 */
private fun Component.isClickable(): Boolean = (accessibleContext?.accessibleAction?.accessibleActionCount ?: 0) > 0

private fun Component.createCover(glassPane: Component) = JPanel().apply {
  // transparent background
  background = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))
  // same bounds (location and size) as the original component
  bounds = SwingUtilities.convertRectangle(this@createCover.parent, this@createCover.bounds, glassPane)
  // green border
  border = javax.swing.border.LineBorder(JBColor.GREEN, 2)
  if (ApplicationManager.getApplication().isInternal) {
    val accessibleName = this@createCover.accessibleContext?.accessibleName
    if (accessibleName != null) {
      // add a label
      add(JLabel().apply {
        text = accessibleName
        foreground = JBColor.RED
      })
    }
  }
  // tree structure
  if (this@createCover is Tree) {
    // red border
    border = javax.swing.border.LineBorder(JBColor.RED, 2)
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
