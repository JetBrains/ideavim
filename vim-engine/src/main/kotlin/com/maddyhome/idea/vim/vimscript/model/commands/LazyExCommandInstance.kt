/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import kotlin.reflect.KClass

class LazyExCommandInstance(private val className: String, private val classLoader: ClassLoader) {
  fun getKClass(): KClass<out Command> {
    @Suppress("UNCHECKED_CAST")
    return classLoader.loadClass(className).kotlin as KClass<out Command>
  }
}