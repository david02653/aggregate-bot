package soselab.msdobot.aggregatebot.Entity;

import java.util.HashMap;

public class ConfigReportMap {

    public HashMap<String, HashMap<String, String>> serviceConfigReport;

    public ConfigReportMap(){
        serviceConfigReport = new HashMap<>();
    }

    /**
     * add new properties in service config report
     * @param serviceName target service name
     * @param contextName target property context
     * @param property target property
     */
    public void addContextProperty(String serviceName, String contextName, String property){
        if(serviceConfigReport.containsKey(serviceName)){
            serviceConfigReport.get(serviceName).put(contextName, property);
        }else{
            HashMap<String, String> temp = new HashMap<>();
            temp.put(contextName, property);
            serviceConfigReport.put(serviceName, temp);
        }
    }


}
