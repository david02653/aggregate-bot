package soselab.msdobot.aggregatebot.Entity.Vocabulary;

import com.google.gson.Gson;

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
