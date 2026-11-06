package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@ModuleInfo(name = "ScoreboardTweaks", category = ModuleCategory.VISUALS, desc = "Modify scoreboard line text with find/replace rules.")
public class ScoreboardTweaks extends BaseModule {
    /** Legacy formatting codes ("§" + one char); compiled once instead of per call. */
    private static final Pattern FORMATTING = Pattern.compile("§.");
    private static final Pattern LEADING_SPACES = Pattern.compile("^\\s*");

    public static String stripFormatting(String text) {
        return FORMATTING.matcher(text).replaceAll("");
    }


    public final BooleanSetting hideNumbers = new BooleanSetting(this, "Hide Numbers")
            .enable();
    public final BooleanSetting hideTitle = new BooleanSetting(this, "Hide Title");

    public final StringSetting pattern1 = new StringSetting(this, "Find 1").text("");
    public final StringSetting replace1 = new StringSetting(this, "Replace 1").text("");
    public final StringSetting pattern2 = new StringSetting(this, "Find 2", () -> pattern1.getText().isEmpty()).text("");
    public final StringSetting replace2 = new StringSetting(this, "Replace 2", () -> pattern1.getText().isEmpty()).text("");
    public final StringSetting pattern3 = new StringSetting(this, "Find 3", () -> pattern2.getText().isEmpty() || !pattern2.isVisible()).text("");
    public final StringSetting replace3 = new StringSetting(this, "Replace 3", () -> pattern2.getText().isEmpty() || !pattern2.isVisible()).text("");
    public final StringSetting pattern4 = new StringSetting(this, "Find 4", () -> pattern3.getText().isEmpty() || !pattern3.isVisible()).text("");
    public final StringSetting replace4 = new StringSetting(this, "Replace 4", () -> pattern3.getText().isEmpty() || !pattern3.isVisible()).text("");
    public final StringSetting pattern5 = new StringSetting(this, "Find 5", () -> pattern4.getText().isEmpty() || !pattern4.isVisible()).text("");
    public final StringSetting replace5 = new StringSetting(this, "Replace 5", () -> pattern4.getText().isEmpty() || !pattern4.isVisible()).text("");
    public final StringSetting pattern6 = new StringSetting(this, "Find 6", () -> pattern5.getText().isEmpty() || !pattern5.isVisible()).text("");
    public final StringSetting replace6 = new StringSetting(this, "Replace 6", () -> pattern5.getText().isEmpty() || !pattern5.isVisible()).text("");
    public final StringSetting pattern7 = new StringSetting(this, "Find 7", () -> pattern6.getText().isEmpty() || !pattern6.isVisible()).text("");
    public final StringSetting replace7 = new StringSetting(this, "Replace 7", () -> pattern6.getText().isEmpty() || !pattern6.isVisible()).text("");
    public final StringSetting pattern8 = new StringSetting(this, "Find 8", () -> pattern7.getText().isEmpty() || !pattern7.isVisible()).text("");
    public final StringSetting replace8 = new StringSetting(this, "Replace 8", () -> pattern7.getText().isEmpty() || !pattern7.isVisible()).text("");
    public final StringSetting pattern9 = new StringSetting(this, "Find 9", () -> pattern8.getText().isEmpty() || !pattern8.isVisible()).text("");
    public final StringSetting replace9 = new StringSetting(this, "Replace 9", () -> pattern8.getText().isEmpty() || !pattern8.isVisible()).text("");
    public final StringSetting pattern10 = new StringSetting(this, "Find 10", () -> pattern9.getText().isEmpty() || !pattern9.isVisible()).text("");
    public final StringSetting replace10 = new StringSetting(this, "Replace 10", () -> pattern9.getText().isEmpty() || !pattern9.isVisible()).text("");
    public final StringSetting pattern11 = new StringSetting(this, "Find 11", () -> pattern10.getText().isEmpty() || !pattern10.isVisible()).text("");
    public final StringSetting replace11 = new StringSetting(this, "Replace 11", () -> pattern10.getText().isEmpty() || !pattern10.isVisible()).text("");
    public final StringSetting pattern12 = new StringSetting(this, "Find 12", () -> pattern11.getText().isEmpty() || !pattern11.isVisible()).text("");
    public final StringSetting replace12 = new StringSetting(this, "Replace 12", () -> pattern11.getText().isEmpty() || !pattern11.isVisible()).text("");
    public final StringSetting pattern13 = new StringSetting(this, "Find 13", () -> pattern12.getText().isEmpty() || !pattern12.isVisible()).text("");
    public final StringSetting replace13 = new StringSetting(this, "Replace 13", () -> pattern12.getText().isEmpty() || !pattern12.isVisible()).text("");
    public final StringSetting pattern14 = new StringSetting(this, "Find 14", () -> pattern13.getText().isEmpty() || !pattern13.isVisible()).text("");
    public final StringSetting replace14 = new StringSetting(this, "Replace 14", () -> pattern13.getText().isEmpty() || !pattern13.isVisible()).text("");
    public final StringSetting pattern15 = new StringSetting(this, "Find 15", () -> pattern14.getText().isEmpty() || !pattern14.isVisible()).text("");
    public final StringSetting replace15 = new StringSetting(this, "Replace 15", () -> pattern14.getText().isEmpty() || !pattern14.isVisible()).text("");
    public final StringSetting pattern16 = new StringSetting(this, "Find 16", () -> pattern15.getText().isEmpty() || !pattern15.isVisible()).text("");
    public final StringSetting replace16 = new StringSetting(this, "Replace 16", () -> pattern15.getText().isEmpty() || !pattern15.isVisible()).text("");
    public final StringSetting pattern17 = new StringSetting(this, "Find 17", () -> pattern16.getText().isEmpty() || !pattern16.isVisible()).text("");
    public final StringSetting replace17 = new StringSetting(this, "Replace 17", () -> pattern16.getText().isEmpty() || !pattern16.isVisible()).text("");
    public final StringSetting pattern18 = new StringSetting(this, "Find 18", () -> pattern17.getText().isEmpty() || !pattern17.isVisible()).text("");
    public final StringSetting replace18 = new StringSetting(this, "Replace 18", () -> pattern17.getText().isEmpty() || !pattern17.isVisible()).text("");
    public final StringSetting pattern19 = new StringSetting(this, "Find 19", () -> pattern18.getText().isEmpty() || !pattern18.isVisible()).text("");
    public final StringSetting replace19 = new StringSetting(this, "Replace 19", () -> pattern18.getText().isEmpty() || !pattern18.isVisible()).text("");
    public final StringSetting pattern20 = new StringSetting(this, "Find 20", () -> pattern19.getText().isEmpty() || !pattern19.isVisible()).text("");
    public final StringSetting replace20 = new StringSetting(this, "Replace 20", () -> pattern19.getText().isEmpty() || !pattern19.isVisible()).text("");

    private static final class Rule {
        final Pattern pattern;
        final String replacement;
        final boolean isKeyValue;
        final Pattern keyPattern;

        Rule(Pattern pattern, String replacement, boolean isKeyValue, Pattern keyPattern) {
            this.pattern = pattern;
            this.replacement = replacement;
            this.isKeyValue = isKeyValue;
            this.keyPattern = keyPattern;
        }
    }

    private List<Rule> compiledRules = new ArrayList<>();
    private long lastCompile = 0;

    private void recompileIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCompile < 200) return;
        lastCompile = now;

        List<Rule> rules = new ArrayList<>();
        addRule(rules, pattern1.getText(), replace1.getText());
        addRule(rules, pattern2.getText(), replace2.getText());
        addRule(rules, pattern3.getText(), replace3.getText());
        addRule(rules, pattern4.getText(), replace4.getText());
        addRule(rules, pattern5.getText(), replace5.getText());
        addRule(rules, pattern6.getText(), replace6.getText());
        addRule(rules, pattern7.getText(), replace7.getText());
        addRule(rules, pattern8.getText(), replace8.getText());
        addRule(rules, pattern9.getText(), replace9.getText());
        addRule(rules, pattern10.getText(), replace10.getText());
        addRule(rules, pattern11.getText(), replace11.getText());
        addRule(rules, pattern12.getText(), replace12.getText());
        addRule(rules, pattern13.getText(), replace13.getText());
        addRule(rules, pattern14.getText(), replace14.getText());
        addRule(rules, pattern15.getText(), replace15.getText());
        addRule(rules, pattern16.getText(), replace16.getText());
        addRule(rules, pattern17.getText(), replace17.getText());
        addRule(rules, pattern18.getText(), replace18.getText());
        addRule(rules, pattern19.getText(), replace19.getText());
        addRule(rules, pattern20.getText(), replace20.getText());
        compiledRules = rules;
    }

    private void addRule(List<Rule> rules, String pat, String rep) {
        if (pat == null || pat.isEmpty()) return;
        boolean isKeyValue = pat.endsWith(":");
        Pattern keyPattern = null;
        if (isKeyValue) {
            String cleanKey = pat.substring(0, pat.length() - 1).trim();
            try {
                keyPattern = Pattern.compile(cleanKey, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                keyPattern = Pattern.compile(Pattern.quote(cleanKey), Pattern.CASE_INSENSITIVE);
            }
        }

        Pattern pattern;
        try {
            pattern = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            pattern = Pattern.compile(Pattern.quote(pat), Pattern.CASE_INSENSITIVE);
        }
        rules.add(new Rule(pattern, rep != null ? rep : "", isKeyValue, keyPattern));
    }

    /**
     * Apply all rules to a scoreboard line text.
     */
    public Text applyRules(Text original) {
        if (!isEnabled()) return original;
        recompileIfNeeded();

        // 1. Flatten the text into individual styled leaf components
        List<MutableText> components = new ArrayList<>();
        collectLeaves(original, components, Style.EMPTY);

        if (components.isEmpty()) return original;

        // 2. Build flatString and starts array
        int[] starts = new int[components.size()];
        String flatString = rebuildFlatString(components, starts);
        String originalFlat = flatString;

        // 3. Apply each rule to the components list
        for (Rule rule : compiledRules) {
            if (rule.isKeyValue) {
                if (flatString.contains(":")) {
                    int colonIndex = flatString.indexOf(':');
                    String part1 = flatString.substring(0, colonIndex);
                    String part2 = flatString.substring(colonIndex + 1);

                    String strippedKey = part1.transform(ScoreboardTweaks::stripFormatting).trim();
                    if (rule.keyPattern.matcher(strippedKey).find()) {
                        // Extract leading spaces
                        Pattern leadingPattern = LEADING_SPACES;
                        Matcher m = leadingPattern.matcher(part2);
                        String leading = "";
                        if (m.find()) {
                            leading = m.group();
                        }

                        int replaceStart = colonIndex + 1 + leading.length();
                        int replaceEnd = flatString.length();

                        replaceSubstringInComponents(components, starts, replaceStart, replaceEnd, rule.replacement);
                        flatString = rebuildFlatString(components, starts);
                    }
                }
            } else {
                List<MatchRange> matches = new ArrayList<>();
                Matcher m = rule.pattern.matcher(flatString);
                while (m.find()) {
                    matches.add(new MatchRange(m.start(), m.end(), rule.replacement));
                }

                // Apply matches right-to-left
                for (int i = matches.size() - 1; i >= 0; i--) {
                    MatchRange match = matches.get(i);
                    replaceSubstringInComponents(components, starts, match.start, match.end, match.replacement);
                }

                flatString = rebuildFlatString(components, starts);
            }
        }

        // If no replacement actually changed the text, return original to preserve object identity
        if (flatString.equals(originalFlat)) return original;

        // 4. Reconstruct the Text by appending all components as siblings
        MutableText resultText = Text.empty();
        for (MutableText c : components) {
            resultText.append(c);
        }
        return resultText;
    }

    private static void collectLeaves(Text text, List<MutableText> list, Style activeStyle) {
        Style currentStyle = text.getStyle().withParent(activeStyle);
        String selfText = getSelfText(text);
        if (!selfText.isEmpty()) {
            list.add(Text.literal(selfText).setStyle(currentStyle));
        }
        for (Text sibling : text.getSiblings()) {
            collectLeaves(sibling, list, currentStyle);
        }
    }

    private static String getSelfText(Text text) {
        StringBuilder sb = new StringBuilder();
        text.getContent().visit(s -> {
            sb.append(s);
            return java.util.Optional.empty();
        });
        return sb.toString();
    }

    private static String rebuildFlatString(List<MutableText> components, int[] starts) {
        StringBuilder sb = new StringBuilder();
        int currentLength = 0;
        for (int i = 0; i < components.size(); i++) {
            starts[i] = currentLength;
            String text = components.get(i).getString();
            sb.append(text);
            currentLength += text.length();
        }
        return sb.toString();
    }

    private static void replaceSubstringInComponents(List<MutableText> components, int[] starts, int replaceStart, int replaceEnd, String newText) {
        boolean firstFound = false;
        for (int i = 0; i < components.size(); i++) {
            int start = starts[i];
            int end = start + components.get(i).getString().length();

            int overlapStart = Math.max(start, replaceStart);
            int overlapEnd = Math.min(end, replaceEnd);

            if (overlapStart < overlapEnd) {
                String originalText = components.get(i).getString();
                int leftLen = Math.max(0, replaceStart - start);
                String left = originalText.substring(0, Math.min(originalText.length(), leftLen));

                int rightStart = Math.max(0, replaceEnd - start);
                String right = rightStart < originalText.length() ? originalText.substring(rightStart) : "";

                if (!firstFound) {
                    components.set(i, Text.literal(left + newText + right).setStyle(components.get(i).getStyle()));
                    firstFound = true;
                } else {
                    components.set(i, Text.literal(left + right).setStyle(components.get(i).getStyle()));
                }
            }
        }
    }

    private static final class MatchRange {
        final int start;
        final int end;
        final String replacement;

        MatchRange(int start, int end, String replacement) {
            this.start = start;
            this.end = end;
            this.replacement = replacement;
        }
    }

    public boolean shouldHideLine(Text text) {
        if (!isEnabled()) return false;
        recompileIfNeeded();

        String stripped = text.getString().transform(ScoreboardTweaks::stripFormatting).trim();
        for (Rule rule : compiledRules) {
            if (rule.replacement.isEmpty()) {
                if (rule.isKeyValue && text.getString().contains(":")) {
                    int colonIndex = text.getString().indexOf(':');
                    String part1 = text.getString().substring(0, colonIndex);
                    String strippedKey = part1.transform(ScoreboardTweaks::stripFormatting).trim();
                    if (rule.keyPattern.matcher(strippedKey).find()) {
                        return true;
                    }
                } else if (rule.pattern.matcher(stripped).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getRulesCount() {
        return compiledRules.size();
    }

    public String getRulesString() {
        StringBuilder sb = new StringBuilder("[");
        for (Rule r : compiledRules) {
            sb.append(r.pattern.pattern()).append("->").append(r.replacement).append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
