package PDVCLI.utils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class AddInformationPanel extends JPanel {

    private ArrayList<String> addInforList;
    /**
     * The font to use.
     */
    private Font iBaseFont = new Font("Arial", Font.PLAIN, 17);

    private int tabSize = 5;

    private Integer width;


    public AddInformationPanel(ArrayList<String> addInforList, Integer width){
        super();
        this.addInforList = addInforList;
        this.width = width;
    }

    public void paint(Graphics g){
        super.paint(g);
        int inforSize = addInforList.size();
        int yHeight = 20;
        if (inforSize != 0){
            if (inforSize > 3){
                tabSize = 3;
                yHeight = 15;
                int newFontSize = 21 - 2*inforSize;
                iBaseFont = new Font("Arial", Font.PLAIN, newFontSize);
            }
            Graphics2D g2 = (Graphics2D) g;

            // Set the base font, arial!
            int smallFontCorrection;
            g2.setFont(iBaseFont);
            g2.setColor(Color.BLUE);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = g2.getFontMetrics();

            for (String eachInfor : addInforList){
                for (int i = 2; i <= 10; i ++){
                    if (fm.stringWidth(eachInfor) + 45 > width){
                        g2.setFont(new Font(iBaseFont.getName(), iBaseFont.getStyle(), iBaseFont.getSize() - i));
                        fm = g2.getFontMetrics();
                    } else {
                        break;
                    }
                }

                smallFontCorrection = g2.getFontMetrics().getAscent() + tabSize;
                g2.drawString(eachInfor, 10, yHeight);
                yHeight += smallFontCorrection;

                g2.setFont(iBaseFont);
            }
            this.setPreferredSize(new Dimension(width, 200));
        }

    }

}
