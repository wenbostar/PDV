package PDVGUI.gui;

import PDVGUI.gui.utils.FileImport.*;
import com.compomics.util.gui.DummyFrame;
import PDVGUI.utils.ImportPTMsFromUnimod;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * PDV start dialog
 * Created by Ken on 5/11/2017.
 */
public class PDVStart extends JDialog {

    private JPanel firstPartJPanel;
    private JPanel secondPartJPanel;
    private JButton newDatabaseResultJButton;
    private JButton newDeNovoResultJButton;
    private JButton singlePeptideDisplayJButton;
    private JButton openProBamJButton;
    private JButton openMaxQuantJButton;
    private JButton openSpectrumLibJButton;
    private JButton openPrideXMLJButton;
    private JButton openSpectrumFileJButton;
    private JButton openProteoQCJButton;

    /**
     * Parent frame
     */
    private PDVMainClass pdvMainClass;
    /**
     * A dummy parent frame to be able to show an icon in the task bar From utilities.
     */
    static private DummyFrame dummyParentFrame = new DummyFrame("", "/icons/SeaGullMass.png");

    /**
     * Create a new PDVStart.
     * @param pdvMainClass the dialog parent
     */
    public PDVStart(PDVMainClass pdvMainClass) {
        super(dummyParentFrame.setNewTitle(pdvMainClass.getTitle()), true);
        this.pdvMainClass = pdvMainClass;

        new Thread("initPTM"){
            @Override
            public void run(){
                File inputFile = new File(PDVMainClass.getJarFilePath() + "/resources/conf/unimod.xml");
                new ImportPTMsFromUnimod(inputFile);
            }
        }.start();

        initComponents();

        setTitle(getTitle() + " " );

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Create a new PDVStart.
     * @param pdvMainClass the dialog parent
     */
    public PDVStart(PDVMainClass pdvMainClass, JFrame oldJFrame) {
        super(dummyParentFrame.setNewTitle(pdvMainClass.getTitle()), true);
        this.pdvMainClass = pdvMainClass;
        initComponents();

        if (oldJFrame != null){
            oldJFrame.setVisible(false);
        }

        setTitle(getTitle() + " " );
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")));

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     *Init all GUI components
     */
    private void initComponents() {

        JPanel backgroundPanel = new JPanel();
        JPanel optionJPanel = new JPanel();
        firstPartJPanel = new JPanel();
        secondPartJPanel = new JPanel();
        newDatabaseResultJButton = new JButton();
        newDeNovoResultJButton = new JButton();
        singlePeptideDisplayJButton = new JButton();
        openProBamJButton = new JButton();
        openMaxQuantJButton = new JButton();
        openSpectrumLibJButton = new JButton();
        openPrideXMLJButton = new JButton();
        openSpectrumFileJButton = new JButton();
        openProteoQCJButton = new JButton();

        JPanel paintJPanel = new JPanel(){
            Image image=null;
            
            public void paint(Graphics g){

                try {

                    Graphics2D g2 = (Graphics2D) g;

                    String pngFilePath = PDVMainClass.getJarFilePath() + "/resources/icons/background.png";

                    image= ImageIO.read(new File(pngFilePath));

                    Map mapH = new HashMap();
                    mapH.put(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
                    mapH.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    g2.setRenderingHints(mapH);
                    g2.drawImage(image, 0, 0, 750, 331, null);

                } catch (Exception e) {

                    e.printStackTrace();

                }
            }
        };

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Welcome to PDV");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        newDatabaseResultJButton.setFont(new Font("Arial", Font.BOLD, 14));
        newDatabaseResultJButton.setBackground(new Color(239, 252,255));
        newDatabaseResultJButton.setIcon(new ImageIcon(getClass().getResource("/icons/data.png")));
        newDatabaseResultJButton.setText("Database Searching");
        newDatabaseResultJButton.setFocusPainted(false);
        newDatabaseResultJButton.setHorizontalAlignment(SwingConstants.LEFT);
        newDatabaseResultJButton.setIconTextGap(5);
        newDatabaseResultJButton.setBorder(null);
        newDatabaseResultJButton.setBorderPainted(false);
        newDatabaseResultJButton.setContentAreaFilled(false);
        newDatabaseResultJButton.addActionListener(this::newDatabaseJButtonActionPerformed);
        newDatabaseResultJButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                newDatabaseResultJButtonMouseEntered(evt);
            }
        });
        newDatabaseResultJButton.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                newDatabaseResultJButtonMouseExited(evt);
            }
        });

        newDeNovoResultJButton.setFont(new Font("Arial", Font.BOLD, 14));
        newDeNovoResultJButton.setBackground(new Color(239, 252,255));
        newDeNovoResultJButton.setIcon(new ImageIcon(getClass().getResource("/icons/lab.png")));
        newDeNovoResultJButton.setText("Denovo Sequencing");
        newDeNovoResultJButton.setFocusPainted(false);
        newDeNovoResultJButton.setHorizontalAlignment(SwingConstants.LEFT);
        newDeNovoResultJButton.setBorder(null);
        newDeNovoResultJButton.setBorderPainted(false);
        newDeNovoResultJButton.setContentAreaFilled(false);
        newDeNovoResultJButton.addActionListener(this::newDeNovoJButtonActionPerformed);
        newDeNovoResultJButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                newDeNovoResultJButtonMouseEntered(evt);
            }
        });
        newDeNovoResultJButton.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                newDeNovoResultJButtonMouseExited(evt);
            }
        });

        singlePeptideDisplayJButton.setFont(new Font("Arial", Font.BOLD, 14));
        singlePeptideDisplayJButton.setBackground(new Color(239, 252,255));
        singlePeptideDisplayJButton.setIcon(new ImageIcon(getClass().getResource("/icons/tag.png")));
        singlePeptideDisplayJButton.setText("One PSM" );
        singlePeptideDisplayJButton.setFocusPainted(false);
        singlePeptideDisplayJButton.setHorizontalAlignment(SwingConstants.LEFT);
        singlePeptideDisplayJButton.setBorder(null);
        singlePeptideDisplayJButton.setBorderPainted(false);
        singlePeptideDisplayJButton.setContentAreaFilled(false);
        singlePeptideDisplayJButton.addActionListener(this::singlePeptideDisplayJButtonActionPerformed);
        singlePeptideDisplayJButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                singlePeptideDisplayJButtonMouseEntered(evt);
            }
        });
        singlePeptideDisplayJButton.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                singlePeptideDisplayJButtonMouseExited(evt);
            }
        });

        openProBamJButton.setFont(new Font("Arial", Font.BOLD, 14));
        openProBamJButton.setBackground(new Color(239, 252,255));
        openProBamJButton.setIcon(new ImageIcon(getClass().getResource("/icons/dna.png")));
        openProBamJButton.setText("Proteogenomics" );
        openProBamJButton.setFocusPainted(false);
        openProBamJButton.setHorizontalAlignment(SwingConstants.LEFT);
        openProBamJButton.setBorder(null);
        openProBamJButton.setBorderPainted(false);
        openProBamJButton.setContentAreaFilled(false);
        openProBamJButton.addActionListener(this::openProBamJButtonActionPerformed);
        openProBamJButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                openProBamJButtonMouseEntered(evt);
            }
        });
        openProBamJButton.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                openProBamJButtonMouseExited(evt);
            }
        });

        openMaxQuantJButton.setFont(new Font("Arial", Font.BOLD, 14));
        openMaxQuantJButton.setBackground(new Color(239, 252,255));
        openMaxQuantJButton.setIcon(new ImageIcon(getClass().getResource("/icons/maxQuantLogo.png")));
        openMaxQuantJButton.setText("MaxQuant" );
        openMaxQuantJButton.setFocusPainted(false);
        openMaxQuantJButton.setHorizontalAlignment(SwingConstants.LEFT);
        openMaxQuantJButton.setBorder(null);
        openMaxQuantJButton.setBorderPainted(false);
        openMaxQuantJButton.setContentAreaFilled(false);
        openMaxQuantJButton.addActionListener(this::openMaxQuantJButtonActionPerformed);
        openMaxQuantJButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                openMaxQuantJButtonMouseEntered(evt);
            }
        });
        openMaxQuantJButton.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                openMaxQuantJButtonMouseExited(evt);
            }
        });

        openSpectrumLibJButton.setFont(new Font("Arial", Font.BOLD, 14));
        openSpectrumLibJButton.setBackground(new Color(239, 252,255));
        openSpectrumLibJButton.setIcon(new ImageIcon(getClass().getResource("/icons/library.png")));
        openSpectrumLibJButton.setText("Spectrum Library" );
        openSpectrumLibJButton.setFocusPainted(false);
        openSpectrumLibJButton.setHorizontalAlignment(SwingConstants.LEFT);
        openSpectrumLibJButton.setBorder(null);
        openSpectrumLibJButton.setBorderPainted(false);
        openSpectrumLibJButton.setContentAreaFilled(false);
        openSpectrumLibJButton.addActionListener(this::openSpectrumLibJButtonActionPerformed);
        openSpectrumLibJButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                openSpectrumLibJButtonMouseEntered(evt);
            }
        });
        openSpectrumLibJButton.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                openSpectrumLibJButtonMouseExited(evt);
            }
        });

        openPrideXMLJButton.setFont(new Font("Arial", Font.BOLD, 14));
        openPrideXMLJButton.setBackground(new Color(239, 252,255));
        openPrideXMLJButton.setIcon(new ImageIcon(getClass().getResource("/icons/pride_logo.png")));
        openPrideXMLJButton.setText("PRIDE XML" );
        openPrideXMLJButton.setFocusPainted(false);
        openPrideXMLJButton.setHorizontalAlignment(SwingConstants.LEFT);
        openPrideXMLJButton.setBorder(null);
        openPrideXMLJButton.setBorderPainted(false);
        openPrideXMLJButton.setContentAreaFilled(false);
        openPrideXMLJButton.addActionListener(this::openPrideXMLJButtonActionPerformed);
        openPrideXMLJButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                openPrideXMLJButtonMouseEntered(evt);
            }
        });
        openPrideXMLJButton.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                openPrideXMLJButtonMouseExited(evt);
            }
        });

        openSpectrumFileJButton.setFont(new Font("Arial", Font.BOLD, 14));
        openSpectrumFileJButton.setBackground(new Color(239, 252,255));
        openSpectrumFileJButton.setIcon(new ImageIcon(getClass().getResource("/icons/ms.png")));
        openSpectrumFileJButton.setText("MS Data" );
        openSpectrumFileJButton.setFocusPainted(false);
        openSpectrumFileJButton.setHorizontalAlignment(SwingConstants.LEFT);
        openSpectrumFileJButton.setBorder(null);
        openSpectrumFileJButton.setIconTextGap(5);
        openSpectrumFileJButton.setBorderPainted(false);
        openSpectrumFileJButton.setContentAreaFilled(false);
        openSpectrumFileJButton.addActionListener(this::openSpectrumFileJButtonActionPerformed);
        openSpectrumFileJButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                openSpectrumFileJButtonMouseEntered(evt);
            }
        });
        openSpectrumFileJButton.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                openSpectrumFileJButtonMouseExited(evt);
            }
        });

        openProteoQCJButton.setFont(new Font("Arial", Font.BOLD, 14));
        openProteoQCJButton.setBackground(new Color(239, 252,255));
        openProteoQCJButton.setIcon(new ImageIcon(getClass().getResource("/icons/control.png")));
        openProteoQCJButton.setText("QC Analysis");
        openProteoQCJButton.setFocusPainted(false);
        openProteoQCJButton.setHorizontalAlignment(SwingConstants.LEFT);
        openProteoQCJButton.setBorder(null);
        openProteoQCJButton.setIconTextGap(5);
        openProteoQCJButton.setBorderPainted(false);
        openProteoQCJButton.setContentAreaFilled(false);
        openProteoQCJButton.addActionListener(this::openProteoQCJButtonActionPerformed);
        openProteoQCJButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                openProteoQCJButtonMouseEntered(evt);
            }
        });
        openProteoQCJButton.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                openProteoQCJButtonMouseExited(evt);
            }
        });

        optionJPanel.setOpaque(false);

        firstPartJPanel.setBorder(null);
        firstPartJPanel.setBackground(Color.WHITE);
        firstPartJPanel.setOpaque(false);
        
        GroupLayout firstPartJPanelLayout = new GroupLayout(firstPartJPanel);
        firstPartJPanel.setLayout(firstPartJPanelLayout);
        
        firstPartJPanelLayout.setHorizontalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE))
        );
        
        firstPartJPanelLayout.setVerticalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createSequentialGroup()
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                )
        );

        secondPartJPanel.setBorder(null);
        secondPartJPanel.setBackground(Color.WHITE);
        secondPartJPanel.setOpaque(false);

        GroupLayout secondPartJPanelLayout = new GroupLayout(secondPartJPanel);
        secondPartJPanel.setLayout(secondPartJPanelLayout);

        secondPartJPanelLayout.setHorizontalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED))
        );

        secondPartJPanelLayout.setVerticalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                        )
        );

        JPanel logoJPanel = new JPanel(){
            Image image=null;

            public void paint(Graphics g){

                try {

                    Graphics2D g2 = (Graphics2D) g;

                    String pngFilePath = PDVMainClass.getJarFilePath() + "/resources/icons/logo.png";

                    image= ImageIO.read(new File(pngFilePath));

                    Map mapH = new HashMap();
                    mapH.put(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
                    mapH.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    g2.setRenderingHints(mapH);

                    g2.draw(new Rectangle(345, 0, 1, 1));

                    g2.drawLine(30,1, 720,0);

                    g2.drawImage(image, 20, 5, 366, 54, null);

                } catch (Exception e) {

                    e.printStackTrace();

                }
            }
        };

        GroupLayout optionJPanelLayout =new GroupLayout(optionJPanel);
        optionJPanel.setLayout(optionJPanelLayout);

        optionJPanelLayout.setHorizontalGroup(
                optionJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(optionJPanelLayout.createSequentialGroup()
                                .addGap(30,40,50)
                                .addComponent(firstPartJPanel, GroupLayout.DEFAULT_SIZE, 375, Short.MAX_VALUE)
                                .addComponent(secondPartJPanel, GroupLayout.DEFAULT_SIZE, 375, Short.MAX_VALUE)
                                .addContainerGap())
                        .addComponent(logoJPanel,GroupLayout.DEFAULT_SIZE, 538, Short.MAX_VALUE)
        );

        optionJPanelLayout.setVerticalGroup(
                optionJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(optionJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(optionJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(firstPartJPanel, GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)
                                        .addComponent(secondPartJPanel, GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)
                                        )
                                .addComponent(logoJPanel, 53, 53, Short.MAX_VALUE)
                                .addContainerGap())
        );

        GroupLayout backgroundPanelLayout = new GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);

        backgroundPanelLayout.setHorizontalGroup(
                backgroundPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(backgroundPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(paintJPanel, GroupLayout.DEFAULT_SIZE, 750, Short.MAX_VALUE)
                                .addComponent(optionJPanel,GroupLayout.DEFAULT_SIZE, 750, Short.MAX_VALUE))
                        .addContainerGap())
        );

        backgroundPanelLayout.setVerticalGroup(
                backgroundPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(paintJPanel, GroupLayout.DEFAULT_SIZE, 331, Short.MAX_VALUE)
                        .addComponent(optionJPanel, GroupLayout.DEFAULT_SIZE, 368, Short.MAX_VALUE)
                        //.addGap(25, 25, 25)
                )
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(backgroundPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(backgroundPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    /**
     * Open the dialog for creating a new database display
     * @param evt Mouse click event
     */
    private void newDatabaseJButtonActionPerformed(ActionEvent evt) {
        new DatabaseImportDialog(this, pdvMainClass, true);
    }

    /**
     * Show the Database tooltip
     * @param evt
     */
    private void newDatabaseResultJButtonMouseEntered(MouseEvent evt) {

        firstPartJPanel.removeAll();

        JLabel databaseJLabel = new JLabel("<html>Visualize database searching result with mzIdentML/pepXML/dat/txt format file. Accept MS/MS data in MGF/mzXML/mzML format.</html>");

        databaseJLabel.setFont(new Font("simsun", Font.ITALIC, 12));

        newDatabaseResultJButton.setForeground(new Color(24, 102,200));

        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        GroupLayout firstPartJPanelLayout = new GroupLayout(firstPartJPanel);
        firstPartJPanel.setLayout(firstPartJPanelLayout);

        firstPartJPanelLayout.setHorizontalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                                .addComponent(databaseJLabel, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))
        );

        firstPartJPanelLayout.setVerticalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createSequentialGroup()
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addComponent(databaseJLabel, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                        )
        );
    }

    /**
     * Disappear the Database tooltip
     * @param evt
     */
    private void newDatabaseResultJButtonMouseExited(MouseEvent evt) {

        newDatabaseResultJButton.setForeground(Color.black);

        setCursor(new java.awt.Cursor(Cursor.DEFAULT_CURSOR));

        firstPartJPanel.removeAll();

        GroupLayout firstPartJPanelLayout = new GroupLayout(firstPartJPanel);
        firstPartJPanel.setLayout(firstPartJPanelLayout);

        firstPartJPanelLayout.setHorizontalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE))
        );

        firstPartJPanelLayout.setVerticalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createSequentialGroup()
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                        )
        );

    }

    /**
     * Open the dialog for creating a new deNovo display
     * @param evt Mouse click event
     */
    private void newDeNovoJButtonActionPerformed (ActionEvent evt){
        new DeNovoImportDialog(this, pdvMainClass);
    }

    /**
     * Show the De novo tooltip
     * @param evt
     */
    private void newDeNovoResultJButtonMouseEntered(MouseEvent evt) {

        JLabel deNovoJLabel = new JLabel("<html>Visualize denovo sequencing result (Casanovo, DeepNovo, Novor, PepNovo, pNovo).</html>");

        deNovoJLabel.setFont(new Font("simsun", Font.ITALIC, 12));

        newDeNovoResultJButton.setForeground(new Color(24, 102,200));

        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        GroupLayout firstPartJPanelLayout = new GroupLayout(firstPartJPanel);
        firstPartJPanel.setLayout(firstPartJPanelLayout);

        firstPartJPanelLayout.setHorizontalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(deNovoJLabel, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE))
        );

        firstPartJPanelLayout.setVerticalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createSequentialGroup()
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addComponent(deNovoJLabel, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                        )
        );
    }

    /**
     * Disappear the De novo tooltip
     * @param evt
     */
    private void newDeNovoResultJButtonMouseExited(MouseEvent evt) {

        firstPartJPanel.removeAll();

        newDeNovoResultJButton.setForeground(Color.black);

        setCursor(new java.awt.Cursor(Cursor.DEFAULT_CURSOR));

        GroupLayout firstPartJPanelLayout = new GroupLayout(firstPartJPanel);
        firstPartJPanel.setLayout(firstPartJPanelLayout);

        firstPartJPanelLayout.setHorizontalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE))
        );

        firstPartJPanelLayout.setVerticalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createSequentialGroup()
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                        )
        );
    }

    /**
     * Open the dialog for creating a single display
     *  @param evt Mouse click event
     */
    private void singlePeptideDisplayJButtonActionPerformed(ActionEvent evt){
        this.setVisible(false);
        new SinglePeptideDisplay();
    }

    /**
     * Show the single peptide tooltip
     * @param evt
     */
    private void singlePeptideDisplayJButtonMouseEntered(MouseEvent evt) {

        firstPartJPanel.removeAll();

        JLabel singlePeptideJLabel = new JLabel("<html>Visualize a single PSM. Import a peptide sequence and a USI or an MS/MS spectrum in MGF format.</html>");

        singlePeptideJLabel.setFont(new Font("simsun", Font.ITALIC, 12));

        singlePeptideDisplayJButton.setForeground(new Color(24, 102,200));

        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        GroupLayout firstPartJPanelLayout = new GroupLayout(firstPartJPanel);
        firstPartJPanel.setLayout(firstPartJPanelLayout);

        firstPartJPanelLayout.setHorizontalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(singlePeptideJLabel, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE))
        );

        firstPartJPanelLayout.setVerticalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createSequentialGroup()
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addComponent(singlePeptideJLabel, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                        )
        );
    }

    /**
     * Disappear the single peptide tooltip
     * @param evt
     */
    private void singlePeptideDisplayJButtonMouseExited(MouseEvent evt) {

        firstPartJPanel.removeAll();

        singlePeptideDisplayJButton.setForeground(Color.black);

        setCursor(new java.awt.Cursor(Cursor.DEFAULT_CURSOR));

        GroupLayout firstPartJPanelLayout = new GroupLayout(firstPartJPanel);
        firstPartJPanel.setLayout(firstPartJPanelLayout);

        firstPartJPanelLayout.setHorizontalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE))
        );

        firstPartJPanelLayout.setVerticalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createSequentialGroup()
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                        )
        );
    }

    /**
     * Open the dialog for creating a proBam display
     *  @param evt Mouse click event
     */
    private void openProBamJButtonActionPerformed(ActionEvent evt){
        new ProBamImportDialog(this, pdvMainClass);
    }

    /**
     * Show the proBam tooltip
     * @param evt
     */
    private void openProBamJButtonMouseEntered(MouseEvent evt) {

        firstPartJPanel.removeAll();

        JLabel proBamJLabel = new JLabel("<html>Visualize proBAM/proBed result.</html>");
        proBamJLabel.setFont(new Font("simsun", Font.ITALIC, 12));

        openProBamJButton.setForeground(new Color(24, 102,200));

        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        GroupLayout firstPartJPanelLayout = new GroupLayout(firstPartJPanel);
        firstPartJPanel.setLayout(firstPartJPanelLayout);

        firstPartJPanelLayout.setHorizontalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(proBamJLabel, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE))
        );

        firstPartJPanelLayout.setVerticalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createSequentialGroup()
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addComponent(proBamJLabel, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                        )
        );
    }

    /**
     * Disappear the proBam tooltip
     * @param evt
     */
    private void openProBamJButtonMouseExited(MouseEvent evt) {

        firstPartJPanel.removeAll();

        openProBamJButton.setForeground(Color.black);

        setCursor(new java.awt.Cursor(Cursor.DEFAULT_CURSOR));

        GroupLayout firstPartJPanelLayout = new GroupLayout(firstPartJPanel);
        firstPartJPanel.setLayout(firstPartJPanelLayout);

        firstPartJPanelLayout.setHorizontalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE))
        );

        firstPartJPanelLayout.setVerticalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createSequentialGroup()
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                        )
        );
    }

    /**
     * Open the dialog for creating maxquant display
     */
    private void openMaxQuantJButtonActionPerformed(ActionEvent evt){
        new MaxQuantImportDialog(this, pdvMainClass);
    }

    /**
     * Show the MaxQuant tooltip
     * @param evt
     */
    private void openMaxQuantJButtonMouseEntered(MouseEvent evt) {

        firstPartJPanel.removeAll();

        JLabel maxQuantJLabel = new JLabel("<html>Visualize MaxQuant result.</html>");

        maxQuantJLabel.setFont(new Font("simsun", Font.ITALIC, 12));

        openMaxQuantJButton.setForeground(new Color(24, 102,200));

        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        GroupLayout firstPartJPanelLayout = new GroupLayout(firstPartJPanel);
        firstPartJPanel.setLayout(firstPartJPanelLayout);

        firstPartJPanelLayout.setHorizontalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(maxQuantJLabel, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE))
        );

        firstPartJPanelLayout.setVerticalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createSequentialGroup()
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addComponent(maxQuantJLabel, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                        )
        );
    }

    /**
     * Disappear the MaxQuant tooltip
     * @param evt
     */
    private void openMaxQuantJButtonMouseExited(MouseEvent evt) {

        firstPartJPanel.removeAll();

        openMaxQuantJButton.setForeground(Color.black);

        setCursor(new java.awt.Cursor(Cursor.DEFAULT_CURSOR));

        GroupLayout firstPartJPanelLayout = new GroupLayout(firstPartJPanel);
        firstPartJPanel.setLayout(firstPartJPanelLayout);

        firstPartJPanelLayout.setHorizontalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE))
        );

        firstPartJPanelLayout.setVerticalGroup(
                firstPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstPartJPanelLayout.createSequentialGroup()
                                .addComponent(newDatabaseResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newDeNovoResultJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openProBamJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(singlePeptideDisplayJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openMaxQuantJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                        )
        );
    }

    /**
     * Open the dialog for spectrum library display
     * @param evt
     */
    private void openSpectrumLibJButtonActionPerformed(ActionEvent evt){
        new SpectrumLibImportDialog(this);
    }

    /**
     * Show the Spectrum lib tooltip
     * @param evt
     */
    private void openSpectrumLibJButtonMouseEntered(MouseEvent evt) {

        secondPartJPanel.removeAll();

        openSpectrumLibJButton.setForeground(new Color(24, 102,200));

        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        JLabel spectrumLibJLabel = new JLabel("<html>Visualize Spectrum library.</html>");
        spectrumLibJLabel.setFont(new Font("simsun", Font.ITALIC, 12));

        GroupLayout secondPartJPanelLayout = new GroupLayout(secondPartJPanel);
        secondPartJPanel.setLayout(secondPartJPanelLayout);

        secondPartJPanelLayout.setHorizontalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(spectrumLibJLabel, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                        .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED))
        );

        secondPartJPanelLayout.setVerticalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addComponent(spectrumLibJLabel, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addGroup(secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                        )
        );
    }

    /**
     * Disappear the spectrum lib tooltip
     * @param evt
     */
    private void openSpectrumLibJButtonMouseExited(MouseEvent evt) {

        secondPartJPanel.removeAll();

        openSpectrumLibJButton.setForeground(Color.black);

        setCursor(new java.awt.Cursor(Cursor.DEFAULT_CURSOR));

        GroupLayout secondPartJPanelLayout = new GroupLayout(secondPartJPanel);
        secondPartJPanel.setLayout(secondPartJPanelLayout);

        secondPartJPanelLayout.setHorizontalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED))
        );

        secondPartJPanelLayout.setVerticalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                        )
        );
    }

    /**
     * Open the JDialog to import pride XML file
     * @param evt
     */
    private void openPrideXMLJButtonActionPerformed(ActionEvent evt){
        new PrideXMLDisplay(this);
    }

    /**
     * Show the pride tooltip
     * @param evt
     */
    private void openPrideXMLJButtonMouseEntered(MouseEvent evt) {

        secondPartJPanel.removeAll();

        openPrideXMLJButton.setForeground(new Color(24, 102,200));

        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        JLabel prideXMLJLabel = new JLabel("<html>Visualize PRIDE XML result.</html>");
        prideXMLJLabel.setFont(new Font("simsun", Font.ITALIC, 12));

        GroupLayout secondPartJPanelLayout = new GroupLayout(secondPartJPanel);
        secondPartJPanel.setLayout(secondPartJPanelLayout);

        secondPartJPanelLayout.setHorizontalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(prideXMLJLabel, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                        .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED))
        );

        secondPartJPanelLayout.setVerticalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addComponent(prideXMLJLabel, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addGroup(secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                        )
        );
    }

    /**
     * Disappear the pride tooltip
     * @param evt
     */
    private void openPrideXMLJButtonMouseExited(MouseEvent evt) {

        secondPartJPanel.removeAll();

        openPrideXMLJButton.setForeground(Color.black);

        setCursor(new java.awt.Cursor(Cursor.DEFAULT_CURSOR));

        GroupLayout secondPartJPanelLayout = new GroupLayout(secondPartJPanel);
        secondPartJPanel.setLayout(secondPartJPanelLayout);

        secondPartJPanelLayout.setHorizontalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED))
        );

        secondPartJPanelLayout.setVerticalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                        )
        );
    }

    /**
     * Open the mzml file
     * @param evt
     */
    private void openSpectrumFileJButtonActionPerformed(ActionEvent evt){
        new MSDataDisplay(this);
    }

    /**
     * Show the mzml tooltip
     * @param evt
     */
    private void openSpectrumFileJButtonMouseEntered(MouseEvent evt) {

        secondPartJPanel.removeAll();

        openSpectrumFileJButton.setForeground(new Color(24, 102,200));

        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        JLabel openSpectrumFileJLabel = new JLabel("<html>Visualize MS data in mzML/mzXML format.</html>");
        openSpectrumFileJLabel.setFont(new Font("simsun", Font.ITALIC, 12));

        GroupLayout secondPartJPanelLayout = new GroupLayout(secondPartJPanel);
        secondPartJPanel.setLayout(secondPartJPanelLayout);

        secondPartJPanelLayout.setHorizontalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openSpectrumFileJLabel, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED))
        );

        secondPartJPanelLayout.setVerticalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addComponent(openSpectrumFileJLabel, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addGroup(secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                        )
        );
    }

    /**
     * Disappear the mzml tooltip
     * @param evt
     */
    private void openSpectrumFileJButtonMouseExited(MouseEvent evt) {

        secondPartJPanel.removeAll();

        openSpectrumFileJButton.setForeground(Color.black);

        setCursor(new java.awt.Cursor(Cursor.DEFAULT_CURSOR));

        GroupLayout secondPartJPanelLayout = new GroupLayout(secondPartJPanel);
        secondPartJPanel.setLayout(secondPartJPanelLayout);

        secondPartJPanelLayout.setHorizontalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED))
        );

        secondPartJPanelLayout.setVerticalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                        )
        );
    }

    /**
     * Open the JDialog to run proteoQC
     * @param evt
     */
    private void openProteoQCJButtonActionPerformed(ActionEvent evt){
        new ProteoQCParaJDialog(this);
    }

    /**
     * Show the proteoQC tooltip
     * @param evt
     */
    private void openProteoQCJButtonMouseEntered(MouseEvent evt) {

        secondPartJPanel.removeAll();

        openProteoQCJButton.setForeground(new Color(24, 102,200));

        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        JLabel openProteoQCJLabel = new JLabel("<html>Run ProteoQC in R.</html>");
        openProteoQCJLabel.setFont(new Font("simsun", Font.ITALIC, 12));

        GroupLayout secondPartJPanelLayout = new GroupLayout(secondPartJPanel);
        secondPartJPanel.setLayout(secondPartJPanelLayout);

        secondPartJPanelLayout.setHorizontalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openProteoQCJLabel, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED))
        );

        secondPartJPanelLayout.setVerticalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                                .addComponent(openProteoQCJLabel, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                        )
        );
    }

    /**
     * Disappear the proteoQC tooltip
     * @param evt
     */
    private void openProteoQCJButtonMouseExited(MouseEvent evt) {

        secondPartJPanel.removeAll();

        openProteoQCJButton.setForeground(Color.black);

        setCursor(new java.awt.Cursor(Cursor.DEFAULT_CURSOR));

        GroupLayout secondPartJPanelLayout = new GroupLayout(secondPartJPanel);
        secondPartJPanel.setLayout(secondPartJPanelLayout);

        secondPartJPanelLayout.setHorizontalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED))
        );

        secondPartJPanelLayout.setVerticalGroup(
                secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(secondPartJPanelLayout.createSequentialGroup()
                                .addComponent(openSpectrumLibJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openSpectrumFileJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openPrideXMLJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(secondPartJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(openProteoQCJButton, GroupLayout.DEFAULT_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                        )
        );
    }

    /**
     * Close welcome dialog and main class
     * @param evt window click event
     */
    private void formWindowClosing(WindowEvent evt) {
        pdvMainClass.close();
    }
    
    /**
     * Visualize this dialog or not
     * @param visible Boolean to be visible or not
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible) {
            ((DummyFrame) getParent()).dispose();
        }
    }

    @Override
    public void setIconImage(Image image) {
        super.setIconImage(image);
        dummyParentFrame.setIconImage(image);
    }
}
