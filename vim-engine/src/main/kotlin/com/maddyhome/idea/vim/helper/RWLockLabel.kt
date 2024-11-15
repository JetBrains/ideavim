/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

/**
 * This annotations marks if annotated function required read or write lock
 */
@Target
annotation class RWLockLabel {
  /**
   * [Readonly] annotation means that annotated function should be called from read action
   * This annotation is only a marker and doesn't enable r/w lock automatically
   */
  @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
  annotation class Readonly

  /**
   * [Writable] annotation means that annotated function should be called from write action
   * This annotation is only a marker and doesn't enable r/w lock automatically
   */
  @Suppress("unused")
  @Target(AnnotationTarget.FUNCTION)
  annotation class Writable

  /**
   * [SelfSynchronized] annotation means that annotated function handles read/write lock by itself
   * This annotation is only a marker and doesn't enable r/w lock automatically
   */
  @Target(AnnotationTarget.FUNCTION)
  annotation class SelfSynchronized

  /**
   * [NoLockRequired] annotation means that annotated function doesn't require any lock
   * This annotation is only a marker and doesn't enable r/w lock automatically
   */
  @Target(AnnotationTarget.FUNCTION)
  annotation class NoLockRequired
}
