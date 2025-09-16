/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.mock.vimscript.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.TabService
import org.jetbrains.plugins.ideavim.mock.MockTestCase
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class TabmoveTest : MockTestCase() {

  @Test
  fun `test move to the first index`() {
    val tabService = mockService(TabService::class.java)
    Mockito.`when`(tabService.getCurrentTabIndex(contextStub)).thenReturn(2)
    Mockito.`when`(tabService.getTabCount(contextStub)).thenReturn(5)
    injector.vimscriptExecutor.execute("tabmove 0", editorStub, contextStub, skipHistory = false)

    verify(tabService).moveCurrentTabToIndex(0, contextStub)
  }

  @Test
  fun `test move to the last index`() {
    val tabService = mockService(TabService::class.java)
    Mockito.`when`(tabService.getCurrentTabIndex(contextStub)).thenReturn(2)
    Mockito.`when`(tabService.getTabCount(contextStub)).thenReturn(5)
    injector.vimscriptExecutor.execute("tabmove $", editorStub, contextStub, skipHistory = false)

    verify(tabService).moveCurrentTabToIndex(4, contextStub)
  }

  @Test
  fun `test move to index that is greater than current`() {
    val tabService = mockService(TabService::class.java)
    Mockito.`when`(tabService.getCurrentTabIndex(contextStub)).thenReturn(4)
    Mockito.`when`(tabService.getTabCount(contextStub)).thenReturn(5)
    injector.vimscriptExecutor.execute("tabmove 2", editorStub, contextStub, skipHistory = false)

    verify(tabService).moveCurrentTabToIndex(2, contextStub)
  }

  @Test
  fun `test move to index that is less than current`() {
    val tabService = mockService(TabService::class.java)
    Mockito.`when`(tabService.getCurrentTabIndex(contextStub)).thenReturn(1)
    Mockito.`when`(tabService.getTabCount(contextStub)).thenReturn(5)
    injector.vimscriptExecutor.execute("tabmove 3", editorStub, contextStub, skipHistory = false)

    verify(tabService).moveCurrentTabToIndex(2, contextStub)
  }

  @Test
  fun `test move to nonexistent index`() {
    val tabService = mockService(TabService::class.java)
    Mockito.`when`(tabService.getCurrentTabIndex(contextStub)).thenReturn(2)
    Mockito.`when`(tabService.getTabCount(contextStub)).thenReturn(5)
    injector.vimscriptExecutor.execute("tabmove 7", editorStub, contextStub, skipHistory = false)

    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument")
    verify(tabService, never()).moveCurrentTabToIndex(any(), any())
  }

  @Test
  fun `test move to positive relative index`() {
    val tabService = mockService(TabService::class.java)
    Mockito.`when`(tabService.getCurrentTabIndex(contextStub)).thenReturn(2)
    Mockito.`when`(tabService.getTabCount(contextStub)).thenReturn(5)
    injector.vimscriptExecutor.execute("tabmove +2", editorStub, contextStub, skipHistory = false)

    verify(tabService).moveCurrentTabToIndex(4, contextStub)
  }

  @Test
  fun `test move to negative relative index`() {
    val tabService = mockService(TabService::class.java)
    Mockito.`when`(tabService.getCurrentTabIndex(contextStub)).thenReturn(4)
    Mockito.`when`(tabService.getTabCount(contextStub)).thenReturn(5)
    injector.vimscriptExecutor.execute("tabmove -2", editorStub, contextStub, skipHistory = false)

    verify(tabService).moveCurrentTabToIndex(2, contextStub)
  }

  @Test
  fun `test move to nonexistent positive relative index`() {
    val tabService = mockService(TabService::class.java)
    Mockito.`when`(tabService.getCurrentTabIndex(contextStub)).thenReturn(2)
    Mockito.`when`(tabService.getTabCount(contextStub)).thenReturn(5)
    injector.vimscriptExecutor.execute("tabmove +10", editorStub, contextStub, skipHistory = false)

    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument")
    verify(tabService, never()).moveCurrentTabToIndex(any(), any())
  }

  @Test
  fun `test move to nonexistent negative relative index`() {
    val tabService = mockService(TabService::class.java)
    Mockito.`when`(tabService.getCurrentTabIndex(contextStub)).thenReturn(2)
    Mockito.`when`(tabService.getTabCount(contextStub)).thenReturn(5)
    injector.vimscriptExecutor.execute("tabmove -10", editorStub, contextStub, skipHistory = false)

    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument")
    verify(tabService, never()).moveCurrentTabToIndex(any(), any())
  }

  @Test
  fun `test move to plus zero`() {
    val tabService = mockService(TabService::class.java)
    Mockito.`when`(tabService.getCurrentTabIndex(contextStub)).thenReturn(2)
    Mockito.`when`(tabService.getTabCount(contextStub)).thenReturn(5)
    injector.vimscriptExecutor.execute("tabmove +0", editorStub, contextStub, skipHistory = false)

    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument")
    verify(tabService, never()).moveCurrentTabToIndex(any(), any())
  }

  @Test
  fun `test move to minus zero`() {
    val tabService = mockService(TabService::class.java)
    Mockito.`when`(tabService.getCurrentTabIndex(contextStub)).thenReturn(2)
    Mockito.`when`(tabService.getTabCount(contextStub)).thenReturn(5)
    injector.vimscriptExecutor.execute("tabmove +0", editorStub, contextStub, skipHistory = false)

    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument")
    verify(tabService, never()).moveCurrentTabToIndex(any(), any())
  }

  @Test
  fun `test move left with omitted number`() {
    val tabService = mockService(TabService::class.java)
    Mockito.`when`(tabService.getCurrentTabIndex(contextStub)).thenReturn(2)
    Mockito.`when`(tabService.getTabCount(contextStub)).thenReturn(5)
    injector.vimscriptExecutor.execute("tabmove +", editorStub, contextStub, skipHistory = false)

    verify(tabService).moveCurrentTabToIndex(3, contextStub)
  }

  @Test
  fun `test move right with omitted number`() {
    val tabService = mockService(TabService::class.java)
    Mockito.`when`(tabService.getCurrentTabIndex(contextStub)).thenReturn(2)
    Mockito.`when`(tabService.getTabCount(contextStub)).thenReturn(5)
    injector.vimscriptExecutor.execute("tabmove -", editorStub, contextStub, skipHistory = false)

    verify(tabService).moveCurrentTabToIndex(1, contextStub)
  }
}
