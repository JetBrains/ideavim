/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension

/**
 * Represents a lazy-loaded Vim extension for IdeaVim.
 *
 * @param name The unique name of the extension.
 * @param className The fully qualified name of the class implementing the extension (in case a function does not belong to a class,
 * class name is `package.FileNameKt`, as it can be seen in decompiled Java code).
 * @param functionName The name of the static function used to initialize the extension.
 * @param classLoader The class loader used to load the extension's implementation.
 */
open class LazyVimExtension(
  val name: String,
  className: String,
  functionName: String,
  classLoader: ClassLoader,
) : LazyExtensionFunctionInstance(className, functionName, classLoader)