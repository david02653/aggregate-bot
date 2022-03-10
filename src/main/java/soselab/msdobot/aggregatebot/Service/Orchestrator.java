package soselab.msdobot.aggregatebot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import soselab.msdobot.aggregatebot.Entity.Capability.Capability;
import soselab.msdobot.aggregatebot.Entity.CapabilityConfig;
import soselab.msdobot.aggregatebot.Entity.RasaIntent;
import soselab.msdobot.aggregatebot.Entity.Service.SubService;
import soselab.msdobot.aggregatebot.Entity.Capability.JsonInfo;
import soselab.msdobot.aggregatebot.Entity.Vocabulary.CustomMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * define which intent activate which agent/skill
 */
@Service
public class Orchestrator {

    /**
     * config of each service
     */
    public static ConcurrentHashMap<String, CapabilityConfig> sessionData;

    public Orchestrator(){
        sessionData = new ConcurrentHashMap<>();
    }

    public void capabilitySelector(RasaIntent intent){
        String intentName = intent.getIntent();
        String jobName = intent.getJobName();
        Gson gson = new Gson();
        final ExecutorService executor = Executors.newFixedThreadPool(5);
        final List<Future<HashMap<String, String>>> futures = new ArrayList<>();
        Future<HashMap<String, String>> future;

        // get correspond capability by intent name
        ArrayList<Capability> capabilityList = getCorrespondCapabilityList(intentName);
        if(capabilityList == null || capabilityList.isEmpty()) {
            System.out.println(">> [DEBUG] No available skill found.");
            return;
        }
        System.out.println("[DEBUG] available skill detected : " + gson.toJson(capabilityList));

        // get service list
        ArrayList<SubService> subServiceList = ConfigLoader.serviceList.getSubServiceList(jobName);
        System.out.println("[DEBUG] todo subService list: " + gson.toJson(subServiceList));
        if(subServiceList.isEmpty()) {
            System.out.println("[DEBUG] target service not exist.");
            return;
        }

        // execute sequenced capability list
        for(Capability capability : capabilityList){
            // todo: store previous capability info, add output data mapping
            // fire skill request for every sub-service
            // todo: any world changing capability ?

            // check capability access level

            if(capability.method.equals("POST")) {
                for (SubService subService : subServiceList) {
                    if(capability.accessLevel.equals(subService.type)) {
                        System.out.println("[DEBUG] current subService " + gson.toJson(subService));
                        future = executor.submit(() -> postRequestCapability(capability, subService));
                        futures.add(future);
                    }
                }
            }else{
                // get method
                for (SubService subService : subServiceList) {
                    System.out.println("[DEBUG] current subService " + gson.toJson(subService));
                    if(capability.accessLevel.equals(subService.type)) {
                        if (!hasPathVariable(capability.apiEndpoint))
                            future = executor.submit(() -> getRequestCapability(capability, subService));
                        else
                            future = executor.submit(() -> getRequestCapabilityViaPathVariable(capability, subService));
                        futures.add(future);
                    }
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

    /**
     * get complete correspond skill list
     * @param intent
     * @return
     */
    public ArrayList<Capability> getCorrespondCapabilityList(String intent){
        ArrayList<Capability> resultList = ConfigLoader.capabilityList.getCapability(intent);
        if(resultList.isEmpty()) {
//            resultList = ConfigLoader.bigIntentList.getSemiSkillList(intent);
            resultList = ConfigLoader.capabilityList.getCompleteCapability(ConfigLoader.upperIntentList.getSemiCapabilityList(intent));
        }
        return resultList;
    }

    public void addServiceConfig(String serviceName, String dataKey, String dataValue){
        CapabilityConfig tempSession = null;
        if(sessionData.containsKey(serviceName)){
            // if target service already has previous record, retrieve previous data
            tempSession = sessionData.get(serviceName);
        }else{
            // create new capability config
            tempSession = new CapabilityConfig();
        }
        // store new config in capability config
        tempSession.addData(dataKey, dataValue);
        sessionData.put(serviceName, tempSession);
    }

    public String generateCustomMappingConfig(String mapName, HashMap<String, String> serviceConfigMap, CapabilityConfig sessionConfig){
        CustomMapping mapping = ConfigLoader.vocabularyList.customMappingHashMap.get(mapName);
        String mappingSchema = mapping.schema;
        for(String usedVocabulary: mapping.usedVocabulary){
            if(serviceConfigMap.containsKey(usedVocabulary))
                mappingSchema = mappingSchema.replaceAll("\\$" + usedVocabulary, serviceConfigMap.get(usedVocabulary));
            else
                mappingSchema = mappingSchema.replaceAll("\\$" + usedVocabulary, sessionConfig.content.get(usedVocabulary));
        }
        return mappingSchema;
    }

    public HashMap<String, String> postRequestCapability(Capability capability, SubService service){
        HashMap<String, String> configMap = service.getConfigMap();
        CapabilityConfig sessionConfig = sessionData.get(service.name);
        System.out.println("[DEBUG] config map: " + new Gson().toJson(configMap));
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonObject requestBody = new JsonObject();
        // load config from service default config
        for(String key: capability.input){
            if(!ConfigLoader.vocabularyList.customMappingHashMap.containsKey(key))
                requestBody.addProperty(key, configMap.get(key));
            else
                requestBody.addProperty(key, generateCustomMappingConfig(key, configMap, sessionConfig));
        }
        // load previous capability config from session
        if(sessionData.containsKey(service.name)){
            for(String key: capability.input){
                if(sessionConfig.content.containsKey(key))
                    requestBody.addProperty(key, sessionConfig.content.get(key));
            }
        }
        System.out.println("[DEBUG][orchestrator][POST] requestBody: " + requestBody);
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        System.out.println("[DEBUG] try to request capability with body " + new Gson().toJson(requestBody));
        System.out.println("[DEBUG] try to request capability from " + capability.apiEndpoint);
        ResponseEntity<String> resp = restTemplate.exchange(capability.apiEndpoint, HttpMethod.POST, entity, String.class);
        System.out.println(service.name + " " + resp.getBody());
        return parseRequestResult(capability, service, resp.getBody());
    }

    public HashMap<String, String> getRequestCapability(Capability capability, SubService service){
        StringBuilder requestUrl = new StringBuilder(capability.apiEndpoint);
        HashMap<String, String> configMap = service.getConfigMap();
        CapabilityConfig previousConfig = sessionData.get(service.name);
        if(!capability.input.isEmpty()){
            requestUrl.append("?");
            for(String input: capability.input){
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
        return parseRequestResult(capability, service, resp.getBody());
    }

    /**
     * request get method capability with path variable
     * @param capability
     * @param service
     */
    public HashMap<String, String> getRequestCapabilityViaPathVariable(Capability capability, SubService service){
        String variablePattern;
        String requestUrl = capability.apiEndpoint;
        HashMap<String, String> configMap = service.getConfigMap();
        CapabilityConfig previousConfig = sessionData.get(service.name);
        if(!capability.input.isEmpty()){
            for(String input: capability.input){
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
        return parseRequestResult(capability, service, resp.getBody());
    }

    /**
     * check if url contains '{}', if contains then return true
     * @param url capability url
     * @return true if capability url contains '{}'
     */
    public boolean hasPathVariable(String url){
        return url.contains("{") && url.contains("}");
    }

    /**
     * parse output result, if output type is json, try to extract info by given json path, if output type is text and has tag, return a hash map data pair with tag as key and data as value
     */
    public HashMap<String, String> parseRequestResult(Capability capability, SubService service, String output){
        Gson gson = new Gson();
        System.out.println("[parse result] capability > " + gson.toJson(capability));
        System.out.println("[parse result] service > " + gson.toJson(service));
        System.out.println("[parse result] output > " + output);
        HashMap<String, String> result = new HashMap<>();
        result.put("targetService", service.name);
        if(capability.output.type.equals("plainText") && capability.output.storedData != null){
            // contains plain text info
            System.out.println(">>> [" + capability.output.storedData + "] " + output);
            result.put(capability.output.storedData, output);
        }
        if(capability.output.type.equals("json")) {
            ArrayList<JsonInfo> targetInfoList = capability.output.jsonInfo;
            for (JsonInfo jsonInfo : targetInfoList) {
                String info = JsonPath.read(output, jsonInfo.jsonPath).toString();
                System.out.println(">>> [" + service.name + "] " + jsonInfo.description + " : " + info);
                if(!jsonInfo.storedData.isEmpty())
                    result.put(jsonInfo.storedData, info);
            }
        }
        System.out.println("[parse result] result map: " + gson.toJson(result));
        // add new config retrieved after capability execution
        for(Map.Entry<String, String> capabilityEntry: result.entrySet()){
            System.out.println("[parse result] key: " + capabilityEntry.getKey() + ", value: " + capabilityEntry.getValue() + " > " + service.name);
            addServiceConfig(service.name, capabilityEntry.getKey(), capabilityEntry.getValue());
        }
        return result;
    }

    /**
     * expire all current service session config
     */
    private void expireAllSessionData(){
        Orchestrator.sessionData = new ConcurrentHashMap<>();
        System.out.println("[DEBUG] all session config is expired !");
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
