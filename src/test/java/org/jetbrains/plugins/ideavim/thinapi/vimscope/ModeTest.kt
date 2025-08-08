/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi.vimscope

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.models.Mode
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.thinapi.VimApiImpl
import com.maddyhome.idea.vim.thinapi.toEngineMode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class ModeTest : VimTestCase() {
  private lateinit var vimApi: VimApi

  fun execute(block: () -> Unit) {
    injector.application.invokeAndWait {
      block()
    }
  }

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    vimApi = VimApiImpl(listenerOwner, mappingOwner)

    configureByText("\n")
  }

  private fun verifyCurrentMode(expectedMode: Mode) {
    assert(injector.vimState.mode == expectedMode.toEngineMode())
  }

  @Test
  fun `test set normal mode`() {
    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }

    verifyCurrentMode(normalMode)
  }

  @Test
  fun `test set insert mode`() {
    val insertMode = Mode.INSERT
    execute {
      vimApi.mode = insertMode
    }

    verifyCurrentMode(insertMode)
  }

  @Test
  fun `test set replace mode`() {
    val replaceMode = Mode.REPLACE
    execute {
      vimApi.mode = replaceMode
    }

    verifyCurrentMode(replaceMode)
  }

  @Test
  fun `test set visual character mode`() {
    val visualMode = Mode.VISUAL_CHARACTER
    execute {
      vimApi.mode = visualMode
    }

    verifyCurrentMode(visualMode)
  }

  @Test
  fun `test set visual line mode`() {
    val visualMode = Mode.VISUAL_LINE
    execute {
      vimApi.mode = visualMode
    }

    verifyCurrentMode(visualMode)
  }

  @Test
  fun `test set visual block mode`() {
    val visualMode = Mode.VISUAL_BLOCK
    execute {
      vimApi.mode = visualMode
    }

    verifyCurrentMode(visualMode)
  }

  @Test
  fun `test set select character mode`() {
    val selectMode = Mode.SELECT_BLOCK
    execute {
      vimApi.mode = selectMode
    }

    verifyCurrentMode(selectMode)
  }

  @Test
  fun `test set select line mode`() {
    val selectMode = Mode.SELECT_LINE
    execute {
      vimApi.mode = selectMode
    }

    verifyCurrentMode(selectMode)
  }

  @Test
  fun `test set select block mode`() {
    val selectMode = Mode.SELECT_BLOCK
    execute {
      vimApi.mode = selectMode
    }

    verifyCurrentMode(selectMode)
  }

  @Test
  fun `test set command line mode`() {
    val cmdLineMode = Mode.COMMAND_LINE
    execute {
      vimApi.mode = cmdLineMode
    }

    verifyCurrentMode(cmdLineMode)
  }

  @Test
  fun `test set operator pending mode`() {
    val opPendingMode = Mode.OP_PENDING
    execute {
      vimApi.mode = opPendingMode
    }

    verifyCurrentMode(opPendingMode)
  }

  @Test
  fun `test transition from normal to insert mode`() {
    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }

    verifyCurrentMode(normalMode)

    val insertMode = Mode.INSERT
    execute {
      vimApi.mode = insertMode
    }

    verifyCurrentMode(insertMode)
  }

  @Test
  fun `test transition from insert to normal mode`() {
    val insertMode = Mode.INSERT
    execute {
      vimApi.mode = insertMode
    }
    verifyCurrentMode(insertMode)

    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)
  }

  @Test
  fun `test transition from visual character to normal mode`() {
    val visualMode = Mode.VISUAL_CHARACTER
    execute {
      vimApi.mode = visualMode
    }
    verifyCurrentMode(visualMode)

    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)
  }

  @Test
  fun `test transition from visual line to normal mode`() {
    val visualMode = Mode.VISUAL_LINE
    execute {
      vimApi.mode = visualMode
    }
    verifyCurrentMode(visualMode)

    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)
  }

  @Test
  fun `test transition from visual block to normal mode`() {
    val visualMode = Mode.VISUAL_BLOCK
    execute {
      vimApi.mode = visualMode
    }
    verifyCurrentMode(visualMode)

    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)
  }

  @Test
  fun `test transition from select character to normal mode`() {
    val selectMode = Mode.SELECT_BLOCK
    execute {
      vimApi.mode = selectMode
    }
    verifyCurrentMode(selectMode)

    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)
  }

  @Test
  fun `test transition between visual character and visual line modes`() {
    val visualCharMode = Mode.VISUAL_CHARACTER
    execute {
      vimApi.mode = visualCharMode
    }
    verifyCurrentMode(visualCharMode)

    val visualLineMode = Mode.VISUAL_LINE
    execute {
      vimApi.mode = visualLineMode
    }
    verifyCurrentMode(visualLineMode)
  }

  @Test
  fun `test transition between visual line and visual block modes`() {
    val visualLineMode = Mode.VISUAL_LINE
    execute {
      vimApi.mode = visualLineMode
    }
    verifyCurrentMode(visualLineMode)

    val visualBlockMode = Mode.VISUAL_BLOCK
    execute {
      vimApi.mode = visualBlockMode
    }
    verifyCurrentMode(visualBlockMode)
  }

  @Test
  fun `test transition between visual block and visual character modes`() {
    val visualBlockMode = Mode.VISUAL_BLOCK
    execute {
      vimApi.mode = visualBlockMode
    }
    verifyCurrentMode(visualBlockMode)

    val visualCharMode = Mode.VISUAL_CHARACTER
    execute {
      vimApi.mode = visualCharMode
    }
    verifyCurrentMode(visualCharMode)
  }

  @Test
  fun `test transition from visual to select mode`() {
    val visualMode = Mode.VISUAL_CHARACTER
    execute {
      vimApi.mode = visualMode
    }
    verifyCurrentMode(visualMode)

    val selectMode = Mode.SELECT_BLOCK
    execute {
      vimApi.mode = selectMode
    }
    verifyCurrentMode(selectMode)
  }

  @Test
  fun `test transition from select to visual mode`() {
    val selectMode = Mode.SELECT_BLOCK
    execute {
      vimApi.mode = selectMode
    }
    verifyCurrentMode(selectMode)

    val visualMode = Mode.VISUAL_CHARACTER
    execute {
      vimApi.mode = visualMode
    }
    verifyCurrentMode(visualMode)
  }

  @Test
  fun `test transition from command line to normal mode`() {
    val cmdLineMode = Mode.COMMAND_LINE
    execute {
      vimApi.mode = cmdLineMode
    }
    verifyCurrentMode(cmdLineMode)

    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)
  }

  @Test
  fun `test transition from operator pending to normal mode`() {
    val opPendingMode = Mode.OP_PENDING
    execute {
      vimApi.mode = opPendingMode
    }
    verifyCurrentMode(opPendingMode)

    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)
  }

  @Test
  fun `test transition from replace to normal mode`() {
    val replaceMode = Mode.REPLACE
    execute {
      vimApi.mode = replaceMode
    }
    verifyCurrentMode(replaceMode)

    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)
  }

  @Test
  fun `test transition from normal to replace mode`() {
    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)

    val replaceMode = Mode.REPLACE
    execute {
      vimApi.mode = replaceMode
    }
    verifyCurrentMode(replaceMode)
  }

  @Test
  fun `test transition from insert to replace mode`() {
    val insertMode = Mode.INSERT
    execute {
      vimApi.mode = insertMode
    }
    verifyCurrentMode(insertMode)

    val replaceMode = Mode.REPLACE
    execute {
      vimApi.mode = replaceMode
    }
    verifyCurrentMode(replaceMode)
  }

  @Test
  fun `test transition from replace to insert mode`() {
    val replaceMode = Mode.REPLACE
    execute {
      vimApi.mode = replaceMode
    }
    verifyCurrentMode(replaceMode)

    val insertMode = Mode.INSERT
    execute {
      vimApi.mode = insertMode
    }
    verifyCurrentMode(insertMode)
  }

  @Test
  fun `test transition from normal to command line mode`() {
    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)

    val cmdLineMode = Mode.COMMAND_LINE
    execute {
      vimApi.mode = cmdLineMode
    }
    verifyCurrentMode(cmdLineMode)
  }

  @Test
  fun `test transition from normal to operator pending mode`() {
    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)

    val opPendingMode = Mode.OP_PENDING
    execute {
      vimApi.mode = opPendingMode
    }
    verifyCurrentMode(opPendingMode)
  }

  @Test
  fun `test transition from normal to visual character mode`() {
    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)

    val visualMode = Mode.VISUAL_CHARACTER
    execute {
      vimApi.mode = visualMode
    }
    verifyCurrentMode(visualMode)
  }

  @Test
  fun `test transition from normal to visual line mode`() {
    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)

    val visualMode = Mode.VISUAL_LINE
    execute {
      vimApi.mode = visualMode
    }
    verifyCurrentMode(visualMode)
  }

  @Test
  fun `test transition from normal to visual block mode`() {
    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)

    val visualMode = Mode.VISUAL_BLOCK
    execute {
      vimApi.mode = visualMode
    }
    verifyCurrentMode(visualMode)
  }

  @Test
  fun `test transition from normal to select character mode`() {
    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)

    val selectMode = Mode.SELECT_BLOCK
    execute {
      vimApi.mode = selectMode
    }
    verifyCurrentMode(selectMode)
  }

  @Test
  fun `test transition from normal to select line mode`() {
    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)

    val selectMode = Mode.SELECT_LINE
    execute {
      vimApi.mode = selectMode
    }
    verifyCurrentMode(selectMode)
  }

  @Test
  fun `test transition from normal to select block mode`() {
    val normalMode = Mode.NORMAL
    execute {
      vimApi.mode = normalMode
    }
    verifyCurrentMode(normalMode)

    val selectMode = Mode.SELECT_BLOCK
    execute {
      vimApi.mode = selectMode
    }
    verifyCurrentMode(selectMode)
  }
}
