package PDVGUI.gui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A free-floating, draggable per-residue confidence bar track for the spectrum annotation plot.
 * One vertical bar per residue, whose height is proportional to that residue's score
 * (tall = high confidence) and shaded green(high) -> red(low) for redundancy, with the residue's
 * amino-acid letter drawn beneath the bar.
 *
 * The residue font and horizontal spacing are taken from the live SequenceFragmentationPanel, and
 * each bar/letter is positioned with the same proportional advance (stringWidth(residue) +
 * horizontalSpace), so the track matches the sequence strip in font and pitch and the bars line up
 * with the residues when dragged over the sequence.
 *
 * It is independent of the sequence strip and can be dragged anywhere on the plot; by default it
 * is placed in the top-right corner. The exact score is shown on hover.
 *
 * Intended for Casanovo de novo results, where the per-residue scores come from the mzTab
 * opt_global_aa_scores column.
 */
public class ConfidenceTrackPanel extends JPanel {

    /**
     * Per-residue confidence scores, expected in [0, 1].
     */
    private final double[] scores;
    /**
     * Amino-acid residues, one per score.
     */
    private final String residues;
    /**
     * Residue font, copied from the sequence strip.
     */
    private final Font seqFont;
    /**
     * Small font used for the per-bar score labels.
     */
    private static final Font SCORE_FONT = new Font("Arial", Font.PLAIN, 9);
    /**
     * Inner padding between the panel edge and the content.
     */
    private final int pad = 6;
    /**
     * Center x of each residue (and its bar), reproducing the sequence-strip layout.
     */
    private final int[] centerX;
    /**
     * Uniform bar width (same for every residue), capped so adjacent bars never overlap.
     */
    private final int barWidth;
    /**
     * Natural content width spanning all residues.
     */
    private final int contentWidth;
    /**
     * Whether the user has dragged the track (so auto-anchoring to the top-right stops).
     */
    private boolean userMoved = false;
    /**
     * Cursor position within the panel at the start of a drag.
     */
    private int dragX, dragY;

    /**
     * Constructor.
     * @param scores per-residue confidence scores in [0, 1]
     * @param residues amino-acid residues, one per score
     * @param seqFont the residue font copied from the sequence strip
     * @param extraSpacing per-residue space added on top of the letter width (matches the panel:
     *                     2 * iHorizontalSpace + iBarWidth)
     * @param fm font metrics measured with the sequence strip's own (on-screen) graphics, so the
     *           per-character widths match how the strip paints (avoids drift on HiDPI displays)
     */
    public ConfidenceTrackPanel(double[] scores, String residues, Font seqFont, int extraSpacing, FontMetrics fm) {
        this.scores = scores;
        this.residues = residues == null ? "" : residues;
        this.seqFont = seqFont;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        // SequenceFragmentationPanel advances per residue by stringWidth(residue) + extraSpacing
        // (where extraSpacing = 2*iHorizontalSpace + iBarWidth). Reproduce it exactly, using the
        // strip's own FontMetrics, so the per-residue centers match with no cumulative drift.
        centerX = new int[scores.length];
        int x = pad;
        for (int i = 0; i < scores.length; i++) {
            String aa = i < this.residues.length() ? String.valueOf(this.residues.charAt(i)) : "?";
            int charWidth = fm.stringWidth(aa);
            centerX[i] = x + charWidth / 2;
            x += charWidth + extraSpacing;
        }
        contentWidth = x - extraSpacing + pad;

        // Uniform bar width: aim for ~0.85 of the font size, but never wider than the smallest
        // center-to-center distance (minus a gap) so adjacent bars don't overlap.
        int minCenterDist = Integer.MAX_VALUE;
        for (int i = 1; i < centerX.length; i++) {
            minCenterDist = Math.min(minCenterDist, centerX[i] - centerX[i - 1]);
        }
        int desired = Math.round(seqFont.getSize() * 0.85f);
        barWidth = Math.max(4, minCenterDist == Integer.MAX_VALUE ? desired : Math.min(desired, minCenterDist - 2));

        setToolTipText("Per-residue confidence (drag to move)");

        MouseAdapter dragger = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragX = e.getX();
                dragY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                userMoved = true;
                setLocation(getX() + e.getX() - dragX, getY() + e.getY() - dragY);
                if (getParent() != null) {
                    getParent().repaint();
                }
            }
        };
        addMouseListener(dragger);
        addMouseMotionListener(dragger);
    }

    /**
     * @return the natural width of the track for the current scores.
     */
    public int naturalWidth() {
        return contentWidth + 1;
    }

    /**
     * @return the x of the first residue's left edge within the track, used to align the track
     *         under the sequence strip.
     */
    public int leftInset() {
        return pad;
    }

    /**
     * @return whether the user has dragged the track away from its default position.
     */
    public boolean isUserMoved() {
        return userMoved;
    }

    /**
     * Map a score in [0, 1] to a red(low) -> yellow -> green(high) color.
     * @param score confidence score
     * @return bar color
     */
    private static Color heatColor(double score) {
        float s = (float) Math.max(0.0, Math.min(1.0, score));
        return Color.getHSBColor(s * 0.34f, 0.85f, 0.9f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int h = getHeight();

        // Amino-acid letters use the sequence font (matching the strip) and sit below the baseline;
        // the score is printed in a smaller font just above each bar.
        FontMetrics lfm = g2.getFontMetrics(seqFont);
        FontMetrics sfm = g2.getFontMetrics(SCORE_FONT);
        int letterRow = lfm.getHeight();
        int baselineY = h - pad - letterRow;
        // Leave room for a score label above even a full-height bar.
        int maxBarHeight = baselineY - pad - sfm.getHeight();

        for (int i = 0; i < scores.length; i++) {
            double s = Math.max(0.0, Math.min(1.0, scores[i]));
            int barHeight = (int) Math.round(s * maxBarHeight);
            int x = centerX[i] - barWidth / 2;
            int barTop = baselineY - barHeight;

            g2.setColor(heatColor(s));
            g2.fillRect(x, barTop, barWidth, barHeight);
            g2.setColor(new Color(120, 120, 120));
            g2.drawRect(x, barTop, barWidth, barHeight);

            // Score (as percent) just above the bar.
            g2.setFont(SCORE_FONT);
            String pct = String.valueOf((int) Math.round(s * 100));
            g2.setColor(new Color(70, 70, 70));
            g2.drawString(pct, centerX[i] - sfm.stringWidth(pct) / 2, barTop - 2);

            // Amino-acid letter below the baseline.
            if (i < residues.length()) {
                g2.setFont(seqFont);
                String aa = String.valueOf(residues.charAt(i));
                g2.setColor(new Color(60, 60, 60));
                g2.drawString(aa, centerX[i] - lfm.stringWidth(aa) / 2, baselineY + lfm.getAscent() + 1);
            }
        }
        g2.dispose();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int idx = nearestResidue(e.getX());
        if (idx >= 0) {
            String aa = idx < residues.length() ? residues.charAt(idx) + " " : "";
            return aa + "(residue " + (idx + 1) + "): " + Math.round(scores[idx] * 100) + "%";
        }
        return null;
    }

    /**
     * @param px x within the panel
     * @return index of the residue whose center is nearest px, or -1 if none
     */
    private int nearestResidue(int px) {
        int best = -1;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < centerX.length; i++) {
            int d = Math.abs(centerX[i] - px);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }
}
