package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.Gson;

import java.util.ArrayList;

public class AggregateDetail {

    // what kinds of data are used to execute this aggregation process
    public ArrayList<AggregateSource> dataSource;
    // should this aggregation result be stored
    public boolean storeResult;
    // what component should be used to retrieve this result
    public AggregateDataComponent usedComponent;
    // how to call to result
    public String resultName;

    public AggregateDetail(){
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
