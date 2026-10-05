/*
 * Copyright 2003-2026 The IdeaVim authors
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
import com.intellij.vim.annotations.ExCommand
import kotlinx.serialization.Serializable

/** The descriptions of one module's commands, merged with the other modules' into the help file by `generateHelp` */
@Serializable
internal data class HelpData(
  val exCommands: List<ExCommandHelp>,
  val commands: List<CommandHelp>,
)

@Serializable
internal data class ExCommandHelp(
  /** The command names as declared by [ExCommand.command], e.g. `h[elp]` */
  val names: List<String>,
  val description: String,
  val className: String,
)

@Serializable
internal data class CommandHelp(
  val keys: List<String>,
  /** The abbreviations of the modes, as in [CommandBean.modes] */
  val modes: String,
  val lookup: Boolean,
  val description: String,
  val className: String,
)

/**
 * Collects the descriptions of [ExCommand] and [CommandOrMotion] declarations.
 *
 * Unlike the command tables, the output is not needed at runtime, so it goes to `help_directory` (in the build
 * directory) rather than to the resources.
 */
class HelpProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
  private val visitor = HelpVisitor()
  private val exCommands = mutableListOf<ExCommandHelp>()
  private val commands = mutableListOf<CommandHelp>()
  private val fileWriter = JsonFileWriter(environment)

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val helpFile = environment.options["help_file"] ?: return emptyList()
    val helpDirectory = environment.options["help_directory"] ?: return emptyList()

    resolver.getAllFiles().forEach { it.accept(visitor, Unit) }

    val data = HelpData(
      exCommands.sortedWith(compareBy({ it.names.first() }, { it.className })),
      commands.sortedWith(compareBy({ it.keys.first() }, { it.modes }, { it.className })),
    )
    fileWriter.write(helpFile, data, helpDirectory)

    return emptyList()
  }

  private inner class HelpVisitor : KSVisitorVoid() {
    @OptIn(KspExperimental::class)
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      val className = classDeclaration.qualifiedName!!.asString()
      for (annotation in classDeclaration.getAnnotationsByType(ExCommand::class)) {
        exCommands.add(ExCommandHelp(annotation.command.split(","), annotation.description, className))
      }
      for (annotation in classDeclaration.getAnnotationsByType(CommandOrMotion::class)) {
        commands.add(
          CommandHelp(
            annotation.keys.toList(),
            annotation.modes.map { it.abbrev }.joinToString(separator = ""),
            annotation.lookup,
            annotation.description,
            className,
          )
        )
      }
    }

    override fun visitFile(file: KSFile, data: Unit) {
      file.declarations.forEach { it.accept(this, Unit) }
    }
  }
}
