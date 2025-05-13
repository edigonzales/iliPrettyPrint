package ch.so.agi.pprint;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import ch.ehi.interlis.modeltopicclass.INTERLIS2Def;
import ch.ehi.uml1_4.foundation.core.Namespace;
import ch.ehi.uml1_4.implementation.UmlModel;
import ch.ehi.umleditor.application.ElementFactory;
import ch.ehi.umleditor.interlis.iliimport.TransferFromIli2cMetamodel;
import ch.interlis.ili2c.Ili2cSettings;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.GenerateOutputKind;
import ch.interlis.ili2c.generator.nls.ModelElements;
import ch.interlis.ili2c.metamodel.TransferDescription;

public class PrettyPrint {

    public static boolean run(File iliFiles[], Path outputDir, String modeldir) {
        Configuration config = new Configuration();
        for (int filei = 0; filei < iliFiles.length; filei++) {
            config.addFileEntry(new ch.interlis.ili2c.config.FileEntry(
                    iliFiles[filei].getAbsolutePath(), ch.interlis.ili2c.config.FileEntryKind.ILIMODELFILE));
        }
        config.setGenerateWarnings(false);
        config.setOutputKind(GenerateOutputKind.NOOUTPUT);
        config.setAutoCompleteModelList(true);
        
        ch.ehi.basics.settings.Settings settings = new ch.ehi.basics.settings.Settings();
        
        if (modeldir != null) {
            settings.setValue(Ili2cSettings.ILIDIRS, modeldir);            
        } else {
            settings.setValue(Ili2cSettings.ILIDIRS, Ili2cSettings.DEFAULT_ILIDIRS);
        }

        TransferDescription ili2cModel = ch.interlis.ili2c.Main.runCompiler(config, settings);
        if (ili2cModel != null) {
            
            UmlModel model = (UmlModel) ElementFactory.createObject(ch.ehi.uml1_4.implementation.UmlModel.class);

            TransferFromIli2cMetamodel convert = new TransferFromIli2cMetamodel();
            convert.visitTransferDescription(model, ili2cModel, null, config);

            TransferToPlantUml transferToPlantUml =  new TransferToPlantUml();
            try {
                System.out.println("vorher");
                transferToPlantUml.export(model, new File("/Users/stefan/tmp/foo.puml"));
                System.out.println("nachher");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
//            TransferFromUmlMetamodel writer = new TransferFromUmlMetamodel(outputDir);
//            Iterator<UmlModel> modelI = model.iteratorOwnedElement();
//            while (modelI.hasNext()) {
//                Object obj = modelI.next();
//                if (obj instanceof INTERLIS2Def) {
//                    INTERLIS2Def objnew = (INTERLIS2Def) obj;
//                    try {
//                        writer.getFileList(objnew.getNamespace());
//                        writer.writeIliFile(objnew);
//                    } catch (IOException ex) {
//                       ex.printStackTrace();
//                       return false;
//                    }
//                } else {
//                    Iterator i = ch.ehi.interlis.tools.ModelElementUtility.getChildElements((Namespace) obj, null)
//                            .iterator();
//                    while (i.hasNext()) {
//                        Object obji = i.next();
//                        if (obji instanceof INTERLIS2Def) {
//                            INTERLIS2Def objnew = (INTERLIS2Def) obji;
//                            if (!objnew.getName().getValue().startsWith("<")) {
//                                try {
//                                    writer.getFileList(objnew.getNamespace());
//                                    writer.writeIliFile(objnew);
//                                } catch (IOException ex) {
//                                    ex.printStackTrace();
//                                    return false;
//                                }
//                            }
//                        }
//                    }
//                }
//            }
            return true; 
        } else {
            return false;
        }
    }
}
