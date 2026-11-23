package com.holub.life;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 미리 정의한 규칙과, B/S 표기에서 규칙을 만드는 정적 팩토리. */
public final class Rules {
    private static final Pattern NOTATION = Pattern.compile("B([0-8]*)/S([0-8]*)");

    public static final Rule CONWAY = parse("B3/S23");
    public static final Rule HIGHLIFE = parse("B36/S23");
    public static final Rule SEEDS = parse("B2/S");
    public static final Rule DAY_AND_NIGHT = parse("B3678/S34678");

    private static final Map<String, Rule> PRESETS;
    static {
        Map<String, Rule> presets = new LinkedHashMap<>();
        presets.put("Conway", CONWAY);
        presets.put("HighLife", HIGHLIFE);
        presets.put("Seeds", SEEDS);
        presets.put("Day & Night", DAY_AND_NIGHT);
        PRESETS = Collections.unmodifiableMap(presets);
    }

    private Rules() {
    }

    /** 이름과 규칙을 메뉴에 보일 순서대로 돌려준다. 여기에 추가한 규칙은 메뉴에도 나타난다. */
    public static Map<String, Rule> presets() {
        return PRESETS;
    }

    /**
     * "B36/S23" 형식의 표기를 규칙으로 바꾼다. 대소문자와 공백은 구분하지 않는다.
     *
     * @throws IllegalArgumentException 표기가 잘못됐거나 지원하지 않는 규칙일 때
     */
    public static Rule parse(String notation) {
        if (notation == null) {
            throw new IllegalArgumentException("Rule is missing");
        }
        Matcher matcher = NOTATION.matcher(notation.replaceAll("\\s", "").toUpperCase());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Rule must look like B3/S23 with neighbor counts from 0 to 8: " + notation);
        }
        return new LifeLikeRule(mask(matcher.group(1)), mask(matcher.group(2)));
    }

    private static int mask(String digits) {
        int mask = 0;
        for (char digit : digits.toCharArray()) {
            mask |= 1 << (digit - '0');
        }
        return mask;
    }
}
