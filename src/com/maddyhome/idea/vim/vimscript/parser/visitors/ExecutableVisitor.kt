package com.maddyhome.idea.vim.vimscript.parser.visitors

import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.statements.CatchBlock
import com.maddyhome.idea.vim.vimscript.model.statements.FinallyBlock
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.IfStatement
import com.maddyhome.idea.vim.vimscript.model.statements.ReturnStatement
import com.maddyhome.idea.vim.vimscript.model.statements.ThrowStatement
import com.maddyhome.idea.vim.vimscript.model.statements.TryBlock
import com.maddyhome.idea.vim.vimscript.model.statements.TryStatement
import com.maddyhome.idea.vim.vimscript.model.statements.loops.BreakStatement
import com.maddyhome.idea.vim.vimscript.model.statements.loops.ContinueStatement
import com.maddyhome.idea.vim.vimscript.model.statements.loops.ForLoop
import com.maddyhome.idea.vim.vimscript.model.statements.loops.WhileLoop
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptBaseVisitor
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser

object ExecutableVisitor : VimscriptBaseVisitor<Executable>() {

  override fun visitExecutable(ctx: VimscriptParser.ExecutableContext): Executable? {
    return when {
      ctx.command() != null -> CommandVisitor.visit(ctx.command())
      ctx.ifStatement() != null -> visitIfStatement(ctx.ifStatement())
      ctx.forLoop() != null -> visitForLoop(ctx.forLoop())
      ctx.whileLoop() != null -> visitWhileLoop(ctx.whileLoop())
      ctx.functionDefinition() != null -> visitFunctionDefinition(ctx.functionDefinition())
      ctx.tryStatement() != null -> visitTryStatement(ctx.tryStatement())
      else -> null
    }
  }

  override fun visitBlockMember(ctx: VimscriptParser.BlockMemberContext): Executable? {
    return when {
      ctx.command() != null -> CommandVisitor.visit(ctx.command())
      ctx.breakStatement() != null -> BreakStatement
      ctx.continueStatement() != null -> ContinueStatement
      ctx.returnStatement() != null -> ReturnStatement(ExpressionVisitor.visit(ctx.returnStatement().expr()))
      ctx.ifStatement() != null -> visitIfStatement(ctx.ifStatement())
      ctx.forLoop() != null -> visitForLoop(ctx.forLoop())
      ctx.whileLoop() != null -> visitWhileLoop(ctx.whileLoop())
      ctx.functionDefinition() != null -> visitFunctionDefinition(ctx.functionDefinition())
      ctx.throwStatement() != null -> visitThrowStatement(ctx.throwStatement())
      ctx.tryStatement() != null -> visitTryStatement(ctx.tryStatement())
      else -> null
    }
  }

  override fun visitWhileLoop(ctx: VimscriptParser.WhileLoopContext): Executable {
    val condition: Expression = ExpressionVisitor.visit(ctx.expr())
    val body: List<Executable> = ctx.blockMember().mapNotNull { visitBlockMember(it) }
    return WhileLoop(condition, body)
  }

  override fun visitForLoop(ctx: VimscriptParser.ForLoopContext): Executable {
    val variableName = ctx.variableName().text
    val iterable = ExpressionVisitor.visit(ctx.expr())
    val body = ctx.blockMember().mapNotNull { visitBlockMember(it) }
    return ForLoop(variableName, iterable, body)
  }

  override fun visitFunctionDefinition(ctx: VimscriptParser.FunctionDefinitionContext): Executable {
    val functionScope = if (ctx.functionScope() != null) Scope.getByValue(ctx.functionScope().text) else null
    val functionName = ctx.functionName().text
    val args = ctx.argumentsDeclaration().variableName().map { it.text }
    val body = ctx.blockMember().mapNotNull { visitBlockMember(it) }
    val replaceExisting = ctx.replace != null
    return FunctionDeclaration(functionScope, functionName, args, body, replaceExisting)
  }

  override fun visitTryStatement(ctx: VimscriptParser.TryStatementContext): Executable {
    val tryBlock = TryBlock(ctx.tryBlock().blockMember().mapNotNull { visitBlockMember(it) })
    val catchBlocks: MutableList<CatchBlock> = mutableListOf()
    for (catchBlock in ctx.catchBlock()) {
      catchBlocks.add(
        CatchBlock(
          catchBlock.pattern().patternBody().text,
          catchBlock.blockMember().mapNotNull { visitBlockMember(it) }
        )
      )
    }
    var finallyBlock: FinallyBlock? = null
    if (ctx.finallyBlock() != null) {
      finallyBlock = FinallyBlock(ctx.finallyBlock().blockMember().mapNotNull { visitBlockMember(it) })
    }
    return TryStatement(tryBlock, catchBlocks, finallyBlock)
  }

  override fun visitReturnStatement(ctx: VimscriptParser.ReturnStatementContext): Executable {
    val expression: Expression = ExpressionVisitor.visit(ctx.expr())
    return ReturnStatement(expression)
  }

  override fun visitThrowStatement(ctx: VimscriptParser.ThrowStatementContext): Executable {
    val expression: Expression = ExpressionVisitor.visit(ctx.expr())
    return ThrowStatement(expression)
  }

  override fun visitIfStatement(ctx: VimscriptParser.IfStatementContext): Executable {
    val conditionToBody: MutableList<Pair<Expression, List<Executable>>> = mutableListOf()
    conditionToBody.add(
      ExpressionVisitor.visit(ctx.ifBlock().expr()) to ctx.ifBlock().blockMember()
        .mapNotNull { visitBlockMember(it) }
    )
    if (ctx.elifBlock() != null) {
      conditionToBody.addAll(
        ctx.elifBlock().map {
          ExpressionVisitor.visit(it.expr()) to it.blockMember().mapNotNull { it2 -> visitBlockMember(it2) }
        }
      )
    }
    if (ctx.elseBlock() != null) {
      conditionToBody.add(
        SimpleExpression(VimInt(1)) to ctx.elseBlock().blockMember()
          .mapNotNull { visitBlockMember(it) }
      )
    }
    return IfStatement(conditionToBody)
  }
}
