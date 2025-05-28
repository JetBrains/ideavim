/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.updown

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionUpCtrlPAction : VimTestCase() {
  @Test
  fun `test last column empty`() {
    val keys = "o<Esc><End><C-P>"
    val before = """
        ${c}Lorem Ipsumm dolor sit amet, consectetur adipiscing elit. Morbi gravida commodo orci, egestas placerat purus rhoncus non. Donec efficitur placerat lorem, non ullamcorper nisl. Aliquam vestibulum, purus a pretium sodales, lorem libero placerat tortor, ut gravida est arcu nec purus. Suspendisse luctus euismod mi, at consectetur sapien facilisis sed. Duis eu magna id nisi lacinia vehicula in quis mauris. Donec tincidunt, erat in euismod placerat, tortor eros efficitur ligula, non finibus metus enim in ex. Nam commodo libero quis vestibulum congue. Vivamus sit amet tincidunt orci, in luctus tortor. Ut aliquam porttitor pharetra. Sed vel mi lacinia, auctor eros vel, condimentum eros. Fusce suscipit auctor venenatis. Aliquam elit risus, eleifend quis mollis eu, venenatis quis ex. Nunc varius consectetur eros sit amet efficitur. Donec a elit rutrum, tristique est in, maximus sem. Donec eleifend magna vitae suscipit viverra. Phasellus luctus aliquam tellus viverra consequat.
    """.trimIndent()
    val after = """
        Lorem Ipsumm dolor sit amet, consectetur adipiscing elit. Morbi gravida commodo orci, egestas placerat purus rhoncus non. Donec efficitur placerat lorem, non ullamcorper nisl. Aliquam vestibulum, purus a pretium sodales, lorem libero placerat tortor, ut gravida est arcu nec purus. Suspendisse luctus euismod mi, at consectetur sapien facilisis sed. Duis eu magna id nisi lacinia vehicula in quis mauris. Donec tincidunt, erat in euismod placerat, tortor eros efficitur ligula, non finibus metus enim in ex. Nam commodo libero quis vestibulum congue. Vivamus sit amet tincidunt orci, in luctus tortor. Ut aliquam porttitor pharetra. Sed vel mi lacinia, auctor eros vel, condimentum eros. Fusce suscipit auctor venenatis. Aliquam elit risus, eleifend quis mollis eu, venenatis quis ex. Nunc varius consectetur eros sit amet efficitur. Donec a elit rutrum, tristique est in, maximus sem. Donec eleifend magna vitae suscipit viverra. Phasellus luctus aliquam tellus viverra consequat${c}.
        
    """.trimIndent()
    doTest(keys, before, after)
  }
}