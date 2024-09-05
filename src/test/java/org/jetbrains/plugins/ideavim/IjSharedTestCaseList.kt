/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim

import com.maddyhome.idea.vim.model.SharedTestCase
import com.maddyhome.idea.vim.SharedTestCaseList
import org.jetbrains.plugins.ideavim.action.change.delete.DeleteVisualLinesActionTestImpl
import org.jetbrains.plugins.ideavim.action.change.insert.InsertDeleteActionTestImpl

@Suppress("unused")
class IjSharedTestCaseList : SharedTestCaseList {
  override val insertDeleteActionTest: SharedTestCase = InsertDeleteActionTestImpl()
  override val deleteVisualLinesActionTest: SharedTestCase = DeleteVisualLinesActionTestImpl()
}