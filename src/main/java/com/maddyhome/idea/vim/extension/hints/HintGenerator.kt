/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.hints

import com.intellij.openapi.wm.impl.content.ContentTabLabel
import com.intellij.openapi.wm.impl.status.IdeStatusBarImpl
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.treeStructure.Tree
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.util.*
import javax.accessibility.Accessible
import javax.accessibility.AccessibleComponent
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities
import javax.swing.text.JTextComponent
import kotlin.math.max

internal class HintGenerator(private val alphabet: List<Char>) {
  init {
    require(alphabet.size > 1) { "Alphabet must contain at least two characters" }
  }

  private var hints: Map<Accessible, String> = emptyMap()

  fun generateHints(targets: List<HintTarget>) = generatePreserving(targets)

  fun <T> generateHints(root: T, glassPane: Component): List<HintTarget> where T : Accessible, T : Component {
    val targets = collectTargets(root, glassPane)
    generateHints(targets)
    hints = WeakHashMap(targets.associateBy(HintTarget::component, HintTarget::hint).filterValues(String::isNotEmpty))
    return targets
  }

  private fun generatePreserving(targets: List<HintTarget>) {
    val length = computeHintLength(targets.size + hints.size)
    val hintIterator = alphabet.permutations(length).map { it.joinToString("") }.iterator()
    for (target in targets) {
      target.hint = resolveHint(target, hintIterator) ?: return generateFresh(targets)
    }
  }

  private fun generateFresh(targets: List<HintTarget>) {
    val length = computeHintLength(targets.size)
    val hintIterator = alphabet.permutations(length).map { it.joinToString("") }.iterator()
    for (target in targets) {
      target.hint = hintIterator.next()
    }
  }

  private fun resolveHint(target: HintTarget, hintIterator: Iterator<String>): String? {
    val preserved = hints[target.component]
    if (preserved != null) return preserved
    return hintIterator.firstOrNull { !conflictsWithExisting(it) }
  }

  private fun conflictsWithExisting(candidate: String): Boolean =
    hints.values.any { it.startsWith(candidate) || candidate.startsWith(it) }

  private fun computeHintLength(targetCount: Int): Int =
    max(generateSequence(1) { it * alphabet.size }.takeWhile { it < targetCount }.count(), 1)

  private fun <T> collectTargets(root: T, glassPane: Component): List<HintTarget>
    where T : Accessible, T : Component {
    val targets = mutableMapOf<Accessible, HintTarget>()
    val startLocation = SwingUtilities.convertPoint(root.parent, root.location, glassPane)
    collectTargets(targets, root, startLocation)
    return targets.values.toList()
  }

  private fun collectTargets(
    targets: MutableMap<Accessible, HintTarget>,
    component: Accessible,
    location: Point,
    depth: Int = 0,
    insideStatusBar: Boolean = false,
  ) {
    val context = component.accessibleContext
    val accessible = context.accessibleComponent ?: return
    val currentLocation = location + (accessible.location ?: return)
    val size = accessible.size ?: return

    if (isVisible(component, accessible) && isInteractive(component)) {
      addTarget(targets, component, currentLocation, size, depth)
    }

    if (component is JTabbedPane) {
      collectTabTargets(targets, component, currentLocation, depth)
    }

    /*
      We are skipping Tree and scroll panes inside status bar as they would be a performance problem.
      Exception is when we are inside the status bar where we want to apply hints on individual components.
     */
    if (component is Tree || (!insideStatusBar && isScrollPane(component))) return

    val childInsideStatusBar = insideStatusBar || component is IdeStatusBarImpl
    for (i in 0..<context.accessibleChildrenCount) {
      val child = context.getAccessibleChild(i) ?: continue
      collectTargets(targets, child, currentLocation, depth + 1, childInsideStatusBar)
    }
  }

  private fun addTarget(
    targets: MutableMap<Accessible, HintTarget>,
    component: Accessible,
    location: Point,
    size: Dimension,
    depth: Int,
  ) {
    val existing = targets[component]
    if (existing != null && existing.depth <= depth) return
    val target = HintTarget(component, location, size, depth)
    target.action = resolveAction(component, isScrollPane(component))
    if (isScrollPane(component) || component is Tree) target.labelPosition = HintLabelPosition.CENTER
    targets[component] = target
  }

  private fun collectTabTargets(
    targets: MutableMap<Accessible, HintTarget>,
    tabbedPane: JTabbedPane,
    location: Point,
    depth: Int,
  ) {
    for (tabIndex in 0 until tabbedPane.tabCount) {
      val tabBounds = tabbedPane.getBoundsAt(tabIndex) ?: continue
      val tabLocation = Point(location.x + tabBounds.x, location.y + tabBounds.y)
      val tabKey = TabAccessible(tabbedPane, tabIndex)
      val target = HintTarget(tabKey, tabLocation, tabBounds.size, depth + 1)
      target.action = { tabbedPane.selectedIndex = tabIndex; true }
      targets[tabKey] = target
    }
  }

  private fun resolveAction(component: Accessible, editorScrollPane: Boolean): (HintTarget) -> Boolean = when {
    editorScrollPane -> { _ -> (component as JScrollPane).viewport?.view?.requestFocusInWindow() ?: false }
    component is Tree -> { _ -> (component as Component).requestFocusInWindow() }
    component is JTextComponent -> { _ -> (component as Component).requestFocusInWindow() }
    else -> HintTarget::clickCenter
  }

  private fun isScrollPane(component: Accessible): Boolean =
    component is JScrollPane


  private fun isTextPanel(component: Accessible): Boolean =
    component is TextPanel || component is JBTextField

  private fun isVisible(component: Accessible, accessible: AccessibleComponent): Boolean {
    if (isTextPanel(component)) return true
    if (!accessible.isVisible) return false
    return (component as? Component)?.isActuallyVisible() != false
  }

  private fun isInteractive(component: Accessible): Boolean =
    component.isClickable() ||
      component is ContentTabLabel ||
      component is Tree ||
      isTextPanel(component) ||
      component is JTextComponent ||
      isScrollPane(component)
}

private class TabAccessible(val tabbedPane: JTabbedPane, val tabIndex: Int) : Accessible {
  override fun getAccessibleContext(): javax.accessibility.AccessibleContext = tabbedPane.accessibleContext

  override fun equals(other: Any?): Boolean {
    if (other !is TabAccessible) return false
    return tabbedPane === other.tabbedPane && tabIndex == other.tabIndex
  }

  override fun hashCode(): Int = System.identityHashCode(tabbedPane) * 31 + tabIndex
}

private fun Accessible.isClickable(): Boolean = (accessibleContext.accessibleAction?.accessibleActionCount ?: 0) > 0

private fun Component.isActuallyVisible(): Boolean {
  val visibleRect = Rectangle()
  (this as? JComponent)?.computeVisibleRect(visibleRect)
  return !visibleRect.isEmpty
}

private operator fun Point.plus(other: Point) = Point(x + other.x, y + other.y)

private fun <T> Collection<T>.permutations(length: Int): Sequence<List<T>> = sequence {
  if (length == 0) {
    yield(emptyList())
    return@sequence
  }
  for (element in this@permutations) {
    this@permutations.permutations(length - 1).forEach { subPermutation ->
      yield(listOf(element) + subPermutation)
    }
  }
}

private fun <T> Iterator<T>.firstOrNull(predicate: (T) -> Boolean): T? {
  while (hasNext()) {
    val next = next()
    if (predicate(next)) return next
  }
  return null
}
