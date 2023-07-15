/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionScope
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("SpellCheckingInspection")
class SetCommandTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test unknown option`() {
    enterCommand("set unknownOption")
    assertPluginError(true)
    assertPluginErrorMessageContains("Unknown option: unknownOption")
  }

  @Test
  fun `test toggle option`() {
    enterCommand("set rnu")
    assertTrue(options().relativenumber)
    enterCommand("set rnu!")
    assertFalse(options().relativenumber)
  }

  @Test
  fun `test number option`() {
    enterCommand("set scrolloff&")
    assertEquals(0, options().scrolloff)
    assertCommandOutput("set scrolloff?", "  scrolloff=0\n")
    enterCommand("set scrolloff=5")
    assertEquals(5, options().scrolloff)
    assertCommandOutput("set scrolloff?", "  scrolloff=5\n")
  }

  @Test
  fun `test toggle option as a number`() {
    enterCommand("set number&")
    assertEquals(0, injector.optionGroup.getOptionValue(Options.number, OptionScope.GLOBAL).asDouble().toInt())
    assertCommandOutput("set number?", "nonumber\n")
    enterCommand("let &nu=1000")
    assertEquals(1000, injector.optionGroup.getOptionValue(Options.number, OptionScope.GLOBAL).asDouble().toInt())
    assertCommandOutput("set number?", "  number\n")
  }

  @Test
  fun `test toggle option exceptions`() {
    enterCommand("set number+=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number+=10")
    enterCommand("set number+=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number+=test")

    enterCommand("set number^=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number^=10")
    enterCommand("set number^=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number^=test")

    enterCommand("set number-=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number-=10")
    enterCommand("set number-=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number-=test")
  }

  @Test
  fun `test number option exceptions`() {
    enterCommand("set scrolloff+=10")
    assertPluginError(false)
    enterCommand("set scrolloff+=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scrolloff+=test")

    enterCommand("set scrolloff^=10")
    assertPluginError(false)
    enterCommand("set scrolloff^=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scrolloff^=test")

    enterCommand("set scrolloff-=10")
    assertPluginError(false)
    enterCommand("set scrolloff-=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scrolloff-=test")
  }

  @Test
  fun `test string option`() {
    enterCommand("set selection&")
    assertEquals("inclusive", options().selection)
    assertCommandOutput("set selection?", "  selection=inclusive\n")
    enterCommand("set selection=exclusive")
    assertEquals("exclusive", options().selection)
    assertCommandOutput("set selection?", "  selection=exclusive\n")
  }

  @Test
  fun `test show numbered value`() {
    assertCommandOutput("set so", "  scrolloff=0\n")
  }

  @Test
  fun `test show numbered value with question mark`() {
    assertCommandOutput("set so?", "  scrolloff=0\n")
  }

  @Test
  fun `test show all modified effective option values`() {
    enterCommand("set number relativenumber scrolloff nrformats")
    assertCommandOutput("set",
      """
        |--- Options ---
        |  ideastrictmode      number              relativenumber
        |
      """.trimMargin())
  }

  @Test
  fun `test show all effective option values`() {
    assertCommandOutput("set all",
      """
        |--- Options ---
        |noargtextobj          ideawrite=all       scrolljump=1      notextobj-indent
        |  closenotebooks    noignorecase          scrolloff=0         timeout
        |nocommentary        noincsearch           selectmode=         timeoutlen=1000
        |nodigraph           nomatchit             shellcmdflag=-c   notrackactionids
        |noexchange            maxmapdepth=20      shellxescape=       undolevels=1000
        |nogdefault            more                shellxquote=        unifyjumps
        |nohighlightedyank   nomultiple-cursors    showcmd             virtualedit=
        |  history=50        noNERDTree            showmode          novisualbell
        |nohlsearch            nrformats=hex       sidescroll=0        visualdelay=100
        |noideaglobalmode    nonumber              sidescrolloff=0     whichwrap=b,s
        |noideajoin          nooctopushandler    nosmartcase           wrapscan
        |  ideamarks           oldundo             startofline
        |  ideastrictmode    norelativenumber    nosurround
        |noideatracetime       scroll=0          notextobj-entire
        |  clipboard=ideaput,autoselect,exclude:cons\|linux
        |  excommandannotation
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
        |  shell=/usr/local/bin/bash
        |novim-paragraph-motion
        |  viminfo='100,<50,s10,h
        |  vimscriptfunctionannotation
        |
      """.trimMargin())
  }

  @Test
  fun `test show named options`() {
    assertCommandOutput("set number? relativenumber? scrolloff? nrformats?", """
      |  nrformats=hex     nonumber            norelativenumber      scrolloff=0
      |""".trimMargin()
    )
  }

  @Test
  fun `test show all modified option values in single column`() {
    enterCommand("set number relativenumber scrolloff nrformats")
    assertCommandOutput("set!",
      """
      |--- Options ---
      |  ideastrictmode
      |  number
      |  relativenumber
      |""".trimMargin()
    )
  }

  @Test
  fun `test show all option values in single column`() {
    assertCommandOutput("set! all", """
      |--- Options ---
      |noargtextobj
      |  clipboard=ideaput,autoselect,exclude:cons\|linux
      |  closenotebooks
      |nocommentary
      |nodigraph
      |noexchange
      |  excommandannotation
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
      |  ideastrictmode
      |noideatracetime
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
      |nooctopushandler
      |  oldundo
      |norelativenumber
      |noReplaceWithRegister
      |  scroll=0
      |  scrolljump=1
      |  scrolloff=0
      |  selection=inclusive
      |  selectmode=
      |  shell=/usr/local/bin/bash
      |  shellcmdflag=-c
      |  shellxescape=
      |  shellxquote=
      |  showcmd
      |  showmode
      |  sidescroll=0
      |  sidescrolloff=0
      |nosmartcase
      |  startofline
      |nosurround
      |notextobj-entire
      |notextobj-indent
      |  timeout
      |  timeoutlen=1000
      |notrackactionids
      |  undolevels=1000
      |  unifyjumps
      |novim-paragraph-motion
      |  viminfo='100,<50,s10,h
      |  vimscriptfunctionannotation
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
    assertCommandOutput("set! number? relativenumber? scrolloff? nrformats?", """
      |  nrformats=hex
      |nonumber
      |norelativenumber
      |  scrolloff=0
      |""".trimMargin()
    )
  }
}
