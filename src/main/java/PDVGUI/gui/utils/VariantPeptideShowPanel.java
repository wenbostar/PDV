package PDVGUI.gui.utils;

import javax.swing.*;
import java.awt.*;

/**
 * Variant peptide show pop menu
 * Created by Ken on 6/16/2017.
 */
public class VariantPeptideShowPanel extends JPanel {

    /**
     * Old peptide sequence split into an array.
     */
    private String[] oldSequenceComponents;
    /**
     * Variant peptide sequence split into an array
     */
    private String[] variantSequenceComponents;
    /**
     * Variant aa site
     */
    private int variantLocation;

    /**
     * Main constructor
     * @param sequence peptide sequence
     * @param variantSequence variant peptide sequence
     * @param variantLocation variant aa location
     */
    public VariantPeptideShowPanel(String sequence, String variantSequence, int variantLocation){

        this.variantLocation = variantLocation;

        oldSequenceComponents = new String[sequence.length()];
        for (int i = 0; i < oldSequenceComponents.length; i++) {
            oldSequenceComponents[i] = Character.toString(sequence.charAt(i));
        }

        variantSequenceComponents = new String [variantSequence.length()];
        for(int i = 0; i < variantSequenceComponents.length; i++) {
            variantSequenceComponents[i] = Character.toString(variantSequence.charAt(i));
        }
    }


    /**
     * Paints the old sequence and variant sequence Link variant aa
     * @param g Graphics window
     */
    public void paint(Graphics g) {
        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setFont(new Font("monospaced", Font.BOLD, 15));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int yLocation = 20;
        int xLocation = 10;

        int lFontHeight = g2.getFontMetrics().getHeight();
        Double lMidStringHeight = yLocation - lFontHeight * 0.2;

        for (int i = 0; i < oldSequenceComponents.length; i++) {
            g2.setColor(Color.black);

            String residue = oldSequenceComponents[i];

            String variantResidue = variantSequenceComponents[i];

            g2.drawString(residue, xLocation, yLocation);
            g2.drawString(variantResidue, xLocation, yLocation+20);

            xLocation += g2.getFontMetrics().stringWidth(residue) + 1;

            if(i == variantLocation || i == variantLocation-1){
                int lBarHeight = 20;
                g2.setColor(Color.blue);
                Rectangle tempRectangle = new Rectangle(xLocation+1, lMidStringHeight.intValue() + 1, 2, lBarHeight);
                g2.fill(tempRectangle);
            }

            xLocation = xLocation + 2 + 3;
        }

        this.setPreferredSize(new Dimension(xLocation, 100));
    }
}
