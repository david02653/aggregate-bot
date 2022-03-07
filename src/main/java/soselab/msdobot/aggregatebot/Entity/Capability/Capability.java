package soselab.msdobot.aggregatebot.Entity.Capability;

import java.util.ArrayList;

public class Capability {
    public String name;
    public String type;
    public String order;
    public String description;
    public String method;
    public String correspondIntent;
    public String apiEndpoint;
    public ArrayList<String> input;
    public CapabilityOutput output;

    public Capability(){}
}
