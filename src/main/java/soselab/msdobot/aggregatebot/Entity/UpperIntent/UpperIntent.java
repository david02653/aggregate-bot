package soselab.msdobot.aggregatebot.Entity.UpperIntent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import soselab.msdobot.aggregatebot.Entity.Capability.Capability;

import java.util.ArrayList;
import java.util.Comparator;

public class UpperIntent {

    public String name;
    public String upperIntent;
    private String description;
    public ArrayList<Capability> sequencedCapabilityList;

    public UpperIntent(){
    }

    public ArrayList<Capability> getSequencedSemiCapabilityList(){
        sortSequencedCapabilityList();
        return new ArrayList<>(sequencedCapabilityList);
    }

    public void sortSequencedCapabilityList(){
        this.sequencedCapabilityList.sort(Comparator.comparingInt((Capability capability) -> Integer.parseInt(capability.order)));
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
