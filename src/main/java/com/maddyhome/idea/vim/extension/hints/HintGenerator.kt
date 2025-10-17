/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.hints

import com.intellij.ui.treeStructure.Tree
import java.awt.Component
import java.awt.Point
import java.util.WeakHashMap
import javax.accessibility.Accessible
import javax.swing.SwingUtilities

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
            !previousHints.values.any { existingHint -> existingHint.startsWith(candidateHint) || candidateHint.startsWith(existingHint) }
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
    if (accessible.isShowing && (component.isClickable() || component is Tree)) {
      targets[component].let {
        // For some reason, the same component may appear multiple times in the accessible tree.
        if (it == null || it.depth > depth) {
          targets[component] = HintTarget(component, location, size, depth)
        }
      }
    }
  }

  // Skip the children of the Tree, otherwise it will easily lead to performance problems
  if (component is Tree) return
  // recursively collect children
  for (i in 0..<accessibleChildrenCount) {
    collectTargets(targets, getAccessibleChild(i), location, depth + 1)
  }
}

/**
 * Check if the component is clickable
 *
 * @return whether the component is clickable
 */
private fun Accessible.isClickable(): Boolean = (accessibleContext.accessibleAction?.accessibleActionCount ?: 0) > 0

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
