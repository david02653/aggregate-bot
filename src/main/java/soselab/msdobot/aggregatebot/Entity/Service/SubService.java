package soselab.msdobot.aggregatebot.Entity.Service;

import java.util.HashMap;

public class SubService {
    public String name;
    public String type;
    public String description;
    public JenkinsConfig jenkinsConfig;
    public HashMap<String, String> jenkinsConfigMap;

    public SubService(){}
    public SubService(String serviceName, String type, String description, JenkinsConfig config){
        this.name = serviceName;
        this.type = type;
        this.description = description;
        this.jenkinsConfig = config;
        updateJenkinsConfigMap();
    }

    public void setJenkinsConfig(JenkinsConfig jenkinsConfig) {
        this.jenkinsConfig = jenkinsConfig;
        updateJenkinsConfigMap();
    }

    private void updateJenkinsConfigMap(){
        HashMap<String, String> map = new HashMap<>();
        map.put("username", this.jenkinsConfig.username);
        map.put("accessToken", this.jenkinsConfig.accessToken);
        map.put("endpoint", this.jenkinsConfig.endpoint);
        map.put("targetService", this.name);
        this.jenkinsConfigMap = map;
    }

    public HashMap<String, String> getJenkinsConfigMap() {
        return jenkinsConfigMap;
    }

    public SubService overrideJenkinsConfig(JenkinsConfig newConfig){
        SubService updatedService = new SubService(this.name, this.type, this.description, this.jenkinsConfig);
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
