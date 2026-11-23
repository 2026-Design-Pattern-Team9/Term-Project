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
import com.holub.life.Rule;
import com.holub.life.Rules;
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
            RuleChange ruleChange = applyFileRule(model, pattern);
            model.clear();
            int placed = format.preservesPosition()
                    ? pattern.placeAsIs(model)
                    : pattern.placeCentered(model);
            afterChange.run();

            StringBuilder notes = new StringBuilder();
            if (ruleChange.message() != null) {
                notes.append(ruleChange.message());
            }
            if (placed < pattern.size()) {
                if (notes.length() > 0) {
                    notes.append("\n\n");
                }
                notes.append(pattern.size() - placed).append(" of ").append(pattern.size())
                        .append(" live cells were outside the ")
                        .append(model.size()).append("x").append(model.size())
                        .append(" board and were not loaded.\nSaving now would lose them.");
            }
            if (notes.length() > 0) {
                boolean warning = placed < pattern.size() || ruleChange.unsupported();
                JOptionPane.showMessageDialog(parent, notes.toString(), TITLE,
                        warning ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
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
            CellPattern pattern = CellPattern.fromBoard(baseName(target), model.snapshot())
                    .withRule(model.rule().notation());
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

    /** 사용자에게 알릴 문구(없으면 null). 파일의 규칙을 쓰지 못했을 때만 문구가 있다. */
    record RuleChange(String message, boolean unsupported) {
        static final RuleChange NONE = new RuleChange(null, false);
    }

    /**
     * 파일에 규칙이 적혀 있고 현재 규칙과 다르면 보드의 규칙을 파일의 규칙으로 조용히 바꾼다.
     * 해석할 수 없는 규칙이면 현재 규칙을 그대로 두고 그 사실을 알린다.
     */
    static RuleChange applyFileRule(BoardModel model, CellPattern pattern) {
        String written = pattern.rule();
        if (written == null) {
            return RuleChange.NONE;
        }
        Rule fileRule = resolveRule(written);
        Rule current = model.rule();
        if (fileRule == null) {
            return new RuleChange("This file uses the rule \"" + written
                    + "\", which this game does not support.\nThe current rule "
                    + describe(current) + " is kept.", true);
        }
        if (fileRule.equals(current)) {
            return RuleChange.NONE;
        }
        model.setRule(fileRule);
        return new RuleChange(null, false);     // 규칙을 맞췄을 뿐이라 따로 알리지 않는다.
    }

    /** "Life"는 Conway의 다른 이름이다. 해석할 수 없는 표기는 null. */
    static Rule resolveRule(String written) {
        if (written.equalsIgnoreCase("Life")) {
            return Rules.CONWAY;
        }
        try {
            return Rules.parse(written);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 메뉴에 있는 이름이 있으면 "HighLife (B36/S23)", 없으면 표기만. */
    static String describe(Rule rule) {
        for (Map.Entry<String, Rule> preset : Rules.presets().entrySet()) {
            if (preset.getValue().equals(rule)) {
                return preset.getKey() + " (" + rule.notation() + ")";
            }
        }
        return rule.notation();
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
