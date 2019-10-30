/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim

import com.maddyhome.idea.vim.option.BoundStringOption
import com.maddyhome.idea.vim.option.ListOption
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.ToggleOption

/**
 * @author Alex Plate
 *
 * This test case helps you to test IdeaVim options
 *
 * While inheriting from this class you should specify (via constructor), which options you are going to test.
 *   After that each test method in this class should contains [VimOptionTestConfiguration] annotation with
 *   description of which values of option should be set before starting test.
 *
 * e.g.
 * ```
 * @VimOptionTestConfiguration(VimTestOption("keymodel", LIST, ["startsel"]), VimTestOption("selectmode", LIST, ["key"]))
 * ```
 *
 * If you want to keep default configuration, you can put [VimOptionDefaultAll] annotation
 */
abstract class VimOptionTestCase(option: String, vararg otherOptions: String) : VimTestCase() {
  private val options: Set<String> = setOf(option, *otherOptions)

  override fun runTest() {
    val testMethod = this.javaClass.getMethod(this.name)
    if (!testMethod.isAnnotationPresent(VimOptionDefaultAll::class.java)) {
      if (!testMethod.isAnnotationPresent(VimOptionTestConfiguration::class.java)) kotlin.test.fail("You should add VimOptionTestConfiguration with options for this method")

      val annotationValues = testMethod.getDeclaredAnnotation(VimOptionTestConfiguration::class.java) ?: run {
        kotlin.test.fail("You should have at least one VimOptionTestConfiguration annotation. Or you can use VimOptionDefaultAll")
      }
      val defaultOptions = testMethod.getDeclaredAnnotation(VimOptionDefault::class.java)?.values ?: emptyArray()

      val annotationsValueList = annotationValues.value.map { it.option } + defaultOptions
      val annotationsValueSet = annotationsValueList.toSet()
      if (annotationsValueSet.size < annotationsValueList.size) kotlin.test.fail("You have duplicated options")
      if (annotationsValueSet != options) kotlin.test.fail("You should present all options in annotations")

      annotationValues.value.forEach {
        val option = OptionsManager.getOption(it.option)
        when (it.type) {
          VimTestOptionType.TOGGLE -> {
            if (option !is ToggleOption) {
              kotlin.test.fail("${it.option} is not a toggle option. Change it for method `${testMethod.name}`")
            }
            if (it.values.size != 1) {
              kotlin.test.fail("You should provide only one value for Toggle option. Change it for method `${testMethod.name}`")
            }

            if (it.values.first().toBoolean()) option.set() else option.reset()
          }
          VimTestOptionType.LIST -> {
            if (option !is ListOption) kotlin.test.fail("${it.option} is not a list option. Change it for method `${testMethod.name}`")

            option.set(it.values.joinToString(","))
          }
          VimTestOptionType.VALUE -> {
            if (option !is BoundStringOption) kotlin.test.fail("${it.option} is not a value option. Change it for method `${testMethod.name}`")

            option.set(it.values.first())
          }
        }
      }
    }
    super.runTest()
  }
}

@Target(AnnotationTarget.FUNCTION)
annotation class VimOptionDefaultAll

@Target(AnnotationTarget.FUNCTION)
annotation class VimOptionDefault(vararg val values: String)

@Target(AnnotationTarget.FUNCTION)
annotation class VimOptionTestConfiguration(vararg val value: VimTestOption)

@Target(AnnotationTarget.PROPERTY)
annotation class VimTestOption(
  val option: String,
  val type: VimTestOptionType,
  val values: Array<String>
)

enum class VimTestOptionType {
  LIST,
  TOGGLE,
  VALUE
}
