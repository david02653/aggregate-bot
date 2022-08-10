package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;

public class StoredData {

    public ArrayList<DataLabel> input; // what should be stored from input data
    public ArrayList<DataLabel> output; // what should be stored from output data

    public StoredData(){
    }

    @Override
    public String toString(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
