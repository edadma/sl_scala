package io.github.edadma.sl_scala
package parser

import io.github.edadma.sl_scala.lexer.{Lexer, Token, TokenType, SourceLocation}
import io.github.edadma.sl_scala.parser.ErrorCode._
import scala.collection.mutable.{ArrayBuffer, HashMap, HashSet}

// C-style recursive descent parser for Slate language
class Parser(lexer: Lexer, fileName: String = "<unknown>") {
  
  // Parser state (mutable for C compatibility)
  private var current = 0
  private val tokens = ArrayBuffer[Token]()
  private var hadError = false
  private var panicMode = false
  private val errorReporter = new ParseErrorReporter(fileName)
  
  // Precedence levels for binary operators
  private val precedence = HashMap[String, Int](
    // Assignment (lowest precedence)
    "=" -> 1, "+=" -> 1, "-=" -> 1, "*=" -> 1, "/=" -> 1, "//=" -> 1, "%=" -> 1, "**=" -> 1,
    "<<=" -> 1, ">>=" -> 1, ">>>=" -> 1, "&=" -> 1, "^=" -> 1, "|=" -> 1,
    "&&=" -> 1, "||=" -> 1, "??=" -> 1,
    
    // Null coalescing
    "??" -> 2,
    
    // Logical OR
    "||" -> 3, "or" -> 3,
    
    // Logical AND  
    "&&" -> 4, "and" -> 4,
    
    // Equality
    "==" -> 5, "!=" -> 5,
    
    // Relational
    "<" -> 6, "<=" -> 6, ">" -> 6, ">=" -> 6, "in" -> 6, "instanceof" -> 6,
    
    // Bitwise OR
    "|" -> 7,
    
    // Bitwise XOR
    "^" -> 8,
    
    // Bitwise AND
    "&" -> 9,
    
    // Shift
    "<<" -> 10, ">>" -> 10, ">>>" -> 10,
    
    // Range
    ".." -> 11, "..<" -> 11,
    
    // Additive
    "+" -> 12, "-" -> 12,
    
    // Multiplicative
    "*" -> 13, "/" -> 13, "//" -> 13, "%" -> 13, "mod" -> 13,
    
    // Power (highest precedence, right-associative)
    "**" -> 14
  )
  
  // Right-associative operators
  private val rightAssociative = HashSet[String]("**", "=", "+=", "-=", "*=", "/=", "//=", "%=", "**=",
    "<<=" ,">>=" ,">>>=", "&=", "^=", "|=", "&&=", "||=", "??=")
  
  // Initialize parser
  loadTokens()
  
  // ============================================================================
  // UTILITY METHODS
  // ============================================================================
  
  private def loadTokens(): Unit = {
    var token = lexer.nextToken()
    while (token.tokenType != TokenType.TOKEN_EOF) {
      tokens += token
      token = lexer.nextToken()
    }
    tokens += token  // Add EOF token
  }
  
  private def isAtEnd(): Boolean = current >= tokens.length || peek().tokenType == TokenType.TOKEN_EOF
  
  private def peek(): Token = {
    if (current < tokens.length) tokens(current) else tokens.last
  }
  
  private def previous(): Token = {
    if (current > 0) tokens(current - 1) else tokens(0)
  }
  
  private def advance(): Token = {
    if (!isAtEnd()) current += 1
    previous()
  }
  
  private def check(tokenType: TokenType): Boolean = {
    if (isAtEnd()) return false
    peek().tokenType == tokenType
  }
  
  private def matchToken(tokenTypes: TokenType*): Boolean = {
    tokenTypes.foreach { tokenType =>
      if (check(tokenType)) {
        advance()
        return true
      }
    }
    false
  }
  
  private def consume(tokenType: TokenType, message: String): ParseResult[Token] = {
    if (check(tokenType)) {
      return ParseResult(advance(), ERROR_NONE)
    }
    
    val error = ParseResult[Token](null, ERROR_EXPECTED_TOKEN, message, peek().location)
    errorReporter.reportExpectedToken(message, peek())
    error
  }
  
  // ============================================================================
  // ERROR HANDLING AND SYNCHRONIZATION
  // ============================================================================
  
  private def createParseError[T](error: ErrorCode, message: String, location: SourceLocation = null): ParseResult[T] = {
    hadError = true
    val loc = if (location != null) location else peek().location
    errorReporter.reportError(error, message, loc)
    ParseResult(null.asInstanceOf[T], error, message, loc)
  }
  
  private def synchronize(): Unit = {
    panicMode = false
    advance()
    
    while (!isAtEnd()) {
      if (previous().tokenType == TokenType.TOKEN_NEWLINE) return
      
      peek().tokenType match {
        case TokenType.TOKEN_VAR | TokenType.TOKEN_VAL | TokenType.TOKEN_DEF |
             TokenType.TOKEN_IF | TokenType.TOKEN_WHILE | TokenType.TOKEN_FOR |
             TokenType.TOKEN_LOOP | TokenType.TOKEN_MATCH | TokenType.TOKEN_IMPORT |
             TokenType.TOKEN_PACKAGE | TokenType.TOKEN_DATA | TokenType.TOKEN_RETURN |
             TokenType.TOKEN_BREAK | TokenType.TOKEN_CONTINUE => return
        case _ => advance()
      }
    }
  }
  
  // ============================================================================
  // PUBLIC API
  // ============================================================================
  
  def parseProgram(): ParseResult[Program] = {
    val statements = ArrayBuffer[ASTNode]()
    val startLocation = if (tokens.nonEmpty) tokens(0).location else SourceLocation(1, 1, 0, 0)
    
    while (!isAtEnd() && peek().tokenType != TokenType.TOKEN_EOF) {
      // Skip newlines at the top level
      if (matchToken(TokenType.TOKEN_NEWLINE)) {
        // Continue to next iteration
      } else {
        val stmtResult = parseStatement()
        if (stmtResult.isSuccess()) {
          statements += stmtResult.result
        } else {
          // Error recovery
          if (!panicMode) {
            panicMode = true
            synchronize()
          }
        }
      }
    }
    
    if (hadError) {
      createParseError(ERROR_INTERNAL, "Parse completed with errors", startLocation)
    } else {
      ParseResult(Program(statements, startLocation), ERROR_NONE)
    }
  }
  
  def hasErrors(): Boolean = hadError || errorReporter.hasErrors()
  
  def getErrors(): Array[String] = errorReporter.getErrors()
  
  // ============================================================================
  // STATEMENT PARSING
  // ============================================================================
  
  private def parseStatement(): ParseResult[ASTNode] = {
    if (check(TokenType.TOKEN_VAR)) {
      val result = parseVarDeclaration()
      return ParseResult(result.result.asInstanceOf[ASTNode], result.error, result.errorMessage, result.errorLocation)
    }
    if (check(TokenType.TOKEN_VAL)) {
      val result = parseValDeclaration()
      return ParseResult(result.result.asInstanceOf[ASTNode], result.error, result.errorMessage, result.errorLocation)
    }
    if (check(TokenType.TOKEN_DEF)) {
      val result = parseFunctionDeclaration()
      return ParseResult(result.result.asInstanceOf[ASTNode], result.error, result.errorMessage, result.errorLocation)
    }
    if (check(TokenType.TOKEN_IMPORT)) {
      val result = parseImportStatement()
      return ParseResult(result.result.asInstanceOf[ASTNode], result.error, result.errorMessage, result.errorLocation)
    }
    if (check(TokenType.TOKEN_PACKAGE)) {
      val result = parsePackageStatement()
      return ParseResult(result.result.asInstanceOf[ASTNode], result.error, result.errorMessage, result.errorLocation)
    }
    if (check(TokenType.TOKEN_DATA)) {
      val result = parseDataDeclaration()
      return ParseResult(result.result.asInstanceOf[ASTNode], result.error, result.errorMessage, result.errorLocation)
    }
    
    // Expression statement
    val exprResult = parseExpression()
    if (exprResult.isError()) return ParseResult(null, exprResult.error, exprResult.errorMessage, exprResult.errorLocation)
    
    ParseResult(ExpressionStatement(exprResult.result, exprResult.result.location), ERROR_NONE)
  }
  
  private def parseVarDeclaration(): ParseResult[VarDeclaration] = {
    val varToken = advance()  // consume 'var'
    
    val identResult = consume(TokenType.TOKEN_IDENTIFIER, "identifier")
    if (identResult.isError()) return createParseError(identResult.error, identResult.errorMessage)
    
    val name = identResult.result.lexeme
    var initializer: Expression = null
    
    if (matchToken(TokenType.TOKEN_EQUAL)) {
      val initResult = parseExpression()
      if (initResult.isError()) return createParseError(initResult.error, initResult.errorMessage)
      initializer = initResult.result
    }
    
    ParseResult(VarDeclaration(name, initializer, varToken.location), ERROR_NONE)
  }
  
  private def parseValDeclaration(): ParseResult[ValDeclaration] = {
    val valToken = advance()  // consume 'val'
    
    val identResult = consume(TokenType.TOKEN_IDENTIFIER, "identifier")
    if (identResult.isError()) return createParseError(identResult.error, identResult.errorMessage)
    
    val name = identResult.result.lexeme
    
    val equalsResult = consume(TokenType.TOKEN_EQUAL, "'='")
    if (equalsResult.isError()) return createParseError(equalsResult.error, equalsResult.errorMessage)
    
    val initResult = parseExpression()
    if (initResult.isError()) return createParseError(initResult.error, initResult.errorMessage)
    
    ParseResult(ValDeclaration(name, initResult.result, valToken.location), ERROR_NONE)
  }
  
  private def parseFunctionDeclaration(): ParseResult[FunctionDeclaration] = {
    val defToken = advance()  // consume 'def'
    
    val nameResult = consume(TokenType.TOKEN_IDENTIFIER, "function name")
    if (nameResult.isError()) return createParseError(nameResult.error, nameResult.errorMessage)
    
    val name = nameResult.result.lexeme
    
    val leftParenResult = consume(TokenType.TOKEN_LEFT_PAREN, "'('")
    if (leftParenResult.isError()) return createParseError(leftParenResult.error, leftParenResult.errorMessage)
    
    val parameters = ArrayBuffer[String]()
    if (!check(TokenType.TOKEN_RIGHT_PAREN)) {
      var continue = true
      while (continue) {
        val paramResult = consume(TokenType.TOKEN_IDENTIFIER, "parameter name")
        if (paramResult.isError()) return createParseError(paramResult.error, paramResult.errorMessage)
        parameters += paramResult.result.lexeme
        continue = matchToken(TokenType.TOKEN_COMMA)
      }
    }
    
    val rightParenResult = consume(TokenType.TOKEN_RIGHT_PAREN, "')'")
    if (rightParenResult.isError()) return createParseError(rightParenResult.error, rightParenResult.errorMessage)
    
    val equalsResult = consume(TokenType.TOKEN_EQUAL, "'='")
    if (equalsResult.isError()) return createParseError(equalsResult.error, equalsResult.errorMessage)
    
    val bodyResult = parseExpression()
    if (bodyResult.isError()) return createParseError(bodyResult.error, bodyResult.errorMessage)
    
    // Check for optional end marker
    var hasEndMarker = false
    if (check(TokenType.TOKEN_END)) {
      advance()  // consume 'end'
      val endNameResult = consume(TokenType.TOKEN_IDENTIFIER, s"function name '$name'")
      if (endNameResult.isSuccess() && endNameResult.result.lexeme == name) {
        hasEndMarker = true
      }
    }
    
    ParseResult(FunctionDeclaration(name, parameters, bodyResult.result, hasEndMarker, defToken.location), ERROR_NONE)
  }
  
  private def parseImportStatement(): ParseResult[ImportStatement] = {
    val importToken = advance()  // consume 'import'
    
    val modulePath = ArrayBuffer[String]()
    val moduleResult = consume(TokenType.TOKEN_IDENTIFIER, "module name")
    if (moduleResult.isError()) return createParseError(moduleResult.error, moduleResult.errorMessage)
    
    modulePath += moduleResult.result.lexeme
    
    while (matchToken(TokenType.TOKEN_DOT)) {
      if (check(TokenType.TOKEN_IDENTIFIER)) {
        modulePath += advance().lexeme
      } else if (check(TokenType.TOKEN_LEFT_BRACE)) {
        // Selective import: import module.{item1, item2}
        advance()  // consume '{'
        val items = ArrayBuffer[ImportItem]()
        
        var continue = true
        while (continue) {
          val itemResult = consume(TokenType.TOKEN_IDENTIFIER, "import item")
          if (itemResult.isError()) return createParseError(itemResult.error, itemResult.errorMessage)
          
          val itemName = itemResult.result.lexeme
          var alias: String = null
          
          if (matchToken(TokenType.TOKEN_FAT_ARROW)) {  // '=>'
            val aliasResult = consume(TokenType.TOKEN_IDENTIFIER, "alias name")
            if (aliasResult.isError()) return createParseError(aliasResult.error, aliasResult.errorMessage)
            alias = aliasResult.result.lexeme
          }
          
          items += ImportItem(itemName, alias, itemResult.result.location)
          continue = matchToken(TokenType.TOKEN_COMMA)
        }
        
        val rightBraceResult = consume(TokenType.TOKEN_RIGHT_BRACE, "'}'")
        if (rightBraceResult.isError()) return createParseError(rightBraceResult.error, rightBraceResult.errorMessage)
        
        return ParseResult(ImportStatement(modulePath, SelectiveImport, items, importToken.location), ERROR_NONE)
        
      } else if (check(TokenType.TOKEN_STAR) || check(TokenType.TOKEN_IDENTIFIER) && peek().lexeme == "_") {
        // Wildcard import: import module._
        advance()
        return ParseResult(ImportStatement(modulePath, WildcardImport, ArrayBuffer(), importToken.location), ERROR_NONE)
      }
    }
    
    // Namespace import: import module
    ParseResult(ImportStatement(modulePath, NamespaceImport, ArrayBuffer(), importToken.location), ERROR_NONE)
  }
  
  private def parsePackageStatement(): ParseResult[PackageStatement] = {
    val packageToken = advance()  // consume 'package'
    
    val packagePath = ArrayBuffer[String]()
    val nameResult = consume(TokenType.TOKEN_IDENTIFIER, "package name")
    if (nameResult.isError()) return createParseError(nameResult.error, nameResult.errorMessage)
    
    packagePath += nameResult.result.lexeme
    
    while (matchToken(TokenType.TOKEN_DOT)) {
      val partResult = consume(TokenType.TOKEN_IDENTIFIER, "package part")
      if (partResult.isError()) return createParseError(partResult.error, partResult.errorMessage)
      packagePath += partResult.result.lexeme
    }
    
    ParseResult(PackageStatement(packagePath, packageToken.location), ERROR_NONE)
  }
  
  private def parseDataDeclaration(): ParseResult[DataDeclaration] = {
    var isPrivate = false
    var startLocation: SourceLocation = null
    
    if (check(TokenType.TOKEN_PRIVATE)) {
      isPrivate = true
      startLocation = advance().location
    }
    
    val dataToken = advance()  // consume 'data'
    if (startLocation == null) startLocation = dataToken.location
    
    val nameResult = consume(TokenType.TOKEN_IDENTIFIER, "data type name")
    if (nameResult.isError()) return createParseError(nameResult.error, nameResult.errorMessage)
    
    val name = nameResult.result.lexeme
    val parameters = ArrayBuffer[String]()
    
    if (matchToken(TokenType.TOKEN_LEFT_PAREN)) {
      if (!check(TokenType.TOKEN_RIGHT_PAREN)) {
        var continue = true
        while (continue) {
          val paramResult = consume(TokenType.TOKEN_IDENTIFIER, "parameter name")
          if (paramResult.isError()) return createParseError(paramResult.error, paramResult.errorMessage)
          parameters += paramResult.result.lexeme
          continue = matchToken(TokenType.TOKEN_COMMA)
        }
      }
      
      val rightParenResult = consume(TokenType.TOKEN_RIGHT_PAREN, "')'")
      if (rightParenResult.isError()) return createParseError(rightParenResult.error, rightParenResult.errorMessage)
    }
    
    val constructors = ArrayBuffer[DataConstructor]()
    val methods = ArrayBuffer[FunctionDeclaration]()
    
    // Check for indented block with constructors and methods
    if (matchToken(TokenType.TOKEN_NEWLINE, TokenType.TOKEN_INDENT)) {
      while (!check(TokenType.TOKEN_DEDENT) && !isAtEnd()) {
        if (matchToken(TokenType.TOKEN_CASE)) {
          val ctorNameResult = consume(TokenType.TOKEN_IDENTIFIER, "constructor name")
          if (ctorNameResult.isError()) return createParseError(ctorNameResult.error, ctorNameResult.errorMessage)
          
          val ctorName = ctorNameResult.result.lexeme
          val ctorParams = ArrayBuffer[String]()
          
          if (matchToken(TokenType.TOKEN_LEFT_PAREN)) {
            if (!check(TokenType.TOKEN_RIGHT_PAREN)) {
              var continue = true
              while (continue) {
                val paramResult = consume(TokenType.TOKEN_IDENTIFIER, "constructor parameter")
                if (paramResult.isError()) return createParseError(paramResult.error, paramResult.errorMessage)
                ctorParams += paramResult.result.lexeme
                continue = matchToken(TokenType.TOKEN_COMMA)
              }
            }
            
            val rightParenResult = consume(TokenType.TOKEN_RIGHT_PAREN, "')'")
            if (rightParenResult.isError()) return createParseError(rightParenResult.error, rightParenResult.errorMessage)
          }
          
          constructors += DataConstructor(ctorName, ctorParams, ctorNameResult.result.location)
          
        } else if (check(TokenType.TOKEN_DEF)) {
          val methodResult = parseFunctionDeclaration()
          if (methodResult.isError()) return createParseError(methodResult.error, methodResult.errorMessage)
          methods += methodResult.result
          
        } else {
          advance()  // skip unknown token
        }
        
        matchToken(TokenType.TOKEN_NEWLINE)  // optional newlines
      }
      
      if (check(TokenType.TOKEN_DEDENT)) {
        advance()  // consume DEDENT
      }
    }
    
    // Check for optional end marker
    var hasEndMarker = false
    if (check(TokenType.TOKEN_END)) {
      advance()  // consume 'end'
      val endNameResult = consume(TokenType.TOKEN_IDENTIFIER, s"data type name '$name'")
      if (endNameResult.isSuccess() && endNameResult.result.lexeme == name) {
        hasEndMarker = true
      }
    }
    
    ParseResult(DataDeclaration(name, parameters, constructors, methods, isPrivate, hasEndMarker, startLocation), ERROR_NONE)
  }
  
  // ============================================================================
  // EXPRESSION PARSING (using precedence climbing)
  // ============================================================================
  
  private def parseExpression(): ParseResult[Expression] = {
    parseAssignment()
  }
  
  private def parseAssignment(): ParseResult[Expression] = {
    val exprResult = parseTernary()
    if (exprResult.isError()) return exprResult
    
    var expr = exprResult.result
    
    val token = peek()
    if (precedence.contains(token.lexeme) && precedence(token.lexeme) == 1) {  // Assignment operators
      val operator = advance()
      
      val valueResult = parseAssignment()  // Right associative
      if (valueResult.isError()) return createParseError(valueResult.error, valueResult.errorMessage)
      
      expr = AssignmentExpression(expr, operator.lexeme, valueResult.result, expr.location)
    }
    
    ParseResult(expr, ERROR_NONE)
  }
  
  private def parseTernary(): ParseResult[Expression] = {
    val exprResult = parseLogicalOr()
    if (exprResult.isError()) return exprResult
    
    var expr = exprResult.result
    
    if (matchToken(TokenType.TOKEN_QUESTION)) {
      val thenResult = parseAssignment()
      if (thenResult.isError()) return createParseError(thenResult.error, thenResult.errorMessage)
      
      val colonResult = consume(TokenType.TOKEN_COLON, "':'")
      if (colonResult.isError()) return createParseError(colonResult.error, colonResult.errorMessage)
      
      val elseResult = parseAssignment()
      if (elseResult.isError()) return createParseError(elseResult.error, elseResult.errorMessage)
      
      expr = TernaryExpression(expr, thenResult.result, elseResult.result, expr.location)
    }
    
    ParseResult(expr, ERROR_NONE)
  }
  
  private def parseLogicalOr(): ParseResult[Expression] = {
    parseBinaryExpression(2)  // Start with null coalescing precedence
  }
  
  private def parseBinaryExpression(minPrec: Int): ParseResult[Expression] = {
    val leftResult = parseUnary()
    if (leftResult.isError()) return leftResult
    
    var left = leftResult.result
    
    while (!isAtEnd()) {
      val token = peek()
      val tokenPrec = precedence.getOrElse(token.lexeme, 0)
      
      if (tokenPrec < minPrec) {
        return ParseResult(left, ERROR_NONE)
      }
      
      val operator = advance()
      val nextMinPrec = if (rightAssociative.contains(operator.lexeme)) tokenPrec else tokenPrec + 1
      
      val rightResult = parseBinaryExpression(nextMinPrec)
      if (rightResult.isError()) return createParseError(rightResult.error, rightResult.errorMessage)
      
      left = BinaryExpression(operator.lexeme, left, rightResult.result, left.location)
    }
    
    ParseResult(left, ERROR_NONE)
  }
  
  private def parseUnary(): ParseResult[Expression] = {
    if (matchToken(TokenType.TOKEN_MINUS, TokenType.TOKEN_PLUS, TokenType.TOKEN_BANG, TokenType.TOKEN_NOT, 
              TokenType.TOKEN_TILDE, TokenType.TOKEN_PLUS_PLUS, TokenType.TOKEN_MINUS_MINUS)) {
      val operator = previous()
      val operandResult = parseUnary()
      if (operandResult.isError()) return createParseError(operandResult.error, operandResult.errorMessage)
      
      return ParseResult(UnaryExpression(operator.lexeme, operandResult.result, true, operator.location), ERROR_NONE)
    }
    
    parsePostfix()
  }
  
  private def parsePostfix(): ParseResult[Expression] = {
    val exprResult = parsePrimary()
    if (exprResult.isError()) return exprResult
    
    var expr = exprResult.result
    
    while (!isAtEnd()) {
      if (matchToken(TokenType.TOKEN_LEFT_PAREN)) {
        // Function call: expr(args)
        val args = ArrayBuffer[Expression]()
        
        if (!check(TokenType.TOKEN_RIGHT_PAREN)) {
          var continue = true
          while (continue) {
            val argResult = parseExpression()
            if (argResult.isError()) return createParseError(argResult.error, argResult.errorMessage)
            args += argResult.result
            continue = matchToken(TokenType.TOKEN_COMMA)
          }
        }
        
        val rightParenResult = consume(TokenType.TOKEN_RIGHT_PAREN, "')'")
        if (rightParenResult.isError()) return createParseError(rightParenResult.error, rightParenResult.errorMessage)
        
        expr = FunctionCallExpression(expr, args, expr.location)
        
      } else if (matchToken(TokenType.TOKEN_DOT)) {
        // Property access: expr.property
        val propertyResult = consume(TokenType.TOKEN_IDENTIFIER, "property name")
        if (propertyResult.isError()) return createParseError(propertyResult.error, propertyResult.errorMessage)
        
        expr = PropertyAccessExpression(expr, propertyResult.result.lexeme, false, expr.location)
        
      } else if (matchToken(TokenType.TOKEN_QUESTION_DOT)) {
        // Optional chaining: expr?.property
        val propertyResult = consume(TokenType.TOKEN_IDENTIFIER, "property name")
        if (propertyResult.isError()) return createParseError(propertyResult.error, propertyResult.errorMessage)
        
        expr = PropertyAccessExpression(expr, propertyResult.result.lexeme, true, expr.location)
        
      } else if (matchToken(TokenType.TOKEN_PLUS_PLUS, TokenType.TOKEN_MINUS_MINUS)) {
        // Postfix increment/decrement: expr++, expr--
        val operator = previous()
        expr = UnaryExpression(operator.lexeme, expr, false, expr.location)
        
      } else {
        return ParseResult(expr, ERROR_NONE)
      }
    }
    
    ParseResult(expr, ERROR_NONE)
  }
  
  private def parsePrimary(): ParseResult[Expression] = {
    // Literals
    if (matchToken(TokenType.TOKEN_INTEGER)) {
      val token = previous()
      val value = token.value.asInstanceOf[Number].longValue()
      return ParseResult(IntegerLiteral(value, token.location), ERROR_NONE)
    }
    
    if (matchToken(TokenType.TOKEN_FLOAT)) {
      val token = previous()
      val value = token.value.asInstanceOf[Double]
      return ParseResult(FloatLiteral(value, token.location), ERROR_NONE)
    }
    
    if (matchToken(TokenType.TOKEN_STRING)) {
      val token = previous()
      val value = token.value.asInstanceOf[String]
      return ParseResult(StringLiteral(value, token.location), ERROR_NONE)
    }
    
    if (matchToken(TokenType.TOKEN_TRUE)) {
      val token = previous()
      return ParseResult(BooleanLiteral(true, token.location), ERROR_NONE)
    }
    
    if (matchToken(TokenType.TOKEN_FALSE)) {
      val token = previous()
      return ParseResult(BooleanLiteral(false, token.location), ERROR_NONE)
    }
    
    if (matchToken(TokenType.TOKEN_NULL)) {
      val token = previous()
      return ParseResult(NullLiteral(token.location), ERROR_NONE)
    }
    
    if (matchToken(TokenType.TOKEN_UNDEFINED)) {
      val token = previous()
      return ParseResult(UndefinedLiteral(token.location), ERROR_NONE)
    }
    
    if (matchToken(TokenType.TOKEN_NAN)) {
      val token = previous()
      return ParseResult(NaNLiteral(token.location), ERROR_NONE)
    }
    
    if (matchToken(TokenType.TOKEN_INFINITY)) {
      val token = previous()
      return ParseResult(InfinityLiteral(token.location), ERROR_NONE)
    }
    
    // Identifier
    if (matchToken(TokenType.TOKEN_IDENTIFIER)) {
      val token = previous()
      return ParseResult(Identifier(token.lexeme, token.location), ERROR_NONE)
    }
    
    // Parenthesized expression
    if (matchToken(TokenType.TOKEN_LEFT_PAREN)) {
      val exprResult = parseExpression()
      if (exprResult.isError()) return createParseError(exprResult.error, exprResult.errorMessage)
      
      val rightParenResult = consume(TokenType.TOKEN_RIGHT_PAREN, "')'")
      if (rightParenResult.isError()) return createParseError(rightParenResult.error, rightParenResult.errorMessage)
      
      return ParseResult(exprResult.result, ERROR_NONE)
    }
    
    // Array literals
    if (matchToken(TokenType.TOKEN_LEFT_BRACKET)) {
      return parseArrayLiteral()
    }
    
    // Object literals
    if (matchToken(TokenType.TOKEN_LEFT_BRACE)) {
      return parseObjectLiteral()
    }
    
    // Template literals
    if (matchToken(TokenType.TOKEN_TEMPLATE_START)) {
      return parseTemplateLiteral()
    }
    
    // Indented blocks
    if (check(TokenType.TOKEN_NEWLINE) && tokens.length > current + 1 && tokens(current + 1).tokenType == TokenType.TOKEN_INDENT) {
      return parseBlockExpression()
    }
    
    // Control flow expressions
    if (check(TokenType.TOKEN_IF)) return parseIfExpression()
    if (check(TokenType.TOKEN_WHILE)) return parseWhileExpression()
    if (check(TokenType.TOKEN_DO)) return parseDoWhileExpression()  
    if (check(TokenType.TOKEN_FOR)) return parseForExpression()
    if (check(TokenType.TOKEN_LOOP)) return parseLoopExpression()
    if (check(TokenType.TOKEN_MATCH)) return parseMatchExpression()
    if (check(TokenType.TOKEN_BREAK)) return parseBreakExpression()
    if (check(TokenType.TOKEN_CONTINUE)) return parseContinueExpression()
    if (check(TokenType.TOKEN_RETURN)) return parseReturnExpression()
    
    createParseError(ERROR_EXPECTED_EXPRESSION, s"Expected expression, got '${peek().lexeme}'")
  }
  
  // ============================================================================
  // COMPLEX EXPRESSION PARSING (stub implementations for now)
  // ============================================================================
  
  private def parseArrayLiteral(): ParseResult[Expression] = {
    val startLocation = previous().location
    val elements = ArrayBuffer[Expression]()
    
    if (!check(TokenType.TOKEN_RIGHT_BRACKET)) {
      var continue = true
      while (continue) {
        val elemResult = parseExpression()
        if (elemResult.isError()) return createParseError(elemResult.error, elemResult.errorMessage)
        elements += elemResult.result
        continue = matchToken(TokenType.TOKEN_COMMA)
      }
    }
    
    val rightBracketResult = consume(TokenType.TOKEN_RIGHT_BRACKET, "']'")
    if (rightBracketResult.isError()) return createParseError(rightBracketResult.error, rightBracketResult.errorMessage)
    
    ParseResult(ArrayLiteralExpression(elements, startLocation), ERROR_NONE)
  }
  
  private def parseObjectLiteral(): ParseResult[Expression] = {
    val startLocation = previous().location
    val properties = ArrayBuffer[ObjectProperty]()
    
    if (!check(TokenType.TOKEN_RIGHT_BRACE)) {
      var continue = true
      while (continue) {
        val keyResult = consume(TokenType.TOKEN_IDENTIFIER, "property key")
        if (keyResult.isError()) return createParseError(keyResult.error, keyResult.errorMessage)
        
        val colonResult = consume(TokenType.TOKEN_COLON, "':'")
        if (colonResult.isError()) return createParseError(colonResult.error, colonResult.errorMessage)
        
        val valueResult = parseExpression()
        if (valueResult.isError()) return createParseError(valueResult.error, valueResult.errorMessage)
        
        properties += ObjectProperty(keyResult.result.lexeme, valueResult.result, keyResult.result.location)
        continue = matchToken(TokenType.TOKEN_COMMA)
      }
    }
    
    val rightBraceResult = consume(TokenType.TOKEN_RIGHT_BRACE, "'}'")
    if (rightBraceResult.isError()) return createParseError(rightBraceResult.error, rightBraceResult.errorMessage)
    
    ParseResult(ObjectLiteralExpression(properties, startLocation), ERROR_NONE)
  }
  
  private def parseTemplateLiteral(): ParseResult[Expression] = {
    val startLocation = previous().location
    val parts = ArrayBuffer[TemplatePart]()
    
    while (!check(TokenType.TOKEN_TEMPLATE_END) && !isAtEnd()) {
      if (matchToken(TokenType.TOKEN_TEMPLATE_TEXT)) {
        val token = previous()
        val text = token.value.asInstanceOf[String]
        parts += TemplateTextPart(text, token.location)
      } else if (matchToken(TokenType.TOKEN_TEMPLATE_SIMPLE_VAR)) {
        val token = previous()
        val name = token.value.asInstanceOf[String]
        parts += TemplateVariablePart(name, token.location)
      } else if (matchToken(TokenType.TOKEN_TEMPLATE_EXPR_START)) {
        val exprResult = parseExpression()
        if (exprResult.isError()) return createParseError(exprResult.error, exprResult.errorMessage)
        
        val endResult = consume(TokenType.TOKEN_TEMPLATE_EXPR_END, "'}'")
        if (endResult.isError()) return createParseError(endResult.error, endResult.errorMessage)
        
        parts += TemplateExpressionPart(exprResult.result, exprResult.result.location)
      } else {
        advance()  // Skip unknown token
      }
    }
    
    val endResult = consume(TokenType.TOKEN_TEMPLATE_END, "'`'")
    if (endResult.isError()) return createParseError(endResult.error, endResult.errorMessage)
    
    ParseResult(TemplateLiteralExpression(parts, startLocation), ERROR_NONE)
  }
  
  private def parseBlockExpression(): ParseResult[Expression] = {
    val newlineResult = consume(TokenType.TOKEN_NEWLINE, "newline")
    if (newlineResult.isError()) return createParseError(newlineResult.error, newlineResult.errorMessage)
    
    val indentResult = consume(TokenType.TOKEN_INDENT, "indentation")
    if (indentResult.isError()) return createParseError(indentResult.error, indentResult.errorMessage)
    
    val startLocation = indentResult.result.location
    val statements = ArrayBuffer[ASTNode]()
    
    while (!check(TokenType.TOKEN_DEDENT) && !isAtEnd()) {
      if (matchToken(TokenType.TOKEN_NEWLINE)) {
        // Skip empty lines
      } else {
        val stmtResult = parseStatement()
        if (stmtResult.isError()) return createParseError(stmtResult.error, stmtResult.errorMessage)
        statements += stmtResult.result
      }
    }
    
    val dedentResult = consume(TokenType.TOKEN_DEDENT, "dedent")
    if (dedentResult.isError()) return createParseError(dedentResult.error, dedentResult.errorMessage)
    
    ParseResult(BlockExpression(statements, startLocation), ERROR_NONE)
  }
  
  // Control flow expressions (placeholder implementations)
  private def parseIfExpression(): ParseResult[Expression] = {
    createParseError(ERROR_EXPECTED_EXPRESSION, "If expression parsing not implemented yet")
  }
  
  private def parseWhileExpression(): ParseResult[Expression] = {
    createParseError(ERROR_EXPECTED_EXPRESSION, "While expression parsing not implemented yet")
  }
  
  private def parseDoWhileExpression(): ParseResult[Expression] = {
    createParseError(ERROR_EXPECTED_EXPRESSION, "Do-while expression parsing not implemented yet")
  }
  
  private def parseForExpression(): ParseResult[Expression] = {
    createParseError(ERROR_EXPECTED_EXPRESSION, "For expression parsing not implemented yet")
  }
  
  private def parseLoopExpression(): ParseResult[Expression] = {
    createParseError(ERROR_EXPECTED_EXPRESSION, "Loop expression parsing not implemented yet")
  }
  
  private def parseMatchExpression(): ParseResult[Expression] = {
    createParseError(ERROR_EXPECTED_EXPRESSION, "Match expression parsing not implemented yet")
  }
  
  private def parseBreakExpression(): ParseResult[Expression] = {
    val token = advance()  // consume 'break'
    ParseResult(BreakExpression(token.location), ERROR_NONE)
  }
  
  private def parseContinueExpression(): ParseResult[Expression] = {
    val token = advance()  // consume 'continue'
    ParseResult(ContinueExpression(token.location), ERROR_NONE)
  }
  
  private def parseReturnExpression(): ParseResult[Expression] = {
    val token = advance()  // consume 'return'
    var value: Expression = null
    
    // Return expression is optional
    if (!check(TokenType.TOKEN_NEWLINE) && !check(TokenType.TOKEN_DEDENT) && !isAtEnd()) {
      val valueResult = parseExpression()
      if (valueResult.isError()) return createParseError(valueResult.error, valueResult.errorMessage)
      value = valueResult.result
    }
    
    ParseResult(ReturnExpression(value, token.location), ERROR_NONE)
  }
}