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

@TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
class SetglobalCommandTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  private fun setOsSpecificOptionsToSafeValues() {
    enterCommand("set isfname=@,48-57,/,\\,.,-,_,+,,,#,$,%,{,},[,],:,@-@,!,~,=")
    enterCommand("setglobal fileformat=unix")
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
    assertPluginErrorMessage("E518: Unknown option: unknownOption")
  }

  @Test
  fun `test set toggle option global value`() {
    enterCommand("setglobal rnu")
    assertCommandOutput("setglobal rnu?", "  relativenumber")
    assertCommandOutput("setlocal rnu?", "norelativenumber")

    enterCommand("setglobal nornu")
    assertCommandOutput("setglobal rnu?", "norelativenumber")
    assertCommandOutput("setlocal rnu?", "norelativenumber")

    enterCommand("setglobal rnu!")
    assertCommandOutput("setglobal rnu?", "  relativenumber")
    assertCommandOutput("setlocal rnu?", "norelativenumber")

    enterCommand("setglobal invrnu")
    assertCommandOutput("setglobal rnu?", "norelativenumber")
    assertCommandOutput("setlocal rnu?", "norelativenumber")
  }

  @Test
  fun `test exceptions from incorrectly setting toggle option`() {
    enterCommand("setglobal number=test")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number=test")

    enterCommand("setglobal number=0")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number=0")

    enterCommand("setglobal number+=10")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number+=10")

    enterCommand("setglobal number+=test")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number+=test")

    enterCommand("setglobal number^=10")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number^=10")

    enterCommand("setglobal number^=test")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number^=test")

    enterCommand("setglobal number-=10")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number-=10")

    enterCommand("setglobal number-=test")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number-=test")
  }

  @Test
  fun `test show toggle option global value`() {
    assertCommandOutput("setglobal rnu?", "norelativenumber")

    enterCommand("setglobal invrnu")
    assertCommandOutput("setglobal rnu?", "  relativenumber")
  }

  @Test
  fun `test reset global toggle option value to default value`() {
    enterCommand("setglobal rnu") // Local option, global value
    assertCommandOutput("setglobal rnu?", "  relativenumber")

    enterCommand("setglobal rnu&")
    assertCommandOutput("setglobal rnu?", "norelativenumber")
  }

  @Test
  fun `test reset global toggle option value to global value does nothing`() {
    enterCommand("setglobal relativenumber") // Local option, global value
    assertCommandOutput("setglobal rnu?", "  relativenumber")

    // Copies the global value to itself, doesn't change anything
    enterCommand("setglobal relativenumber<")
    assertCommandOutput("setglobal rnu?", "  relativenumber")
  }

  @Test
  fun `test reset global-local toggle option to default value`() {
    val option = ToggleOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", false)
    try {
      injector.optionGroup.addOption(option)

      enterCommand("setglobal test")
      enterCommand("setlocal notest")
      assertCommandOutput("setglobal test?", "  test")
      assertCommandOutput("setlocal test?", "notest")

      // Reset global value to default
      enterCommand("setglobal test&")

      assertCommandOutput("setglobal test?", "notest")
      assertCommandOutput("setlocal test?", "notest")
    } finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test reset global-local toggle option to global value does nothing`() {
    val option = ToggleOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", false)
    try {
      injector.optionGroup.addOption(option)

      enterCommand("setglobal test")
      enterCommand("setlocal notest")
      assertCommandOutput("setglobal test?", "  test")
      assertCommandOutput("setlocal test?", "notest")

      // Copies the global value to the target scope (i.e. global, this is a no-op)
      enterCommand("setglobal test<")

      assertCommandOutput("setglobal test?", "  test")
      assertCommandOutput("setlocal test?", "notest")
    } finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test set number option global value`() {
    enterCommand("setglobal scroll&")
    assertCommandOutput("setglobal scroll?", "  scroll=0")
    assertCommandOutput("setlocal scroll?", "  scroll=0")

    enterCommand("setglobal scroll=5")
    assertCommandOutput("setglobal scroll?", "  scroll=5")
    assertCommandOutput("setlocal scroll?", "  scroll=0")

    enterCommand("setglobal scroll:10")
    assertCommandOutput("setglobal scroll?", "  scroll=10")
    assertCommandOutput("setlocal scroll?", "  scroll=0")

    enterCommand("setglobal scroll+=5")
    assertCommandOutput("setglobal scroll?", "  scroll=15")
    assertCommandOutput("setlocal scroll?", "  scroll=0")

    enterCommand("setglobal scroll-=10")
    assertCommandOutput("setglobal scroll?", "  scroll=5")
    assertCommandOutput("setlocal scroll?", "  scroll=0")

    enterCommand("setglobal scroll^=2")
    assertCommandOutput("setglobal scroll?", "  scroll=10")
    assertCommandOutput("setlocal scroll?", "  scroll=0")
  }

  @Test
  fun `test exceptions from incorrectly setting number option`() {
    enterCommand("setglobal scroll=test")
    assertPluginError(true)
    assertPluginErrorMessage("E521: Number required after =: scroll=test")

    enterCommand("setglobal scroll+=test")
    assertPluginError(true)
    assertPluginErrorMessage("E521: Number required after =: scroll+=test")

    enterCommand("setglobal scroll-=test")
    assertPluginError(true)
    assertPluginErrorMessage("E521: Number required after =: scroll-=test")

    enterCommand("setglobal scroll^=test")
    assertPluginError(true)
    assertPluginErrorMessage("E521: Number required after =: scroll^=test")
  }

  @Test
  fun `test show number option global value`() {
    enterCommand("setglobal scroll=10")
    assertCommandOutput("setglobal scroll?", "  scroll=10")
  }

  @Test
  fun `test number option with no arguments shows current global value`() {
    enterCommand("setglobal scroll=10")
    assertCommandOutput("setglobal scroll", "  scroll=10")
  }

  @Test
  fun `test reset number global option value to default value`() {
    enterCommand("setglobal scroll=10")  // Default global value is 0

    enterCommand("setglobal scroll&")
    assertCommandOutput("setglobal scroll?", "  scroll=0")
  }

  @Test
  fun `test reset number global option value to global value does nothing`() {
    enterCommand("setglobal scroll=10")  // Default global value is 0

    enterCommand("setglobal scroll<")
    assertCommandOutput("setglobal scroll?", "  scroll=10")
  }

  @Test
  fun `test reset number global-local option to default value`() {
    val option = NumberOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", 10)
    try {
      injector.optionGroup.addOption(option)

      enterCommand("setglobal test=20")
      enterCommand("setlocal test=30")
      assertCommandOutput("setglobal test?", "  test=20")
      assertCommandOutput("setlocal test?", "  test=30")

      // setglobal {option}< copies the global value to the target scope
      enterCommand("setglobal test&")

      assertCommandOutput("setglobal test?", "  test=10")
      assertCommandOutput("setlocal test?", "  test=30")
    } finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test reset number global-local option to global value does nothing`() {
    val option = NumberOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", 10)
    try {
      injector.optionGroup.addOption(option)

      enterCommand("setglobal test=20")
      enterCommand("setlocal test=30")
      assertCommandOutput("setglobal test?", "  test=20")
      assertCommandOutput("setlocal test?", "  test=30")

      // setglobal {option}< copies the global value to the target scope
      enterCommand("setglobal test<")

      assertCommandOutput("setglobal test?", "  test=20")
      assertCommandOutput("setlocal test?", "  test=30")
    } finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test set string option global value`() {
    enterCommand("setglobal nrformats=octal")
    assertCommandOutput("setglobal nrformats", "  nrformats=octal")
    assertCommandOutput("setlocal nrformats", "  nrformats=hex")

    enterCommand("setglobal nrformats:alpha")
    assertCommandOutput("setglobal nrformats", "  nrformats=alpha")
    assertCommandOutput("setlocal nrformats", "  nrformats=hex")

    enterCommand("setglobal nrformats+=hex")
    assertCommandOutput("setglobal nrformats", "  nrformats=alpha,hex")
    assertCommandOutput("setlocal nrformats", "  nrformats=hex")

    enterCommand("setglobal nrformats-=hex")
    assertCommandOutput("setglobal nrformats", "  nrformats=alpha")
    assertCommandOutput("setlocal nrformats", "  nrformats=hex")

    enterCommand("setglobal nrformats^=hex")
    assertCommandOutput("setglobal nrformats", "  nrformats=hex,alpha")
    assertCommandOutput("setlocal nrformats", "  nrformats=hex")
  }

  @Test
  fun `test exceptions from incorrectly setting string option`() {
    enterCommand("setglobal nrformats=unknown")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: nrformats=unknown")

    enterCommand("setglobal nrformats+=10")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: nrformats+=10")

    enterCommand("setglobal nrformats-=10")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: nrformats-=10")

    enterCommand("setglobal nrformats^=10")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: nrformats^=10")
  }

  @Test
  fun `test show string option global value`() {
    assertCommandOutput("setglobal nrformats?", "  nrformats=hex")

    enterCommand("setglobal nrformats+=alpha")
    assertCommandOutput("setglobal nrformats?", "  nrformats=hex,alpha")
  }

  @Test
  fun `test string option with no arguments shows current global value`() {
    assertCommandOutput("setglobal nrformats", "  nrformats=hex")
  }

  @Test
  fun `test reset string global option value to default value`() {
    enterCommand("setglobal nrformats=alpha")
    enterCommand("setglobal nrformats&")
    assertCommandOutput("setglobal nrformats?", "  nrformats=hex")
  }

  @Test
  fun `test reset string global option value to global value does nothing`() {
    enterCommand("setglobal nrformats=alpha")
    enterCommand("setglobal nrformats<")
    assertCommandOutput("setglobal nrformats?", "  nrformats=alpha")
  }

  @Test
  fun `test reset string global-local option to default value`() {
    val option = StringOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", "testValue")
    try {
      injector.optionGroup.addOption(option)

      enterCommand("setglobal test=globalValue")
      enterCommand("setlocal test=localValue")

      // Copies the default value to the target scope
      enterCommand("setglobal test&")

      assertCommandOutput("setglobal test?", "  test=testValue")
    } finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test reset string global-local option to global value does nothing`() {
    val option = StringOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", "testValue")
    try {
      injector.optionGroup.addOption(option)

      enterCommand("setglobal test=globalValue")
      enterCommand("setlocal test=localValue")

      // Copies the global value to the target scope (i.e. global, this is a no-op)
      enterCommand("setglobal test<")

      assertCommandOutput("setglobal test?", "  test=globalValue")
    } finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test reset string option to default value`() {
    enterCommand("setglobal nrformats=alpha")
    enterCommand("setglobal nrformats&")
    assertCommandOutput("setglobal nrformats?", "  nrformats=hex")
  }

  @Test
  fun `test show all modified global option values`() {
    assertCommandOutput(
      "setglobal", """
      |--- Global option values ---
      """.trimMargin()
    )
  }

  @Test
  fun `test show all modified global option values 2`() {
    enterCommand("setglobal number relativenumber scrolloff=10 nrformats=alpha,hex,octal sidescrolloff=10")
    assertCommandOutput(
      "setglobal", """
      |--- Global option values ---
      |  number              relativenumber      scrolloff=10        sidescrolloff=10
      |  nrformats=alpha,hex,octal
      """.trimMargin()
    )
  }

  @Test
  fun `test show all global option values`() {
    setOsSpecificOptionsToSafeValues()
    val expected = """
    |--- Global option values ---
    |noargtextobj        noideajoin          norelativenumber    nosurround
    |nobomb                ideamarks           scroll=0          notextobj-entire
    |nobreakindent         ideawrite=all       scrolljump=1      notextobj-indent
    |  colorcolumn=      noignorecase          scrolloff=0         textwidth=0
    |nocommentary        noincsearch           selectmode=         timeout
    |nocursorline        nolist                shellcmdflag=-x     timeoutlen=1000
    |nodigraph           nomatchit             shellxescape=@    notrackactionids
    |noexchange            maxmapdepth=20      shellxquote={       undolevels=1000
    |  fileencoding=     nomini-ai             showcmd             virtualedit=
    |  fileformat=unix     more                showmode          novisualbell
    |nogdefault          nomultiple-cursors    sidescroll=0        visualdelay=100
    |nohighlightedyank   noNERDTree            sidescrolloff=0     whichwrap=b,s
    |  history=50          nrformats=hex     nosmartcase           wrap
    |nohlsearch          nonumber            nosneak               wrapscan
    |  ide=IntelliJ IDEA   operatorfunc=       startofline
    |  clipboard=ideaput,autoselect
    |  guicursor=n-v-c:block-Cursor/lCursor,ve:ver35-Cursor,o:hor50-Cursor,i-ci:ver25-Cursor/lCursor,r-cr:hor20-Cursor/lCursor,sm:block-Cursor-blinkwait175-blinkoff150-blinkon175
    |noideacopypreprocess
    |  idearefactormode=select
    |  ideastatusicon=enabled
    |  ideavimsupport=dialog
    |  isfname=@,48-57,/,\,.,-,_,+,,,#,$,%,{,},[,],:,@-@,!,~,=
    |  iskeyword=@,48-57,_
    |  keymodel=continueselect,stopselect
    |  lookupkeys=<Tab>,<Down>,<Up>,<Enter>,<Left>,<Right>,<C-Down>,<C-Up>,<PageUp>,<PageDown>,<C-J>,<C-Q>
    |  matchpairs=(:),{:},[:]
    |noNERDTreeEverywhere
    |noReplaceWithRegister
    |  selection=inclusive
    |  shell=/dummy/path/to/bash
    |novim-paragraph-motion
    |  viminfo='100,<50,s10,h
    """.trimMargin()
    assertCommandOutput("setglobal all", expected)
  }

  @Test
  fun `test show named options`() {
    assertCommandOutput(
      "setglobal number? relativenumber? scrolloff? nrformats?", """
      |  nrformats=hex     nonumber            norelativenumber      scrolloff=0
      """.trimMargin()
    )
  }

  @Test
  fun `test show all modified global option values in single column`() {
    assertCommandOutput(
      "setglobal!", """
      |--- Global option values ---
      """.trimMargin()
    )
  }

  @Test
  fun `test show all modified global option values in single column 2`() {
    enterCommand("setglobal number relativenumber scrolloff=10 nrformats=alpha,hex,octal sidescrolloff=10")
    assertCommandOutput(
      "setglobal!", """
      |--- Global option values ---
      |  nrformats=alpha,hex,octal
      |  number
      |  relativenumber
      |  scrolloff=10
      |  sidescrolloff=10
      """.trimMargin()
    )
  }

  @Test
  fun `test show all global option values in single column`() {
    setOsSpecificOptionsToSafeValues()
    val expected = """
    |--- Global option values ---
    |noargtextobj
    |nobomb
    |nobreakindent
    |  clipboard=ideaput,autoselect
    |  colorcolumn=
    |nocommentary
    |nocursorline
    |nodigraph
    |noexchange
    |  fileencoding=
    |  fileformat=unix
    |nogdefault
    |  guicursor=n-v-c:block-Cursor/lCursor,ve:ver35-Cursor,o:hor50-Cursor,i-ci:ver25-Cursor/lCursor,r-cr:hor20-Cursor/lCursor,sm:block-Cursor-blinkwait175-blinkoff150-blinkon175
    |nohighlightedyank
    |  history=50
    |nohlsearch
    |  ide=IntelliJ IDEA
    |noideacopypreprocess
    |noideajoin
    |  ideamarks
    |  idearefactormode=select
    |  ideastatusicon=enabled
    |  ideavimsupport=dialog
    |  ideawrite=all
    |noignorecase
    |noincsearch
    |  isfname=@,48-57,/,\,.,-,_,+,,,#,$,%,{,},[,],:,@-@,!,~,=
    |  iskeyword=@,48-57,_
    |  keymodel=continueselect,stopselect
    |nolist
    |  lookupkeys=<Tab>,<Down>,<Up>,<Enter>,<Left>,<Right>,<C-Down>,<C-Up>,<PageUp>,<PageDown>,<C-J>,<C-Q>
    |nomatchit
    |  matchpairs=(:),{:},[:]
    |  maxmapdepth=20
    |nomini-ai
    |  more
    |nomultiple-cursors
    |noNERDTree
    |noNERDTreeEverywhere
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
    |  textwidth=0
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
    |  wrap
    |  wrapscan
    """.trimMargin()
    assertCommandOutput("setglobal! all", expected)
  }

  @Test
  fun `test show named options in single column`() {
    assertCommandOutput(
      "setglobal! number? relativenumber? scrolloff? nrformats?", """
      |  nrformats=hex
      |nonumber
      |norelativenumber
      |  scrolloff=0
      """.trimMargin()
    )
  }
}
