package com.maddyhome.idea.vim.helper

import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.editor.CaretVisualAttributes
import java.awt.Color

// [VERSION UPDATE] 212+
internal fun getCaretVisualAttributes(color: Color?, weight: CaretVisualAttributes.Weight, shape: String, thickness: Float): CaretVisualAttributes {
  if (ApplicationInfoEx.getInstance().build.components[0] >= 212) {
    val constructor = CaretVisualAttributes::class.java.constructors.find { it.parameterCount == 4 }!!
    return constructor.newInstance(color, weight, getShape(shape), thickness) as CaretVisualAttributes
  }
  else {
    val constrcutor = CaretVisualAttributes::class.java.constructors.find { it.parameterCount == 2 }!!
    return constrcutor.newInstance(color, weight) as CaretVisualAttributes
  }
}

@Suppress("UNCHECKED_CAST")
internal fun getShape(shape: String): Any {
  val shapeClass = CaretVisualAttributes::class.java.classLoader.loadClass("com.intellij.openapi.editor.CaretVisualAttributes\$Shape")
  return java.lang.Enum.valueOf(shapeClass as Class<Enum<*>>, shape)
}
