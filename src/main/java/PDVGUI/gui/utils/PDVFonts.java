package PDVGUI.gui.utils;

import javax.swing.UIManager;
import java.awt.Font;

/**
 * Centralized fonts for PDV.
 *
 * All PDV chrome fonts are derived from the active Look and Feel's default font
 * (set by {@link PDVLookAndFeel}) so the whole UI shares one consistent, modern
 * font family instead of the previous mix of hardcoded "Console", "Lucida",
 * "simsun" and "Arial" families - several of which are not installed on most
 * systems and silently fell back to a default font anyway.
 *
 * Only the family is unified; each caller keeps its intended style and size.
 * Spectrum/chart visualization fonts are intentionally NOT routed through here.
 */
public class PDVFonts {

    private PDVFonts() {
    }

    /** The Look and Feel default font, or a sane sans-serif fallback. */
    public static Font base() {
        Font font = UIManager.getFont("defaultFont");
        if (font == null) {
            font = UIManager.getFont("Label.font");
        }
        return font != null ? font : new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }

    /**
     * Default-family font with the given style ({@link Font#PLAIN},
     * {@link Font#BOLD} or {@link Font#ITALIC}) and point size.
     */
    public static Font of(int style, float size) {
        return base().deriveFont(style, size);
    }
}
