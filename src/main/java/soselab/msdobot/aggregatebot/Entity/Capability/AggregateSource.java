package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.Gson;

public class AggregateSource {

    public String context;     // in purpose to access different context domain config
    public String from;        // config source property
    public String useAs;          // what name should be used for this data
    public boolean isAggregationData;  // if this data is an aggregation result
    public String aggregationLevel; // what level should this aggregation access
    public AggregateDataMaterial aggregateDataMaterial;  // what kinds of data should be used as materials to retrieve this aggregation result

    public AggregateSource(){
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
