package soselab.msdobot.aggregatebot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import soselab.msdobot.aggregatebot.Entity.SkillConfig;
import soselab.msdobot.aggregatebot.Entity.RasaIntent;
import soselab.msdobot.aggregatebot.Entity.Service.SubService;
import soselab.msdobot.aggregatebot.Entity.Skill.JsonInfo;
import soselab.msdobot.aggregatebot.Entity.Skill.Skill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

/**
 * define which intent activate which agent/skill
 */
@Service
public class Orchestrator {

    /**
     * config of each service
     */
    public static ConcurrentHashMap<String, SkillConfig> sessionData;

    public Orchestrator(){
        sessionData = new ConcurrentHashMap<>();
    }

    public void skillSelector(RasaIntent intent){
        String intentName = intent.getIntent();
        String jobName = intent.getJobName();
        Gson gson = new Gson();
        final ExecutorService executor = Executors.newFixedThreadPool(5);
        final List<Future<HashMap<String, String>>> futures = new ArrayList<>();
        Future<HashMap<String, String>> future;

        // get correspond skill by intent name
        ArrayList<Skill> skillList = getCorrespondSkillList(intentName);
        if(skillList == null || skillList.isEmpty()) {
            System.out.println(">> [DEBUG] No available skill found.");
            return;
        }
        System.out.println("[DEBUG] available skill detected : " + gson.toJson(skillList));

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

        // execute sequenced skill list
        for(Skill skill: skillList){
            // todo: store previous skill info, add output data mapping
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
                HashMap<String, String> result;
                for(Future<HashMap<String, String>> executeResult: futures){
                    result = executeResult.get();
                }
                futures.clear();
            }catch (InterruptedException | ExecutionException e){
                e.printStackTrace();
            }
        }

    }

    public ArrayList<Skill> getCorrespondSkillList(String intent){
        ArrayList<Skill> resultList = ConfigLoader.skillList.getSkill(intent);
        if(resultList.isEmpty())
            resultList = ConfigLoader.bigIntentList.getSkillList(intent);
        return resultList;
    }

    public void addServiceConfig(String serviceName, String dataKey, String dataValue){
        SkillConfig tempSession = null;
        if(sessionData.containsKey(serviceName)){
            // if target service already has previous record, retrieve previous data
            tempSession = sessionData.get(serviceName);
        }else{
            // create new skill config
            tempSession = new SkillConfig();
        }
        // store new config in skill config
        tempSession.addData(dataKey, dataValue);
        sessionData.put(serviceName, tempSession);
    }

    public HashMap<String, String> postRequestSkill(Skill skill, SubService service){
        HashMap<String, String> configMap = service.getConfigMap();
        System.out.println("[DEBUG] config map: " + new Gson().toJson(configMap));
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonObject requestBody = new JsonObject();
        for(String key: skill.input){
            requestBody.addProperty(key, configMap.get(key));
        }
        // load previous skill config
        if(sessionData.containsKey(service.name)){
            SkillConfig previousConfig = sessionData.get(service.name);
            for(String key: skill.input){
                if(previousConfig.content.containsKey(key))
                    requestBody.addProperty(key, previousConfig.content.get(key));
            }
        }
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        System.out.println("[DEBUG] try to request skill with body " + new Gson().toJson(requestBody));
        System.out.println("[DEBUG] try to request skill from " + skill.endpoint);
        ResponseEntity<String> resp = restTemplate.exchange(skill.endpoint, HttpMethod.POST, entity, String.class);
        System.out.println(service.name + " " + resp.getBody());
        return parseRequestResult(skill, service, resp.getBody());
    }

    public HashMap<String, String> getRequestSkill(Skill skill, SubService service){
        StringBuilder requestUrl = new StringBuilder(skill.endpoint);
        HashMap<String, String> configMap = service.getConfigMap();
        SkillConfig previousConfig = sessionData.get(service.name);
        if(!skill.input.isEmpty()){
            requestUrl.append("?");
            for(String input: skill.input){
                if(previousConfig != null && previousConfig.content.containsKey(input))
                    requestUrl.append(input).append("=").append(previousConfig.content.get(input)).append("&");
                else
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
        return parseRequestResult(skill, service, resp.getBody());
    }

    /**
     * request get method skill with path variable
     * @param skill
     * @param service
     */
    public HashMap<String, String> getRequestSkillViaPathVariable(Skill skill, SubService service){
        String variablePattern;
        String requestUrl = skill.endpoint;
        HashMap<String, String> configMap = service.getConfigMap();
        SkillConfig previousConfig = sessionData.get(service.name);
        if(!skill.input.isEmpty()){
            for(String input: skill.input){
                variablePattern = "\\{" + input + "}";
                if(previousConfig != null && previousConfig.content.containsKey(input))
                    requestUrl = requestUrl.replaceAll(variablePattern, previousConfig.content.get(input));
                else
                    requestUrl = requestUrl.replaceAll(variablePattern, configMap.get(input));
            }
        }
        System.out.println("[DEBUG] request url: " + requestUrl);
        RestTemplate template = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = template.exchange(requestUrl, HttpMethod.GET, entity, String.class);
        System.out.println(resp.getBody());
        return parseRequestResult(skill, service, resp.getBody());
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
     * parse output result, if output type is json, try to extract info by given json path, if output type is text and has tag, return a hash map data pair with tag as key and data as value
     */
    public HashMap<String, String> parseRequestResult(Skill skill, SubService service, String output){
        HashMap<String, String> result = new HashMap<>();
        result.put("targetService", service.name);
        if(skill.output.type.equals("plaintext") && skill.output.tag != null){
            // contains plain text info
            System.out.println(">>> [" + skill.output.tag + "] " + output);
            result.put(skill.output.tag, output);
        }
        if(skill.output.type.equals("json")) {
            ArrayList<JsonInfo> targetInfoList = skill.output.jsonInfo;
            for (JsonInfo jsonInfo : targetInfoList) {
                String info = JsonPath.read(output, jsonInfo.jsonPath).toString();
                System.out.println(">>> [" + service.name + "] " + jsonInfo.description + " : " + info);
                if(!jsonInfo.tag.isEmpty())
                    result.put(jsonInfo.tag, info);
            }
        }
        return result;
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
