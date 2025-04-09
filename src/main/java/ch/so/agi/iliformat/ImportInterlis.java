package ch.so.agi.iliformat;

import java.io.File;
import java.nio.file.Paths;
import java.util.Iterator;

import ch.interlis.ili2c.Ili2cException;
import ch.interlis.ili2c.Ili2cSettings;
import ch.interlis.ili2c.config.*;
import ch.interlis.ili2c.metamodel.TransferDescription;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.interlis.modeltopicclass.INTERLIS2Def;
import ch.ehi.umleditor.application.ElementFactory;
import ch.ehi.umleditor.interlis.iliexport.ExportInterlis;
import ch.ehi.umleditor.translationxml.TransferToXml;
import ch.ehi.uml1_4.modelmanagement.Model;
import ch.ehi.uml1_4.foundation.core.Namespace;
import ch.ehi.uml1_4.implementation.UmlModel;

public class ImportInterlis {
    
    public static void readIliFile(File iliFiles[]) {
        //ch.ehi.umleditor.interlis.iliimport.TransferFromIli2cMetamodel convert = new ch.ehi.umleditor.interlis.iliimport.TransferFromIli2cMetamodel();
        TransferFromIli2cMetamodel convert = new TransferFromIli2cMetamodel();
        
        Configuration config = new Configuration();
        for (int filei = 0; filei < iliFiles.length; filei++) {
             config.addFileEntry(new ch.interlis.ili2c.config.FileEntry(
                     iliFiles[filei].getAbsolutePath(),ch.interlis.ili2c.config.FileEntryKind.ILIMODELFILE));
        }
        config.setGenerateWarnings(false);
        config.setOutputKind(GenerateOutputKind.NOOUTPUT);
        config.setAutoCompleteModelList(true);
        
        ch.ehi.basics.settings.Settings settings = new ch.ehi.basics.settings.Settings();
        settings.setValue(Ili2cSettings.ILIDIRS, Ili2cSettings.DEFAULT_ILIDIRS);

        TransferDescription ili2cModel = ch.interlis.ili2c.Main.runCompiler(config, settings);
        if (ili2cModel != null) {
            // translate the compiler metamodel to our metamodel
            
            UmlModel model = (UmlModel) ElementFactory.createObject(ch.ehi.uml1_4.implementation.UmlModel.class);

            
            convert.visitTransferDescription(model, ili2cModel, null, config);
          
            
            Iterator modelI = model.iteratorOwnedElement();
            while (modelI.hasNext()) {
                Object obj = modelI.next();
                if (obj instanceof INTERLIS2Def) {
                    //modelElementHelper(modelElements,obj);
                } else {
                    Iterator i = ch.ehi.interlis.tools.ModelElementUtility.getChildElements((Namespace) obj, null)
                            .iterator();
                    while (i.hasNext()) {
                        INTERLIS2Def objnew = (INTERLIS2Def) i.next();
                        System.out.println(objnew.getName().getValue());
                        //modelElementHelper(modelElements,objnew);
                        
                        if (!objnew.getName().getValue().startsWith("<")) {
//                            ExportInterlis exportInterlis = new ExportInterlis();
//                            exportInterlis.writeIli(objnew);
                            
                            TransferFromUmlMetamodel writer = new TransferFromUmlMetamodel();
                            try {
                                writer.getFileList(objnew.getNamespace());
                                writer.writeIliFile(objnew, Paths.get("/Users/stefan/tmp/"));
                            } catch (java.io.IOException ex) {
                               ex.printStackTrace();
                            }
                        }
                        
                    }
                }
            }

            
            //ExportInterlis exportInterlis = new ExportInterlis();
            //exportInterlis.writeIli(model);
           
//            TransferToXml transferToXml = new TransferToXml();
//            try {
//                transferToXml.export(model, new File("/Users/stefan/tmp/foo.xml"));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            
        }
    }

    
}
