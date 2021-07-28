/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim

import com.ensarsarajcic.neovim.java.api.NeovimApi
import com.ensarsarajcic.neovim.java.api.NeovimApis
import com.ensarsarajcic.neovim.java.api.types.api.VimCoords
import com.ensarsarajcic.neovim.java.corerpc.client.ProcessRpcConnection
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.CharacterPosition
import com.maddyhome.idea.vim.group.RegisterGroup
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.helper.commandState
import org.junit.Assert.assertEquals

internal object NeovimTesting {
  private lateinit var neovimApi: NeovimApi
  private lateinit var neovim: Process

  private var neovimTestsCounter = 0

  private var currentTestName = ""
  private val untested = mutableListOf<String>()

  private lateinit var exitCommand: String
  private lateinit var escapeCommand: String
  private lateinit var ctrlcCommand: String

  fun setUp(test: VimTestCase) {
    if (!neovimEnabled(test)) return
    val nvimPath = System.getenv("ideavim.nvim.path") ?: "nvim"
    val pb = ProcessBuilder(nvimPath, "-u", "NONE", "--embed", "--headless", "--clean", "--cmd", "set noswapfile")
    neovim = pb.start()
    val neovimConnection = ProcessRpcConnection(neovim, true)
    neovimApi = NeovimApis.getApiForConnection(neovimConnection)
    exitCommand = neovimApi.replaceTermcodes("<esc><esc>:qa!", true, false, true).get()
    escapeCommand = neovimApi.replaceTermcodes("<esc>", true, false, true).get()
    ctrlcCommand = neovimApi.replaceTermcodes("<C-C>", true, false, true).get()
    currentTestName = test.name
  }

  fun tearDown(test: VimTestCase) {
    if (!neovimEnabled(test)) return
    println("Tested with neovim: $neovimTestsCounter")
    if (VimTestCase.Checks.neoVim.exitOnTearDown) {
      neovimApi.input(exitCommand).get()
    }
    neovim.destroy()
    if (currentTestName.isNotBlank()) {
      untested.add(currentTestName)
      println("----")
      println("$untested : ${untested.size}")
    }
  }

  private fun neovimEnabled(test: VimTestCase): Boolean {
    val method = test.javaClass.getMethod(test.name)
    return !method.isAnnotationPresent(VimBehaviorDiffers::class.java) &&
      !method.isAnnotationPresent(TestWithoutNeovim::class.java) &&
      System.getProperty("ideavim.nvim.test", "false")!!.toBoolean()
  }

  fun setupEditor(editor: Editor, test: VimTestCase) {
    if (!neovimEnabled(test)) return
    neovimApi.currentBuffer.get().setLines(0, -1, false, editor.document.text.split("\n")).get()
    val charPosition = CharacterPosition.fromOffset(editor, editor.caretModel.offset)
    neovimApi.currentWindow.get().setCursor(VimCoords(charPosition.line + 1, charPosition.column)).get()
  }

  fun typeCommand(keys: String, test: VimTestCase) {
    if (!neovimEnabled(test)) return
    when {
      keys.equals("<esc>", ignoreCase = true) -> neovimApi.input(escapeCommand).get()
      keys.equals("<C-C>", ignoreCase = true) -> neovimApi.input(ctrlcCommand).get()
      else -> neovimApi.input(neovimApi.replaceTermcodes(keys, true, false, true).get()).get()
    }
  }

  fun assertState(editor: Editor, test: VimTestCase) {
    if (!neovimEnabled(test)) return
    if (currentTestName != "") {
      currentTestName = ""
      neovimTestsCounter++
    }
    assertText(editor)
    assertCaret(editor, test)
    assertMode(editor)
    assertRegisters()
  }

  fun setRegister(register: Char, keys: String, test: VimTestCase) {
    if (!neovimEnabled(test)) return
    neovimApi.callFunction("setreg", listOf(register, keys, 'c'))
  }

  private fun getCaret(): VimCoords = neovimApi.currentWindow.get().cursor.get()
  private fun getText(): String = neovimApi.currentBuffer.get().getLines(0, -1, false).get().joinToString("\n")

  fun assertCaret(editor: Editor, test: VimTestCase) {
    if (!neovimEnabled(test)) return
    if (currentTestName != "") {
      currentTestName = ""
      neovimTestsCounter++
    }
    val vimCoords = getCaret()
    val resultVimCoords = CharacterPosition.atCaret(editor).toVimCoords()
    assertEquals(vimCoords.toString(), resultVimCoords.toString())
  }

  private fun assertText(editor: Editor) {
    val neovimContent = getText()
    assertEquals(neovimContent, editor.document.text)
  }

  private fun assertMode(editor: Editor) {
    val ideavimState = editor.commandState.toVimNotation()
    val neovimState = neovimApi.mode.get().mode
    assertEquals(neovimState, ideavimState)
  }

  private const val nonCheckingRegisters =
    RegisterGroup.CLIPBOARD_REGISTERS +
      RegisterGroup.LAST_INSERTED_TEXT_REGISTER +
      RegisterGroup.BLACK_HOLE_REGISTER +
      RegisterGroup.LAST_SEARCH_REGISTER +
      RegisterGroup.ALTERNATE_BUFFER_REGISTER +
      RegisterGroup.EXPRESSION_BUFFER_REGISTER +
      RegisterGroup.CURRENT_FILENAME_REGISTER

  private fun assertRegisters() {
    for (register in RegisterGroup.VALID_REGISTERS) {
      if (register in nonCheckingRegisters) continue
      if (register in VimTestCase.Checks.neoVim.ignoredRegisters) continue
      val neovimRegister = neovimApi.callFunction("getreg", listOf(register)).get().toString()
      val vimPluginRegister = VimPlugin.getRegister().getRegister(register)
      val ideavimRegister = vimPluginRegister?.text ?: ""
      assertEquals("Register '$register'", neovimRegister, ideavimRegister)

      if (neovimRegister.isNotEmpty()) {
        val neovimRegisterType = neovimApi.callFunction("getregtype", listOf(register)).get().toString()
        val expectedType = when (vimPluginRegister?.type) {
          SelectionType.CHARACTER_WISE -> "v"
          SelectionType.LINE_WISE -> "V"
          SelectionType.BLOCK_WISE -> "\u0016"
          else -> ""
        }

        // We take only the first char because neovim returns width for block selection
        val neovimChar = neovimRegisterType.getOrNull(0)?.toString() ?: ""
        assertEquals("Register '$register'", expectedType, neovimChar)
      }
    }
  }
}

annotation class TestWithoutNeovim(val reason: SkipNeovimReason, val description: String = "")

enum class SkipNeovimReason {
  PLUGIN,
  MULTICARET,

  @Suppress("unused")
  INLAYS,
  OPTION,
  UNCLEAR,
  NON_ASCII,
  MAPPING,
  SELECT_MODE,
  VISUAL_BLOCK_MODE,
  DIFFERENT,

  // This test doesn't check vim behaviour
  NOT_VIM_TESTING,

  SHOW_CMD,
  SCROLL,
  TEMPLATES,
  EDITOR_MODIFICATION,

  CMD,
  IDEAVIMRC,
  ACTION_COMMAND,
  PLUG,
  FOLDING,
  TABS,
  PLUGIN_ERROR,
}

fun LogicalPosition.toVimCoords(): VimCoords {
  return VimCoords(this.line + 1, this.column)
}
