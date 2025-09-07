package io.github.edadma.sl_scala
package lexer

enum TokenType {
  case TOKEN_EOF
  case TOKEN_ERROR
  case TOKEN_NEWLINE
  case TOKEN_INDENT
  case TOKEN_DEDENT
  
  // Literals
  case TOKEN_INTEGER
  case TOKEN_FLOAT
  case TOKEN_STRING
  case TOKEN_TRUE
  case TOKEN_FALSE
  case TOKEN_NULL
  case TOKEN_UNDEFINED
  case TOKEN_NAN
  case TOKEN_INFINITY
  
  // Identifiers
  case TOKEN_IDENTIFIER
  
  // Keywords
  case TOKEN_VAR
  case TOKEN_VAL
  case TOKEN_DEF
  case TOKEN_IF
  case TOKEN_THEN
  case TOKEN_ELIF
  case TOKEN_ELSE
  case TOKEN_WHILE
  case TOKEN_DO
  case TOKEN_FOR
  case TOKEN_LOOP
  case TOKEN_BREAK
  case TOKEN_CONTINUE
  case TOKEN_RETURN
  case TOKEN_IMPORT
  case TOKEN_PACKAGE
  case TOKEN_PRIVATE
  case TOKEN_MATCH
  case TOKEN_CASE
  case TOKEN_DEFAULT
  case TOKEN_DATA
  case TOKEN_AND
  case TOKEN_OR
  case TOKEN_NOT
  case TOKEN_IN
  case TOKEN_INSTANCEOF
  case TOKEN_MOD
  case TOKEN_STEP
  case TOKEN_END
  
  // Operators
  case TOKEN_PLUS
  case TOKEN_MINUS
  case TOKEN_STAR
  case TOKEN_SLASH
  case TOKEN_SLASH_SLASH  // floor division
  case TOKEN_PERCENT
  case TOKEN_POWER        // **
  
  // Comparison
  case TOKEN_EQUAL
  case TOKEN_EQUAL_EQUAL
  case TOKEN_BANG_EQUAL
  case TOKEN_LESS
  case TOKEN_LESS_EQUAL
  case TOKEN_GREATER
  case TOKEN_GREATER_EQUAL
  
  // Logical
  case TOKEN_AMP_AMP
  case TOKEN_PIPE_PIPE
  case TOKEN_BANG
  case TOKEN_QUESTION_QUESTION  // null coalesce
  
  // Bitwise
  case TOKEN_AMP
  case TOKEN_PIPE
  case TOKEN_CARET
  case TOKEN_TILDE
  case TOKEN_LESS_LESS      // <<
  case TOKEN_GREATER_GREATER // >>
  case TOKEN_GREATER_GREATER_GREATER // >>>
  
  // Assignment
  case TOKEN_PLUS_EQUAL
  case TOKEN_MINUS_EQUAL
  case TOKEN_STAR_EQUAL
  case TOKEN_SLASH_EQUAL
  case TOKEN_SLASH_SLASH_EQUAL
  case TOKEN_PERCENT_EQUAL
  case TOKEN_POWER_EQUAL
  case TOKEN_LESS_LESS_EQUAL
  case TOKEN_GREATER_GREATER_EQUAL
  case TOKEN_GREATER_GREATER_GREATER_EQUAL
  case TOKEN_AMP_EQUAL
  case TOKEN_PIPE_EQUAL
  case TOKEN_CARET_EQUAL
  case TOKEN_AMP_AMP_EQUAL
  case TOKEN_PIPE_PIPE_EQUAL
  case TOKEN_QUESTION_QUESTION_EQUAL
  
  // Increment/Decrement
  case TOKEN_PLUS_PLUS
  case TOKEN_MINUS_MINUS
  
  // Delimiters
  case TOKEN_LEFT_PAREN
  case TOKEN_RIGHT_PAREN
  case TOKEN_LEFT_BRACKET
  case TOKEN_RIGHT_BRACKET
  case TOKEN_LEFT_BRACE
  case TOKEN_RIGHT_BRACE
  case TOKEN_COMMA
  case TOKEN_DOT
  case TOKEN_COLON
  case TOKEN_SEMICOLON
  case TOKEN_ARROW         // ->
  case TOKEN_FAT_ARROW     // =>
  case TOKEN_QUESTION
  case TOKEN_QUESTION_DOT  // ?.
  case TOKEN_DOT_DOT       // ..
  case TOKEN_DOT_DOT_LESS  // ..<
  
  // Template literals
  case TOKEN_TEMPLATE_START
  case TOKEN_TEMPLATE_END
  case TOKEN_TEMPLATE_TEXT
  case TOKEN_TEMPLATE_SIMPLE_VAR      // $identifier
  case TOKEN_TEMPLATE_EXPR_START      // ${
  case TOKEN_TEMPLATE_EXPR_END        // }
  
  // Comments (for documentation purposes)
  case TOKEN_COMMENT
  
  def toDisplayString: String = this match {
    case TOKEN_EOF => "EOF"
    case TOKEN_ERROR => "ERROR"
    case TOKEN_NEWLINE => "NEWLINE"
    case TOKEN_INDENT => "INDENT"
    case TOKEN_DEDENT => "DEDENT"
    
    case TOKEN_INTEGER => "INTEGER"
    case TOKEN_FLOAT => "FLOAT"
    case TOKEN_STRING => "STRING"
    case TOKEN_TRUE => "true"
    case TOKEN_FALSE => "false"
    case TOKEN_NULL => "null"
    case TOKEN_UNDEFINED => "undefined"
    case TOKEN_NAN => "NaN"
    case TOKEN_INFINITY => "Infinity"
    
    case TOKEN_IDENTIFIER => "IDENTIFIER"
    
    case TOKEN_VAR => "var"
    case TOKEN_VAL => "val"
    case TOKEN_DEF => "def"
    case TOKEN_IF => "if"
    case TOKEN_THEN => "then"
    case TOKEN_ELIF => "elif"
    case TOKEN_ELSE => "else"
    case TOKEN_WHILE => "while"
    case TOKEN_DO => "do"
    case TOKEN_FOR => "for"
    case TOKEN_LOOP => "loop"
    case TOKEN_BREAK => "break"
    case TOKEN_CONTINUE => "continue"
    case TOKEN_RETURN => "return"
    case TOKEN_IMPORT => "import"
    case TOKEN_PACKAGE => "package"
    case TOKEN_PRIVATE => "private"
    case TOKEN_MATCH => "match"
    case TOKEN_CASE => "case"
    case TOKEN_DEFAULT => "default"
    case TOKEN_DATA => "data"
    case TOKEN_AND => "and"
    case TOKEN_OR => "or"
    case TOKEN_NOT => "not"
    case TOKEN_IN => "in"
    case TOKEN_INSTANCEOF => "instanceof"
    case TOKEN_MOD => "mod"
    case TOKEN_STEP => "step"
    case TOKEN_END => "end"
    
    case TOKEN_PLUS => "+"
    case TOKEN_MINUS => "-"
    case TOKEN_STAR => "*"
    case TOKEN_SLASH => "/"
    case TOKEN_SLASH_SLASH => "//"
    case TOKEN_PERCENT => "%"
    case TOKEN_POWER => "**"
    
    case TOKEN_EQUAL => "="
    case TOKEN_EQUAL_EQUAL => "=="
    case TOKEN_BANG_EQUAL => "!="
    case TOKEN_LESS => "<"
    case TOKEN_LESS_EQUAL => "<="
    case TOKEN_GREATER => ">"
    case TOKEN_GREATER_EQUAL => ">="
    
    case TOKEN_AMP_AMP => "&&"
    case TOKEN_PIPE_PIPE => "||"
    case TOKEN_BANG => "!"
    case TOKEN_QUESTION_QUESTION => "??"
    
    case TOKEN_AMP => "&"
    case TOKEN_PIPE => "|"
    case TOKEN_CARET => "^"
    case TOKEN_TILDE => "~"
    case TOKEN_LESS_LESS => "<<"
    case TOKEN_GREATER_GREATER => ">>"
    case TOKEN_GREATER_GREATER_GREATER => ">>>"
    
    case TOKEN_PLUS_EQUAL => "+="
    case TOKEN_MINUS_EQUAL => "-="
    case TOKEN_STAR_EQUAL => "*="
    case TOKEN_SLASH_EQUAL => "/="
    case TOKEN_SLASH_SLASH_EQUAL => "//="
    case TOKEN_PERCENT_EQUAL => "%="
    case TOKEN_POWER_EQUAL => "**="
    case TOKEN_LESS_LESS_EQUAL => "<<="
    case TOKEN_GREATER_GREATER_EQUAL => ">>="
    case TOKEN_GREATER_GREATER_GREATER_EQUAL => ">>>="
    case TOKEN_AMP_EQUAL => "&="
    case TOKEN_PIPE_EQUAL => "|="
    case TOKEN_CARET_EQUAL => "^="
    case TOKEN_AMP_AMP_EQUAL => "&&="
    case TOKEN_PIPE_PIPE_EQUAL => "||="
    case TOKEN_QUESTION_QUESTION_EQUAL => "??="
    
    case TOKEN_PLUS_PLUS => "++"
    case TOKEN_MINUS_MINUS => "--"
    
    case TOKEN_LEFT_PAREN => "("
    case TOKEN_RIGHT_PAREN => ")"
    case TOKEN_LEFT_BRACKET => "["
    case TOKEN_RIGHT_BRACKET => "]"
    case TOKEN_LEFT_BRACE => "{"
    case TOKEN_RIGHT_BRACE => "}"
    case TOKEN_COMMA => ","
    case TOKEN_DOT => "."
    case TOKEN_COLON => ":"
    case TOKEN_SEMICOLON => ";"
    case TOKEN_ARROW => "->"
    case TOKEN_FAT_ARROW => "=>"
    case TOKEN_QUESTION => "?"
    case TOKEN_QUESTION_DOT => "?."
    case TOKEN_DOT_DOT => ".."
    case TOKEN_DOT_DOT_LESS => "..<"
    
    case TOKEN_TEMPLATE_START => "`"
    case TOKEN_TEMPLATE_END => "`"
    case TOKEN_TEMPLATE_TEXT => "TEMPLATE_TEXT"
    case TOKEN_TEMPLATE_SIMPLE_VAR => "TEMPLATE_VAR"
    case TOKEN_TEMPLATE_EXPR_START => "${"
    case TOKEN_TEMPLATE_EXPR_END => "}"
    
    case TOKEN_COMMENT => "COMMENT"
  }
}