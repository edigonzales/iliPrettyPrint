package ch.so.agi.umleditor;

import java.nio.file.Path;

public class PrettyPrint {
    
    public static boolean run(Path iliFile, Path outputDir, String modeldir) {
        return UmlEditorUtility.prettyPrint(iliFile, modeldir, outputDir);        
    }
}
