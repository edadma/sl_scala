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
    tokenTypes.exists { tokenType =>
      if (check(tokenType)) {
        advance()
        true
      } else {
        false
      }
    }
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
    if (check(TokenType.TOKEN_DATA) || check(TokenType.TOKEN_PRIVATE)) {
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
    
    val patternResult = parsePattern()
    if (patternResult.isError()) return createParseError(patternResult.error, patternResult.errorMessage)
    
    var initializer: Expression = null
    
    if (matchToken(TokenType.TOKEN_EQUAL)) {
      val initResult = parseExpression()
      if (initResult.isError()) return createParseError(initResult.error, initResult.errorMessage)
      initializer = initResult.result
    }
    
    ParseResult(VarDeclaration(patternResult.result, initializer, varToken.location), ERROR_NONE)
  }
  
  private def parseValDeclaration(): ParseResult[ValDeclaration] = {
    val valToken = advance()  // consume 'val'
    
    val patternResult = parsePattern()
    if (patternResult.isError()) return createParseError(patternResult.error, patternResult.errorMessage)
    
    val equalsResult = consume(TokenType.TOKEN_EQUAL, "'='")
    if (equalsResult.isError()) return createParseError(equalsResult.error, equalsResult.errorMessage)
    
    val initResult = parseExpression()
    if (initResult.isError()) return createParseError(initResult.error, initResult.errorMessage)
    
    ParseResult(ValDeclaration(patternResult.result, initResult.result, valToken.location), ERROR_NONE)
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
    
    val path = ArrayBuffer[String]()
    val pathResult = consume(TokenType.TOKEN_IDENTIFIER, "module name")
    if (pathResult.isError()) return createParseError(pathResult.error, pathResult.errorMessage)
    
    path += pathResult.result.lexeme
    
    // Parse the rest of the path
    while (matchToken(TokenType.TOKEN_DOT)) {
      if (check(TokenType.TOKEN_IDENTIFIER)) {
        val ident = advance().lexeme
        if (ident == "_") {
          // Wildcard: import module._
          return ParseResult(ImportStatement(path, WildcardSpec, importToken.location), ERROR_NONE)
        } else {
          path += ident
        }
      } else if (check(TokenType.TOKEN_LEFT_BRACE)) {
        // Selective: import module.{item1, item2}
        advance()  // consume '{'
        val items = ArrayBuffer[ImportItem]()
        
        if (!check(TokenType.TOKEN_RIGHT_BRACE)) {
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
        }
        
        val rightBraceResult = consume(TokenType.TOKEN_RIGHT_BRACE, "'}'")
        if (rightBraceResult.isError()) return createParseError(rightBraceResult.error, rightBraceResult.errorMessage)
        
        return ParseResult(ImportStatement(path, SelectiveSpec(items), importToken.location), ERROR_NONE)
        
      } else if (check(TokenType.TOKEN_STAR)) {
        // Wildcard: import module.*
        advance()
        return ParseResult(ImportStatement(path, WildcardSpec, importToken.location), ERROR_NONE)
      } else {
        // Error: expected identifier, '{', or '*' after '.'
        return createParseError(ERROR_EXPECTED_IDENTIFIER, "Expected identifier, '{', or '*' after '.'")
      }
    }
    
    // Simple import: just the path with no special suffix
    ParseResult(ImportStatement(path, SimpleSpec, importToken.location), ERROR_NONE)
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
    val constructors = ArrayBuffer[DataConstructor]()
    val methods = ArrayBuffer[FunctionDeclaration]()
    
    // Check for indented block with constructors and methods
    if (matchToken(TokenType.TOKEN_NEWLINE, TokenType.TOKEN_INDENT)) {
      while (!check(TokenType.TOKEN_DEDENT) && !isAtEnd()) {
        if (matchToken(TokenType.TOKEN_CASE)) {
          // Constructor with 'case' keyword
          val ctorNameResult = consume(TokenType.TOKEN_IDENTIFIER, "constructor name")
          if (ctorNameResult.isError()) return createParseError(ctorNameResult.error, ctorNameResult.errorMessage)
          
          val ctorName = ctorNameResult.result.lexeme
          val ctorParams = ArrayBuffer[String]()
          var isSingleton = true
          
          if (matchToken(TokenType.TOKEN_LEFT_PAREN)) {
            // Has parentheses - either empty () or with parameters
            isSingleton = false
            
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
          // If no parentheses, it's a singleton (isSingleton remains true)
          
          constructors += DataConstructor(ctorName, ctorParams, isSingleton, ctorNameResult.result.location)
          
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
    
    ParseResult(DataDeclaration(name, constructors, methods, isPrivate, hasEndMarker, startLocation), ERROR_NONE)
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
              TokenType.TOKEN_TILDE, TokenType.TOKEN_PLUS_PLUS, TokenType.TOKEN_MINUS_MINUS, TokenType.TOKEN_TYPEOF)) {
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
    if (check(TokenType.TOKEN_LET)) { advance(); return parseLetExpression() }
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
    val ifToken = advance()  // consume 'if'
    
    // Parse condition
    val conditionResult = parseExpression()
    if (conditionResult.isError()) return createParseError(conditionResult.error, conditionResult.errorMessage)
    
    // Optional 'then'
    matchToken(TokenType.TOKEN_THEN)
    
    // Parse then branch (either expression or indented block)
    val thenResult = if (check(TokenType.TOKEN_NEWLINE) && 
                           tokens.length > current + 1 && 
                           tokens(current + 1).tokenType == TokenType.TOKEN_INDENT) {
      parseBlockExpression()
    } else {
      parseExpression()
    }
    if (thenResult.isError()) return createParseError(thenResult.error, thenResult.errorMessage)
    
    val elifBranches = ArrayBuffer[ElifBranch]()
    var elseBranch: Expression = null
    var hasEndMarker = false
    
    // Parse elif branches
    // Check for elif on the same line or next line
    var continueElif = true
    while (continueElif && ((check(TokenType.TOKEN_ELIF)) || 
           (check(TokenType.TOKEN_NEWLINE) && 
            tokens.length > current + 1 && 
            tokens(current + 1).tokenType == TokenType.TOKEN_ELIF))) {
      
      // Skip newline if present
      if (check(TokenType.TOKEN_NEWLINE)) {
        advance()
      }
      
      if (!matchToken(TokenType.TOKEN_ELIF)) {
        continueElif = false
      } else {
        val elifConditionResult = parseExpression()
        if (elifConditionResult.isError()) return createParseError(elifConditionResult.error, elifConditionResult.errorMessage)
        
        matchToken(TokenType.TOKEN_THEN)  // optional 'then'
        
        val elifBodyResult = if (check(TokenType.TOKEN_NEWLINE) && 
                                   tokens.length > current + 1 && 
                                   tokens(current + 1).tokenType == TokenType.TOKEN_INDENT) {
          parseBlockExpression()
        } else {
          parseExpression()
        }
        if (elifBodyResult.isError()) return createParseError(elifBodyResult.error, elifBodyResult.errorMessage)
        
        elifBranches += ElifBranch(elifConditionResult.result, elifBodyResult.result, elifConditionResult.result.location)
      }
    }
    
    // Parse else branch
    // Check for else on the same line or next line
    if (check(TokenType.TOKEN_ELSE) || 
        (check(TokenType.TOKEN_NEWLINE) && 
         tokens.length > current + 1 && 
         tokens(current + 1).tokenType == TokenType.TOKEN_ELSE)) {
      
      // Skip newline if present
      if (check(TokenType.TOKEN_NEWLINE)) {
        advance()
      }
      
      if (matchToken(TokenType.TOKEN_ELSE)) {
        val elseResult = if (check(TokenType.TOKEN_NEWLINE) && 
                              tokens.length > current + 1 && 
                              tokens(current + 1).tokenType == TokenType.TOKEN_INDENT) {
          parseBlockExpression()
        } else {
          parseExpression()
        }
        if (elseResult.isError()) return createParseError(elseResult.error, elseResult.errorMessage)
        elseBranch = elseResult.result
      }
    }
    
    // Check for end marker
    if (matchToken(TokenType.TOKEN_END)) {
      if (matchToken(TokenType.TOKEN_IF)) {
        hasEndMarker = true
      } else {
        // Put back the 'end' token if not followed by 'if'
        current -= 1
      }
    }
    
    ParseResult(IfExpression(conditionResult.result, thenResult.result, elifBranches, elseBranch, hasEndMarker, ifToken.location), ERROR_NONE)
  }
  
  private def parseWhileExpression(): ParseResult[Expression] = {
    val whileToken = advance()  // consume 'while'
    
    // Parse condition
    val conditionResult = parseExpression()
    if (conditionResult.isError()) return createParseError(conditionResult.error, conditionResult.errorMessage)
    
    // Optional 'do'
    matchToken(TokenType.TOKEN_DO)
    
    // Parse body (either expression or indented block)
    val bodyResult = if (check(TokenType.TOKEN_NEWLINE) && 
                          tokens.length > current + 1 && 
                          tokens(current + 1).tokenType == TokenType.TOKEN_INDENT) {
      parseBlockExpression()
    } else {
      parseExpression()
    }
    if (bodyResult.isError()) return createParseError(bodyResult.error, bodyResult.errorMessage)
    
    // Check for end marker
    var hasEndMarker = false
    if (matchToken(TokenType.TOKEN_END)) {
      if (matchToken(TokenType.TOKEN_WHILE)) {
        hasEndMarker = true
      } else {
        // Put back the 'end' token if not followed by 'while'
        current -= 1
      }
    }
    
    ParseResult(WhileExpression(conditionResult.result, bodyResult.result, hasEndMarker, whileToken.location), ERROR_NONE)
  }
  
  private def parseDoWhileExpression(): ParseResult[Expression] = {
    val doToken = advance()  // consume 'do'
    
    // Parse body
    val bodyResult = parseExpression()
    if (bodyResult.isError()) return createParseError(bodyResult.error, bodyResult.errorMessage)
    
    // Expect 'while'
    val whileResult = consume(TokenType.TOKEN_WHILE, "'while'")
    if (whileResult.isError()) return createParseError(whileResult.error, whileResult.errorMessage)
    
    // Parse condition
    val conditionResult = parseExpression()
    if (conditionResult.isError()) return createParseError(conditionResult.error, conditionResult.errorMessage)
    
    ParseResult(DoWhileExpression(bodyResult.result, conditionResult.result, doToken.location), ERROR_NONE)
  }
  
  private def parseForExpression(): ParseResult[Expression] = {
    val forToken = advance()  // consume 'for'
    
    // Parse init (can be var declaration or expression, or empty)
    var init: ASTNode = null
    if (!check(TokenType.TOKEN_SEMICOLON)) {
      if (check(TokenType.TOKEN_VAR)) {
        val initResult = parseVarDeclaration()
        if (initResult.isError()) return createParseError(initResult.error, initResult.errorMessage)
        init = initResult.result
      } else {
        val initResult = parseExpression()
        if (initResult.isError()) return createParseError(initResult.error, initResult.errorMessage)
        init = ExpressionStatement(initResult.result, initResult.result.location)
      }
    }
    
    // Expect semicolon
    val semicolon1Result = consume(TokenType.TOKEN_SEMICOLON, "';'")
    if (semicolon1Result.isError()) return createParseError(semicolon1Result.error, semicolon1Result.errorMessage)
    
    // Parse condition (optional)
    var condition: Expression = null
    if (!check(TokenType.TOKEN_SEMICOLON)) {
      val conditionResult = parseExpression()
      if (conditionResult.isError()) return createParseError(conditionResult.error, conditionResult.errorMessage)
      condition = conditionResult.result
    }
    
    // Expect semicolon
    val semicolon2Result = consume(TokenType.TOKEN_SEMICOLON, "';'")
    if (semicolon2Result.isError()) return createParseError(semicolon2Result.error, semicolon2Result.errorMessage)
    
    // Parse update (optional)
    var update: Expression = null
    if (!check(TokenType.TOKEN_DO) && !check(TokenType.TOKEN_NEWLINE)) {
      val updateResult = parseExpression()
      if (updateResult.isError()) return createParseError(updateResult.error, updateResult.errorMessage)
      update = updateResult.result
    }
    
    // Optional 'do'
    matchToken(TokenType.TOKEN_DO)
    
    // Parse body (either expression or indented block)
    val bodyResult = if (check(TokenType.TOKEN_NEWLINE) && 
                          tokens.length > current + 1 && 
                          tokens(current + 1).tokenType == TokenType.TOKEN_INDENT) {
      parseBlockExpression()
    } else {
      parseExpression()
    }
    if (bodyResult.isError()) return createParseError(bodyResult.error, bodyResult.errorMessage)
    
    // Check for end marker
    var hasEndMarker = false
    if (matchToken(TokenType.TOKEN_END)) {
      if (matchToken(TokenType.TOKEN_FOR)) {
        hasEndMarker = true
      } else {
        // Put back the 'end' token if not followed by 'for'
        current -= 1
      }
    }
    
    ParseResult(ForExpression(init, condition, update, bodyResult.result, hasEndMarker, forToken.location), ERROR_NONE)
  }
  
  private def parseLoopExpression(): ParseResult[Expression] = {
    val loopToken = advance()  // consume 'loop'
    
    // Parse body (either expression or indented block)
    val bodyResult = if (check(TokenType.TOKEN_NEWLINE) && 
                          tokens.length > current + 1 && 
                          tokens(current + 1).tokenType == TokenType.TOKEN_INDENT) {
      parseBlockExpression()
    } else {
      parseExpression()
    }
    if (bodyResult.isError()) return createParseError(bodyResult.error, bodyResult.errorMessage)
    
    // Check for end marker
    var hasEndMarker = false
    if (matchToken(TokenType.TOKEN_END)) {
      if (matchToken(TokenType.TOKEN_LOOP)) {
        hasEndMarker = true
      } else {
        // Put back the 'end' token if not followed by 'loop'
        current -= 1
      }
    }
    
    ParseResult(LoopExpression(bodyResult.result, hasEndMarker, loopToken.location), ERROR_NONE)
  }
  
  private def parseMatchExpression(): ParseResult[Expression] = {
    val matchToken = advance()  // consume 'match'
    
    // Parse value to match against
    val valueResult = parseExpression()
    if (valueResult.isError()) return createParseError(valueResult.error, valueResult.errorMessage)
    
    // Expect newline and indent for match block
    val newlineResult = consume(TokenType.TOKEN_NEWLINE, "newline after match expression")
    if (newlineResult.isError()) return createParseError(newlineResult.error, newlineResult.errorMessage)
    
    val indentResult = consume(TokenType.TOKEN_INDENT, "indentation for match block")
    if (indentResult.isError()) return createParseError(indentResult.error, indentResult.errorMessage)
    
    val cases = ArrayBuffer[MatchCase]()
    var defaultCase: Expression = null
    
    // Parse match cases
    while (!check(TokenType.TOKEN_DEDENT) && !isAtEnd()) {
      if (check(TokenType.TOKEN_CASE)) {
        advance()  // consume 'case'
        // Parse case pattern
        val patternResult = parsePattern()
        if (patternResult.isError()) return createParseError(patternResult.error, patternResult.errorMessage)
        
        // Expect '->' arrow
        val arrowResult = consume(TokenType.TOKEN_ARROW, "'->' after match case pattern")
        if (arrowResult.isError()) return createParseError(arrowResult.error, arrowResult.errorMessage)
        
        // Parse case body
        val bodyResult = if (check(TokenType.TOKEN_NEWLINE) && 
                            tokens.length > current + 1 && 
                            tokens(current + 1).tokenType == TokenType.TOKEN_INDENT) {
          // Multi-line case with indent
          parseBlockExpression()
        } else {
          // Single expression after '->'
          parseExpression()
        }
        if (bodyResult.isError()) return createParseError(bodyResult.error, bodyResult.errorMessage)
        
        cases += MatchCase(patternResult.result, bodyResult.result, patternResult.result.location)
        
      } else if (check(TokenType.TOKEN_DEFAULT)) {
        advance()  // consume 'default'
        // Parse default case
        val bodyResult = if (check(TokenType.TOKEN_NEWLINE) && 
                            tokens.length > current + 1 && 
                            tokens(current + 1).tokenType == TokenType.TOKEN_INDENT) {
          parseBlockExpression()
        } else {
          parseExpression()
        }
        if (bodyResult.isError()) return createParseError(bodyResult.error, bodyResult.errorMessage)
        
        defaultCase = bodyResult.result
        
      } else {
        // Skip unknown tokens or newlines
        if (check(TokenType.TOKEN_NEWLINE)) {
          advance()  // consume newline
        } else {
          advance()  // Skip unexpected token
        }
      }
    }
    
    // Consume DEDENT
    val dedentResult = consume(TokenType.TOKEN_DEDENT, "dedent to close match block")
    if (dedentResult.isError()) return createParseError(dedentResult.error, dedentResult.errorMessage)
    
    // Check for end marker
    var hasEndMarker = false
    if (check(TokenType.TOKEN_END)) {
      advance()  // consume 'end'
      if (check(TokenType.TOKEN_MATCH)) {
        advance()  // consume 'match'
        hasEndMarker = true
      } else {
        // Put back the 'end' token if not followed by 'match'
        current -= 1
      }
    }
    
    ParseResult(MatchExpression(valueResult.result, cases, defaultCase, hasEndMarker, matchToken.location), ERROR_NONE)
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
  
  private def parseLetExpression(): ParseResult[Expression] = {
    val startLocation = previous().location
    val bindings = ArrayBuffer[LetBinding]()
    
    // Check if we have an indented binding block (newline followed by indent)
    if (check(TokenType.TOKEN_NEWLINE) && current + 1 < tokens.length && tokens(current + 1).tokenType == TokenType.TOKEN_INDENT) {
      // Indented binding block style
      advance() // consume newline
      advance() // consume indent
      
      // Parse bindings until dedent
      while (!check(TokenType.TOKEN_DEDENT) && !isAtEnd()) {
        val bindingResult = parseLetBinding()
        if (bindingResult.isError()) return createParseError(bindingResult.error, bindingResult.errorMessage)
        bindings += bindingResult.result
        
        // Consume newline between bindings
        if (check(TokenType.TOKEN_NEWLINE)) {
          advance()
        }
      }
      
      val dedentResult = consume(TokenType.TOKEN_DEDENT, "dedent after let bindings")
      if (dedentResult.isError()) return createParseError(dedentResult.error, dedentResult.errorMessage)
      
    } else {
      // Comma-separated binding list style (inline)
      var continue = true
      while (continue) {
        val bindingResult = parseLetBinding()
        if (bindingResult.isError()) return createParseError(bindingResult.error, bindingResult.errorMessage)
        bindings += bindingResult.result
        continue = matchToken(TokenType.TOKEN_COMMA)
      }
    }
    
    // Consume 'in' keyword
    val inResult = consume(TokenType.TOKEN_IN, "'in' after let bindings")
    if (inResult.isError()) return createParseError(inResult.error, inResult.errorMessage)
    
    // Parse body
    var hasEndMarker = false
    val bodyResult = if (check(TokenType.TOKEN_NEWLINE) && 
                          tokens.length > current + 1 && 
                          tokens(current + 1).tokenType == TokenType.TOKEN_INDENT) {
      // Indented body - let parseBlockExpression handle newline and indent
      val blockResult = parseBlockExpression()
      if (blockResult.isError()) return createParseError(blockResult.error, blockResult.errorMessage)
      
      // Check for optional 'end let'
      if (matchToken(TokenType.TOKEN_END)) {
        val letResult = consume(TokenType.TOKEN_LET, "'let' after 'end'")
        if (letResult.isError()) return createParseError(letResult.error, letResult.errorMessage)
        hasEndMarker = true
      }
      
      blockResult
    } else {
      // Simple expression body
      parseExpression()
    }
    
    if (bodyResult.isError()) return createParseError(bodyResult.error, bodyResult.errorMessage)
    
    ParseResult(LetExpression(bindings, bodyResult.result, hasEndMarker, startLocation), ERROR_NONE)
  }
  
  private def parseLetBinding(): ParseResult[LetBinding] = {
    val nameResult = consume(TokenType.TOKEN_IDENTIFIER, "variable name")
    if (nameResult.isError()) return createParseError(nameResult.error, nameResult.errorMessage)
    
    val equalResult = consume(TokenType.TOKEN_EQUAL, "'=' after variable name")
    if (equalResult.isError()) return createParseError(equalResult.error, equalResult.errorMessage)
    
    // Parse expression but stop at 'in' token (let binding context)
    val valueResult = parseExpressionStopAt(TokenType.TOKEN_IN)
    if (valueResult.isError()) return createParseError(valueResult.error, valueResult.errorMessage)
    
    ParseResult(LetBinding(nameResult.result.lexeme, valueResult.result, nameResult.result.location), ERROR_NONE)
  }
  
  // Parse expression but stop when encountering specific token
  private def parseExpressionStopAt(stopToken: TokenType): ParseResult[Expression] = {
    parseAssignmentStopAt(stopToken)
  }
  
  private def parseAssignmentStopAt(stopToken: TokenType): ParseResult[Expression] = {
    val exprResult = parseTernaryStopAt(stopToken)
    if (exprResult.isError()) return exprResult
    
    var expr = exprResult.result
    
    val token = peek()
    if (!check(stopToken) && precedence.contains(token.lexeme) && precedence(token.lexeme) == 1) {  // Assignment operators
      val operator = advance()
      val valueResult = parseAssignmentStopAt(stopToken)  // Right associative
      if (valueResult.isError()) return createParseError(valueResult.error, valueResult.errorMessage)
      
      expr = AssignmentExpression(expr, operator.lexeme, valueResult.result, expr.location)
    }
    
    ParseResult(expr, ERROR_NONE)
  }
  
  private def parseTernaryStopAt(stopToken: TokenType): ParseResult[Expression] = {
    // For now, just call parseLogicalOr since we don't have ternary implemented
    parseLogicalOrStopAt(stopToken)
  }
  
  private def parseLogicalOrStopAt(stopToken: TokenType): ParseResult[Expression] = {
    val leftResult = parseLogicalAndStopAt(stopToken)
    if (leftResult.isError()) return leftResult
    
    var left = leftResult.result
    
    while (!check(stopToken) && (matchToken(TokenType.TOKEN_PIPE_PIPE) || matchToken(TokenType.TOKEN_OR))) {
      val operator = previous()
      val rightResult = parseLogicalAndStopAt(stopToken)
      if (rightResult.isError()) return createParseError(rightResult.error, rightResult.errorMessage)
      left = BinaryExpression(operator.lexeme, left, rightResult.result, left.location)
    }
    
    ParseResult(left, ERROR_NONE)
  }
  
  private def parseLogicalAndStopAt(stopToken: TokenType): ParseResult[Expression] = {
    val leftResult = parseEqualityStopAt(stopToken)
    if (leftResult.isError()) return leftResult
    
    var left = leftResult.result
    
    while (!check(stopToken) && (matchToken(TokenType.TOKEN_AMP_AMP) || matchToken(TokenType.TOKEN_AND))) {
      val operator = previous()
      val rightResult = parseEqualityStopAt(stopToken)
      if (rightResult.isError()) return createParseError(rightResult.error, rightResult.errorMessage)
      left = BinaryExpression(operator.lexeme, left, rightResult.result, left.location)
    }
    
    ParseResult(left, ERROR_NONE)
  }
  
  private def parseEqualityStopAt(stopToken: TokenType): ParseResult[Expression] = {
    val leftResult = parseBinaryExpressionStopAt(6, stopToken)  // Start at comparison precedence
    if (leftResult.isError()) return leftResult
    
    var left = leftResult.result
    
    while (!check(stopToken) && (matchToken(TokenType.TOKEN_EQUAL_EQUAL) || matchToken(TokenType.TOKEN_BANG_EQUAL))) {
      val operator = previous()
      val rightResult = parseBinaryExpressionStopAt(6, stopToken)
      if (rightResult.isError()) return createParseError(rightResult.error, rightResult.errorMessage)
      left = BinaryExpression(operator.lexeme, left, rightResult.result, left.location)
    }
    
    ParseResult(left, ERROR_NONE)
  }
  
  private def parseBinaryExpressionStopAt(minPrec: Int, stopToken: TokenType): ParseResult[Expression] = {
    val leftResult = parseUnary()
    if (leftResult.isError()) return leftResult
    
    var left = leftResult.result
    
    while (!isAtEnd() && !check(stopToken)) {
      val token = peek()
      val tokenPrec = precedence.getOrElse(token.lexeme, 0)
      
      if (tokenPrec < minPrec) {
        return ParseResult(left, ERROR_NONE)
      }
      
      val operator = advance()
      val nextMinPrec = if (rightAssociative.contains(operator.lexeme)) tokenPrec else tokenPrec + 1
      
      val rightResult = parseBinaryExpressionStopAt(nextMinPrec, stopToken)
      if (rightResult.isError()) return createParseError(rightResult.error, rightResult.errorMessage)
      
      left = BinaryExpression(operator.lexeme, left, rightResult.result, left.location)
    }
    
    ParseResult(left, ERROR_NONE)
  }
  
  // ===== PATTERN PARSING =====
  
  private def parsePattern(): ParseResult[Pattern] = {
    parseAlternativePattern()
  }
  
  // Parse alternative patterns: pattern1 | pattern2 | pattern3
  private def parseAlternativePattern(): ParseResult[Pattern] = {
    val leftResult = parseBindingPattern()
    if (leftResult.isError()) return leftResult
    
    if (check(TokenType.TOKEN_PIPE)) {
      val alternatives = ArrayBuffer[Pattern]()
      alternatives += leftResult.result
      
      while (matchToken(TokenType.TOKEN_PIPE)) {
        val rightResult = parseBindingPattern()
        if (rightResult.isError()) return createParseError(rightResult.error, rightResult.errorMessage)
        alternatives += rightResult.result
      }
      
      ParseResult(AlternativePattern(alternatives, leftResult.result.location), ERROR_NONE)
    } else {
      leftResult
    }
  }
  
  // Parse binding patterns: name@pattern
  private def parseBindingPattern(): ParseResult[Pattern] = {
    if (check(TokenType.TOKEN_IDENTIFIER)) {
      val checkpoint = current
      val identToken = advance()
      
      if (matchToken(TokenType.TOKEN_AT)) {
        // This is a binding pattern: name@pattern
        val patternResult = parsePrimaryPattern()
        if (patternResult.isError()) return createParseError(patternResult.error, patternResult.errorMessage)
        return ParseResult(BindingPattern(identToken.lexeme, patternResult.result, identToken.location), ERROR_NONE)
      } else {
        // Rewind and parse as primary pattern
        current = checkpoint
        parsePrimaryPattern()
      }
    } else {
      parsePrimaryPattern()
    }
  }
  
  // Parse primary patterns (everything except alternatives and binding)
  private def parsePrimaryPattern(): ParseResult[Pattern] = {
    // Parenthesized patterns: (pattern)
    if (check(TokenType.TOKEN_LEFT_PAREN)) {
      advance()  // consume '('
      val patternResult = parsePattern()
      if (patternResult.isError()) return createParseError(patternResult.error, patternResult.errorMessage)
      val rightParenResult = consume(TokenType.TOKEN_RIGHT_PAREN, "')'")
      if (rightParenResult.isError()) return createParseError(rightParenResult.error, rightParenResult.errorMessage)
      return patternResult
    }
    
    // Array destructuring pattern: [a, b, ...rest]
    else if (check(TokenType.TOKEN_LEFT_BRACKET)) {
      val result = parseArrayPattern()
      ParseResult(result.result.asInstanceOf[Pattern], result.error, result.errorMessage, result.errorLocation)
    }
    
    // Object destructuring pattern: {name, age}
    else if (check(TokenType.TOKEN_LEFT_BRACE)) {
      val result = parseObjectPattern()
      ParseResult(result.result.asInstanceOf[Pattern], result.error, result.errorMessage, result.errorLocation)
    }
    
    // Literal patterns: 0, 1, "hello", true, false, null, undefined
    else if (check(TokenType.TOKEN_INTEGER) || check(TokenType.TOKEN_FLOAT) || check(TokenType.TOKEN_STRING) ||
             check(TokenType.TOKEN_TRUE) || check(TokenType.TOKEN_FALSE) || check(TokenType.TOKEN_NULL) || check(TokenType.TOKEN_UNDEFINED)) {
      val literalResult = parsePrimary()  // parsePrimary handles all literal types
      if (literalResult.isError()) return createParseError(literalResult.error, literalResult.errorMessage)
      ParseResult(LiteralPattern(literalResult.result, literalResult.result.location), ERROR_NONE)
    }
    
    // Identifier pattern (could be simple binding or constructor pattern)
    else if (check(TokenType.TOKEN_IDENTIFIER)) {
      val token = advance()
      
      // Check if it's a constructor pattern: Name(patterns...)
      if (check(TokenType.TOKEN_LEFT_PAREN)) {
        advance()  // consume '('
        
        val args = ArrayBuffer[Pattern]()
        
        // Handle empty constructor: Name()
        if (check(TokenType.TOKEN_RIGHT_PAREN)) {
          advance()
          return ParseResult(ConstructorPattern(token.lexeme, args, token.location), ERROR_NONE)
        }
        
        // Parse argument patterns
        while (!check(TokenType.TOKEN_RIGHT_PAREN) && !isAtEnd()) {
          val argResult = parsePattern()
          if (argResult.isError()) return createParseError(argResult.error, argResult.errorMessage)
          args += argResult.result
          
          if (!check(TokenType.TOKEN_RIGHT_PAREN)) {
            val commaResult = consume(TokenType.TOKEN_COMMA, "','")
            if (commaResult.isError()) return createParseError(commaResult.error, commaResult.errorMessage)
          }
        }
        
        val rightParenResult = consume(TokenType.TOKEN_RIGHT_PAREN, "')'")
        if (rightParenResult.isError()) return createParseError(rightParenResult.error, rightParenResult.errorMessage)
        
        ParseResult(ConstructorPattern(token.lexeme, args, token.location), ERROR_NONE)
      } else {
        // Simple identifier pattern
        ParseResult(IdentifierPattern(token.lexeme, token.location), ERROR_NONE)
      }
    }
    else {
      createParseError(ERROR_UNEXPECTED_TOKEN, s"Expected pattern, got: ${peek().lexeme}")
    }
  }
  
  private def parseArrayPattern(): ParseResult[ArrayPattern] = {
    val leftBracket = advance()  // consume '['
    
    val elements = ArrayBuffer[Pattern]()
    var restPattern: String = null
    
    // Handle empty array pattern []
    if (check(TokenType.TOKEN_RIGHT_BRACKET)) {
      advance()
      return ParseResult(ArrayPattern(elements, restPattern, leftBracket.location), ERROR_NONE)
    }
    
    // Parse pattern elements
    while (!check(TokenType.TOKEN_RIGHT_BRACKET) && !isAtEnd()) {
      // Handle rest pattern: ...rest (note: we don't have TOKEN_DOT_DOT_DOT, so this needs to be implemented differently)
      // For now, we'll handle simple patterns
      
      // Handle hole in pattern: [a, , c]
      if (check(TokenType.TOKEN_COMMA)) {
        // Add placeholder pattern for holes
        elements += IdentifierPattern("_", peek().location)
      } else {
        val patternResult = parsePattern()
        if (patternResult.isError()) return createParseError(patternResult.error, patternResult.errorMessage)
        elements += patternResult.result
      }
      
      if (!check(TokenType.TOKEN_RIGHT_BRACKET)) {
        val commaResult = consume(TokenType.TOKEN_COMMA, "','")
        if (commaResult.isError()) return createParseError(commaResult.error, commaResult.errorMessage)
      }
    }
    
    val rightBracketResult = consume(TokenType.TOKEN_RIGHT_BRACKET, "']'")
    if (rightBracketResult.isError()) return createParseError(rightBracketResult.error, rightBracketResult.errorMessage)
    
    ParseResult(ArrayPattern(elements, restPattern, leftBracket.location), ERROR_NONE)
  }
  
  private def parseObjectPattern(): ParseResult[ObjectPattern] = {
    val leftBrace = advance()  // consume '{'
    
    val properties = ArrayBuffer[ObjectPatternProperty]()
    
    // Handle empty object pattern {}
    if (check(TokenType.TOKEN_RIGHT_BRACE)) {
      advance()
      return ParseResult(ObjectPattern(properties, leftBrace.location), ERROR_NONE)
    }
    
    // Parse property patterns
    while (!check(TokenType.TOKEN_RIGHT_BRACE) && !isAtEnd()) {
      val keyResult = consume(TokenType.TOKEN_IDENTIFIER, "property name")
      if (keyResult.isError()) return createParseError(keyResult.error, keyResult.errorMessage)
      
      val key = keyResult.result.lexeme
      var pattern: Pattern = IdentifierPattern(key, keyResult.result.location)  // Default: shorthand property
      
      // Check for explicit pattern: {x: pattern}
      if (matchToken(TokenType.TOKEN_COLON)) {
        val patternResult = parsePattern()
        if (patternResult.isError()) return createParseError(patternResult.error, patternResult.errorMessage)
        pattern = patternResult.result
      }
      
      properties += ObjectPatternProperty(key, pattern, keyResult.result.location)
      
      if (!check(TokenType.TOKEN_RIGHT_BRACE)) {
        val commaResult = consume(TokenType.TOKEN_COMMA, "','")
        if (commaResult.isError()) return createParseError(commaResult.error, commaResult.errorMessage)
      }
    }
    
    val rightBraceResult = consume(TokenType.TOKEN_RIGHT_BRACE, "'}'")
    if (rightBraceResult.isError()) return createParseError(rightBraceResult.error, rightBraceResult.errorMessage)
    
    ParseResult(ObjectPattern(properties, leftBrace.location), ERROR_NONE)
  }
}