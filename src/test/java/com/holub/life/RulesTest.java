package com.holub.life;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RulesTest {
    @Test
    void conwayRuleGivesTheSameAnswerAsTheOriginalExpression() {
        for (int neighbors = 0; neighbors <= 8; neighbors++) {
            for (boolean alive : new boolean[] {false, true}) {
                // 원본 Resident.figureNextState()에 있던 식
                boolean original = neighbors == 3 || (alive && neighbors == 2);
                assertEquals(original, Rules.CONWAY.nextState(alive, neighbors),
                        "alive=" + alive + ", neighbors=" + neighbors);
            }
        }
    }

    @Test
    void highLifeAlsoGivesBirthWithSixNeighbors() {
        assertTrue(Rules.HIGHLIFE.nextState(false, 6));
        assertFalse(Rules.CONWAY.nextState(false, 6));
        assertTrue(Rules.HIGHLIFE.nextState(false, 3));
        assertTrue(Rules.HIGHLIFE.nextState(true, 2));
        assertFalse(Rules.HIGHLIFE.nextState(true, 6));
    }

    @Test
    void seedsNeverLetsALiveCellSurvive() {
        for (int neighbors = 0; neighbors <= 8; neighbors++) {
            assertFalse(Rules.SEEDS.nextState(true, neighbors), "neighbors=" + neighbors);
            assertEquals(neighbors == 2, Rules.SEEDS.nextState(false, neighbors), "neighbors=" + neighbors);
        }
    }

    @Test
    void dayAndNightFollowsItsBirthAndSurvivalSets() {
        List<Integer> birth = List.of(3, 6, 7, 8);
        List<Integer> survival = List.of(3, 4, 6, 7, 8);
        for (int neighbors = 0; neighbors <= 8; neighbors++) {
            assertEquals(birth.contains(neighbors), Rules.DAY_AND_NIGHT.nextState(false, neighbors));
            assertEquals(survival.contains(neighbors), Rules.DAY_AND_NIGHT.nextState(true, neighbors));
        }
    }

    @Test
    void presetsAreListedInMenuOrderStartingWithConway() {
        assertEquals(List.of("Conway", "HighLife", "Seeds", "Day & Night"),
                List.copyOf(Rules.presets().keySet()));
        assertEquals(List.of("B3/S23", "B36/S23", "B2/S", "B3678/S34678"),
                Rules.presets().values().stream().map(Rule::notation).toList());
        assertThrows(UnsupportedOperationException.class,
                () -> Rules.presets().put("Mine", Rules.CONWAY));
    }

    @Test
    void parseIgnoresCaseSpacesDigitOrderAndRepeatedDigits() {
        assertEquals(Rules.HIGHLIFE, Rules.parse("b36/s23"));
        assertEquals(Rules.HIGHLIFE, Rules.parse("  B63 / S32 "));
        assertEquals("B3/S23", Rules.parse("B33/S322").notation());
        assertEquals(Rules.CONWAY.hashCode(), Rules.parse("B33/S322").hashCode());
    }

    @Test
    void parseAcceptsEmptySurvivalAndEmptyBirthSets() {
        assertEquals(Rules.SEEDS, Rules.parse("B2/S"));
        Rule noBirth = Rules.parse("B/S23");
        assertEquals("B/S23", noBirth.notation());
        assertFalse(noBirth.nextState(false, 3));
        assertTrue(noBirth.nextState(true, 3));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"3/23", "B3S23", "S23/B3", "B9/S23", "B3/S2x", "B3/S23/1", "B-1/S2", "Conway"})
    void parseRejectsNotationThatIsNotBirthSlashSurvival(String notation) {
        assertThrows(IllegalArgumentException.class, () -> Rules.parse(notation));
    }

    @Test
    void parseRejectsBirthWithoutNeighbors() {
        // 빈 구역의 계산을 건너뛰는 보드에서는 B0 규칙을 계산할 수 없다.
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> Rules.parse("B03/S23"));
        assertTrue(error.getMessage().contains("B0"));
    }
}
