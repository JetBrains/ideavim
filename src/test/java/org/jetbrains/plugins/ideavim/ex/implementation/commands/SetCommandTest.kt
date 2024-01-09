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
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionAccessScope
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
class SetCommandTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  private fun setOsSpecificOptionsToSafeValues() {
    enterCommand("set shell=/dummy/path/to/bash")
    enterCommand("set shellcmdflag=-x")
    enterCommand("set shellxescape=@")
    enterCommand("set shellxquote={")
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
    enterCommand("set nornu")
    assertFalse(options().relativenumber)
    enterCommand("set rnu!")
    assertTrue(options().relativenumber)
    enterCommand("set invrnu")
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
    enterCommand("set number&")   // Local to window. Reset local + per-window "global" value to default: nonu
    assertEquals(0, injector.optionGroup.getOptionValue(Options.number, OptionAccessScope.LOCAL(fixture.editor.vim)).asDouble().toInt())
    assertCommandOutput("set number?", "nonumber\n")

    // Should have the same effect as `:set` (although `:set` doesn't allow assigning a number to a boolean)
    // I.e. this sets the local value and the per-window "global" value
    enterCommand("let &nu=1000")
    assertEquals(1000, injector.optionGroup.getOptionValue(Options.number, OptionAccessScope.GLOBAL(fixture.editor.vim)).asDouble().toInt())
    assertEquals(1000, injector.optionGroup.getOptionValue(Options.number, OptionAccessScope.LOCAL(fixture.editor.vim)).asDouble().toInt())
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
        |  number              relativenumber
        |
      """.trimMargin())
  }

  @Test
  fun `test show all effective option values`() {
    setOsSpecificOptionsToSafeValues()
    assertCommandOutput("set all",
      """
        |--- Options ---
        |noargtextobj        noignorecase          scrolloff=0       notextobj-indent
        |nobreakindent       noincsearch           selectmode=         timeout
        |nocommentary        nolist                shellcmdflag=-x     timeoutlen=1000
        |nocursorline        nomatchit             shellxescape=@    notrackactionids
        |nodigraph             maxmapdepth=20      shellxquote={       undolevels=1000
        |noexchange            more                showcmd             virtualedit=
        |nogdefault          nomultiple-cursors    showmode          novisualbell
        |nohighlightedyank   noNERDTree            sidescroll=0        visualdelay=100
        |  history=50          nrformats=hex       sidescrolloff=0     whichwrap=b,s
        |nohlsearch          nonumber            nosmartcase           wrap
        |noideaglobalmode      operatorfunc=     nosneak               wrapscan
        |noideajoin          norelativenumber      startofline
        |  ideamarks           scroll=0          nosurround
        |  ideawrite=all       scrolljump=1      notextobj-entire
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
      |  number
      |  relativenumber
      |""".trimMargin()
    )
  }

  @Test
  fun `test show all option values in single column`() {
    setOsSpecificOptionsToSafeValues()
    assertCommandOutput("set! all", """
      |--- Options ---
      |noargtextobj
      |nobreakindent
      |  clipboard=ideaput,autoselect,exclude:cons\|linux
      |nocommentary
      |nocursorline
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
      |nolist
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
      |  wrap
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

  @Test
  fun `test reset local value for local-to-buffer option`() {
    enterCommand("set nrformats=octal")

    enterCommand("set nrformats&")

    assertCommandOutput("set nrformats?", "  nrformats=hex\n")
  }

  @Test
  fun `test reset local value for global-local option`() {
    enterCommand("set virtualedit=block") // Sets the global + effective values. Local is unset
    enterCommand("setlocal virtualedit=onemore")  // Sets the local + effective values
    assertCommandOutput("set virtualedit?", "  virtualedit=onemore\n")
    assertCommandOutput("setlocal virtualedit?", "  virtualedit=onemore\n")

    // This is like setting the global-local value to its own global value. :set with a global-local option will set the
    // global value and unset the local value
    enterCommand("set virtualedit<")

    assertCommandOutput("set virtualedit?", "  virtualedit=block\n")
    assertCommandOutput("setlocal virtualedit?", "  virtualedit=\n")
  }
}
