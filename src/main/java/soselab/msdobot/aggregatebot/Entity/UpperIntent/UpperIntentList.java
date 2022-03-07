package soselab.msdobot.aggregatebot.Entity.UpperIntent;

import com.google.gson.Gson;
import soselab.msdobot.aggregatebot.Entity.Capability.Capability;

import java.util.ArrayList;

public class UpperIntentList {

    public ArrayList<UpperIntent> crossCapabilityList;

    public UpperIntentList(){
    }

    public ArrayList<Capability> getSemiCapabilityList(String intent){
        for(UpperIntent upperIntent : crossCapabilityList){
            if(upperIntent.correspondIntent.equals(intent)) {
                return upperIntent.getSequencedSemiSkillList();
            }
        }
        return new ArrayList<>();
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
