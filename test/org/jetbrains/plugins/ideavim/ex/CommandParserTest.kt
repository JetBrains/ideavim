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

package org.jetbrains.plugins.ideavim.ex

import com.intellij.openapi.actionSystem.DataContext
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.vimscript.Executor
import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.commands.LetCommand
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import com.maddyhome.idea.vim.vimscript.parser.errors.IdeavimErrorListener
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class CommandParserTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "Caret different position")
  fun `test simple ex command execution`() {
    val keys = ">>"
    val before = "I ${c}found it in a legendary land"
    val after = "    ${c}I found it in a legendary land"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  fun `test execute in disabled state`() {
    setupChecks {
      caretShape = false
    }
    val keys = commandToKeys(">>")
    val before = "I ${c}found it in a legendary land"
    val after = "I ${c}found it in a legendary land"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.setEnabled(false)
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  fun `test turn off and on`() {
    val keys = commandToKeys(">>")
    val before = "I ${c}found it in a legendary land"
    val after = "        ${c}I found it in a legendary land"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.setEnabled(false)
      VimPlugin.setEnabled(true)
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  fun `test turn off and on twice`() {
    val keys = commandToKeys(">>")
    val before = "I ${c}found it in a legendary land"
    val after = "        ${c}I found it in a legendary land"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.setEnabled(false)
      VimPlugin.setEnabled(true)
      VimPlugin.setEnabled(true)
    }
  }

  fun `test multiline command input`() {
    val script1 = VimscriptParser.parse(
      """
     let s:patBR = substitute(match_words.',',
      \ s:notslash.'\zs[,:]*,[,:]*', ',', 'g') 
      """.trimIndent()
    )
    val script2 = VimscriptParser.parse(
      """
     let s:patBR = substitute(match_words.',',s:notslash.'\zs[,:]*,[,:]*', ',', 'g')
      """.trimIndent()
    )
    assertEquals(1, script1.units.size)
    assertTrue(script1.units[0] is LetCommand)
    assertEquals(script1, script2)
  }

  fun `test multiline expression input`() {
    configureByText("\n")
    val script1 = VimscriptParser.parse(
      """
      let dict = {'one': 1,
      \ 'two': 2}
      """.trimIndent()
    )
    val script2 = VimscriptParser.parse("let dict = {'one': 1, 'two': 2}")
    assertEquals(1, script1.units.size)
    assertTrue(script1.units[0] is LetCommand)
    assertEquals(script1, script2)
  }

  fun `test errors`() {
    configureByText("\n")
    VimscriptParser.parse(
      """
        echo 4
        let x = 3
        echo ^523
        echo 6
      """.trimIndent()
    )
    assertTrue(IdeavimErrorListener.testLogger.any { it.startsWith("line 3:5") })
  }

  fun `test errors 2`() {
    VimscriptParser.parse(
      """
        delfunction F1()
        echo 4
        echo 6
        *(
        let x = 5
      """.trimIndent()
    )
    assertTrue(IdeavimErrorListener.testLogger.any { it.startsWith("line 1:14") })
    assertTrue(IdeavimErrorListener.testLogger.any { it.startsWith("line 4:0") })
  }

  fun `test lua code in vimrc`() {
    VimscriptParser.parse(
      """
        " telescope
        nnoremap <leader>ff <cmd>Telescope find_files<cr>
        nnoremap <leader>fg <cmd>Telescope live_grep<cr>
        nnoremap <leader>b <cmd>Telescope buffers<cr>
        nnoremap <leader>fh <cmd>Telescope help_tags<cr>

        " undotree
        nnoremap <leader>u <cmd>UndotreeToggle<cr><cmd>UndotreeFocus<cr>

        let g:airline_theme='angr'

        " telescope default config
        lua << EOF
        require('telescope').setup{
          defaults = {
            vimgrep_arguments = {
              'rg',
              '--color=never',
              '--no-heading',
              '--with-filename',
              '--line-number',
              '--column',
              '--smart-case'
            },
            prompt_prefix = "> ",
            selection_caret = "> ",
            entry_prefix = "  ",
            initial_mode = "insert",
            selection_strategy = "reset",
            sorting_strategy = "descending",
            layout_strategy = "horizontal",
            layout_config = {
              horizontal = {
                mirror = false,
              },
              vertical = {
                mirror = false,
              },
            },
            file_sorter =  require'telescope.sorters'.get_fuzzy_file,
            file_ignore_patterns = {},
            generic_sorter =  require'telescope.sorters'.get_generic_fuzzy_sorter,
            winblend = 0,
            border = {},
            borderchars = { '─', '│', '─', '│', '╭', '╮', '╯', '╰' },
            color_devicons = true,
            use_less = true,
            path_display = {},
            set_env = { ['COLORTERM'] = 'truecolor' }, -- default = nil,
            file_previewer = require'telescope.previewers'.vim_buffer_cat.new,
            grep_previewer = require'telescope.previewers'.vim_buffer_vimgrep.new,
            qflist_previewer = require'telescope.previewers'.vim_buffer_qflist.new,

            -- Developer configurations: Not meant for general override
            buffer_previewer_maker = require'telescope.previewers'.buffer_previewer_maker
          }
        }
        EOF
      """.trimIndent()
    )
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
  }

  fun `test lines with errors are skipped`() {
    configureByText("\n")
    val script = VimscriptParser.parse(
      """
        let g:auto_save = 2
        echo (*
        let g:y = 10
      """.trimIndent()
    )
    assertTrue(IdeavimErrorListener.testLogger.any { it.startsWith("line 2:") })
    assertEquals(2, script.units.size)
    assertTrue(script.units[0] is LetCommand)
    val let1 = script.units[0] as LetCommand
    assertEquals(Variable(Scope.GLOBAL_VARIABLE, "auto_save"), let1.variable)
    assertEquals(SimpleExpression(VimInt(2)), let1.expression)
    val let2 = script.units[1] as LetCommand
    assertEquals(Variable(Scope.GLOBAL_VARIABLE, "y"), let2.variable)
    assertEquals(SimpleExpression(VimInt(10)), let2.expression)
  }

  fun `test ignore commands between comments`() {
    configureByText("\n")
    val script = VimscriptParser.parse(
      """
        echo 1
        "ideaVim ignore
        echo 2
        "ideaVim ignore end

        echo 3

        "ideaVim ignore
        echo 4
        echo 5
        "ideaVim ignore end

        echo 6

        "ideaVim ignore
        fa;sdlk 
        *(-78fa=09*&
        dfas;dlkfj afjldkfja s;d
        "ideaVim ignore end
      """.trimIndent()
    )
    assertEquals(3, script.units.size)
    assertTrue(script.units[0] is EchoCommand)
    assertEquals(SimpleExpression(VimInt(1)), (script.units[0] as EchoCommand).args[0])
    assertTrue(script.units[1] is EchoCommand)
    assertEquals(SimpleExpression(VimInt(3)), (script.units[1] as EchoCommand).args[0])
    assertTrue(script.units[2] is EchoCommand)
    assertEquals(SimpleExpression(VimInt(6)), (script.units[2] as EchoCommand).args[0])
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
  }

  fun `test bug with caret return symbol`() {
    configureByText("----------\n1234${c}567890\n----------\n")
    Executor.execute(
      """
        " Map perso ---------------------------------------------
        nnoremap Y y${'$'}

      """.trimIndent().replace("\n", "\r\n"),
      myFixture.editor, DataContext.EMPTY_CONTEXT, true
    )
    typeText(StringHelper.parseKeys("Yp"))
    assertState("----------\n1234556789${c}067890\n----------\n")
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
  }

  fun `test bars do not break comments`() {
    configureByText("\n")
    val script = VimscriptParser.parse(
      """
        " comment | let x = 10 | echo x
      """.trimIndent()
    )
    assertEquals(0, script.units.size)
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
  }

  fun `test autocmd is parsed without any errors`() {
    configureByText("\n")
    var script = VimscriptParser.parse(
      """
        autocmd BufReadPost *
        \ if line("'\"") > 0 && line ("'\"") <= line("$") |
        \   exe "normal! g'\"" |
        \ endif
      """.trimIndent()
    )
    assertEquals(0, script.units.size)
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())

    script = VimscriptParser.parse(
      """
        autocmd BufReadPost * echo "oh, hi Mark"
      """.trimIndent()
    )
    assertEquals(0, script.units.size)
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
  }

  fun `test unknown let command's cases are ignored`() {
    configureByText("\n")
    val script = VimscriptParser.parse(
      """
        let x[a, b; c] = something()
      """.trimIndent()
    )
    assertEquals(0, script.units.size)
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
  }
}
