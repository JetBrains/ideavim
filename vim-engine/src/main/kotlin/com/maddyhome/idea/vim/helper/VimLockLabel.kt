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
annotation class VimLockLabel {
  /**
   * [RequiresReadLock] annotation means that annotated function should be called from read action
   * This annotation is only a marker and doesn't enable r/w lock automatically.
   *
   * This is an analog of the RequiresReadLock from IntelliJ IDEA that is not bound to IntelliJ SDK.
   *   When using this annotation from code that has access to the IntelliJ SDK,
   *   it's better to pair it with the original annotation as that annotation also generates runtime checks.
   */
  @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
  annotation class RequiresReadLock

  /**
   * [RequiresWriteLock] annotation means that annotated function should be called from write action
   * This annotation is only a marker and doesn't enable r/w lock automatically
   *
   * This is an analog of the RequiresWriteLock from IntelliJ IDEA that is not bound to IntelliJ SDK.
   *   When using this annotation from code that has access to the IntelliJ SDK,
   *   it's better to pair it with the original annotation as that annotation also generates runtime checks.
   */
  @Suppress("unused")
  @Target(AnnotationTarget.FUNCTION)
  annotation class RequiresWriteLock

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
