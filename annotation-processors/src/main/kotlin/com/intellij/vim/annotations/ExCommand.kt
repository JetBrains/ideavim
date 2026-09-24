/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.annotations

/**
 * [command] is formatted the same way it is formatted in Vim (with optional part in square brackets).
 *
 * [barSeparates] and [delimitedSections] say how a `|` in the argument is treated, the job Vim's command table does
 * with its `TRLBAR` flag. Repeat the annotation when the names it declares need different values.
 * See `CommandVisitor.splitOffNextCommand` and `:help :bar`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class ExCommand(
  val command: String,
  /** `false` when the command takes a `|` as part of its argument, so it can never be followed by another command */
  val barSeparates: Boolean = true,
  /** The number of delimiter-wrapped sections the argument holds, like the pattern and replacement of `:s/pat/sub/` */
  val delimitedSections: Int = 0,
)
