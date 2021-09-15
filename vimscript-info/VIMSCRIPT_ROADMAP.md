# Vimscript
## Done in current release
[x] expressions: binary, unary, ternary, function calls, sublists, options, registers, variables, lists, dictionaries  
[x] `if` condition  
[x] `for` and `while` loops  
[x] `try`/`catch`/`finally`  
[x] function definition (without flags)  
[x] `break`, `continue`, `throw`, `return`  
[x] scopes: `g:`, `s:`, `l:`, `a:`, `b:`, `w:`, `t:` (the `v:` scope is not supported yet)  
[x] `map <expr>`

## Plans for the next releases:
### IdeaVim 1.8.0

[] move `s:` scoped variables to `Script` class  
[] move `l:` and `a:` scoped variables to the `FunctionDeclaration` class  
[] `closure` function flag  
[] `..` as concatenation operator    
[] access dictionary value by `dict.key`  
[] `abort` function flag  
[] `range` function flag  
[] `dict` function flag  
[] dictionary functions  
[] anonymous functions  
[] lambdas  
[] function as method  
[] funcref type  
[] default value in functions e.g. `function F1(a, b = 10)`  
[] falsy operator `??`  
[] pass Lists and Dictionaries by reference  
[] variable locking (`lock`, `unlock`, `const`)  
[] rewrite OptionManager to vim data types  
[] scoped options  
[] classic package structure  
[] loggers loggers loggers  
[] more loggers

### IdeaVim 1.9.0   
  
[] make `LibraryFunction` return `Result`  
[] exception wrapping in try/catch  
[] store exception messages in property file  
[] store vim exception stacktrace in ExException  
[] expression register (`<C-R>=`)  
[] update tests to JUnit 5  
[] rethink vimscript tests  
[] delayed parsing of if/for/while etc.  
[] `has("ide")` or "ide" option  
[] `normal` command  
[] `call` command  
[] context dependent parsing e.g. `dict.key`  
[] improve `w:` and `t:` scopes  
[] `v:` scope  
[] curly-braces-names  
[] pass scopes to functions e.g. `for k in keys(s:)`  
[] all the let command's cases (e.g. registers)  
  
## Less important things that might be added soon  
  
[] add `-range` option to `command` command  