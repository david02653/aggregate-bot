package soselab.msdobot.aggregatebot.Entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import soselab.msdobot.aggregatebot.Service.RenderingService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * execute result of capability, include information about missing context properties
 */
public class CapabilityReport {

    public String capability;
    public String service;
    // error property map
    // context - propertyName[]
    public HashMap<String, HashSet<String>> missingContextProperty;
    // execution result
    public HashMap<String, String> resultMap;

    public CapabilityReport(){
        missingContextProperty = new HashMap<>();
        resultMap = new HashMap<>();
    }
    public CapabilityReport(String capability, String service){
        this.capability = capability;
        this.service = service;
        missingContextProperty = new HashMap<>();
        resultMap = new HashMap<>();
    }

    public boolean hasError(){
        return this.missingContextProperty.size() > 0;
    }

    public void addProperty(String contextName, String property){
        HashSet<String> temp;
        if(missingContextProperty.containsKey(contextName)){
            temp = missingContextProperty.get(contextName);
        }else{
            temp = new HashSet<>();
        }
        temp.add(property);
        this.missingContextProperty.put(contextName, temp);
    }

    public void mergeProperty(HashMap<String, HashSet<String>> properties){
        for(Map.Entry<String, HashSet<String>> entry: properties.entrySet()){
            for(String propertyName: entry.getValue()){
                addProperty(entry.getKey(), propertyName);
            }
        }
    }

    public void addExecuteResult(String resultName, String result){
        this.resultMap.put(resultName, result);
    }

    /**
     * parse aggregate report in to capability result
     * @param aggregateReport
     */
    public void addResultFromAggregateReport(JsonArray aggregateReport){
        JsonArray keyArray = aggregateReport.get(RenderingService.AGGREGATE_RESULT_KEY).getAsJsonArray();
        JsonArray valueArray = aggregateReport.get(RenderingService.AGGREGATE_RESULT_VALUE).getAsJsonArray();
        int count = 0;
        for(JsonElement key: keyArray){
            resultMap.put(key.getAsString(), valueArray.get(count).getAsString());
            count++;
        }
    }

    public void setMissingContextProperty(HashMap<String, HashSet<String>> missingContextProperty) {
        this.missingContextProperty = missingContextProperty;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }
}
