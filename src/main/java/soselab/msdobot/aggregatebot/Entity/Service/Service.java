package soselab.msdobot.aggregatebot.Entity.Service;

import java.util.ArrayList;
import java.util.HashMap;

public class Service {
    public String name;
    public String type;
    public String description;
    public ArrayList<ServiceConfig> config;
    /* HashMap< contextName, HashMap < propertyName, propertyValue >> */
    public HashMap<String, HashMap<String, String>> configMap;

    public Service(){}

    /**
     * create config map when new instance of Service is created
     * @param serviceName name of new service
     * @param type type of new service
     * @param description description of new service
     * @param config config of new service
     */
    public Service(String serviceName, String type, String description, ArrayList<ServiceConfig> config){
        this.name = serviceName;
        this.type = type;
        this.description = description;
        this.config = config;
        createConfigMap();
    }

    /**
     * create hash map of current service config, assume given service config already override system config with service config<br>
     * hash map data format : HashMap< contextName, HashMap < PropertyName, PropertyValue >>
     */
    public void createConfigMap(){
        HashMap<String, HashMap<String, String>> map = new HashMap<>();
        if(config != null) {
            for (ServiceConfig serviceConfig : config) {
                HashMap<String, String> propertyMap = new HashMap<>();
                for (ContextConfig contextConfig : serviceConfig.properties) {
                    propertyMap.put(contextConfig.name, contextConfig.value);
                }
                // auto inject service name
                propertyMap.put("Api.serviceName", this.name);
                map.put(serviceConfig.context, propertyMap);
            }
        }
        if(config == null || config.isEmpty()){
            System.out.println("[NULL CONFIG] " + this.name);
            HashMap<String, String> propertyMap = new HashMap<>();
            propertyMap.put("Api.serviceName", this.name);
            map.put("general", propertyMap);
        }
        this.configMap = map;
    }

    public void setConfig(ArrayList<ServiceConfig> config) {
        this.config = config;
    }

    public HashMap<String, HashMap<String, String>> getConfigMap() {
        return configMap;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
