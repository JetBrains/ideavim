/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex

import com.intellij.openapi.actionSystem.DataContext
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.commands.LetCommand
import com.maddyhome.idea.vim.vimscript.model.commands.NormalCommand
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import com.maddyhome.idea.vim.vimscript.parser.errors.IdeavimErrorListener
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author Alex Plate
 */
class CommandParserTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "Caret different position")
  @Test
  fun `test simple ex command execution`() {
    val keys = ">>"
    val before = "I ${c}found it in a legendary land"
    val after = "    ${c}I found it in a legendary land"
    doTest(keys, before, after, Mode.NORMAL())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  @Test
  fun `test execute in disabled state`() {
    setupChecks {
      caretShape = false
    }
    val before = "I ${c}found it in a legendary land"
    val after = """I :>>
      |${c}found it in a legendary land
    """.trimMargin()
    doTest(exCommand(">>"), before, after) {
      VimPlugin.setEnabled(false)
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  @Test
  fun `test turn off and on`() {
    val before = "I ${c}found it in a legendary land"
    val after = "        ${c}I found it in a legendary land"
    doTest(exCommand(">>"), before, after) {
      VimPlugin.setEnabled(false)
      VimPlugin.setEnabled(true)
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  @Test
  fun `test turn off and on twice`() {
    val before = "I ${c}found it in a legendary land"
    val after = "        ${c}I found it in a legendary land"
    doTest(exCommand(">>"), before, after) {
      VimPlugin.setEnabled(false)
      VimPlugin.setEnabled(true)
      VimPlugin.setEnabled(true)
    }
  }

  @Test
  fun `test multiline command input`() {
    val script1 = VimscriptParser.parse(
      """
     let s:patBR = substitute(match_words.',',
      \ s:notslash.'\zs[,:]*,[,:]*', ',', 'g') 
      """.trimIndent(),
    )
    val script2 = VimscriptParser.parse(
      """
     let s:patBR = substitute(match_words.',', s:notslash.'\zs[,:]*,[,:]*', ',', 'g')
      """.trimIndent(),
    )
    assertEquals(1, script1.units.size)
    assertTrue(script1.units[0] is LetCommand)
    assertEquals(script1, script2)
  }

  @Test
  fun `test multiline command input with tabs`() {
    val script1 = VimscriptParser.parse(
      """
     let s:patBR = substitute(match_words.',',
      ${"\t"}\ s:notslash.'\zs[,:]*,[,:]*', ',', 'g') 
      """.trimIndent(),
    )
    val script2 = VimscriptParser.parse(
      """
     let s:patBR = substitute(match_words.',', s:notslash.'\zs[,:]*,[,:]*', ',', 'g')
      """.trimIndent(),
    )
    assertEquals(1, script1.units.size)
    assertTrue(script1.units[0] is LetCommand)
    assertEquals(script1, script2)
  }

  @Test
  fun `test multiline expression input`() {
    configureByText("\n")
    val script1 = VimscriptParser.parse(
      """
      let dict = {'one': 1,
      \ 'two': 2}
      """.trimIndent(),
    )
    val script2 = VimscriptParser.parse("let dict = {'one': 1, 'two': 2}")
    assertEquals(1, script1.units.size)
    assertTrue(script1.units[0] is LetCommand)
    assertEquals(script1, script2)
  }

  @Test
  fun `test errors`() {
    configureByText("\n")
    VimscriptParser.parse(
      """
        echo 4
        let x = 3
        echo ^523
        echo 6
      """.trimIndent(),
    )
    assertTrue(IdeavimErrorListener.testLogger.any { it.startsWith("line 3:5") })
  }

  @Test
  fun `test errors 2`() {
    VimscriptParser.parse(
      """
        delfunction F1()
        echo 4
        echo 6
        *(
        let x = 5
      """.trimIndent(),
    )
    assertTrue(IdeavimErrorListener.testLogger.any { it.startsWith("line 1:14") })
    assertTrue(IdeavimErrorListener.testLogger.any { it.startsWith("line 4:0") })
  }

  @Test
  fun `test lua code in vimrc with 'lua EOF'`() {
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
      """.trimIndent(),
    )
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
  }

  @Test
  fun `test lua code in vimrc with 'lua END'`() {
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
        lua << END
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
        END
      """.trimIndent(),
    )
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
  }

  @Test
  fun `test lines with errors are skipped`() {
    configureByText("\n")
    val script = VimscriptParser.parse(
      """
        let g:auto_save = 2
        echo (*
        let g:y = 10
      """.trimIndent(),
    )
    assertTrue(IdeavimErrorListener.testLogger.any { it.startsWith("line 2:") })
    assertEquals(2, script.units.size)
    assertTrue(script.units[0] is LetCommand)
    val let1 = script.units[0] as LetCommand
    assertEquals(VariableExpression(Scope.GLOBAL_VARIABLE, "auto_save"), let1.lvalue)
    assertEquals(SimpleExpression(2), let1.expression)
    val let2 = script.units[1] as LetCommand
    assertEquals(VariableExpression(Scope.GLOBAL_VARIABLE, "y"), let2.lvalue)
    assertEquals(SimpleExpression(10), let2.expression)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `test bug with caret return symbol`() {
    configureByText("----------\n1234${c}567890\n----------\n")
    injector.vimscriptExecutor.execute(
      """
        " Map perso ---------------------------------------------
        nnoremap Y y${'$'}

      """.trimIndent().replace("\n", "\r\n"),
      fixture.editor.vim,
      DataContext.EMPTY_CONTEXT.vim,
      skipHistory = true,
      indicateErrors = true,
      CommandLineVimLContext,
    )
    typeText(injector.parser.parseKeys("Yp"))
    assertState("----------\n1234556789${c}067890\n----------\n")
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
  }

  @Test
  fun `test bars do not break comments`() {
    configureByText("\n")
    val script = VimscriptParser.parse(
      """
        " comment | let x = 10 | echo x
      """.trimIndent(),
    )
    assertEquals(0, script.units.size)
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
  }

  @Test
  fun `test autocmd is parsed without any errors`() {
    configureByText("\n")
    var script = VimscriptParser.parse(
      """
        autocmd BufReadPost *
        \ if line("'\"") > 0 && line ("'\"") <= line("$") |
        \   exe "normal! g'\"" |
        \ endif
      """.trimIndent(),
    )
    assertEquals(0, script.units.size)
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())

    script = VimscriptParser.parse(
      """
        autocmd BufReadPost * echo "oh, hi Mark"
      """.trimIndent(),
    )
    assertEquals(0, script.units.size)
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
  }

  @Test
  fun `test unknown let command's cases`() {
    configureByText("\n")
    val script = VimscriptParser.parse(
      """
        let x[a, b; c] = something()
      """.trimIndent(),
    )
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
    assertEquals(1, script.units.size)
    assertFalse((script.units[0] as LetCommand).isSyntaxSupported)
  }

  @Test
  fun `test ignore commands between comments`() {
    configureByText("\n")
    val script = VimscriptParser.parse(
      """
        echo 1
        " ideaVim ignore
        echo 2
        " ideaVim ignore end

        echo 3

        "IdeaVim ignore
        echo 4
        echo 5
        "ideaVim ignore end

        echo 6

        "ideaVim ignore
        fa;sdlk 
        *(-78fa=09*&
        dfas;dlkfj afjldkfja s;d
        "ideaVim ignore end
      """.trimIndent(),
    )
    assertEquals(3, script.units.size)
    assertTrue(script.units[0] is EchoCommand)
    assertEquals(SimpleExpression(1), (script.units[0] as EchoCommand).args[0])
    assertTrue(script.units[1] is EchoCommand)
    assertEquals(SimpleExpression(3), (script.units[1] as EchoCommand).args[0])
    assertTrue(script.units[2] is EchoCommand)
    assertEquals(SimpleExpression(6), (script.units[2] as EchoCommand).args[0])
    assertTrue(IdeavimErrorListener.testLogger.isEmpty())
  }

  @Test
  fun `test finish statement`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
        let x = 3 |
        finish |
        let x = 10
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("echo x"))
    assertExOutput("3")
  }

  @Test
  fun `test all the lines are executed`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        """
          if has('ide') |
           set unknowOption |
           let x = 42 |
          endif
        """.trimIndent(),
      ),
    )
    typeText(commandToKeys("echo x"))
    assertExOutput("42")
  }

  @Test
  fun `test carriage return in the end of a command`() {
    val command = VimscriptParser.parseCommand("normal /search\r")
    assertTrue(command is NormalCommand)
    assertEquals("/search\r", (command as NormalCommand).argument)
  }

  @Test
  fun `test carriage return in the end of a script`() {
    val script = VimscriptParser.parse("normal /search\r")
    assertEquals(1, script.units.size)
    val command = script.units[0]
    assertTrue(command is NormalCommand)
    assertEquals("/search\r", (command as NormalCommand).argument)
  }
}
