package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;

/**
 * capability data format, consider Polymorphism ?
 */
public class Capability {
    public String name; // what is this capability
    public String context; // what tools is used
    public boolean isAggregateMethod; // determine whether this is an aggregate method
    public boolean isRenderingMethod; // determine whether this is a rendering method
    public String accessLevel; // do this capability works on a single service or a system
    public String order; // attribute of upperIntent
    public String description; // what is this capability
    public String method; // what RESTful method should be used
    public String atomicIntent; // intent used to trigger current capability
    public String apiEndpoint; // endpoint of this capability
    public ArrayList<String> input; // what data should be used to call the endpoint
    public CapabilityOutput output; // what data should the endpoint response
    public ArrayList<CustomMapping> usedMappingList; // used specific mapping input data
    public StoredData storedData; // what data should be stored
    public AggregateDetail aggregateDetail; // what data is used to call this aggregate capability
    public RenderingDetail renderingDetail; // what data is used to call this rendering capability

    public void setAggregateMethod(boolean aggregateMethod) {
        isAggregateMethod = aggregateMethod;
    }

    public void setRenderingMethod(boolean renderingMethod) {
        isRenderingMethod = renderingMethod;
    }

    public Capability(){}

    @Override
    public String toString(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
