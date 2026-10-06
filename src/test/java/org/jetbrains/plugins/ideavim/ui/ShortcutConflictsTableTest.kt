/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.maddyhome.idea.vim.ui.VimEmulationConfigurable
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class ShortcutConflictsTableTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `Esc is shown as the editor Escape action`() {
    val model = VimEmulationConfigurable.VimShortcutConflictsTable.Model()
    val esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
    val row = model.rows.find { it.keyStroke == esc }
    assertNotNull(row, "Esc should be listed in the shortcut conflicts")
    assertSame(ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_ESCAPE), row.action)
  }
}
