package soselab.msdobot.aggregatebot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import soselab.msdobot.aggregatebot.Entity.Capability.*;
import soselab.msdobot.aggregatebot.Entity.CapabilityReport;
import soselab.msdobot.aggregatebot.Entity.ContextConfigMap;
import soselab.msdobot.aggregatebot.Entity.RasaIntent;
import soselab.msdobot.aggregatebot.Entity.Service.Service;
import soselab.msdobot.aggregatebot.Exception.NoSessionFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * define which intent activate which agent/capability
 */
@org.springframework.stereotype.Service
public class Orchestrator {

    /**
     * config of each service
     */
    // service -> context -> propertyKey: propertyValue
    public static ConcurrentHashMap<String, ContextConfigMap> contextSessionData;
    // general session config
    public static ConcurrentHashMap<String, String> generalSessionData;
    private final String expireTrigger;
    private final ConfigLoader configLoader;

    public Orchestrator(Environment env, ConfigLoader configLoader){
        contextSessionData = new ConcurrentHashMap<>();
        generalSessionData = new ConcurrentHashMap<>();
        expireTrigger = env.getProperty("bot.session.expire.trigger");
        this.configLoader = configLoader;
    }

    public void capabilitySelector(RasaIntent intent){
        String intentName = intent.getIntent();
        String jobName = intent.getJobName();
        // check if try to expire session
        if(intentName.equals(expireTrigger)){
            expireAllSessionData();
            return;
        }
        Gson gson = new Gson();
        final ExecutorService executor = Executors.newFixedThreadPool(5);
        final List<Future<CapabilityReport>> futures = new ArrayList<>();
        Future<CapabilityReport> future;

        // get correspond capability by intent name
        ArrayList<Capability> capabilityList = getCorrespondCapabilityList(intentName);
        if(capabilityList == null || capabilityList.isEmpty()) {
            System.out.println(">> [DEBUG] No available skill found.");
            return;
        }
        System.out.println("[DEBUG] available skill detected : " + gson.toJson(capabilityList));

        // get service list
        // todo : maybe consider job name is also a session config
        ArrayList<Service> serviceList = ConfigLoader.serviceList.getSubServiceList(jobName);
        System.out.println("[DEBUG] todo subService list: " + gson.toJson(serviceList));
        if(serviceList.isEmpty()) {
            System.out.println("[DEBUG] target service not exist.");
            return;
        }

        // execute sequenced capability list
        for(Capability capability : capabilityList){
            // todo: add output data mapping
            // fire skill request for every sub-service
            // todo: any world changing capability ?
            if(capability.method.equals("POST")) {
                for (Service service : serviceList) {
                    if(capability.accessLevel.equals(service.type)) {
                        System.out.println("[DEBUG] current subService " + gson.toJson(service));
                        future = executor.submit(() -> postRequestCapability(capability, service));
                        futures.add(future);
                    }
                }
            }else{
                // get method
                for (Service service : serviceList) {
                    System.out.println("[DEBUG] current subService " + gson.toJson(service));
                    if(capability.accessLevel.equals(service.type)) {
                        if (!hasPathVariable(capability.apiEndpoint))
                            future = executor.submit(() -> getRequestCapability(capability, service));
                        else
                            future = executor.submit(() -> getRequestCapabilityViaPathVariable(capability, service));
                        futures.add(future);
                    }
                }
            }
            // collect futures and check if every thread works fine
            try{
                CapabilityReport result;
                for(Future<CapabilityReport> executeResult: futures){
                    // check if response map is empty
                    result = executeResult.get();
                    System.out.println(">>> [check result]:");
                    System.out.println(gson.toJson(result));
                    System.out.println("-----");
                }
                futures.clear();
            }catch (InterruptedException | ExecutionException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * retrieve config from session data<br>
     * if service config is not available or service context config has no correspond property, check general session config<br>
     * if no correspond property in general session config, throw exception
     * @param serviceName target service name
     * @param context target service context
     * @param propertyName target property
     * @return property value
     * @throws NoSessionFoundException if no session config and general session config available
     */
    public String retrieveSessionConfig(String serviceName, String context, String propertyName) throws NoSessionFoundException {
        System.out.println("[DEBUG] check session config '" + propertyName + "'");
        if(contextSessionData.containsKey(serviceName) && contextSessionData.get(serviceName).context.containsKey(context) && contextSessionData.get(serviceName).context.get(context).containsKey(propertyName)){
            return contextSessionData.get(serviceName).context.get(context).get(propertyName);
        }else{
            if(generalSessionData.containsKey(propertyName))
                return generalSessionData.get(propertyName);
            else{
                System.out.println("[DEBUG] no session config found");
                throw new NoSessionFoundException();
            }
        }
    }

    /**
     * retrieve config from service config or session config
     * @param service
     * @param propertyName
     * @return
     */
    public String retrieveConfig(Service service, String context, String propertyName) throws NoSessionFoundException {
        // todo: retrieve config
        System.out.println("[DEBUG] try to retrieve '" + propertyName + "' from context '" + context + "'");
        String serviceName = service.name;
        HashMap<String, HashMap<String, String>> serviceConfigMap = service.getConfigMap();
        System.out.println("[DEBUG] config map: " + new Gson().toJson(serviceConfigMap));
        if(serviceConfigMap.containsKey(context) && serviceConfigMap.get(context).containsKey(propertyName))
            return serviceConfigMap.get(context).get(propertyName);
        else
            return retrieveSessionConfig(serviceName, context, propertyName);
    }

    /**
     * retrieve custom mapping config
     * @param service target service
     * @param context capability context
     * @param mapping target custom mapping
     * @return missing config hashmap, contextName: property
     * @throws NoSessionFoundException if any property required in mapping schema is unable to retrieve
     */
    public HashMap<String, String> retrieveCustomMapConfig(Service service, String context, CustomMapping mapping) {
        HashMap<String, String> resultMap = new HashMap<>();
        Pattern propertyPattern = Pattern.compile("%\\{([a-zA-Z0-9-/.]+)}");
        String tempSchema = mapping.schema;
        Matcher matcher = propertyPattern.matcher(tempSchema);
        while(matcher.find()){
            String property = matcher.group(1);
            System.out.println("[DEBUG][mapping property detect] " + property);
            try{
                String propertyValue = retrieveConfig(service, context, property);
                System.out.println("[DEBUG][mapping process] property: " + property);
                System.out.println("[DEBUG][mapping process] value: " + propertyValue);
                resultMap.put(property, propertyValue);
                tempSchema = tempSchema.replaceAll("%\\{" + property + "}", "\"" + propertyValue + "\"");
            }catch (NoSessionFoundException ne){
                System.out.println("[DEBUG] retrieve custom failed");
                resultMap.put(property, null);
                System.out.println(resultMap.size());
            }
        }
        if(!resultMap.containsValue(null))
            resultMap.put(mapping.mappingName, tempSchema);
        System.out.println("[DEBUG][custom mapping] " + new Gson().toJson(resultMap));
        System.out.println(resultMap.size());
        System.out.println(resultMap.get("User.username"));
        System.out.println("---");
        return resultMap;
    }

    /**
     * retrieve multiple config, return null if config is not available
     * @param service target service
     * @param capability target capability
     * @return config query result
     */
    public HashMap<String, String> retrieveRequiredConfig(Service service, Capability capability) {
        // todo: retrieve capability used property config
        HashMap<String, String> resultMap = new HashMap<>();
        String context = capability.context;
        for(String property: capability.input){
            if(property.contains(".")){
                // concept property
                try{
                    String queryResult = retrieveConfig(service, context, property);
                    resultMap.put(property, queryResult);
                }catch (NoSessionFoundException ne){
                    resultMap.put(property, null);
                }
            }else{
                // custom mapping, need to check every property used in mapping schema
                CustomMapping targetMapping = capability.usedMappingList.stream().filter(mapping -> mapping.mappingName.equals(property)).findFirst().get();
                HashMap<String, String> customMapResult = retrieveCustomMapConfig(service, context, targetMapping);
                System.out.println("[DEBUG][retrieve require] size: " + customMapResult.size());
                System.out.println("[DEBUG] origin size: " + resultMap.size());
                resultMap.putAll(customMapResult);
                System.out.println("[DEBUG] after size: " + resultMap.size());
            }
        }
        return resultMap;
    }

    /**
     * get complete correspond capability list, check normal capability first, if received empty capability list, check upper intent
     * @param intent
     * @return
     */
    public ArrayList<Capability> getCorrespondCapabilityList(String intent){
        ArrayList<Capability> resultList = configLoader.getCorrespondCapabilityByIntent(intent);
        if(resultList.isEmpty()) {
            resultList = configLoader.getUpperIntentCapabilityListByIntent(intent);
        }
        return resultList;
    }

    /**
     * add or update properties in general/context session config
     * @param serviceName target service name
     * @param context target context name
     * @param propertyName new property name
     * @param propertyValue property value
     */
    public void addServiceSessionConfig(String serviceName, String context, String propertyName, String propertyValue){
        if(context.equals("general")){
            generalSessionData.put(propertyName, propertyValue);
        }else{
            ContextConfigMap tempSession;
            if(contextSessionData.containsKey(serviceName))
                tempSession = contextSessionData.get(serviceName);
            else
                tempSession = new ContextConfigMap();
            tempSession.addContextProperty(context, propertyName, propertyValue);
            contextSessionData.put(serviceName, tempSession);
        }
//        ContextConfigMap tempSession = null;
//        if(contextSessionData.containsKey(serviceName)){
//            // if target service already has previous record, retrieve previous data
//            tempSession = contextSessionData.get(serviceName);
//        }else{
//            // create new capability config
//            tempSession = new ContextConfigMap();
//        }
//        // store new config in capability config
//        tempSession.addContextProperty(propertyName, propertyValue);
//        contextSessionData.put(serviceName, tempSession);
    }

    /**
     * request given capability endpoint with service config, use session config or request for user input if necessary
     * @param capability target capability
     * @param service target service
     * @return parsed request response
     */
    public CapabilityReport postRequestCapability(Capability capability, Service service){
        // get config from service
        // HashMap< contextName, HashMap< propertyName, propertyValue >>
        HashMap<String, HashMap<String, String>> serviceConfigMap = service.getConfigMap();
        String capabilityContext = capability.context;
        System.out.println("[DEBUG] config map: " + new Gson().toJson(serviceConfigMap));
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonObject requestBody = new JsonObject();
        HashMap<String, String> requiredConfig = retrieveRequiredConfig(service, capability);
        System.out.println("### check retrieved config ###");
        System.out.println(new Gson().toJson(requiredConfig));
        System.out.println("[size] " + requiredConfig.size());
        System.out.println("### end retrieved data check ###");
        if(requiredConfig.containsValue(null)){
            // missing input property, return error
            CapabilityReport report = new CapabilityReport(capability.name, service.name);
            for(java.util.Map.Entry<String, String> config: requiredConfig.entrySet()){
                String key = config.getKey();
                String value = config.getValue();
                if(value == null){
                    System.out.println("[DEBUG][check result null] " + key);
                    report.addProperty(capabilityContext, key);
                }
            }
//            requiredConfig.forEach((property, propertyValue) -> {
//                if(propertyValue == null){
//                    reportMap.addContextProperty(service.name, capabilityContext, property);
//                }
//            });
            System.out.println("[report] " + new Gson().toJson(report));
            return report;
        }
        // build request body
        for(String input: capability.input){
            requestBody.addProperty(input, requiredConfig.get(input));
        }
        System.out.println("[DEBUG][orchestrator][POST] requestBody: " + requestBody);
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        System.out.println("[DEBUG] try to request capability with body " + new Gson().toJson(requestBody));
        System.out.println("[DEBUG] try to request capability from " + capability.apiEndpoint);
        ResponseEntity<String> resp = restTemplate.exchange(capability.apiEndpoint, HttpMethod.POST, entity, String.class);
        System.out.println(service.name + " " + resp.getBody());
        return parseRequestResult(capability, service, requiredConfig, resp.getBody());
    }

    // todo: refactor
    private void gatherRequestPropertiesConfig(JsonObject requestConfig, Capability capability, String serviceName, HashMap<String, HashMap<String, String>> serviceConfigMap, String propertyKey){
        String capabilityContext = capability.context;
        ContextConfigMap serviceSessionConfig = contextSessionData.get(serviceName);
        if(propertyKey.contains(".")){
            /* concept property */
            // check service's context config first, if not available, check service's general config
            if(serviceConfigMap.containsKey(capabilityContext) && serviceConfigMap.get(capabilityContext).containsKey(propertyKey))
                requestConfig.addProperty(propertyKey, serviceConfigMap.get(capabilityContext).get(propertyKey));
            else if(serviceConfigMap.containsKey("general") && serviceConfigMap.get("general").containsKey(propertyKey))
                requestConfig.addProperty(propertyKey, serviceConfigMap.get("general").get(propertyKey));
            else{
                try{
                    requestConfig.addProperty(propertyKey, retrieveSessionConfig(serviceName, capabilityContext, propertyKey));
                }catch (NoSessionFoundException e){
                    System.out.println("[DEBUG] session config retrieve failed. no session available.");
                }
            }
        }else{
            /* custom map */
            String mapSchema = capability.usedMappingList.stream().filter(customMapping -> customMapping.mappingName.equals(propertyKey)).findFirst().get().schema;
            String mapSchemaTemp = new String(mapSchema);
            Pattern propertyPattern = Pattern.compile("%\\{([a-zA-Z0-9-/.]+)}");
            Matcher propertyMatcher = propertyPattern.matcher(mapSchemaTemp);
            while(propertyMatcher.find()){
                String property = propertyMatcher.group(1);
                if(serviceConfigMap.containsKey(capabilityContext) && serviceConfigMap.get(capabilityContext).containsKey(property))
                    mapSchemaTemp = mapSchemaTemp.replaceAll("%\\{" + property + "}", "\"" + serviceConfigMap.get(capabilityContext).get(property) + "\"");
                else if(serviceConfigMap.containsKey("general") && serviceConfigMap.get("general").containsKey(property))
                    mapSchemaTemp = mapSchemaTemp.replaceAll("%\\{" + property + "}", "\"" + serviceConfigMap.get("general") + "\"");
                else{
                    try{
                        mapSchemaTemp = mapSchemaTemp.replaceAll("%\\{" + property + "}", "\"" + retrieveSessionConfig(serviceName, capabilityContext, property) + "\"");
                    }catch (NoSessionFoundException ne){
                        System.out.println("[DEBUG] session config retrieve failed. no session available.");
                    }
                }
            }
            requestConfig.addProperty(propertyKey, mapSchemaTemp);
        }
    }

//     // todo: currently broken due to data structure changing, fix it
//     /**
//     * fill up config slot in custom mapping schema and return result
//     * @param mapName
//     * @param serviceConfigMap
//     * @param sessionConfig
//     * @return
//     */
//    public String generateCustomMappingConfig(String mapName, HashMap<String, String> serviceConfigMap, ContextConfigMap sessionConfig){
//        CustomMapping mapping = ConfigLoader.vocabularyList.customMappingHashMap.get(mapName);
//        String mappingSchema = mapping.schema;
//        Pattern vocabularyPattern = Pattern.compile("%\\{([a-zA-Z0-9-/.]+)}");
//        Matcher vocabularyMatcher = vocabularyPattern.matcher(mappingSchema);
//        while(vocabularyMatcher.find()){
//            String vocabulary = vocabularyMatcher.group(1);
//            if(serviceConfigMap.containsKey(vocabulary))
//                mappingSchema = mappingSchema.replaceAll("%\\{" + vocabulary + "}", "\"" + serviceConfigMap.get(vocabulary) + "\"");
//            else
//                mappingSchema = mappingSchema.replaceAll("%\\{" + vocabulary + "}", "\"" + sessionConfig.context.get(vocabulary) + "\"");
//        }
//        return mappingSchema;
//    }

    /**
     * fix format of input parameter by removing concept prefix<br>
     * example:<br>
     * 'User.username' to 'User-username'
     * @param raw original parameter
     * @return fixed parameter
     */
    private String formatParameter(String raw){
        return raw.replace(".", "-");
    }

    public CapabilityReport getRequestCapability(Capability capability, Service service){
        String capabilityContext = capability.context;
        StringBuilder requestUrl = new StringBuilder(capability.apiEndpoint);
        HashMap<String, String> requiredConfig = retrieveRequiredConfig(service, capability);
        if(requiredConfig.containsValue(null)){
            /* missing config */
            CapabilityReport report = new CapabilityReport(capability.name, service.name);
            requiredConfig.forEach((property, propertyValue) -> {
                report.addProperty(capabilityContext, property);
            });
            return report;
        }
        if(!capability.input.isEmpty()){
            requestUrl.append("?");
            for(String input: capability.input){
                String config = requiredConfig.get(input);
                requestUrl.append(formatParameter(input)).append("=").append(config).append("&");
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
        return parseRequestResult(capability, service, requiredConfig, resp.getBody());
    }

    /**
     * request get method capability with path variable
     * @param capability
     * @param service
     */
    public CapabilityReport getRequestCapabilityViaPathVariable(Capability capability, Service service){
        String variablePattern;
        String requestUrl = capability.apiEndpoint;
        HashMap<String, String> requiredConfig = retrieveRequiredConfig(service, capability);
        if(requiredConfig.containsValue(null)){
            /* missing config */
            CapabilityReport report = new CapabilityReport(capability.name, service.name);
            requiredConfig.forEach((property, propertyValue) -> {
                report.addProperty(capability.context, property);
            });
        }
        if(!capability.input.isEmpty()){
            for(String input: capability.input){
                variablePattern = "\\{" + input + "}";
                requestUrl = requestUrl.replaceAll(variablePattern, requiredConfig.get(input));
//                if(previousConfig != null && previousConfig.context.containsKey(input))
//                    requestUrl = requestUrl.replaceAll(variablePattern, previousConfig.context.get(input));
//                else
//                    requestUrl = requestUrl.replaceAll(variablePattern, configMap.get(input));
            }
        }
        System.out.println("[DEBUG] request url: " + requestUrl);
        RestTemplate template = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = template.exchange(requestUrl, HttpMethod.GET, entity, String.class);
        System.out.println(resp.getBody());
        return parseRequestResult(capability, service, requiredConfig, resp.getBody());
    }

    /**
     * check if url contains '{}', if contains then return true
     * @param url capability url
     * @return true if capability url contains '{}'
     */
    public boolean hasPathVariable(String url){
        return url.contains("{") && url.contains("}");
    }

    // todo: broken session data storage due to data structure change, fix it
    /**
     * parse output result, if output type is json, try to extract info by given json path, if output type is text and has tag, return a hash map data pair with tag as key and data as value
     * @return
     */
    public CapabilityReport parseRequestResult(Capability capability, Service service, HashMap<String, String> inputConfig, String output){
        Gson gson = new Gson();
        System.out.println("[parse result] capability > " + gson.toJson(capability));
        System.out.println("[parse result] service > " + gson.toJson(service));
        System.out.println("[parse result] output > " + output);
        String capabilityContext = capability.context;
        StoredData storedData = capability.storedData;
        HashMap<String, HashMap<String, String>> serviceConfigMap = service.getConfigMap();
        CapabilityReport report = new CapabilityReport();
        // check input stored data
        for(DataLabel inputData: storedData.input){
            addServiceSessionConfig(service.name, capabilityContext, inputData.to, inputConfig.get(inputData.from));
        }
        // check output stored data
        for(DataLabel outputData: storedData.output){
            String outputType = capability.output.type;
            if (outputType.equals("plainText")) {
                if (outputData.from.equals(capability.output.dataLabel))
                    addServiceSessionConfig(service.name, capabilityContext, outputData.to, output);
            } else if (outputType.equals("json")) {
                ArrayList<JsonInfo> jsonInfos = capability.output.jsonInfo;
                JsonInfo targetInfo = jsonInfos.stream().filter(jsonInfo -> jsonInfo.dataLabel.equals(outputData.from)).findFirst().get();
                String info = JsonPath.read(output, targetInfo.jsonPath).toString();
                System.out.println(">>> [" + service.name + "]" + targetInfo.description + " : " + info);
                addServiceSessionConfig(service.name, capabilityContext, outputData.to, info);
            }
        }
        System.out.println("[parse result] result map: " + gson.toJson(report));
        return report;
    }

    /**
     * expire all current service session config
     */
    private void expireAllSessionData(){
        Orchestrator.contextSessionData = new ConcurrentHashMap<>();
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
