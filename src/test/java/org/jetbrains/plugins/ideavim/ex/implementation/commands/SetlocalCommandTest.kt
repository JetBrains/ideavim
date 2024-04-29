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
import com.maddyhome.idea.vim.vimscript.model.commands.SetlocalCommand
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("SpellCheckingInspection")
@TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
class SetlocalCommandTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  private fun setOsSpecificOptionsToSafeValues() {
    enterCommand("setlocal shell=/dummy/path/to/bash")
    enterCommand("setlocal shellcmdflag=-x")
    enterCommand("setlocal shellxescape=@")
    enterCommand("setlocal shellxquote={")
  }

  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("setlocal nu")
    assertTrue(command is SetlocalCommand)
    assertEquals("nu", command.argument)
  }

  @Test
  fun `test set unknown option`() {
    enterCommand("setlocal unknownOption")
    assertPluginError(true)
    assertPluginErrorMessageContains("Unknown option: unknownOption")
  }

  @Test
  fun `test set toggle option local value`() {
    enterCommand("setlocal rnu")
    assertTrue(options().relativenumber)  // Tests effective (i.e. local) value

    enterCommand("setlocal nornu")
    assertFalse(options().relativenumber)

    enterCommand("setlocal rnu!")
    assertTrue(options().relativenumber)

    enterCommand("setlocal invrnu")
    assertFalse(options().relativenumber)
  }

  @Test
  fun `test exceptions from incorrectly setting toggle option`() {
    enterCommand("setlocal number=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number=test")

    enterCommand("setlocal number=0")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number=0")

    enterCommand("setlocal number+=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number+=10")

    enterCommand("setlocal number+=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number+=test")

    enterCommand("setlocal number^=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number^=10")

    enterCommand("setlocal number^=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number^=test")

    enterCommand("setlocal number-=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number-=10")

    enterCommand("setlocal number-=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: number-=test")
  }

  @Test
  fun `test show toggle option local value`() {
    assertCommandOutput("setlocal rnu?", "norelativenumber\n")

    enterCommand("setlocal invrnu")

    assertCommandOutput("setlocal rnu?", "  relativenumber\n")
  }

  @Test
  fun `test show unset global-local toggle option value with -- prefix`() {
    val option = ToggleOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", true)
    try {
      injector.optionGroup.addOption(option)

      assertCommandOutput("setlocal test?", "--test\n")
    }
    finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test reset local toggle option value to global value`() {
    enterCommand("setlocal relativenumber") // Default global value is off
    assertTrue(options().relativenumber)

    enterCommand("setlocal relativenumber<")
    assertFalse(options().relativenumber)
  }

  @Test
  fun `test reset global-local toggle option to global value`() {
    val option = ToggleOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", false)
    try {
      injector.optionGroup.addOption(option)

      enterCommand("setlocal test")

      assertCommandOutput("setlocal test?", "  test\n")

      enterCommand("setlocal test<")  // setlocal {option}< copies the global value to the local value

      assertCommandOutput("setlocal test?", "notest\n")
    }
    finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test reset toggle option to default value`() {
    enterCommand("setlocal rnu")
    assertTrue(options().relativenumber)  // Tests effective (i.e. local) value

    enterCommand("setlocal rnu&")
    assertFalse(options().relativenumber)
  }

  @Test
  fun `test set number option local value`() {
    enterCommand("setlocal scroll&")
    assertEquals(0, options().scroll)

    enterCommand("setlocal scroll=5")
    assertEquals(5, options().scroll)

    enterCommand("setlocal scroll:10")
    assertEquals(10, options().scroll)

    enterCommand("setlocal scroll+=5")
    assertEquals(15, options().scroll)

    enterCommand("setlocal scroll-=10")
    assertEquals(5, options().scroll)

    enterCommand("setlocal scroll^=2")
    assertEquals(10, options().scroll)
  }

  @Test
  fun `test exceptions from incorrectly setting number option`() {
    enterCommand("setlocal scroll=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scroll=test")

    enterCommand("setlocal scroll+=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scroll+=test")

    enterCommand("setlocal scroll-=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scroll-=test")

    enterCommand("setlocal scroll^=test")
    assertPluginError(true)
    assertPluginErrorMessageContains("E521: Number required after =: scroll^=test")
  }

  @Test
  fun `test show number option local value`() {
    assertCommandOutput("setlocal scroll?", "  scroll=0\n")

    enterCommand("setlocal scroll=10")

    assertCommandOutput("setlocal scroll?", "  scroll=10\n")
  }

  @Test
  fun `test number option with no arguments shows current local value`() {
    assertCommandOutput("setlocal scroll", "  scroll=0\n")
  }

  @Test
  fun `test show unset global-local number option value`() {
    val option = NumberOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", 10)
    try {
      injector.optionGroup.addOption(option)

      assertCommandOutput("setlocal test?", "  test=-1\n")
    }
    finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test reset local number option value to global value`() {
    enterCommand("setlocal scroll=10")  // Default global value is 0

    enterCommand("setlocal scroll<")
    assertEquals(0, options().scroll)
  }

  @Test
  fun `test reset global-local number option to global value`() {
    val option = NumberOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", 10)
    try {
      injector.optionGroup.addOption(option)

      enterCommand("setlocal test=20")

      assertCommandOutput("setlocal test?", "  test=20\n")

      enterCommand("setlocal test<")  // setlocal {option}< copies the global value to the local value

      assertCommandOutput("setlocal test?", "  test=10\n")
    }
    finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test set string option local value`() {
    enterCommand("setlocal nrformats=octal")
    assertEquals("octal", options().nrformats.value)

    enterCommand("setlocal nrformats:alpha")
    assertEquals("alpha", options().nrformats.value)

    enterCommand("setlocal nrformats+=hex")
    assertEquals("alpha,hex", options().nrformats.value)

    enterCommand("setlocal nrformats-=hex")
    assertEquals("alpha", options().nrformats.value)

    enterCommand("setlocal nrformats^=hex")
    assertEquals("hex,alpha", options().nrformats.value)
  }

  @Test
  fun `test exceptions from incorrectly setting string option`() {
    enterCommand("setlocal nrformats=unknown")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: nrformats=unknown")

    enterCommand("setlocal nrformats+=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: nrformats+=10")

    enterCommand("setlocal nrformats-=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: nrformats-=10")

    enterCommand("setlocal nrformats^=10")
    assertPluginError(true)
    assertPluginErrorMessageContains("E474: Invalid argument: nrformats^=10")
  }

  @Test
  fun `test show string option local value`() {
    assertCommandOutput("setlocal nrformats?", "  nrformats=hex\n")

    enterCommand("setlocal nrformats+=alpha")

    assertCommandOutput("setlocal nrformats?", "  nrformats=hex,alpha\n")
  }

  @Test
  fun `test string option with no arguments shows current local value`() {
    assertCommandOutput("setlocal nrformats", "  nrformats=hex\n")
  }

  @Test
  fun `test show unset global-local string option value`() {
    val option = StringOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", "testValue")
    try {
      injector.optionGroup.addOption(option)

      assertCommandOutput("setlocal test?", "  test=\n")
    }
    finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test reset local string option value to global value`() {
    enterCommand("setlocal nrformats=alpha")
    enterCommand("setlocal nrformats<")
    assertEquals("hex", options().nrformats.value)
  }

  @Test
  fun `test reset global-local string option to global value`() {
    val option = StringOption("test", OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW, "test", "testValue")
    try {
      injector.optionGroup.addOption(option)

      enterCommand("setlocal test<")

      assertCommandOutput("setlocal test?", "  test=testValue\n")
    }
    finally {
      injector.optionGroup.removeOption(option.name)
    }
  }

  @Test
  fun `test reset string option to default value`() {
    enterCommand("setlocal nrformats=alpha")
    enterCommand("setlocal nrformats&")
    assertEquals("hex", options().nrformats.value)
  }

  @Test
  fun `test show all modified local option and unset global-local values`() {
    assertCommandOutput("setlocal", """
      |--- Local option values ---
      |--ideajoin            idearefactormode=   scrolloff=-1        sidescrolloff=-1
      |--ideacopypreprocess
      |  undolevels=-123456
      |""".trimMargin()
    )
  }

  @Test
  fun `test show all modified local option and unset global-local values 2`() {
    enterCommand("setlocal number relativenumber scrolloff=10 nrformats=alpha,hex,octal sidescrolloff=10")
    assertCommandOutput("setlocal", """
      |--- Local option values ---
      |--ideajoin            number              scrolloff=10
      |  idearefactormode=   relativenumber      sidescrolloff=10
      |--ideacopypreprocess
      |  nrformats=alpha,hex,octal
      |  undolevels=-123456
      |""".trimMargin()
    )
  }

  @Test
  fun `test show all local option values`() {
    setOsSpecificOptionsToSafeValues()
    assertCommandOutput("setlocal all", """
      |--- Local option values ---
      |noargtextobj        noignorecase          scrolloff=-1      notextobj-entire
      |nocommentary        noincsearch           selectmode=       notextobj-indent
      |nodigraph           nomatchit             shellcmdflag=-x     timeout
      |noexchange            maxmapdepth=20      shellxescape=@      timeoutlen=1000
      |nogdefault            more                shellxquote={     notrackactionids
      |nohighlightedyank   nomultiple-cursors    showcmd             virtualedit=
      |  history=50        noNERDTree            showmode          novisualbell
      |nohlsearch            nrformats=hex       sidescroll=0        visualdelay=100
      |noideaglobalmode    nonumber              sidescrolloff=-1    whichwrap=b,s
      |--ideajoin            operatorfunc=     nosmartcase           wrap
      |  ideamarks         norelativenumber    nosneak               wrapscan
      |  idearefactormode=   scroll=0            startofline
      |  ideawrite=all       scrolljump=1      nosurround
      |  clipboard=ideaput,autoselect,exclude:cons\|linux
      |  guicursor=n-v-c:block-Cursor/lCursor,ve:ver35-Cursor,o:hor50-Cursor,i-ci:ver25-Cursor/lCursor,r-cr:hor20-Cursor/lCursor,sm:block-Cursor-blinkwait175-blinkoff150-blinkon175
      |  ide=IntelliJ IDEA Community Edition
      |--ideacopypreprocess
      |  ideastatusicon=enabled
      |  ideavimsupport=dialog
      |  iskeyword=@,48-57,_
      |  keymodel=continueselect,stopselect
      |  lookupkeys=<Tab>,<Down>,<Up>,<Enter>,<Left>,<Right>,<C-Down>,<C-Up>,<PageUp>,<PageDown>,<C-J>,<C-Q>
      |  matchpairs=(:),{:},[:]
      |noReplaceWithRegister
      |  selection=inclusive
      |  shell=/dummy/path/to/bash
      |  undolevels=-123456
      |novim-paragraph-motion
      |  viminfo='100,<50,s10,h
      |""".trimMargin()
    )
  }

  @Test
  fun `test show named options`() {
    assertCommandOutput("setlocal number? relativenumber? scrolloff? nrformats?", """
      |  nrformats=hex     nonumber            norelativenumber      scrolloff=-1
      |""".trimMargin()
    )
  }

  @Test
  fun `test show all modified local option values in single column`() {
    assertCommandOutput("setlocal!", """
      |--- Local option values ---
      |--ideacopypreprocess
      |--ideajoin
      |  idearefactormode=
      |  scrolloff=-1
      |  sidescrolloff=-1
      |  undolevels=-123456
      |""".trimMargin()
    )
  }

  @Test
  fun `test show all local option values in single column`() {
    setOsSpecificOptionsToSafeValues()
    assertCommandOutput("setlocal! all", """
      |--- Local option values ---
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
      |--ideacopypreprocess
      |noideaglobalmode
      |--ideajoin
      |  ideamarks
      |  idearefactormode=
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
      |  scrolloff=-1
      |  selection=inclusive
      |  selectmode=
      |  shell=/dummy/path/to/bash
      |  shellcmdflag=-x
      |  shellxescape=@
      |  shellxquote={
      |  showcmd
      |  showmode
      |  sidescroll=0
      |  sidescrolloff=-1
      |nosmartcase
      |nosneak
      |  startofline
      |nosurround
      |notextobj-entire
      |notextobj-indent
      |  timeout
      |  timeoutlen=1000
      |notrackactionids
      |  undolevels=-123456
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
    assertCommandOutput("setlocal! number? relativenumber? scrolloff? nrformats?", """
      |  nrformats=hex
      |nonumber
      |norelativenumber
      |  scrolloff=-1
      |""".trimMargin()
    )
  }
}
