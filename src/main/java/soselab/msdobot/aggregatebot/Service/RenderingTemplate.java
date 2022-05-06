package soselab.msdobot.aggregatebot.Service;

import com.google.gson.JsonArray;

import java.util.HashMap;

public interface RenderingTemplate {
    int AGGREGATE_RESULT_KEY = 0;
    int AGGREGATE_RESULT_VALUE = 1;
    void parseAggregateReport(JsonArray aggregateReport);
}
