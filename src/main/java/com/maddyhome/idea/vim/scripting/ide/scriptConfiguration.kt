/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.scripting.ide

import com.intellij.vim.api.scopes.VimScope
import org.jetbrains.kotlin.idea.base.plugin.artifacts.KotlinArtifacts
import java.io.File
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.fileExtension
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.templates.standard.ScriptTemplateWithBindings

private const val FILE_EXTENSION = "vim.kts"
const val IDEAVIM_ID = "IdeaVIM"
const val KOTLIN_ID = "org.jetbrains.kotlin"

val defaultClasspath: List<File> = listOf(
  KotlinArtifacts.kotlinScriptRuntime,
  KotlinArtifacts.kotlinStdlib,
  KotlinArtifacts.kotlinReflect
)

fun ideaVimScriptCompilationConfiguration(): ScriptCompilationConfiguration =
  ScriptCompilationConfiguration {
    fileExtension(FILE_EXTENSION)
    baseClass(ScriptTemplateWithBindings::class)
    jvm {
      DependenciesProviderService.instance().getClassLoader(IDEAVIM_ID)?.let {
        dependenciesFromClassloader(classLoader = it, wholeClasspath = true)
      }
      updateClasspath(DependenciesProviderService.instance().collectDependencies(IDEAVIM_ID))
      updateClasspath(DependenciesProviderService.instance().collectDependencies(KOTLIN_ID))
    }
    dependencies(
      JvmDependency(
        defaultClasspath + DependenciesProviderService.instance().collectDependencies(IDEAVIM_ID)
      )
    )
    implicitReceivers(VimScope::class)
    ide {
      acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
  }

fun ideaVimScriptEvaluationConfiguration(): ScriptEvaluationConfiguration =
  ScriptEvaluationConfiguration {
    implicitReceivers(getVimScope())
  }
