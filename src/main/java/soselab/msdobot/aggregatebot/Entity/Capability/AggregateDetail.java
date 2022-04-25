package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.Gson;

import java.util.ArrayList;

public class AggregateDetail {

    public String method;
    public ArrayList<AggregateSource> dataSource;

    public AggregateDetail(){
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
