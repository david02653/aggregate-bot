package soselab.msdobot.aggregatebot.Entity.Service;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;

public class ServiceConfig {
    public String context;
    public ArrayList<ContextConfig> properties;

    public void setProperties(ArrayList<ContextConfig> properties) {
        this.properties = properties;
    }

    public ServiceConfig(){}

    @Override
    public String toString(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
