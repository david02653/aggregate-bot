package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;

public class StoredData {

    public ArrayList<DataLabel> input;
    public ArrayList<DataLabel> output;

    public StoredData(){
    }

    @Override
    public String toString(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
