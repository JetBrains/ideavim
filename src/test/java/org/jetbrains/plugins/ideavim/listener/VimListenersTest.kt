/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.listener

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.listener.VimListenerTestObject
import org.jetbrains.plugins.ideavim.VimTestCaseBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VimListenersTest : VimTestCaseBase() {
  @AfterEach
  fun tearDown() {
    VimListenerTestObject.disposedCounter = 0
    VimListenerTestObject.enabled = false
  }

  @Test
  fun `disposable is called on plugin disable`() {
    configureByText("XYZ")
    VimListenerTestObject.disposedCounter = 0
    VimListenerTestObject.enabled = true

    VimPlugin.setEnabled(false)

    assertEquals(1, VimListenerTestObject.disposedCounter)

    VimPlugin.setEnabled(true)
  }
}