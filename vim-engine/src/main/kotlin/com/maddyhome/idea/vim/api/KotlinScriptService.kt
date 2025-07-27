/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import kotlin.script.experimental.api.ScriptDiagnostic

interface KotlinScriptService {
  fun executeScript(
    sourceCode: String,
    onCompilationError: (List<ScriptDiagnostic>) -> Unit,
    onExecutionError: (List<ScriptDiagnostic>, String) -> Unit,
    onFinished: (String) -> Unit,
  )

  fun unloadChanges()
}