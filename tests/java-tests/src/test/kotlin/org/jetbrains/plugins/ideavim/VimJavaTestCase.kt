/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim

import com.intellij.ide.highlighter.JavaFileType

abstract class VimJavaTestCase : VimTestCase() {
  protected fun configureByJavaText(content: String) = configureByText(JavaFileType.INSTANCE, content)
}