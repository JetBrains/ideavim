/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension

import com.intellij.vim.api.VimApi
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.jvm.java

/**
 * Represents a lazily initialized instance of a static extension function.
 *
 * It dynamically loads a specific function by its name and class during runtime,
 * using the provided class loader. The function is expected to have a specific signature -
 * `VimApi` parameter and returning void.
 *
 * @property className The fully qualified name of the class that contains the target static function.
 * @property functionName The name of the static function to be loaded.
 * @property classLoader The class loader used to load the class and retrieve the function.
 */
abstract class LazyExtensionFunctionInstance(
  private val className: String,
  private val functionName: String,
  private val classLoader: ClassLoader,
) {
  open val instance: MethodHandle by lazy {
    val aClass = classLoader.loadClass(className)
    val lookup = MethodHandles.privateLookupIn(aClass, MethodHandles.lookup())
    lookup.findStatic(aClass, functionName, MethodType.methodType(Void.TYPE, VimApi::class.java))
  }
}