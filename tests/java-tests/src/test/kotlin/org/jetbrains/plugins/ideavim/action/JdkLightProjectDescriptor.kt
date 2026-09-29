/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.testFramework.LightProjectDescriptor

/**
 * A light project with a real JDK, for the tests that need `java.util.*` to resolve - without one there is nothing to
 * import and nothing to complete.
 *
 * One instance, shared by every test that needs it: a descriptor is the key a light project is cached under, so a
 * second instance means a second project, and a project that outlives its test is what `LeakHunter` fails the suite
 * over (see `fbb1836dd`, "VIM-2010 fix editor leak in tc tests").
 */
internal val WITH_REAL_JDK: LightProjectDescriptor = object : LightProjectDescriptor() {
  override fun getSdk(): Sdk = JavaSdk.getInstance().createJdk("Test JDK", System.getProperty("java.home"), false)
}
