/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi.script

import com.intellij.openapi.components.Service
import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.KotlinScriptService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.thinapi.VimScopeImpl
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvmhost.JvmScriptCompiler

@Service
class IjKotlinScriptService() : KotlinScriptService {
  private var scriptContext: VimScope? = null

  private val logger = vimLogger<IjKotlinScriptService>()

  private val scriptCompiler = JvmScriptCompiler()
  private val scriptEvaluator = BasicJvmScriptEvaluator()

  override fun executeScript(
    sourceCode: String,
    onCompilationError: (List<ScriptDiagnostic>) -> Unit,
    onExecutionError: (List<ScriptDiagnostic>, String) -> Unit,
    onFinished: (String) -> Unit,
  ) {
    val classLoader: ClassLoader = this@IjKotlinScriptService.javaClass.classLoader

    try {
      thread(contextClassLoader = classLoader) {
        compileScript(
          sourceCode,
          onSuccess = { compiledScript ->
            executeScript(
              compiledScript,
              onSuccess = { output ->
                onFinished(output)
              },
              onFailure = { diagnostics, error ->
                onExecutionError(diagnostics, error)
              }
            )
          },
          onFailure = { results ->
            onCompilationError(results)
          }
        )

      }
    } catch (e: Exception) {
      println("Error during script execution: ${e.message}")
    }
  }

  private fun executeScript(
    compiledScript: CompiledScript,
    onSuccess: (String) -> Unit,
    onFailure: (List<ScriptDiagnostic>, String) -> Unit,
  ) {
    val context: VimScope = scriptContext ?: createScriptContext().also { scriptContext = it }
    val evaluationConfiguration = createEvaluationConfiguration()
    runBlocking {
      val evaluationResult: ResultWithDiagnostics<EvaluationResult>
      val output = redirectOutput {
        evaluationResult = scriptEvaluator.invoke(compiledScript, evaluationConfiguration)
      }
      when (evaluationResult) {
        is ResultWithDiagnostics.Success -> {
          logger.info("Successfully executed: $evaluationResult")
          onSuccess(output.out)
        }

        is ResultWithDiagnostics.Failure -> {
          logger.warn("Error during script execution: ${evaluationResult.reports.joinToString("\n") { it.message }}")
          onFailure(evaluationResult.reports, output.err)
        }
      }
    }
  }

  private fun compileScript(
    scriptSource: String,
    onSuccess: (CompiledScript) -> Unit,
    onFailure: (List<ScriptDiagnostic>) -> Unit,
  ) {
    val compilationConfiguration = createCompilationConfiguration()
    val timestamp = System.currentTimeMillis()
    val scriptSource: SourceCode = scriptSource.toScriptSource(name = "KotlinScript_$timestamp.kts")
    return runBlocking {
      val compilationResult = scriptCompiler.invoke(scriptSource, compilationConfiguration)
      when (compilationResult) {
        is ResultWithDiagnostics.Success -> {
          logger.info("Successfully compiled: $compilationResult")
          val compiledScript = compilationResult.value
          onSuccess(compiledScript)
        }

        is ResultWithDiagnostics.Failure -> {
          logger.warn("Error during script compilation: ${compilationResult.reports.joinToString("\n") { it.message }}")
          onFailure(compilationResult.reports)
        }
      }
    }
  }

  override fun unloadChanges() {
    scriptContext?.let {
      injector.keyGroup.removeKeyMapping((it as VimScopeImpl).mappingOwner)
    }
  }
}