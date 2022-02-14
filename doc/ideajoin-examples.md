Some examples of join command with `ideajoin` option enabled.  
Put `set ideajoin` to your `~/.ideavimrc` to enable this functionality.

Now, you can press `J` (`shift+j`) on a line or a selected block of text to join the lines together.

* Automatic join concatenated lines:

```
"Hello" +                 ->       "Hello world"
" world!"
```

* Nested if's:

```
if (a) {                  ->       if (a && b) {
  if (b) {                             ...
     ...                           }
  }
}
```

* Comments:

```
// Hello                  ->       // Hello world
// world
```

* Remove braces from one line for / if / while:

```
if (fail) {               ->       if (fail) return;
     return;
}
```

* Kotlin one line method:

```
fun myNumber(): Int {     ->       fun myNumber(): Int = 42
    return 42
}
```

* Join declaration and initialization:

```
int a;                    ->       int a = 5;
a = 5;
```

* Chain call:

```
sb.append("a");           ->       sb.append("a").append("b");
sb.append("b");
```

And other features provided by the plugins.
