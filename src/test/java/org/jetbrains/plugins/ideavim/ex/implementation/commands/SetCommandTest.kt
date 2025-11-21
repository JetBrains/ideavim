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
    enterCommand("set isfname=@,48-57,/,\\,.,-,_,+,,,#,$,%,{,},[,],:,@-@,!,~,=")
    enterCommand("set shell=/dummy/path/to/bash")
    enterCommand("set shellcmdflag=-x")
    enterCommand("set shellxescape=@")
    enterCommand("set shellxquote={")
  }

  @Test
  fun `test unknown option`() {
    enterCommand("set unknownOption")
    assertPluginError(true)
    assertPluginErrorMessage("E518: Unknown option: unknownOption")
  }

  @Test
  fun `test toggle option`() {
    enterCommand("set rnu")
    assertTrue(optionsIj().relativenumber)
    enterCommand("set nornu")
    assertFalse(optionsIj().relativenumber)
    enterCommand("set rnu!")
    assertTrue(optionsIj().relativenumber)
    enterCommand("set invrnu")
    assertFalse(optionsIj().relativenumber)
  }

  @Test
  fun `test number option`() {
    enterCommand("set scrolloff&")
    assertEquals(0, options().scrolloff)
    assertCommandOutput("set scrolloff?", "  scrolloff=0")
    enterCommand("set scrolloff=5")
    assertEquals(5, options().scrolloff)
    assertCommandOutput("set scrolloff?", "  scrolloff=5")
  }

  @Test
  fun `test toggle option as a number`() {
    enterCommand("set digraph&")   // Local to window. Reset local + per-window "global" value to default: nodigraph
    assertEquals(0,
      injector.optionGroup.getOptionValue(Options.digraph, OptionAccessScope.LOCAL(fixture.editor.vim)).value
    )
    assertCommandOutput("set digraph?", "nodigraph")

    // Should have the same effect as `:set` (although `:set` doesn't allow assigning a number to a boolean)
    // I.e. this sets the local value and the per-window "global" value
    enterCommand("let &dg=1000")
    assertEquals(1000,
      injector.optionGroup.getOptionValue(Options.digraph, OptionAccessScope.GLOBAL(fixture.editor.vim)).value
    )
    assertEquals(1000,
      injector.optionGroup.getOptionValue(Options.digraph, OptionAccessScope.LOCAL(fixture.editor.vim)).value
    )
    assertCommandOutput("set digraph?", "  digraph")
  }

  @Test
  fun `test toggle option exceptions`() {
    enterCommand("set number+=10")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number+=10")
    enterCommand("set number+=test")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number+=test")

    enterCommand("set number^=10")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number^=10")
    enterCommand("set number^=test")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number^=test")

    enterCommand("set number-=10")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number-=10")
    enterCommand("set number-=test")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: number-=test")
  }

  @Test
  fun `test number option exceptions`() {
    enterCommand("set scrolloff+=10")
    assertPluginError(false)
    enterCommand("set scrolloff+=test")
    assertPluginError(true)
    assertPluginErrorMessage("E521: Number required after =: scrolloff+=test")

    enterCommand("set scrolloff^=10")
    assertPluginError(false)
    enterCommand("set scrolloff^=test")
    assertPluginError(true)
    assertPluginErrorMessage("E521: Number required after =: scrolloff^=test")

    enterCommand("set scrolloff-=10")
    assertPluginError(false)
    enterCommand("set scrolloff-=test")
    assertPluginError(true)
    assertPluginErrorMessage("E521: Number required after =: scrolloff-=test")
  }

  @Test
  fun `test string option`() {
    enterCommand("set selection&")
    assertEquals("inclusive", options().selection)
    assertCommandOutput("set selection?", "  selection=inclusive")
    enterCommand("set selection=exclusive")
    assertEquals("exclusive", options().selection)
    assertCommandOutput("set selection?", "  selection=exclusive")
  }

  @Test
  fun `test show numbered value`() {
    assertCommandOutput("set so", "  scrolloff=0")
  }

  @Test
  fun `test show numbered value with question mark`() {
    assertCommandOutput("set so?", "  scrolloff=0")
  }

  @Test
  fun `test show all modified effective option values`() {
    // 'fileformat' will always be "unix" because the platform normalises line endings to `\n`, but the default is
    // different for Mac/Unix and DOS. For the sake of tests, reset to the OS default
    enterCommand("setlocal fileformat&")

    // 'fileencoding' defaults to "", but is automatically detected as UTF-8
    enterCommand("set number relativenumber scrolloff nrformats")
    assertExOutput("  nrformats=hex       scrolloff=0")
    injector.outputPanel.getCurrentOutputPanel()?.close()
    assertCommandOutput(
      "set",
      """
        |--- Options ---
        |  number              relativenumber
        |  fileencoding=utf-8
      """.trimMargin()
    )
  }

  @Test
  fun `test show all effective option values`() {
    // 'fileencoding' defaults to "", but is automatically detected as UTF-8
    setOsSpecificOptionsToSafeValues()
    assertCommandOutput(
      "set all",
      """
        |--- Options ---
        |noargtextobj          ideawrite=all       scrolljump=1      notextobj-indent
        |nobomb              noignorecase          scrolloff=0         textwidth=0
        |nobreakindent       noincsearch           selectmode=         timeout
        |  colorcolumn=      nolist                shellcmdflag=-x     timeoutlen=1000
        |nocommentary        nomatchit             shellxescape=@    notrackactionids
        |nocursorline          maxmapdepth=20      shellxquote={       undolevels=1000
        |nodigraph           nomini-ai             showcmd             virtualedit=
        |noexchange            more                showmode          novisualbell
        |  fileformat=unix   nomultiple-cursors    sidescroll=0        visualdelay=100
        |nogdefault          noNERDTree            sidescrolloff=0     whichwrap=b,s
        |nohighlightedyank     nrformats=hex     nosmartcase           wrap
        |  history=50        nonumber            nosneak               wrapscan
        |nohlsearch            operatorfunc=       startofline
        |noideajoin          norelativenumber    nosurround
        |  ideamarks           scroll=0          notextobj-entire
        |  clipboard=ideaput,autoselect
        |  fileencoding=utf-8
        |  guicursor=n-v-c:block-Cursor/lCursor,ve:ver35-Cursor,o:hor50-Cursor,i-ci:ver25-Cursor/lCursor,r-cr:hor20-Cursor/lCursor,sm:block-Cursor-blinkwait175-blinkoff150-blinkon175
        |  ide=IntelliJ IDEA Community Edition
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
    )
  }

  @Test
  fun `test show named options`() {
    assertCommandOutput(
      "set number? relativenumber? scrolloff? nrformats?", """
      |  nrformats=hex     nonumber            norelativenumber      scrolloff=0
      """.trimMargin()
    )
  }

  @Test
  fun `test show all modified option values in single column`() {
    // 'fileformat' will always be "unix" because the platform normalises line endings to `\n`, but the default is
    // different for Mac/Unix and DOS. For the sake of tests, reset to the OS default
    enterCommand("setlocal fileformat&")

    // 'fileencoding' defaults to "", but is automatically detected as UTF-8
    enterCommand("set number relativenumber scrolloff nrformats")
    assertExOutput("  nrformats=hex       scrolloff=0")
    injector.outputPanel.getCurrentOutputPanel()?.close()
    assertCommandOutput(
      "set!",
      """
      |--- Options ---
      |  fileencoding=utf-8
      |  number
      |  relativenumber
      """.trimMargin()
    )
  }

  @Test
  fun `test show all option values in single column`() {
    // 'fileencoding' defaults to "", but is automatically detected as UTF-8
    setOsSpecificOptionsToSafeValues()
    assertCommandOutput(
      "set! all", """
      |--- Options ---
      |noargtextobj
      |nobomb
      |nobreakindent
      |  clipboard=ideaput,autoselect
      |  colorcolumn=
      |nocommentary
      |nocursorline
      |nodigraph
      |noexchange
      |  fileencoding=utf-8
      |  fileformat=unix
      |nogdefault
      |  guicursor=n-v-c:block-Cursor/lCursor,ve:ver35-Cursor,o:hor50-Cursor,i-ci:ver25-Cursor/lCursor,r-cr:hor20-Cursor/lCursor,sm:block-Cursor-blinkwait175-blinkoff150-blinkon175
      |nohighlightedyank
      |  history=50
      |nohlsearch
      |  ide=IntelliJ IDEA Community Edition
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
    )
  }

  @Test
  fun `test show named options in single column`() {
    assertCommandOutput(
      "set! number? relativenumber? scrolloff? nrformats?", """
      |  nrformats=hex
      |nonumber
      |norelativenumber
      |  scrolloff=0
      """.trimMargin()
    )
  }

  @Test
  fun `test reset local value for local-to-buffer option`() {
    enterCommand("set nrformats=octal")

    enterCommand("set nrformats&")

    assertCommandOutput("set nrformats?", "  nrformats=hex")
  }

  @Test
  fun `test reset local value for global-local option`() {
    enterCommand("set virtualedit=block") // Sets the global + effective values. Local is unset
    enterCommand("setlocal virtualedit=onemore")  // Sets the local + effective values
    assertCommandOutput("set virtualedit?", "  virtualedit=onemore")
    assertCommandOutput("setlocal virtualedit?", "  virtualedit=onemore")

    // This is like setting the global-local value to its own global value. :set with a global-local option will set the
    // global value and unset the local value
    enterCommand("set virtualedit<")

    assertCommandOutput("set virtualedit?", "  virtualedit=block")
    assertCommandOutput("setlocal virtualedit?", "  virtualedit=")
  }

  // Environment variable expansion tests

  @Test
  fun `test shell option expands existing environment variable`() {
    val pathValue = System.getenv("PATH")
    enterCommand("set shell=\$PATH")
    assertEquals(pathValue, options().shell)
  }

  @Test
  fun `test shell option keeps non-existent variable as-is`() {
    enterCommand("set shell=\$NONEXISTENT_VAR_12345")
    assertEquals("\$NONEXISTENT_VAR_12345", options().shell)
  }

  @Test
  fun `test shell option expands tilde`() {
    val home = System.getProperty("user.home")
    enterCommand("set shell=~/bin/bash")
    assertEquals("$home/bin/bash", options().shell)
  }

  @Test
  fun `test shell option expands mixed tilde and env var`() {
    val home = System.getProperty("user.home")
    val pathValue = System.getenv("PATH")
    enterCommand("set shell=~/\$PATH")
    assertEquals("$home/$pathValue", options().shell)
  }
}
