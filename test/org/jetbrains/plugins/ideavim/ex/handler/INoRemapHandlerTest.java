package org.jetbrains.plugins.ideavim.ex.handler;

import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.ex.handler.INoRemapHandler;
import org.jetbrains.plugins.ideavim.VimTestCase;

public class INoRemapHandlerTest extends VimTestCase{
  public void test1ArgThrowsExException() {
    non2ArgsThrowsExException("1arg");
  }

  public void test3ArgsThrowsExException() {
    non2ArgsThrowsExException("1arg 2arg 3arg");
  }

  public void testImplementedIfEndsWithEsc() {
    assertImplemented("jk <esc>", true);
  }

  public void testNotImplementIfDoesNotEndWithEsc() {
    assertImplemented("jk ab", false);
  }


  public void assertImplemented(String args, boolean assertValue) {
    INoRemapHandler handler = new INoRemapHandler();
    ExCommand cmd = new ExCommand(null, "inoremap", args);
    try {
    assertEquals(handler.execute(null, null, cmd), assertValue);
    }
    catch (ExException e) {
      throw new RuntimeException(e);
    }

  }

  public void non2ArgsThrowsExException(String args) {
    INoRemapHandler handler = new INoRemapHandler();
    ExCommand cmd = new ExCommand(null, "inoremap", args);
    try {
      handler.execute(null, null, cmd);
      assertTrue(false);
    }
    catch (ExException e) {
      assertTrue(true);
    }
  }
}
