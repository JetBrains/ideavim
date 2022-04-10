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

package org.jetbrains.plugins.ideavim.extension.matchit

import com.maddyhome.idea.vim.command.CommandState
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class MatchitRubyTest : VimTestCase() {
  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    enableExtensions("matchit")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic jump from if to end`() {
    doTest(
      "%",
      """
        ${c}if some_boolean
          puts "result is true"
        end
      """.trimIndent(),
      """
        if some_boolean
          puts "result is true"
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from whitespace before if to end`() {
    doTest(
      "%",
      """ $c  if some_boolean puts "result is true" end""",
      """   if some_boolean puts "result is true" ${c}end""",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from whitespace before class to end`() {
    doTest(
      "%",
      """ $c class Foo end""",
      """  class Foo ${c}end""",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic jump from end to if`() {
    doTest(
      "%",
      """
        if some_boolean
          puts "result is true"
        ${c}end
      """.trimIndent(),
      """
        ${c}if some_boolean
          puts "result is true"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic jump from if to else`() {
    doTest(
      "%",
      """
        ${c}if some_boolean
          puts "result is true"
        else
          puts "result is false"
        end
      """.trimIndent(),
      """
        if some_boolean
          puts "result is true"
        ${c}else
          puts "result is false"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic jump from else to end`() {
    doTest(
      "%",
      """
        if some_boolean
          puts "result is true"
        ${c}else
          puts "result is false"
        end
      """.trimIndent(),
      """
        if some_boolean
          puts "result is true"
        else
          puts "result is false"
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from end to if in if-else structure`() {
    doTest(
      "%",
      """
        if some_boolean
          puts "result is true"
        else
          puts "result is false"
        ${c}end
      """.trimIndent(),
      """
        ${c}if some_boolean
          puts "result is true"
        else
          puts "result is false"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from if to elsif`() {
    doTest(
      "%",
      """
        ${c}if first_value
          puts "first value is true"
        elsif second_value
          puts "second value is true"
        else
          puts "both values are false"
        end
      """.trimIndent(),
      """
        if first_value
          puts "first value is true"
        ${c}elsif second_value
          puts "second value is true"
        else
          puts "both values are false"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from elsif to else`() {
    doTest(
      "%",
      """
        if first_value
          puts "first value is true"
        ${c}elsif second_value
          puts "second value is true"
        else
          puts "both values are false"
        end
      """.trimIndent(),
      """
        if first_value
          puts "first value is true"
        elsif second_value
          puts "second value is true"
        ${c}else
          puts "both values are false"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from elsif to end`() {
    doTest(
      "%",
      """
        if first_value
          puts "first value is true"
        ${c}elsif second_value
          puts "second value is true"
        end
      """.trimIndent(),
      """
        if first_value
          puts "first value is true"
        elsif second_value
          puts "second value is true"
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from elsif to elsif`() {
    doTest(
      "%",
      """
        if first_value
          puts "first value is true"
        ${c}elsif second_value
          puts "second value is true"
        elsif third_value
          puts "third value is true"
        else
          puts "all values are false"
        end
      """.trimIndent(),
      """
        if first_value
          puts "first value is true"
        elsif second_value
          puts "second value is true"
        ${c}elsif third_value
          puts "third value is true"
        else
          puts "all values are false"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from for to end`() {
    doTest(
      "%",
      """
        target = "baz"
        ${c}for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            break
          end
        end
      """.trimIndent(),
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            break
          end
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from for to end and ignore inner if blocks`() {
    doTest(
      "%",
      """
        ${c}for i in 0..5
          if i > 1
            puts "Greater than 1"
          elsif i > 2
            puts "Greater than 2"
          elsif i > 3 
            puts "Greater than 3"
          elsif i > 4 
            puts "Greater than 4"
          end
        end
      """.trimIndent(),
      """
        for i in 0..5
          if i > 1
            puts "Greater than 1"
          elsif i > 2
            puts "Greater than 2"
          elsif i > 3 
            puts "Greater than 3"
          elsif i > 4 
            puts "Greater than 4"
          end
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from if to break`() {
    doTest(
      "%",
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          ${c}if s == target
            puts "Found target"
            break
          end
        end
      """.trimIndent(),
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            ${c}break
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from break to end`() {
    doTest(
      "%",
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            ${c}break
          end
        end
      """.trimIndent(),
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            break
          ${c}end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from end to if`() {
    doTest(
      "%",
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            break
          ${c}end
        end
      """.trimIndent(),
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          ${c}if s == target
            puts "Found target"
            break
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from end to for`() {
    doTest(
      "%",
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            break
          end
        ${c}end
      """.trimIndent(),
      """
        target = "baz"
        ${c}for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            break
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic jump from class to end`() {
    doTest(
      "%",
      """
        ${c}class SomeRubyClass
          def some_method
            puts "hello!"
          end
        end
      """.trimIndent(),
      """
        class SomeRubyClass
          def some_method
            puts "hello!"
          end
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic jump from end to class`() {
    doTest(
      "%",
      """
        class SomeRubyClass
          def some_method
            puts "hello!"
          end
        ${c}end
      """.trimIndent(),
      """
        ${c}class SomeRubyClass
          def some_method
            puts "hello!"
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic jump from def to end`() {
    doTest(
      "%",
      """
        class SomeRubyClass
          ${c}def some_method
            puts "hello!"
          end
        end
      """.trimIndent(),
      """
        class SomeRubyClass
          def some_method
            puts "hello!"
          ${c}end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic jump from end to def`() {
    doTest(
      "%",
      """
        class SomeRubyClass
          def some_method
            puts "hello!"
          ${c}end
        end
      """.trimIndent(),
      """
        class SomeRubyClass
          ${c}def some_method
            puts "hello!"
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from class to end ignoring nested class`() {
    doTest(
      "%",
      """
        ${c}class OuterClass

          class InnerClass
            def inner_method(arg1, arg2)
              puts "I am the inner method"
            end
          end

          def outer_method
            puts "I am the outer method"
          end

        end
      """.trimIndent(),
      """
        class OuterClass

          class InnerClass
            def inner_method(arg1, arg2)
              puts "I am the inner method"
            end
          end

          def outer_method
            puts "I am the outer method"
          end

        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from if to end over inner loop`() {
    doTest(
      "%",
      """
        ${c}if some_boolean
          for i in 0..5
            if i > 2
              puts "Greater than 2"
            elsif i > 3
              puts "Greater than 3"
            end
          end
        end
      """.trimIndent(),
      """
        if some_boolean
          for i in 0..5
            if i > 2
              puts "Greater than 2"
            elsif i > 3
              puts "Greater than 3"
            end
          end
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from end to if ignoring malformed code`() {
    doTest(
      "%",
      """
        end # Syntax error
 
        if some_boolean
          puts some_boolean
        ${c}end
      """.trimIndent(),
      """
        end # Syntax error
 
        ${c}if some_boolean
          puts some_boolean
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from if to end after a semicolon`() {
    doTest(
      "%",
      """puts "This line has two statements"; ${c}if true then puts "true" end""",
      """puts "This line has two statements"; if true then puts "true" ${c}end""",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from end to if after a semicolon`() {
    doTest(
      "%",
      """puts "This line has two statements"; if true then puts "true" ${c}end""",
      """puts "This line has two statements"; ${c}if true then puts "true" end""",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from before if and semicolon to end`() {
    doTest(
      "%",
      """puts "This line has ${c}two statements"; if true then puts "true" end""",
      """puts "This line has two statements"; if true then puts "true" ${c}end""",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from unless to end after a semicolon`() {
    doTest(
      "%",
      """puts "This line has two statements"; ${c}unless nil then puts "not nil" end""",
      """puts "This line has two statements"; unless nil then puts "not nil" ${c}end""",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from one line condition to if`() {
    doTest(
      "%",
      """if tr${c}ue puts "true" end""",
      """${c}if true puts "true" end""",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from end to unless after a semicolon`() {
    doTest(
      "%",
      """puts "This line has two statements"; unless nil then puts "not nill" ${c}end""",
      """puts "This line has two statements"; ${c}unless nil then puts "not nill" end""",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from end to do when no other opening keyword is present`() {
    doTest(
      "%",
      """
        for x in [1, 2, 3] do
          puts x
        end
       
        [1, 2, 3].each do |x| puts x ${c}end
      """.trimIndent(),
      """
        for x in [1, 2, 3] do
          puts x
        end
       
        [1, 2, 3].each ${c}do |x| puts x end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from do to end`() {
    doTest(
      "%",
      """
        for number in [1, 2, 3, 4, 5] ${c}do
          if number.even?
            puts "Even"
          else
            puts "Odd"
          end
        end
      """.trimIndent(),
      """
        for number in [1, 2, 3, 4, 5] do
          if number.even?
            puts "Even"
          else
            puts "Odd"
          end
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from module to end`() {
    doTest(
      "%",
      """
        ${c}module MyModule
          def my_function
            return true
          end
        end
      """.trimIndent(),
      """
        module MyModule
          def my_function
            return true
          end
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from end to module`() {
    doTest(
      "%",
      """
        module MyModule
          def my_function
            return true
          end
        ${c}end
      """.trimIndent(),
      """
        ${c}module MyModule
          def my_function
            return true
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from while to end`() {
    doTest(
      "%",
      """
        def count_down(x)
          ${c}while x >= 0 
            puts x
            x = x - 1
          end        
        end
      """.trimIndent(),
      """
        def count_down(x)
          while x >= 0 
            puts x
            x = x - 1
          ${c}end        
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from begin to rescue`() {
    doTest(
      "%",
      """
        def open_file(file_name)
          ${c}begin
            file = open(file_name)
          rescue
            puts "File could not be opened"
          ensure
            file.close
          end
        end
      """.trimIndent(),
      """
        def open_file(file_name)
          begin
            file = open(file_name)
          ${c}rescue
            puts "File could not be opened"
          ensure
            file.close
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from rescue to ensure`() {
    doTest(
      "%",
      """
        def open_file(file_name)
          begin
            file = open(file_name)
          ${c}rescue
            puts "File could not be opened"
          ensure
            file.close
          end
        end
      """.trimIndent(),
      """
        def open_file(file_name)
          begin
            file = open(file_name)
          rescue
            puts "File could not be opened"
          ${c}ensure
            file.close
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from ensure to end`() {
    doTest(
      "%",
      """
        def open_file(file_name)
          begin
            file = open(file_name)
          rescue
            puts "File could not be opened"
          ${c}ensure
            file.close
          end
        end
      """.trimIndent(),
      """
        def open_file(file_name)
          begin
            file = open(file_name)
          rescue
            puts "File could not be opened"
          ensure
            file.close
          ${c}end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from end to begin`() {
    doTest(
      "%",
      """
        def open_file(file_name)
          begin
            file = open(file_name)
          rescue
            puts "File could not be opened"
          ensure
            file.close
          ${c}end
        end
      """.trimIndent(),
      """
        def open_file(file_name)
          ${c}begin
            file = open(file_name)
          rescue
            puts "File could not be opened"
          ensure
            file.close
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from rescue to retry`() {
    doTest(
      "%",
      """
        begin
          puts "Loop forever"
          raise
        ${c}rescue
          retry
        end
      """.trimIndent(),
      """
        begin
          puts "Loop forever"
          raise
        rescue
          ${c}retry
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from retry to end`() {
    doTest(
      "%",
      """
        begin
          puts "Loop forever"
          raise
        rescue
          ${c}retry
        end
      """.trimIndent(),
      """
        begin
          puts "Loop forever"
          raise
        rescue
          retry
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from end to while`() {
    doTest(
      "%",
      """
        def count_down(x)
          while x >= 0 
            puts x
            x = x - 1
          ${c}end        
        end
      """.trimIndent(),
      """
        def count_down(x)
          ${c}while x >= 0 
            puts x
            x = x - 1
          end        
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from case to when`() {
    doTest(
      "%",
      """
        ${c}case age
          when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        end
      """.trimIndent(),
      """
        case age
          ${c}when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from when to when`() {
    doTest(
      "%",
      """
        case age
          ${c}when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        end
      """.trimIndent(),
      """
        case age
          when 0 .. 12
            puts "Child"
          ${c}when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from else to end in case block`() {
    doTest(
      "%",
      """
        case age
          when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          ${c}else
            puts "Adult"
        end
      """.trimIndent(),
      """
        case age
          when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from end to case`() {
    doTest(
      "%",
      """
        case age
          when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        ${c}end
      """.trimIndent(),
      """
        ${c}case age
          when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from until to end`() {
    doTest(
      "%",
      """
        ${c}until var == 11
          var = var + 1
        end
      """.trimIndent(),
      """
        until var == 11
          var = var + 1
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from if to redo`() {
    doTest(
      "%",
      """
        10.times do |i|
          puts "Iteration #{i}"
          ${c}if i > 2
            redo
          elsif i == 3
            next
          end
        end
      """.trimIndent(),
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            ${c}redo
          elsif i == 3
            next
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from redo to elsif`() {
    doTest(
      "%",
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            ${c}redo
          elsif i == 3
            next
          end
        end
      """.trimIndent(),
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            redo
          ${c}elsif i == 3
            next
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from elsif to next`() {
    doTest(
      "%",
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            redo
          ${c}elsif i == 3
            next
          end
        end
      """.trimIndent(),
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            redo
          elsif i == 3
            ${c}next
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from next to end`() {
    doTest(
      "%",
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            redo
          elsif i == 3
            ${c}next
          end
        end
      """.trimIndent(),
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            redo
          elsif i == 3
            next
          ${c}end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from =begin to =end`() {
    doTest(
      "%",
      """
        $c=begin
          end comment
        =end
      """.trimIndent(),
      """
        =begin
          end comment
        =${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from =end to =begin`() {
    doTest(
      "%",
      """
        =begin
          begin comment
        $c=end
      """.trimIndent(),
      """
        =${c}begin
          begin comment
        =end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from middle of cucumber test to end`() {
    doTest(
      "%",
      """
        Given /^ Some ${c}Cucumber test $/ do
          users.each do |group|
            begin
              test_fun()
            rescue
              clean_up()
            end
          end
        end
      """.trimIndent(),
      """
        Given /^ Some Cucumber test $/ do
          users.each do |group|
            begin
              test_fun()
            rescue
              clean_up()
            end
          end
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from end of cucumber test to do`() {
    doTest(
      "%",
      """
        Given /^ Some Cucumber test $/ do
          users.each do |group|
            begin
              test_fun()
            rescue
              clean_up()
            end
          end
        ${c}end
      """.trimIndent(),
      """
        Given /^ Some Cucumber test ${'$'}/ ${c}do
          users.each do |group|
            begin
              test_fun()
            rescue
              clean_up()
            end
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  /*
   * Tests for reverse g% motion
   */

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic reverse jump from end to if `() {
    doTest(
      "g%",
      """
        if some_boolean
          puts "result is true"
        ${c}end
      """.trimIndent(),
      """
        ${c}if some_boolean
          puts "result is true"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic reverse jump from end to if`() {
    doTest(
      "g%",
      """
        if some_boolean
          puts "result is true"
        ${c}end
      """.trimIndent(),
      """
        ${c}if some_boolean
          puts "result is true"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic reverse jump from else to if`() {
    doTest(
      "g%",
      """
        if some_boolean
          puts "result is true"
        ${c}else
          puts "result is false"
        end
      """.trimIndent(),
      """
        ${c}if some_boolean
          puts "result is true"
        else
          puts "result is false"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic reverse jump from end to else`() {
    doTest(
      "g%",
      """
        if some_boolean
          puts "result is true"
        else
          puts "result is false"
        ${c}end
      """.trimIndent(),
      """
        if some_boolean
          puts "result is true"
        ${c}else
          puts "result is false"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from if to end in if-else structure`() {
    doTest(
      "g%",
      """
        ${c}if some_boolean
          puts "result is true"
        else
          puts "result is false"
        end
      """.trimIndent(),
      """
        if some_boolean
          puts "result is true"
        else
          puts "result is false"
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from elsif to if`() {
    doTest(
      "g%",
      """
        if first_value
          puts "first value is true"
        ${c}elsif second_value
          puts "second value is true"
        else
          puts "both values are false"
        end
      """.trimIndent(),
      """
        ${c}if first_value
          puts "first value is true"
        elsif second_value
          puts "second value is true"
        else
          puts "both values are false"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from else to elsif`() {
    doTest(
      "g%",
      """
        if first_value
          puts "first value is true"
        elsif second_value
          puts "second value is true"
        ${c}else
          puts "both values are false"
        end
      """.trimIndent(),
      """
        if first_value
          puts "first value is true"
        ${c}elsif second_value
          puts "second value is true"
        else
          puts "both values are false"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to elsif`() {
    doTest(
      "g%",
      """
        if first_value
          puts "first value is true"
        elsif second_value
          puts "second value is true"
        ${c}end
      """.trimIndent(),
      """
        if first_value
          puts "first value is true"
        ${c}elsif second_value
          puts "second value is true"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from elsif to elsif`() {
    doTest(
      "g%",
      """
        if first_value
          puts "first value is true"
        elsif second_value
          puts "second value is true"
        ${c}elsif third_value
          puts "third value is true"
        else
          puts "all values are false"
        end
      """.trimIndent(),
      """
        if first_value
          puts "first value is true"
        ${c}elsif second_value
          puts "second value is true"
        elsif third_value
          puts "third value is true"
        else
          puts "all values are false"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to for and ignore inner if blocks`() {
    doTest(
      "g%",
      """
        for i in 0..5
          if i > 1
            puts "Greater than 1"
          elsif i > 2
            puts "Greater than 2"
          elsif i > 3 
            puts "Greater than 3"
          elsif i > 4 
            puts "Greater than 4"
          end
        ${c}end
      """.trimIndent(),
      """
        ${c}for i in 0..5
          if i > 1
            puts "Greater than 1"
          elsif i > 2
            puts "Greater than 2"
          elsif i > 3 
            puts "Greater than 3"
          elsif i > 4 
            puts "Greater than 4"
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from break to if`() {
    doTest(
      "g%",
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            ${c}break
          end
        end
      """.trimIndent(),
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          ${c}if s == target
            puts "Found target"
            break
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to break`() {
    doTest(
      "g%",
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            break
          ${c}end
        end
      """.trimIndent(),
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            ${c}break
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from if to end ignoring break`() {
    doTest(
      "g%",
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          ${c}if s == target
            puts "Found target"
            break
          end
        end
      """.trimIndent(),
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            break
          ${c}end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from for to end ignoring nested if`() {
    doTest(
      "g%",
      """
        target = "baz"
        ${c}for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            break
          end
        end
      """.trimIndent(),
      """
        target = "baz"
        for s in ["foo", "bar", "baz"]
          if s == target
            puts "Found target"
            break
          end
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic reverse jump from end to class`() {
    doTest(
      "g%",
      """
        class SomeRubyClass
          def some_method
            puts "hello!"
          end
        ${c}end
      """.trimIndent(),
      """
        ${c}class SomeRubyClass
          def some_method
            puts "hello!"
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test basic reverse jump from class to end`() {
    doTest(
      "g%",
      """
        ${c}class SomeRubyClass
          def some_method
            puts "hello!"
          end
        end
      """.trimIndent(),
      """
        class SomeRubyClass
          def some_method
            puts "hello!"
          end
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from def to end`() {
    doTest(
      "g%",
      """
        class SomeRubyClass
          ${c}def some_method
            puts "hello!"
          end
        end
      """.trimIndent(),
      """
        class SomeRubyClass
          def some_method
            puts "hello!"
          ${c}end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to def across multiple functions`() {
    doTest(
      "g%",
      """
        def func_one
          puts "I am func_one"
        end
       
        def func_two
          puts "I am func_two"
        end
        
        def func_three
          puts "I am func_three"
        ${c}end
      """.trimIndent(),
      """
        def func_one
          puts "I am func_one"
        end
       
        def func_two
          puts "I am func_two"
        end
        
        ${c}def func_three
          puts "I am func_three"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to def`() {
    doTest(
      "g%",
      """
        class SomeRubyClass
          def some_method
            puts "hello!"
          ${c}end
        end
      """.trimIndent(),
      """
        class SomeRubyClass
          ${c}def some_method
            puts "hello!"
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to class ignoring nested class`() {
    doTest(
      "g%",
      """
        class OuterClass

          class InnerClass
            def inner_method(arg1, arg2)
              puts "I am the inner method"
            end
          end

          def outer_method
            puts "I am the outer method"
          end

        ${c}end
      """.trimIndent(),
      """
        ${c}class OuterClass

          class InnerClass
            def inner_method(arg1, arg2)
              puts "I am the inner method"
            end
          end

          def outer_method
            puts "I am the outer method"
          end

        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to if over inner loop`() {
    doTest(
      "g%",
      """
        if some_boolean
          for i in 0..5
            if i > 2
              puts "Greater than 2"
            elsif i > 3
              puts "Greater than 3"
            end
          end
        ${c}end
      """.trimIndent(),
      """
        ${c}if some_boolean
          for i in 0..5
            if i > 2
              puts "Greater than 2"
            elsif i > 3
              puts "Greater than 3"
            end
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to if ignoring malformed code`() {
    doTest(
      "g%",
      """
        end # Syntax error
 
        if some_boolean
          puts some_boolean
        ${c}end
      """.trimIndent(),
      """
        end # Syntax error
 
        ${c}if some_boolean
          puts some_boolean
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from one line condition to if`() {
    doTest(
      "g%",
      """if tr${c}ue puts "true" end""",
      """${c}if true puts "true" end""",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from do to end`() {
    doTest(
      "g%",
      """
        for number in [1, 2, 3, 4, 5] ${c}do
          if number.even?
            puts "Even"
          else
            puts "Odd"
          end
        end
      """.trimIndent(),
      """
        for number in [1, 2, 3, 4, 5] do
          if number.even?
            puts "Even"
          else
            puts "Odd"
          end
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from module to end`() {
    doTest(
      "g%",
      """
        ${c}module MyModule
          def my_function
            return true
          end
        end
      """.trimIndent(),
      """
        module MyModule
          def my_function
            return true
          end
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to module`() {
    doTest(
      "g%",
      """
        module MyModule
          def my_function
            return true
          end
        ${c}end
      """.trimIndent(),
      """
        ${c}module MyModule
          def my_function
            return true
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to ensure`() {
    doTest(
      "g%",
      """
        def open_file(file_name)
          begin
            file = open(file_name)
          rescue
            puts "File could not be opened"
          ensure
            file.close
          ${c}end
        end
      """.trimIndent(),
      """
        def open_file(file_name)
          begin
            file = open(file_name)
          rescue
            puts "File could not be opened"
          ${c}ensure
            file.close
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from ensure to rescue`() {
    doTest(
      "g%",
      """
        def open_file(file_name)
          begin
            file = open(file_name)
          rescue
            puts "File could not be opened"
          ${c}ensure
            file.close
          end
        end
      """.trimIndent(),
      """
        def open_file(file_name)
          begin
            file = open(file_name)
          ${c}rescue
            puts "File could not be opened"
          ensure
            file.close
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from rescue to begin`() {
    doTest(
      "g%",
      """
        def open_file(file_name)
          begin
            file = open(file_name)
          ${c}rescue
            puts "File could not be opened"
          ensure
            file.close
          end
        end
      """.trimIndent(),
      """
        def open_file(file_name)
          ${c}begin
            file = open(file_name)
          rescue
            puts "File could not be opened"
          ensure
            file.close
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from begin to end`() {
    doTest(
      "g%",
      """
        def open_file(file_name)
          ${c}begin
            file = open(file_name)
          rescue
            puts "File could not be opened"
          ensure
            file.close
          end
        end
      """.trimIndent(),
      """
        def open_file(file_name)
          begin
            file = open(file_name)
          rescue
            puts "File could not be opened"
          ensure
            file.close
          ${c}end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to retry`() {
    doTest(
      "g%",
      """
        begin
          puts "Loop forever"
          raise
        rescue
          retry
        ${c}end
      """.trimIndent(),
      """
        begin
          puts "Loop forever"
          raise
        rescue
          ${c}retry
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from retry to rescue`() {
    doTest(
      "g%",
      """
        begin
          puts "Loop forever"
          raise
        rescue
          ${c}retry
        end
      """.trimIndent(),
      """
        begin
          puts "Loop forever"
          raise
        ${c}rescue
          retry
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from while to end`() {
    doTest(
      "g%",
      """
        def count_down(x)
          ${c}while x >= 0 
            puts x
            x = x - 1
          end        
        end
      """.trimIndent(),
      """
        def count_down(x)
          while x >= 0 
            puts x
            x = x - 1
          ${c}end        
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to while`() {
    doTest(
      "g%",
      """
        def count_down(x)
          while x >= 0 
            puts x
            x = x - 1
          ${c}end        
        end
      """.trimIndent(),
      """
        def count_down(x)
          ${c}while x >= 0 
            puts x
            x = x - 1
          end        
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from case to end`() {
    doTest(
      "g%",
      """
        ${c}case age
          when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        end
      """.trimIndent(),
      """
        case age
          when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to else in case block`() {
    doTest(
      "g%",
      """
        case age
          when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        ${c}end
      """.trimIndent(),
      """
        case age
          when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          ${c}else
            puts "Adult"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from else to when`() {
    doTest(
      "g%",
      """
        case age
          when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          ${c}else
            puts "Adult"
        end
      """.trimIndent(),
      """
        case age
          when 0 .. 12
            puts "Child"
          ${c}when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from when to when`() {
    doTest(
      "g%",
      """
        case age
          when 0 .. 12
            puts "Child"
          ${c}when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        end
      """.trimIndent(),
      """
        case age
          ${c}when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from when to case`() {
    doTest(
      "g%",
      """
        case age
          ${c}when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        end
      """.trimIndent(),
      """
        ${c}case age
          when 0 .. 12
            puts "Child"
          when 13 .. 18
            puts "Youth"
          else
            puts "Adult"
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to until`() {
    doTest(
      "g%",
      """
        until var == 11
          var = var + 1
        ${c}end
      """.trimIndent(),
      """
        ${c}until var == 11
          var = var + 1
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from until to end`() {
    doTest(
      "g%",
      """
        ${c}until var == 11
          var = var + 1
        end
      """.trimIndent(),
      """
        until var == 11
          var = var + 1
        ${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to next`() {
    doTest(
      "g%",
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            redo
          elsif i == 3
            next
          ${c}end
        end
      """.trimIndent(),
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            redo
          elsif i == 3
            ${c}next
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from next to elsif`() {
    doTest(
      "g%",
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            redo
          elsif i == 3
            ${c}next
          end
        end
      """.trimIndent(),
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            redo
          ${c}elsif i == 3
            next
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from elsif to redo`() {
    doTest(
      "g%",
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            redo
          ${c}elsif i == 3
            next
          end
        end
      """.trimIndent(),
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            ${c}redo
          elsif i == 3
            next
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from redo to if`() {
    doTest(
      "g%",
      """
        10.times do |i|
          puts "Iteration #{i}"
          if i > 2
            ${c}redo
          elsif i == 3
            next
          end
        end
      """.trimIndent(),
      """
        10.times do |i|
          puts "Iteration #{i}"
          ${c}if i > 2
            redo
          elsif i == 3
            next
          end
        end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from =begin to =end`() {
    doTest(
      "g%",
      """
        $c=begin
          end comment
        =end
      """.trimIndent(),
      """
        =begin
          end comment
        =${c}end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from =end to =begin`() {
    doTest(
      "g%",
      """
        =begin
          begin comment
        $c=end
      """.trimIndent(),
      """
        =${c}begin
          begin comment
        =end
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from before if and semicolon to end`() {
    doTest(
      "g%",
      """puts "This line has ${c}two statements"; if true then puts "true" end""",
      """puts "This line has two statements"; if true then puts "true" ${c}end""",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from whitespace before class to end`() {
    doTest(
      "g%",
      """ $c  class Foo end""",
      """   class Foo ${c}end""",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby.rb"
    )
  }

  /*
   * Tests for embedded Ruby
   */

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from opening to closing HTML tag`() {
    // This is just to test that HTML jumps are working in Ruby files.
    // MatchitHtmlTest is what tests the correctness of the HTML jumps.
    doTest(
      "%",
      """
        <div><${c}div>contents</div></div>
      """.trimIndent(),
      """
        <div><div>contents<$c/div></div>
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby-template.html.erb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from opening to closing div while on class attribute`() {
    doTest(
      "%",
      """
        <div ${c}class="container">
          <% flash.each do |message_type, message| %>
            <div class="alert alert-<%= message_type %>"><%= message %></div>
          <% end %>
          <%= yield %>
          <%= render 'layouts/footer' %>
        </div>
      """.trimIndent(),
      """
        <div class="container">
          <% flash.each do |message_type, message| %>
            <div class="alert alert-<%= message_type %>"><%= message %></div>
          <% end %>
          <%= yield %>
          <%= render 'layouts/footer' %>
        <$c/div>
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby-template.html.erb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from do to end in template block`() {
    doTest(
      "%",
      """
        <div class="container">
          <% flash.each ${c}do |message_type, message| %>
            <div class="alert alert-<%= message_type %>"><%= message %></div>
          <% end %>
          <%= yield %>
          <%= render 'layouts/footer' %>
        </div>
      """.trimIndent(),
      """
        <div class="container">
          <% flash.each do |message_type, message| %>
            <div class="alert alert-<%= message_type %>"><%= message %></div>
          <% ${c}end %>
          <%= yield %>
          <%= render 'layouts/footer' %>
        </div>
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby-template.html.erb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from end to do in template block`() {
    doTest(
      "%",
      """
        <div class="container">
          <% flash.each do |message_type, message| %>
            <div class="alert alert-<%= message_type %>"><%= message %></div>
          <% ${c}end %>
          <%= yield %>
          <%= render 'layouts/footer' %>
        </div>
      """.trimIndent(),
      """
        <div class="container">
          <% flash.each ${c}do |message_type, message| %>
            <div class="alert alert-<%= message_type %>"><%= message %></div>
          <% end %>
          <%= yield %>
          <%= render 'layouts/footer' %>
        </div>
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby-template.html.erb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from closing to opening div`() {
    doTest(
      "%",
      """
        <div class="container">
          <% flash.each do |message_type, message| %>
            <div class="alert alert-<%= message_type %>"><%= message %></div>
          <% end %>
          <%= yield %>
          <%= render 'layouts/footer' %>
        </d${c}iv>
      """.trimIndent(),
      """
        <${c}div class="container">
          <% flash.each do |message_type, message| %>
            <div class="alert alert-<%= message_type %>"><%= message %></div>
          <% end %>
          <%= yield %>
          <%= render 'layouts/footer' %>
        </div>
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby-template.html.erb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from opening to closing angle bracket in template`() {
    doTest(
      "%",
      """<div class="alert alert-$c<%= message_type %>"><%= message %></div>""",
      """<div class="alert alert-<%= message_type %$c>"><%= message %></div>""",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby-template.html.erb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from opening to closing angle bracket in template`() {
    doTest(
      "g%",
      """<div class="alert alert-$c<%= message_type %>"><%= message %></div>""",
      """<div class="alert alert-<%= message_type %$c>"><%= message %></div>""",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby-template.html.erb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from do to end in table template`() {
    doTest(
      "%",
      """
        <tbody>
          <% @books.each ${c}do |book| %>
            <tr>
              <td><%= book.title %></td>
              <% if book.content %>
                <td><%= book.content %></td>
              <% else %>
                <td> N/A </td>
              <% end %>
              <td><%= link_to "Show", book %></td>
            </tr>
          <% end %>
        </tbody>
      """.trimIndent(),
      """
        <tbody>
          <% @books.each do |book| %>
            <tr>
              <td><%= book.title %></td>
              <% if book.content %>
                <td><%= book.content %></td>
              <% else %>
                <td> N/A </td>
              <% end %>
              <td><%= link_to "Show", book %></td>
            </tr>
          <% ${c}end %>
        </tbody>
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby-template.html.erb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from if to else in table template`() {
    doTest(
      "%",
      """
        <tbody>
          <% @books.each do |book| %>
            <tr>
              <td><%= book.title %></td>
              <% ${c}if book.content %>
                <td><%= book.content %></td>
              <% else %>
                <td> N/A </td>
              <% end %>
              <td><%= link_to "Show", book %></td>
            </tr>
          <% end %>
        </tbody>
      """.trimIndent(),
      """
        <tbody>
          <% @books.each do |book| %>
            <tr>
              <td><%= book.title %></td>
              <% if book.content %>
                <td><%= book.content %></td>
              <% ${c}else %>
                <td> N/A </td>
              <% end %>
              <td><%= link_to "Show", book %></td>
            </tr>
          <% end %>
        </tbody>
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby-template.html.erb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from else to end in table template`() {
    doTest(
      "%",
      """
        <tbody>
          <% @books.each do |book| %>
            <tr>
              <td><%= book.title %></td>
              <% if book.content %>
                <td><%= book.content %></td>
              <% ${c}else %>
                <td> N/A </td>
              <% end %>
              <td><%= link_to "Show", book %></td>
            </tr>
          <% end %>
        </tbody>
      """.trimIndent(),
      """
        <tbody>
          <% @books.each do |book| %>
            <tr>
              <td><%= book.title %></td>
              <% if book.content %>
                <td><%= book.content %></td>
              <% else %>
                <td> N/A </td>
              <% ${c}end %>
              <td><%= link_to "Show", book %></td>
            </tr>
          <% end %>
        </tbody>
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby-template.html.erb"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from end to else in table template`() {
    doTest(
      "g%",
      """
        <tbody>
          <% @books.each do |book| %>
            <tr>
              <td><%= book.title %></td>
              <% if book.content %>
                <td><%= book.content %></td>
              <% else %>
                <td> N/A </td>
              <% ${c}end %>
              <td><%= link_to "Show", book %></td>
            </tr>
          <% end %>
        </tbody>
      """.trimIndent(),
      """
        <tbody>
          <% @books.each do |book| %>
            <tr>
              <td><%= book.title %></td>
              <% if book.content %>
                <td><%= book.content %></td>
              <% ${c}else %>
                <td> N/A </td>
              <% end %>
              <td><%= link_to "Show", book %></td>
            </tr>
          <% end %>
        </tbody>
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "ruby-template.html.erb"
    )
  }
}
