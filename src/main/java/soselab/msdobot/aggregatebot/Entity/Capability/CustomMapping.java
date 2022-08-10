package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

/**
 * declare specific input data format
 */
public class CustomMapping {
    public String mappingName; // what is this mapping's name
    public String description; // what is this mapping
    public String schema; // mapping schema, should be a json string
//    public ArrayList<Concept> usedConcept;

//    public CustomMapping(){
//        this.usedConcept = new ArrayList<>();
//    }

    @Override
    public String toString(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
