package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.Gson;

public class AggregateSource {

    public String context;     // in purpose to access different context domain config
    public String from;        // config source
    public String outputName;  // output property name

    public AggregateSource(){
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
