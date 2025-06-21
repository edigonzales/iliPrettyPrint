package ch.so.agi.umleditor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.ehi.interlis.associations.AssociationDef;
import ch.ehi.interlis.associations.RoleDef;
import ch.ehi.interlis.attributes.AttributeDef;
import ch.ehi.interlis.attributes.DomainAttribute;
import ch.ehi.interlis.constraints.ConstraintDef;
import ch.ehi.interlis.domainsandconstants.DomainDef;
import ch.ehi.interlis.domainsandconstants.Type;
import ch.ehi.interlis.domainsandconstants.UnknownType;
import ch.ehi.interlis.domainsandconstants.basetypes.BooleanType;
import ch.ehi.interlis.domainsandconstants.basetypes.DateType;
import ch.ehi.interlis.domainsandconstants.basetypes.EnumElement;
import ch.ehi.interlis.domainsandconstants.basetypes.Enumeration;
import ch.ehi.interlis.domainsandconstants.basetypes.NumericType;
import ch.ehi.interlis.domainsandconstants.basetypes.StructAttrType;
import ch.ehi.interlis.domainsandconstants.basetypes.Text;
import ch.ehi.interlis.domainsandconstants.basetypes.TextKind;
import ch.ehi.interlis.domainsandconstants.linetypes.LineFormTypeDef;
import ch.ehi.interlis.domainsandconstants.linetypes.LineType;
import ch.ehi.interlis.functions.FunctionDef;
import ch.ehi.interlis.graphicdescriptions.GraphicParameterDef;
import ch.ehi.interlis.metaobjects.MetaDataUseDef;
import ch.ehi.interlis.metaobjects.ParameterDef;
import ch.ehi.interlis.modeltopicclass.AbstractClassDef;
import ch.ehi.interlis.modeltopicclass.ClassDef;
import ch.ehi.interlis.modeltopicclass.ClassDefKind;
import ch.ehi.interlis.modeltopicclass.ClassExtends;
import ch.ehi.interlis.modeltopicclass.INTERLIS2Def;
import ch.ehi.interlis.modeltopicclass.IliImport;
import ch.ehi.interlis.modeltopicclass.ModelDef;
import ch.ehi.interlis.modeltopicclass.TopicDef;
import ch.ehi.interlis.tools.AbstractClassDefUtility;
import ch.ehi.interlis.units.UnitDef;
import ch.ehi.interlis.views.ViewDef;
import ch.ehi.uml1_4.foundation.core.Constraint;
import ch.ehi.uml1_4.foundation.core.ModelElement;
import ch.ehi.uml1_4.foundation.core.Namespace;
import ch.ehi.uml1_4.foundation.datatypes.Multiplicity;
import ch.ehi.uml1_4.foundation.datatypes.MultiplicityRange;
import ch.ehi.uml1_4.implementation.AbstractModelElement;
import ch.ehi.uml1_4.implementation.UmlMultiplicityRange;
import ch.ehi.uml1_4.modelmanagement.Model;
import ch.interlis.ili2c.generator.nls.ElementType;
import ch.ehi.basics.settings.Settings;

public class MermaidDiagramGenerator implements DiagramGenerator {

    private PrintWriter writer;
    private Map<String, String> classNameMap = new HashMap<>(); // wird glaub momentan nicht benötigt
    private Set<String> processedAssociations = new HashSet<>(); // Welche Assoziation wurde bereits prozessiert?
    private List<String> inheritanceList = new ArrayList<>(); // Mermaid-String für Vererbung
    private List<String> associationList = new ArrayList<>(); // Mermaid-String für Assoziationen
    private List<String> dependencyList = new ArrayList<>();
    private List<String> internalModels = new ArrayList<>(); // Liste mit Namen der nicht-internen Modelle. Damit man bei der Vererbung weiss, ob die Klasse intern oder extern ist. 
    private Map<String, String> externalClasses = new HashMap<>(); // Externe Klassen werden bei der Vererbung gesammelt und nachträglich exportiert.
    private boolean closeDomain = false; // Zur Sicherheit, falls noch nicht alles unterstützt ist.
    private String language;
    private Settings settings;
    private boolean showAttributes = false;
    private boolean showAttributeTypes = false;
    private boolean showCardinalitiesOfAttributes = false;
    private boolean showCardinalities = true;
    private boolean showTitle = true;

    // TODO
    // - ...
    
    // Konfigmöglichkeiten
    // - qualifiedNames (wegen Strukturen etc.)
    
    /**
     * Exports a model to a PlantUML file
     * 
     * @param model Source Data
     * @param plantUmlFile path of the destination file
     * @throws Exception Exception
     */
    public Path export(Model model, Path outputDir, Settings settings) throws Exception {
        this.settings = settings;
        showAttributes = Boolean.valueOf(settings.getValue(PlantUMLDiagramGenerator.SHOW_ATTRIBUTES));
        showAttributeTypes = Boolean.valueOf(settings.getValue(PlantUMLDiagramGenerator.SHOW_ATTRIBUTE_TYPES));
        showCardinalitiesOfAttributes = Boolean.valueOf(settings.getValue(PlantUMLDiagramGenerator.SHOW_CARDINALITIES_OF_ATTRIBUTES));
        showCardinalities = Boolean.valueOf(settings.getValue(PlantUMLDiagramGenerator.SHOW_CARDINALITIES));
        showTitle = Boolean.valueOf(settings.getValue(PlantUMLDiagramGenerator.SHOW_TITLE));
        
        try {
            File mermaidFile = outputDir.resolve(model.getDefLangName() + ".mmd").toFile();
            
            writer = new PrintWriter(new FileWriter(mermaidFile));
            
            // Start Mermaid diagram
            writer.println("---");
            if (showTitle) writer.println("title: " + model.getDefLangName());
            writer.println("  config:");
            writer.println("    class:");
            writer.println("      hideEmptyMembersBox: true");
            writer.println("---");                
            writer.println("classDiagram");
            
            // Process model elements
            Iterator modelI = model.iteratorOwnedElement();
            while (modelI.hasNext()) {
                Object obj = modelI.next();
                if (obj instanceof INTERLIS2Def) {
                    processModelElement((INTERLIS2Def) obj);
                } else {
                    // ch.ehi.uml1_4.implementation.UmlPackage
                    Iterator i = ch.ehi.interlis.tools.ModelElementUtility.getChildElements((Namespace) obj, null)
                            .iterator();
                    while (i.hasNext()) {
                        Object objnew = i.next();
                        if (objnew instanceof INTERLIS2Def) {
                            processModelElement((INTERLIS2Def) objnew);
                        }
                    }
                }
            }

            for (var entry : externalClasses.entrySet()) {
                String oid = entry.getKey();
                String elementName = entry.getValue();
                writer.println("    class c" + oid + "[\""+ elementName +"\"]:::aclass {");
                writer.println("      <<External>>");
                writer.println("    }");
            }
            
            for (String association : associationList) {
                writer.println(association);
            }

            for (String inheritance : inheritanceList) {
                writer.println(inheritance);
            }

            // Scheint glaub nicht für Namespaces zu funktionieren in Mermaid.
//            for (String dependency : dependencyList) {
//                writer.println(dependency);
//            }
            
            // Funktioniert nicht, falls embedded in HTML.
//            writer.println("classDef aclass fill:#EEEEEE,stroke:#999999,stroke-width:2px,color:#000;");
//            writer.println("classDef aenumeration fill:#EEEEEE,stroke:#999999,stroke-width:2px,color:#000;");
//            writer.println("classDef astructure fill:#EEEEEE,stroke:#999999,stroke-width:2px,color:#000,stroke-dasharray: 6 6;");
            
            // End Mermaid diagram
            writer.close();
            return mermaidFile.toPath();
        } catch (IOException e) {
            throw e;
        }
    }
    
    private void processModelElement(INTERLIS2Def obj) {
        // Entspricht more or less den importieren Modellen oder die mit "<>"?
        if (!ModelElementUtility.isInternal(obj)) {
            String baselanguage = findBaseLanguage(obj);
            language = baselanguage;
            Set<String> languages = findLanguages(obj);

            // Process all child elements
            Set<ModelElement> childElements = ch.ehi.interlis.tools.ModelElementUtility.getChildElements((Namespace) obj, null);
            Iterator<ModelElement> i = childElements.iterator();
            
            while (i.hasNext()) {
                ModelElement modelElement = i.next();
                //System.out.println("modelElement: " + modelElement.getName());
                internalModels.add(modelElement.getDefLangName());
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
     * Processes a model element and generates Mermaid representation
     */
    private void visitModelElement(ModelElement modelDef, String scopedNamePrefix, String baselanguage, Set languages) {
        String elementType = getElementType(modelDef);
        String elementName = modelDef.getName() != null ? modelDef.getName().getValue(baselanguage) : "Unnamed";
        
        // Calculate full scoped name
        String fullScopedName = getScopedName(scopedNamePrefix, modelDef, baselanguage);
        //System.out.println("fullScopedName: " + fullScopedName);
        
//        // Store class name for later relationship building
//        if (elementType != null && (elementType.equals(ElementType.CLASS) || 
//                                     elementType.equals(ElementType.STRUCTURE))) {
//            classNameMap.put(fullScopedName, elementName);
//        }
        
        //System.out.println("elementType: " + elementType);

        // Process according to element type
        if (elementType != null) {
            switch (elementType) {
//                case ElementType.MODEL:
//                    writer.println("namespace " + elementName + " {");
//                    break;
                    
                case ElementType.TOPIC:
                    writer.println("  namespace " + elementName + " {");
                    break;
                    
                case ElementType.STRUCTURE:
                case ElementType.CLASS:                                  
                    String type = elementType.equals(ElementType.STRUCTURE) ? "astructure" : "aclass";
                    ClassDef classDef = (ClassDef) modelDef;
                    String oid = classDef.getOid();
                    writer.println("    class c" + oid + "[\""+ elementName +"\"]:::"+type+" {");
                    if (classDef.isAbstract()) {
                        writer.println("      <<abstract>>");
                    }
                    if (elementType.equals(ElementType.STRUCTURE)) {
                        writer.println("     <<Structure>>");
                    }

                    // Handle inheritance
                    handleInheritance(classDef, baselanguage);

                    break;
                       
                case ElementType.ASSOCIATION:
                    // Associations are handled separately when processing roles
                    break;
                    
                case ElementType.DOMAIN:                    
                    DomainDef domainDef = (DomainDef) modelDef;
                    //System.out.println("domainDef: " + domainDef.getType());
                    if (domainDef.getType() instanceof Enumeration) {
                        writer.println("    class e" + domainDef.getOid() + "[\""+ elementName +"\"]:::aenumeration {");
                        writer.println("      <<Enumeration>>");
                        closeDomain = true;
                    } else if (domainDef.getType() instanceof LineType) {
                        writer.println("    class l" + domainDef.getOid() + "[\""+ elementName +"\"]:::alinetype {");
                        writer.println("      <<LineType>>");
                        closeDomain = true;
                    } else if (domainDef.getType() instanceof DateType) {
                        writer.println("    class b" + domainDef.getOid() + "[\""+ elementName +"\"]:::abasetype {");
                        writer.println("      <<DateType>>");
                        closeDomain = true;                        
                    } else if (domainDef.getType() instanceof Text) {
                        writer.println("    class b" + domainDef.getOid() + "[\""+ elementName +"\"]:::abasetype {");
                        writer.println("      <<Text>>");
                        closeDomain = true;                        
                    }
                    break;
                case ElementType.FUNCTION: 
                    FunctionDef functionDef = (FunctionDef) modelDef;
                    writer.println("    class f" + functionDef.getOid() + "[\""+ elementName +"\"]:::afunctiontype {");
                    writer.println("      <<Function>>");

                    break;
                default:
                    // Other element types not directly represented in class diagram
                    break;
            }
        }
        
        // Process children
        if (modelDef instanceof AbstractClassDef) {
            // Process attributes
            Iterator attributeIt = AbstractClassDefUtility.getIliAttributes((AbstractClassDef) modelDef).iterator();
            while (attributeIt.hasNext()) {
                Object object = attributeIt.next();
                if (object instanceof AttributeDef) {
//                    System.out.println("\n");
//                    System.out.println("---------------------------------------------------------------");
                    
                    AttributeDef attrDef = (AttributeDef) object;
                    String attrName = attrDef.getName().getValue(baselanguage);
                    String attrType = getAttributeType(attrDef);
                    
//                    System.out.println(attrName + " " + attrType);
                           
                    String multiplicityString = "";
                    Multiplicity m = attrDef.getMultiplicity();
                    MultiplicityRange mr = null;
                    if (m != null) {
                        mr = (MultiplicityRange) m.iteratorRange().next();
                        long lower = mr.getLower();
                        long upper = mr.getUpper();
                        if (lower == upper) {
                            multiplicityString = "[" + lower + "]";                               
                        } else {
                            multiplicityString = "[" + lower + ".." + (upper == Long.MAX_VALUE ? "*" : upper) + "]";                               
                        }
                    }

                    if (showAttributes) {
                        writer.println("      " + attrName + " " + (showCardinalitiesOfAttributes ? multiplicityString : "") + (showAttributeTypes ?  " : " + attrType : ""));
                    }
                    
                    // Process enum attributes if needed
                    if (attrDef.containsAttrType()) {
                        DomainAttribute domainAttr = (DomainAttribute) attrDef.getAttrType();
                        if (domainAttr.containsDirect() && domainAttr.getDirect() instanceof Enumeration) {
                            Enumeration enumeration = (Enumeration) domainAttr.getDirect();
                            // If needed, process enum values
                        }
                    }
                }
            }
            
            // Process parameters if class definition
            if (modelDef instanceof ClassDef) {
                ClassDef classDef = (ClassDef) modelDef;
                Iterator paramIt = classDef.iteratorParameterDef();
                while (paramIt.hasNext()) {
                    ParameterDef param = (ParameterDef) paramIt.next();
                    // Handle parameters if needed
                }
            }
            
            // Process associations
            if (modelDef instanceof AssociationDef) {
                AssociationDef assocDef = (AssociationDef) modelDef;
                
                // Skip if already processed
                if (processedAssociations.contains(fullScopedName)) {
                    return;
                }
                processedAssociations.add(fullScopedName);
                
                // Process roles and create relationship
                processAssociation(assocDef, baselanguage);
            }
            
        } else if (modelDef instanceof Namespace) {
            Namespace topicDef = (Namespace)modelDef;
            
            Iterator dependsi = topicDef.iteratorClientDependency();
            while (dependsi.hasNext()) {
                Object obj = dependsi.next();
                if (obj instanceof ch.ehi.interlis.modeltopicclass.TopicDepends) {
                    ch.ehi.interlis.modeltopicclass.TopicDepends depends = (ch.ehi.interlis.modeltopicclass.TopicDepends)obj;
                    Iterator supplieri = depends.iteratorSupplier();
                    if (supplieri.hasNext()) {
                        TopicDef supplier = (TopicDef)supplieri.next();
                        String dependency = topicDef.getDefLangName() + "..>" + supplier.getDefLangName();
                        dependencyList.add(dependency);
                    }
                }
            }
            
            // Process children elements
            Iterator childIt = ch.ehi.interlis.tools.ModelElementUtility.getChildElements((Namespace) modelDef, null)
                    .iterator();

            while (childIt.hasNext()) {
                ModelElement childElement = (ModelElement) childIt.next();
                visitModelElement(childElement, fullScopedName, baselanguage, languages);
            }
            
        }
        
        // Close any opened blocks
        if (elementType != null) {
            switch (elementType) {
//                case ElementType.MODEL:
//                    writer.println("}");
//                    writer.println();
//                    break;
                case ElementType.FUNCTION:
                    writer.println("  }");
                    writer.println();
                    break;
                case ElementType.DOMAIN:
                    if (closeDomain) {
                        writer.println("    }");
                        writer.println();                        
                    }
                    closeDomain = false;
                    break;
                case ElementType.TOPIC:
                    writer.println("  }");
                    writer.println();
                    break;
                case ElementType.CLASS:
                case ElementType.STRUCTURE:
                    writer.println("    }");
                    writer.println();
                    break;
            }
        }
    }
    
    private void visitType(AbstractModelElement owner, Type def) {
        
    }
    
    /**
     * Handles inheritance relationships for a class
     */
    private void handleInheritance(ClassDef classDef, String baselanguage) {
        if (!classDef.iteratorGeneralization().hasNext()) {
            return;
        }
        
        String childClassOid = classDef.getOid();
        String parentClassOid = "";
        
        Iterator geni = classDef.iteratorGeneralization();
        while (geni.hasNext()) {
            ClassExtends classExtends = (ClassExtends) geni.next();
            ClassDef parentDef = (ClassDef)classExtends.getParent();

            String parentModelName = ch.ehi.interlis.tools.ModelElementUtility.getModel(parentDef).getDefLangName();
            if (!internalModels.contains(parentModelName)) {
//                System.out.println("********* externes Modell gefunden: " + parentModelName );
                externalClasses.put(parentDef.getOid(), parentDef.getDefLangName());
            }
//            System.out.println("getModel: " + ch.ehi.interlis.tools.ModelElementUtility.getModel(parentDef).getDefLangName());
//            System.out.println("((ClassDef)classExtends.getParent()): " + ((ClassDef)classExtends.getParent()).getName().getValue());
//            System.out.println("((ClassDef)classExtends.getParent()): " + ((ClassDef)classExtends.getParent()).getOid());
            parentClassOid = ((ClassDef)classExtends.getParent()).getOid();
        }
                        
        inheritanceList.add("c" + parentClassOid + " <|-- c" + childClassOid);
    }
    
    private void processAssociation(AssociationDef assocDef, String baselanguage) {
        // Process roles in the association
        String assocName = assocDef.getName() != null ? assocDef.getName().getValue(baselanguage) : "";
        
//        System.out.println("assocName: " + assocName);
        
        StringBuilder relationBuilder = new StringBuilder();
        String leftClass = null;
        String rightClass = null;
        String leftCard = null;
        String rightCard = null;
        String leftRole = null;
        String rightRole = null;
        
        Iterator roleIt = assocDef.iteratorConnection();
        int roleCount = 0;
        
        while (roleIt.hasNext() && roleCount < 2) {
            RoleDef roleDef = (RoleDef) roleIt.next();
            
            String roleName = roleDef.getName() != null ? roleDef.getName().getValue(baselanguage) : "";
//            String className = roleDef.getParticipant() != null ? 
//                               roleDef.getParticipant().getName().getValue(baselanguage) : "UnknownClass";
            
            String className = ((ClassDef)roleDef.getParticipant()).getOid();
            
            // Get cardinality
            String cardinality = "";
            if (showCardinalities && roleDef.getMultiplicity() != null) {                
                Iterator rangeIt = roleDef.getMultiplicity().iteratorRange();
                if (rangeIt.hasNext()) {
                    UmlMultiplicityRange range = (UmlMultiplicityRange) rangeIt.next();
                    if (range.getLower() == range.getUpper()) {
                        cardinality = Long.toString(range.getLower());
                    } else {
                        cardinality = range.getLower() + ".." + (range.getUpper() == Long.MAX_VALUE ? "*" : range.getUpper());   
                    }                
                }                
            }
            
            if (roleCount == 0) {
                leftClass = className;
                leftCard = cardinality;
                leftRole = roleName;
            } else {
                rightClass = className;
                rightCard = cardinality;
                rightRole = roleName;
            }
            
            roleCount++;
        }
        
        // Create PlantUML relationship notation if we have two roles
        if (leftClass != null && rightClass != null) {
            relationBuilder.append("c").append(leftClass).append("");
            
            if (leftCard != null && !leftCard.isEmpty()) {
                relationBuilder.append(" \"").append(leftCard).append("\" ");
            } else {
                relationBuilder.append(" ");
            }
            
            // Basic association
            relationBuilder.append("--");
            
            if (rightCard != null && !rightCard.isEmpty()) {
                relationBuilder.append(" \"").append(rightCard).append("\" ");
            } else {
                relationBuilder.append(" ");
            }
            
            relationBuilder.append("c").append(rightClass).append("");
            
            // Add roles and association name if present
//            if ((leftRole != null && !leftRole.isEmpty()) || (rightRole != null && !rightRole.isEmpty())) {
//                relationBuilder.append(" : ");
//                
//                if (leftRole != null && !leftRole.isEmpty()) {
//                    relationBuilder.append(leftRole).append(" ");
//                }
//                
//                if (assocName != null && !assocName.isEmpty()) {
//                    relationBuilder.append("(").append(assocName).append(") ");
//                }
//                
//                if (rightRole != null && !rightRole.isEmpty()) {
//                    relationBuilder.append(rightRole);
//                }
//            }
            
            associationList.add(relationBuilder.toString());
        }
    }

    private String getAttributeType(AttributeDef attrDef) {                
        if (attrDef.containsAttrType()) {
            DomainAttribute attr = (DomainAttribute) attrDef.getAttrType(); 
//            System.out.println("DomainAttribute attr: " + attr);
            if (attr.containsDomainDef()) {
                // e.g. enums                
                return attr.getDomainDef().getDefLangName();
            } else if (attr.containsDirect()) {
//                System.out.println("attr.containsDirect()");
                Type type = attr.getDirect();
//                System.out.println("attr.getDirect(): " + type.getClass());
                if (type instanceof StructAttrType) {
                    StructAttrType structAttrType = (StructAttrType) (type);
                    return structAttrType.getParticipant().getDefLangName();
                    //return classRef(attrDef, structAttrType.getParticipant());
                } else if (type instanceof BooleanType)  {
                    return "BOOLEAN";
                } else if (type instanceof Text) {
                    Text text = (Text)type;
                    switch (text.getKind()) {
                    case TextKind.UNDEFINED:
                        String typeTag = "TEXT";
                        if (text.isMultiline()) {
                            typeTag = "MTEXT";
                        }
                        return typeTag;
                    case TextKind.MAXLEN:
                        typeTag = "TEXT";
                        if (text.isMultiline()) {
                            typeTag = "MTEXT";
                        }
                        return (typeTag + "*" + Long.toString(text.getMaxLength()));
                    case TextKind.NAME:
                        return ("NAME");
                    case TextKind.URI:
                        return ("URI");
                    }
                } else if (type instanceof NumericType) {
                    NumericType numeric = (NumericType)type;
                    if (numeric.isRangeDefined()) {
                        return numeric.getMinDec().toString() + ".." + numeric.getMaxDec().toString();
                    } else {
                        return "NUMERIC";
                    }
                    
                } else if (type instanceof UnknownType) {
                    UnknownType unknownType = (UnknownType)type;
                    return unknownType.getSyntax().getValue();
                }

                // Return a string representation of the type
                return type.getClass().getSimpleName().replace("Def", "");
            }
        }
        return "Object";
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

    private String classRef(ModelElement source, AbstractClassDef ref) {
        if ("ANYCLASS".equals(ref.getDefLangName())) {
            return "ANYCLASS";
        }
        if ("ANYSTRUCTURE".equals(ref.getDefLangName())) {
            return "ANYSTRUCTURE";
        }
        return modelElementRef(source, ref, null);
    }
    
    private String modelElementRef(ModelElement source, ModelElement ref, String language) {
        if (language == null) {
            language = this.language;
        }
        ModelDef modelOfSource = null;
        ModelDef modelOfRef = null;
        modelOfSource = ch.ehi.interlis.tools.ModelElementUtility.getModel(source);
        modelOfRef = ch.ehi.interlis.tools.ModelElementUtility.getModel(ref);
        if (!modelOfRef.equals(modelOfSource)) {
            final IliImport iliImp = modelOfSource.getImport(modelOfRef);
            if (iliImp != null) {
                language = iliImp.getSupplierLanguage(language);
            }
        }
        return ch.ehi.interlis.tools.ModelElementUtility.getIliQualifiedName(source, ref, language);
    }
    
    private static String getFileNameWithoutExtension(String filePath) {
        String fileName = Paths.get(filePath).getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }    
}