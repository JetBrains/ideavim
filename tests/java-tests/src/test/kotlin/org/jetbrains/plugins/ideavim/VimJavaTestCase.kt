/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim

/**
 * Base test case for tests that require Java file type support.
 *
 * This class extends [VimTestCase] and is specifically designed for tests that need to work with
 * Java source files, where language-specific features like auto-indentation, code folding, or
 * syntax-aware operations are required.
 *
 * Tests can use the inherited [configureByJavaText] method from [VimTestCase] to set up Java files.
 * This class serves as a semantic marker and provides a convenient base for Java-specific test setup
 * if needed in the future.
 *
 * @see VimTestCase
 */
abstract class VimJavaTestCase : VimTestCase()