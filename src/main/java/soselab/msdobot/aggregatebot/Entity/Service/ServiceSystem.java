package soselab.msdobot.aggregatebot.Entity.Service;

import java.util.ArrayList;

/**
 * raw data read from service config
 */
public class ServiceSystem {
    public String name;
    public String type;
    public String description;
    public ArrayList<ServiceConfig> config;
    public ArrayList<Service> service;

    public ServiceSystem(){}

    public int subSystemCount(){
        return service.size();
    }

    public ArrayList<Service> getSubService(){
        return this.service;
    }
}
