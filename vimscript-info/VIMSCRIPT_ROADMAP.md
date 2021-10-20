# Vimscript
## IdeaVim 1.7.0
- [x] expressions: binary, unary, ternary, function calls, sublists, options, registers, variables, lists, dictionaries  
- [x] `if` condition  
- [x] `for` and `while` loops  
- [x] `try`/`catch`/`finally`  
- [x] function definition (without flags)  
- [x] `break`, `continue`, `throw`, `return`  
- [x] scopes: `g:`, `s:`, `l:`, `a:`, `b:`, `w:`, `t:` (the `v:` scope is not supported yet)  
- [x] `map <expr>`

## Plans for the next releases:
### IdeaVim 1.8.0

- [x] move `s:` scoped variables to `Script` class  
- [x] move `l:` and `a:` scoped variables to the `FunctionDeclaration` class  
- [x] `closure` function flag  
- [x] `..` as concatenation operator    
- [x] falsy operator `??`
- [x] access dictionary value by `dict.key`  
- [x] `abort` function flag  
- [x] `range` function flag  
- [x] `call` command
- [x] optional arguments `...`
- [x] funcref type
- [x] lambdas
- [x] function as method
- [x] `function` function
- [x] `funcref` function
- [x] `dict` function flag
- [x] anonymous functions  
- [x] default value in functions e.g. `function F1(a, b = 10)`
- [x] `has("ide")` or "ide" option
- [x] reduce number of rules in grammar
- [x] classic package structure  

### IdeaVim 1.9.0
// basic language support
- [x] support `for [a, b] in {expr}`
- [x] pass scopes to functions e.g. `for k in keys(s:)`
- [x] curly-braces-names
- [x] `finish` statement

// variable service 
- [x] pass Lists and Dictionaries by reference
- [x] variable locking

// option service
- [ ] rewrite OptionManager to vim data types
- [ ] scoped options

// exceptions
- [ ] make `LibraryFunction` return `Result`  
- [ ] throwing multiple exceptions at once
- [ ] exception wrapping in try/catch  
- [ ] store exception messages in property file  
- [ ] store vim exception stacktrace in ExException  

// big brain things
- [ ] classloading
- [ ] executing context (script / command line) & better parent for executable
- [ ] `normal` command
- [ ] vim "special" type
- [ ] `v:` scope

// finishing touches
- [ ] update tests to JUnit 5  
- [ ] rethink vimscript tests  
- [ ] loggers
- [ ] todos, warnings

## Less important things that might be added soon  
  
- [ ] all the let command's cases (e.g. registers)
- [ ] expression register (`<C-R>=`)
- [ ] expressions in substitute command (`\=`)
- [ ] vim status line and beautiful exceptions output
- [ ] improve `w:` and `t:` scopes
- [ ] delayed parsing of if/for/while etc.
- [ ] context dependent parsing e.g. `dict.key`
- [ ] add `-range` option to `command` command  
