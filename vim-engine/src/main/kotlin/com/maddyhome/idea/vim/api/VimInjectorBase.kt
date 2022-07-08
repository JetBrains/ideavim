package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.api.stubs.ExecutionContextManagerStub
import com.maddyhome.idea.vim.api.stubs.VimApplicationStub
import com.maddyhome.idea.vim.api.stubs.VimEnablerStub
import com.maddyhome.idea.vim.api.stubs.VimMessagesStub
import com.maddyhome.idea.vim.api.stubs.VimProcessGroupStub
import com.maddyhome.idea.vim.common.VimMachine
import com.maddyhome.idea.vim.common.VimMachineBase
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.register.VimRegisterGroup
import com.maddyhome.idea.vim.register.VimRegisterGroupBase
import com.maddyhome.idea.vim.vimscript.services.OptionService
import com.maddyhome.idea.vim.vimscript.services.VariableService
import com.maddyhome.idea.vim.vimscript.services.VimVariableServiceBase

abstract class VimInjectorBase : VimInjector {
  companion object {
    val logger by lazy { vimLogger<VimInjectorBase>() }
    val registerGroupStub by lazy { object : VimRegisterGroupBase() {} }
  }

  override val parser: VimStringParser = object : VimStringParserBase() {}
  override val vimMachine: VimMachine = object : VimMachineBase() {}
  override val optionService: OptionService by lazy { object : VimOptionServiceBase() {} }
  override val variableService: VariableService by lazy { object : VimVariableServiceBase() {} }

  override val registerGroup: VimRegisterGroup by lazy { registerGroupStub }
  override val registerGroupIfCreated: VimRegisterGroup? by lazy { registerGroupStub }
  override val messages: VimMessages by lazy { VimMessagesStub() }
  override val processGroup: VimProcessGroup by lazy { VimProcessGroupStub() }
  override val application: VimApplication by lazy { VimApplicationStub() }
  override val executionContextManager: ExecutionContextManager by lazy { ExecutionContextManagerStub() }
  override val enabler: VimEnabler by lazy { VimEnablerStub() }
}
