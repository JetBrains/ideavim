/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.matchit

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class MatchitPhpTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("matchit")
  }

  @Test
  fun `test jump from opening HTML angle bracket to closing bracket`() {
    doTest(
      "%",
      "$c<h1>Heading</h1>",
      "<h1$c>Heading</h1>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from opening HTML tag to closing tag`() {
    doTest(
      "%",
      "<${c}h1>Heading</h1>",
      "<h1>Heading<$c/h1>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from opening angle bracket to closing question mark`() {
    doTest(
      "%",
      "$c<?php \$n=1 ?>",
      "<?php \$n=1 $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from whitespace before opening angle bracket to closing question mark`() {
    doTest(
      "%",
      "$c  <?php \$n=1 ?>",
      "  <?php \$n=1 $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from closing angle bracket to opening angle bracket`() {
    doTest(
      "%",
      "<?php \$n=1 ?$c>",
      "$c<?php \$n=1 ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from opening question mark to closing question mark`() {
    doTest(
      "%",
      "<$c?php \$n=1 ?>",
      "<?php \$n=1 $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from closing question mark to opening angle bracket`() {
    doTest(
      "%",
      "<?php \$n=1 $c?>",
      "$c<?php \$n=1 ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from php to closing question mark`() {
    doTest(
      "%",
      "<?ph${c}p \$n=1 ?>",
      "<?php \$n=1 $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from angle bracket on short opening to closing question mark`() {
    doTest(
      "%",
      "$c<?= func(123) ?>",
      "<?= func(123) $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from question mark on short opening to closing question mark`() {
    doTest(
      "%",
      "<$c?= func(123) ?>",
      "<?= func(123) $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from equals on short opening to closing question mark`() {
    doTest(
      "%",
      "<?$c= func(123) ?>",
      "<?= func(123) $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from function to closing paren`() {
    doTest(
      "%",
      "<?= ${c}func(123) ?>",
      "<?= func(123$c) ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from whitespace before question mark to opening angle bracket`() {
    doTest(
      "%",
      "<?= func(123)$c ?>",
      "$c<?= func(123) ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from closing angle bracket to opening bracket on short tag`() {
    doTest(
      "%",
      "<?= func(123) ?$c>",
      "$c<?= func(123) ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from angle bracket on extra short opening to closing question mark`() {
    doTest(
      "%",
      "$c<? func(123) ?>",
      "<? func(123) $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from question mark on extra short opening to closing question mark`() {
    doTest(
      "%",
      "<$c? func(123) ?>",
      "<? func(123) $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from function to closing paren in extra short tags`() {
    doTest(
      "%",
      "<? ${c}func(123) ?>",
      "<? func(123$c) ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from whitespace before question mark to opening extra short tag`() {
    doTest(
      "%",
      "<? func(123)$c ?>",
      "$c<? func(123) ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from closing angle bracket to opening bracket on extra short tag`() {
    doTest(
      "%",
      "<? func(123) ?$c>",
      "$c<? func(123) ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from if to endif`() {
    doTest(
      "%",
      """
        ${c}if (true):
          echo "true";
        endif;
      """.trimIndent(),
      """
        if (true):
          echo "true";
        ${c}endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from endif to if`() {
    doTest(
      "%",
      """
        if (true):
          echo "true";
        ${c}endif;
      """.trimIndent(),
      """
        ${c}if (true):
          echo "true";
        endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  // NOTE: Dollar signs are excluded from code examples to avoid escaping issues.

  @Test
  fun `test jump from if to else`() {
    doTest(
      "%",
      """
        ${c}if (x > 0):
          echo "x is greater than 0";
        else:
          echo "x is not greater than 0";
        endif;
      """.trimIndent(),
      """
        if (x > 0):
          echo "x is greater than 0";
        ${c}else:
          echo "x is not greater than 0";
        endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from else to endif`() {
    doTest(
      "%",
      """
        if (x > 0):
          echo "x is greater than 0";
        ${c}else:
          echo "x is not greater than 0";
        endif;
      """.trimIndent(),
      """
        if (x > 0):
          echo "x is greater than 0";
        else:
          echo "x is not greater than 0";
        ${c}endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from endif to if in if-else structure`() {
    doTest(
      "%",
      """
        if (x > 0):
          echo "x is greater than 0";
        else:
          echo "x is not greater than 0";
        ${c}endif;
      """.trimIndent(),
      """
        ${c}if (x > 0):
          echo "x is greater than 0";
        else:
          echo "x is not greater than 0";
        endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from if to elseif`() {
    doTest(
      "%",
      """
        ${c}if (first):
          echo "first"
        elseif (second):
          echo "second"
        else:
          echo "false"
        endif;
      """.trimIndent(),
      """
        if (first):
          echo "first"
        ${c}elseif (second):
          echo "second"
        else:
          echo "false"
        endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from elseif to else`() {
    doTest(
      "%",
      """
        if (first):
          echo "first"
        ${c}elseif (second):
          echo "second"
        else:
          echo "false"
        endif;
      """.trimIndent(),
      """
        if (first):
          echo "first"
        elseif (second):
          echo "second"
        ${c}else:
          echo "false"
        endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from endif to if in else-if structure`() {
    doTest(
      "%",
      """
        if (first):
          echo "first"
        elseif (second):
          echo "second"
        else:
          echo "false"
        ${c}endif;
      """.trimIndent(),
      """
        ${c}if (first):
          echo "first"
        elseif (second):
          echo "second"
        else:
          echo "false"
        endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from switch to case`() {
    doTest(
      "%",
      """
        ${c}switch (x):
          case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      """
        switch (x):
          ${c}case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from case to continue`() {
    doTest(
      "%",
      """
        switch (x):
          ${c}case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      """
        switch (x):
          case "one":
            echo "one";
            ${c}continue;
          case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from continue to case`() {
    doTest(
      "%",
      """
        switch (x):
          case "one":
            echo "one";
            ${c}continue;
          case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      """
        switch (x):
          case "one":
            echo "one";
            continue;
          ${c}case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from case to break`() {
    doTest(
      "%",
      """
        switch (x):
          case "one":
            echo "one";
            continue;
          ${c}case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      """
        switch (x):
          case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            ${c}break;
        endswitch;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from break to endswitch`() {
    doTest(
      "%",
      """
        switch (x):
          case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            ${c}break;
        endswitch;
      """.trimIndent(),
      """
        switch (x):
          case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            break;
        ${c}endswitch;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from endswitch to switch`() {
    doTest(
      "%",
      """
        switch (x):
          case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            break;
        ${c}endswitch;
      """.trimIndent(),
      """
        ${c}switch (x):
          case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from while to endwhile`() {
    doTest(
      "%",
      """
        ${c}while (i <= 10):
          echo i
        endwhile;
      """.trimIndent(),
      """
        while (i <= 10):
          echo i
        ${c}endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from endwhile to while`() {
    doTest(
      "%",
      """
        while (i <= 10):
          echo i
        ${c}endwhile;
      """.trimIndent(),
      """
        ${c}while (i <= 10):
          echo i
        endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from while to continue`() {
    doTest(
      "%",
      """
        ${c}while (n <= 10):
          echo n
          if (n % 2 == 0):
            continue;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            ${c}continue;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from continue to endwhile`() {
    doTest(
      "%",
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            ${c}continue;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            continue;
          endif;
          n++;
        ${c}endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from endwhile to while over continue`() {
    doTest(
      "%",
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            continue;
          endif;
          n++;
        ${c}endwhile;
      """.trimIndent(),
      """
        ${c}while (n <= 10):
          echo n
          if (n % 2 == 0):
            continue;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from while to break`() {
    doTest(
      "%",
      """
        ${c}while (n <= 10):
          echo n
          if (n % 2 == 0):
            break;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            ${c}break;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from break to endwhile`() {
    doTest(
      "%",
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            ${c}break;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            break;
          endif;
          n++;
        ${c}endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from endwhile to while over break`() {
    doTest(
      "%",
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            break;
          endif;
          n++;
        ${c}endwhile;
      """.trimIndent(),
      """
        ${c}while (n <= 10):
          echo n
          if (n % 2 == 0):
            break;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from inner while to endwhile`() {
    doTest(
      "%",
      """
        while (): ${c}while (): endwhile; endwhile;
      """.trimIndent(),
      """
        while (): while (): ${c}endwhile; endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from inner endwhile to while`() {
    doTest(
      "%",
      """
        while (): while (): ${c}endwhile; endwhile;
      """.trimIndent(),
      """
        while (): ${c}while (): endwhile; endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from for to endfor`() {
    doTest(
      "%",
      """
        ${c}for (i = 1; i <= 5; i++):
          echo i;
        endfor;
      """.trimIndent(),
      """
        for (i = 1; i <= 5; i++):
          echo i;
        ${c}endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from endfor to for`() {
    doTest(
      "%",
      """
        for (i = 1; i <= 5; i++):
          echo i;
        ${c}endfor;
      """.trimIndent(),
      """
        ${c}for (i = 1; i <= 5; i++):
          echo i;
        endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from for to continue`() {
    doTest(
      "%",
      """
        ${c}for (i = 1; i <= 5; i++):
          if (i == 1) continue;
          echo i;
        endfor;
      """.trimIndent(),
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) ${c}continue;
          echo i;
        endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from continue to endfor`() {
    doTest(
      "%",
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) ${c}continue;
          echo i;
        endfor;
      """.trimIndent(),
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) continue;
          echo i;
        ${c}endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from endfor to for over continue`() {
    doTest(
      "%",
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) continue;
          echo i;
        ${c}endfor;
      """.trimIndent(),
      """
        ${c}for (i = 1; i <= 5; i++):
          if (i == 1) continue;
          echo i;
        endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from for to break`() {
    doTest(
      "%",
      """
        ${c}for (i = 1; i <= 5; i++):
          if (i == 1) break;
          echo i;
        endfor;
      """.trimIndent(),
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) ${c}break;
          echo i;
        endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from break to endfor`() {
    doTest(
      "%",
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) ${c}break;
          echo i;
        endfor;
      """.trimIndent(),
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) break;
          echo i;
        ${c}endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from endfor to for over break`() {
    doTest(
      "%",
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) break;
          echo i;
        ${c}endfor;
      """.trimIndent(),
      """
        ${c}for (i = 1; i <= 5; i++):
          if (i == 1) break;
          echo i;
        endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from foreach to endforeach`() {
    doTest(
      "%",
      """
        ${c}foreach (nums as n):
          echo n;
        endforeach;
      """.trimIndent(),
      """
        foreach (nums as n):
          echo n;
        ${c}endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from endforeach to foreach`() {
    doTest(
      "%",
      """
        foreach (nums as n):
          echo n;
        ${c}endforeach;
      """.trimIndent(),
      """
        ${c}foreach (nums as n):
          echo n;
        endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from foreach to continue`() {
    doTest(
      "%",
      """
        ${c}foreach (nums as n):
          if (n == 1): continue
          echo n;
        endforeach;
      """.trimIndent(),
      """
        foreach (nums as n):
          if (n == 1): ${c}continue
          echo n;
        endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from continue to endforeach`() {
    doTest(
      "%",
      """
        foreach (nums as n):
          if (n == 1): ${c}continue
          echo n;
        endforeach;
      """.trimIndent(),
      """
        foreach (nums as n):
          if (n == 1): continue
          echo n;
        ${c}endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from endforeach to foreach over continue`() {
    doTest(
      "%",
      """
        foreach (nums as n):
          if (n == 1): continue
          echo n;
        ${c}endforeach;
      """.trimIndent(),
      """
        ${c}foreach (nums as n):
          if (n == 1): continue
          echo n;
        endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from foreach to break`() {
    doTest(
      "%",
      """
        ${c}foreach (nums as n):
          if (n == 1): break
          echo n;
        endforeach;
      """.trimIndent(),
      """
        foreach (nums as n):
          if (n == 1): ${c}break
          echo n;
        endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from break to endforeach`() {
    doTest(
      "%",
      """
        foreach (nums as n):
          if (n == 1): ${c}break
          echo n;
        endforeach;
      """.trimIndent(),
      """
        foreach (nums as n):
          if (n == 1): break
          echo n;
        ${c}endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from endforeach to foreach over break`() {
    doTest(
      "%",
      """
        foreach (nums as n):
          if (n == 1): break
          echo n;
        ${c}endforeach;
      """.trimIndent(),
      """
        ${c}foreach (nums as n):
          if (n == 1): break
          echo n;
        endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from opening heredoc to closing`() {
    doTest(
      "%",
      """
        m = <<<${c}EOT
        message
        EOT;
      """.trimIndent(),
      """
        m = <<<EOT
        message
        ${c}EOT;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from closing heredoc to opening`() {
    doTest(
      "%",
      """
        m = <<<EOT
        message
        EO${c}T;
      """.trimIndent(),
      """
        m = <<<${c}EOT
        message
        EOT;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from opening nowdoc to closing`() {
    doTest(
      "%",
      """
        m = <<<${c}'EOT'
        nowdoc
        EOT;
      """.trimIndent(),
      """
        m = <<<'EOT'
        nowdoc
        ${c}EOT;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from closing nowdoc to opening`() {
    doTest(
      "%",
      """
        m = <<<'EOT'
        nowdoc
        ${c}EOT;
      """.trimIndent(),
      """
        m = <<<${c}'EOT'
        nowdoc
        EOT;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from opening nowdoc to closing in array`() {
    doTest(
      "%",
      """
        values = [<<<${c}'EOD'
          a
          EOD, 'c d e'];
      """.trimIndent(),
      """
        values = [<<<'EOD'
          a
          ${c}EOD, 'c d e'];
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from closing nowdoc to opening in array`() {
    doTest(
      "%",
      """
        values = [<<<'EOD'
          a
          ${c}EOD, 'c d e'];
      """.trimIndent(),
      """
        values = [<<<${c}'EOD'
          a
          EOD, 'c d e'];
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from do to while`() {
    doTest(
      "%",
      """
        ${c}do {
          n++;
        } while (n <= 10);
      """.trimIndent(),
      """
        do {
          n++;
        } ${c}while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from while to do`() {
    doTest(
      "%",
      """
        do {
          n++;
        } ${c}while (n <= 10);
      """.trimIndent(),
      """
        ${c}do {
          n++;
        } while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from do to continue in do-while`() {
    doTest(
      "%",
      """
        ${c}do {
          if (n == 2) { continue; }
        } while (n <= 10);
      """.trimIndent(),
      """
        do {
          if (n == 2) { ${c}continue; }
        } while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from continue to while in do-while`() {
    doTest(
      "%",
      """
        do {
          if (n == 2) { ${c}continue; }
        } while (n <= 10);
      """.trimIndent(),
      """
        do {
          if (n == 2) { continue; }
        } ${c}while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from while to do over continue`() {
    doTest(
      "%",
      """
        do {
          if (n == 2) { continue; }
        } ${c}while (n <= 10);
      """.trimIndent(),
      """
        ${c}do {
          if (n == 2) { continue; }
        } while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from do to break in do-while`() {
    doTest(
      "%",
      """
        ${c}do {
          if (n == 2) { break; }
        } while (n <= 10);
      """.trimIndent(),
      """
        do {
          if (n == 2) { ${c}break; }
        } while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from break to while in do-while`() {
    doTest(
      "%",
      """
        do {
          if (n == 2) { ${c}break; }
        } while (n <= 10);
      """.trimIndent(),
      """
        do {
          if (n == 2) { break; }
        } ${c}while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test jump from while to do over break`() {
    doTest(
      "%",
      """
        do {
          if (n == 2) { break; }
        } ${c}while (n <= 10);
      """.trimIndent(),
      """
        ${c}do {
          if (n == 2) { break; }
        } while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  /*
   *  g% motion tests.
   */

  @Test
  fun `test reverse jump from opening angle bracket to closing question mark`() {
    doTest(
      "g%",
      "$c<?php \$n=1 ?>",
      "<?php \$n=1 $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from whitespace before opening angle bracket to closing question mark`() {
    doTest(
      "g%",
      "$c  <?php \$n=1 ?>",
      "  <?php \$n=1 $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from closing angle bracket to opening angle bracket`() {
    doTest(
      "g%",
      "<?php \$n=1 ?$c>",
      "$c<?php \$n=1 ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from opening question mark to closing question mark`() {
    doTest(
      "g%",
      "<$c?php \$n=1 ?>",
      "<?php \$n=1 $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from closing question mark to opening angle bracket`() {
    doTest(
      "g%",
      "<?php \$n=1 $c?>",
      "$c<?php \$n=1 ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from php to closing question mark`() {
    doTest(
      "g%",
      "<?ph${c}p \$n=1 ?>",
      "<?php \$n=1 $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from angle bracket on short opening to closing question mark`() {
    doTest(
      "g%",
      "$c<?= func(123) ?>",
      "<?= func(123) $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from question mark on short opening to closing question mark`() {
    doTest(
      "g%",
      "<$c?= func(123) ?>",
      "<?= func(123) $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from equals on short opening to closing question mark`() {
    doTest(
      "g%",
      "<?$c= func(123) ?>",
      "<?= func(123) $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from function to closing paren`() {
    doTest(
      "g%",
      "<?= ${c}func(123) ?>",
      "<?= func(123$c) ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from whitespace before question mark to opening angle bracket`() {
    doTest(
      "g%",
      "<?= func(123)$c ?>",
      "$c<?= func(123) ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from closing angle bracket to opening bracket on short tag`() {
    doTest(
      "g%",
      "<?= func(123) ?$c>",
      "$c<?= func(123) ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from angle bracket on extra short opening to closing question mark`() {
    doTest(
      "g%",
      "$c<? func(123) ?>",
      "<? func(123) $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from question mark on extra short opening to closing question mark`() {
    doTest(
      "g%",
      "<$c? func(123) ?>",
      "<? func(123) $c?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from function to closing paren in extra short tags`() {
    doTest(
      "g%",
      "<? ${c}func(123) ?>",
      "<? func(123$c) ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from whitespace before question mark to opening extra short tag`() {
    doTest(
      "g%",
      "<? func(123)$c ?>",
      "$c<? func(123) ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from closing angle bracket to opening bracket on extra short tag`() {
    doTest(
      "g%",
      "<? func(123) ?$c>",
      "$c<? func(123) ?>",
      Mode.NORMAL(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from if to endif`() {
    doTest(
      "g%",
      """
        ${c}if (true):
          echo "true";
        endif;
      """.trimIndent(),
      """
        if (true):
          echo "true";
        ${c}endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from endif to if`() {
    doTest(
      "g%",
      """
        if (true):
          echo "true";
        ${c}endif;
      """.trimIndent(),
      """
        ${c}if (true):
          echo "true";
        endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from if to endif in if-else structure`() {
    doTest(
      "g%",
      """
        ${c}if (x > 0):
          echo "x is greater than 0";
        else:
          echo "x is not greater than 0";
        endif;
      """.trimIndent(),
      """
        if (x > 0):
          echo "x is greater than 0";
        else:
          echo "x is not greater than 0";
        ${c}endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from else to if`() {
    doTest(
      "g%",
      """
        if (x > 0):
          echo "x is greater than 0";
        ${c}else:
          echo "x is not greater than 0";
        endif;
      """.trimIndent(),
      """
        ${c}if (x > 0):
          echo "x is greater than 0";
        else:
          echo "x is not greater than 0";
        endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from endif to else in if-else structure`() {
    doTest(
      "g%",
      """
        if (x > 0):
          echo "x is greater than 0";
        else:
          echo "x is not greater than 0";
        ${c}endif;
      """.trimIndent(),
      """
        if (x > 0):
          echo "x is greater than 0";
        ${c}else:
          echo "x is not greater than 0";
        endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from elseif to if`() {
    doTest(
      "g%",
      """
        if (first):
          echo "first"
        ${c}elseif (second):
          echo "second"
        else:
          echo "false"
        endif;
      """.trimIndent(),
      """
        ${c}if (first):
          echo "first"
        elseif (second):
          echo "second"
        else:
          echo "false"
        endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from else to elseif`() {
    doTest(
      "g%",
      """
        if (first):
          echo "first"
        elseif (second):
          echo "second"
        ${c}else:
          echo "false"
        endif;
      """.trimIndent(),
      """
        if (first):
          echo "first"
        ${c}elseif (second):
          echo "second"
        else:
          echo "false"
        endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from elseif to elseif`() {
    doTest(
      "g%",
      """
        if (first):
          echo "first"
        elseif (second):
          echo "second"
        ${c}elseif (third):
          echo "third"
        else:
          echo "false"
        endif;
      """.trimIndent(),
      """
        if (first):
          echo "first"
        ${c}elseif (second):
          echo "second"
        elseif (third):
          echo "third"
        else:
          echo "false"
        endif;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from switch to endswitch`() {
    doTest(
      "g%",
      """
        ${c}switch (x):
          case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      """
        switch (x):
          case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            break;
        ${c}endswitch;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from endswitch to break`() {
    doTest(
      "g%",
      """
        switch (x):
          case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            break;
        ${c}endswitch;
      """.trimIndent(),
      """
        switch (x):
          case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            ${c}break;
        endswitch;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from break to case`() {
    doTest(
      "g%",
      """
        switch (x):
          case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            ${c}break;
        endswitch;
      """.trimIndent(),
      """
        switch (x):
          case "one":
            echo "one";
            continue;
          ${c}case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from case to continue`() {
    doTest(
      "g%",
      """
        switch (x):
          case "one":
            echo "one";
            continue;
          ${c}case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      """
        switch (x):
          case "one":
            echo "one";
            ${c}continue;
          case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from continue to case`() {
    doTest(
      "g%",
      """
        switch (x):
          case "one":
            echo "one";
            ${c}continue;
          case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      """
        switch (x):
          ${c}case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from case to switch`() {
    doTest(
      "g%",
      """
        switch (x):
          ${c}case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      """
        ${c}switch (x):
          case "one":
            echo "one";
            continue;
          case "two":
            echo "two";
            break;
        endswitch;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from while to endwhile`() {
    doTest(
      "g%",
      """
        ${c}while (i <= 10):
          echo i
        endwhile;
      """.trimIndent(),
      """
        while (i <= 10):
          echo i
        ${c}endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from endwhile to while`() {
    doTest(
      "g%",
      """
        while (i <= 10):
          echo i
        ${c}endwhile;
      """.trimIndent(),
      """
        ${c}while (i <= 10):
          echo i
        endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from while to endwhile over continue`() {
    doTest(
      "g%",
      """
        ${c}while (n <= 10):
          echo n
          if (n % 2 == 0):
            continue;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            continue;
          endif;
          n++;
        ${c}endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from continue to while`() {
    doTest(
      "g%",
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            ${c}continue;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      """
        ${c}while (n <= 10):
          echo n
          if (n % 2 == 0):
            continue;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from endwhile to continue`() {
    doTest(
      "g%",
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            continue;
          endif;
          n++;
        ${c}endwhile;
      """.trimIndent(),
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            ${c}continue;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from while to endwhile over break`() {
    doTest(
      "g%",
      """
        ${c}while (n <= 10):
          echo n
          if (n % 2 == 0):
            break;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            break;
          endif;
          n++;
        ${c}endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from break to while`() {
    doTest(
      "g%",
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            ${c}break;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      """
        ${c}while (n <= 10):
          echo n
          if (n % 2 == 0):
            break;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from endwhile to break`() {
    doTest(
      "g%",
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            break;
          endif;
          n++;
        ${c}endwhile;
      """.trimIndent(),
      """
        while (n <= 10):
          echo n
          if (n % 2 == 0):
            ${c}break;
          endif;
          n++;
        endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from inner while to endwhile`() {
    doTest(
      "g%",
      """
        while (): ${c}while (): endwhile; endwhile;
      """.trimIndent(),
      """
        while (): while (): ${c}endwhile; endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from inner endwhile to while`() {
    doTest(
      "g%",
      """
        while (): while (): ${c}endwhile; endwhile;
      """.trimIndent(),
      """
        while (): ${c}while (): endwhile; endwhile;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from for to endfor`() {
    doTest(
      "g%",
      """
        ${c}for (i = 1; i <= 5; i++):
          echo i;
        endfor;
      """.trimIndent(),
      """
        for (i = 1; i <= 5; i++):
          echo i;
        ${c}endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from endfor to for`() {
    doTest(
      "g%",
      """
        for (i = 1; i <= 5; i++):
          echo i;
        ${c}endfor;
      """.trimIndent(),
      """
        ${c}for (i = 1; i <= 5; i++):
          echo i;
        endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from for to endfor over continue`() {
    doTest(
      "g%",
      """
        ${c}for (i = 1; i <= 5; i++):
          if (i == 1) continue;
          echo i;
        endfor;
      """.trimIndent(),
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) continue;
          echo i;
        ${c}endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from continue to for`() {
    doTest(
      "g%",
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) ${c}continue;
          echo i;
        endfor;
      """.trimIndent(),
      """
        ${c}for (i = 1; i <= 5; i++):
          if (i == 1) continue;
          echo i;
        endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from endfor to continue`() {
    doTest(
      "g%",
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) continue;
          echo i;
        ${c}endfor;
      """.trimIndent(),
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) ${c}continue;
          echo i;
        endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from for to endfor over break`() {
    doTest(
      "g%",
      """
        ${c}for (i = 1; i <= 5; i++):
          if (i == 1) break;
          echo i;
        endfor;
      """.trimIndent(),
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) break;
          echo i;
        ${c}endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from break to for`() {
    doTest(
      "g%",
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) ${c}break;
          echo i;
        endfor;
      """.trimIndent(),
      """
        ${c}for (i = 1; i <= 5; i++):
          if (i == 1) break;
          echo i;
        endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from endfor to break`() {
    doTest(
      "g%",
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) break;
          echo i;
        ${c}endfor;
      """.trimIndent(),
      """
        for (i = 1; i <= 5; i++):
          if (i == 1) ${c}break;
          echo i;
        endfor;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from foreach to endforeach`() {
    doTest(
      "g%",
      """
        ${c}foreach (nums as n):
          echo n;
        endforeach;
      """.trimIndent(),
      """
        foreach (nums as n):
          echo n;
        ${c}endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from endforeach to foreach`() {
    doTest(
      "g%",
      """
        foreach (nums as n):
          echo n;
        ${c}endforeach;
      """.trimIndent(),
      """
        ${c}foreach (nums as n):
          echo n;
        endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from foreach to endforeach over continue`() {
    doTest(
      "g%",
      """
        ${c}foreach (nums as n):
          if (n == 1): continue
          echo n;
        endforeach;
      """.trimIndent(),
      """
        foreach (nums as n):
          if (n == 1): continue
          echo n;
        ${c}endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from continue to foreach`() {
    doTest(
      "g%",
      """
        foreach (nums as n):
          if (n == 1): ${c}continue
          echo n;
        endforeach;
      """.trimIndent(),
      """
        ${c}foreach (nums as n):
          if (n == 1): continue
          echo n;
        endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from endforeach to continue`() {
    doTest(
      "g%",
      """
        foreach (nums as n):
          if (n == 1): continue
          echo n;
        ${c}endforeach;
      """.trimIndent(),
      """
        foreach (nums as n):
          if (n == 1): ${c}continue
          echo n;
        endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from foreach to endforeach over break`() {
    doTest(
      "g%",
      """
        ${c}foreach (nums as n):
          if (n == 1): break
          echo n;
        endforeach;
      """.trimIndent(),
      """
        foreach (nums as n):
          if (n == 1): break
          echo n;
        ${c}endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from break to foreach`() {
    doTest(
      "g%",
      """
        foreach (nums as n):
          if (n == 1): ${c}break
          echo n;
        endforeach;
      """.trimIndent(),
      """
        ${c}foreach (nums as n):
          if (n == 1): break
          echo n;
        endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from endforeach to break`() {
    doTest(
      "g%",
      """
        foreach (nums as n):
          if (n == 1): break
          echo n;
        ${c}endforeach;
      """.trimIndent(),
      """
        foreach (nums as n):
          if (n == 1): ${c}break
          echo n;
        endforeach;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from opening heredoc to closing`() {
    doTest(
      "g%",
      """
        m = <<<${c}EOT
        message
        EOT;
      """.trimIndent(),
      """
        m = <<<EOT
        message
        ${c}EOT;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from closing heredoc to opening`() {
    doTest(
      "g%",
      """
        m = <<<EOT
        message
        EO${c}T;
      """.trimIndent(),
      """
        m = <<<${c}EOT
        message
        EOT;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from opening nowdoc to closing`() {
    doTest(
      "g%",
      """
        m = <<<${c}'EOT'
        nowdoc
        EOT;
      """.trimIndent(),
      """
        m = <<<'EOT'
        nowdoc
        ${c}EOT;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from closing nowdoc to opening`() {
    doTest(
      "g%",
      """
        m = <<<'EOT'
        nowdoc
        ${c}EOT;
      """.trimIndent(),
      """
        m = <<<${c}'EOT'
        nowdoc
        EOT;
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from do to while`() {
    doTest(
      "g%",
      """
        ${c}do {
          n++;
        } while (n <= 10);
      """.trimIndent(),
      """
        do {
          n++;
        } ${c}while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from while to do`() {
    doTest(
      "g%",
      """
        do {
          n++;
        } ${c}while (n <= 10);
      """.trimIndent(),
      """
        ${c}do {
          n++;
        } while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from do to while over continue`() {
    doTest(
      "g%",
      """
        ${c}do {
          if (n == 2) { continue; }
        } while (n <= 10);
      """.trimIndent(),
      """
        do {
          if (n == 2) { continue; }
        } ${c}while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from continue to do`() {
    doTest(
      "g%",
      """
        do {
          if (n == 2) { ${c}continue; }
        } while (n <= 10);
      """.trimIndent(),
      """
        ${c}do {
          if (n == 2) { continue; }
        } while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from while to continue in do-while`() {
    doTest(
      "g%",
      """
        do {
          if (n == 2) { continue; }
        } ${c}while (n <= 10);
      """.trimIndent(),
      """
        do {
          if (n == 2) { ${c}continue; }
        } while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from do to while over break`() {
    doTest(
      "g%",
      """
        ${c}do {
          if (n == 2) { break; }
        } while (n <= 10);
      """.trimIndent(),
      """
        do {
          if (n == 2) { break; }
        } ${c}while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from break to do`() {
    doTest(
      "g%",
      """
        do {
          if (n == 2) { ${c}break; }
        } while (n <= 10);
      """.trimIndent(),
      """
        ${c}do {
          if (n == 2) { break; }
        } while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

  @Test
  fun `test reverse jump from while to break in do-while`() {
    doTest(
      "g%",
      """
        do {
          if (n == 2) { break; }
        } ${c}while (n <= 10);
      """.trimIndent(),
      """
        do {
          if (n == 2) { ${c}break; }
        } while (n <= 10);
      """.trimIndent(),
      fileName = "file.php",
    )
  }

}
