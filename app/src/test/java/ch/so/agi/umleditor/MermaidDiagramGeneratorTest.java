package ch.so.agi.umleditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.ehi.basics.settings.Settings;
import ch.ehi.uml1_4.implementation.UmlModel;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MermaidDiagramGeneratorTest {

    @TempDir
    File tempDirectory;

    @Test
    public void blackboxBinary_Ok() throws Exception {
        // Prepare
        tempDirectory = Paths.get("/Users/stefan/tmp/").toFile();
        
        Path iliFile = Paths.get("src/test/data/Gewaesserraum_LegendeEintrag_OEREBZusatz_V1_1.ili");
        UmlModel model = UmlEditorUtility.iliimport(iliFile, null);

        Settings settings = new Settings();
        settings.setValue(PlantUMLDiagramGenerator.SHOW_ATTRIBUTES, String.valueOf(true));
        settings.setValue(PlantUMLDiagramGenerator.SHOW_ATTRIBUTE_TYPES, String.valueOf(true));
        settings.setValue(PlantUMLDiagramGenerator.SHOW_CARDINALITIES_OF_ATTRIBUTES, String.valueOf(true));
        settings.setValue(PlantUMLDiagramGenerator.SHOW_CARDINALITIES, String.valueOf(true));

        // Execute test
        DiagramGenerator diagramGenerator = new MermaidDiagramGenerator();
        diagramGenerator.export(model, tempDirectory.toPath(), settings);

    }

}
