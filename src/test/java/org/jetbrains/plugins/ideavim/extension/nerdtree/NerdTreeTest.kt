/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.nerdtree

import com.intellij.openapi.options.advanced.AdvancedSettings
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class NerdTreeTest : VimTestCase() {
  @Test
  fun `test collapse recursively advanced setting id`() {
    assertDoesNotThrow {
      AdvancedSettings.getBoolean("ide.tree.collapse.recursively") // will throw if the id is invalid
    }
  }
}
