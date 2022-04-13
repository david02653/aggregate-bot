package soselab.msdobot.aggregatebot.Entity.Vocabulary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import soselab.msdobot.aggregatebot.Exception.IllegalConceptException;

import java.util.ArrayList;
import java.util.HashMap;

public class Vocabulary {
    //    public ArrayList<String> general;
//    public ArrayList<String> output;
    public ArrayList<Concept> ConceptList;
    public ArrayList<Context> ContextList;
    //    public ArrayList<CustomMapping> customMappingList;
//    public HashMap<String, CustomMapping> customMappingHashMap;
    public HashMap<String, ArrayList<String>> conceptMap;
    public HashMap<String, ArrayList<String>> contextMap;

    public Vocabulary(){
    }

//    /**
//     * retrieve specific concept
//     * @param conceptName target concept name
//     * @return target concept
//     * @throws IllegalConceptException if no matched concept found
//     */
//    public Concept getConcept(String conceptName) throws IllegalConceptException {
//        for(Concept concept: ConceptList){
//            if(concept.conceptName.equals(conceptName))
//                return concept;
//        }
//        throw new IllegalConceptException("no available concept found");
//    }

    /**
     * check if specific concept-property set exist
     * @param concept target concept
     * @param property target property
     * @return true if existed, otherwise false
     */
    public boolean isIllegalConceptProperty(String concept, String property){
        ArrayList<String> conceptProperties;
        if(conceptMap.containsKey(concept)){
            conceptProperties = conceptMap.get(concept);
            if(!conceptProperties.contains(property)) {
                System.out.println("[WARNING] property '" + property + "' does not exist in concept '" + concept + "'.");
                return true;
            }else
                return false;
        }else {
            System.out.println("[WARNING] concept '" + concept + "' does not exist.");
            return true;
        }
    }

    /**
     * check if given context is available<br>note that context 'general' is treated as default
     * @param context given context
     * @return true if available, otherwise false
     */
    public boolean isAvailableContext(String context){
        if(context.equals("general")) return true;
        if(!contextMap.containsKey(context)){
            System.out.println("[WARNING] context '" + context + "' does not exist.");
            return false;
        }
        return true;
    }

    /**
     * create hashmap of concept properties
     */
    public void createConceptHashMap(){
        HashMap<String, ArrayList<String>> mapping = new HashMap<>();
        for(Concept concept: ConceptList){
            mapping.put(concept.conceptName, concept.properties);
        }
        this.conceptMap = mapping;
    }

    /**
     * create hashmap of context properties
     */
    public void createContextHashMap(){
        HashMap<String, ArrayList<String>> mapping = new HashMap<>();
        for(Context context: ContextList){
            mapping.put(context.contextName, context.properties);
        }
        this.contextMap = mapping;
    }

    public ArrayList<String> getOutputConcept(){
        return conceptMap.get("Output");
    }
//    /**
//     * create hashmap of custom mapping list
//     */
//    public void createCustomMappingHashMap(){
//        HashMap<String, CustomMapping> mapping = new HashMap<>();
//        for(CustomMapping map: customMappingList)
//            mapping.put(map.mappingName, map);
//        this.customMappingHashMap = mapping;
//    }

//    public HashMap<String, CustomMapping> getCustomMappingHashMap(){
//        return this.customMappingHashMap;
//    }

    @Override
    public String toString(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
