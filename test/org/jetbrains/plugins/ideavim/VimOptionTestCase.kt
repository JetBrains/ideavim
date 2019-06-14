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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim

import com.maddyhome.idea.vim.option.ListOption
import com.maddyhome.idea.vim.option.Options
import com.maddyhome.idea.vim.option.ToggleOption
import junit.framework.TestCase

/**
 * @author Alex Plate
 *
 * This test case helps you to test IdeaVim options
 *
 * While inheriting from this class you should specify (via constructor), which options you are going to test.
 *   After that each test method in this class should contains [VimListOptionTestConfiguration] annotation with
 *   description of which values of option should be set before starting test.
 *
 * e.g.
 * ```
 * @VimListOptionTestConfiguration(VimListConfig("keymodel", ["startsel"]), VimListConfig("selectmode", ["key"]))
 * ```
 *
 * If you want to keep default configuration, you can put [VimListOptionDefault] annotation
 */
abstract class VimOptionTestCase(option: String, vararg otherOptions: String) : VimTestCase() {
  val options: Set<String> = setOf(option, *otherOptions)
  override fun runTest() {
    val testMethod = this.javaClass.getMethod(this.name)
    if (!testMethod.isAnnotationPresent(VimListOptionDefault::class.java)) {
      if (!testMethod.isAnnotationPresent(VimListOptionTestConfiguration::class.java) &&
        !testMethod.isAnnotationPresent(VimToggleOptionTestConfiguration::class.java)) TestCase.fail("You should add VimOptionTestAnnotation with options for this method")

      val listAnnotation: VimListOptionTestConfiguration? = testMethod.getDeclaredAnnotation(VimListOptionTestConfiguration::class.java)
      val toggle: VimToggleOptionTestConfiguration? = testMethod.getDeclaredAnnotation(VimToggleOptionTestConfiguration::class.java)

      val annotationsValuesList = (listAnnotation?.value?.map { it.option } ?: emptyList()) + (toggle?.value?.map { it.option } ?: emptyList())
      val annotationsValuesSet = annotationsValuesList.toSet()
      if (annotationsValuesSet.size < annotationsValuesList.size) TestCase.fail("You have duplicated options")
      if (annotationsValuesSet != options) TestCase.fail("You should present all options in annotations")

      listAnnotation?.value?.forEach {
        val option = Options.getInstance().getOption(it.option)
        if (option !is ListOption) {
          TestCase.fail("Only list options are supported")
          return
        }

        option.set(it.values.joinToString(","))
      }
      toggle?.value?.forEach {
        val option = Options.getInstance().getOption(it.option)
        if (option !is ToggleOption) {
          TestCase.fail("Only list options are supported")
          return
        }
        if (it.value) option.set() else option.reset()
      }
    }
    super.runTest()
  }
}

@Target(AnnotationTarget.PROPERTY)
annotation class VimListConfig(
  val option: String,
  val values: Array<String>
)

@Target(AnnotationTarget.FUNCTION)
annotation class VimListOptionTestConfiguration(vararg val value: VimListConfig)

@Target(AnnotationTarget.FUNCTION)
annotation class VimListOptionDefault

@Target(AnnotationTarget.PROPERTY)
annotation class VimToggleConfig(
  val option: String,
  val value: Boolean
)

@Target(AnnotationTarget.FUNCTION)
annotation class VimToggleOptionTestConfiguration(vararg val value: VimToggleConfig)

@Target(AnnotationTarget.FUNCTION)
annotation class VimToggleOptionDefault
