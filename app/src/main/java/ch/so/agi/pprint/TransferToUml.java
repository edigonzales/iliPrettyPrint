package ch.so.agi.pprint;

import java.nio.file.Path;

import ch.ehi.basics.settings.Settings;
import ch.ehi.uml1_4.modelmanagement.Model;

public interface TransferToUml {
    public void export(Model model, Path plantUmlFile, Settings settings) throws Exception;

    public static final String SHOW_ATTRIBUTES = "ch.so.agi.interlis.uml.showAttributes";
    
    public static final String SHOW_ATTRIBUTE_TYPES = "ch.so.agi.interlis.uml.showAttributeTypes";
    
    public static final String SHOW_CARDINALITIES_OF_ATTRIBUTES = "ch.so.agi.interlis.uml.showCardinalitiesOfAttributes";
    
    public static final String SHOW_CARDINALITIES = "ch.so.agi.interlis.uml.showCardinalities";
}
