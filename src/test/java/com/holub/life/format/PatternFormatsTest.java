package com.holub.life.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.holub.life.BoardModel;

import static org.junit.jupiter.api.Assertions.*;

class PatternFormatsTest {
    private static final CellPattern GLIDER = CellPattern.of("Glider", ".O.", "..O", "OOO");

    private static ByteArrayInputStream text(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static CellPattern roundTrip(PatternFormat format, CellPattern pattern) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.write(pattern, out);
        return format.read(new ByteArrayInputStream(out.toByteArray()));
    }

    // ---- 형식 선택(Factory) ----

    @Test
    void factoryPicksFormatByExtensionIgnoringCase() throws IOException {
        assertInstanceOf(LifeFileFormat.class, PatternFormats.forFileName("a.life"));
        assertInstanceOf(PlaintextFormat.class, PatternFormats.forFileName("a.cells"));
        assertInstanceOf(RleFormat.class, PatternFormats.forFileName("glider.RLE"));
    }

    @Test
    void factoryRejectsUnknownOrMissingExtension() {
        assertThrows(IOException.class, () -> PatternFormats.forFileName("a.png"));
        assertThrows(IOException.class, () -> PatternFormats.forFileName("noextension"));
    }

    @Test
    void defaultExtensionIsAddedOnlyWhenMissing() {
        PatternFormat rle = new RleFormat();
        assertEquals("a.rle", PatternFormats.withDefaultExtension("a", rle));
        assertEquals("a.rle", PatternFormats.withDefaultExtension("a.rle", rle));
        assertEquals("a.txt.rle", PatternFormats.withDefaultExtension("a.txt", rle));
    }

    // ---- RLE ----

    @Test
    void rleReadsTheStandardGliderText() throws IOException {
        CellPattern read = new RleFormat().read(text(
                "#N Glider\nx = 3, y = 3, rule = B3/S23\nbob$2bo$3o!\n"));
        assertEquals(GLIDER, read);
        assertEquals("Glider", read.name());
    }

    @Test
    void rleRoundTripsEveryPresetIncludingEmptyRows() throws IOException {
        PatternLibrary library = PatternLibrary.standard();
        for (String name : library.names()) {
            CellPattern original = library.create(name);
            assertEquals(original, roundTrip(new RleFormat(), original), name);
        }
        CellPattern gap = CellPattern.of("gap", "O.O", "...", "...", "O..");
        assertEquals(gap, roundTrip(new RleFormat(), gap));
        assertTrue(roundTrip(new RleFormat(), new CellPattern("empty")).isEmpty());
    }

    @Test
    void rleRejectsMalformedFiles() {
        RleFormat rle = new RleFormat();
        assertThrows(IOException.class, () -> rle.read(text("bob$2bo$3o!\n")));          // 헤더 없음
        assertThrows(IOException.class, () -> rle.read(text("x = 3, y = 3\nbob$2bo$3o\n"))); // ! 없음
        assertThrows(IOException.class, () -> rle.read(text("x = 3, y = 3\nbXb!\n")));   // 잘못된 문자
        assertThrows(IOException.class, () -> rle.read(text("x = 1, y = 1\n99999999999o!\n"))); // 너무 긴 반복
    }

    // ---- 규칙 헤더 ----

    @Test
    void rleKeepsTheRuleWrittenInTheHeader() throws IOException {
        assertEquals("B36/S23", new RleFormat().read(text("x = 1, y = 1, rule = B36/S23\no!\n")).rule());
        assertEquals("B36/S23", new RleFormat().read(text("x = 1, y = 1,rule=B36/S23\no!\n")).rule());
        assertNull(new RleFormat().read(text("x = 1, y = 1\no!\n")).rule());
    }

    @Test
    void rleWritesTheRuleOfThePatternAndDefaultsToConway() throws IOException {
        CellPattern highLife = CellPattern.of("p", "O").withRule("B36/S23");
        assertEquals("B36/S23", roundTrip(new RleFormat(), highLife).rule());
        assertEquals("B3/S23", roundTrip(new RleFormat(), CellPattern.of("p", "O")).rule());
        assertEquals("B36/S23", highLife.copy().rule());
    }

    @Test
    void fileRuleIsAppliedQuietlyWhenItDiffersFromTheBoardRule() {
        BoardModel model = new BoardModel();
        model.setRule(com.holub.life.Rules.SEEDS);

        PatternFileActions.RuleChange change = PatternFileActions.applyFileRule(model,
                CellPattern.of("p", "O").withRule("B3/S23"));

        assertEquals(com.holub.life.Rules.CONWAY, model.rule());
        assertNull(change.message());
        assertFalse(change.unsupported());
    }

    @Test
    void lifeIsAnotherNameForConwayAndMissingRuleChangesNothing() {
        BoardModel model = new BoardModel();
        model.setRule(com.holub.life.Rules.HIGHLIFE);

        PatternFileActions.applyFileRule(model, CellPattern.of("p", "O"));
        assertEquals(com.holub.life.Rules.HIGHLIFE, model.rule());

        PatternFileActions.applyFileRule(model, CellPattern.of("p", "O").withRule("Life"));
        assertEquals(com.holub.life.Rules.CONWAY, model.rule());
    }

    @Test
    void unsupportedFileRuleKeepsTheCurrentRuleAndWarns() {
        BoardModel model = new BoardModel();
        model.setRule(com.holub.life.Rules.SEEDS);

        for (String written : new String[] {"23/3", "B0/S", "Generations", "B3/S23V"}) {
            PatternFileActions.RuleChange change = PatternFileActions.applyFileRule(model,
                    CellPattern.of("p", "O").withRule(written));
            assertEquals(com.holub.life.Rules.SEEDS, model.rule(), written);
            assertTrue(change.unsupported(), written);
            assertTrue(change.message().contains(written), written);
        }
    }

    // ---- Plaintext ----

    @Test
    void plaintextSkipsCommentsAndReadsName() throws IOException {
        CellPattern read = new PlaintextFormat().read(text("!Name: Glider\n!comment\n.O.\n..O\nOOO\n"));
        assertEquals(GLIDER, read);
        assertEquals("Glider", read.name());
    }

    @Test
    void plaintextRoundTripsAndRejectsOtherCharacters() throws IOException {
        for (String name : PatternLibrary.standard().names()) {
            CellPattern original = PatternLibrary.standard().create(name);
            assertEquals(original, roundTrip(new PlaintextFormat(), original), name);
        }
        assertThrows(IOException.class, () -> new PlaintextFormat().read(text(".X.\n")));
    }

    // ---- 기존 .life 파일(Adapter) ----

    @Test
    void lifeFormatReadsFilesWrittenByTheOriginalStore() throws IOException {
        BoardModel model = new BoardModel();
        model.setAlive(3, 5, true);
        model.setAlive(10, 20, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        model.store(out);

        CellPattern read = new LifeFileFormat().read(new ByteArrayInputStream(out.toByteArray()));

        assertEquals(2, read.size());
        assertTrue(read.contains(5, 3));    // x = 열, y = 행
        assertTrue(read.contains(20, 10));
    }

    @Test
    void originalLoadReadsFilesWrittenByLifeFormat() throws IOException {
        CellPattern pattern = new CellPattern("p");
        pattern.add(7, 2);
        pattern.add(8, 2);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new LifeFileFormat().write(pattern, out);

        BoardModel model = new BoardModel();
        model.load(new ByteArrayInputStream(out.toByteArray()));

        assertTrue(model.isAlive(2, 7));
        assertTrue(model.isAlive(2, 8));
        assertFalse(model.isAlive(2, 9));
    }

    @Test
    void lifeFormatRefusesUnexpectedSerializedClasses() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream sink = new ObjectOutputStream(out)) {
            sink.writeObject(new HashMap<String, String>());
        }
        assertThrows(IOException.class,
                () -> new LifeFileFormat().read(new ByteArrayInputStream(out.toByteArray())));
        assertThrows(IOException.class,
                () -> new LifeFileFormat().read(text("not a serialized stream")));
    }

    // ---- 프리셋(Prototype) ----

    @Test
    void createReturnsIndependentCopiesOfThePrototype() {
        PatternLibrary library = PatternLibrary.standard();
        CellPattern first = library.create("Glider");
        first.rotateClockwise().flipHorizontal();
        first.add(50, 50);

        assertEquals(GLIDER, library.create("Glider"));
    }

    @Test
    void fourRotationsAndTwoFlipsReturnToTheOriginalShape() {
        CellPattern pattern = PatternLibrary.standard().create("Lightweight spaceship");
        CellPattern original = pattern.copy();
        for (int i = 0; i < 4; i++) {
            pattern.rotateClockwise();
        }
        assertEquals(original, pattern);
        assertEquals(original, pattern.flipHorizontal().flipHorizontal());
    }

    @Test
    void libraryHasTheGosperGunAndRejectsUnknownNames() {
        CellPattern gun = PatternLibrary.standard().create("Gosper glider gun");
        assertEquals(36, gun.size());
        assertEquals(36, gun.width());
        assertEquals(9, gun.height());
        assertThrows(IllegalArgumentException.class, () -> PatternLibrary.standard().create("nope"));
    }

    // ---- 보드에 놓기 ----

    @Test
    void placeOnAddsCellsWithoutTurningOffExistingOnes() {
        BoardModel model = new BoardModel();
        model.setAlive(10, 10, true);

        int placed = GLIDER.copy().placeOn(model, 10, 10);

        assertEquals(5, placed);
        assertTrue(model.isAlive(10, 10), "이미 살아 있던 셀은 그대로여야 한다");
        assertTrue(model.isAlive(10, 11));
        assertTrue(model.isAlive(12, 12));
    }

    @Test
    void placeOnSkipsCellsOutsideTheBoard() {
        BoardModel model = new BoardModel();
        int last = model.size() - 1;

        int placed = GLIDER.copy().placeOn(model, last - 1, last - 1);

        assertEquals(1, placed);
        assertTrue(model.isAlive(last - 1, last));
        assertEquals(1, CellPattern.fromBoard("b", model.snapshot()).size());
    }

    @Test
    void placementReportsHowManyCellsDidNotFitOnTheBoard() throws IOException {
        BoardModel model = new BoardModel();
        CellPattern wide = new RleFormat().read(text("x = 100, y = 1, rule = B3/S23\n100o!\n"));

        assertEquals(100, wide.size());
        assertEquals(model.size(), wide.placeCentered(model));
    }

    @Test
    void centeredPlacementUsesTheBoundingBoxMiddle() {
        BoardModel model = new BoardModel();
        CellPattern block = PatternLibrary.standard().create("Block");

        block.placeCentered(model);

        int start = model.size() / 2 - 1;
        assertTrue(model.isAlive(start, start));
        assertTrue(model.isAlive(start + 1, start + 1));
        assertEquals(4, CellPattern.fromBoard("b", model.snapshot()).size());
    }

    @Test
    void placedGliderMovesDiagonallyAfterFourGenerations() {
        BoardModel model = new BoardModel();
        GLIDER.copy().placeOn(model, 20, 20);
        for (int i = 0; i < 4; i++) {
            model.step();
        }

        CellPattern after = CellPattern.fromBoard("g", model.snapshot());
        assertEquals(GLIDER, after.normalize());
        assertTrue(model.isAlive(21, 22));
    }

    @Test
    void boardCanBeStoredInAnyFormatAndLoadedBack() throws IOException {
        BoardModel model = new BoardModel();
        PatternLibrary.standard().create("R-pentomino").placeOn(model, 5, 9);
        CellPattern saved = CellPattern.fromBoard("board", model.snapshot());

        for (PatternFormat format : PatternFormats.all()) {
            CellPattern read = roundTrip(format, saved);
            BoardModel loaded = new BoardModel();
            if (format.preservesPosition()) {
                read.placeAsIs(loaded);
                assertEquals(saved, CellPattern.fromBoard("board", loaded.snapshot()), format.description());
            } else {
                assertEquals(saved.copy().normalize(), read.copy().normalize(), format.description());
            }
        }
    }
}
