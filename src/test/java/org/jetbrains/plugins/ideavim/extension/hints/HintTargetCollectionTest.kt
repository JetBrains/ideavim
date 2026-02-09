/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.hints

import com.maddyhome.idea.vim.extension.hints.HintGenerator
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import java.awt.GraphicsEnvironment
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTabbedPane
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HintTargetCollectionTest : VimTestCase() {

  private val defaultAlphabet = "ASDFGHJKL".toList()

  @Test
  fun `test clickable components are collected as targets`() {
    val button1 = JButton("Click Me").apply { setBounds(10, 10, 100, 30) }
    val button2 = JButton("Press Me").apply { setBounds(10, 50, 100, 30) }
    withTestFrame(button1, button2) { frame ->
      val generator = HintGenerator(defaultAlphabet)
      val targets = generator.generateHints(frame.rootPane, frame.rootPane.glassPane)

      assertTrue(targets.any { it.component === button1 }, "Button1 should be a target")
      assertTrue(targets.any { it.component === button2 }, "Button2 should be a target")
      assertTrue(targets.all { it.hint.isNotEmpty() }, "All targets should have hints")
    }
  }

  @Test
  fun `test non-interactive components are not targets`() {
    val label = JLabel("Just a label").apply { setBounds(10, 10, 100, 30) }
    val panel = JPanel().apply { setBounds(10, 50, 100, 100) }
    withTestFrame(label, panel) { frame ->
      val generator = HintGenerator(defaultAlphabet)
      val targets = generator.generateHints(frame.rootPane, frame.rootPane.glassPane)

      assertFalse(targets.any { it.component === label }, "JLabel should not be a target")
      assertFalse(targets.any { it.component === panel }, "Plain JPanel should not be a target")
    }
  }

  @Test
  fun `test JTabbedPane tabs are collected as individual targets`() {
    val tabbedPane = JTabbedPane().apply {
      setBounds(10, 10, 300, 200)
      addTab("Tab 1", JPanel())
      addTab("Tab 2", JPanel())
      addTab("Tab 3", JPanel())
    }
    withTestFrame(tabbedPane) { frame ->
      val generator = HintGenerator(defaultAlphabet)
      val targets = generator.generateHints(frame.rootPane, frame.rootPane.glassPane)

      // JTabbedPane creates individual tab targets via TabAccessible wrappers
      assertTrue(targets.size >= 3, "Should have at least 3 targets for 3 tabs, got ${targets.size}")
    }
  }

  @Test
  fun `test all generated hints are unique for swing hierarchy`() {
    val buttons = (1..5).map { i ->
      JButton("Button $i").apply { setBounds(10, i * 40, 100, 30) }
    }
    withTestFrame(*buttons.toTypedArray()) { frame ->
      val generator = HintGenerator(defaultAlphabet)
      val targets = generator.generateHints(frame.rootPane, frame.rootPane.glassPane)

      val hints = targets.map { it.hint }
      assertEquals(hints.toSet().size, hints.size, "All hints should be unique")
    }
  }

  @Test
  fun `test target locations are non-negative`() {
    val button = JButton("Test").apply { setBounds(50, 70, 100, 30) }
    withTestFrame(button) { frame ->
      val generator = HintGenerator(defaultAlphabet)
      val targets = generator.generateHints(frame.rootPane, frame.rootPane.glassPane)

      targets.forEach { target ->
        assertTrue(target.location.x >= 0, "Target x should be non-negative, got ${target.location.x}")
        assertTrue(target.location.y >= 0, "Target y should be non-negative, got ${target.location.y}")
      }
    }
  }

  @Test
  fun `test target sizes are positive`() {
    val button = JButton("Test").apply { setBounds(10, 10, 120, 35) }
    withTestFrame(button) { frame ->
      val generator = HintGenerator(defaultAlphabet)
      val targets = generator.generateHints(frame.rootPane, frame.rootPane.glassPane)

      val buttonTarget = targets.find { it.component === button }
      assertNotNull(buttonTarget, "Button should be a target")
      assertTrue(buttonTarget.size.width > 0, "Target width should be positive")
      assertTrue(buttonTarget.size.height > 0, "Target height should be positive")
    }
  }

  private fun <T> withTestFrame(vararg children: javax.swing.JComponent, action: (JFrame) -> T): T {
    assumeFalse(GraphicsEnvironment.isHeadless(), "Test requires a graphical environment")
    val panel = JPanel(null)
    for (child in children) panel.add(child)
    val frame = JFrame().apply {
      setSize(400, 300)
      contentPane = panel
      isVisible = true
    }
    try {
      return action(frame)
    } finally {
      frame.dispose()
    }
  }
}
