package com.holub.life.format;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.holub.life.BoardModel;
import com.holub.ui.MenuSite;

/**
 * Grid 메뉴의 Load/Store와 Patterns 메뉴가 쓰는 Swing 코드.
 * 보드 수정은 모두 {@link BoardModel}을 통하고, 형식별 차이는 {@link PatternFormat}이 맡는다.
 */
public final class PatternFileActions {
    private static final String TITLE = "The Game of Life";

    private final Component parent;
    private final BoardModel model;
    private final Runnable beforeChange;
    private final Runnable afterChange;
    private final PatternLibrary library = PatternLibrary.standard();

    /**
     * @param beforeChange 보드를 바꾸기 직전에 부른다(예: 시계 정지).
     * @param afterChange  보드를 바꾼 뒤 부른다(예: 다시 그리기).
     */
    public PatternFileActions(Component parent, BoardModel model,
                              Runnable beforeChange, Runnable afterChange) {
        this.parent = parent;
        this.model = model;
        this.beforeChange = beforeChange;
        this.afterChange = afterChange;
    }

    /** 지원하는 모든 형식을 열 수 있는 불러오기. 읽기에 성공해야 현재 보드를 지운다. */
    public void load() {
        JFileChooser chooser = new JFileChooser(".");
        Map<FileFilter, PatternFormat> filters = new HashMap<>();
        FileNameExtensionFilter all = new FileNameExtensionFilter("All Life files", allExtensions());
        chooser.setFileFilter(all);
        for (PatternFormat format : PatternFormats.all()) {
            FileFilter filter = filterFor(format);
            filters.put(filter, format);
            chooser.addChoosableFileFilter(filter);
        }
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showDialog(parent, "Load") != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        try {
            PatternFormat format = PatternFormats.forFileName(file.getName());
            CellPattern pattern;
            try (InputStream in = new FileInputStream(file)) {
                pattern = format.read(in);
            }
            beforeChange.run();
            model.clear();
            int placed = format.preservesPosition()
                    ? pattern.placeAsIs(model)
                    : pattern.placeCentered(model);
            afterChange.run();
            if (placed < pattern.size()) {
                JOptionPane.showMessageDialog(parent,
                        (pattern.size() - placed) + " of " + pattern.size()
                                + " live cells were outside the " + model.size() + "x" + model.size()
                                + " board and were not loaded.\nSaving now would lose them.",
                        TITLE, JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException e) {
            error("Read Failed!\n" + e.getMessage());
        }
    }

    /** 선택한 형식(또는 파일 확장자)으로 현재 보드를 저장한다. */
    public void store() {
        JFileChooser chooser = new JFileChooser(".");
        Map<FileFilter, PatternFormat> filters = new HashMap<>();
        boolean first = true;
        for (PatternFormat format : PatternFormats.all()) {
            FileFilter filter = filterFor(format);
            filters.put(filter, format);
            chooser.addChoosableFileFilter(filter);
            if (first) {
                chooser.setFileFilter(filter);
                first = false;
            }
        }
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showDialog(parent, "Write") != JFileChooser.APPROVE_OPTION) {
            return;
        }

        PatternFormat format = filters.get(chooser.getFileFilter());
        File selected = chooser.getSelectedFile();
        try {
            PatternFormat byName = null;
            try {
                byName = PatternFormats.forFileName(selected.getName());
            } catch (IOException noKnownExtension) {
                // 확장자가 없거나 낯설면 선택한 형식의 확장자를 붙인다.
            }
            if (byName != null) {
                format = byName;
            }
            File target = new File(selected.getParentFile(),
                    PatternFormats.withDefaultExtension(selected.getName(), format));
            if (target.exists() && JOptionPane.showConfirmDialog(parent,
                    target.getName() + " already exists. Overwrite?", TITLE,
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }

            beforeChange.run();
            CellPattern pattern = CellPattern.fromBoard(baseName(target), model.snapshot());
            try (OutputStream out = new FileOutputStream(target)) {
                format.write(pattern, out);
            }
        } catch (IOException e) {
            error("Write Failed!\n" + e.getMessage());
        }
    }

    /** Patterns 메뉴에 미리 만든 패턴을 등록한다. 고르면 보드 가운데에 놓는다. */
    public void installPresetMenu(Object requester) {
        for (String name : library.names()) {
            MenuSite.addLine(requester, "Patterns", name, e -> placePreset(name));
        }
    }

    private void placePreset(String name) {
        beforeChange.run();
        library.create(name).placeCentered(model);
        afterChange.run();
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(parent, message, TITLE, JOptionPane.ERROR_MESSAGE);
    }

    private static FileFilter filterFor(PatternFormat format) {
        return new FileNameExtensionFilter(format.description(),
                format.extensions().toArray(String[]::new));
    }

    private static String[] allExtensions() {
        return PatternFormats.all().stream()
                .flatMap(format -> format.extensions().stream())
                .toArray(String[]::new);
    }

    private static String baseName(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
