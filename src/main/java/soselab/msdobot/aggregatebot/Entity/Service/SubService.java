package soselab.msdobot.aggregatebot.Entity.Service;

import java.util.HashMap;

public class SubService {
    public String name;
    public String type;
    public String description;
    public Config config;
    public HashMap<String, String> configMap;

    public SubService(){}
    public SubService(String serviceName, String type, String description, Config config){
        this.name = serviceName;
        this.type = type;
        this.description = description;
        this.config = config;
        updateJenkinsConfigMap();
    }

    public void setJenkinsConfig(Config config) {
        this.config = config;
        updateJenkinsConfigMap();
    }

    private void updateJenkinsConfigMap(){
        HashMap<String, String> map = new HashMap<>();
        map.put("username", this.config.username);
        map.put("accessToken", this.config.accessToken);
        map.put("endpoint", this.config.endpoint);
        map.put("targetService", this.name);
        this.configMap = map;
    }

    public HashMap<String, String> getConfigMap() {
        return configMap;
    }

    public SubService overrideJenkinsConfig(Config newConfig){
        SubService updatedService = new SubService(this.name, this.type, this.description, this.config);
        if(newConfig != null)
            updatedService.setJenkinsConfig(newConfig);
        return updatedService;
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
