/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.model

/**
 * List of all shared test classes.
 * Every IDE (Fleet, IJ-based, etc.) should have a successor to this class in its test files.
 * When a new test is added to vim-engine (and listed in this class), compilation of tests in all IDEs will fail,
 * requiring developers of the Vim plugin for each IDE to take action and implement the missing test.
 * This class doesn't run any tests; it only declares them.
 * Compilation errors resulting from this class will help IDE developers understand whether they have implemented all the vim-engine tests.
 */
abstract class SharedTestCaseList {
}