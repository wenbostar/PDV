package PDVGUI.gui.utils;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Shared, theme-aware row styling for PDV's PSM / spectrum list tables.
 *
 * Replaces the previously duplicated prepareRenderer() blocks that hardcoded
 * white/black/cyan striping and a near-black selection colour. Colours are now
 * pulled from the active (FlatLaf) Look and Feel so every table stays
 * consistent with the rest of the UI and would follow a future dark theme.
 * The domain-specific rank-1 (best match) highlight is preserved unchanged.
 *
 * This only affects the row colouring of the list tables; it has nothing to do
 * with the spectrum annotation panel (SpectrumMainPanel) visualization.
 */
public class PDVTableStyle {

    /** Background used to flag the best-scoring (rank 1) match. Preserved from the original code. */
    private static final Color RANK1_BACKGROUND = new Color(255, 116, 135);

    /** Standard list-table row height, in pixels. */
    public static final int ROW_HEIGHT = 23;

    private PDVTableStyle() {
    }

    /**
     * Apply PDV's standard density and fonts to a list/data table so every such
     * table looks the same: a consistent row height, a plain cell font and a
     * bold header, all derived from the active Look and Feel via {@link PDVFonts}.
     */
    public static void applyDefaults(JTable table) {
        table.setRowHeight(ROW_HEIGHT);
        table.setFont(PDVFonts.of(Font.PLAIN, 12f));
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setFont(PDVFonts.of(Font.BOLD, 13f));
        }
    }

    /**
     * Apply alternating-row, selection and rank-1 colours to an already
     * prepared cell component. Intended to be called from a JTable's
     * prepareRenderer(...) override:
     *
     * <pre>
     * public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
     *     return PDVTableStyle.applyRowStyle(this, super.prepareRenderer(renderer, row, column), row, column);
     * }
     * </pre>
     */
    public static Component applyRowStyle(JTable table, Component component, int row, int column) {
        if (table.isRowSelected(row)) {
            component.setBackground(UIManager.getColor("Table.selectionBackground"));
            component.setForeground(UIManager.getColor("Table.selectionForeground"));
        } else {
            Color background = UIManager.getColor("Table.background");
            if (background == null) {
                background = Color.white;
            }
            if (row % 2 != 0) {
                background = alternateRowColor(background);
            }
            component.setBackground(background);
            Color foreground = UIManager.getColor("Table.foreground");
            component.setForeground(foreground != null ? foreground : Color.black);
        }

        // Domain-specific: highlight the rank-1 (best) match, exactly as before.
        Object value = table.getValueAt(row, column);
        if (value != null && String.valueOf(value).contains(" Rank:" + "&nbsp<html>" + 1)) {
            component.setBackground(RANK1_BACKGROUND);
            component.setForeground(Color.black);
        }
        return component;
    }

    /**
     * Subtle alternating-row colour. Prefers the Look and Feel's own
     * Table.alternateRowColor; otherwise derives a faint tint from the base
     * background so striping works on both light and dark themes.
     */
    private static Color alternateRowColor(Color base) {
        Color alternate = UIManager.getColor("Table.alternateRowColor");
        if (alternate != null) {
            return alternate;
        }
        boolean dark = (base.getRed() + base.getGreen() + base.getBlue()) / 3 < 128;
        int delta = dark ? 12 : -10;
        return new Color(clamp(base.getRed() + delta), clamp(base.getGreen() + delta), clamp(base.getBlue() + delta));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
