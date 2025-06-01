package ch.so.agi.pprint;

import java.io.File;
import java.nio.file.Path;

import ch.ehi.uml1_4.implementation.UmlModel;

public class PrettyPrint {
    
    public static boolean run(File iliFiles[], Path outputDir, String modeldir) {
        
        UmlModel model = UmlEditorUtility.iliimport(iliFiles, modeldir);
        
        if (model != null) {
            return UmlEditorUtility.iliexport(outputDir, model);
        }
        
        return false;        
    }
}
