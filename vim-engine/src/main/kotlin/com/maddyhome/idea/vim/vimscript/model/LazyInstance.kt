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

public open class LazyInstance<T>(private val className: String, private val classLoader: ClassLoader) {
  public open val instance: T by lazy {
    val aClass = classLoader.loadClass(className)
    val lookup = MethodHandles.privateLookupIn(aClass, MethodHandles.lookup())
    val instance = lookup.findConstructor(aClass, MethodType.methodType(Void.TYPE)).invoke()
    @Suppress("UNCHECKED_CAST")
    instance as T
  }
}