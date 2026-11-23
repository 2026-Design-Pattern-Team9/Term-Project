package com.holub.life.format;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 미리 만들어 둔 대표 패턴(원형)을 이름으로 보관한다.
 * {@link #create(String)}는 원형을 복제해 돌려주므로 회전·뒤집기를 해도 원형은 그대로다.
 */
public final class PatternLibrary {
    private final Map<String, CellPattern> prototypes = new LinkedHashMap<>();

    public PatternLibrary register(CellPattern prototype) {
        prototypes.put(prototype.name(), prototype.copy().normalize());
        return this;
    }

    public List<String> names() {
        return List.copyOf(prototypes.keySet());
    }

    public CellPattern create(String name) {
        CellPattern prototype = prototypes.get(name);
        if (prototype == null) {
            throw new IllegalArgumentException("Unknown pattern: " + name);
        }
        return prototype.copy();
    }

    public static PatternLibrary standard() {
        return new PatternLibrary()
                .register(CellPattern.of("Block", "OO", "OO"))
                .register(CellPattern.of("Blinker", "OOO"))
                .register(CellPattern.of("Toad", ".OOO", "OOO."))
                .register(CellPattern.of("Beacon", "OO..", "OO..", "..OO", "..OO"))
                .register(CellPattern.of("Glider", ".O.", "..O", "OOO"))
                .register(CellPattern.of("R-pentomino", ".OO", "OO.", ".O."))
                .register(CellPattern.of("Lightweight spaceship",
                        ".O..O", "O....", "O...O", "OOOO."))
                .register(CellPattern.of("Gosper glider gun",
                        "........................O...........",
                        "......................O.O...........",
                        "............OO......OO............OO",
                        "...........O...O....OO............OO",
                        "OO........O.....O...OO..............",
                        "OO........O...O.OO....O.O...........",
                        "..........O.....O.......O...........",
                        "...........O...O....................",
                        "............OO......................"));
    }
}
