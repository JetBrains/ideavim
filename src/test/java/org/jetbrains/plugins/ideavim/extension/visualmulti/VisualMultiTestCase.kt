/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

abstract class VisualMultiTestCase : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("visual-multi")
  }

  /**
   * Runs [commands] before the extension is initialized, the same as in the ideavimrc, which is executed before the
   * extensions are initialized
   */
  protected fun reinitAfter(vararg commands: String) {
    enterCommand("set novisual-multi")
    commands.forEach { enterCommand(it) }
    enterCommand("set visual-multi")
  }
}
