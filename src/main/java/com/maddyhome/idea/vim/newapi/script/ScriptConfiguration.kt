/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi.script

import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.thinapi.VimScopeImpl
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.fileExtension
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm

data class OutputStream(
  val out: String,
  val err: String,
)

@OptIn(ExperimentalContracts::class)
fun redirectOutput(block: suspend () -> Unit): OutputStream {
  contract {
    callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
  }
  // out
  val newOutStream = ByteArrayOutputStream()
  val prevOut = System.out
  System.setOut(PrintStream(newOutStream))

  // err
  val newErrStream = ByteArrayOutputStream()
  val prevErr = System.err
  System.setErr(PrintStream(newErrStream))

  try {
    runBlocking {
      block()
    }
  } finally {
    // restore out
    System.out.flush()
    System.setOut(prevOut)

    // restore error
    System.err.flush()
    System.setErr(prevErr)
  }

  return OutputStream(out = newOutStream.toString().trim(), err = newErrStream.toString().trim())
}

internal fun createCompilationConfiguration(): ScriptCompilationConfiguration {
  return ScriptCompilationConfiguration {
    implicitReceivers(VimScope::class)
    fileExtension(SCRIPT_EXTENSION)
    jvm {
      dependenciesFromClassloader(classLoader = injector.kotlinScriptService.javaClass.classLoader, wholeClasspath = true)
    }
    ide {
      acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
  }
}

fun createScriptContext(): VimScope {
  val timestamp: Long = System.currentTimeMillis()
  val scriptContextId = "KotlinScriptId_$timestamp"
  val mappingOwner = MappingOwner.Plugin.get(scriptContextId)
  val listenerOwner = ListenerOwner.Plugin.get(scriptContextId)
  val vimScopeImpl = VimScopeImpl(listenerOwner, mappingOwner)
  return vimScopeImpl
}

internal fun createEvaluationConfiguration(context: VimScope): ScriptEvaluationConfiguration {
  return ScriptEvaluationConfiguration {
    implicitReceivers(context)
  }
}