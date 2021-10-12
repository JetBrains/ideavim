package com.maddyhome.idea.vim.helper

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretVisualAttributes
import java.awt.Color

// [VERSION UPDATE] 212+ (the whole file)

internal fun getCaretVisualAttributes(color: Color?, weight: CaretVisualAttributes.Weight, shape: String, thickness: Float): CaretVisualAttributes {
  if (buildGreater212()) {
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

internal fun Caret.shape(): Any? {
  val method = CaretVisualAttributes::class.java.getMethod("getShape")
  return method.invoke(this.visualAttributes)
}

internal fun Caret.thickness(): Any? {
  val method = CaretVisualAttributes::class.java.getMethod("getThickness")
  return method.invoke(this.visualAttributes)
}

internal fun buildGreater212(): Boolean {
  return ApplicationInfo.getInstance().build.baselineVersion >= 212
}
