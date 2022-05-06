package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;

public class Capability {
    public String name;
    public String context;
    public boolean isAggregateMethod; // determine whether this is an aggregate method
    public boolean isRenderingMethod; // determine whether this is a rendering method
    public String accessLevel;
    public String order; // attribute of upperIntent
    public String description;
    public String method;
    public String atomicIntent;
    public String apiEndpoint;
    public ArrayList<String> input;
    public CapabilityOutput output;
    public ArrayList<CustomMapping> usedMappingList;
    public StoredData storedData;
    public AggregateDetail aggregateDetail;
    public RenderingDetail renderingDetail;

    public Capability(){}

    @Override
    public String toString(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
