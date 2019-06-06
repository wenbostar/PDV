package PDVGUI.utils;

import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.pride.CvTerm;
import uk.ac.ebi.pride.utilities.pridemod.io.unimod.model.*;
import uk.ac.ebi.pride.utilities.pridemod.io.unimod.xml.UnimodReader;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Import all PTMs from unimod
 * Created by Ken on 7/25/2017.
 */
public class ImportPTMsFromUnimod {

    private File unimodXMLFile;
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    private HashMap<String, String> titleTFullName = new HashMap<>();

    /**
     * Constructor
     * @param unimodXMLFile Unimod file
     */
    public ImportPTMsFromUnimod(File unimodXMLFile){
        this.unimodXMLFile = unimodXMLFile;

        ptmFactory.clearFactory();
        parsingFile();
    }

    /**
     * Reading xml file
     */
    private void parsingFile(){
        try {

            ptmFactory.clearFactory();

            PTM ptm = null;
            String ptmName;

            ArrayList<String > residues;

            ArrayList<String> testSite = new ArrayList<>();
            ArrayList<String> testPosition = new ArrayList<>();

            InputStream inputStream = new FileInputStream(unimodXMLFile);

            UnimodReader unimodreader = new UnimodReader(inputStream);

            Unimod unimod = unimodreader.getUnimodObject();

            List<UnimodModification> unimodModificationList = unimod.getModifications().getMod();

            //Parsing and get each modification;
            for (UnimodModification unimodModification: unimodModificationList){

                Double monoMass = unimodModification.getDelta().getMonoMass().doubleValue();//Get modification Mass
                String modificationTitle = unimodModification.getTitle();
                String modificationName = unimodModification.getFullName();
                titleTFullName.put(modificationTitle, modificationName);

                BigInteger recordID = unimodModification.getRecordId();

                CvTerm cvTerm = new CvTerm("",  "UNIMOD:" + String.valueOf(recordID), "", "");

                //Get amino acid modification detail;
                List<Specificity> specificityList = unimodModification.getSpecificity();
                for(Specificity specificity: specificityList){
                    String site = specificity.getSite();
                    String position = specificity.getPosition();
                    String classfication = specificity.getClassification();

                    if(site.equals("N-term")){
                        if(position.equals("Any N-term")){
                            ptmName = modificationTitle + " of " + site;
                            ptm = new PTM(PTM.MODNP, ptmName, monoMass, null);
                            if(modificationTitle.contains(">")){
                                modificationTitle = modificationTitle.replace(">","&gt;");
                            }
                            ptm.setShortName(modificationTitle);
                        }else if(position.equals("Protein N-term")){
                            ptmName = modificationTitle + " of protein " + site;
                            ptm = new PTM(PTM.MODN, ptmName, monoMass, null);
                            if(modificationTitle.contains(">")){
                                modificationTitle = modificationTitle.replace(">","&gt;");
                            }
                            ptm.setShortName(modificationTitle);
                        }
                    }else if(site.equals("C-term")){
                        if(position.equals("Any C-term")){
                            ptmName = modificationTitle + " of " + site;
                            ptm = new PTM(PTM.MODCP, ptmName, monoMass, null);
                            if(modificationTitle.contains(">")){
                                modificationTitle = modificationTitle.replace(">","&gt;");
                            }
                            ptm.setShortName(modificationTitle);
                        }else if(position.equals("Protein C-term")){
                            ptmName = modificationTitle + " of protein " + site;
                            ptm = new PTM(PTM.MODC, ptmName, monoMass, null);
                            if(modificationTitle.contains(">")){
                                modificationTitle = modificationTitle.replace(">","&gt;");
                            }
                            ptm.setShortName(modificationTitle);
                        }
                    }else {
                        residues = new ArrayList<>();
                        residues .add(site);
                        if(position.equals("Any N-term")){
                            ptmName = modificationTitle + " of " + site;
                            ptm = new PTM(PTM.MODNPAA, ptmName, monoMass, residues);
                            if(modificationTitle.contains(">")){
                                modificationTitle = modificationTitle.replace(">","&gt;");
                            }
                            ptm.setShortName(modificationTitle);
                        }else if(position.equals("Protein N-term")){
                            ptmName = modificationTitle + " of protein " + site;
                            ptm = new PTM(PTM.MODNAA, ptmName, monoMass, residues);
                            if(modificationTitle.contains(">")){
                                modificationTitle = modificationTitle.replace(">","&gt;");
                            }
                            ptm.setShortName(modificationTitle);
                        }else if (position.equals("Any C-term")){
                            ptmName = modificationTitle + " of " + site;
                            ptm = new PTM(PTM.MODCPAA, ptmName, monoMass, residues);
                            if(modificationTitle.contains(">")){
                                modificationTitle = modificationTitle.replace(">","&gt;");
                            }
                            ptm.setShortName(modificationTitle);
                        }else if(position.equals("Protein C-term")){
                            ptmName = modificationTitle + " of protein " + site;
                            ptm = new PTM(PTM.MODCAA, ptmName, monoMass, residues);
                            if(modificationTitle.contains(">")){
                                modificationTitle = modificationTitle.replace(">","&gt;");
                            }
                            ptm.setShortName(modificationTitle);
                        }else {

                            ptmName = modificationTitle + " of " + site;
                            ptm = new PTM(PTM.MODAA, ptmName, monoMass, residues);

                            if (site.equals("T") || site.equals("S")){
                                if (monoMass < 80.01 && monoMass > 79.9){
                                    ptm.addNeutralLoss(NeutralLoss.H3PO4);
                                }
                            }

                            if(modificationTitle.contains(">")){
                                modificationTitle = modificationTitle.replace(">","&gt;");
                            }
                            ptm.setShortName(modificationTitle);
                        }
                    }

                    cvTerm.setName(modificationTitle);

                    ptm.setCvTerm(cvTerm);

                    ptmFactory.addUserPTM(ptm); // add modification into ptmFactory

                    if(!testSite.contains(site)){
                        testSite.add(site);
                    }
                    if(!testPosition.contains(position)){
                        testPosition.add(position);
                    }
                }
            }

        } catch (FileNotFoundException | JAXBException e) {
            e.printStackTrace();
        }
    }
}
