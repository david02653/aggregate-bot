package soselab.msdobot.aggregatebot.Entity;

import java.util.ArrayList;
import java.util.HashMap;

public class CapabilityReport {

    public String capability;
    public String service;
    public HashMap<String, ArrayList<String>> contextProperty;
//    public HashMap<String, HashMap<String, String>> serviceConfigReport;

    public CapabilityReport(){
        contextProperty = new HashMap<>();
    }
    public CapabilityReport(String capability, String service){
        this.capability = capability;
        this.service = service;
        contextProperty = new HashMap<>();
    }

    public void addProperty(String contextName, String property){
        ArrayList<String> temp;
        if(contextProperty.containsKey(contextName)){
            temp = contextProperty.get(contextName);
        }else{
            temp = new ArrayList<>();
        }
        temp.add(property);
        this.contextProperty.put(contextName, temp);
    }

//    /**
//     * add new properties in service config report
//     * @param serviceName target service name
//     * @param contextName target property context
//     * @param property target property
//     */
//    public void addContextProperty(String serviceName, String contextName, String property){
//        if(serviceConfigReport.containsKey(serviceName)){
//            serviceConfigReport.get(serviceName).put(contextName, property);
//        }else{
//            HashMap<String, String> temp = new HashMap<>();
//            temp.put(contextName, property);
//            serviceConfigReport.put(serviceName, temp);
//        }
//    }


}
