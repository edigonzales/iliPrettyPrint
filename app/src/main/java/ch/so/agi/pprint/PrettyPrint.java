package ch.so.agi.pprint;

import java.io.File;
import java.nio.file.Path;

import ch.ehi.uml1_4.implementation.UmlModel;

public class PrettyPrint {
    
    public static boolean run(File iliFiles[], Path outputDir, String modeldir) {
        return UmlEditorUtility.prettyPrint(iliFiles, modeldir, outputDir);        
    }
}
