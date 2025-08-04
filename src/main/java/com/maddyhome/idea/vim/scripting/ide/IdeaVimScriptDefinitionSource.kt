/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.scripting.ide

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class IdeaVimScriptDefinitionSource(val project: Project) : ScriptDefinitionsSource {
  override val definitions: Sequence<ScriptDefinition>
    get() {
      val hostConfiguration = defaultJvmScriptingHostConfiguration
      val compilationConfiguration = ideaVimScriptCompilationConfiguration()
      val evaluationConfiguration = ideaVimScriptEvaluationConfiguration()

      return sequenceOf(
        ScriptDefinition.FromConfigurations(
          hostConfiguration, compilationConfiguration, evaluationConfiguration,
        ).apply {
          order = Int.MIN_VALUE
        }
      )
    }
}