package soselab.msdobot.aggregatebot.Entity.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;

public class ServiceList {
    public ArrayList<ServiceSystem> serviceList;
    public HashMap<String, Service> serviceMap;

    public ServiceList(){}

    public boolean isSystem(String serviceName){
        for(ServiceSystem serviceSystem : serviceList){
            if(serviceSystem.name.equals(serviceName))
                return true;
        }
        return false;
    }

    public int subServiceCount(String serviceName){
        for(ServiceSystem serviceSystem : serviceList){
            if(serviceSystem.name.equals(serviceName))
                return serviceSystem.subSystemCount();
        }
        return 0;
    }

    /**
     * get service list of specific system name
     * @param systemName target system name
     * @return service list of system, return empty arraylist if nothing found
     */
    public ArrayList<Service> getSubServiceList(String systemName){
        ArrayList<Service> serviceList = new ArrayList<>();
        if(systemName.isBlank())
            return serviceList;
        // normal service level
        if(serviceMap.get(systemName).type.equals("service"))
            serviceList.add(serviceMap.get(systemName));
        else{
            // system level
            for(ServiceSystem serviceSystem: this.serviceList){
                if(serviceSystem.name.equals(systemName)) {
                    // add system level data as sub service
                    serviceList.add(serviceMap.get(systemName));
                    // add sub service of target system
                    for(Service service : serviceSystem.service){
                        serviceList.add(serviceMap.get(service.name));
                    }
                    break;
                }
            }
        }
        return serviceList;
    }

    public void setServiceMap(HashMap<String, Service> serviceMap) {
        this.serviceMap = serviceMap;
    }

    @Override
    public String toString(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(serviceList);
    }
}
