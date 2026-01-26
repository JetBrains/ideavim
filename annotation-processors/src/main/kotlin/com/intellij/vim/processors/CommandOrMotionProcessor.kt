/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.processors

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.intellij.vim.annotations.CommandOrMotion

class CommandOrMotionProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
  private val visitor = CommandOrMotionVisitor()
  private val commands = mutableListOf<CommandBean>()
  private val fileWriter = JsonFileWriter(environment)

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val commandsFile = environment.options["commands_file"] ?: return emptyList()

    resolver.getAllFiles().forEach { it.accept(visitor, Unit) }

    val sortedCommands = commands.sortedWith(compareBy({ it.keys }, { it.`class` }))
    fileWriter.write(commandsFile, sortedCommands)

    return emptyList()
  }

  private inner class CommandOrMotionVisitor : KSVisitorVoid() {
    @OptIn(KspExperimental::class)
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      val commandAnnotation = classDeclaration.getAnnotationsByType(CommandOrMotion::class).firstOrNull() ?: return
      for (key in commandAnnotation.keys) {
        commands.add(
          CommandBean(key, classDeclaration.qualifiedName!!.asString(), commandAnnotation.modes.map { it.abbrev }.joinToString(separator = ""))
        )
      }
    }

    override fun visitFile(file: KSFile, data: Unit) {
      file.declarations.forEach { it.accept(this, Unit) }
    }
  }

}