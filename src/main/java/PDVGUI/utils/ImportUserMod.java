package PDVGUI.utils;

import PDVGUI.gui.PDVMainClass;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;

import java.io.*;
import java.util.ArrayList;

public class ImportUserMod {

    /**
     * PTM factory
     */
    PTMFactory ptmFactory = PTMFactory.getInstance();

    public ImportUserMod(){

        try {
            importPTM();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void importPTM() throws IOException {
        File inputFile = new File(PDVMainClass.getJarFilePath() + "/resources/conf/user_mod.txt");

        BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile));

        String line;
        int count = 0;
        while ((line = bufferedReader.readLine()) != null){
            if (count != 0){
                ArrayList<String> residues = new ArrayList<>();
                String modification = line.split("\t")[0];
                String aa = modification.split(" of ")[1];
                residues.add(aa);
                String modName = modification.split(" of ")[0];
                double modMass = Double.parseDouble(line.split("\t")[1]);
                PTM ptm;
                if (aa.contains("N-term")){
                    ptm = new PTM(PTM.MODNP, modification, modMass, null);
                } else if (aa.contains("C-term")){
                    ptm = new PTM(PTM.MODCP, modification, modMass, null);
                } else {
                    ptm = new PTM(PTM.MODAA, modification, modMass, residues);
                }
                ptm.setShortName(modName);
                ptmFactory.addUserPTM(ptm);
            }
            count ++;
        }
    }
}
