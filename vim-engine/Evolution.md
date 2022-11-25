# Vim engine evolution and improvements

- We have a mechanism to understand if the command is writable or not. However, this mechanism requires an
  instance of the editor, what makes idea of splitting editor into mutable and immutable useless.
  We need an additional step in our engine to determine if the command is writable without acquiring the editor.
- At the moment, we can move the caret at any moment of the code. The caret is mutable, so in the code you rely on the
  fact that the caret has changed. This is not true for the Fleet editor, where the caret movement is in fact creation
  of the new caret.
  - Also, we can move the caret to any position at any moment of time. This would be better if the actions were only
    calculating the new position and this change was applied at once. This is already work very similar to that,
    but this approach should be reviewed with the fact that we create a new caret instead of moving the old one.