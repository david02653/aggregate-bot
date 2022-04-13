package soselab.msdobot.aggregatebot.Entity.UpperIntent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import soselab.msdobot.aggregatebot.Entity.Capability.Capability;

import java.util.ArrayList;

public class UpperIntentList {

    public ArrayList<UpperIntent> crossCapabilityList;

    public UpperIntentList(){
    }

    public ArrayList<Capability> getSemiCapabilityList(String intent){
        for(UpperIntent upperIntent : crossCapabilityList){
            if(upperIntent.upperIntent.equals(intent)) {
                return upperIntent.getSequencedSemiCapabilityList();
            }
        }
        return new ArrayList<>();
    }

    @Override
    public String toString(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
