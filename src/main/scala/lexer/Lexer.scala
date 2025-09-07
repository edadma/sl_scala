package io.github.edadma.sl_scala
package lexer

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

class Lexer(sourceInput: String, fileName: String = "<unknown>") {
  
  // Source tracking
  private val source = sourceInput
  private var current = 0        // Current position in source
  private var line = 1           // Current line number
  private var column = 1         // Current column number
  private var tokenStart = 0     // Start of current token
  private var tokenLine = 1      // Line where current token started
  private var tokenColumn = 1    // Column where current token started
  
  // Indentation tracking
  private val indentStack = ArrayBuffer[Int](0)  // Stack of indentation levels
  private var atLineStart = true                 // Are we at the start of a line?
  private var pendingDedents = 0                 // Number of DEDENT tokens to emit
  private var braceDepth = 0                     // Track nesting for ignoring indentation
  private var parenDepth = 0
  private var bracketDepth = 0
  
  // Template literal tracking
  private var inTemplate = false
  private var templateBraceDepth = 0
  
  // Error tracking
  private val errors = ArrayBuffer[String]()
  
  // Keywords map
  private val keywords = HashMap[String, TokenType](
    "var" -> TokenType.TOKEN_VAR,
    "val" -> TokenType.TOKEN_VAL,
    "def" -> TokenType.TOKEN_DEF,
    "if" -> TokenType.TOKEN_IF,
    "then" -> TokenType.TOKEN_THEN,
    "elif" -> TokenType.TOKEN_ELIF,
    "else" -> TokenType.TOKEN_ELSE,
    "while" -> TokenType.TOKEN_WHILE,
    "do" -> TokenType.TOKEN_DO,
    "for" -> TokenType.TOKEN_FOR,
    "loop" -> TokenType.TOKEN_LOOP,
    "break" -> TokenType.TOKEN_BREAK,
    "continue" -> TokenType.TOKEN_CONTINUE,
    "return" -> TokenType.TOKEN_RETURN,
    "import" -> TokenType.TOKEN_IMPORT,
    "package" -> TokenType.TOKEN_PACKAGE,
    "private" -> TokenType.TOKEN_PRIVATE,
    "match" -> TokenType.TOKEN_MATCH,
    "case" -> TokenType.TOKEN_CASE,
    "default" -> TokenType.TOKEN_DEFAULT,
    "data" -> TokenType.TOKEN_DATA,
    "and" -> TokenType.TOKEN_AND,
    "or" -> TokenType.TOKEN_OR,
    "not" -> TokenType.TOKEN_NOT,
    "in" -> TokenType.TOKEN_IN,
    "instanceof" -> TokenType.TOKEN_INSTANCEOF,
    "mod" -> TokenType.TOKEN_MOD,
    "step" -> TokenType.TOKEN_STEP,
    "end" -> TokenType.TOKEN_END,
    "true" -> TokenType.TOKEN_TRUE,
    "false" -> TokenType.TOKEN_FALSE,
    "null" -> TokenType.TOKEN_NULL,
    "undefined" -> TokenType.TOKEN_UNDEFINED,
    "NaN" -> TokenType.TOKEN_NAN,
    "Infinity" -> TokenType.TOKEN_INFINITY
  )
  
  // Token buffer for handling DEDENT tokens
  private val tokenBuffer = ArrayBuffer[Token]()
  
  // Public API
  def nextToken(): Token = {
    if (tokenBuffer.nonEmpty) {
      return tokenBuffer.remove(0)
    }
    
    scanToken()
  }
  
  def getAllTokens(): Array[Token] = {
    val tokens = ArrayBuffer[Token]()
    var token = nextToken()
    while (token.tokenType != TokenType.TOKEN_EOF) {
      tokens += token
      token = nextToken()
    }
    tokens += token // Add EOF token
    tokens.toArray
  }
  
  def getErrors(): Array[String] = errors.toArray
  
  def hasErrors(): Boolean = errors.nonEmpty
  
  // Character utilities
  private def isAtEnd(): Boolean = current >= source.length
  
  private def peek(): Char = {
    if (isAtEnd()) '\u0000' else source.charAt(current)
  }
  
  private def peekNext(): Char = {
    if (current + 1 >= source.length) '\u0000' else source.charAt(current + 1)
  }
  
  private def advance(): Char = {
    val c = peek()
    current += 1
    if (c == '\n') {
      line += 1
      column = 1
    } else {
      column += 1
    }
    c
  }
  
  private def matchChar(expected: Char): Boolean = {
    if (isAtEnd()) return false
    if (peek() != expected) return false
    advance()
    true
  }
  
  private def skipWhitespace(): Unit = {
    while (!isAtEnd()) {
      peek() match {
        case ' ' | '\r' | '\t' => advance()
        case _ => return
      }
    }
  }
  
  private def skipComment(): Unit = {
    // Comments start with \ and go to end of line
    if (peek() == '\\') {
      advance() // Skip the backslash
      while (peek() != '\n' && !isAtEnd()) {
        advance()
      }
    }
  }
  
  // Token creation
  private def makeToken(tokenType: TokenType, value: Any = null): Token = {
    val lexeme = source.substring(tokenStart, current)
    val location = SourceLocation(tokenLine, tokenColumn, tokenStart, current - tokenStart)
    Token(tokenType, lexeme, location, value)
  }
  
  private def errorToken(message: String): Token = {
    val location = SourceLocation(tokenLine, tokenColumn, tokenStart, current - tokenStart)
    errors += s"$fileName:${location.line}:${location.column}: $message"
    Token(TokenType.TOKEN_ERROR, message, location, null)
  }
  
  // Main scanning logic
  private def scanToken(): Token = {
    // Handle pending dedents
    if (pendingDedents > 0) {
      pendingDedents -= 1
      return makeToken(TokenType.TOKEN_DEDENT)
    }
    
    // Skip whitespace but track if we're at line start
    if (atLineStart && braceDepth == 0 && parenDepth == 0 && bracketDepth == 0) {
      val indentLevel = countIndentation()
      if (indentLevel >= 0) { // Not a blank line
        val token = handleIndentation(indentLevel)
        if (token != null) return token
      }
    }
    
    skipWhitespace()
    
    // Skip comments
    while (peek() == '\\') {
      skipComment()
      skipWhitespace()
    }
    
    tokenStart = current
    tokenLine = line
    tokenColumn = column
    
    if (isAtEnd()) {
      // Emit remaining dedents at EOF
      if (indentStack.length > 1) {
        // Set pending dedents to all levels except the base level (0)
        pendingDedents = indentStack.length - 1
        indentStack.clear()
        indentStack += 0  // Reset to base level
        if (pendingDedents > 0) {
          pendingDedents -= 1
          return makeToken(TokenType.TOKEN_DEDENT)
        }
      }
      return makeToken(TokenType.TOKEN_EOF)
    }
    
    atLineStart = false
    
    val c = advance()
    
    // Handle template literals specially
    if (inTemplate && templateBraceDepth == 0) {
      return scanTemplatePart(c)
    }
    
    c match {
      // Single character tokens
      case '(' => 
        parenDepth += 1
        makeToken(TokenType.TOKEN_LEFT_PAREN)
      case ')' => 
        parenDepth -= 1
        makeToken(TokenType.TOKEN_RIGHT_PAREN)
      case '[' => 
        bracketDepth += 1
        makeToken(TokenType.TOKEN_LEFT_BRACKET)
      case ']' => 
        bracketDepth -= 1
        makeToken(TokenType.TOKEN_RIGHT_BRACKET)
      case '{' => 
        if (inTemplate) templateBraceDepth += 1
        braceDepth += 1
        makeToken(TokenType.TOKEN_LEFT_BRACE)
      case '}' => 
        if (inTemplate && templateBraceDepth > 0) {
          templateBraceDepth -= 1
          if (templateBraceDepth == 0) {
            // This closes a template expression
            braceDepth -= 1
            return makeToken(TokenType.TOKEN_TEMPLATE_EXPR_END)
          }
        }
        braceDepth -= 1
        makeToken(TokenType.TOKEN_RIGHT_BRACE)
      case ',' => makeToken(TokenType.TOKEN_COMMA)
      case ';' => makeToken(TokenType.TOKEN_SEMICOLON)
      case ':' => makeToken(TokenType.TOKEN_COLON)
      case '~' => makeToken(TokenType.TOKEN_TILDE)
      case '?' =>
        if (matchChar('.')) makeToken(TokenType.TOKEN_QUESTION_DOT)
        else if (matchChar('?')) {
          if (matchChar('=')) makeToken(TokenType.TOKEN_QUESTION_QUESTION_EQUAL)
          else makeToken(TokenType.TOKEN_QUESTION_QUESTION)
        }
        else makeToken(TokenType.TOKEN_QUESTION)
      
      // Newlines
      case '\n' =>
        atLineStart = true
        if (braceDepth == 0 && parenDepth == 0 && bracketDepth == 0) {
          makeToken(TokenType.TOKEN_NEWLINE)
        } else {
          // Inside braces/parens/brackets, ignore newlines
          scanToken()
        }
      
      // Operators and multi-character tokens
      case '+' =>
        if (matchChar('+')) makeToken(TokenType.TOKEN_PLUS_PLUS)
        else if (matchChar('=')) makeToken(TokenType.TOKEN_PLUS_EQUAL)
        else makeToken(TokenType.TOKEN_PLUS)
      
      case '-' =>
        if (matchChar('-')) makeToken(TokenType.TOKEN_MINUS_MINUS)
        else if (matchChar('>')) makeToken(TokenType.TOKEN_ARROW)
        else if (matchChar('=')) makeToken(TokenType.TOKEN_MINUS_EQUAL)
        else makeToken(TokenType.TOKEN_MINUS)
      
      case '*' =>
        if (matchChar('*')) {
          if (matchChar('=')) makeToken(TokenType.TOKEN_POWER_EQUAL)
          else makeToken(TokenType.TOKEN_POWER)
        }
        else if (matchChar('=')) makeToken(TokenType.TOKEN_STAR_EQUAL)
        else makeToken(TokenType.TOKEN_STAR)
      
      case '/' =>
        if (matchChar('/')) {
          if (matchChar('=')) makeToken(TokenType.TOKEN_SLASH_SLASH_EQUAL)
          else makeToken(TokenType.TOKEN_SLASH_SLASH)
        }
        else if (matchChar('=')) makeToken(TokenType.TOKEN_SLASH_EQUAL)
        else makeToken(TokenType.TOKEN_SLASH)
      
      case '%' =>
        if (matchChar('=')) makeToken(TokenType.TOKEN_PERCENT_EQUAL)
        else makeToken(TokenType.TOKEN_PERCENT)
      
      case '&' =>
        if (matchChar('&')) {
          if (matchChar('=')) makeToken(TokenType.TOKEN_AMP_AMP_EQUAL)
          else makeToken(TokenType.TOKEN_AMP_AMP)
        }
        else if (matchChar('=')) makeToken(TokenType.TOKEN_AMP_EQUAL)
        else makeToken(TokenType.TOKEN_AMP)
      
      case '|' =>
        if (matchChar('|')) {
          if (matchChar('=')) makeToken(TokenType.TOKEN_PIPE_PIPE_EQUAL)
          else makeToken(TokenType.TOKEN_PIPE_PIPE)
        }
        else if (matchChar('=')) makeToken(TokenType.TOKEN_PIPE_EQUAL)
        else makeToken(TokenType.TOKEN_PIPE)
      
      case '^' =>
        if (matchChar('=')) makeToken(TokenType.TOKEN_CARET_EQUAL)
        else makeToken(TokenType.TOKEN_CARET)
      
      case '!' =>
        if (matchChar('=')) makeToken(TokenType.TOKEN_BANG_EQUAL)
        else makeToken(TokenType.TOKEN_BANG)
      
      case '=' =>
        if (matchChar('=')) makeToken(TokenType.TOKEN_EQUAL_EQUAL)
        else if (matchChar('>')) makeToken(TokenType.TOKEN_FAT_ARROW)
        else makeToken(TokenType.TOKEN_EQUAL)
      
      case '<' =>
        if (matchChar('<')) {
          if (matchChar('=')) makeToken(TokenType.TOKEN_LESS_LESS_EQUAL)
          else makeToken(TokenType.TOKEN_LESS_LESS)
        }
        else if (matchChar('=')) makeToken(TokenType.TOKEN_LESS_EQUAL)
        else makeToken(TokenType.TOKEN_LESS)
      
      case '>' =>
        if (matchChar('>')) {
          if (matchChar('>')) {
            if (matchChar('=')) makeToken(TokenType.TOKEN_GREATER_GREATER_GREATER_EQUAL)
            else makeToken(TokenType.TOKEN_GREATER_GREATER_GREATER)
          }
          else if (matchChar('=')) makeToken(TokenType.TOKEN_GREATER_GREATER_EQUAL)
          else makeToken(TokenType.TOKEN_GREATER_GREATER)
        }
        else if (matchChar('=')) makeToken(TokenType.TOKEN_GREATER_EQUAL)
        else makeToken(TokenType.TOKEN_GREATER)
      
      case '.' =>
        if (peek().isDigit) {
          // This is a float starting with decimal point
          scanNumber()
        }
        else if (matchChar('.')) {
          if (matchChar('<')) makeToken(TokenType.TOKEN_DOT_DOT_LESS)
          else makeToken(TokenType.TOKEN_DOT_DOT)
        }
        else makeToken(TokenType.TOKEN_DOT)
      
      // String literals
      case '"' => scanString('"')
      case '\'' => scanString('\'')
      
      // Template literals
      case '`' =>
        if (!inTemplate) {
          inTemplate = true
          makeToken(TokenType.TOKEN_TEMPLATE_START)
        } else {
          inTemplate = false
          makeToken(TokenType.TOKEN_TEMPLATE_END)
        }
      
      // Numbers
      case c if c.isDigit => scanNumber()
      
      // Identifiers and keywords
      case c if c.isLetter || c == '_' => scanIdentifier()
      
      case _ => errorToken(s"Unexpected character: '$c'")
    }
  }
  
  // Indentation handling
  private def countIndentation(): Int = {
    var spaces = 0
    var i = current
    
    while (i < source.length) {
      source.charAt(i) match {
        case ' ' => spaces += 1; i += 1
        case '\t' => spaces += 8; i += 1  // Treat tab as 8 spaces
        case '\n' => return -1  // Blank line
        case '\\' => 
          // Comment line - skip it
          while (i < source.length && source.charAt(i) != '\n') i += 1
          if (i < source.length) i += 1  // Skip the newline
          return -1
        case _ => return spaces
      }
    }
    
    spaces
  }
  
  private def handleIndentation(level: Int): Token = {
    val currentIndent = indentStack.last
    
    if (level > currentIndent) {
      // Indent
      indentStack += level
      return makeToken(TokenType.TOKEN_INDENT)
    } else if (level < currentIndent) {
      // Dedent - possibly multiple levels
      var dedentCount = 0
      while (indentStack.length > 1 && indentStack.last > level) {
        indentStack.remove(indentStack.length - 1)
        dedentCount += 1
      }
      
      if (indentStack.last != level) {
        return errorToken(s"Indentation error: inconsistent indentation level $level")
      }
      
      if (dedentCount > 0) {
        pendingDedents = dedentCount - 1
        return makeToken(TokenType.TOKEN_DEDENT)
      }
    }
    
    null  // Same indentation level
  }
  
  // String scanning
  private def scanString(quote: Char): Token = {
    val startLine = line
    val startColumn = column - 1
    val sb = new StringBuilder()
    
    while (peek() != quote && !isAtEnd()) {
      if (peek() == '\n') {
        // Allow multiline strings
        sb.append('\n')
        advance()
      } else if (peek() == '\\') {
        advance()
        if (!isAtEnd()) {
          advance() match {
            case 'n' => sb.append('\n')
            case 't' => sb.append('\t')
            case 'r' => sb.append('\r')
            case '\\' => sb.append('\\')
            case '"' => sb.append('"')
            case '\'' => sb.append('\'')
            case c => sb.append(c)  // Unknown escape, keep as-is
          }
        }
      } else {
        sb.append(advance())
      }
    }
    
    if (isAtEnd()) {
      return errorToken(s"Unterminated string starting at line $startLine, column $startColumn")
    }
    
    advance()  // Closing quote
    makeToken(TokenType.TOKEN_STRING, sb.toString())
  }
  
  // Number scanning
  private def scanNumber(): Token = {
    // Check for hex literal
    if (peek() == '0' && (peekNext() == 'x' || peekNext() == 'X')) {
      advance()  // Skip '0'
      advance()  // Skip 'x'
      return scanHexNumber()
    }
    
    // Scan integer part
    while (peek().isDigit) {
      advance()
    }
    
    var isFloat = false
    
    // Look for decimal part
    if (peek() == '.' && peekNext().isDigit) {
      isFloat = true
      advance()  // Skip '.'
      while (peek().isDigit) {
        advance()
      }
    }
    
    // Look for exponent
    if (peek() == 'e' || peek() == 'E') {
      isFloat = true
      advance()
      if (peek() == '+' || peek() == '-') {
        advance()
      }
      if (!peek().isDigit) {
        return errorToken("Invalid number: expected digits after exponent")
      }
      while (peek().isDigit) {
        advance()
      }
    }
    
    val numberStr = source.substring(tokenStart, current)
    
    if (isFloat) {
      try {
        val value = numberStr.toDouble
        makeToken(TokenType.TOKEN_FLOAT, value)
      } catch {
        case _: NumberFormatException =>
          errorToken(s"Invalid floating-point number: $numberStr")
      }
    } else {
      try {
        val value = numberStr.toInt
        makeToken(TokenType.TOKEN_INTEGER, value)
      } catch {
        case _: NumberFormatException =>
          // Try as Long for large integers
          try {
            val value = numberStr.toLong
            makeToken(TokenType.TOKEN_INTEGER, value)
          } catch {
            case _: NumberFormatException =>
              errorToken(s"Integer too large: $numberStr")
          }
      }
    }
  }
  
  private def scanHexNumber(): Token = {
    val hexStart = current
    
    while (isHexDigit(peek())) {
      advance()
    }
    
    if (current == hexStart) {
      return errorToken("Invalid hex number: no digits after 0x")
    }
    
    val hexStr = source.substring(hexStart, current)
    
    try {
      val value = java.lang.Long.parseLong(hexStr, 16)
      if (value <= Int.MaxValue) {
        makeToken(TokenType.TOKEN_INTEGER, value.toInt)
      } else {
        makeToken(TokenType.TOKEN_INTEGER, value)
      }
    } catch {
      case _: NumberFormatException =>
        errorToken(s"Invalid hex number: 0x$hexStr")
    }
  }
  
  private def isHexDigit(c: Char): Boolean = {
    c.isDigit || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
  }
  
  // Identifier scanning
  private def scanIdentifier(): Token = {
    while (peek().isLetterOrDigit || peek() == '_') {
      advance()
    }
    
    val text = source.substring(tokenStart, current)
    val tokenType = keywords.getOrElse(text, TokenType.TOKEN_IDENTIFIER)
    
    // Special handling for boolean literals
    tokenType match {
      case TokenType.TOKEN_TRUE => makeToken(TokenType.TOKEN_TRUE, true)
      case TokenType.TOKEN_FALSE => makeToken(TokenType.TOKEN_FALSE, false)
      case _ => makeToken(tokenType)
    }
  }
  
  // Template literal scanning
  private def scanTemplatePart(startChar: Char): Token = {
    if (startChar == '`') {
      inTemplate = false
      return makeToken(TokenType.TOKEN_TEMPLATE_END)
    }
    
    if (startChar == '$') {
      if (peek() == '{') {
        advance()  // Skip '{'
        templateBraceDepth = 1
        braceDepth += 1
        return makeToken(TokenType.TOKEN_TEMPLATE_EXPR_START)
      } else if (peek().isLetter || peek() == '_') {
        // Simple variable interpolation
        advance()
        while (peek().isLetterOrDigit || peek() == '_') {
          advance()
        }
        val varName = source.substring(tokenStart + 1, current)
        return makeToken(TokenType.TOKEN_TEMPLATE_SIMPLE_VAR, varName)
      }
    }
    
    // Regular template text
    val sb = new StringBuilder()
    if (startChar != '$') {
      sb.append(startChar)
    }
    
    while (!isAtEnd() && peek() != '`' && peek() != '$') {
      if (peek() == '\\') {
        advance()
        if (!isAtEnd()) {
          advance() match {
            case 'n' => sb.append('\n')
            case 't' => sb.append('\t')
            case 'r' => sb.append('\r')
            case '\\' => sb.append('\\')
            case '`' => sb.append('`')
            case '$' => sb.append('$')
            case c => sb.append(c)
          }
        }
      } else {
        sb.append(advance())
      }
    }
    
    makeToken(TokenType.TOKEN_TEMPLATE_TEXT, sb.toString())
  }
}