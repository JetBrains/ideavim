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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimMarkService
import com.maddyhome.idea.vim.api.injector
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
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
    val testClass = test.testClass.get()
    val noBehaviourDiffers = !method.isAnnotationPresent(VimBehaviorDiffers::class.java)
    val noTestingWithoutNeovim = !method.isAnnotationPresent(TestWithoutNeovim::class.java) &&
      !testClass.isAnnotationPresent(TestWithoutNeovim::class.java)
    val neovimTestingEnabled = isNeovimTestingEnabled()
    val notParserTest = "org.jetbrains.plugins.ideavim.ex.parser" !in testClass.packageName
    val notScriptImplementation = "org.jetbrains.plugins.ideavim.ex.implementation" !in testClass.packageName
    val notExtension = "org.jetbrains.plugins.ideavim.extension" !in testClass.packageName
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

  private fun isNeovimTestingEnabled(): Boolean {
    val property = System.getProperty("ideavim.nvim.test", "false")
    val neovimTestingEnabled = if (property.isBlank()) true else property.toBoolean()
    return neovimTestingEnabled
  }

  fun setupEditor(editor: Editor, test: TestInfo) {
    if (!neovimEnabled(test, editor)) return
    neovimApi.currentBuffer.get().setLines(0, -1, false, editor.document.text.split("\n")).get()
    ApplicationManager.getApplication().runReadAction {
      val charPosition = CharacterPosition.fromOffset(editor, editor.caretModel.offset)
      neovimApi.currentWindow.get().setCursor(VimCoords(charPosition.line + 1, charPosition.column)).get()
    }
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
    assertAll(
      { assertText(editor) },
      { assertCaret(editor, test) },
      { assertMode(editor) },
      { assertRegisters(editor) },
      { assertMarks(editor) },
    )
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
    ApplicationManager.getApplication().runReadAction {
      val resultVimCoords = CharacterPosition.atCaret(editor).toVimCoords()
      assertEquals(vimCoords.toString(), resultVimCoords.toString(), "Caret position differs. The expected position is vim coords")
    }
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

  private fun assertRegisters(editor: Editor) {
    for (register in VALID_REGISTERS) {
      if (register in nonCheckingRegisters) continue
      if (register in VimTestCase.Checks.neoVim.ignoredRegisters) continue
      val neovimRegister = getRegister(register)
      val vimEditor = editor.vim
      val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
      val vimPluginRegister = VimPlugin.getRegister().getRegister(vimEditor, context, register)
      val ideavimRegister = vimPluginRegister?.text ?: ""
      assertEquals(neovimRegister, ideavimRegister, "Register '$register'")

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
        assertEquals(expectedType, neovimChar, "Register '$register'")
      }
    }
  }

  // Marks to check: local (a-z), global (A-Z), numbered (0-9), and special marks
  // Note: '.' (last change mark) excluded because we set up Neovim by modifying the buffer
  // which affects the '.' mark, but IdeaVim doesn't have this mark set during test setup
  // Note: '[' and ']' (change marks) excluded - see VIM-4107 for undo behavior difference
  private val marksToCheck =
    VimMarkService.LOWERCASE_MARKS +
    VimMarkService.UPPERCASE_MARKS +
    VimMarkService.NUMBERED_MARKS +
    "<>'^\"" // Special marks: visual selection, jump mark, insert exit, last buffer position

  private fun assertMarks(editor: Editor) {
    for (markChar in marksToCheck) {
      if (markChar in VimTestCase.Checks.neoVim.ignoredMarks) continue

      // Get mark position from Neovim using getpos()
      // Returns [bufnum, lnum, col, off] where lnum and col are 1-based
      val neovimMarkPos = try {
        neovimApi.callFunction("getpos", listOf("'$markChar")).get()
      } catch (e: Exception) {
        continue // Mark doesn't exist in Neovim
      }

      // Parse the position list
      val posList = neovimMarkPos as? List<*> ?: continue
      if (posList.size < 4) continue

      val bufnum = (posList[0] as? Number)?.toInt() ?: continue
      val neovimLine = (posList[1] as? Number)?.toInt() ?: continue
      val neovimCol = (posList[2] as? Number)?.toInt() ?: continue

      // If mark is not set in Neovim (position is [0, 0, 0, 0])
      if (bufnum == 0 && neovimLine == 0 && neovimCol == 0) {
        // Verify it's also not set in IdeaVim
        val vimEditor = editor.vim
        val ideavimMark = injector.markService.getMark(vimEditor.primaryCaret(), markChar)
        assertEquals(null, ideavimMark, "Mark '$markChar' should not be set")
        continue
      }

      // Get mark from IdeaVim
      val vimEditor = editor.vim
      val ideavimMark = injector.markService.getMark(vimEditor.primaryCaret(), markChar)

      // Verify mark exists in IdeaVim
      assertNotNull(ideavimMark, "Mark '$markChar' should exist in IdeaVim")

      // Convert Neovim's 1-based line/col to 0-based for comparison
      val expectedLine = neovimLine - 1
      val expectedCol = neovimCol - 1

      assertEquals(expectedLine, ideavimMark.line, "Mark '$markChar' line position")
      assertEquals(expectedCol, ideavimMark.col, "Mark '$markChar' column position")
    }
  }

  fun getRegister(register: Char) = neovimApi.callFunction("getreg", listOf(register)).get().toString()
  fun getMark(register: String) = neovimApi.callFunction("getpos", listOf(register)).get().toString()
}

annotation class TestWithoutNeovim(val reason: SkipNeovimReason, val description: String = "")

enum class SkipNeovimReason {
  /**
   * Case-specific difference that doesn't fit into any of the standard categories.
   *
   * This is a catch-all reason for edge cases and unique scenarios that don't fall under
   * any other reason category. When using this reason, the description parameter is REQUIRED
   * and MUST provide a clear explanation of why the test cannot be compared with Neovim.
   *
   * Use this reason sparingly - if a pattern emerges with multiple tests using SEE_DESCRIPTION
   * for similar reasons, consider creating a new dedicated reason instead.
   */
  SEE_DESCRIPTION,

  PLUGIN,

  @Suppress("unused")
  INLAYS,
  OPTION,
  UNCLEAR,
  NON_ASCII,
  MAPPING,
  SELECT_MODE,
  VISUAL_BLOCK_MODE,

  @Deprecated("Use a more specific reason instead. Consider SEE_DESCRIPTION with a detailed explanation, or create a new dedicated reason if a pattern emerges.")
  DIFFERENT,

  // This test doesn't check vim behaviour
  NOT_VIM_TESTING,

  SHOW_CMD,
  SCROLL,
  TEMPLATES,
  EDITOR_MODIFICATION,

  CMD,
  ACTION_COMMAND,
  FOLDING,
  TABS,
  PLUGIN_ERROR,

  VIM_SCRIPT,

  GUARDED_BLOCKS,
  CTRL_CODES,

  /**
   * Neovim RPC API cannot properly handle special keys in insert mode.
   *
   * This annotation is applied to tests that use special keys like backspace, delete, or arrow keys
   * in insert mode when testing against Neovim via RPC. The nvim_input() function from the
   * ensarsarajcic/neovim-java library (v0.2.3) does not correctly handle Neovim's internal
   * termcode format for these special keys.
   *
   * Technical Details:
   * - Special keys like `<BS>`, `<Del>`, `<Left>`, `<Right>`, `<Insert>` use Neovim's termcode format:
   *   0x80 prefix + key code sequence
   * - When sent via nvim_input(), these get inserted as literal text instead of executing as key commands
   * - Simple ASCII keys like `<Esc>` (0x1B) work correctly because they don't use the termcode format
   *
   * Affected Keys:
   * - `<BS>` (backspace)
   * - `<Del>` (delete)
   * - `<Insert>` (insert/replace mode toggle)
   * - Arrow keys: `<Left>`, `<Right>`, `<Up>`, `<Down>`
   * - Other special keys that use termcode format in insert mode
   *
   * This is a limitation of the RPC communication layer, not IdeaVim or Neovim behavior.
   */
  NEOVIM_RPC_SPECIAL_KEYS_INSERT_MODE,

  BUG_IN_NEOVIM,
  PSI,

  /**
   * Test uses IdeaVim API functions that prevent proper Neovim state synchronization.
   *
   * This annotation is applied when tests use:
   * - Public IdeaVim API for plugin development (e.g., VimPlugin.* methods)
   * - Internal IdeaVim API (e.g., injector.* methods, VimEditor operations)
   *
   * When these APIs are called directly in tests, we cannot update the Neovim state
   * accordingly, making it impossible to verify test behavior against Neovim.
   *
   * Tests should only use functions from VimTestCase for Neovim compatibility.
   */
  IDEAVIM_API_USED,

  /**
   * IdeaVim intentionally behaves differently from Neovim for better user experience.
   *
   * This annotation is applied when IdeaVim deliberately deviates from Neovim behavior to:
   * - Provide more convenient user experience
   * - Follow IntelliJ Platform conventions and patterns
   * - Better integrate with IntelliJ IDEA features
   *
   * When using this reason, the description parameter MUST explain what exactly is different
   * and why IdeaVim chose to deviate from standard Vim/Neovim behavior.
   */
  IDEAVIM_WORKS_INTENTIONALLY_DIFFERENT,

  /**
   * Behavior difference inherited from the IntelliJ Platform's underlying implementation.
   *
   * This annotation is applied when IdeaVim behavior differs from Neovim due to constraints
   * or design decisions in the IntelliJ Platform that IdeaVim is built on top of.
   *
   * Examples:
   * - Empty buffer handling: Neovim buffers always contain at least one newline character,
   *   while IntelliJ editors can be completely empty
   * - Position/offset calculations: IntelliJ Platform returns different values for positions
   *   at newline characters compared to Neovim
   * - Line/column indexing differences between IntelliJ Platform and Neovim
   *
   * When using this reason, the description parameter MUST explain what Platform behavior
   * causes the difference and how it manifests in the test.
   */
  INTELLIJ_PLATFORM_INHERITED_DIFFERENCE,
}

fun LogicalPosition.toVimCoords(): VimCoords {
  return VimCoords(this.line + 1, this.column)
}
