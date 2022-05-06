package soselab.msdobot.aggregatebot.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jakewharton.fliptables.FlipTable;
import soselab.msdobot.aggregatebot.Entity.Service.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RenderingService implements RenderingTemplate {

    HashMap<String, String> aggregateResult;
    ArrayList<Service> serviceList;

    public RenderingService(ArrayList<Service> serviceList, JsonArray aggregateResult){
        this.aggregateResult = new HashMap<>();
        this.serviceList = serviceList;
        parseAggregateReport(aggregateResult);
    }

    @Override
    public void parseAggregateReport(JsonArray aggregateReport) {
        JsonArray keyArray = aggregateReport.get(AGGREGATE_RESULT_KEY).getAsJsonArray();
        JsonArray valueArray = aggregateReport.get(AGGREGATE_RESULT_VALUE).getAsJsonArray();
        int count = 0;
        for(JsonElement key: keyArray){
            aggregateResult.put(key.getAsString(), valueArray.get(count).getAsString());
            count++;
        }
    }

    /**
     * parse aggregate report to simple ascii art table
     * @return simple ascii art table
     */
    public String parseToSimpleAsciiArtTable(){
//        ArrayList<String> header = new ArrayList<>();
//        ArrayList<ArrayList<String>> body;
        String[] header = {"key", "value"};
        String[][] body = new String[aggregateResult.size()][2];
        int count = 0;
        for(Map.Entry<String, String> data: aggregateResult.entrySet()){
            String key = data.getKey();
            String value = data.getValue();
            body[count][0] = key;
            if(value.length() > 150)
                body[count][1] = value.substring(0, 100);
            else
                body[count][1] = value;
            count++;
        }
        String resultTable = FlipTable.of(header, body);
        System.out.println(resultTable);
        return resultTable;
    }
}
