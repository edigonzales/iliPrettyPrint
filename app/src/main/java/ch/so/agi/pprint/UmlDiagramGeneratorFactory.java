package ch.so.agi.pprint;

public class UmlDiagramGeneratorFactory {
    public static TransferToUml getGenerator(UmlDiagramVendor vendor) {
        if (vendor == null) {
            throw new IllegalArgumentException("Vendor must not be null");
        }

        switch (vendor) {
            case MERMAID:
                return new TransferToPlantUml();
            case PLANTUML:
                return new TransferToMermaid();
            default:
                throw new IllegalArgumentException("Unknown UML diagram vendor: " + vendor);
        }
    }
}
