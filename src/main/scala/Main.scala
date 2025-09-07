package io.github.edadma.sl_scala

import lexer.{Lexer, TokenType}
import parser.{Parser, Program}

import java.io.File
import scala.io.Source
import scala.util.Using

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println("Usage: slate <file.sl> or slate -tokens <file.sl> or slate -ast <file.sl>")
      System.exit(1)
    }
    
    val showTokens = args(0) == "-tokens"
    val showAST = args(0) == "-ast"
    val fileName = if ((showTokens || showAST) && args.length > 1) args(1) else args(0)
    
    val file = new File(fileName)
    if (!file.exists()) {
      println(s"Error: File '$fileName' not found")
      System.exit(1)
    }
    
    val source = Using(Source.fromFile(file))(_.mkString).get
    val lexer = new Lexer(source, fileName)
    
    if (showTokens) {
      // Show all tokens for debugging
      println(s"=== Tokens for $fileName ===")
      val tokens = lexer.getAllTokens()
      var lineNum = 1
      tokens.foreach { token =>
        if (token.location.line != lineNum) {
          lineNum = token.location.line
          println()
        }
        print(s"${token.typeName} ")
        if (token.tokenType == TokenType.TOKEN_NEWLINE) println()
      }
      println("\n=== End of tokens ===")
      
      if (lexer.hasErrors()) {
        println("\nErrors found:")
        lexer.getErrors().foreach(println)
      }
    } else if (showAST) {
      // Show AST for debugging
      println(s"=== AST for $fileName ===")
      val parser = new Parser(lexer, fileName)
      val result = parser.parseProgram()
      
      if (result.isSuccess()) {
        printAST(result.result, 0)
      } else {
        println(s"Parse error: ${result.errorMessage}")
      }
      
      if (parser.hasErrors()) {
        println("\nParse errors found:")
        parser.getErrors().foreach(println)
      }
      println("=== End of AST ===")
    } else {
      // Parse the file and check for errors
      val parser = new Parser(lexer, fileName)
      val result = parser.parseProgram()
      
      if (lexer.hasErrors()) {
        println("Lexer errors:")
        lexer.getErrors().foreach(System.err.println)
        System.exit(1)
      } else if (parser.hasErrors()) {
        println("Parser errors:")
        parser.getErrors().foreach(System.err.println)
        System.exit(1)
      } else if (result.isSuccess()) {
        println(s"Successfully parsed $fileName")
        val program = result.result
        println(s"  ${program.statements.length} top-level statements")
      } else {
        println(s"Parse failed: ${result.errorMessage}")
        System.exit(1)
      }
    }
  }
  
  // Helper method to print AST structure
  def printAST(node: Any, indent: Int): Unit = {
    val prefix = "  " * indent
    
    node match {
      case p: Program =>
        println(s"${prefix}Program(${p.statements.length} statements)")
        p.statements.foreach(printAST(_, indent + 1))
        
      case v: parser.VarDeclaration =>
        println(s"${prefix}VarDeclaration(${v.name})")
        if (v.initializer != null) printAST(v.initializer, indent + 1)
        
      case v: parser.ValDeclaration =>
        println(s"${prefix}ValDeclaration(${v.name})")
        printAST(v.initializer, indent + 1)
        
      case f: parser.FunctionDeclaration =>
        println(s"${prefix}FunctionDeclaration(${f.name}, params: ${f.parameters.mkString(", ")})")
        printAST(f.body, indent + 1)
        
      case e: parser.ExpressionStatement =>
        println(s"${prefix}ExpressionStatement")
        printAST(e.expression, indent + 1)
        
      case b: parser.BinaryExpression =>
        println(s"${prefix}BinaryExpression(${b.operator})")
        printAST(b.left, indent + 1)
        printAST(b.right, indent + 1)
        
      case u: parser.UnaryExpression =>
        println(s"${prefix}UnaryExpression(${u.operator}, prefix: ${u.isPrefix})")
        printAST(u.operand, indent + 1)
        
      case a: parser.AssignmentExpression =>
        println(s"${prefix}AssignmentExpression(${a.operator})")
        printAST(a.target, indent + 1)
        printAST(a.value, indent + 1)
        
      case f: parser.FunctionCallExpression =>
        println(s"${prefix}FunctionCallExpression(${f.arguments.length} args)")
        printAST(f.function, indent + 1)
        f.arguments.foreach(printAST(_, indent + 1))
        
      case p: parser.PropertyAccessExpression =>
        println(s"${prefix}PropertyAccessExpression(${p.property}, optional: ${p.isOptional})")
        printAST(p.obj, indent + 1)
        
      case a: parser.ArrayLiteralExpression =>
        println(s"${prefix}ArrayLiteralExpression(${a.elements.length} elements)")
        a.elements.foreach(printAST(_, indent + 1))
        
      case o: parser.ObjectLiteralExpression =>
        println(s"${prefix}ObjectLiteralExpression(${o.properties.length} properties)")
        o.properties.foreach { prop =>
          println(s"${prefix}  Property(${prop.key})")
          printAST(prop.value, indent + 2)
        }
        
      case i: parser.Identifier =>
        println(s"${prefix}Identifier(${i.name})")
        
      case il: parser.IntegerLiteral =>
        println(s"${prefix}IntegerLiteral(${il.value})")
        
      case fl: parser.FloatLiteral =>
        println(s"${prefix}FloatLiteral(${fl.value})")
        
      case sl: parser.StringLiteral =>
        println(s"${prefix}StringLiteral(\"${sl.value}\")")
        
      case bl: parser.BooleanLiteral =>
        println(s"${prefix}BooleanLiteral(${bl.value})")
        
      case _: parser.NullLiteral =>
        println(s"${prefix}NullLiteral")
        
      case _: parser.UndefinedLiteral =>
        println(s"${prefix}UndefinedLiteral")
        
      case other =>
        println(s"${prefix}${other.getClass.getSimpleName}")
    }
  }
}