package com.maddyhome.idea.vim.var;

import java.util.HashMap;
import java.util.Map;

/** All Vim Variables
 *
 * Created by psjay on 15/3/14.
 */
public class Variables {

  private static Map<String, Variable> variables = new HashMap<String, Variable>();

  static {
    initDefaultVariables();
  }

  private static void initDefaultVariables() {
    setVariable("mapleader", '\\');
  }

  public static <T> void setVariable(String name, T value) {
    Variable<T> var = variables.get(name);
    if (var != null){
      var.setValue(value);
      return;
    } else {
      var = new Variable<T>(name, value);
      variables.put(name, var);
    }
  }

  public static void deleteVariable(String name) {
    variables.remove(name);
  }

  public static Variable getVariable(String name) {
    return variables.get(name);
  }

  public static Object getVariableValue(String name) {
    Variable var = variables.get(name);
    if (var == null) {
      return null;
    }
    return var.getValue();
  }

  public static <T> T getVariableValue(String name, Class<T> clz) {
    Variable<T> var = variables.get(name);
    if (var == null) {
      return null;
    }
    return var.getValue();
  }

  public static boolean parseVariableLine(String content) {
    int eq = content.indexOf("=");
    if ( eq == -1) {
      return false;
    }
    String name = content.substring(0, eq - 1).trim();
    String value = content.substring(eq + 1, content.length()).trim();
    if ((value.startsWith("\"") && value.endsWith("\""))
        || (value.startsWith("'") && value.endsWith("'"))) {
      // strip quote
      value = value.substring(1, value.length() - 1);
      if (value.length() == 1) {
        // character
        Character chVal = value.charAt(0);
        setVariable(name, chVal);
        return true;
      }
    }
    setVariable(name, value);
    return true;
  }

}
