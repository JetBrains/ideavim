/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex.implementation

import org.jetbrains.plugins.ideavim.VimTestCase

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

  // todo normal required
//  fun `test 1`() {
//    configureByText("""
//      const val ${c}VERY_IMPORTANT_VALUE = 42
//    """.trimIndent())
//    injector.vimscriptExecutor.execute(script)
//    typeText(injector.parser.parseKeys("veu"))
//    assertState("""
//      const val veryImportantValue${c} = 42
//    """.trimIndent())
//  }

  // todo normal required
//  fun `test 2`() {
//    configureByText("""
//      val ${c}myCamelCaseValue = "Hi, I'm a simple value"
//    """.trimIndent())
//    injector.vimscriptExecutor.execute(script)
//    typeText(injector.parser.parseKeys("veU"))
//    assertState("""
//      val MY_CAMEL_CASE_VALUE${c} = "Hi, I'm a simple value"
//    """.trimIndent())
//  }
}
