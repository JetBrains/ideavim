/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.hints

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorEx
import com.maddyhome.idea.vim.extension.hints.HintGenerator
import com.maddyhome.idea.vim.extension.hints.HintLabelPosition
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import java.awt.Container
import java.awt.GraphicsEnvironment
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HintEditorIntegrationTest : VimTestCase() {

  private val defaultAlphabet = "ASDFGHJKL".toList()

  @Test
  fun `test generate from editor in frame`() {
    assumeFalse(GraphicsEnvironment.isHeadless(), "Test requires a graphical environment")
    configureByText("Hello World\nLine 2\nLine 3")

    ApplicationManager.getApplication().invokeAndWait {
      val editor = fixture.editor as EditorEx
      withEditorInFrame(editor) { frame ->
        val generator = HintGenerator(defaultAlphabet)
        val targets = generator.generateHints(frame.rootPane, frame.rootPane.glassPane)

        assertTrue(targets.isNotEmpty(), "Should find at least one target from editor hierarchy")

        val hints = targets.map { it.hint }
        val nonEmptyHints = hints.filter { it.isNotEmpty() }
        assertEquals(nonEmptyHints.toSet().size, nonEmptyHints.size, "Non-empty hints should be unique")
      }
    }
  }

  @Test
  fun `test editor scroll pane gets center label position`() {
    assumeFalse(GraphicsEnvironment.isHeadless(), "Test requires a graphical environment")
    configureByText("Hello World")

    ApplicationManager.getApplication().invokeAndWait {
      val editor = fixture.editor as EditorEx
      withEditorInFrame(editor) { frame ->
        val generator = HintGenerator(defaultAlphabet)
        val targets = generator.generateHints(frame.rootPane, frame.rootPane.glassPane)

        val editorScrollTarget = targets.find { target ->
          target.component is JScrollPane &&
            target.component.viewport?.view?.javaClass?.simpleName == "EditorComponentImpl"
        }
        assertNotNull(editorScrollTarget, "Editor scroll pane should be found as a target")
        assertEquals(
          HintLabelPosition.CENTER,
          editorScrollTarget.labelPosition,
          "Editor scroll pane should have CENTER label position",
        )
      }
    }
  }

  /**
   * Temporarily reparents the editor's scroll pane into a visible JFrame
   * so that `collectTargets` can traverse a real component hierarchy with
   * proper visibility rects.
   */
  private fun <T> withEditorInFrame(editor: EditorEx, action: (JFrame) -> T): T {
    val scrollPane = editor.scrollPane
    val originalParent = scrollPane.parent
    val originalIndex = originalParent?.let { parent ->
      parent.components?.indexOf(scrollPane) ?: -1
    } ?: -1

    val panel = JPanel(null)
    scrollPane.setBounds(0, 0, 600, 400)
    panel.add(scrollPane)
    val frame = JFrame().apply {
      setSize(600, 400)
      contentPane = panel
      isVisible = true
    }
    try {
      return action(frame)
    } finally {
      frame.dispose()
      if (originalParent is Container && originalIndex >= 0) {
        originalParent.add(scrollPane, originalIndex)
        originalParent.validate()
      }
    }
  }
}
