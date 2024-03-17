/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class LongerFunctionTest : VimTestCase() {
  val script = """
      function! IsUppercase(char)
        return a:char >=# 'A' && a:char <=# 'Z'
      endfunction

      function! IsCaps(word)
        return a:word ==# toupper(a:word)
      endfunction

      function! Capitalize(word)
        return toupper(a:word[0]) .. a:word[1:]
      endfunction

      function! Split(text, delimeter)
        let parts = []
        let part = ''
        for char in a:text
          if char ==? a:delimeter
            let parts += [part]
            let part = ''
          else
            let part .= char
          endif
        endfor
        let parts += [part]
        return parts
      endfunction

      function! ToCamelCase()
        let capitalize = 0
        
        normal gv"wy
        let wordUnderCaret = @w
        let parts = Split(wordUnderCaret, '_')
        let result = tolower(parts[0])
        let counter = 1
        while counter < len(parts)
          let result .= Capitalize(tolower(parts[counter])) 
          let counter += 1
        endwhile
        execute 'normal gvc' .. result 
      endfunction

      function! ToSnakeCase()
        normal gv"wy
        let wordUnderCaret = @w
        
        let parts = []
        let subword = ''
        let atWordStart = 1
        for char in wordUnderCaret
          if IsUppercase(char) && !atWordStart
            let parts += [toupper(subword)]
            let subword = char
          else
            let subword .= char
          endif

          let atWordStart = char ==? ' '
        endfor
        let parts += [toupper(subword)]
        execute 'normal gvc' .. join(parts, '_')
      endfunction
      
      vnoremap u :<C-u>call ToCamelCase()<CR>
      vnoremap U :<C-u>call ToSnakeCase()<CR>
  """.trimIndent()
  val invert = """
      " This function replaces the word under caret with it's antonym
      " e.g. true -> false
      " e.g. first -> last
      function! Invert(calledFromVisual)
        let antonyms = [
                         \'true', 'false', 'after', 'before', 'start', 'end', 'left', 'right', 'first', 'last',
                         \'up', 'down', 'min', 'max', 'minimum', 'maximum',
                         \'True', 'False', 'After', 'Before', 'Start', 'End', 'Left', 'Right', 'First', 'Last',
                         \'Up', 'Down', 'Min', 'Max', 'Minimum', 'Maximum',
                       \]

        if a:calledFromVisual
          normal gv"wy
        else
          normal "wyiw
        endif
        let wordUnderCaret = @w

        let eraseWord = a:calledFromVisual ? 'gvc' : 'ciw'
        let count = 0
        while (count < len(antonyms))
          if (antonyms[count] ==# wordUnderCaret)
            let antonym = (count % 2 ==? 0) ? antonyms[count + 1] : antonyms[count - 1]
            execute 'normal ' .. eraseWord .. antonym
            break
          endif
          let count += 1
        endwhile
      endfunction
      
      nnoremap ! :call Invert(0)<CR>
      vnoremap ! :<C-u>call Invert(1)<CR>
  """.trimIndent()

  @Test
  fun `test 1`() {
    configureByText(
      """
      const val ${c}VERY_IMPORTANT_VALUE = 42
      """.trimIndent(),
    )
    executeVimscript(script)
    typeText(injector.parser.parseKeys("veu"))
    assertState(
      """
      const val veryImportantValu${c}e = 42
      """.trimIndent(),
    )
  }

  @Test
  fun `test 2`() {
    configureByText(
      """
      val ${c}myCamelCaseValue = "Hi, I'm a simple value"
      """.trimIndent(),
    )
    executeVimscript(script)
    typeText(injector.parser.parseKeys("veU"))
    assertState(
      """
      val MY_CAMEL_CASE_VALU${c}E = "Hi, I'm a simple value"
      """.trimIndent(),
    )
  }

  @Test
  fun `test invert function in normal mode`() {
    configureByText(
      """
      val myValue = t${c}rue
      """,
    )
    executeVimscript(invert)
    typeText("!")
    assertState(
      """
      val myValue = fals${c}e
      """,
    )
  }

  @Test
  fun `test invert function in visual mode`() {
    configureByText(
      """
      val my${c}StartOffset = 10
      """,
    )
    executeVimscript(invert)
    typeText("vtO!")
    assertState(
      """
      val myEndOffset = 10
      """,
    )
  }
}
