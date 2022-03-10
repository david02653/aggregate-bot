package soselab.msdobot.aggregatebot.Entity.Service;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

public class ServiceList {
    public ArrayList<ServiceSystem> serviceList;
    public HashMap<String, SubService> serviceMap;

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

    public ArrayList<SubService> getSubServiceList(String serviceName){
        ArrayList<SubService> subServiceList = new ArrayList<>();
        // normal service level
        if(serviceMap.get(serviceName).type.equals("service"))
            subServiceList.add(serviceMap.get(serviceName));
        else{
            // system level
            for(ServiceSystem serviceSystem: serviceList){
                if(serviceSystem.name.equals(serviceName)) {
                    // add system level data as sub service
                    subServiceList.add(serviceMap.get(serviceName));
                    // add sub service of target system
                    for(SubService subService: serviceSystem.subService){
                        subServiceList.add(serviceMap.get(subService.name));
                    }
                    break;
                }
            }
        }
        return subServiceList;
    }

    public void setServiceMap(HashMap<String, SubService> serviceMap) {
        this.serviceMap = serviceMap;
    }

    @Override
    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(serviceList);
    }
}
