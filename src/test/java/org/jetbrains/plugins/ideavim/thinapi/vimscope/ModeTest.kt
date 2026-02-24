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

    configureByText("\n")

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    val projectId = injector.file.getProjectId(fixture.project)
    vimApi = VimApiImpl(listenerOwner, mappingOwner, projectId)
  }

  private fun verifyCurrentMode(expectedMode: Mode) {
    assert(injector.vimState.mode == expectedMode.toEngineMode())
  }

  @Test
  fun `test enter insert mode via normal`() {
    execute {
      vimApi.normal("i")
    }
    verifyCurrentMode(Mode.INSERT)
  }

  @Test
  fun `test exit to normal mode via normal Esc`() {
    execute {
      vimApi.normal("i")
    }
    verifyCurrentMode(Mode.INSERT)

    execute {
      vimApi.normal("<Esc>")
    }
    verifyCurrentMode(Mode.NORMAL)
  }

  @Test
  fun `test enter visual character mode via normal`() {
    execute {
      vimApi.normal("v")
    }
    verifyCurrentMode(Mode.VISUAL_CHARACTER)
  }

  @Test
  fun `test enter visual line mode via normal`() {
    execute {
      vimApi.normal("V")
    }
    verifyCurrentMode(Mode.VISUAL_LINE)
  }

  @Test
  fun `test exit visual to normal mode via normal Esc`() {
    execute {
      vimApi.normal("v")
    }
    verifyCurrentMode(Mode.VISUAL_CHARACTER)

    execute {
      vimApi.normal("<Esc>")
    }
    verifyCurrentMode(Mode.NORMAL)
  }

  @Test
  fun `test transition from insert to normal to visual`() {
    execute {
      vimApi.normal("i")
    }
    verifyCurrentMode(Mode.INSERT)

    execute {
      vimApi.normal("<Esc>")
    }
    verifyCurrentMode(Mode.NORMAL)

    execute {
      vimApi.normal("v")
    }
    verifyCurrentMode(Mode.VISUAL_CHARACTER)
  }

  @Test
  fun `test read mode returns correct value`() {
    execute {
      vimApi.normal("i")
    }
    assert(vimApi.mode == Mode.INSERT)

    execute {
      vimApi.normal("<Esc>")
    }
    assert(vimApi.mode == Mode.NORMAL)
  }
}
