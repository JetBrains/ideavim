/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.commands.SetglobalCommand
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("SpellCheckingInspection")
@TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
class SetglobalCommandTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  private fun setOsSpecificOptionsToSafeValues() {
    enterCommand("setglobal shell=/dummy/path/to/bash")
    enterCommand("setglobal shellcmdflag=-x")
    enterCommand("setglobal shellxescape=@")
    enterCommand("setglobal shellxquote={")
  }

  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("setglobal nu")
    assertTrue(command is SetglobalCommand)
    assertEquals("nu", command.argument)
  }

  @Test
  fun `test set unknown option`() {
    enterCommand("setglobal unknownOption")
    assertPluginError(true)
    assertPluginErrorMessageContains("Unknown option: unknownOption")
  }

  @Test
  fun `test set toggle option global value`() {
    enterCommand("setglobal rnu")
    assertCommandOutput("setglobal rnu?", "  relativenumber\n")
    assertCommandOutput("setlocal rnu?", "norelativenumber\n")

    enterCommand("setglobal nornu")
    assertCommandOutput("setglobal rnu?", "norelativenumber\n")
    assertCommandOutput("setlocal rnu?", "norelativenumber\n")

    enterCommand("setglobal rnu!")
    assertCommandOutput("setglobal rnu?", "  relativenumber\n")
    assertCommandOutput("setlocal rnu?", "norelativenumber\n")

    enterCommand("setglobal invrnu")
    assertCommandOutput("setglobal rnu?", "norelativenumber\n")
    assertCommandOutput("setlocal rnu?", "norelativenumber\n")
  }

  @Test
  fun `test exceptions from incorrectly setting toggle option`() {
    enterCommand("setglobal number=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number=test")

    enterCommand("setglobal number=0")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number=0")

    enterCommand("setglobal number+=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number+=10")

    enterCommand("setglobal number+=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number+=test")

    enterCommand("setglobal number^=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number^=10")

    enterCommand("setglobal number^=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number^=test")

    enterCommand("setglobal number-=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number-=10")

    enterCommand("setglobal number-=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number-=test")
  }

  @Test
  fun `test show toggle option global value`() {
    assertCommandOutput("setglobal rnu?", "norelativenumber\n")

    enterCommand("setglobal invrnu")
    assertCommandOutput("setglobal rnu?", "  relativenumber\n")
  }

  @Test
  fun `test reset global toggle option value to global value does nothing`() {
    enterCommand("setglobal relativenumber") // Default global value is off
    assertCommandOutput("setglobal rnu?", "  relativenumber\n")

    // Copies the global value to itself, doesn't change anything
    enterCommand("setglobal relativenumber<")
    assertCommandOutput("setglobal rnu?", "  relativenumber\n")
  }

  @Test
  fun `test reset global-local toggle option to global value does nothing`() {
    val option = ToggleOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", false)
    try {
      injector.optionGroup.addOption(option)

      enterCommand("setglobal test")
      assertCommandOutput("setglobal test?", "  test\n")

      // Copies the global value to the target scope (i.e. global, this is a no-op)
      enterCommand("setglobal test<")

      assertCommandOutput("setglobal test?", "  test\n")
    }
    finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test reset toggle option to default value`() {
    enterCommand("setglobal rnu")
    assertCommandOutput("setglobal rnu?", "  relativenumber\n")

    enterCommand("setglobal rnu&")
    assertCommandOutput("setglobal rnu?", "norelativenumber\n")
  }

  @Test
  fun `test set number option global value`() {
    enterCommand("setglobal scroll&")
    assertCommandOutput("setglobal scroll?", "  scroll=0\n")
    assertCommandOutput("setlocal scroll?", "  scroll=0\n")

    enterCommand("setglobal scroll=5")
    assertCommandOutput("setglobal scroll?", "  scroll=5\n")
    assertCommandOutput("setlocal scroll?", "  scroll=0\n")

    enterCommand("setglobal scroll:10")
    assertCommandOutput("setglobal scroll?", "  scroll=10\n")
    assertCommandOutput("setlocal scroll?", "  scroll=0\n")

    enterCommand("setglobal scroll+=5")
    assertCommandOutput("setglobal scroll?", "  scroll=15\n")
    assertCommandOutput("setlocal scroll?", "  scroll=0\n")

    enterCommand("setglobal scroll-=10")
    assertCommandOutput("setglobal scroll?", "  scroll=5\n")
    assertCommandOutput("setlocal scroll?", "  scroll=0\n")

    enterCommand("setglobal scroll^=2")
    assertCommandOutput("setglobal scroll?", "  scroll=10\n")
    assertCommandOutput("setlocal scroll?", "  scroll=0\n")
  }

  @Test
  fun `test exceptions from incorrectly setting number option`() {
    enterCommand("setglobal scroll=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scroll=test")

    enterCommand("setglobal scroll+=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scroll+=test")

    enterCommand("setglobal scroll-=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scroll-=test")

    enterCommand("setglobal scroll^=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scroll^=test")
  }

  @Test
  fun `test show number option global value`() {
    enterCommand("setglobal scroll=10")
    assertCommandOutput("setglobal scroll?", "  scroll=10\n")
  }

  @Test
  fun `test number option with no arguments shows current global value`() {
    enterCommand("setglobal scroll=10")
    assertCommandOutput("setglobal scroll", "  scroll=10\n")
  }

  @Test
  fun `test reset global number option value to global value does nothing`() {
    enterCommand("setglobal scroll=10")  // Default global value is 0

    enterCommand("setglobal scroll<")
    assertCommandOutput("setglobal scroll?", "  scroll=10\n")
  }

  @Test
  fun `test reset global-local number option to global value does nothing`() {
    val option = NumberOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", 10)
    try {
      injector.optionGroup.addOption(option)

      enterCommand("setglobal test=20")
      assertCommandOutput("setglobal test?", "  test=20\n")

      // setglobal {option}< copies the global value to the local value
      enterCommand("setglobal test<")

      assertCommandOutput("setglobal test?", "  test=20\n")
    }
    finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test set string option global value`() {
    enterCommand("setglobal nrformats=octal")
    assertCommandOutput("setglobal nrformats", "  nrformats=octal\n")
    assertCommandOutput("setlocal nrformats", "  nrformats=hex\n")

    enterCommand("setglobal nrformats:alpha")
    assertCommandOutput("setglobal nrformats", "  nrformats=alpha\n")
    assertCommandOutput("setlocal nrformats", "  nrformats=hex\n")

    enterCommand("setglobal nrformats+=hex")
    assertCommandOutput("setglobal nrformats", "  nrformats=alpha,hex\n")
    assertCommandOutput("setlocal nrformats", "  nrformats=hex\n")

    enterCommand("setglobal nrformats-=hex")
    assertCommandOutput("setglobal nrformats", "  nrformats=alpha\n")
    assertCommandOutput("setlocal nrformats", "  nrformats=hex\n")

    enterCommand("setglobal nrformats^=hex")
    assertCommandOutput("setglobal nrformats", "  nrformats=hex,alpha\n")
    assertCommandOutput("setlocal nrformats", "  nrformats=hex\n")
  }

  @Test
  fun `test exceptions from incorrectly setting string option`() {
    enterCommand("setglobal nrformats=unknown")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: nrformats=unknown")

    enterCommand("setglobal nrformats+=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: nrformats+=10")

    enterCommand("setglobal nrformats-=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: nrformats-=10")

    enterCommand("setglobal nrformats^=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: nrformats^=10")
  }

  @Test
  fun `test show string option global value`() {
    assertCommandOutput("setglobal nrformats?", "  nrformats=hex\n")

    enterCommand("setglobal nrformats+=alpha")
    assertCommandOutput("setglobal nrformats?", "  nrformats=hex,alpha\n")
  }

  @Test
  fun `test string option with no arguments shows current global value`() {
    assertCommandOutput("setglobal nrformats", "  nrformats=hex\n")
  }

  @Test
  fun `test reset global string option value to global value does nothing`() {
    enterCommand("setglobal nrformats=alpha")
    enterCommand("setglobal nrformats<")
    assertCommandOutput("setglobal nrformats?", "  nrformats=alpha\n")
  }

  @Test
  fun `test reset global-local string option to global value does nothing`() {
    val option = StringOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", "testValue")
    try {
      injector.optionGroup.addOption(option)

      // Copies the global value to the target scope (i.e. global, this is a no-op)
      enterCommand("setlocal test<")

      assertCommandOutput("setlocal test?", "  test=testValue\n")
    }
    finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test reset string option to default value`() {
    enterCommand("setglobal nrformats=alpha")
    enterCommand("setglobal nrformats&")
    assertCommandOutput("setglobal nrformats?", "  nrformats=hex\n")
  }

  @Test
  fun `test show all modified global option values`() {
    assertCommandOutput("setglobal", """
      |--- Global option values ---
      |""".trimMargin()
    )
  }

  @Test
  fun `test show all modified global option values 2`() {
    enterCommand("setglobal number relativenumber scrolloff=10 nrformats=alpha,hex,octal sidescrolloff=10")
    assertCommandOutput("setglobal", """
      |--- Global option values ---
      |  number              relativenumber      scrolloff=10        sidescrolloff=10
      |  nrformats=alpha,hex,octal
      |""".trimMargin()
    )
  }

  @Test
  fun `test show all global option values`() {
    setOsSpecificOptionsToSafeValues()
    assertCommandOutput("setglobal all", """
      |--- Global option values ---
      |noargtextobj        noincsearch           selectmode=       notextobj-indent
      |nocommentary        nomatchit             shellcmdflag=-x     timeout
      |nodigraph             maxmapdepth=20      shellxescape=@      timeoutlen=1000
      |noexchange            more                shellxquote={     notrackactionids
      |nogdefault          nomultiple-cursors    showcmd             undolevels=1000
      |nohighlightedyank   noNERDTree            showmode            virtualedit=
      |  history=50          nrformats=hex       sidescroll=0      novisualbell
      |nohlsearch          nonumber              sidescrolloff=0     visualdelay=100
      |noideaglobalmode      operatorfunc=     nosmartcase           whichwrap=b,s
      |noideajoin          norelativenumber    nosneak               wrapscan
      |  ideamarks           scroll=0            startofline
      |  ideawrite=all       scrolljump=1      nosurround
      |noignorecase          scrolloff=0       notextobj-entire
      |  clipboard=ideaput,autoselect,exclude:cons\|linux
      |  guicursor=n-v-c:block-Cursor/lCursor,ve:ver35-Cursor,o:hor50-Cursor,i-ci:ver25-Cursor/lCursor,r-cr:hor20-Cursor/lCursor,sm:block-Cursor-blinkwait175-blinkoff150-blinkon175
      |  ide=IntelliJ IDEA Community Edition
      |noideacopypreprocess
      |  idearefactormode=select
      |  ideastatusicon=enabled
      |  ideavimsupport=dialog
      |  iskeyword=@,48-57,_
      |  keymodel=continueselect,stopselect
      |  lookupkeys=<Tab>,<Down>,<Up>,<Enter>,<Left>,<Right>,<C-Down>,<C-Up>,<PageUp>,<PageDown>,<C-J>,<C-Q>
      |  matchpairs=(:),{:},[:]
      |noReplaceWithRegister
      |  selection=inclusive
      |  shell=/dummy/path/to/bash
      |novim-paragraph-motion
      |  viminfo='100,<50,s10,h
      |""".trimMargin()
    )
  }

  @Test
  fun `test show named options`() {
    assertCommandOutput("setglobal number? relativenumber? scrolloff? nrformats?", """
      |  nrformats=hex     nonumber            norelativenumber      scrolloff=0
      |""".trimMargin()
    )
  }

  @Test
  fun `test show all modified global option values in single column`() {
    assertCommandOutput("setglobal!", """
      |--- Global option values ---
      |""".trimMargin()
    )
  }

  @Test
  fun `test show all modified global option values in single column 2`() {
    enterCommand("setglobal number relativenumber scrolloff=10 nrformats=alpha,hex,octal sidescrolloff=10")
    assertCommandOutput("setglobal!", """
      |--- Global option values ---
      |  nrformats=alpha,hex,octal
      |  number
      |  relativenumber
      |  scrolloff=10
      |  sidescrolloff=10
      |""".trimMargin()
    )
  }

  @Test
  fun `test show all global option values in single column`() {
    setOsSpecificOptionsToSafeValues()
    assertCommandOutput("setglobal! all", """
      |--- Global option values ---
      |noargtextobj
      |  clipboard=ideaput,autoselect,exclude:cons\|linux
      |nocommentary
      |nodigraph
      |noexchange
      |nogdefault
      |  guicursor=n-v-c:block-Cursor/lCursor,ve:ver35-Cursor,o:hor50-Cursor,i-ci:ver25-Cursor/lCursor,r-cr:hor20-Cursor/lCursor,sm:block-Cursor-blinkwait175-blinkoff150-blinkon175
      |nohighlightedyank
      |  history=50
      |nohlsearch
      |  ide=IntelliJ IDEA Community Edition
      |noideacopypreprocess
      |noideaglobalmode
      |noideajoin
      |  ideamarks
      |  idearefactormode=select
      |  ideastatusicon=enabled
      |  ideavimsupport=dialog
      |  ideawrite=all
      |noignorecase
      |noincsearch
      |  iskeyword=@,48-57,_
      |  keymodel=continueselect,stopselect
      |  lookupkeys=<Tab>,<Down>,<Up>,<Enter>,<Left>,<Right>,<C-Down>,<C-Up>,<PageUp>,<PageDown>,<C-J>,<C-Q>
      |nomatchit
      |  matchpairs=(:),{:},[:]
      |  maxmapdepth=20
      |  more
      |nomultiple-cursors
      |noNERDTree
      |  nrformats=hex
      |nonumber
      |  operatorfunc=
      |norelativenumber
      |noReplaceWithRegister
      |  scroll=0
      |  scrolljump=1
      |  scrolloff=0
      |  selection=inclusive
      |  selectmode=
      |  shell=/dummy/path/to/bash
      |  shellcmdflag=-x
      |  shellxescape=@
      |  shellxquote={
      |  showcmd
      |  showmode
      |  sidescroll=0
      |  sidescrolloff=0
      |nosmartcase
      |nosneak
      |  startofline
      |nosurround
      |notextobj-entire
      |notextobj-indent
      |  timeout
      |  timeoutlen=1000
      |notrackactionids
      |  undolevels=1000
      |novim-paragraph-motion
      |  viminfo='100,<50,s10,h
      |  virtualedit=
      |novisualbell
      |  visualdelay=100
      |  whichwrap=b,s
      |  wrapscan
      |""".trimMargin()
    )
  }

  @Test
  fun `test show named options in single column`() {
    assertCommandOutput("setglobal! number? relativenumber? scrolloff? nrformats?", """
      |  nrformats=hex
      |nonumber
      |norelativenumber
      |  scrolloff=0
      |""".trimMargin()
    )
  }
}
