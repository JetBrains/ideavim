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
import org.junit.jupiter.api.Test
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HintPreservationTest : VimTestCase() {

  private val defaultAlphabet = "ASDFGHJKL".toList()

  @Test
  fun `test hints are preserved across regeneration`() {
    val button1 = JButton("A").apply { setBounds(10, 10, 100, 30) }
    val button2 = JButton("B").apply { setBounds(10, 50, 100, 30) }
    withTestFrame(button1, button2) { frame ->
      val generator = HintGenerator.Permutation(defaultAlphabet)

      val targets1 = generator.generate(frame.rootPane, frame.rootPane.glassPane)
      val hint1 = targets1.find { it.component === button1 }?.hint
      val hint2 = targets1.find { it.component === button2 }?.hint
      assertNotNull(hint1, "Button1 should have a hint")
      assertNotNull(hint2, "Button2 should have a hint")

      val targets2 = generator.generate(frame.rootPane, frame.rootPane.glassPane)
      assertEquals(hint1, targets2.find { it.component === button1 }?.hint, "Button1 hint should be preserved")
      assertEquals(hint2, targets2.find { it.component === button2 }?.hint, "Button2 hint should be preserved")
    }
  }

  @Test
  fun `test hints preserved when new component is added`() {
    val button1 = JButton("A").apply { setBounds(10, 10, 100, 30) }
    val button2 = JButton("B").apply { setBounds(10, 50, 100, 30) }
    val contentPane = JPanel(null)
    contentPane.add(button1)
    contentPane.add(button2)
    val frame = JFrame().apply {
      setSize(400, 300)
      this.contentPane = contentPane
      isVisible = true
    }
    try {
      val generator = HintGenerator.Permutation(defaultAlphabet)

      val targets1 = generator.generate(frame.rootPane, frame.rootPane.glassPane)
      val hint1 = targets1.find { it.component === button1 }?.hint
      val hint2 = targets1.find { it.component === button2 }?.hint
      assertNotNull(hint1)
      assertNotNull(hint2)

      // Add a new button and regenerate
      val button3 = JButton("C").apply { setBounds(10, 90, 100, 30) }
      contentPane.add(button3)
      frame.validate()

      val targets2 = generator.generate(frame.rootPane, frame.rootPane.glassPane)
      assertEquals(hint1, targets2.find { it.component === button1 }?.hint, "Button1 hint should be preserved")
      assertEquals(hint2, targets2.find { it.component === button2 }?.hint, "Button2 hint should be preserved")
      assertNotNull(targets2.find { it.component === button3 }?.hint, "Button3 should get a hint")
    } finally {
      frame.dispose()
    }
  }

  private fun <T> withTestFrame(vararg children: javax.swing.JComponent, action: (JFrame) -> T): T {
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
