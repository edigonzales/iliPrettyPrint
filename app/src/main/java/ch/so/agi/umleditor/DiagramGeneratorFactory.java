package ch.so.agi.umleditor;

public class DiagramGeneratorFactory {
    public static DiagramGenerator getGenerator(UmlDiagramVendor vendor) {
        if (vendor == null) {
            throw new IllegalArgumentException("Vendor must not be null");
        }

        switch (vendor) {
            case PLANTUML:
                return new PlantUMLDiagramGenerator();
            case MERMAID:
                return new MermaidDiagramGenerator();
            default:
                throw new IllegalArgumentException("Unknown UML diagram vendor: " + vendor);
        }
    }
}
