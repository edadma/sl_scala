package io.github.edadma.sl_scala
package lexer

case class SourceLocation(
  line: Int,
  column: Int,
  offset: Int,    // Absolute offset in source
  length: Int     // Length of the token
)

case class Token(
  tokenType: TokenType,
  lexeme: String,     // The actual text of the token
  location: SourceLocation,
  value: Any = null   // For literals: Int, Double, String, Boolean, etc.
) {
  def typeName: String = tokenType.toDisplayString
  
  override def toString: String = {
    val valueStr = if (value != null) s" ($value)" else ""
    s"Token[${typeName} '${lexeme}'${valueStr} at ${location.line}:${location.column}]"
  }
}