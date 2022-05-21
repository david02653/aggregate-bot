package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.Gson;

public class AggregateSource {

    public String context;     // in purpose to access different context domain config
    public String from;        // config source property
    public String useAs;          // use this name to call this data
    public boolean isAggregationData;  // if this data is an aggregation result
    public String aggregationLevel; // what level should this aggregation access
    public AggregateDataComponent aggregateDataComponent;  // what kinds of data should be used as materials to retrieve this aggregation result

    public AggregateSource(){
    }

    public void setUseAs(String useAs) {
        this.useAs = useAs;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFrom() {
        return from;
    }

    public String getUseAs() {
        return useAs;
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
