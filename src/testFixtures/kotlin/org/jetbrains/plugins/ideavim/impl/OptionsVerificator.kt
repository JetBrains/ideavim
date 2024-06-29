/*
 * Copyright 2003-2024 The IdeaVim authors
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
import com.maddyhome.idea.vim.api.VimInjector
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.group.IjVimOptionGroup
import com.maddyhome.idea.vim.options.EffectiveOptionValueChangeListener
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.StringListOption
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.options.UnsignedNumberOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.TestIjOptionConstants
import org.jetbrains.plugins.ideavim.TestOptionConstants
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
annotation class TraceOptions(vararg val optionNames: String)

/**
 * Defines values of options for tests. Test will be started multiple times with all possible combinations
 *   of the defined options.
 * Read the doc for [VimOption] to understand how the option can be defined
 */
@ExtendWith(VimOptionsInvocator::class)
@TestTemplate
@Target(AnnotationTarget.FUNCTION)
annotation class OptionTest(vararg val value: VimOption)

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
annotation class VimOption(
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
    getStore(context).put("OriginalInjector", injector)
    getStore(context).put("TraceCollector", traceCollector)
    testInjector.setTracer(OptionsTracer, traceCollector)
    testInjector.setTracer("OptionTracerIgnore", ignore)
    injector = testInjector
  }

  override fun afterTestExecution(context: ExtensionContext) {
    injector = getStore(context).get("OriginalInjector", VimInjector::class.java)
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
//      LOG.warn("Options $specifiedButNotUsed are specified in annotation, but not actually used for test ${context.testClass.get().simpleName}.${context.testMethod.get().name}")
    }
  }

  private fun getStore(context: ExtensionContext): ExtensionContext.Store {
    return context.getStore(ExtensionContext.Namespace.create(javaClass, context.requiredTestMethod))
  }

  companion object {
    val LOG by lazy { vimLogger<OptionsVerificator>() }
    private val ignored = setOf(
      TestOptionConstants.guicursor,
      TestOptionConstants.ideatracetime,
      TestIjOptionConstants.ideavimsupport,
      TestOptionConstants.maxmapdepth,
      TestOptionConstants.number,
      TestOptionConstants.relativenumber,
      TestOptionConstants.scrolljump,
      TestOptionConstants.scrolloff,
      TestOptionConstants.showmode,
      TestOptionConstants.sidescroll,
      TestOptionConstants.sidescrolloff,
      TestOptionConstants.timeoutlen,
      TestIjOptionConstants.trackactionids,
      TestIjOptionConstants.unifyjumps,
      TestOptionConstants.virtualedit,
      TestOptionConstants.whichwrap,
    )
  }
}

internal class OptionsTraceCollector {
  val requestedKeys = HashSet<String>()
}

internal class OptionsTracer(
  private val vimOptionGroup: IjVimOptionGroup,
  private val trace: OptionsTraceCollector,
  private val ignoreFlag: AtomicBoolean,
) : IjVimOptionGroup by vimOptionGroup {
  override fun getOption(key: String): Option<VimDataType>? {
    if (!ignoreFlag.get()) {
      trace.requestedKeys += key
    }
    return vimOptionGroup.getOption(key)
  }

  override fun getAllOptions(): Set<Option<VimDataType>> {
    val allOptions = vimOptionGroup.getAllOptions()
    if (!ignoreFlag.get()) {
      allOptions.forEach { trace.requestedKeys += it.name }
    }
    return allOptions
  }

  override fun <T : VimDataType> getOptionValue(option: Option<T>, scope: OptionAccessScope): T {
    if (!ignoreFlag.get()) {
      trace.requestedKeys += option.name
    }
    return vimOptionGroup.getOptionValue(option, scope)
  }

  override fun <T : VimDataType> addGlobalOptionChangeListener(
    option: Option<T>,
    listener: GlobalOptionChangeListener
  ) {
    ignoreFlag.set(true)
    try {
      vimOptionGroup.addGlobalOptionChangeListener(option, listener)
    }
    finally {
      ignoreFlag.set(false)
    }
  }

  override fun <T : VimDataType> removeGlobalOptionChangeListener(
    option: Option<T>,
    listener: GlobalOptionChangeListener
  ) {
    ignoreFlag.set(true)
    try {
      vimOptionGroup.removeGlobalOptionChangeListener(option, listener)
    }
    finally {
      ignoreFlag.set(false)
    }
  }

  override fun <T : VimDataType> addEffectiveOptionValueChangeListener(
    option: Option<T>,
    listener: EffectiveOptionValueChangeListener
  ) {
    ignoreFlag.set(true)
    try {
      vimOptionGroup.addEffectiveOptionValueChangeListener(option, listener)
    }
    finally {
      ignoreFlag.set(false)
    }
  }

  override fun <T : VimDataType> removeEffectiveOptionValueChangeListener(
    option: Option<T>,
    listener: EffectiveOptionValueChangeListener
  ) {
    ignoreFlag.set(true)
    try {
      vimOptionGroup.removeEffectiveOptionValueChangeListener(option, listener)
    }
    finally {
      ignoreFlag.set(false)
    }
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
      return generateContexts(context)
    } finally {
      fixture.tearDown()
    }
  }

  // Sometimes we need an injector before @BeforeEach function is executed. We set up and tear down the project for this case
  private fun fixtureSetup(): CodeInsightTestFixture {
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
    val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor, "IdeaVim")
    val projectFixture = fixtureBuilder.fixture
    val testFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
      projectFixture,
      LightTempDirTestFixtureImpl(true),
    )
    testFixture.setUp()
    return testFixture
  }

  private fun generateContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    val annotation = context.testMethod.get().getAnnotation(OptionTest::class.java)
    val options: List<List<Pair<Option<out VimDataType>, VimDataType?>>> = annotation.value.map { vimOption ->
      val optionName = vimOption.name
      // Explicitly treat the return value as covariant, so we can compare it against derived types that are specialised
      // by derived types of `VimDataType`
      val option: Option<out VimDataType> = injector.optionGroup.getOption(optionName)!!
      if (!vimOption.doesntAffectTest) {
        if (vimOption.limitedValues.isEmpty()) {
          defaultOptionCombinations(option)
        } else {
          when (option) {
            is ToggleOption -> vimOption.limitedValues.map { option to if (it == "true") VimInt.ONE else VimInt.ZERO }
            is NumberOption -> vimOption.limitedValues.map { option to VimInt(it) }
            is StringOption, is StringListOption -> {
              vimOption.limitedValues.map { limitedValue -> option to VimString(limitedValue) }
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
        val values = if (option is UnsignedNumberOption) {
          listOf(VimInt.ZERO, VimInt.ONE, VimInt(10), VimInt(1000))
        } else {
          listOf(VimInt(-1000), VimInt(-10), VimInt(-1), VimInt.ZERO, VimInt.ONE, VimInt(10), VimInt(1000))
        }
        values.map { option to it }
      }

      is StringOption -> {
        val boundedValues = option.boundedValues
        boundedValues?.map { option to VimString(it) }
          ?: fail("Cannot generate values automatically. Please specify option values explicitly using 'limitedValues' field")
      }

      is StringListOption -> {
        val boundedValues = option.boundedValues
        if (boundedValues != null) {
          val valuesCombinations = boundedValues.indices.map { index ->
            kCombinations(boundedValues.toList(), index + 1)
              .map { VimString(it.joinToString(",")) }
          }.flatten()
          valuesCombinations.map { option to it }
        } else {
          fail("Cannot generate values automatically. Please specify option values explicitly using 'limitedValues' field")
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
      var j: Int = sequenceLength - 1
      while (j >= 0 && s[j] == input.size - sequenceLength + j) {
        j--
      }
      if (j < 0) {
        break
      }
      s[j]++ // increment this item
      ++j
      while (j < sequenceLength) {
        // fill up remaining items
        s[j] = s[j - 1] + 1
        j++
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

class OptionsSetup(private val options: List<Pair<Option<out VimDataType>, VimDataType?>>) : BeforeTestExecutionCallback {
  override fun beforeTestExecution(context: ExtensionContext?) {
    options.forEach { (key, value) ->
      if (value != null) {
        // We must explicitly make an unchecked cast to remove the out annotation so that we can set the value, or the
        // compiler will treat the value as type `CapturedType(out VimDataType)`, which cannot be passed in (producer vs
        // consumer)
        @Suppress("UNCHECKED_CAST") val option = key as Option<VimDataType>

        // We don't have a window at this point, so we can only pass null. This means we can't set the global value of a
        // local option (local-to-window's global options are per-window), or we will hit asserts
        injector.optionGroup.setOptionValue(option, OptionAccessScope.GLOBAL(null), value)
      }
    }
  }
}
