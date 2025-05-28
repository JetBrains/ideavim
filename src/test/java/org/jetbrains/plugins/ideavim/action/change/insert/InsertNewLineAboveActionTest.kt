/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.ijOptions
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InsertNewLineAboveActionTest : VimTestCase() {
  @Test
  fun `test insert new line above`() {
    val before = """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """Lorem ipsum dolor sit amet,
        |$c
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest("O", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert new line above with caret in middle of line`() {
    val before = """I found it in a legendary land
        |all rocks and ${c}lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    val after = """I found it in a legendary land
        |$c
        |all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("O", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert new line above matches indent for plain text`() {
    val before = """    Lorem ipsum dolor sit amet,
        |    consectetur adipiscing elit
        |    ${c}Sed in orci mauris.
        |    Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """    Lorem ipsum dolor sit amet,
        |    consectetur adipiscing elit
        |    $c
        |    Sed in orci mauris.
        |    Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest("O", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert new line above matches indent for first line of plain text`() {
    val before = """    ${c}Lorem ipsum dolor sit amet,
        |    consectetur adipiscing elit
        |    Sed in orci mauris.
        |    Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """    $c
        |    Lorem ipsum dolor sit amet,
        |    consectetur adipiscing elit
        |    Sed in orci mauris.
        |    Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest("O", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert new line above with multiple carets`() {
    val before = """    I fou${c}nd it in a legendary land
        |    all rocks and laven${c}der and tufted grass,
        |    where it was sett${c}led on some sodden sand
        |    hard by the tor${c}rent of a mountain pass.
    """.trimMargin()
    val after = """    $c
        |    I found it in a legendary land
        |    $c
        |    all rocks and lavender and tufted grass,
        |    $c
        |    where it was settled on some sodden sand
        |    $c
        |    hard by the torrent of a mountain pass.
    """.trimMargin()
    doTest("O", before, after, Mode.INSERT)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test insert new line above at top of screen does not scroll top of screen`() {
    configureByLines(50, "Lorem ipsum dolor sit amet,")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(5, 15)
    typeText("O")
    assertPosition(15, 0)
    assertVisibleArea(5, 39)
  }

  @Test
  fun `test insert new line above first line`() {
    val before = """${c}Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """
        |$c
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest("O", before, after, Mode.INSERT)
  }

  @Test
  fun `test insert line above first line that is soft-wrapped`() {
    val before = """
          Lorem Ipsumm dolor sit amet, consectetur adipiscing elit. Morbi gravida commodo orci, egestas placerat ${c}purus rhoncus non. Donec efficitur placerat lorem, non ullamcorper nisl. Aliquam vestibulum, purus a pretium sodales, lorem libero placerat tortor, ut gravida est arcu nec purus. Suspendisse luctus euismod mi, at consectetur sapien facilisis sed. Duis eu magna id nisi lacinia vehicula in quis mauris. Donec tincidunt, erat in euismod placerat, tortor eros efficitur ligula, non finibus metus enim in ex. Nam commodo libero quis vestibulum congue. Vivamus sit amet tincidunt orci, in luctus tortor. Ut aliquam porttitor pharetra. Sed vel mi lacinia, auctor eros vel, condimentum eros. Fusce suscipit auctor venenatis. Aliquam elit risus, eleifend quis mollis eu, venenatis quis ex. Nunc varius consectetur eros sit amet efficitur. Donec a elit rutrum, tristique est in, maximus sem. Donec eleifend magna vitae suscipit viverra. Phasellus luctus aliquam tellus viverra consequat.

          Aliquam tristique eros vel magna dictum tincidunt. Duis sagittis mi et bibendum congue. Donec sollicitudin, ipsum quis pellentesque efficitur, metus quam congue nulla, vel rutrum neque lectus vitae sem. In accumsan scelerisque risus, ac sollicitudin purus ornare in. Proin leo erat, tempus vitae purus nec, lobortis bibendum tortor. Aenean mauris sem, interdum id facilisis et, ullamcorper ut libero. Quisque magna ligula, euismod sit amet ipsum non, maximus ultrices nulla. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Cras facilisis arcu vitae orci scelerisque, vel dignissim massa dapibus. Fusce sed urna ut orci pellentesque consectetur. Maecenas rutrum erat ac libero elementum dictum. Donec pulvinar, sem feugiat suscipit mattis, turpis tellus consectetur dui, vitae vehicula dolor purus eu lectus. Nullam lorem ligula, aliquet id eros sed, rhoncus consequat neque. Cras eget erat non nunc convallis accumsan id in ipsum.

          In id lacus diam. Curabitur orci libero, sollicitudin sed magna efficitur, finibus elementum mi. Cras aliquam enim eu scelerisque consectetur. Ut lacinia, velit sed dictum sollicitudin, mauris metus fringilla quam, vitae pellentesque tortor leo ut lectus. Fusce facilisis, eros ac egestas porttitor, enim arcu molestie purus, ut porta erat neque ac est. Ut facilisis, ante vel feugiat ultricies, metus nulla vestibulum dui, eget luctus lorem urna sed ex. Mauris quis lectus efficitur, sollicitudin urna vel, suscipit mi. Aliquam fringilla fermentum nunc. Phasellus suscipit nunc a dui gravida, sed euismod elit mattis. Donec pharetra, sem at finibus fermentum, dui lacus ornare arcu, eget maximus massa purus at ipsum.

          Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nunc quis ligula sed quam suscipit varius id et metus. Ut hendrerit diam eu turpis semper luctus. Aliquam efficitur tortor ut eros consectetur tristique. Vestibulum odio nunc, finibus eu ex auctor, pharetra congue urna. Proin sit amet malesuada nisl. Proin sagittis metus diam, vitae sollicitudin eros rutrum id. Nam imperdiet lacus et mi iaculis, vitae suscipit felis consequat.

          Fusce in lectus eros. Vivamus imperdiet sodales enim id vulputate. Ut tincidunt hendrerit cursus. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Cras maximus et justo et congue. Nam iaculis elementum ultrices. Quisque nec semper eros. Nulla nisl nunc, finibus ac ligula vel, ullamcorper egestas risus. Nunc dictum cursus leo, id pulvinar augue ullamcorper ac. Vivamus condimentum nunc non justo convallis, in condimentum ante malesuada. Vivamus gravida et metus vitae porta. Integer blandit magna metus, sodales commodo nibh rutrum ac. Ut tincidunt et justo a luctus. Nunc lacus lorem, finibus id vehicula eu, gravida ut augue.
    """.trimMargin()
    val after = """
          ${c}
          Lorem Ipsumm dolor sit amet, consectetur adipiscing elit. Morbi gravida commodo orci, egestas placerat purus rhoncus non. Donec efficitur placerat lorem, non ullamcorper nisl. Aliquam vestibulum, purus a pretium sodales, lorem libero placerat tortor, ut gravida est arcu nec purus. Suspendisse luctus euismod mi, at consectetur sapien facilisis sed. Duis eu magna id nisi lacinia vehicula in quis mauris. Donec tincidunt, erat in euismod placerat, tortor eros efficitur ligula, non finibus metus enim in ex. Nam commodo libero quis vestibulum congue. Vivamus sit amet tincidunt orci, in luctus tortor. Ut aliquam porttitor pharetra. Sed vel mi lacinia, auctor eros vel, condimentum eros. Fusce suscipit auctor venenatis. Aliquam elit risus, eleifend quis mollis eu, venenatis quis ex. Nunc varius consectetur eros sit amet efficitur. Donec a elit rutrum, tristique est in, maximus sem. Donec eleifend magna vitae suscipit viverra. Phasellus luctus aliquam tellus viverra consequat.

          Aliquam tristique eros vel magna dictum tincidunt. Duis sagittis mi et bibendum congue. Donec sollicitudin, ipsum quis pellentesque efficitur, metus quam congue nulla, vel rutrum neque lectus vitae sem. In accumsan scelerisque risus, ac sollicitudin purus ornare in. Proin leo erat, tempus vitae purus nec, lobortis bibendum tortor. Aenean mauris sem, interdum id facilisis et, ullamcorper ut libero. Quisque magna ligula, euismod sit amet ipsum non, maximus ultrices nulla. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Cras facilisis arcu vitae orci scelerisque, vel dignissim massa dapibus. Fusce sed urna ut orci pellentesque consectetur. Maecenas rutrum erat ac libero elementum dictum. Donec pulvinar, sem feugiat suscipit mattis, turpis tellus consectetur dui, vitae vehicula dolor purus eu lectus. Nullam lorem ligula, aliquet id eros sed, rhoncus consequat neque. Cras eget erat non nunc convallis accumsan id in ipsum.

          In id lacus diam. Curabitur orci libero, sollicitudin sed magna efficitur, finibus elementum mi. Cras aliquam enim eu scelerisque consectetur. Ut lacinia, velit sed dictum sollicitudin, mauris metus fringilla quam, vitae pellentesque tortor leo ut lectus. Fusce facilisis, eros ac egestas porttitor, enim arcu molestie purus, ut porta erat neque ac est. Ut facilisis, ante vel feugiat ultricies, metus nulla vestibulum dui, eget luctus lorem urna sed ex. Mauris quis lectus efficitur, sollicitudin urna vel, suscipit mi. Aliquam fringilla fermentum nunc. Phasellus suscipit nunc a dui gravida, sed euismod elit mattis. Donec pharetra, sem at finibus fermentum, dui lacus ornare arcu, eget maximus massa purus at ipsum.

          Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nunc quis ligula sed quam suscipit varius id et metus. Ut hendrerit diam eu turpis semper luctus. Aliquam efficitur tortor ut eros consectetur tristique. Vestibulum odio nunc, finibus eu ex auctor, pharetra congue urna. Proin sit amet malesuada nisl. Proin sagittis metus diam, vitae sollicitudin eros rutrum id. Nam imperdiet lacus et mi iaculis, vitae suscipit felis consequat.

          Fusce in lectus eros. Vivamus imperdiet sodales enim id vulputate. Ut tincidunt hendrerit cursus. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Cras maximus et justo et congue. Nam iaculis elementum ultrices. Quisque nec semper eros. Nulla nisl nunc, finibus ac ligula vel, ullamcorper egestas risus. Nunc dictum cursus leo, id pulvinar augue ullamcorper ac. Vivamus condimentum nunc non justo convallis, in condimentum ante malesuada. Vivamus gravida et metus vitae porta. Integer blandit magna metus, sodales commodo nibh rutrum ac. Ut tincidunt et justo a luctus. Nunc lacus lorem, finibus id vehicula eu, gravida ut augue.
    """.trimMargin()
    doTest("O", before, after, Mode.INSERT) {
      // This test is for soft wraps. The caret should be on 0 logical position and 1 visual position
      val editor = fixture.editor.vim
      assertTrue(injector.ijOptions(editor).wrap)
      assertEquals(1, editor.carets().single().getVisualPosition().line)
    }
  }

  @Test
  fun `test insert new line above clears status line`() {
    configureByText("lorem ipsum")
    enterSearch("dolor")
    assertStatusLineMessageContains("Pattern not found: dolor")
    typeText("O")
    assertStatusLineCleared()
  }
}
