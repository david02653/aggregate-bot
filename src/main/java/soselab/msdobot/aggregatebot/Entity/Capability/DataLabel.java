package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.GsonBuilder;

public class DataLabel {

    // data source
    public String from;
    // data destination
    public String to;

    public DataLabel(){
    }

    @Override
    public String toString(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
