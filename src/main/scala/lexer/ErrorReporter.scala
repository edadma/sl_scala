package io.github.edadma.sl_scala
package lexer

object ErrorReporter {
  
  def reportError(source: String, fileName: String, location: SourceLocation, message: String): Unit = {
    System.err.println(s"Error in $fileName:${location.line}:${location.column}: $message")
    showSourceContext(source, location)
  }
  
  def reportTokenError(source: String, fileName: String, token: Token, message: String): Unit = {
    reportError(source, fileName, token.location, message)
  }
  
  private def showSourceContext(source: String, location: SourceLocation): Unit = {
    // Find the line containing the error
    val lines = source.split("\n", -1)
    if (location.line > 0 && location.line <= lines.length) {
      val lineIndex = location.line - 1
      val line = lines(lineIndex)
      
      // Show the line
      System.err.println(s"    ${location.line} | $line")
      
      // Show the caret pointing to the error
      val padding = s"    ${location.line} | ".length
      val caretPos = location.column - 1
      val spaces = " " * (padding + caretPos)
      val carets = "^" * Math.max(1, location.length)
      System.err.println(s"$spaces$carets")
    }
  }
  
  def formatError(fileName: String, line: Int, column: Int, message: String): String = {
    s"$fileName:$line:$column: error: $message"
  }
}