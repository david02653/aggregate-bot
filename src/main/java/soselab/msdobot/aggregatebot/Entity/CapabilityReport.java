package soselab.msdobot.aggregatebot.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CapabilityReport {

    public String capability;
    public String service;
    // context - propertyName[]
    public HashMap<String, ArrayList<String>> contextProperty;

    public CapabilityReport(){
        contextProperty = new HashMap<>();
    }
    public CapabilityReport(String capability, String service){
        this.capability = capability;
        this.service = service;
        contextProperty = new HashMap<>();
    }

    public void addProperty(String contextName, String property){
        ArrayList<String> temp;
        if(contextProperty.containsKey(contextName)){
            temp = contextProperty.get(contextName);
        }else{
            temp = new ArrayList<>();
        }
        temp.add(property);
        this.contextProperty.put(contextName, temp);
    }

    public void mergeProperty(HashMap<String, ArrayList<String>> properties){
        for(Map.Entry<String, ArrayList<String>> entry: properties.entrySet()){
            for(String propertyName: entry.getValue()){
                addProperty(entry.getKey(), propertyName);
            }
        }
    }

}
