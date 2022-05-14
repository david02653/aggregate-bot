package soselab.msdobot.aggregatebot.Service;

import com.google.gson.JsonArray;
import soselab.msdobot.aggregatebot.Entity.DiscordMessageTemplate;

import java.util.HashMap;

public interface RenderingTemplate {
    int AGGREGATE_RESULT_KEY = 0;
    int AGGREGATE_RESULT_VALUE = 1;
    void parseAggregateReport(JsonArray aggregateReport);
    DiscordMessageTemplate defaultRendering(HashMap<String, String> aggregateData, HashMap<String, HashMap<String, String>> specificAggregateData, HashMap<String, HashMap<String, String>> properties);
}
