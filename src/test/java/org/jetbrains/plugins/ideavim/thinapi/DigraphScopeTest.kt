/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi

import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.VimDigraphGroup
import com.maddyhome.idea.vim.api.VimDigraphGroupBase
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimInjector
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.thinapi.VimScopeImpl
import org.jetbrains.plugins.ideavim.mock.MockTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import kotlin.test.assertTrue

class DigraphScopeTest : MockTestCase() {
  private lateinit var vimScope: VimScope
  private lateinit var digraphGroup: VimDigraphGroup
  private lateinit var vimEditor: VimEditor
  private lateinit var mockInjector: VimInjector
  private lateinit var realInjector: VimInjector

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    vimScope = VimScopeImpl(listenerOwner, mappingOwner)

    mockInjector = spy(injector)

    digraphGroup = mockService(VimDigraphGroupBase::class.java)
    Mockito.`when`(mockInjector.digraphGroup).thenReturn(digraphGroup)

    vimEditor = fixture.editor.vim

    realInjector = injector
    injector = mockInjector
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    super.tearDown(testInfo)
    injector = realInjector
  }

  fun assertEqualsEditor(expected: VimEditor, actual: VimEditor) {
    assertTrue(
      expected.getPath() == actual.getPath(),
      "Expected and actual VimEditor instances do not match in file path"
    )
  }

  @Test
  fun `test getCharacter`() {
    val ch1 = 'a'
    val ch2 = 'b'

    vimScope.digraph {
      getCharacter(ch1, ch2)
    }

    verify(injector.digraphGroup).getCharacterForDigraph(eq(ch1), eq(ch2))
  }

  @Test
  fun `test addDigraph with char parameters`() {
    val ch1 = 'a'
    val ch2 = 'b'
    val codepoint = 228

    vimScope.digraph {
      addDigraph(ch1, ch2, codepoint)
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.digraphGroup).parseCommandLine(editorCaptor.capture(), eq("$ch1$ch2 $codepoint"))
    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test clearCustomDigraphs`() {
    vimScope.digraph {
      clearCustomDigraphs()
    }

    verify(injector.digraphGroup).clearCustomDigraphs()
  }
}
