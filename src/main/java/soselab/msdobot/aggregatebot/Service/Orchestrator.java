package soselab.msdobot.aggregatebot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import soselab.msdobot.aggregatebot.Entity.RasaIntent;
import soselab.msdobot.aggregatebot.Entity.Service.SubService;
import soselab.msdobot.aggregatebot.Entity.Skill.JsonInfo;
import soselab.msdobot.aggregatebot.Entity.Skill.Skill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * define which intent activate which agent/skill
 */
@Service
public class Orchestrator {

    public void skillSelector(RasaIntent intent){
        String intentName = intent.getIntent();
        String jobName = intent.getJobName();
        Gson gson = new Gson();
        final ExecutorService executor = Executors.newFixedThreadPool(5);
        final List<Future<?>> futures = new ArrayList<>();
        Future<?> future;

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
        // todo: check if every thread execute successfully, use future.get() and add return type
        if(skill.method.equals("POST")) {
            for (SubService subService : subServiceList) {
                System.out.println("[DEBUG] current subService " + gson.toJson(subService));
                future = executor.submit(() -> postRequestSkill(skill, subService));
                futures.add(future);
            }
        }else{
            // get method
            for (SubService subService : subServiceList) {
                System.out.println("[DEBUG] current subService " + gson.toJson(subService));
                if(!hasPathVariable(skill.endpoint))
                    future = executor.submit(() -> getRequestSkill(skill, subService));
                else
                    future = executor.submit(() -> getRequestSkillViaPathVariable(skill, subService));
                futures.add(future);
            }
        }
        // collect futures and check if every thread works fine
        try{
            for(Future<?> executeResult: futures){
                executeResult.get();
            }
        }catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
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
        parseJsonResult(skill, service, resp.getBody());
    }

    public void getRequestSkill(Skill skill, SubService service){
        StringBuilder requestUrl = new StringBuilder(skill.endpoint);
        HashMap<String, String> configMap = service.getJenkinsConfigMap();
        if(!skill.input.isEmpty()){
            requestUrl.append("?");
            for(String input: skill.input){
                requestUrl.append(input).append("=").append(configMap.get(input)).append("&");
            }
            requestUrl = new StringBuilder(requestUrl.substring(0, requestUrl.length() - 1));
        }
        System.out.println("[DEBUG] request url : " + requestUrl);
        RestTemplate template = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = template.exchange(requestUrl.toString(), HttpMethod.GET, entity, String.class);
        System.out.println(resp.getBody());
    }

    /**
     * request get method skill with path variable
     * @param skill
     * @param service
     */
    public void getRequestSkillViaPathVariable(Skill skill, SubService service){
        String variablePattern;
        String requestUrl = skill.endpoint;
        HashMap<String, String> configMap = service.getJenkinsConfigMap();
        if(!skill.input.isEmpty()){
            for(String input: skill.input){
                variablePattern = "\\{" + input + "}";
                requestUrl = requestUrl.replaceAll(variablePattern, configMap.get(input));
            }
        }
        System.out.println("[DEBUG] request url: " + requestUrl);
        RestTemplate template = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = template.exchange(requestUrl, HttpMethod.GET, entity, String.class);
        System.out.println(resp.getBody());
    }

    /**
     * check if url contains '{}', if contains then return true
     * @param url skill url
     * @return true if skill url contains '{}'
     */
    public boolean hasPathVariable(String url){
        return url.contains("{") && url.contains("}");
    }

    /**
     * parse output result, if output type is json, try to extract info by given json path
     */
    public void parseJsonResult(Skill skill, SubService service, String output){
        if(!skill.output.type.equals("json")) return;
        ArrayList<JsonInfo> targetInfoList = skill.output.jsonInfo;
        for(JsonInfo jsonInfo: targetInfoList){
            System.out.println(">>> [" + service.name + "] " + jsonInfo.name + " : " + JsonPath.read(output, jsonInfo.jsonPath).toString());
        }
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
