package ch.so.agi.pprint;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ch.ehi.interlis.associations.AssociationDef;
import ch.ehi.interlis.associations.RoleDef;
import ch.ehi.interlis.attributes.AttributeDef;
import ch.ehi.interlis.attributes.DomainAttribute;
import ch.ehi.interlis.constraints.ConstraintDef;
import ch.ehi.interlis.domainsandconstants.DomainDef;
import ch.ehi.interlis.domainsandconstants.Type;
import ch.ehi.interlis.domainsandconstants.basetypes.EnumElement;
import ch.ehi.interlis.domainsandconstants.basetypes.Enumeration;
import ch.ehi.interlis.domainsandconstants.linetypes.LineFormTypeDef;
import ch.ehi.interlis.functions.FunctionDef;
import ch.ehi.interlis.graphicdescriptions.GraphicParameterDef;
import ch.ehi.interlis.metaobjects.MetaDataUseDef;
import ch.ehi.interlis.metaobjects.ParameterDef;
import ch.ehi.interlis.modeltopicclass.AbstractClassDef;
import ch.ehi.interlis.modeltopicclass.ClassDef;
import ch.ehi.interlis.modeltopicclass.ClassDefKind;
import ch.ehi.interlis.modeltopicclass.INTERLIS2Def;
import ch.ehi.interlis.modeltopicclass.ModelDef;
import ch.ehi.interlis.modeltopicclass.TopicDef;
import ch.ehi.interlis.tools.AbstractClassDefUtility;
import ch.ehi.interlis.units.UnitDef;
import ch.ehi.interlis.views.ViewDef;
import ch.ehi.uml1_4.foundation.core.Constraint;
import ch.ehi.uml1_4.foundation.core.ModelElement;
import ch.ehi.uml1_4.foundation.core.Namespace;
import ch.ehi.uml1_4.foundation.extensionmechanisms.TaggedValue;
import ch.ehi.uml1_4.implementation.UmlMultiplicityRange;
import ch.ehi.uml1_4.modelmanagement.Model;
import ch.interlis.ili2c.generator.nls.ElementType;

public class TransferToPlantUml {

    private PrintWriter writer;
    private Map<String, String> classNameMap = new HashMap<>();
    private Set<String> processedAssociations = new HashSet<>();

    /**
     * Exports a model to a PlantUML file
     * 
     * @param model Source Data
     * @param plantUmlFile path of the Destination file
     * @throws Exception Exception
     */
    public void export(Model model, java.io.File plantUmlFile) throws Exception {
        try {
            writer = new PrintWriter(new FileWriter(plantUmlFile));
            
            // Start PlantUML diagram
            writer.println("@startuml");
            writer.println("' INTERLIS model diagram");
            writer.println("skinparam packageStyle rectangle");
            writer.println("skinparam classAttributeIconSize 0");
            writer.println("skinparam monochrome true");
            writer.println("skinparam shadowing false");
            writer.println();
            
            // Process model elements
            Iterator modelI = model.iteratorOwnedElement();
            while (modelI.hasNext()) {
                Object obj = modelI.next();
                if (obj instanceof INTERLIS2Def) {
                    System.out.println("Processing INTERLIS2Def: " + obj.toString());
                    processModelElement((INTERLIS2Def) obj);
                } else {
                    System.out.println("Processing namespace: " + obj.toString());
                    Iterator i = ch.ehi.interlis.tools.ModelElementUtility.getChildElements((Namespace) obj, null)
                            .iterator();
                    while (i.hasNext()) {
                        Object objnew = i.next();
                        System.out.println("Processing child: " + objnew);
                        if (objnew instanceof INTERLIS2Def) {
                            processModelElement((INTERLIS2Def) objnew);
                        }
                    }
                }
            }
            
            // End PlantUML diagram
            writer.println("@enduml");
            writer.close();
            System.out.println("PlantUML file created successfully: " + plantUmlFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing PlantUML file: " + e.getMessage());
            throw e;
        }
    }
    
    private void processModelElement(INTERLIS2Def obj) {
        if (!ModelElementUtility.isInternal(obj)) {
            String baselanguage = findBaseLanguage(obj);
            Set<String> languages = findLanguages(obj);
            
            // Process all child elements
            Iterator i = ch.ehi.interlis.tools.ModelElementUtility.getChildElements((Namespace) obj, null)
                    .iterator();
            
            while (i.hasNext()) {
                ModelElement modelElement = (ModelElement) i.next();
                System.out.println("modelElement: " + modelElement.getName());
                visitModelElement(modelElement, null, baselanguage, languages);
            }
        }
    }
    
    private String findBaseLanguage(INTERLIS2Def modelDef) {
        Set set = ch.ehi.interlis.tools.ModelElementUtility.getChildElements(modelDef, ModelDef.class);
        Iterator iterator = set.iterator();

        String baseLanguage = "";
        while (iterator.hasNext()) {
            ModelDef modelDefLangu = (ModelDef) iterator.next();
            if (modelDefLangu.getBaseLanguage() != null) {
                baseLanguage = modelDefLangu.getBaseLanguage();
                break;
            }
        }
        return baseLanguage;
    }
    
    private Set<String> findLanguages(INTERLIS2Def modelDef) {
        Set<String> languages = new HashSet<>();
        Set set = ch.ehi.interlis.tools.ModelElementUtility.getChildElements(modelDef, ModelDef.class);
        Iterator iterator = set.iterator();

        while (iterator.hasNext()) {
            ModelDef modelDefLangu = (ModelDef) iterator.next();
            if (modelDefLangu.getBaseLanguage() != null) {
                languages.add(modelDefLangu.getBaseLanguage());
            }
            languages.addAll(((ModelDef) modelDefLangu).getValidSecondLanguages());
        }
        return languages;
    }
    
    /**
     * Processes a model element and generates PlantUML representation
     */
    private void visitModelElement(ModelElement modelDef, String scopedNamePrefix, String baselanguage, Set languages) {
        String elementType = getElementType(modelDef);
        String elementName = modelDef.getName() != null ? modelDef.getName().getValue(baselanguage) : "Unnamed";
        
        // Calculate full scoped name
        String fullScopedName = getScopedName(scopedNamePrefix, modelDef, baselanguage);
        System.out.println("fullScopedName: " + fullScopedName);
        
        // Store class name for later relationship building
//        if (elementType != null && (elementType.equals(ElementType.CLASS) || 
//                                     elementType.equals(ElementType.STRUCTURE))) {
//            classNameMap.put(fullScopedName, elementName);
//        }
//        
//        // Process according to element type
//        if (elementType != null) {
//            switch (elementType) {
//                case ElementType.MODEL:
//                    writer.println("package \"" + elementName + "\" {");
//                    break;
//                    
//                case ElementType.TOPIC:
//                    writer.println("package \"" + elementName + "\" {");
//                    break;
//                    
//                case ElementType.CLASS:
//                case ElementType.STRUCTURE:
//                    String stereotype = elementType.equals(ElementType.STRUCTURE) ? "<<structure>>" : "";
//                    writer.println("class \"" + elementName + "\" " + stereotype + " {");
//                    break;
//                    
//                case ElementType.ASSOCIATION:
//                    // Associations are handled separately when processing roles
//                    break;
//                    
//                default:
//                    // Other element types not directly represented in class diagram
//                    break;
//            }
//        }
//        
//        // Process children
//        if (modelDef instanceof AbstractClassDef) {
//            // Process attributes
//            Iterator attributeIt = AbstractClassDefUtility.getIliAttributes((AbstractClassDef) modelDef).iterator();
//            while (attributeIt.hasNext()) {
//                Object object = attributeIt.next();
//                if (object instanceof AttributeDef) {
//                    AttributeDef attrDef = (AttributeDef) object;
//                    String attrName = attrDef.getName().getValue(baselanguage);
//                    String attrType = getAttributeType(attrDef);
//                    
//                    writer.println("  " + attrName + " : " + attrType);
//                    
//                    // Process enum attributes if needed
//                    if (attrDef.containsAttrType()) {
//                        DomainAttribute attrType = (DomainAttribute) attrDef.getAttrType();
//                        if (attrType.containsDirect() && attrType.getDirect() instanceof Enumeration) {
//                            Enumeration enumeration = (Enumeration) attrType.getDirect();
//                            // If needed, process enum values
//                        }
//                    }
//                }
//            }
//            
//            // Process parameters if class definition
//            if (modelDef instanceof ClassDef) {
//                ClassDef classDef = (ClassDef) modelDef;
//                Iterator paramIt = classDef.iteratorParameterDef();
//                while (paramIt.hasNext()) {
//                    ParameterDef param = (ParameterDef) paramIt.next();
//                    // Handle parameters if needed
//                }
//            }
//            
//            // Process associations
//            if (modelDef instanceof AssociationDef) {
//                AssociationDef assocDef = (AssociationDef) modelDef;
//                
//                // Skip if already processed
//                if (processedAssociations.contains(fullScopedName)) {
//                    return;
//                }
//                processedAssociations.add(fullScopedName);
//                
//                // Process roles and create relationship
//                processAssociation(assocDef, baselanguage);
//            }
//            
//        } else if (modelDef instanceof Namespace) {
//            // Process children elements
//            Iterator childIt = ch.ehi.interlis.tools.ModelElementUtility.getChildElements((Namespace) modelDef, null)
//                    .iterator();
//
//            while (childIt.hasNext()) {
//                ModelElement childElement = (ModelElement) childIt.next();
//                visitModelElement(childElement, fullScopedName, baselanguage, languages);
//            }
//            
//        }
//        
//        // Close any opened blocks
//        if (elementType != null) {
//            switch (elementType) {
//                case ElementType.MODEL:
//                case ElementType.TOPIC:
//                case ElementType.CLASS:
//                case ElementType.STRUCTURE:
//                    writer.println("}");
//                    writer.println();
//                    break;
//            }
//        }
    }

    /**
     * Concatenates the scoped name of the given Model Element
     */
    private String getScopedName(String scopedNamePrefix, ModelElement modelElement, String language) {
        if (modelElement.getName() != null) {
            if (scopedNamePrefix == null) {
                scopedNamePrefix = modelElement.getName().getValue(language);
            } else {
                scopedNamePrefix += "." + modelElement.getName().getValue(language);
            }
            return scopedNamePrefix;
        }
        return null;
    }
    
    /**
     * Returns the element type name of the given object
     */
    public String getElementType(Object obj) {
        if (obj instanceof MetaDataUseDef) {
            return ElementType.META_DATA_BASKET;
        } else if (obj instanceof ViewDef) {
            return ElementType.VIEW;
        } else if (obj instanceof UnitDef) {
            return ElementType.UNIT;
        } else if (obj instanceof FunctionDef) {
            return ElementType.FUNCTION;
        } else if (obj instanceof LineFormTypeDef) {
            return ElementType.LINE_FORM;
        } else if (obj instanceof ConstraintDef) {
            return ElementType.CONSTRAINT;
        } else if (obj instanceof ParameterDef) {
            return ElementType.PARAMETER;
        } else if (obj instanceof AttributeDef) {
            return ElementType.ATTRIBUTE;
        } else if (obj instanceof RoleDef) {
            return ElementType.ROLE;
        } else if (obj instanceof DomainDef) {
            return ElementType.DOMAIN;
        } else if (obj instanceof GraphicParameterDef) {
            return ElementType.GRAPHIC;
        } else if (obj instanceof ClassDef) {
            if(((ClassDef)obj).getKind()==ClassDefKind.STRUCTURE) {
                return ElementType.STRUCTURE;
            }
            return ElementType.CLASS;
        } else if (obj instanceof AssociationDef) {
            return ElementType.ASSOCIATION;
        } else if (obj instanceof ModelDef) {
            return ElementType.MODEL;
        } else if (obj instanceof Constraint) {
            return ElementType.CONSTRAINT;
        } else if (obj instanceof EnumElement) {
            return ElementType.ENUMERATION_ELEMENT;
        } else if (obj instanceof TopicDef) {
            return ElementType.TOPIC;
        } else {
            return null;
        }
    }

}