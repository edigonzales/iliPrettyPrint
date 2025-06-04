package ch.so.agi.umleditor;

import java.io.File;
import java.nio.file.Path;

import ch.ehi.uml1_4.implementation.UmlModel;

public class PrettyPrint {
    
    public static boolean run(Path iliFile, Path outputDir, String modeldir) {
        return UmlEditorUtility.prettyPrint(iliFile, modeldir, outputDir);        
    }
}
