package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.api.stubs.ExecutionContextManagerStub
import com.maddyhome.idea.vim.api.stubs.VimApplicationStub
import com.maddyhome.idea.vim.api.stubs.VimEnablerStub
import com.maddyhome.idea.vim.api.stubs.VimMessagesStub
import com.maddyhome.idea.vim.api.stubs.VimProcessGroupStub
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.common.VimMachine
import com.maddyhome.idea.vim.common.VimMachineBase
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.mark.VimMarkGroup
import com.maddyhome.idea.vim.register.VimRegisterGroup
import com.maddyhome.idea.vim.register.VimRegisterGroupBase
import javax.swing.KeyStroke

abstract class VimInjectorBase : VimInjector {
  companion object {
    val logger = vimLogger<VimInjectorBase>()
    val registerGroupStub = object : VimRegisterGroupBase() {}
  }

  // todo remove StringHelper & CommonStringHelper
  override val parser: VimStringParser = object : VimStringParserBase() {}
  override val vimMachine: VimMachine = object : VimMachineBase() {}
  override val registerGroup: VimRegisterGroup = registerGroupStub
  override val registerGroupIfCreated: VimRegisterGroup? = registerGroupStub
  override val messages: VimMessages = VimMessagesStub()
  override val processGroup: VimProcessGroup = VimProcessGroupStub()
  override val application: VimApplication = VimApplicationStub()
  override val executionContextManager: ExecutionContextManager = ExecutionContextManagerStub()
  override val enabler: VimEnabler = VimEnablerStub()
}