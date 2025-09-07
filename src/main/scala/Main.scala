package io.github.edadma.sl_scala

import lexer.{Lexer, TokenType}

import java.io.File
import scala.io.Source
import scala.util.Using

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println("Usage: slate <file.sl> or slate -tokens <file.sl>")
      System.exit(1)
    }
    
    val showTokens = args(0) == "-tokens"
    val fileName = if (showTokens && args.length > 1) args(1) else args(0)
    
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
    } else {
      // Just check for lexer errors for now
      val tokens = lexer.getAllTokens()
      
      if (lexer.hasErrors()) {
        lexer.getErrors().foreach(System.err.println)
        System.exit(1)
      } else {
        println(s"Successfully tokenized $fileName (${tokens.length} tokens)")
      }
    }
  }
}