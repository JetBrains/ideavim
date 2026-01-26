/*
 * Copyright 2003-2025 The IdeaVim authors
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
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.intellij.vim.api.VimPlugin

// Used for processing VimPlugin annotations
class ExtensionsProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
  private val visitor = ExtensionsVisitor()
  private val declaredExtensions = mutableListOf<KspExtensionBean>()
  private val fileWriter = JsonFileWriter(environment)

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val extensionsFile = environment.options["extensions_file"] ?: return emptyList()

    resolver.getAllFiles().forEach { it.accept(visitor, Unit) }

    val sortedExtensions = declaredExtensions.sortedWith(compareBy { it.extensionName })
    fileWriter.write(extensionsFile, sortedExtensions)

    return emptyList()
  }


  private inner class ExtensionsVisitor : KSVisitorVoid() {
    @OptIn(KspExperimental::class)
    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
      val pluginAnnotation = function.getAnnotationsByType(VimPlugin::class).firstOrNull() ?: return
      val pluginName = pluginAnnotation.name
      val functionPath = function.simpleName.asString()

      // Extensions are not declared as part of class, however, when Kotlin code is decompiled to Java,
      // function `fun init()` in a file File.kt, will be a static method in a class FileKt since Java
      // does not support top-level functions. Then, it can be loaded with class loader.

      val surroundingFileName = function.containingFile?.fileName?.removeSuffix(".kt") ?: return
      val packageName = function.packageName.asString()

      val className = "$packageName.${surroundingFileName}Kt"

      val kspExtensionBean = KspExtensionBean(pluginName, functionPath, className)
      declaredExtensions.add(kspExtensionBean)
    }

    override fun visitFile(file: KSFile, data: Unit) {
      file.declarations.forEach { it.accept(this, Unit) }
    }
  }
}