package PDVGUI.gui.utils;

import PDVGUI.gui.utils.Export.ExportExpectedSizeDialog;
import com.compomics.util.gui.spectrum.SequenceFragmentationPanel;
import com.compomics.util.gui.spectrum.SpectrumPanel;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Set action in SpectrumMainPanel
 * Created by Ken on 9/5/2017.
 */
public class SetAction {

    /**
     * The screen x
     */
    private volatile int screenX = 0;
    /**
     * The screen y
     */
    private volatile int screenY = 0;
    /**
     * The new x
     */
    private volatile int myX = 0;
    /**
     * The new y
     */
    private volatile int myY = 0;
    /**
     * 2nd screen x
     */
    private volatile int screenX2 = 0;
    /**
     * 2nd screen y
     */
    private volatile int screenY2 = 0;
    /**
     * 2nd new x
     */
    private volatile int myX2 = 0;
    /**
     * 2nd new y
     */
    private volatile int myY2 = 0;

    /**
     * ExportExpectedSizeDialog
     */
    private ExportExpectedSizeDialog exportExpectedSizeDialog;

    /**
     * Constructor
     * @param parent Spectrum main panel
     * @param jLayeredPane JLayer panel in spectrum main panel
     * @param secondarySpectrumPlotsJPanel Secondary spectrum plot panel
     * @param mirrorSequenceFragmentationPanel Mirror sequence fragment panel
     * @param spectrumJPanel SpectrumPanel
     * @param height Panel height
     * @param width Panel width
     * @param spectrumMainPanel Spectrum main panel
     */
    public SetAction(SpectrumMainPanel parent, JLayeredPane jLayeredPane, SequenceFragmentationPanel secondarySpectrumPlotsJPanel, JPanel mirrorSequenceFragmentationPanel,
                     SpectrumPanel spectrumJPanel, Integer height, Integer width, JPanel spectrumMainPanel){

        spectrumJPanel.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if(secondarySpectrumPlotsJPanel != null) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);
                }

                if(mirrorSequenceFragmentationPanel != null){
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                }

                jLayeredPane.setPosition(spectrumJPanel, 1);

                if (e.getButton() == MouseEvent.BUTTON3 && parent != null) {

                    if (exportExpectedSizeDialog != null){

                        exportExpectedSizeDialog.dispose();
                        parent.updateSpectrum();
                        parent.updateExportJDialog();
                    } else {
                        parent.updateSpectrum();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if(secondarySpectrumPlotsJPanel != null) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);
                }

                if(mirrorSequenceFragmentationPanel != null){
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                }

                jLayeredPane.setPosition(spectrumJPanel, 1);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if(secondarySpectrumPlotsJPanel != null) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);
                }

                if(mirrorSequenceFragmentationPanel != null){
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                }

                jLayeredPane.setPosition(spectrumJPanel, 1);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if(secondarySpectrumPlotsJPanel != null) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);
                }

                if(mirrorSequenceFragmentationPanel != null){
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                }

                jLayeredPane.setPosition(spectrumJPanel, 1);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if(secondarySpectrumPlotsJPanel != null) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);
                }

                if(mirrorSequenceFragmentationPanel != null){
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                }

                jLayeredPane.setPosition(spectrumJPanel, 1);
            }

        });

        spectrumJPanel.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if(spectrumMainPanel != null){
                    if (secondarySpectrumPlotsJPanel != null){
                        spectrumJPanel.setBounds(0,75, spectrumMainPanel.getWidth(), spectrumMainPanel.getHeight()-85);
                    }
                } else {
                    spectrumJPanel.setBounds(0,0,width, height);
                }


                if(secondarySpectrumPlotsJPanel != null) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);
                }

                if(mirrorSequenceFragmentationPanel != null){
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                }

                jLayeredPane.setPosition(spectrumJPanel, 1);
            }

        });

        if(secondarySpectrumPlotsJPanel != null) {

            secondarySpectrumPlotsJPanel.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) {
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    screenX = e.getXOnScreen();
                    screenY = e.getYOnScreen();

                    myX = secondarySpectrumPlotsJPanel.getX();
                    myY = secondarySpectrumPlotsJPanel.getY();
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);

                    if (mirrorSequenceFragmentationPanel != null) {
                        jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                    }

                    jLayeredPane.setPosition(spectrumJPanel, 1);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);

                    if (mirrorSequenceFragmentationPanel != null) {
                        jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                    }

                    jLayeredPane.setPosition(spectrumJPanel, 1);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    secondarySpectrumPlotsJPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    secondarySpectrumPlotsJPanel.setBorder(null);
                }

            });

            secondarySpectrumPlotsJPanel.addMouseMotionListener(new MouseMotionListener() {

                @Override
                public void mouseDragged(MouseEvent e) {
                    int deltaX = e.getXOnScreen() - screenX;
                    int deltaY = e.getYOnScreen() - screenY;

                    secondarySpectrumPlotsJPanel.setLocation(myX + deltaX, myY + deltaY);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                }

            });
        }

        if(mirrorSequenceFragmentationPanel != null){
            mirrorSequenceFragmentationPanel.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) { }

                @Override
                public void mousePressed(MouseEvent e) {
                    screenX2 = e.getXOnScreen();
                    screenY2 = e.getYOnScreen();

                    myX2 = mirrorSequenceFragmentationPanel.getX();
                    myY2 = mirrorSequenceFragmentationPanel.getY();
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);

                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);

                    jLayeredPane.setPosition(spectrumJPanel, 1);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);

                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);

                    jLayeredPane.setPosition(spectrumJPanel, 1);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    mirrorSequenceFragmentationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    mirrorSequenceFragmentationPanel.setBorder(null);
                }

            });

            mirrorSequenceFragmentationPanel.addMouseMotionListener(new MouseMotionListener() {

                @Override
                public void mouseDragged(MouseEvent e) {
                    int deltaX = e.getXOnScreen() - screenX2;
                    int deltaY = e.getYOnScreen() - screenY2;

                    mirrorSequenceFragmentationPanel.setLocation(myX2 + deltaX, myY2 + deltaY);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                }

            });
        }
    }

    /**
     * Set ExportExpectedSizeDialog
     * @param exportDialog
     */
    public void setExportDialog(ExportExpectedSizeDialog exportDialog){

        this.exportExpectedSizeDialog = exportDialog;

    }

    /**
     * Constructor
     * @param jLayeredPane JLayer panel
     * @param secondarySpectrumPlotsJPanel Secondary spectrum plot panel
     * @param mirrorSequenceFragmentationPanel Mirror sequence fragment panel
     * @param spectrumJPanel Spectrum panel
     * @param height Panel height
     * @param width Panel width
     */
    public SetAction(JLayeredPane jLayeredPane, SequenceFragmentationPanel secondarySpectrumPlotsJPanel, JPanel mirrorSequenceFragmentationPanel,
                     JPanel spectrumJPanel, Integer height, Integer width){

        spectrumJPanel.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if(secondarySpectrumPlotsJPanel != null) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);
                }

                if(mirrorSequenceFragmentationPanel != null){
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                }

                jLayeredPane.setPosition(spectrumJPanel, 1);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if(secondarySpectrumPlotsJPanel != null) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);
                }

                if(mirrorSequenceFragmentationPanel != null){
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                }

                jLayeredPane.setPosition(spectrumJPanel, 1);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if(secondarySpectrumPlotsJPanel != null) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);
                }

                if(mirrorSequenceFragmentationPanel != null){
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                }

                jLayeredPane.setPosition(spectrumJPanel, 1);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if(secondarySpectrumPlotsJPanel != null) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);
                }

                if(mirrorSequenceFragmentationPanel != null){
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                }

                jLayeredPane.setPosition(spectrumJPanel, 1);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if(secondarySpectrumPlotsJPanel != null) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);
                }

                if(mirrorSequenceFragmentationPanel != null){
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                }

                jLayeredPane.setPosition(spectrumJPanel, 1);
            }

        });

        spectrumJPanel.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {
            }

            @Override
            public void mouseMoved(MouseEvent e) {

                spectrumJPanel.setBounds(0,75, width, height - 85);

                if(secondarySpectrumPlotsJPanel != null) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);
                }

                if(mirrorSequenceFragmentationPanel != null){
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                }

                jLayeredPane.setPosition(spectrumJPanel, 1);
            }

        });

        if(secondarySpectrumPlotsJPanel != null) {

            secondarySpectrumPlotsJPanel.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) {
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    screenX = e.getXOnScreen();
                    screenY = e.getYOnScreen();

                    myX = secondarySpectrumPlotsJPanel.getX();
                    myY = secondarySpectrumPlotsJPanel.getY();
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);

                    if (mirrorSequenceFragmentationPanel != null) {
                        jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                    }

                    jLayeredPane.setPosition(spectrumJPanel, 1);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);

                    if (mirrorSequenceFragmentationPanel != null) {
                        jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);
                    }

                    jLayeredPane.setPosition(spectrumJPanel, 1);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    secondarySpectrumPlotsJPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    secondarySpectrumPlotsJPanel.setBorder(null);
                }

            });

            secondarySpectrumPlotsJPanel.addMouseMotionListener(new MouseMotionListener() {

                @Override
                public void mouseDragged(MouseEvent e) {
                    int deltaX = e.getXOnScreen() - screenX;
                    int deltaY = e.getYOnScreen() - screenY;

                    secondarySpectrumPlotsJPanel.setLocation(myX + deltaX, myY + deltaY);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                }

            });
        }

        if(mirrorSequenceFragmentationPanel != null){
            mirrorSequenceFragmentationPanel.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) { }

                @Override
                public void mousePressed(MouseEvent e) {
                    screenX2 = e.getXOnScreen();
                    screenY2 = e.getYOnScreen();

                    myX2 = mirrorSequenceFragmentationPanel.getX();
                    myY2 = mirrorSequenceFragmentationPanel.getY();
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);

                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);

                    jLayeredPane.setPosition(spectrumJPanel, 1);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    jLayeredPane.setPosition(mirrorSequenceFragmentationPanel, 0);

                    jLayeredPane.setPosition(secondarySpectrumPlotsJPanel, 0);

                    jLayeredPane.setPosition(spectrumJPanel, 1);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    mirrorSequenceFragmentationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    mirrorSequenceFragmentationPanel.setBorder(null);
                }

            });

            mirrorSequenceFragmentationPanel.addMouseMotionListener(new MouseMotionListener() {

                @Override
                public void mouseDragged(MouseEvent e) {
                    int deltaX = e.getXOnScreen() - screenX2;
                    int deltaY = e.getYOnScreen() - screenY2;

                    mirrorSequenceFragmentationPanel.setLocation(myX2 + deltaX, myY2 + deltaY);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                }

            });
        }
    }
}
