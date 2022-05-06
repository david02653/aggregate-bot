package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.Gson;

import java.util.ArrayList;

public class AggregateDetail {

    // what kinds of data are used to execute this aggregation process
    public ArrayList<AggregateSource> dataSource;
    // should this aggregation result be stored
    public boolean storeResult;
    // what material should be used to retrieve this result
    public AggregateDataMaterial usedMaterial;

    public AggregateDetail(){
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
