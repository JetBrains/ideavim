/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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
import com.maddyhome.idea.vim.common.CharacterPosition
import com.maddyhome.idea.vim.group.RegisterGroup
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.helper.commandState
import org.junit.Assert.assertEquals

internal object NeovimTesting {
  private lateinit var neovimApi: NeovimApi
  private lateinit var neovim: Process

  private var neovimTestsCounter = 0

  fun setUp(test: VimTestCase) {
    if (!neovimEnabled(test)) return
    val nvimPath = System.getenv("ideavim.nvim.path") ?: "nvim"
    val pb = ProcessBuilder(nvimPath, "-u", "NONE", "--embed", "--headless", "--clean")
    neovim = pb.start()
    val neovimConnection = ProcessRpcConnection(neovim, true)
    neovimApi = NeovimApis.getApiForConnection(neovimConnection)
  }

  fun tearDown(test: VimTestCase) {
    if (!neovimEnabled(test)) return
    println("Tested with neovim: $neovimTestsCounter")
    neovim.destroy()
  }

  private fun neovimEnabled(test: VimTestCase): Boolean {
    val method = test.javaClass.getMethod(test.name)
    return !method.isAnnotationPresent(VimBehaviorDiffers::class.java)
      && !method.isAnnotationPresent(TestWithoutNeovim::class.java)
      && System.getProperty("ideavim.nvim.test", "false")!!.toBoolean()
  }

  fun setupEditor(editor: Editor, test: VimTestCase) {
    if (!neovimEnabled(test)) return
    neovimApi.currentBuffer.get().setLines(0, -1, false, editor.document.text.split("\n")).get()
    val charPosition = CharacterPosition.fromOffset(editor, editor.caretModel.offset)
    neovimApi.currentWindow.get().setCursor(VimCoords(charPosition.line + 1, charPosition.column)).get()
  }

  fun typeCommand(keys: String, test: VimTestCase) {
    if (!neovimEnabled(test)) return
    neovimApi.input(neovimApi.replaceTermcodes(keys, true, false, true).get()).get()
  }

  fun assertState(editor: Editor, test: VimTestCase) {
    if (!neovimEnabled(test)) return
    neovimTestsCounter++
    assertText(editor)
    assertCaret(editor)
    assertMode(editor)
    assertRegisters()
  }

  fun setRegister(register: Char, keys: String, test: VimTestCase) {
    if (!neovimEnabled(test)) return
    neovimApi.callFunction("setreg", listOf(register, keys, 'c'))
  }

  private fun getCaret(): VimCoords = neovimApi.currentWindow.get().cursor.get()
  private fun getText(): String = neovimApi.currentBuffer.get().getLines(0, -1, false).get().joinToString("\n")

  private fun assertCaret(editor: Editor) {
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

  private const val nonCheckingRegisters = RegisterGroup.CLIPBOARD_REGISTERS + RegisterGroup.LAST_INSERTED_TEXT_REGISTER

  private fun assertRegisters() {
    for (register in RegisterGroup.VALID_REGISTERS) {
      if (register in nonCheckingRegisters) continue
      if (register in VimTestCase.Checks.neoVim.ignoredRegisters) continue
      val neovimRegister = neovimApi.callFunction("getreg", listOf(register)).get().toString()
      val ideavimRegister = VimPlugin.getRegister().getRegister(register)?.text ?: ""
      assertEquals("Register '$register'", neovimRegister, ideavimRegister)
    }
  }
}

annotation class TestWithoutNeovim(val reason: SkipNeovimReason, val description: String = "")

enum class SkipNeovimReason {
  PLUGIN,
  MULTICARET,
  INLAYS,
  OPTION,
  UNCLEAR,
  NON_ASCII,
  MAPPING,
  SELECT_MODE,
  VISUAL_BLOCK_MODE,
  DIFFERENT,
}

fun LogicalPosition.toVimCoords(): VimCoords {
  return VimCoords(this.line + 1, this.column)
}
