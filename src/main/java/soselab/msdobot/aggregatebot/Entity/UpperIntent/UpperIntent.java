package soselab.msdobot.aggregatebot.Entity.UpperIntent;

import com.google.gson.Gson;
import soselab.msdobot.aggregatebot.Entity.Capability.Capability;

import java.util.ArrayList;
import java.util.Comparator;

public class UpperIntent {

    public String name;
    public String correspondIntent;
    public ArrayList<Capability> sequencedCapabilityList;

    public UpperIntent(){
    }

    public ArrayList<Capability> getSequencedSemiSkillList(){
        sortSequencedCapabilityList();
        return new ArrayList<>(sequencedCapabilityList);
    }

    public void sortSequencedCapabilityList(){
        this.sequencedCapabilityList.sort(Comparator.comparingInt((Capability capability) -> Integer.parseInt(capability.order)));
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
