/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.register.RegisterConstants.ALTERNATE_BUFFER_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.BLACK_HOLE_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.CLIPBOARD_REGISTERS
import com.maddyhome.idea.vim.register.RegisterConstants.CURRENT_FILENAME_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.EXPRESSION_BUFFER_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_INSERTED_TEXT_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_SEARCH_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.VALID_REGISTERS
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.toVimNotation
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.TestInfo

object NeovimTesting {
  private lateinit var neovimApi: NeovimApi
  private lateinit var neovim: Process

  private var neovimTestsCounter = 0

  private var currentTestName = ""
  private val untested = mutableListOf<String>()

  private lateinit var exitCommand: String
  private lateinit var escapeCommand: String
  private lateinit var ctrlcCommand: String

  private var singleCaret = true

  fun setUp(test: TestInfo) {
    if (!neovimEnabled(test)) return
    val nvimPath = System.getenv("ideavim.nvim.path") ?: "nvim"

    val pb = ProcessBuilder(
      nvimPath,
      "-u", "NONE",
      "--embed",
      "--headless",
      "--clean",
      "--cmd", "set noswapfile",
      "--cmd", "set sol",
    )

    neovim = pb.start()
    val neovimConnection = ProcessRpcConnection(neovim, true)
    neovimApi = NeovimApis.getApiForConnection(neovimConnection)
    exitCommand = neovimApi.replaceTermcodes("<esc><esc>:qa!", true, false, true).get()
    escapeCommand = neovimApi.replaceTermcodes("<esc>", true, false, true).get()
    ctrlcCommand = neovimApi.replaceTermcodes("<C-C>", true, false, true).get()
    currentTestName = test.displayName
  }

  fun tearDown(test: TestInfo) {
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

  private fun neovimEnabled(test: TestInfo, editor: Editor? = null): Boolean {
    val method = test.testMethod.get()
    val noBehaviourDiffers = !method.isAnnotationPresent(VimBehaviorDiffers::class.java)
    val noTestingWithoutNeovim = !method.isAnnotationPresent(TestWithoutNeovim::class.java) &&
      !test.javaClass.isAnnotationPresent(TestWithoutNeovim::class.java)
    val neovimTestingEnabled = isNeovimTestingEnabled()
    val notParserTest = "org.jetbrains.plugins.ideavim.ex.parser" !in test.javaClass.packageName
    val notScriptImplementation = "org.jetbrains.plugins.ideavim.ex.implementation" !in test.javaClass.packageName
    val notExtension = "org.jetbrains.plugins.ideavim.extension" !in test.javaClass.packageName
    if (singleCaret) {
      singleCaret = editor == null || editor.caretModel.caretCount == 1
    }
    return noBehaviourDiffers &&
      noTestingWithoutNeovim &&
      neovimTestingEnabled &&
      notParserTest &&
      notScriptImplementation &&
      notExtension &&
      singleCaret
  }

  fun isNeovimTestingEnabled(): Boolean {
    val property = System.getProperty("ideavim.nvim.test", "false")
    val neovimTestingEnabled = if (property.isBlank()) true else property.toBoolean()
    return neovimTestingEnabled
  }

  fun setupEditor(editor: Editor, test: TestInfo) {
    if (!neovimEnabled(test, editor)) return
    neovimApi.currentBuffer.get().setLines(0, -1, false, editor.document.text.split("\n")).get()
    val charPosition = CharacterPosition.fromOffset(editor, editor.caretModel.offset)
    neovimApi.currentWindow.get().setCursor(VimCoords(charPosition.line + 1, charPosition.column)).get()
  }

  fun typeCommand(keys: String, test: TestInfo, editor: Editor) {
    if (!neovimEnabled(test, editor)) return
    when {
      keys.equals("<esc>", ignoreCase = true) -> neovimApi.input(escapeCommand).get()
      keys.equals("<C-C>", ignoreCase = true) -> neovimApi.input(ctrlcCommand).get()
      else -> {
        val replacedCodes = neovimApi.replaceTermcodes(keys, true, false, true).get()
        neovimApi.input(replacedCodes).get()
      }
    }
  }

  fun assertState(editor: Editor, test: TestInfo) {
    if (!neovimEnabled(test, editor)) return
    if (currentTestName != "") {
      currentTestName = ""
      neovimTestsCounter++
    }
    assertText(editor)
    assertCaret(editor, test)
    assertMode(editor)
    assertRegisters()
  }

  fun setRegister(register: Char, keys: String, test: TestInfo) {
    if (!neovimEnabled(test)) return
    neovimApi.callFunction("setreg", listOf(register, keys, 'c'))
  }

  private fun getCaret(): VimCoords = neovimApi.currentWindow.get().cursor.get()
  private fun getText(): String = neovimApi.currentBuffer.get().getLines(0, -1, false).get().joinToString("\n")

  fun assertCaret(editor: Editor, test: TestInfo) {
    if (!neovimEnabled(test, editor)) return
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

  fun vimMode() = neovimApi.mode.get().mode

  private fun assertMode(editor: Editor) {
    val ideavimState = editor.vim.mode.toVimNotation()
    val neovimState = vimMode()
    assertEquals(neovimState, ideavimState)
  }

  private const val nonCheckingRegisters =
    CLIPBOARD_REGISTERS +
      LAST_INSERTED_TEXT_REGISTER +
      BLACK_HOLE_REGISTER +
      LAST_SEARCH_REGISTER +
      ALTERNATE_BUFFER_REGISTER +
      EXPRESSION_BUFFER_REGISTER +
      CURRENT_FILENAME_REGISTER

  private fun assertRegisters() {
    for (register in VALID_REGISTERS) {
      if (register in nonCheckingRegisters) continue
      if (register in VimTestCase.Checks.neoVim.ignoredRegisters) continue
      val neovimRegister = getRegister(register)
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

  fun getRegister(register: Char) = neovimApi.callFunction("getreg", listOf(register)).get().toString()
  fun getMark(register: String) = neovimApi.callFunction("getpos", listOf(register)).get().toString()
}

annotation class TestWithoutNeovim(val reason: SkipNeovimReason, val description: String = "")

enum class SkipNeovimReason {
  PLUGIN,

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
  ACTION_COMMAND,
  PLUG,
  FOLDING,
  TABS,
  PLUGIN_ERROR,

  VIM_SCRIPT,

  GUARDED_BLOCKS,
  CTRL_CODES,

  BUG_IN_NEOVIM,
  PSI,
}

fun LogicalPosition.toVimCoords(): VimCoords {
  return VimCoords(this.line + 1, this.column)
}
