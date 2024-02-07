/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim

/**
 * This annotation is created for test functions (methods).
 * It means that the original vim behavior has small differences from behavior of IdeaVim.
 * [shouldBeFixed] flag indicates whether the given functionality should be fixed
 *   or the given behavior is normal for IdeaVim and should be leaved as is.
 *
 * E.g. after execution of some commands original vim has the following text:
 *    Hello1
 *    Hello2
 *    Hello3
 *
 * But IdeaVim gives you:
 *    Hello1
 *
 *    Hello2
 *    Hello3
 *
 * In this case you should still create the test function and mark this function with [VimBehaviorDiffers] annotation.
 *
 * Why does this annotation exist?
 * After creating some functionality you can understand that IdeaVim has a bit different behavior, but you
 *   cannot fix it right now because of any reason (bugs in IDE,
 *   the impossibility of this functionality in IDEA (*[shouldBeFixed] == false*), leak of time for fixing).
 *   In that case, you should NOT remove the corresponding test or leave it without any marks that this test
 *   not fully convenient with vim, but leave the test with IdeaVim's behavior and put this annotation
 *   with description of how original vim works.
 *
 * Note that using this annotation should be avoided as much as possible and behavior of IdeaVim should be as close
 *   to vim as possible.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class VimBehaviorDiffers(
  val originalVimAfter: String = "",
  val description: String = "",
  val shouldBeFixed: Boolean = true,
)
