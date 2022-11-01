/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change

// class InsertRegisterTest : VimTestCase() {
// todo test cursor position VIM-2732
//  fun `test multiline insert from expression register`() {
//    val keys = "VjyGo<C-r>=@\"<CR>"
//    val before = """
//            A Discovery
//
//            ${c}I found it in a legendary land
//            all rocks and lavender and tufted grass,
//            where it was settled on some sodden sand
//            hard by the torrent of a mountain pass.
//    """.trimIndent()
//    val after = """
//            A Discovery
//
//            I found it in a legendary land
//            all rocks and lavender and tufted grass,
//            where it was settled on some sodden sand
//            hard by the torrent of a mountain pass.
//            I found it in a legendary land
//            all rocks and lavender and tufted grass,
//            ${c}
//    """.trimIndent()
//    doTest(keys, before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
//  }
// }
