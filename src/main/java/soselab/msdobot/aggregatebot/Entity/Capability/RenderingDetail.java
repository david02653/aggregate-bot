package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.Gson;

import java.util.ArrayList;

public class RenderingDetail {

    // what kinds of data are used to execute this aggregation process
    public ArrayList<AggregateSource> dataSource;

    public RenderingDetail(){}

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
