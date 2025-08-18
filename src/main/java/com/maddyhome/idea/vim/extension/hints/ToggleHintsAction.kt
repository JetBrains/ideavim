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
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import com.intellij.ui.JBColor
import com.intellij.ui.treeStructure.Tree
import java.awt.Color
import java.awt.Component
import java.awt.Point
import javax.accessibility.Accessible
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRootPane
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

  private var hints: List<Hint> = emptyList()

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

    if (selected) { // enable
      val frame = WindowManager.getInstance().findVisibleFrame()!!
      val rootPane = frame.rootPane
      val glassPane = frame.glassPane as IdeGlassPaneImpl

      updateCovers(rootPane, glassPane)
      if (cover !in glassPane.components) glassPane.add(cover)
      glassPane.isVisible = true
      cover.isVisible = true
      val popup =
        JBPopupFactory.getInstance().createListPopup(object : BaseListPopupStep<Hint>("Type to Filter", hints) {
          override fun isSpeedSearchEnabled(): Boolean = true
          override fun onChosen(selectedValue: Hint?, finalChoice: Boolean): PopupStep<*>? {
            selectedValue?.component?.accessibleContext?.accessibleAction?.doAccessibleAction(0)
            return FINAL_CHOICE
          }
        })
      popup.addListener(object : JBPopupListener {
        override fun onClosed(event: LightweightWindowEvent) {
          setSelected(false)
        }
      })
      popup.showInCenterOf(rootPane)
    } else { // disable
      cover.isVisible = false
    }
  }

  /**
   * Update cover panels in the container
   *
   * @param rootComponent root component
   * @param glassPane glass pane for positioning
   */
  private fun updateCovers(rootComponent: JRootPane, glassPane: Component) {
    cover.removeAll() // clear existing covers
    hints =
      rootComponent.createCovers(SwingUtilities.convertPoint(rootComponent.parent, rootComponent.location, glassPane))
    hints.map(Hint::cover).forEach(cover::add)
    cover.size = glassPane.size
  }
}

/**
 * Check if the component is clickable
 *
 * @return whether the component is clickable
 */
private fun Accessible.isClickable(): Boolean = (accessibleContext.accessibleAction?.accessibleActionCount ?: 0) > 0

private class Hint(val component: Accessible, loc: Point) {
  val label: String?
    get() = component.accessibleContext?.accessibleName

  val bounds: Rectangle = Rectangle(loc, component.accessibleContext.accessibleComponent.size)

  val cover = JPanel().apply {
    // transparent background
    background = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))
    // same bounds (location and size) as the original component
    bounds = this@Hint.bounds
    // green border
    border = javax.swing.border.LineBorder(JBColor.GREEN, 2)
    if (ApplicationManager.getApplication().isInternal) {
      if (label != null) {
        // add a label
        add(JLabel().apply {
          text = this@Hint.label
          foreground = JBColor.RED
        })
      }
    }
    // tree structure
    if (component is Tree) {
      // red border
      border = javax.swing.border.LineBorder(JBColor.RED, 2)
    }
    // visible
    isVisible = true
  }

  override fun toString(): String = label ?: "<not labelled>"
}

/**
 * Create covers for the component and its children recursively
 *
 * @param this the ancestor of components to be highlighted
 * @return list of cover panels
 */
private fun Accessible.createCovers(loc: Point): List<Hint> = if (accessibleContext.accessibleComponent.isShowing) {
  val hints = mutableListOf<Hint>()
  val location = loc + accessibleContext.accessibleComponent.location
  // recursively create covers for children
  hints.addAll((0..<accessibleContext.accessibleChildrenCount).flatMap {
    accessibleContext.getAccessibleChild(it).createCovers(location)
  })
  if (this.isClickable() || this is Tree) {
    hints.add(Hint(this, location))
  }
  hints
} else emptyList()

private operator fun Point.plus(other: Point) = Point(x + other.x, y + other.y)
