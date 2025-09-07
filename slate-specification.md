# Slate Language Specification

## Overview

Slate is a dynamically-typed, expression-oriented programming language designed for both embedded systems (MCU-friendly) and general computing. It features significant whitespace, prototype-based objects, and a C-compatible implementation architecture.

## Type System

Slate uses a tagged union value representation with reference counting for memory management. All values are represented as `value_t` structures containing a type tag and a union of possible values.

### Core Value Types

**Primitive Types:**
- **null**: The null value
- **undefined**: Undefined/uninitialized value  
- **boolean**: `true` or `false`

**Numeric Types:**
- **int32**: 32-bit signed integer (default for integer literals, MCU-friendly)
- **bigint**: Arbitrary precision integer (automatic promotion on overflow)
- **float32**: Single precision floating point (32-bit)
- **float64**: Double precision floating point (64-bit, default for decimal literals)

**Text Types:**
- **string**: Immutable UTF-8 encoded strings
- **string_builder**: Mutable string builder for efficient concatenation

**Collection Types:**
- **array**: Dynamic arrays `[1, 2, 3]` with automatic resizing
- **object**: Key-value hash maps `{key: value}` (prototype-based)
- **range**: Numeric ranges `1..10` (inclusive) or `1..<10` (exclusive) with optional step
- **iterator**: Lazy iteration over collections (arrays, ranges, objects)

**Binary Data Types:**
- **buffer**: Immutable byte buffers for binary data
- **buffer_builder**: Mutable buffer builder for constructing binary data
- **buffer_reader**: Sequential reader for parsing binary data

**Function Types:**
- **function**: User-defined Slate functions
- **closure**: Functions with captured environment
- **native**: Native C functions exposed to Slate
- **bound_method**: Method bound to a specific receiver object

**Object-Oriented Types:**
- **class**: Class definitions (prototypes with constructors and methods)

**Date/Time Types:**
- **local_date**: Date without timezone (2024-12-25)
- **local_time**: Time without date or timezone (15:30:45)
- **local_datetime**: Date and time without timezone (2024-12-25T15:30:45)
- **zone**: Timezone information (America/Toronto, UTC, etc.)
- **date**: Date and time with timezone (primary zoned datetime type)
- **instant**: Point in time (Unix timestamp with nanoseconds)
- **duration**: Time-based amount (2 hours, 30 minutes)  
- **period**: Date-based amount (2 years, 3 months, 5 days)

## Object Model

### Prototype-Based Objects

Slate uses a prototype-based object system similar to JavaScript:

- **Objects** are hash maps that can have arbitrary properties added at runtime
- **Property access** can be static (`obj.property`) or dynamic (`obj("property")`)
- **Class instances** have a pointer to their class definition for method resolution
- **Inheritance** works through prototype chains

### Class System

Classes in Slate are prototype definitions that contain:

```c
struct class {
    size_t ref_count;                    // Reference counting
    char* name;                          // Class name  
    do_object instance_properties;       // Instance methods/properties
    do_object static_properties;         // Static methods/class properties
    value_t (*factory)(vm_t*, class_t*, int, value_t*); // Constructor function
};
```

**Class Features:**
- **Instance methods**: Methods available on all instances of the class
- **Static methods**: Methods available on the class itself
- **Factory functions**: Constructor functions for creating and initializing instances
- **Prototype inheritance**: Classes can inherit from other classes

### Method Resolution

When accessing a property on an object:

1. **Own properties**: Look up property in the object's own hash map
2. **Instance methods**: If not found and object has a class, look in class instance methods  
3. **Prototype chain**: Follow prototype chain up for inherited methods
4. **Static methods**: Accessed directly through the class object

### Property Access Patterns

```slate
\ Static property access (compile-time resolved)
obj.property
obj.method()

\ Dynamic property access (runtime resolved)  
var key = "property"
obj(key)
obj(key, value)  // assignment via function call syntax
```

## Memory Management

Slate uses **reference counting** instead of garbage collection for C compatibility:

### Reference Counting Model

- **All complex types** (strings, arrays, objects, classes) use reference counting
- **Automatic cleanup** when reference count reaches zero
- **No cycle collection** (trade-off for C compatibility and predictable behavior)
- **Manual reference management** via `vm_retain()` and `vm_release()` functions

### Memory Optimization

- **Copy-on-write semantics** for some data structures
- **String interning** for frequently used strings  
- **Object pooling** for commonly created objects
- **Stack allocation** for temporary values when possible

### Reference Counting Rules

```c
// Retaining values (increment reference count)
value_t vm_retain(value_t value);

// Releasing values (decrement reference count, cleanup if zero)
void vm_release(value_t* value);

// Example: Function parameter handling
value_t my_function(vm_t* vm, value_t arg) {
    vm_retain(arg);        // Retain input
    // ... use arg ...
    vm_release(&arg);      // Release when done
    return result;
}
```

## Type Coercion and Conversion

### Automatic Type Promotion

- **int32 → float64**: In mixed arithmetic operations
- **int32 → bigint**: On integer overflow (seamless arbitrary precision)
- **Numeric → string**: In string concatenation operations

### Division Semantics

```slate
5 / 2      // → 2.5 (always produces float)
5 // 2     // → 2 (floor division, produces integer)
5 % 2      // → 1 (modulo)
5 ** 2     // → 25 (exponentiation)
```

### Range Operations

```slate
1..10      // Inclusive range [1, 2, 3, ..., 10]
1..<10     // Exclusive range [1, 2, 3, ..., 9]  
1..10 step 2  // Range with step [1, 3, 5, 7, 9]
```

## Module System

Slate uses a **namespace-based module system** with shared VM execution:

### Module Loading

- **Single VM**: All modules execute in shared virtual machine
- **Namespace isolation**: Each module has its own namespace for globals
- **Python-like behavior**: Modules can share closures and state
- **Import resolution**: Modules resolved relative to working directory

### Import Syntax

```slate
import math._                      // Wildcard import (all exports)
import utils.{max, min => minimum} // Selective import with aliasing  
import io.File                     // Simple import
import collections                 // Namespace import
```

### Module Structure

- **Each `.sl` file** is a module
- **Package declarations** define module namespace
- **Export/import** through global variable definitions
- **Cross-module closures** supported (unlike isolated VM approach)

## Function System

### Function Types

1. **User Functions**: Defined with `def` keyword, compiled to bytecode
2. **Native Functions**: C functions exposed to Slate via function pointers  
3. **Closures**: Functions that capture variables from enclosing scope
4. **Bound Methods**: Methods bound to specific object instances

### Function Declaration

```slate
// Simple function
def add(a, b) = a + b

// Block function with end marker
def complex_calculation(x) =
    val temp = x * 2
    val result = temp + 10
    result
end complex_calculation

// Anonymous function
val multiply = (a, b) -> a * b
```

### Closure Semantics

```slate
def make_counter(initial) =
    var count = initial
    () -> 
        count += 1
        count

val counter = make_counter(0)
print(counter())  // → 1
print(counter())  // → 2
```

## Error Handling

### Runtime Errors

Slate uses **error codes and early returns** instead of exceptions for C compatibility:

```c
typedef enum {
    VM_SUCCESS,
    VM_RUNTIME_ERROR,
    VM_COMPILE_ERROR,
    VM_MEMORY_ERROR
} vm_result;
```

### Debug Information

- **Source location tracking** for every value (when debug mode enabled)
- **Stack traces** showing call chain and source locations
- **Variable scope information** for debugging tools

## Built-in Functions and Operators

### Mathematical Functions
- `abs(x)`, `sqrt(x)`, `floor(x)`, `ceil(x)`
- `sin(x)`, `cos(x)`, `tan(x)`, `exp(x)`, `ln(x)`
- `min(a, b)`, `max(a, b)`, `random()`

### I/O Functions  
- `print(...)`: Print values to stdout
- `input(prompt?)`: Read line from stdin

### Type Functions
- `type(value)`: Returns type name as string
- `Array(...)`: Array factory function
- `String(...)`: String factory function  
- `Buffer(...)`: Buffer factory function

### Collection Functions
- Array access: `arr(index)` (using function call syntax)
- Object access: `obj("key")` or `obj.key`
- Range creation: `start..end` or `start..<end`

This specification provides the foundation for implementing the Slate language interpreter while maintaining compatibility with the existing C-based runtime and ensuring consistent behavior across implementations.