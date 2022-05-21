package soselab.msdobot.aggregatebot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import soselab.msdobot.aggregatebot.Entity.Capability.Capability;
import soselab.msdobot.aggregatebot.Entity.CapabilityReport;
import soselab.msdobot.aggregatebot.Entity.Service.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * handle available aggregate method execution
 */
public class AggregateService {

    public AggregateService(){
    }

    /**
     * default aggregate method
     * @param reports last capability execute result
     * @return aggregate report
     */
    public static JsonArray normalAggregate(ArrayList<CapabilityReport> reports){
        JsonArray keyArray = new JsonArray();
        JsonArray valueArray = new JsonArray();
        JsonArray aggregateReport = new JsonArray();
        // parse collected reports in to key-value arrays
        for(CapabilityReport report: reports){
            for(Map.Entry<String, String> result: report.resultMap.entrySet()){
                String resultName = result.getKey();
                String resultContent = result.getValue();
                valueArray.add(resultContent);
                keyArray.add(getResultPropertyName(report.service, resultName));
            }
        }
        aggregateReport.add(keyArray);
        aggregateReport.add(valueArray);
        System.out.println("[Default Aggregate] " + new Gson().toJson(aggregateReport));
        return aggregateReport;
    }

    /**
     * combine service name and property name
     * @param service service name
     * @param propertyName property name
     * @return result property name
     */
    private static String getResultPropertyName(String service, String propertyName){
        StringBuilder builder = new StringBuilder();
        if(service != null && service.length() > 0)
            builder.append(service).append(".");
        builder.append(propertyName);
        return builder.toString();
    }

}
