package soselab.msdobot.aggregatebot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import soselab.msdobot.aggregatebot.Entity.RasaIntent;
import soselab.msdobot.aggregatebot.Entity.Service.SubService;
import soselab.msdobot.aggregatebot.Entity.Skill.Skill;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * define which intent activate which agent/skill
 */
@Service
public class Orchestrator {

    public void skillSelector(RasaIntent intent){
        String intentName = intent.getIntent();
        String jobName = intent.getJobName();
        Gson gson = new Gson();

        // get correspond skill by intent name
        Skill skill = ConfigLoader.skillList.getSkill(intentName);
        if(skill == null) {
            System.out.println(">> [DEBUG] No available skill found.");
            return;
        }
        System.out.println("[DEBUG] available skill detected : " + gson.toJson(skill));

        // check if target is on system level
//        if(ConfigLoader.serviceList.serviceMap.get(jobName) == null) {
//            System.out.println("[DEBUG] target service not exist.");
//            return;
//        }
        ArrayList<SubService> subServiceList = ConfigLoader.serviceList.getSubServiceList(jobName);
        System.out.println("[DEBUG] todo subService list: " + gson.toJson(subServiceList));
        if(subServiceList.isEmpty()) {
            System.out.println("[DEBUG] target service not exist.");
            return;
        }

        // fire skill request for every sub-service
        if(skill.method.equals("POST")) {
            for (SubService subService : subServiceList) {
                System.out.println("[DEBUG] current subService " + gson.toJson(subService));
                postRequestSkill(skill, subService);
            }
        }else{
            // get method
            for (SubService subService : subServiceList) {
                System.out.println("[DEBUG] current subService " + gson.toJson(subService));
                getRequestSkill(skill, subService);
            }
        }
    }

    public void postRequestSkill(Skill skill, SubService service){
        HashMap<String, String> configMap = service.getJenkinsConfigMap();
        System.out.println("[DEBUG] config map: " + new Gson().toJson(configMap));
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonObject requestBody = new JsonObject();
        for(String key: skill.input){
            requestBody.addProperty(key, configMap.get(key));
        }
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        System.out.println("[DEBUG] try to request skill with body " + new Gson().toJson(requestBody));
        System.out.println("[DEBUG] try to request skill from " + skill.endpoint);
        ResponseEntity<String> resp = restTemplate.exchange(skill.endpoint, HttpMethod.POST, entity, String.class);
        System.out.println(service.name + " " + resp.getBody());
    }

    public void getRequestSkill(Skill skill, SubService service){
        String requestUrl = skill.endpoint;
        HashMap<String, String> configMap = service.getJenkinsConfigMap();
        if(!skill.input.isEmpty()){
            requestUrl = requestUrl + "?";
            for(String input: skill.input){
                requestUrl = requestUrl + input + "=" + configMap.get(input) + "&";
            }
            requestUrl = requestUrl.substring(0, requestUrl.length() - 1);
        }
        System.out.println("[DEBUG] request url : " + requestUrl);
        RestTemplate template = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = template.exchange(requestUrl, HttpMethod.GET, entity, String.class);
        System.out.println(resp.getBody());
    }

    /**
     * check target service is on System level or sub-service level
     * @param serviceName target service name
     * @return if target service is a system or not, return true if true
     */
    public boolean isSystem(String serviceName){
        // check service list
        return ConfigLoader.serviceList.isSystem(serviceName);
    }
}
