Some examples of join command with `ideajoin` option enabled.  
Put `set ideajoin` to your `~/.ideavimrc` to enable this functionality.

1) Automatic join concatenated lines:

```
"Hello" +                 ->       "Hello world"
" world!"
```

2) Nested if's:

```
if (a) {                  ->       if (a && b) {
  if (b) {                             ...
     ...                           }
  }
}
```

3) Remove braces from one line for / if / while:

```
if (fail) {               ->       if (fail) return;
     return;
}
```

4) Kotlin one line method:

```
fun myNumber(): Int {     ->       fun myNumber(): Int = 42
    return 42
}
```

5) Join declaration and initialization:

```
int a;                    ->       int a = 5;
a = 5;
```

6) Chain call:

```
sb.append("a");           ->       sb.append("a").append("b");
sb.append("b");
```

And other functions provided by the plugins.