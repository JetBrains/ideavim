/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

sealed interface OptionType {
  data class IntType(val value: Int) : OptionType
  data class StringType(val value: String) : OptionType
  data class BooleanType(val value: Boolean) : OptionType
}

sealed interface Option<T> {
  val name: String
  val declaredScope: OptionDeclaredScope
  val abbrev: String
  val defaultValue: T
  val unsetValue: T
  val isLocalNoGlobal: Boolean
  val isHidden: Boolean

  data class StringOption(
    override val name: String,
    override val declaredScope: OptionDeclaredScope,
    override val abbrev: String,
    override val defaultValue: OptionType.StringType,
    override val unsetValue: OptionType.StringType,
    override val isLocalNoGlobal: Boolean,
    override val isHidden: Boolean,
  ) : Option<OptionType.StringType>

  data class IntOption(
    override val name: String,
    override val declaredScope: OptionDeclaredScope,
    override val abbrev: String,
    override val defaultValue: OptionType.IntType,
    override val unsetValue: OptionType.IntType,
    override val isLocalNoGlobal: Boolean,
    override val isHidden: Boolean,
  ) : Option<OptionType.IntType>

  data class BooleanOption(
    override val name: String,
    override val declaredScope: OptionDeclaredScope,
    override val abbrev: String,
    override val defaultValue: OptionType.BooleanType,
    override val unsetValue: OptionType.BooleanType,
    override val isLocalNoGlobal: Boolean,
    override val isHidden: Boolean,
  ) : Option<OptionType.BooleanType>

  data class StringListOption(
    override val name: String,
    override val declaredScope: OptionDeclaredScope,
    override val abbrev: String,
    override val defaultValue: OptionType.StringType,
    override val unsetValue: OptionType.StringType,
    override val isLocalNoGlobal: Boolean,
    override val isHidden: Boolean,
    val values: List<String>? = null,
  ) : Option<OptionType.StringType>
}

enum class OptionDeclaredScope {
  GLOBAL,
  LOCAL_TO_BUFFER,
  LOCAL_TO_WINDOW,
  GLOBAL_OR_LOCAL_TO_BUFFER,
  GLOBAL_OR_LOCAL_TO_WINDOW
}