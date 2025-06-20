package ch.so.agi.umleditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.ehi.basics.settings.Settings;
import ch.ehi.uml1_4.implementation.UmlModel;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MermaidDiagramGeneratorTest {

    @TempDir
    File tempDirectory;

    @Test
    public void blackboxBinary_Ok() throws Exception {
        // Prepare
        //tempDirectory = Paths.get("/Users/stefan/tmp/").toFile();
        
        Path iliFile = Paths.get("src/test/data/Gewaesserraum_LegendeEintrag_OEREBZusatz_V1_1.ili");
        UmlModel model = UmlEditorUtility.iliimport(iliFile, null);

        // Execute test
        DiagramGenerator diagramGenerator = new MermaidDiagramGenerator();
        diagramGenerator.export(model, tempDirectory.toPath(), getSettings());
        
        // Validate
        String fileContent = readMermaidFile(tempDirectory.toPath());
        assertTrue(fileContent.contains("Symbol [1] : BLACKBOX BINARY"));
    }
    
    @Test
    public void singleMultiplicity_Ok() throws Exception {
        // Prepare
        //tempDirectory = Paths.get("/Users/stefan/tmp/").toFile();
        
        Path iliFile = Paths.get("src/test/data/Gewaesserraum_LegendeEintrag_OEREBZusatz_V1_1.ili");
        UmlModel model = UmlEditorUtility.iliimport(iliFile, null);

        // Execute test
        DiagramGenerator diagramGenerator = new MermaidDiagramGenerator();
        diagramGenerator.export(model, tempDirectory.toPath(), getSettings());
        
        // Validate
        String fileContent = readMermaidFile(tempDirectory.toPath());
        assertTrue(fileContent.contains("Symbol [1] : BLACKBOX BINARY"));
    }
    
    // TODO
    @Test
    public void range_Ok() throws Exception {
        // Prepare
        tempDirectory = Paths.get("/Users/stefan/tmp/").toFile();
        
        Path iliFile = Paths.get("src/test/data/Paerke_V1_3.ili");
        UmlModel model = UmlEditorUtility.iliimport(iliFile, null);

        // Execute test
        DiagramGenerator diagramGenerator = new MermaidDiagramGenerator();
        diagramGenerator.export(model, tempDirectory.toPath(), getSettings());
        
        // Validate
//        String fileContent = readMermaidFile(tempDirectory.toPath());
//        assertTrue(fileContent.contains("Symbol [1] : BLACKBOX BINARY"));
    }

    
    private Settings getSettings() {
        Settings settings = new Settings();
        settings.setValue(PlantUMLDiagramGenerator.SHOW_ATTRIBUTES, String.valueOf(true));
        settings.setValue(PlantUMLDiagramGenerator.SHOW_ATTRIBUTE_TYPES, String.valueOf(true));
        settings.setValue(PlantUMLDiagramGenerator.SHOW_CARDINALITIES_OF_ATTRIBUTES, String.valueOf(true));
        settings.setValue(PlantUMLDiagramGenerator.SHOW_CARDINALITIES, String.valueOf(true));
        return settings;
    }
    
    private String readMermaidFile(Path folder) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.mmd")) {
            Path mmdFile = null;

            for (Path entry : stream) {
                mmdFile = entry;  // assuming only one *.mmd file
                break;
            }

            if (mmdFile == null) {
                return null;
            }

            String content = Files.readString(mmdFile);
            return content;

        } catch (IOException | DirectoryIteratorException e) {
            e.printStackTrace();
            return null;
        }
    }
}
