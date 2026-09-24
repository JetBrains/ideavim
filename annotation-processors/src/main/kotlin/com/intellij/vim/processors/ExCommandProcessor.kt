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
import com.intellij.vim.annotations.ExCommand
import kotlinx.serialization.Serializable

/** One entry of the generated ex-command table, read back by `ExCommandProvider` */
@Serializable
internal data class ExCommandEntry(
  val className: String,
  val barSeparates: Boolean = true,
  val delimitedSections: Int = 0,
)

class ExCommandProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
  private val visitor = EXCommandVisitor()
  private val commandToClass = mutableMapOf<String, ExCommandEntry>()
  private val fileWriter = JsonFileWriter(environment)

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val exCommandsFile = environment.options["ex_commands_file"] ?: return emptyList()

    resolver.getAllFiles().forEach { it.accept(visitor, Unit) }
    
    val sortedCommandToClass =
      commandToClass.toList().sortedWith(compareBy({ it.first }, { it.second.className })).toMap()
    fileWriter.write(exCommandsFile, sortedCommandToClass)

    return emptyList()
  }

  private inner class EXCommandVisitor : KSVisitorVoid(enableNewFeatures = true) {
    @OptIn(KspExperimental::class)
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      // The annotation is repeatable, so that one class can declare different bar handling for different names
      for (exCommandAnnotation in classDeclaration.getAnnotationsByType(ExCommand::class)) {
        for (command in exCommandAnnotation.command.split(",")) {
          commandToClass[command] = ExCommandEntry(
            classDeclaration.qualifiedName!!.asString(),
            exCommandAnnotation.barSeparates,
            exCommandAnnotation.delimitedSections,
          )
        }
      }
    }

    override fun visitFile(file: KSFile, data: Unit) {
      file.declarations.forEach { it.accept(this, Unit) }
    }
  }
}