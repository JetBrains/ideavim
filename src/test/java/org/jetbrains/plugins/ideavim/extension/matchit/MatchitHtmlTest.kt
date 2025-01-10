/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.matchit

import com.intellij.ide.highlighter.HtmlFileType
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class MatchitHtmlTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("matchit")
  }

  @Test
  fun `test basic jump to closing tag`() {
    doTest(
      "%",
      """
        <${c}h1>Heading</h1>
      """.trimIndent(),
      """
        <h1>Heading<$c/h1>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test basic jump to opening tag`() {
    doTest(
      "%",
      """
        <h1>Heading</${c}h1>
      """.trimIndent(),
      """
        <${c}h1>Heading</h1>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test multiline jump to closing tag`() {
    doTest(
      "%",
      """
        <${c}div>
          <p>paragraph body</p>
        </div>
      """.trimIndent(),
      """
        <div>
          <p>paragraph body</p>
        <$c/div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test multiline jump to opening tag`() {
    doTest(
      "%",
      """
        <div>
          <p>paragraph body</p>
        </${c}div>
      """.trimIndent(),
      """
        <${c}div>
          <p>paragraph body</p>
        </div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump to closing tag while ignoring nested tags`() {
    doTest(
      "%",
      """
        <${c}div>
          <div>
            <div>contents</div>
          </div>
        </div>
      """.trimIndent(),
      """
        <div>
          <div>
            <div>contents</div>
          </div>
        <$c/div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump to closing tag while ignoring outer tags`() {
    doTest(
      "%",
      """
        <div>
          <${c}div>contents</div>
        </div>
      """.trimIndent(),
      """
        <div>
          <div>contents<$c/div>
        </div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump to opening tag while ignoring nested tags`() {
    doTest(
      "%",
      """
        <div>
          <div>
            <div>contents</div>
          </div>
        </d${c}iv>
      """.trimIndent(),
      """
        <${c}div>
          <div>
            <div>contents</div>
          </div>
        </div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump to opening tag while ignoring outer tags`() {
    doTest(
      "%",
      """
        <div>
          <div>contents</d${c}iv>
        </div>
      """.trimIndent(),
      """
        <div>
          <${c}div>contents</div>
        </div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump to closing tag while in tag attributes`() {
    doTest(
      "%",
      """
        <h1 class="he${c}adline">Post HeadLine</h1>
      """.trimIndent(),
      """
        <h1 class="headline">Post HeadLine<$c/h1>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test don't jump on standalone tags`() {
    doTest(
      "%",
      """
        <div>
          <img src=$c"my-image.png" alt="my-image">
        </div>
      """.trimIndent(),
      """
        <div>
          <img src=$c"my-image.png" alt="my-image">
        </div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test don't jump on empty lines`() {
    doTest(
      "%",
      """
        <div>
        $c 
        </div>
      """.trimIndent(),
      """
        <div>
        $c 
        </div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump forwards to matching angle bracket on opening tag`() {
    doTest(
      "%",
      """
        $c<h1>Heading</h1>
      """.trimIndent(),
      """
        <h1$c>Heading</h1>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump forwards to matching angle bracket when on whitespace`() {
    doTest(
      "%",
      "$c    <h1>Heading</h1>",
      "    <h1$c>Heading</h1>",
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump to last angle bracket when in tag body`() {
    doTest(
      "%",
      """
        <h1 class="headline">Post$c HeadLine</h1>
      """.trimIndent(),
      """
        <h1 class="headline">Post HeadLine</h1$c>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump forwards to matching angle bracket on closing tag`() {
    doTest(
      "%",
      """
        <h1>Heading$c</h1>
      """.trimIndent(),
      """
        <h1>Heading</h1$c>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump backwards to matching angle bracket on opening tag`() {
    doTest(
      "%",
      """
        <h1$c>Heading</h1>
      """.trimIndent(),
      """
        $c<h1>Heading</h1>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump backwards to matching angle bracket on closing tag`() {
    doTest(
      "%",
      """
        <h1>Heading</h1$c>
      """.trimIndent(),
      """
        <h1>Heading$c</h1>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump to matching square bracket inside tag`() {
    doTest(
      "%",
      """
        <div $c[ngIf]="someCondition()">{{displayValue}}</div>
      """.trimIndent(),
      """
        <div [ngIf$c]="someCondition()">{{displayValue}}</div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump to matching parenthesis inside tag`() {
    doTest(
      "%",
      """
        <div [ngIf]="someCondition$c()">{{displayValue}}</div>
      """.trimIndent(),
      """
        <div [ngIf]="someCondition($c)">{{displayValue}}</div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump to matching curly brace in tag body`() {
    doTest(
      "%",
      """
        <div [ngIf]="someCondition()">$c{{displayValue}}</div>
      """.trimIndent(),
      """
        <div [ngIf]="someCondition()">{{displayValue}$c}</div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump to closing tag when inside brackets in opening tag`() {
    doTest(
      "%",
      """
        <div [ng${c}If]="someCondition()">{{displayValue}}</div>
      """.trimIndent(),
      """
        <div [ngIf]="someCondition()">{{displayValue}}<$c/div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump to opening curly brace when in tag body`() {
    doTest(
      "%",
      """
        <div [ngIf]="someCondition()">{{dis${c}playValue}}</div>
      """.trimIndent(),
      """
        <div [ngIf]="someCondition()">{$c{displayValue}}</div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test don't jump on standalone tag with brackets on the same line`() {
    doTest(
      "%",
      """
        <img ${c}src={{imagePath}} alt={{imageDescription}}>
      """.trimIndent(),
      """
        <img ${c}src={{imagePath}} alt={{imageDescription}}>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump from opening to closing tag while ignoring comments`() {
    doTest(
      "%",
      """
        <!-- <div> -->
        <!-- This div is completely commented out -->
        <!-- </div> -->
        
        <${c}div>
          <p>paragraph 1</p>
          <!-- </div> -->
          <p>paragraph 2</p>
        </div>
      """.trimIndent(),
      """
        <!-- <div> -->
        <!-- This div is completely commented out -->
        <!-- </div> -->
        
        <div>
          <p>paragraph 1</p>
          <!-- </div> -->
          <p>paragraph 2</p>
        <$c/div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump from closing to opening tag while ignoring comments`() {
    doTest(
      "%",
      """
        <!-- <div> -->
        <!-- This div is completely commented out -->
        <!-- </div> -->
        
        <${c}div>
          <!-- This <div> holds paragraphs -->
          <p>paragraph 1</p>
          <p>paragraph 2</p>
        </div>
      """.trimIndent(),
      """
        <!-- <div> -->
        <!-- This div is completely commented out -->
        <!-- </div> -->
        
        <div>
          <!-- This <div> holds paragraphs -->
          <p>paragraph 1</p>
          <p>paragraph 2</p>
        <$c/div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump from opening to closing tag inside a comment block`() {
    doTest(
      "%",
      """
        <!-- <${c}div> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      """
        <!-- <div> -->
        <!--   This div is commented out -->
        <!-- <$c/div> -->
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump from closing to opening tag inside a comment block`() {
    doTest(
      "%",
      """
        <!-- <div> -->
        <!--   This div is commented out -->
        <!-- <$c/div> -->
      """.trimIndent(),
      """
        <!-- <${c}div> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump from opening to closing angle bracket inside a comment block`() {
    doTest(
      "%",
      """
        <!-- $c<div> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      """
        <!-- <div$c> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump from closing to opening angle bracket inside a comment block`() {
    doTest(
      "%",
      """
        <!-- <div$c> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      """
        <!-- $c<div> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump from opening to closing angle bracket on a comment marker`() {
    doTest(
      "%",
      """
        $c<!-- <div> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      """
        <!-- <div> --$c>
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump from opening to closing angle bracket ignoring bracket in string`() {
    doTest(
      "%",
      """
        $c<p *ngIf="count > 0">Count is greater than zero</p>
      """.trimIndent(),
      """
        <p *ngIf="count > 0"$c>Count is greater than zero</p>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump from multiline opening tag to closing`() {
    doTest(
      "%",
      """
        <h1 ${c}id="title"
           class="red right-aligned">
           Header Content
        </h1>
      """.trimIndent(),
      """
        <h1 id="title"
           class="red right-aligned">
           Header Content
        <$c/h1>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump from whitespace before comment angle bracket to closing bracket`() {
    doTest(
      "%",
      "$c  <!-- A comment -->",
      "  <!-- A comment --$c>",
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump from opening tag to closing tag ignoring inner tag with similar name`() {
    doTest(
      "%",
      """
        <${c}Box>
          <BoxHeading></BoxHeading>
        </Box>
      """.trimIndent(),
      """
        <Box>
          <BoxHeading></BoxHeading>
        <${c}/Box>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test jump from closing tag to opening tag ignoring inner tag with similar name`() {
    doTest(
      "%",
      """
        <Box>
          <BoxHeading></BoxHeading>
        <${c}/Box>
      """.trimIndent(),
      """
        <${c}Box>
          <BoxHeading></BoxHeading>
        </Box>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  /*
   *  g% motion tests. For HTML, g% should behave the same as %.
   */

  @Test
  fun `test reverse jump to closing tag`() {
    doTest(
      "g%",
      """
        <${c}h1>Heading</h1>
      """.trimIndent(),
      """
        <h1>Heading<$c/h1>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to opening tag`() {
    doTest(
      "g%",
      """
        <h1>Heading</${c}h1>
      """.trimIndent(),
      """
        <${c}h1>Heading</h1>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to closing tag while ignoring nested tags`() {
    doTest(
      "g%",
      """
        <${c}div>
          <div>
            <div>contents</div>
          </div>
        </div>
      """.trimIndent(),
      """
        <div>
          <div>
            <div>contents</div>
          </div>
        <$c/div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to closing tag while ignoring outer tags`() {
    doTest(
      "g%",
      """
        <div>
          <${c}div>contents</div>
        </div>
      """.trimIndent(),
      """
        <div>
          <div>contents<$c/div>
        </div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to opening tag while ignoring nested tags`() {
    doTest(
      "g%",
      """
        <div>
          <div>
            <div>contents</div>
          </div>
        </d${c}iv>
      """.trimIndent(),
      """
        <${c}div>
          <div>
            <div>contents</div>
          </div>
        </div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to opening tag while ignoring outer tags`() {
    doTest(
      "g%",
      """
        <div>
          <div>contents</d${c}iv>
        </div>
      """.trimIndent(),
      """
        <div>
          <${c}div>contents</div>
        </div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to closing tag while in tag attributes`() {
    doTest(
      "g%",
      """
        <h1 class="he${c}adline">Post HeadLine</h1>
      """.trimIndent(),
      """
        <h1 class="headline">Post HeadLine<$c/h1>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test don't reverse jump on standalone tags`() {
    doTest(
      "g%",
      """
        <div>
          <img src=$c"my-image.png" alt="my-image">
        </div>
      """.trimIndent(),
      """
        <div>
          <img src=$c"my-image.png" alt="my-image">
        </div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test don't reverse jump on empty lines`() {
    doTest(
      "g%",
      """
        <div>
        $c 
        </div>
      """.trimIndent(),
      """
        <div>
        $c 
        </div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to closing angle bracket`() {
    doTest(
      "g%",
      """
        $c<h1>Heading</h1>
      """.trimIndent(),
      """
        <h1$c>Heading</h1>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to closing angle bracket when on whitespace`() {
    doTest(
      "g%",
      "$c    <h1>Heading</h1>",
      "    <h1$c>Heading</h1>",
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to last angle bracket when in tag body`() {
    doTest(
      "g%",
      """
        <h1 class="headline">Post$c HeadLine</h1>
      """.trimIndent(),
      """
        <h1 class="headline">Post HeadLine</h1$c>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to opening angle bracket`() {
    doTest(
      "g%",
      """
        <h1$c>Heading</h1>
      """.trimIndent(),
      """
        $c<h1>Heading</h1>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to matching square bracket inside tag`() {
    doTest(
      "g%",
      """
        <div $c[ngIf]="someCondition()">{{displayValue}}</div>
      """.trimIndent(),
      """
        <div [ngIf$c]="someCondition()">{{displayValue}}</div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to matching parenthesis inside tag`() {
    doTest(
      "g%",
      """
        <div [ngIf]="someCondition$c()">{{displayValue}}</div>
      """.trimIndent(),
      """
        <div [ngIf]="someCondition($c)">{{displayValue}}</div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to matching curly brace in tag body`() {
    doTest(
      "g%",
      """
        <div [ngIf]="someCondition()">$c{{displayValue}}</div>
      """.trimIndent(),
      """
        <div [ngIf]="someCondition()">{{displayValue}$c}</div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to closing tag when inside brackets in opening tag`() {
    doTest(
      "g%",
      """
        <div [ng${c}If]="someCondition()">{{displayValue}}</div>
      """.trimIndent(),
      """
        <div [ngIf]="someCondition()">{{displayValue}}<$c/div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump to opening curly brace when in tag body`() {
    doTest(
      "g%",
      """
        <div [ngIf]="someCondition()">{{dis${c}playValue}}</div>
      """.trimIndent(),
      """
        <div [ngIf]="someCondition()">{$c{displayValue}}</div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test don't reverse jump on standalone tag with brackets on the same line`() {
    doTest(
      "g%",
      """
        <img ${c}src={{imagePath}} alt={{imageDescription}}>
      """.trimIndent(),
      """
        <img ${c}src={{imagePath}} alt={{imageDescription}}>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump from opening to closing tag while ignoring comments`() {
    doTest(
      "g%",
      """
        <!-- <div> -->
        <!-- This div is completely commented out -->
        <!-- </div> -->
        
        <${c}div>
          <p>paragraph 1</p>
          <!-- </div> -->
          <p>paragraph 2</p>
        </div>
      """.trimIndent(),
      """
        <!-- <div> -->
        <!-- This div is completely commented out -->
        <!-- </div> -->
        
        <div>
          <p>paragraph 1</p>
          <!-- </div> -->
          <p>paragraph 2</p>
        <$c/div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump from closing to opening tag while ignoring comments`() {
    doTest(
      "g%",
      """
        <!-- <div> -->
        <!-- This div is completely commented out -->
        <!-- </div> -->
        
        <${c}div>
          <!-- This <div> holds paragraphs -->
          <p>paragraph 1</p>
          <p>paragraph 2</p>
        </div>
      """.trimIndent(),
      """
        <!-- <div> -->
        <!-- This div is completely commented out -->
        <!-- </div> -->
        
        <div>
          <!-- This <div> holds paragraphs -->
          <p>paragraph 1</p>
          <p>paragraph 2</p>
        <$c/div>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump from opening to closing tag inside a comment block`() {
    doTest(
      "g%",
      """
        <!-- <${c}div> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      """
        <!-- <div> -->
        <!--   This div is commented out -->
        <!-- <$c/div> -->
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump from closing to opening tag inside a comment block`() {
    doTest(
      "g%",
      """
        <!-- <div> -->
        <!--   This div is commented out -->
        <!-- <$c/div> -->
      """.trimIndent(),
      """
        <!-- <${c}div> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump from opening to closing angle bracket inside a comment block`() {
    doTest(
      "g%",
      """
        <!-- $c<div> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      """
        <!-- <div$c> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump from closing to opening angle bracket inside a comment block`() {
    doTest(
      "g%",
      """
        <!-- <div$c> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      """
        <!-- $c<div> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump from opening to closing angle bracket on a comment marker`() {
    doTest(
      "g%",
      """
        $c<!-- <div> -->
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      """
        <!-- <div> --$c>
        <!--   This div is commented out -->
        <!-- </div> -->
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump from opening to closing angle bracket ignoring bracket in string`() {
    doTest(
      "g%",
      """
        $c<p *ngIf="count > 0">Count is greater than zero</p>
      """.trimIndent(),
      """
        <p *ngIf="count > 0"$c>Count is greater than zero</p>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump from whitespace before comment angle bracket to closing bracket`() {
    doTest(
      "g%",
      "$c  <!-- A comment -->",
      "  <!-- A comment --$c>",
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump from opening tag to closing tag ignoring inner tag with similar name`() {
    doTest(
      "g%",
      """
        <${c}Box>
          <BoxHeading></BoxHeading>
        </Box>
      """.trimIndent(),
      """
        <Box>
          <BoxHeading></BoxHeading>
        <${c}/Box>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test reverse jump from closing tag to opening tag ignoring inner tag with similar name`() {
    doTest(
      "g%",
      """
        <Box>
          <BoxHeading></BoxHeading>
        <${c}/Box>
      """.trimIndent(),
      """
        <${c}Box>
          <BoxHeading></BoxHeading>
        </Box>
      """.trimIndent(),
      Mode.NORMAL(),
      HtmlFileType.INSTANCE,
    )
  }
}
