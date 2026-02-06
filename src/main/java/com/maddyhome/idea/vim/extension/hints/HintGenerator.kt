/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.hints

import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.treeStructure.Tree
import java.awt.Component
import java.awt.Point
import java.awt.Rectangle
import java.util.*
import javax.accessibility.Accessible
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities
import javax.swing.text.JTextComponent

internal sealed class HintGenerator {
  private var hints: Map<Accessible, String> = emptyMap()
  protected val previousHints get() = hints

  abstract fun generate(targets: List<HintTarget>)

  fun <T> generate(root: T, glassPane: Component): List<HintTarget> where T : Accessible, T : Component =
    collectTargets(root, glassPane).also { targets ->
      generate(targets)
      hints = WeakHashMap(targets.associateBy(HintTarget::component, HintTarget::hint).filterValues(String::isNotEmpty))
    }

  class Permutation(private val alphabet: List<Char>) : HintGenerator() {
    init {
      require(alphabet.size > 1) { "Alphabet must contain at least two characters" }
    }

    override fun generate(targets: List<HintTarget>) = generate(targets, true)

    /**
     * @param preserve Whether to preserve the previous hints if possible
     */
    private fun generate(targets: List<HintTarget>, preserve: Boolean) {
      val length = generateSequence(1) { it * alphabet.size }.takeWhile {
        it < targets.size + if (preserve) previousHints.size else 0
      }.count()
      val hintIterator = alphabet.permutations(length).map { it.joinToString("") }.iterator()
      targets.forEach { target ->
        target.hint = if (preserve) {
          previousHints[target.component] ?: hintIterator.firstOrNull { candidateHint ->
            // Check if the hint is not already used by previous targets
            !previousHints.values.any { existingHint ->
              existingHint.startsWith(candidateHint) || candidateHint.startsWith(
                existingHint
              )
            }
          } ?: return generate(targets, false) // do not preserve previous hints if failed
        } else {
          hintIterator.next()
        }
      }
    }
  }
}

private fun <T> collectTargets(
  component: T,
  destination: Component,
): List<HintTarget> where T : Accessible, T : Component = mutableMapOf<Accessible, HintTarget>().also {
  collectTargets(it, component, SwingUtilities.convertPoint(component.parent, component.location, destination))
}.values.toList()

private fun collectTargets(
  targets: MutableMap<Accessible, HintTarget>,
  component: Accessible,
  location: Point,
  depth: Int = 0,
): Unit = with(component.accessibleContext) {
  val accessible = accessibleComponent ?: return
  val location = location + (accessible.location ?: return)

  accessible.size?.let { size ->
    // TextPanel (status bar widgets) may report incorrect visibility until hovered, so skip visibility check for them
    val isTextPanel = component is TextPanel || component is JBTextField
    val isTextComponent = component is JTextComponent
    val isEditorScrollPane = component is JScrollPane && component.viewport?.view is EditorComponentImpl
    val isVisible = isTextPanel || (accessible.isVisible && (component as? Component)?.isActuallyVisible() != false)
    val isInteractive =
      component.isClickable() || component is Tree || isTextPanel || isTextComponent || isEditorScrollPane

    if (isVisible && isInteractive) {
      targets[component].let {
        // For some reason, the same component may appear multiple times in the accessible tree.
        if (it == null || it.depth > depth) {
          targets[component] = HintTarget(component, location, size, depth).apply {
            action = when {
              isEditorScrollPane -> ({ component.viewport?.view?.requestFocusInWindow() ?: false })
              component is Tree || component is EditorComponentImpl -> ({ (component as Component).requestFocusInWindow() })
              component is JTextComponent -> ({ (component as Component).requestFocusInWindow() })
              else -> HintTarget::clickCenter
            }
            if (isEditorScrollPane) {
              labelPosition = HintLabelPosition.CENTER
            }
          }
        }
      }
    }
  }

  // Handle JTabbedPane tabs specially - create a target for each tab
  if (component is JTabbedPane) {
    for (tabIndex in 0 until component.tabCount) {
      val tabBounds = component.getBoundsAt(tabIndex) ?: continue
      val tabLocation = Point(location.x + tabBounds.x, location.y + tabBounds.y)
      val tabSize = tabBounds.size
      // Use a unique key for each tab by combining tabbedPane and index
      val tabKey = TabAccessible(component, tabIndex)
      targets[tabKey] = HintTarget(tabKey, tabLocation, tabSize, depth + 1).apply {
        action = {
          component.selectedIndex = tabIndex
          true
        }
      }
    }
  }

  // Skip the children of the Tree, otherwise it will easily lead to performance problems
  if (component is Tree) return
  // recursively collect children
  for (i in 0..<accessibleChildrenCount) {
    getAccessibleChild(i)?.let {
      collectTargets(targets, it, location, depth + 1)
    }
  }
}

/**
 * Wrapper to make each tab in a JTabbedPane a unique Accessible key
 */
private class TabAccessible(val tabbedPane: JTabbedPane, val tabIndex: Int) : Accessible {
  override fun getAccessibleContext(): javax.accessibility.AccessibleContext = tabbedPane.accessibleContext

  override fun equals(other: Any?): Boolean {
    if (other !is TabAccessible) return false
    return tabbedPane === other.tabbedPane && tabIndex == other.tabIndex
  }

  override fun hashCode(): Int = System.identityHashCode(tabbedPane) * 31 + tabIndex
}

/**
 * Check if the component is clickable
 *
 * @return whether the component is clickable
 */
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
