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
import java.awt.Component
import java.awt.Point
import java.awt.Rectangle
import java.util.*
import javax.accessibility.Accessible
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRootPane
import javax.swing.SwingUtilities

class ToggleHintsAction : DumbAwareToggleAction() {
  private var enabled = false

  /** The mask layer container for placing all hints */
  private val cover = JPanel().apply {
    layout = null // no layout manager (absolute positioning)
    isOpaque = false
    isVisible = false
  }

  private val highlight = HighlightComponent()

  private var hints: List<Hint> = emptyList()
  private var cache: Map<Accessible, String> = WeakHashMap()

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
    if (highlight !in glassPane.components) glassPane.add(highlight)
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
        disable()
      }
    })
    popup.addListSelectionListener {
      val current = ((it.source as JBList<*>).selectedValue as? Hint)
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

  private fun updateCovers(rootComponent: JRootPane, glassPane: Component) {
    cover.removeAll() // clear existing covers
    hints =
      rootComponent.createCovers(SwingUtilities.convertPoint(rootComponent.parent, rootComponent.location, glassPane))
    val hintIterator = alphabet.permutations(2).map { it.joinToString("") }.iterator()
    hints.forEach { hint ->
      hint.hint = cache[hint.component] ?: hintIterator.firstOrNull { it !in cache.values }!!
    }
    hints.map(Hint::cover).forEach(cover::add)
    cache = hints.associateBy({ it.component }, { it.hint })
    cover.size = glassPane.size
  }
}

private fun Accessible.isClickable(): Boolean = (accessibleContext.accessibleAction?.accessibleActionCount ?: 0) > 0

private class Hint(val component: Accessible, loc: Point) {
  val label: String?
    get() = component.accessibleContext?.accessibleName

  val bounds: Rectangle = Rectangle(loc, component.accessibleContext.accessibleComponent.size)

  lateinit var hint: String

  val cover: JPanel by lazy {
    JPanel().apply {
      background = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))
      bounds = this@Hint.bounds
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
  }

  override fun toString(): String = hint
}

private class HighlightComponent : JPanel() {
  init {
    background = JBColor(Color(0, 255, 0, 50), Color(0, 255, 0, 50))
    border = javax.swing.border.LineBorder(JBColor.GREEN, 1)
  }

  fun setTarget(target: Hint?) {
    if (target != null) {
      bounds = target.bounds
      isVisible = true
    } else {
      isVisible = false
    }
  }
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

private fun <T> Collection<T>.permutations(length: Int): Sequence<List<T>> = sequence {
  if (length == 0) {
    yield(emptyList())
    return@sequence
  }
  for (element in this@permutations) {
    (this@permutations - element).permutations(length - 1).forEach { subPermutation ->
      yield(listOf(element) + subPermutation)
    }
  }
}

private val alphabet = "ASDFGHJKL".toList()

private fun <T> Iterator<T>.firstOrNull(predicate: (T) -> Boolean): T? {
  while (hasNext()) {
    val next = next()
    if (predicate(next)) return next
  }
  return null
}
