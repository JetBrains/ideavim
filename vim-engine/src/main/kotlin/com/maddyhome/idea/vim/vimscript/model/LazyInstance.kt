/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 * Abstract class representing a lazily loaded instance of a specified class. The class is dynamically
 * loaded and instantiated at runtime, using the provided class name and class loader. This approach is
 * useful for deferring the loading and instantiation of a class until it is actually needed, reducing
 * initial memory footprint and startup time.
 */
abstract class LazyInstance<T>(private val className: String, private val classLoader: ClassLoader) {
  open val instance: T by lazy {
    val aClass = classLoader.loadClass(className)
    val lookup = MethodHandles.privateLookupIn(aClass, MethodHandles.lookup())
    val instance = lookup.findConstructor(aClass, MethodType.methodType(Void.TYPE)).invoke()
    @Suppress("UNCHECKED_CAST")
    instance as T
  }
}