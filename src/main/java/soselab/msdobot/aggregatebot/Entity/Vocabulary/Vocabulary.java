package soselab.msdobot.aggregatebot.Entity.Vocabulary;

import com.google.gson.Gson;
import soselab.msdobot.aggregatebot.Exception.IllegalConceptException;

import java.util.ArrayList;
import java.util.HashMap;

public class Vocabulary {
    public ArrayList<String> general;
    public ArrayList<String> output;
    public ArrayList<Concept> conceptList;
    public ArrayList<CustomMapping> customMappingList;
    public HashMap<String, CustomMapping> customMappingHashMap;

    public Vocabulary(){
    }

    /**
     * retrieve specific concept
     * @param conceptName target concept name
     * @return target concept
     * @throws IllegalConceptException if no matched concept found
     */
    public Concept getConcept(String conceptName) throws IllegalConceptException {
        for(Concept concept: conceptList){
            if(concept.conceptName.equals(conceptName))
                return concept;
        }
        throw new IllegalConceptException("no available concept found");
    }

    /**
     * create hashmap of custom mapping list
     */
    public void createCustomMappingHashMap(){
        HashMap<String, CustomMapping> mapping = new HashMap<>();
        for(CustomMapping map: customMappingList)
            mapping.put(map.mappingName, map);
        this.customMappingHashMap = mapping;
    }

    public HashMap<String, CustomMapping> getCustomMappingHashMap(){
        return this.customMappingHashMap;
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
