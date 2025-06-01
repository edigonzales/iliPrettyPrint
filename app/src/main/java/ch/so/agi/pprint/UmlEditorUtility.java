package ch.so.agi.pprint;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import ch.ehi.interlis.modeltopicclass.INTERLIS2Def;
import ch.ehi.uml1_4.foundation.core.Namespace;
import ch.ehi.uml1_4.implementation.UmlModel;
import ch.ehi.umleditor.application.ElementFactory;
import ch.ehi.umleditor.interlis.iliimport.TransferFromIli2cMetamodel;
import ch.interlis.ili2c.Ili2cSettings;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.FileEntry;
import ch.interlis.ili2c.config.GenerateOutputKind;
import ch.interlis.ili2c.metamodel.TransferDescription;

public class UmlEditorUtility {
    
    public static UmlModel iliimport(File iliFiles[], String modeldir) {
        Configuration config = new Configuration();
        for (int filei = 0; filei < iliFiles.length; filei++) {
            config.addFileEntry(new FileEntry(iliFiles[filei].getAbsolutePath(),
                    ch.interlis.ili2c.config.FileEntryKind.ILIMODELFILE));
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
            
            UmlModel model = (UmlModel) ElementFactory.createObject(UmlModel.class);

            TransferFromIli2cMetamodel convert = new TransferFromIli2cMetamodel();
            convert.visitTransferDescription(model, ili2cModel, null, config);
            
            return model;
        }
        return null;
    }
    
    public static boolean iliexport(Path outputDir, UmlModel model) {
        TransferFromUmlMetamodel writer = new TransferFromUmlMetamodel(outputDir);
        Iterator<UmlModel> modelI = model.iteratorOwnedElement();
        while (modelI.hasNext()) {
            Object obj = modelI.next();
            if (obj instanceof INTERLIS2Def) {
                INTERLIS2Def objnew = (INTERLIS2Def) obj;
                try {
                    writer.getFileList(objnew.getNamespace());
                    writer.writeIliFile(objnew);
                } catch (IOException ex) {
                   ex.printStackTrace();
                   return false;
                }
            } else {
                Iterator i = ch.ehi.interlis.tools.ModelElementUtility.getChildElements((Namespace) obj, null)
                        .iterator();
                while (i.hasNext()) {
                    Object obji = i.next();
                    if (obji instanceof INTERLIS2Def) {
                        INTERLIS2Def objnew = (INTERLIS2Def) obji;
                        if (!objnew.getName().getValue().startsWith("<")) {
                            try {
                                writer.getFileList(objnew.getNamespace());
                                writer.writeIliFile(objnew);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
    
    public static boolean umlexport(Path outputDir, String umlFileName, UmlModel model, UmlDiagramVendor vendor) {
        TransferToUml transferToUml = UmlDiagramGeneratorFactory.getGenerator(vendor);
        try {
            ch.ehi.basics.settings.Settings settings = new ch.ehi.basics.settings.Settings();
            settings.setValue(TransferToPlantUml.SHOW_ATTRIBUTES, String.valueOf(true));
            settings.setValue(TransferToPlantUml.SHOW_ATTRIBUTE_TYPES, String.valueOf(true));
            settings.setValue(TransferToPlantUml.SHOW_CARDINALITIES_OF_ATTRIBUTES, String.valueOf(true));
            settings.setValue(TransferToPlantUml.SHOW_CARDINALITIES, String.valueOf(true));
            transferToUml.export(model, outputDir.resolve(umlFileName), settings);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
