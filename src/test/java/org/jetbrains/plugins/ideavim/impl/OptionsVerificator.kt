/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.impl

import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimOptionGroup
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.group.IjOptionConstants
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionChangeListener
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.OptionValueAccessor
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.options.UnsignedNumberOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.product
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream
import kotlin.test.fail

/**
 * Enables option tracing
 *
 * If this annotation is presented, tests will trace if any of [optionNames] option was accessed.
 * If any of these options were accessed, they should be presented in [OptionTest] annotation that defines
 *   options behaviour for this test.
 * This annotation is not necessary is usable for self-check
 *
 * Possible usage: At some moment, the usage of this annotation can be extended, and we can verify what options
 *   were accessed during test run. Such tests can be started multiple times for different values of accessed option.
 * At the moment, this functionality is not implemented and the amount of tests grows exponentially.
 */
@ExtendWith(OptionsVerificator::class)
@Target(AnnotationTarget.CLASS)
internal annotation class TraceOptions(vararg val optionNames: String)

/**
 * Defines values of options for tests. Test will be started multiple times with all possible combinations
 *   of the defined options.
 * Read the doc for [VimOption] to understand how the option can be defined
 */
@ExtendWith(VimOptionsInvocator::class)
@TestTemplate
@Target(AnnotationTarget.FUNCTION)
internal annotation class OptionTest(vararg val value: VimOption)

/**
 * Defines the values that will be used for the option.
 * Test will be started multiple times for every possible value. If there are multiple [VimOption] presented
 *   in [OptionTest] container, test will be started for all possible combinations for the option.
 *
 * If only option name is specified, the values for the option will be automatically generated (if possible).
 *   See next sections to understand what values are automatically generated.
 *
 * [limitedValues] if this field is presented, options values will be taken from this field.
 * - Use just a string for string option
 * - Use "true" or "false" for boolean option
 * - Use number in quotes "1" for number options
 *
 * [doesntAffectTest] field means that this option doesn't affect test, event the option is read.
 * For classic test configuration, this means that the default value will be used.
 *
 * However, in OptionVerificationTest (WIP) configuration, we'll run this test against all possible option values.
 * This behaviour is not used by default as it dramatically increases the amount of tests and execution time.
 *
 * # Option values generation
 *
 * If only the option name is defined, the following values will be automatically generated:
 * - For boolean option: true, false
 * - For number option:
 *   - Signed: -1000, -10, -1, 0, 1, 10, 1000
 *   - Unsigned: 0, 1, 10, 1000
 * - For string option:
 *   - With bounded list:
 *     - Single value option: all values from the list
 *     - Multiple values option: all possible combinations of values from the list
 *   - Without bounded list:
 *     - Not supported, please specify possible values using [limitedValues]
 */
internal annotation class VimOption(
  val name: String,
  val limitedValues: Array<String> = [],
  val doesntAffectTest: Boolean = false,
)

//  ----------- Implementation

private class OptionsVerificator : BeforeTestExecutionCallback, AfterTestExecutionCallback {
  override fun beforeTestExecution(context: ExtensionContext) {
    val testInjector = TestInjector(injector)
    val traceCollector = OptionsTraceCollector()
    val ignore = AtomicBoolean(false)
    getStore(context).put("TraceCollector", traceCollector)
    testInjector.setTracer(OptionsTracer, traceCollector)
    testInjector.setTracer("OptionTracerIgnore", ignore)
    injector = testInjector
  }

  override fun afterTestExecution(context: ExtensionContext) {
    val collector = getStore(context).get("TraceCollector", OptionsTraceCollector::class.java)
    val usedOptions = collector.requestedKeys
      .mapNotNull { injector.optionGroup.getOption(it)?.name }
      .toSet()
    val traceOptions = context.testClass.get().getAnnotation(TraceOptions::class.java)
      ?: error("This extension should be used via @TraceOption annotation")
    val usedTracedOptions = usedOptions.filter { it in traceOptions.optionNames }
    val usedOptionsWithoutIgnored = usedTracedOptions.filterNot { it in ignored }
    val optionAnnotation: OptionTest? = context.testMethod.get().getAnnotation(OptionTest::class.java)
    if (optionAnnotation == null && usedOptionsWithoutIgnored.isEmpty()) return
    if (optionAnnotation == null) {
      fail("Specify @OptionTest annotation for the following options: ${usedOptionsWithoutIgnored.joinToString()}")
    }
    val specifiedNames = optionAnnotation.value.map { it.name }.toSet()
    val usedButNotSpecified = usedOptionsWithoutIgnored.filterNot { it in specifiedNames }
    if (usedButNotSpecified.isNotEmpty()) {
      fail("Options accessed: ${usedButNotSpecified.joinToString()}. Please specify the explicitly using @OptionTest annotation")
    }
    val specifiedButNotUsed = specifiedNames.filter { it !in usedOptions }
    if (specifiedButNotUsed.isNotEmpty()) {
      LOG.warn("Options $specifiedButNotUsed are specified in annotation, but not actually used")
    }
  }

  private fun getStore(context: ExtensionContext): ExtensionContext.Store {
    return context.getStore(ExtensionContext.Namespace.create(javaClass, context.requiredTestMethod))
  }

  companion object {
    val LOG by lazy { vimLogger<OptionsVerificator>() }
    private val ignored = setOf(
      OptionConstants.guicursor,
      OptionConstants.ideaglobalmode,
      OptionConstants.ideatracetime,
      OptionConstants.number,
      OptionConstants.timeoutlen,
      OptionConstants.relativenumber,
      OptionConstants.maxmapdepth,
      IjOptionConstants.octopushandler,
      IjOptionConstants.ideavimsupport,
      "unifyjumps",
      OptionConstants.sidescrolloff,
      OptionConstants.sidescroll,
      OptionConstants.scrolloff,
      OptionConstants.scrolljump,
      IjOptionConstants.trackactionids,
      OptionConstants.showmode,
      OptionConstants.virtualedit,
      OptionConstants.whichwrap,
    )
  }
}

internal class OptionsTraceCollector {
  val requestedKeys = HashSet<String>()
}

internal class OptionsTracer(
  private val vimOptionGroup: VimOptionGroup,
  private val trace: OptionsTraceCollector,
  private val ignoreFlag: AtomicBoolean,
) : VimOptionGroup by vimOptionGroup {
  override fun getOption(key: String): Option<out VimDataType>? {
    if (!ignoreFlag.get()) {
      trace.requestedKeys += key
    }
    return vimOptionGroup.getOption(key)
  }

  override fun getAllOptions(): Set<Option<out VimDataType>> {
    val allOptions = vimOptionGroup.getAllOptions()
    if (!ignoreFlag.get()) {
      allOptions.forEach { trace.requestedKeys += it.name }
    }
    return allOptions
  }

  override fun getOptionValue(option: Option<out VimDataType>, scope: OptionScope): VimDataType {
    if (!ignoreFlag.get()) {
      trace.requestedKeys += option.name
    }
    return vimOptionGroup.getOptionValue(option, scope)
  }

  override fun addListener(optionName: String, listener: OptionChangeListener<VimDataType>, executeOnAdd: Boolean) {
    ignoreFlag.set(true)
    try {
      vimOptionGroup.addListener(optionName, listener, executeOnAdd)
    } finally {
      ignoreFlag.set(false)
    }
  }

  override fun removeListener(optionName: String, listener: OptionChangeListener<VimDataType>) {
    ignoreFlag.set(true)
    try {
      vimOptionGroup.removeListener(optionName, listener)
    } finally {
      ignoreFlag.set(false)
    }
  }

  override fun getValueAccessor(editor: VimEditor?): OptionValueAccessor {
    // I don't like this solution. Would love to see something better without rewrapping.
    // The point is that OptionValueAccesor should use our group to be property traced
    val accessor = vimOptionGroup.getValueAccessor(editor)
    return OptionValueAccessor(this, accessor.scope)
  }

  companion object
}

private class VimOptionsInvocator : TestTemplateInvocationContextProvider {
  override fun supportsTestTemplate(context: ExtensionContext?): Boolean {
    return true
  }

  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    val fixture = fixtureSetup()
    try {
      return generateContextes(context)
    } finally {
      fixture.tearDown()
    }
  }

  // Sometimes we need an injector before @BeforeEach function is executed. We set up and tear down the project for this case
  private fun fixtureSetup(): CodeInsightTestFixture {
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
    val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor, "IdeaVim")
    val fixture = fixtureBuilder.fixture
    val myfixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
      fixture,
      LightTempDirTestFixtureImpl(true),
    )
    myfixture.setUp()
    return myfixture
  }

  private fun generateContextes(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    val annotation = context.testMethod.get().getAnnotation(OptionTest::class.java)
    val options: List<List<Pair<Option<out VimDataType>, VimDataType?>>> = annotation.value.map {
      val optionName = it.name
      val option = injector.optionGroup.getOption(optionName)!!
      if (!it.doesntAffectTest) {
        if (it.limitedValues.isEmpty()) {
          defaultOptionCombinations(option)
        } else {
          when (option) {
            is ToggleOption -> it.limitedValues.map { option to if (it == "true") VimInt.ONE else VimInt.ZERO }
            is NumberOption -> it.limitedValues.map { option to VimInt(it) }
            is StringOption -> {
              it.limitedValues.map { limitedValue -> option to VimString(limitedValue) }
            }

            else -> error("Unexpected option type: $option")
          }
        }
      } else {
        listOf(option to null)
      }
    }
    return product(*options.toTypedArray())
      .map {
        // Some wierd kotlin bug. Code doesn't compile without this cast
        @Suppress("USELESS_CAST")
        VimOptionsInvocationContext(it) as TestTemplateInvocationContext
      }
      .stream()
  }

  private fun defaultOptionCombinations(option: Option<out VimDataType>): List<Pair<Option<out VimDataType>, VimDataType>> {
    return when (option) {
      is ToggleOption -> listOf(option to VimInt.ONE, option to VimInt.ZERO)
      is NumberOption -> {
        val vals = if (option is UnsignedNumberOption) {
          listOf(VimInt.ZERO, VimInt.ONE, VimInt(10), VimInt(1000))
        } else {
          listOf(VimInt(-1000), VimInt(-10), VimInt(-1), VimInt.ZERO, VimInt.ONE, VimInt(10), VimInt(1000))
        }
        vals.map { option to it }
      }

      is StringOption -> {
        if (option.isList) {
          val boundedValues = option.boundedValues
          if (boundedValues != null) {
            val valuesCombinations = boundedValues.indices.map {
              kCombinations(boundedValues.toList(), it + 1)
                .map { VimString(it.joinToString(",")) }
            }.flatten()
            valuesCombinations.map { option to it }
          } else {
            fail("Cannot generate values automatically. Please specify option values explicitelly using 'limitedValues' field")
          }
        } else {
          val boundedValues = option.boundedValues
          if (boundedValues != null) {
            boundedValues.map { option to VimString(it) }
          } else {
            fail("Cannot generate values automatically. Please specify option values explicitelly using 'limitedValues' field")
          }
        }
      }

      else -> error("Unexpected option type: $option")
    }
  }
}

// Yeah, MATHEMATICS!!!
private fun <T> kCombinations(input: List<T>, sequenceLength: Int): List<List<T>> {
  val subsets: MutableList<List<T>> = ArrayList()

  val s = IntArray(sequenceLength) // here we'll keep indices

  if (sequenceLength <= input.size) {
    var i = 0
    while (i.also { s[i] = it } < sequenceLength - 1) {
      i++
    }
    subsets.add(getSubset(input, s))
    while (true) {
      var i: Int = sequenceLength - 1
      while (i >= 0 && s[i] == input.size - sequenceLength + i) {
        i--
      }
      if (i < 0) {
        break
      }
      s[i]++ // increment this item
      ++i
      while (i < sequenceLength) {
        // fill up remaining items
        s[i] = s[i - 1] + 1
        i++
      }
      subsets.add(getSubset(input, s))
    }
  }
  return subsets
}

// generate actual subset by index sequence
private fun <T> getSubset(input: List<T>, subset: IntArray): List<T> {
  val result = ArrayList<T>()
  for (i in subset.indices) {
    result += input[subset[i]]
  }
  return result
}

class VimOptionsInvocationContext(private val options: List<Pair<Option<out VimDataType>, VimDataType?>>) :
  TestTemplateInvocationContext {
  override fun getDisplayName(invocationIndex: Int): String {
    return options.joinToString(separator = "; ") { "${it.first.name}=${it.second ?: "[default]"}" }
  }

  override fun getAdditionalExtensions(): List<Extension> {
    return listOf(OptionsSetup(options))
  }
}

class OptionsSetup(private val options: List<Pair<Option<*>, VimDataType?>>) : BeforeTestExecutionCallback {
  override fun beforeTestExecution(context: ExtensionContext?) {
    options.forEach { (key, value) ->
      if (value != null) {
        injector.optionGroup.setOptionValue(key, OptionScope.GLOBAL, value)
      }
    }
  }
}
