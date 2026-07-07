/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.extension.Alias
import com.maddyhome.idea.vim.extension.ExtensionBeanClass
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for the runtime function-handler registration API used by extensions such as textobj-user.
 *
 * Extensions that expose vimscript functions implemented in Kotlin (e.g. `textobj#user#plugin`) need to
 * register those handlers when enabled and remove them when disabled, so the functions are gated behind
 * the extension rather than being globally available. These tests exercise:
 *  - [com.maddyhome.idea.vim.api.VimscriptFunctionService.registerFunctionHandler]
 *  - [com.maddyhome.idea.vim.api.VimscriptFunctionService.unregisterFunctionHandler]
 *
 * They drive the API through its real caller (an extension's `init` / `dispose`) and observe the result
 * user-facing: the function is callable only while the extension is enabled, otherwise `E117` is raised.
 */
class RegisterFunctionHandlerTest : VimTestCase() {
  private lateinit var extension: ExtensionBeanClass
  private var disposable: Disposable = Disposer.newDisposable()

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
    extension = FunctionRegisteringExtension.createBean()
    VimExtension.EP_NAME.point.registerExtension(extension, disposable)
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    Disposer.dispose(disposable)
    super.tearDown(testInfo)
  }

  @Test
  fun `function is unknown before the extension is enabled`() {
    enterCommand("echo TestObjFn()")
    assertPluginErrorMessage("E117: Unknown function: TestObjFn")
  }

  @Test
  fun `registerFunctionHandler makes the function callable`() {
    enableExtensions("TestFunctionExtension")
    assertCommandOutput("echo TestObjFn()", "registered")
  }

  @Test
  fun `unregisterFunctionHandler makes the function unknown again`() {
    enableExtensions("TestFunctionExtension")
    assertCommandOutput("echo TestObjFn()", "registered")

    // Disabling the extension disposes it, which must unregister the handler.
    enterCommand("set noTestFunctionExtension")
    enterCommand("echo TestObjFn()")
    assertPluginErrorMessage("E117: Unknown function: TestObjFn")
  }

  // A realistic handler extends BuiltinFunctionHandler to get arity checking and typed arguments.
  // Its `name` is a lateinit field that registerFunctionHandler must initialize.
  @Test
  fun `registered BuiltinFunctionHandler can access its own name`() {
    enableExtensions("TestFunctionExtension")
    // The handler returns its own name; this only works if registration initialized `name`.
    assertCommandOutput("echo MyBuiltinFn('x')", "MyBuiltinFn")
  }

  @Test
  fun `registered BuiltinFunctionHandler reports its name in arity errors`() {
    enableExtensions("TestFunctionExtension")
    // Calling with too few arguments makes the framework read `name` for the E119 message.
    enterCommand("echo MyBuiltinFn()")
    assertPluginErrorMessage("E119: Not enough arguments for function: MyBuiltinFn")
  }
}

/**
 * A test-only handler for a `TestObjFn()` vimscript function that simply returns the string "registered".
 */
private class RegisteredTestFunction(override val name: String) : FunctionHandler {
  override val scope: Scope? = null

  override fun executeFunction(
    arguments: List<Expression>,
    range: Range?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType = VimString("registered")
}

/**
 * A test-only handler that extends [BuiltinFunctionHandler] (the base most real handlers use), whose
 * `name` is a `lateinit` field. It requires exactly one argument and returns its own name, so both the
 * arity check and the body exercise whether registration initialized `name`.
 */
private class NamedBuiltinTestFunction : BuiltinFunctionHandler<VimString>(arity = 1) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimString = VimString(name)
}

/**
 * A test-only extension that registers [RegisteredTestFunction] on enable and removes it on disable.
 */
private class FunctionRegisteringExtension : VimExtension {
  override fun getName(): String = "TestFunctionExtension"

  override fun init() {
    injector.functionService.registerFunctionHandler(FUNCTION_NAME, RegisteredTestFunction(FUNCTION_NAME))
    injector.functionService.registerFunctionHandler(BUILTIN_FUNCTION_NAME, NamedBuiltinTestFunction())
  }

  override fun dispose() {
    injector.functionService.unregisterFunctionHandler(FUNCTION_NAME)
    injector.functionService.unregisterFunctionHandler(BUILTIN_FUNCTION_NAME)
    super.dispose()
  }

  companion object {
    private const val FUNCTION_NAME = "TestObjFn"
    private const val BUILTIN_FUNCTION_NAME = "MyBuiltinFn"

    fun createBean(): ExtensionBeanClass {
      val beanClass = ExtensionBeanClass()
      beanClass.implementation = FunctionRegisteringExtension::class.java.canonicalName
      beanClass.aliases = listOf(Alias().also { it.name = "MyTestFunctions" })
      beanClass.pluginDescriptor = PluginManagerCore.getPlugin(VimPlugin.getPluginId())!!
      return beanClass
    }
  }
}
