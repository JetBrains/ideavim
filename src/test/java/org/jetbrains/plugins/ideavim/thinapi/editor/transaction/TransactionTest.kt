/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi.editor.transaction

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.replaceService
import com.intellij.vim.api.VimApi
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimInjector
import com.maddyhome.idea.vim.api.VimJumpService
import com.maddyhome.idea.vim.api.VimMarkService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.thinapi.VimApiImpl
import kotlinx.coroutines.runBlocking
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

class TransactionTest : MockTestCase() {
  private lateinit var myVimApi: VimApi
  private lateinit var mockMarkService: VimMarkService
  private lateinit var mockJumpService: VimJumpService
  private lateinit var vimEditor: VimEditor
  private lateinit var mockInjector: VimInjector
  private lateinit var realInjector: VimInjector
  private lateinit var serviceDisposable: Disposable

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    myVimApi = VimApiImpl(listenerOwner, mappingOwner, null)

    mockInjector = spy(injector)

    // Use a separate disposable for service replacements so we can dispose it
    // before super.tearDown(). This prevents editor teardown from calling methods
    // on mock services (via editorReleased), which would record invocations on
    // the EDT's MockingProgressImpl thread-local and leak the project.
    serviceDisposable = Disposer.newDisposable("TransactionTest services")

    mockMarkService = Mockito.mock(VimMarkService::class.java)
    ApplicationManager.getApplication().replaceService(VimMarkService::class.java, mockMarkService, serviceDisposable)
    Mockito.`when`(mockInjector.markService).thenReturn(mockMarkService)

    mockJumpService = Mockito.mock(VimJumpService::class.java)
    ApplicationManager.getApplication().replaceService(VimJumpService::class.java, mockJumpService, serviceDisposable)
    Mockito.`when`(mockInjector.jumpService).thenReturn(mockJumpService)

    vimEditor = fixture.editor.vim
    Mockito.`when`(mockInjector.fallbackWindow).thenReturn(vimEditor)

    realInjector = injector
    injector = mockInjector
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    injector = realInjector

    // Dispose service replacements BEFORE super.tearDown() so that editor
    // teardown (editorReleased) hits the real services, not mocks.
    Disposer.dispose(serviceDisposable)

    Mockito.reset(mockMarkService)
    Mockito.reset(mockJumpService)
    Mockito.reset(mockInjector)
    Mockito.framework().clearInlineMocks()

    super.tearDown(testInfo)
  }

  fun assertEqualsEditor(expected: VimEditor, actual: VimEditor) {
    assertTrue(
      expected.getPath() == actual.getPath(),
      "Expected and actual VimEditor instances do not match in file path"
    )
  }

  @Test
  fun `test setMark calls setMark`() {
    val char = 'a'

    runBlocking {
      myVimApi.editor {
        change {
          setMark(char)
        }
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.markService).setMark(
      editorCaptor.capture(),
      eq(char)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test removeMark calls removeMark`() {
    val char = 'b'

    runBlocking {
      myVimApi.editor {
        change {
          removeMark(char)
        }
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.markService).removeMark(
      editorCaptor.capture(),
      eq(char)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test setGlobalMark calls setGlobalMark`() {
    val char = 'A'
    val offset = 0

    runBlocking {
      myVimApi.editor {
        change {
          setGlobalMark(char)
        }
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.markService).setGlobalMark(
      editorCaptor.capture(),
      eq(char),
      eq(offset)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test removeGlobalMark calls removeGlobalMark`() {
    val char = 'B'

    runBlocking {
      myVimApi.editor {
        change {
          removeGlobalMark(char)
        }
      }
    }

    verify(injector.markService).removeGlobalMark(eq(char))
  }

  @Test
  fun `test setGlobalMark with offset calls setGlobalMark`() {
    val char = 'C'
    val offset = 10

    runBlocking {
      myVimApi.editor {
        change {
          setGlobalMark(char, offset)
        }
      }
    }

    val editorCaptor = argumentCaptor<VimEditor>()
    verify(injector.markService).setGlobalMark(
      editorCaptor.capture(),
      eq(char),
      eq(offset)
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)
  }

  @Test
  fun `test resetAllMarks calls resetAllMarks`() {
    runBlocking {
      myVimApi.editor {
        change {
          resetAllMarks()
        }
      }
    }

    verify(injector.markService).resetAllMarks()
  }

  @Test
  fun `test dropLastJump calls dropLastJump`() {
    runBlocking {
      myVimApi.editor {
        change {
          dropLastJump()
        }
      }
    }

    verify(injector.jumpService).dropLastJump(eq(vimEditor.projectId))
  }

  @Test
  fun `test clearJumps calls clearJumps`() {
    runBlocking {
      myVimApi.editor {
        change {
          clearJumps()
        }
      }
    }

    verify(injector.jumpService).clearJumps(eq(vimEditor.projectId))
  }
}
