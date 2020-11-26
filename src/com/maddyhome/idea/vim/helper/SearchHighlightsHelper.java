/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.ColorUtil;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.ranges.LineRange;
import com.maddyhome.idea.vim.group.SearchGroup;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;

public class SearchHighlightsHelper {
  public static void updateSearchHighlights(@Nullable String pattern, boolean shouldIgnoreSmartCase, boolean showHighlights, boolean forceUpdate) {
    updateSearchHighlights(pattern, shouldIgnoreSmartCase, showHighlights, -1, null, true, forceUpdate);
  }

  public static int updateIncsearchHighlights(@NotNull Editor editor, @NotNull String pattern, boolean forwards, int caretOffset, @Nullable LineRange searchRange) {
    final int searchStartOffset = searchRange != null ? EditorHelper.getLineStartOffset(editor, searchRange.startLine) : caretOffset;
    final boolean showHighlights = OptionsManager.INSTANCE.getHlsearch().isSet();
    return updateSearchHighlights(pattern, false, showHighlights, searchStartOffset, searchRange, forwards, false);
  }

  @NotNull
  public static RangeHighlighter addSubstitutionConfirmationHighlight(@NotNull Editor editor, int start, int end) {
    TextAttributes color = new TextAttributes(
      editor.getColorsScheme().getColor(EditorColors.SELECTION_FOREGROUND_COLOR),
      editor.getColorsScheme().getColor(EditorColors.SELECTION_BACKGROUND_COLOR),
      editor.getColorsScheme().getColor(EditorColors.CARET_COLOR),
      EffectType.ROUNDED_BOX, Font.PLAIN
    );
    return editor.getMarkupModel().addRangeHighlighter(start, end, HighlighterLayer.SELECTION,
      color, HighlighterTargetArea.EXACT_RANGE);
  }

  /**
   * Refreshes current search highlights for all editors of currently active text editor/document
   */
  private static int updateSearchHighlights(@Nullable String pattern, boolean shouldIgnoreSmartCase, boolean showHighlights,
                                            int initialOffset, @Nullable LineRange searchRange, boolean forwards, boolean forceUpdate) {
    int currentMatchOffset = -1;

    ProjectManager projectManager = ProjectManager.getInstanceIfCreated();
    if (projectManager == null) return currentMatchOffset;
    Project[] projects = projectManager.getOpenProjects();
    for (Project project : projects) {
      Editor current = FileEditorManager.getInstance(project).getSelectedTextEditor();
      Editor[] editors = current == null ? null : EditorFactory.getInstance().getEditors(current.getDocument(), project);
      if (editors == null) {
        continue;
      }

      for (final Editor editor : editors) {
        // Try to keep existing highlights if possible. Update if hlsearch has changed or if the pattern has changed.
        // Force update for the situations where the text is the same, but the ignore case values have changed.
        // E.g. Use `*` to search for a word (which ignores smartcase), then use `/<Up>` to search for the same pattern,
        // which will match smartcase. Or changing the smartcase/ignorecase settings
        if (shouldRemoveSearchHighlights(editor, pattern, showHighlights) || forceUpdate) {
          removeSearchHighlights(editor);
        }

        if (shouldAddAllSearchHighlights(editor, pattern, showHighlights)) {
          // hlsearch (+ incsearch/noincsearch)
          final int startLine = searchRange == null ? 0 : searchRange.startLine;
          final int endLine = searchRange == null ? -1 : searchRange.endLine;
          List<TextRange> results = SearchGroup.findAll(editor, pattern, startLine, endLine,
            SearchHelper.shouldIgnoreCase(pattern, shouldIgnoreSmartCase));
          if (!results.isEmpty()) {
            currentMatchOffset = findClosestMatch(editor, results, initialOffset, forwards);
            highlightSearchResults(editor, pattern, results, currentMatchOffset);
          }
          UserDataManager.setVimLastSearch(editor, pattern);
        }
        else if (shouldAddCurrentMatchSearchHighlight(pattern, showHighlights, initialOffset)) {
          // nohlsearch + incsearch
          final boolean wrap = OptionsManager.INSTANCE.getWrapscan().isSet();
          final EnumSet<SearchGroup.SearchOptions> searchOptions = EnumSet.of(SearchGroup.SearchOptions.WHOLE_FILE);
          if (wrap) searchOptions.add(SearchGroup.SearchOptions.WRAP);
          if (shouldIgnoreSmartCase) searchOptions.add(SearchGroup.SearchOptions.IGNORE_SMARTCASE);
          if (!forwards) searchOptions.add(SearchGroup.SearchOptions.BACKWARDS);
          final TextRange result = SearchGroup.findIt(editor, pattern, initialOffset, 1, searchOptions);
          if (result != null && pattern != null) {
            currentMatchOffset = result.getStartOffset();
            final List<TextRange> results = Collections.singletonList(result);
            highlightSearchResults(editor, pattern, results, currentMatchOffset);
          }
        }
        else if (shouldMaintainCurrentMatchOffset(pattern, initialOffset)) {
          // incsearch. If nothing has changed (e.g. we've edited offset values in `/foo/e+2`) make sure we return the
          // current match offset so the caret remains at the current incsarch match
          final Integer offset = UserDataManager.getVimIncsearchCurrentMatchOffset(editor);
          if (offset != null) {
            currentMatchOffset = offset;
          }
        }
      }
    }

    return currentMatchOffset;
  }

  /**
   * Remove current search highlights if hlSearch is false, or if the pattern is changed
   */
  @Contract("_, _, false -> true; _, null, true -> false")
  private static boolean shouldRemoveSearchHighlights(@NotNull Editor editor, @Nullable String newPattern, boolean hlSearch) {
    return !hlSearch || (newPattern != null && !newPattern.equals(UserDataManager.getVimLastSearch(editor)));
  }

  private static void removeSearchHighlights(@NotNull Editor editor) {
    UserDataManager.setVimLastSearch(editor, null);

    Collection<RangeHighlighter> ehl = UserDataManager.getVimLastHighlighters(editor);
    if (ehl == null) {
      return;
    }

    for (RangeHighlighter rh : ehl) {
      editor.getMarkupModel().removeHighlighter(rh);
    }

    ehl.clear();

    UserDataManager.setVimLastHighlighters(editor, null);
  }

  /**
   * Add search highlights if hlSearch is true and the pattern is changed
   */
  @Contract("_, _, false -> false; _, null, true -> false")
  private static boolean shouldAddAllSearchHighlights(@NotNull Editor editor, @Nullable String newPattern, boolean hlSearch) {
    return hlSearch && newPattern != null && !newPattern.equals(UserDataManager.getVimLastSearch(editor)) && !Objects.equals(newPattern, "");
  }

  private static int findClosestMatch(@NotNull Editor editor, @NotNull List<TextRange> results, int initialOffset, boolean forwards) {
    if (results.isEmpty() || initialOffset == -1) {
      return -1;
    }

    final int size = EditorHelperRt.getFileSize(editor);
    final TextRange max = Collections.max(results, (r1, r2) -> {
      final int d1 = distance(r1, initialOffset, forwards, size);
      final int d2 = distance(r2, initialOffset, forwards, size);
      if (d1 < 0 && d2 >= 0) {
        return Integer.MAX_VALUE;
      }
      return d2 - d1;
    });

    if (!OptionsManager.INSTANCE.getWrapscan().isSet()) {
      final int start = max.getStartOffset();
      if (forwards && start < initialOffset) {
        return -1;
      }
      else if (start >= initialOffset) {
        return -1;
      }
    }

    return max.getStartOffset();
  }

  private static int distance(@NotNull TextRange range, int pos, boolean forwards, int size) {
    final int start = range.getStartOffset();
    if (start <= pos) {
      return forwards ? size - pos + start : pos - start;
    }
    else {
      return forwards ? start - pos : pos + size - start;
    }
  }

  public static void highlightSearchResults(@NotNull Editor editor, @NotNull String pattern, List<TextRange> results,
                                            int currentMatchOffset) {
    Collection<RangeHighlighter> highlighters = UserDataManager.getVimLastHighlighters(editor);
    if (highlighters == null) {
      highlighters = new ArrayList<>();
      UserDataManager.setVimLastHighlighters(editor, highlighters);
    }

    for (TextRange range : results) {
      final boolean current = range.getStartOffset() == currentMatchOffset;
      final RangeHighlighter highlighter = highlightMatch(editor, range.getStartOffset(), range.getEndOffset(), current, pattern);
      highlighters.add(highlighter);
    }

    UserDataManager.setVimIncsearchCurrentMatchOffset(editor, currentMatchOffset);
  }

  private static @NotNull RangeHighlighter highlightMatch(@NotNull Editor editor, int start, int end, boolean current, String tooltip) {
    TextAttributes attributes = editor.getColorsScheme().getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES);
    if (current) {
      // This mimics what IntelliJ does with the Find live preview
      attributes = attributes.clone();
      attributes.setEffectType(EffectType.ROUNDED_BOX);
      attributes.setEffectColor(editor.getColorsScheme().getColor(EditorColors.CARET_COLOR));
    }
    if (attributes.getErrorStripeColor() == null) {
      attributes.setErrorStripeColor(getFallbackErrorStripeColor(attributes, editor.getColorsScheme()));
    }
    final RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(start, end, HighlighterLayer.SELECTION - 1,
      attributes, HighlighterTargetArea.EXACT_RANGE);
    highlighter.setErrorStripeTooltip(tooltip);
    return highlighter;
  }

  /**
   * Return a valid error stripe colour based on editor background
   *
   * <p>Based on HighlightManager#addRangeHighlight behaviour, which we can't use because it will hide highlights
   * when hitting Escape.</p>
   */
  private static @Nullable Color getFallbackErrorStripeColor(TextAttributes attributes, EditorColorsScheme colorsScheme) {
    if (attributes.getBackgroundColor() != null) {
      boolean isDark = ColorUtil.isDark(colorsScheme.getDefaultBackground());
      return isDark ? attributes.getBackgroundColor().brighter() : attributes.getBackgroundColor().darker();
    }
    return null;
  }

  /**
   * Add search highlight for current match if hlsearch is false and we're performing incsearch highlights
   */
  @Contract("_, true, _ -> false")
  private static boolean shouldAddCurrentMatchSearchHighlight(@Nullable String pattern, boolean hlSearch, int initialOffset) {
    return !hlSearch && isIncrementalSearchHighlights(initialOffset) && pattern != null && pattern.length() > 0;
  }

  /**
   * Keep the current match offset if the pattern is still valid and we're performing incremental search highlights
   * This will keep the caret position when editing the offset in e.g. `/foo/e+1`
   */
  @Contract("null, _ -> false")
  private static boolean shouldMaintainCurrentMatchOffset(@Nullable String pattern, int initialOffset) {
    return pattern != null && pattern.length() > 0 && isIncrementalSearchHighlights(initialOffset);
  }

  /**
   * initialOffset is only valid if we're highlighting incsearch
   */
  @Contract(pure = true)
  private static boolean isIncrementalSearchHighlights(int initialOffset) {
    return initialOffset != -1;
  }
}
