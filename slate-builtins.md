# Slate Language Built-in Functions

This document describes all built-in functions available in the Slate language runtime. These functions are globally available without imports and provide core functionality for mathematical operations, I/O, type manipulation, and collection construction.

## Type Operator

- `typeof expression` - Returns the type of a value as a string (unary operator)
  - Returns: `"number"`, `"string"`, `"boolean"`, `"array"`, `"object"`, `"function"`, `"null"`, `"undefined"`
  - Usage: `typeof x`, `typeof (a + b)`, `typeof obj.property`

## I/O Functions  

### Console Operations
- `print(...)` - Prints one or more values to stdout, separated by spaces
  - Accepts variable number of arguments
  - Automatically converts values to string representation
- `input(prompt?)` - Reads a line from stdin
  - Optional prompt parameter to display before reading input
  - Returns the input as a string

## Collection Functions

### Factory Functions
These functions create new instances of collection types:

- `Array(...)` - Array factory function
  - Creates a new array from the provided arguments
  - Example: `Array(1, 2, 3)` creates `[1, 2, 3]`
  
- `String(...)` - String factory function
  - Converts arguments to string representation
  - Example: `String(42)` returns `"42"`
  
- `Buffer(...)` - Buffer factory function
  - Creates a new binary buffer from the provided data
  - Used for handling binary data operations

## Math Functions

### Basic Math Operations
- `abs(number)` - Returns the absolute value of a number
- `sqrt(number)` - Returns the square root of a number
- `floor(number)` - Returns the largest integer less than or equal to the number
- `ceil(number)` - Returns the smallest integer greater than or equal to the number
- `min(a, b)` - Returns the smaller of two numbers
- `max(a, b)` - Returns the larger of two numbers
- `random()` - Returns a random number between 0 (inclusive) and 1 (exclusive)

### Trigonometric Functions
- `sin(x)` - Returns the sine of x (x in radians)
- `cos(x)` - Returns the cosine of x (x in radians)  
- `tan(x)` - Returns the tangent of x (x in radians)

### Inverse Trigonometric Functions
- `asin(x)` - Returns the arcsine of x (result in radians)
- `acos(x)` - Returns the arccosine of x (result in radians)
- `atan(x)` - Returns the arctangent of x (result in radians)
- `atan2(y, x)` - Returns the arctangent of y/x (result in radians, handles all quadrants)

### Exponential and Logarithmic Functions
- `exp(x)` - Returns e raised to the power of x (e^x)
- `ln(x)` - Returns the natural logarithm (base e) of x

## Usage Examples

### Type Inspection
```slate
val x = 42
print(typeof x)         \\ Prints: "number"

val arr = [1, 2, 3]
print(typeof arr)       \\ Prints: "array"

\\ Operator precedence - parentheses recommended for complex expressions
print(typeof (a + b))   \\ Prints type of sum result
print(typeof obj.prop)  \\ Prints type of property value
```

### Mathematical Operations
```slate
print(abs(-5))          \\ Prints: 5
print(sqrt(16))         \\ Prints: 4.0
print(floor(3.7))       \\ Prints: 3
print(ceil(3.2))        \\ Prints: 4
```

### I/O Operations
```slate
print("Hello", "World", 123)    \\ Prints: Hello World 123

val name = input("Enter your name: ")
print("Hello, " + name)
```

### Collection Creation
```slate
val numbers = Array(1, 2, 3, 4, 5)
val text = String(42)           \\ "42"
val data = Buffer(0x48, 0x65, 0x6c, 0x6c, 0x6f)  \\ Binary "Hello"
```

### Math Functions
```slate
print(min(10, 5))       \\ Prints: 5
print(max(10, 5))       \\ Prints: 10
print(random())         \\ Prints: 0.7234... (random)

\\ Trigonometric calculations
val angle = 3.14159 / 4  \\ 45 degrees in radians
print(sin(angle))       \\ Prints: 0.707...
print(cos(angle))       \\ Prints: 0.707...

\\ Exponential calculations
print(exp(1))           \\ Prints: 2.718... (e)
print(ln(2.718))        \\ Prints: 1.0... (approximately)
```

## Implementation Notes

These built-in functions are implemented as native functions in the Slate runtime and are automatically available in the global scope. They do not require importing and cannot be shadowed by user-defined variables in the global scope.

The functions follow standard mathematical conventions and provide the foundation for building more complex mathematical and utility functions in user code.