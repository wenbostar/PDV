package PDVGUI.gui.utils;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Centralized Swing Look and Feel setup for PDV.
 *
 * Installs the FlatLaf light theme plus a small layer of shared UI polish so all
 * PDV windows share a consistent, modern appearance (matching the Carafe GUI).
 */
public class PDVLookAndFeel {

    /**
     * Install the FlatLaf light look and feel and apply shared UI defaults.
     * Safe to call multiple times (each PDV window installs it on construction).
     */
    public static void setup() {
        FlatLightLaf.setup();
        customizeUIDefaults();
    }

    private static void customizeUIDefaults() {
        Font defaultFont = UIManager.getFont("Label.font");
        if (defaultFont != null) {
            UIManager.put("defaultFont", defaultFont.deriveFont(13f));
        } else {
            UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        }
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TabbedPane.tabInsets", new Insets(8, 14, 8, 14));
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.hideMnemonics", true);
        ToolTipManager.sharedInstance().setDismissDelay(30000);
    }
}
