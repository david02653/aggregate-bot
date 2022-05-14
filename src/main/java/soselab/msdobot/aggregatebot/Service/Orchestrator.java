package soselab.msdobot.aggregatebot.Service;

import com.google.gson.*;
import com.jayway.jsonpath.JsonPath;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import soselab.msdobot.aggregatebot.Entity.Capability.*;
import soselab.msdobot.aggregatebot.Entity.CapabilityReport;
import soselab.msdobot.aggregatebot.Entity.ContextConfigMap;
import soselab.msdobot.aggregatebot.Entity.RasaIntent;
import soselab.msdobot.aggregatebot.Entity.Service.Service;
import soselab.msdobot.aggregatebot.Exception.NoSessionFoundException;

import java.util.*;
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
    // missing config found: service - context - propertyKey
    public static ConcurrentHashMap<String, HashMap<String, HashSet<String>>> missingConfigMap;
    // previous aggregate result, use sorted server + sorted context + sorted property hash as key
    public static ConcurrentHashMap<String, String> aggregateDataMap;
    private final String expireTrigger;
    private final ConfigLoader configLoader;
    private final RenderingService renderingService;

    public Orchestrator(Environment env, ConfigLoader configLoader){
        contextSessionData = new ConcurrentHashMap<>();
        generalSessionData = new ConcurrentHashMap<>();
        missingConfigMap = new ConcurrentHashMap<>();
        aggregateDataMap = new ConcurrentHashMap<>();
        expireTrigger = env.getProperty("bot.session.expire.trigger");
        this.configLoader = configLoader;
        this.renderingService = new RenderingService();
    }

    /**
     * select and execute correspond capabilities<br>
     * return discord message if capabilities are successfully executed/ execution failed due to lacking of config
     * @param intent rasa intent
     * @return execute result message
     */
    public Message capabilitySelector(RasaIntent intent){
        HashMap<String, ArrayList<CapabilityReport>> finalReport = new HashMap<>();
        String intentName = intent.getIntent();
        String jobName = intent.getJobName();
        // check if try to expire session
        if(intentName.equals(expireTrigger)){
            expireAllSessionData();
            return RenderingService.createSimpleMessage("ok, all current session data removed.");
        }
        Gson gson = new Gson();
        final ExecutorService executor = Executors.newFixedThreadPool(5);
        final List<Future<CapabilityReport>> futures = new ArrayList<>();
        Future<CapabilityReport> future;

        /* get correspond capability by intent name */
        ArrayList<Capability> capabilityList = getCorrespondCapabilityList(intentName);
        if(capabilityList == null || capabilityList.isEmpty()) {
            System.out.println(">> [DEBUG] No available capability found.");
            return RenderingService.createSimpleMessage("No available capability found.");
        }
        System.out.println("[DEBUG] available capability detected : " + gson.toJson(capabilityList));

        /* get service list */
        // try to extract service name from general config if service name is not available
        if(jobName == null || jobName.isEmpty())
            jobName = generalSessionData.getOrDefault("Api.serviceName", "");
        ArrayList<Service> serviceList = ConfigLoader.serviceList.getSubServiceList(jobName);
        System.out.println("[DEBUG] todo subService list: " + gson.toJson(serviceList));
        if(serviceList.isEmpty()) {
            System.out.println("[DEBUG] target service not exist.");
            return RenderingService.createSimpleMessage("No correspond service found.");
//            return finalReport;
        }

        // default message, use this variable to store message if default aggregate and rendering triggered
        Message defaultMessage = new MessageBuilder().append("init orchestrator message.").build();
        /* execute sequenced capability list */
        for(Capability capability : capabilityList){
            // fire skill request for every sub-service
            if(capability.isAggregateMethod || capability.isRenderingMethod){
                // todo: handle aggregate and rendering capabilities
                if(capability.isAggregateMethod) {
                    future = executor.submit(() -> handleAggregateCapability(capability, serviceList));
                    futures.add(future);
                }else{
                    // todo: handle rendering capability, return result message
                    return handleRenderingCapability(capability, serviceList);
                }
            }else {
                /* POST method */
                if (capability.method.equals("POST")) {
                    for (Service service : serviceList) {
                        if (capability.accessLevel.equals(service.type)) {
                            System.out.println("[DEBUG] current subService " + gson.toJson(service));
                            future = executor.submit(() -> postRequestCapability(capability, service));
                            futures.add(future);
                        }
                    }
                } else {
                    /* GET method */
                    for (Service service : serviceList) {
                        System.out.println("[DEBUG] current subService " + gson.toJson(service));
                        if (capability.accessLevel.equals(service.type)) {
                            if (!hasPathVariable(capability.apiEndpoint))
                                future = executor.submit(() -> getRequestCapability(capability, service));
                            else
                                future = executor.submit(() -> getRequestCapabilityViaPathVariable(capability, service));
                            futures.add(future);
                        }
                    }
                }
            }
            // collect futures and check if every thread works fine
            try{
                System.out.println("[DEBUG][orchestrator result] future size: " + futures.size());
                CapabilityReport tempReport = new CapabilityReport();
                boolean reportFlag = false;
                for(Future<CapabilityReport> executeResult: futures){
                    // check if response map is empty
                    tempReport = executeResult.get();
                    System.out.println(">>> [check result]:");
                    System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(tempReport));
                    System.out.println("-----");
                    if(tempReport.hasError()) {
                        mergeMissingReport(finalReport, tempReport);
                        addMissingConfig(tempReport);
                        reportFlag = true;
                    }
                }
                // if any error report found, return current report
                if(reportFlag) {
                    // create and return missing report message from current capability report (may have multiple)
                    return renderingService.createMissingReportMessage(finalReport.get(capability.name));
                }
                // if last capability, check capability type, run default aggregation and rendering if no rendering capability assigned
                if(checkLastCapability(capabilityList, capability)){
                    // collect capability execute result
                    ArrayList<CapabilityReport> results = new ArrayList<>();
                    for(Future<CapabilityReport> report: futures){
                        results.add(report.get());
                    }
                    // default aggregation
                    JsonArray aggregateReport = AggregateService.normalAggregate(results);
                    // default rendering
                    RenderingService rendering = new RenderingService(serviceList, aggregateReport);
                    String resultTable = rendering.parseToSimpleAsciiArtTable();
                    // return default rendering result
                    defaultMessage = RenderingService.createSimpleMessage(resultTable);
                }
                futures.clear();
            }catch (InterruptedException | ExecutionException e){
                e.printStackTrace();
            }
        }
        return defaultMessage;
    }

    /**
     * check if given capability is the last element of capability list and whether it is a rendering capability or not
     * @param capabilityList capability list
     * @param currentCapability target capability
     * @return true if given capability is not the last one and is an aggregate capability, otherwise return false
     */
    private boolean checkLastCapability(ArrayList<Capability> capabilityList, Capability currentCapability){
        // check if current capability is the last capability
        if((capabilityList.size() -1) == capabilityList.indexOf(currentCapability)){
            // todo: change aggregate capability check to rendering capability check
//            return !currentCapability.isAggregateMethod;
            return !currentCapability.isRenderingMethod;
        }
        return true;
    }

//    private void checkCapabilityExecuteResult(HashMap<String, ArrayList<CapabilityReport>> report){
//        // todo: handle capability final report
//        // report contains error, return error message
//    }

    /**
     * add new missing config in previous report map
     * @param reportMap previous report map, use capability name as key, service report list as value
     * @param report new report
     */
    private void mergeMissingReport(HashMap<String, ArrayList<CapabilityReport>> reportMap, CapabilityReport report){
        if(reportMap.containsKey(report.capability)){
            // has previous service config
            ArrayList<CapabilityReport> reportList = reportMap.get(report.capability);
            if(reportList.stream().anyMatch(previous -> previous.service.equals(report.service))){
                // has previous missing info while executing same capability
                reportList.stream().filter(previous -> previous.service.equals(report.service)).findFirst().get().mergeProperty(report.missingContextProperty);
            }else{
                reportList.add(report);
            }
            reportMap.put(report.capability, reportList);
        }else{
            ArrayList<CapabilityReport> tempList = new ArrayList<>();
            tempList.add(report);
            reportMap.put(report.capability, tempList);
        }
    }

    /**
     * add new missing config from report
     * @param report
     */
    public void addMissingConfig(CapabilityReport report){
        if(report.missingContextProperty.size() <= 0) return;
        if(missingConfigMap.containsKey(report.service)){
            HashMap<String, HashSet<String>> missingContextMap = missingConfigMap.get(report.service);
            for(Map.Entry<String, HashSet<String>> reportContent: report.missingContextProperty.entrySet()){
                String contextName = reportContent.getKey();
                HashSet<String> properties = reportContent.getValue();
                if(missingContextMap.containsKey(contextName)){
                    // missing config has same service-context config set
                    HashSet<String> previousConfig = missingContextMap.get(contextName);
                    for(String property: properties){
                        if(!previousConfig.contains(property))
                            previousConfig.add(property);
                    }
                    missingContextMap.put(contextName, previousConfig);
                }else{
                    missingContextMap.put(contextName, properties);
                }
            }
            missingConfigMap.put(report.service, missingContextMap);
        }else{
            // service missing config not exist
            missingConfigMap.put(report.service, report.missingContextProperty);
        }
    }

    /**
     * remove missing config
     * @param service service name
     * @param context context name
     * @param propertyName config property name
     */
    public void removeMissingConfig(String service, String context, String propertyName){
        if(missingConfigMap.containsKey(service)){
            HashMap<String, HashSet<String>> missingContextProperties = missingConfigMap.get(service);
            if(missingContextProperties.containsKey(context)){
                HashSet<String> missingProperties = missingContextProperties.get(context);
                // remove property
                missingProperties.remove(propertyName);
                // remove current context if missing properties is empty
                if(missingProperties.isEmpty())
                    missingContextProperties.remove(context);
                // remove current service if context map is empty
                if(missingContextProperties.isEmpty())
                    missingConfigMap.remove(service);
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
        System.out.println("[DEBUG] start to search intent '" + intent + "'");
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
    }

    /**
     * retrieve required config and request aggregate endpoint<br>
     * note that used config in aggregate capability may cover multiple service/context/properties
     * @param capability
     * @param serviceList
     */
    public CapabilityReport handleAggregateCapability(Capability capability, ArrayList<Service> serviceList){
        CapabilityReport report = new CapabilityReport();
        report.setCapability(capability.name);
        /* collect required config */
        AggregateDetail aggregateDetail = capability.aggregateDetail;
        ArrayList<AggregateSource> dataSources = aggregateDetail.dataSource;
        // aggregateDataName - aggregateDataValue, use this to store no service-specific aggregate data
        HashMap<String, String> aggregateData = new HashMap<>();
        // aggregateDataName - serviceName - aggregateDataValue, use this to store service-specific aggregate data
        HashMap<String, HashMap<String, String>> specificAggregateData = new HashMap<>();
        // propertyName - serviceName - propertyValue
        HashMap<String, HashMap<String, String>> properties = new HashMap<>();
        // contextName - propertyName[]
        HashMap<String, HashSet<String>> missingPropertyMap = new HashMap<>();
        collectRequiredAggregateConfig(dataSources, serviceList, aggregateData, specificAggregateData, properties, missingPropertyMap);
        /* check if any required data is missing */
        if(missingPropertyMap.size() > 0){
            report.setMissingContextProperty(missingPropertyMap);
            System.out.println("[WARNING][handle aggregate] missing config");
            return report;
        }
        /* request endpoint */
        String requestMethod = capability.method;
        String requestEndpoint = capability.apiEndpoint;
        String rawAggregateReport = "";
        // todo: assume all aggregate capability only use POST method for now
        rawAggregateReport = postRequestEndpointWithDataSource(capability, aggregateData, specificAggregateData, properties);
        /* parse and store aggregate result */
        // use access level to determine data should be stored seperated or not
        // key: context-service-property, if multiple service involved, include all service name
        JsonArray aggregateReport = new Gson().fromJson(rawAggregateReport, JsonArray.class);
        AggregateDataComponent usedComponent = aggregateDetail.usedComponent;
        // todo: add storeResult check
        if(capability.accessLevel.equals("system")) {
            storeAggregateResult(usedComponent.context, usedComponent.property, collectServiceName(serviceList), extractSystemAggregateData(aggregateReport, aggregateDetail.resultName));
        }else{
            // store each service result
            for(Service service: serviceList){
                storeAggregateResult(usedComponent.context, usedComponent.property, new ArrayList<String>(Collections.singletonList(service.name)), extractServiceSpecificAggregateData(aggregateReport, service.name, aggregateDetail.resultName));
            }
        }
        // add aggregate data in capability execution report
        report.addResultFromAggregateReport(aggregateReport);
        return report;
    }

    /**
     * get system level aggregate data from aggregate report<br>
     * expect report like: {[targetDataName], [dataContent]}
     * @param aggregateReport json array aggregate report
     * @param targetDataName aggregate data key name
     * @return aggregate data content
     */
    private String extractSystemAggregateData(JsonArray aggregateReport, String targetDataName){
        var keyArray = aggregateReport.get(RenderingService.AGGREGATE_RESULT_KEY).getAsJsonArray();
        var valueArray = aggregateReport.get(RenderingService.AGGREGATE_RESULT_VALUE).getAsJsonArray();
        int index = 0;
        for(JsonElement key: keyArray){
            if(key.getAsString().equals(targetDataName))
                return valueArray.get(index).getAsString();
            index++;
        }
        return "";
    }

    /**
     * get service specific aggregate data from aggregate report<br>
     * expect report like: {[serviceA.targetDataName, serviceB.targetDataName], [contentA, contentB]}
     * @param aggregateReport json array aggregate report
     * @param serviceName target service name
     * @param targetDataName aggregate data key name
     * @return aggregate data content
     */
    private String extractServiceSpecificAggregateData(JsonArray aggregateReport, String serviceName, String targetDataName){
        var keyArray = aggregateReport.get(RenderingService.AGGREGATE_RESULT_KEY).getAsJsonArray();
        var valueArray = aggregateReport.get(RenderingService.AGGREGATE_RESULT_VALUE).getAsJsonArray();
        int index = 0;
        for(JsonElement key: keyArray){
            var keyName = key.getAsString();
            if(keyName.startsWith(serviceName) && keyName.contains(targetDataName))
                return valueArray.get(index).getAsString();
            index++;
        }
        return "";
    }

    /**
     * handle rendering capability and convert rendering result into discord message<br>
     * return missing properties message if any required resource is unavailable<br>
     * run default aggregate and rendering if given rendering result is illegal
     * @param capability target capability
     * @param serviceList applied service list
     * @return result discord message
     */
    private Message handleRenderingCapability(Capability capability, ArrayList<Service> serviceList){
        /* collect data from configured data source */
        RenderingDetail detail = capability.renderingDetail;
        ArrayList<AggregateSource> dataSource = detail.dataSource;
        HashMap<String, String> aggregateData = new HashMap<>();
        HashMap<String, HashMap<String, String>> specificAggregateData = new HashMap<>();
        HashMap<String, HashMap<String, String>> properties = new HashMap<>();
        HashMap<String, HashSet<String>> missingPropertyMap = new HashMap<>();
        collectRequiredAggregateConfig(dataSource, serviceList, aggregateData, specificAggregateData, properties, missingPropertyMap);
        /* return missing properties message if required resource is missing */
        if(missingPropertyMap.size() > 0){
            System.out.println("[WARNING][handle aggregate] missing config");
            return RenderingService.createMissingReportMessage(missingPropertyMap);
        }
        /* request rendering endpoint */
        String requestMethod = capability.method;
        String requestEndpoint = capability.apiEndpoint;
        String rawRenderingReport = "";
        // todo: assume all rendering capability use POST method for now
        rawRenderingReport = postRequestEndpointWithDataSource(capability, aggregateData, specificAggregateData, properties);
        /* run default aggregate and rendering if given rendering result is illegal */
        Message resultMessage = new MessageBuilder().append("init rendering result").build();
        if(RenderingService.templateFormatCheck(rawRenderingReport)){
            // convert rendering result
            var resultMessageTemplate = RenderingService.parseRenderingResult(rawRenderingReport);
            resultMessage = RenderingService.createDiscordMessage(resultMessageTemplate);
        }else{
            // run default aggregate and rendering
            var resultMessageTemplate = renderingService.defaultRendering(aggregateData, specificAggregateData, properties);
            resultMessage = RenderingService.createDiscordMessage(resultMessageTemplate);
        }
        // return rendering result
        return resultMessage;
    }

    /**
     * construct data source request body with required data sets<br>
     * result body should contain three main json object: aggregateData, specificAggregateData and properties<br>
     * aggregateData contains normal data pairs<br>
     * specificAggregateData and properties contains 'keyName - serviceName - value' data sets<br>
     * for example: {spec: {spec-1: {serviceA: a, serviceB: b}, spec-2: {serviceA: a2, serviceB: b2}}}
     * @param dataSources defined data source
     * @param aggregateData system aggregate data
     * @param specificAggregateData service specific aggregate data
     * @param properties service properties
     * @return request body
     */
    public JsonObject constructDataSourceBody(ArrayList<AggregateSource> dataSources, HashMap<String, String> aggregateData, HashMap<String, HashMap<String, String>> specificAggregateData, HashMap<String, HashMap<String, String>> properties){
        var resultBody = new JsonObject();
        var gson = new Gson();
        var aggregateObj = new JsonObject();
        var specAggregateObj = new JsonObject();
        var propObj = new JsonObject();
        // insert data into temp variable
        for(AggregateSource source: dataSources){
            if (source.isAggregationData){
                // system and service aggregate data
                if(source.aggregationLevel.equals("system")){
                    aggregateObj.addProperty(source.useAs, aggregateData.get(source.useAs));
                }else{
//                    JsonObject tempObj;
//                    if(specAggregateObj.has(source.useAs))
//                        tempObj = specAggregateObj.get(source.useAs).getAsJsonObject();
//                    else
//                        tempObj = new JsonObject();
//                    for (Map.Entry<String, String> specData: specificAggregateData.get(source.useAs).entrySet())
//                        tempObj.addProperty(specData.getKey(), specData.getValue());
//                    specAggregateObj.add(source.useAs, gson.toJsonTree(tempObj));
                    constructSpecDataJson(source, specificAggregateData, specAggregateObj);
                }
            }else{
//                JsonObject tempObj;
//                // properties
//                if(propObj.has(source.useAs))
//                    tempObj = propObj.get(source.useAs).getAsJsonObject();
//                else
//                    tempObj = new JsonObject();
//                // loop each service properties
//                for(Map.Entry<String, String> property: properties.get(source.useAs).entrySet())
//                    tempObj.addProperty(property.getKey(), property.getValue());
//                propObj.add(source.useAs, gson.toJsonTree(tempObj));
                constructSpecDataJson(source, properties, propObj);
            }
        }
        // insert each data in to result body
        resultBody.add("aggregate", gson.toJsonTree(aggregateObj));
        resultBody.add("specificAggregate", gson.toJsonTree(specAggregateObj));
        resultBody.add("properties", gson.toJsonTree(propObj));
        return resultBody;
    }

    /**
     * add data from specific aggregate data and service properties into result json object
     * @param source data mapping
     * @param resource resource data
     * @param result result jsonObject
     */
    private void constructSpecDataJson(AggregateSource source, HashMap<String, HashMap<String, String>> resource, JsonObject result){
        var gson = new Gson();
        JsonObject tempObj;
        if(result.has(source.useAs))
            tempObj = result.get(source.useAs).getAsJsonObject();
        else
            tempObj = new JsonObject();
        for (Map.Entry<String, String> specData: resource.get(source.useAs).entrySet())
            tempObj.addProperty(specData.getKey(), specData.getValue());
        result.add(source.useAs, gson.toJsonTree(tempObj));
    }

    /**
     * request endpoint with data collected from dataSource<br>
     * this method could be used to request aggregate and rendering endpoint
     * @param capability target capability
     * @param aggregateData data from aggregate result
     * @param specificAggregateData data from service-specific aggregate result
     * @param properties data from normal properties
     * @return request result
     */
    private String postRequestEndpointWithDataSource(Capability capability, HashMap<String, String> aggregateData, HashMap<String, HashMap<String, String>> specificAggregateData, HashMap<String, HashMap<String, String>> properties){
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonObject requestBody = new JsonObject();
//        Gson gson = new Gson();
//        for(AggregateSource dataSource: capability.aggregateDetail.dataSource){
//            if(dataSource.isAggregationData){
//                if(dataSource.aggregationLevel.equals("system")){
//                    requestBody.addProperty(dataSource.useAs, aggregateData.get(dataSource.useAs));
//                }else{
//                    requestBody.addProperty(dataSource.useAs, gson.toJson(specificAggregateData.get(dataSource.useAs)));
//                }
//            }else{
//                requestBody.addProperty(dataSource.useAs, gson.toJson(properties.get(dataSource.useAs)));
//            }
//        }
        requestBody = constructDataSourceBody(capability.aggregateDetail.dataSource, aggregateData, specificAggregateData, properties);
        System.out.println("[DEBUG][orchestrator][POST dataSource] requestBody: " + requestBody);
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        System.out.println("[DEBUG] try to request capability (dataSource) with body " + new Gson().toJson(requestBody));
        System.out.println("[DEBUG] try to request capability (dataSource) from " + capability.apiEndpoint);
        ResponseEntity<String> resp = restTemplate.exchange(capability.apiEndpoint, HttpMethod.POST, entity, String.class);
        return resp.getBody();
    }
    private void getRequestAggregateEndpoint(){
        // todo: get request
    }
    private void getRequestAggregateEndpointViaPathVariable(){
        // todo: get request with path variable
    }

    /**
     * collect all service name from service list
     * @param serviceList service list
     * @return service name list
     */
    private ArrayList<String> collectServiceName(ArrayList<Service> serviceList){
        ArrayList<String> serviceNameList = new ArrayList<>();
        for (Service service : serviceList) {
            serviceNameList.add(service.name);
        }
        return serviceNameList;
    }

    /**
     * add new property in aggregate normal properties map<br>
     * properties format:<br>
     * propertyName(useAs) - serviceName - propertyValue
     * @param serviceName
     * @param propertyName
     * @param propertyValue
     * @param properties result aggregate normal properties map
     */
    private void addAggregateNormalProperty(String serviceName, String propertyName, String propertyValue, HashMap<String, HashMap<String, String>> properties){
        HashMap<String, String> temp;
        if(properties.containsKey(propertyName)){
            temp = properties.get(propertyName);
        }else{
            temp = new HashMap<>();
        }
        temp.put(serviceName, propertyValue);
        properties.put(propertyName, temp);
    }

    /**
     * add new property in aggregate missing property map<br>
     * property format: context - propertyName[]
     * @param propertyContext
     * @param propertyName
     * @param missingPropertyMap result missing property map
     */
    private void addAggregateMissingProperty(String propertyContext, String propertyName, HashMap<String, HashSet<String>> missingPropertyMap){
        HashSet<String> missingProperties;
        if(missingPropertyMap.containsKey(propertyContext))
            missingProperties = missingPropertyMap.get(propertyContext);
        else
            missingProperties = new HashSet<>();
        missingProperties.add(propertyName);
        missingPropertyMap.put(propertyContext, missingProperties);
    }

    /**
     * collect used data from aggregate capability<br>
     * result data will be stored in given variable of aggregate data and properties
     * @param dataSources target aggregate capability data source
     * @param serviceList target service list
     * @param aggregateData result aggregate data set
     * @param specificAggregateData result specific aggregate data set
     * @param properties result property data set
     * @param missingPropertyMap missing properties
     */
    // todo: should consider capability access level
    private void collectRequiredAggregateConfig(ArrayList<AggregateSource> dataSources, ArrayList<Service> serviceList, HashMap<String, String> aggregateData, HashMap<String, HashMap<String, String>> specificAggregateData, HashMap<String, HashMap<String, String>> properties, HashMap<String, HashSet<String>> missingPropertyMap){
        ArrayList<String> serviceNameList = collectServiceName(serviceList);
        // todo: collect required config in aggregate capability
        for(AggregateSource source: dataSources){
            if(source.isAggregationData){
                // retrieve aggregate data
                try{
                    String aggregateResult;
                    if(source.aggregationLevel.equals("system")){
                        // system level aggregate data, use all service list to retrieve data
                        aggregateResult = retrieveAggregateData(source.aggregateDataComponent.context, source.aggregateDataComponent.property, serviceNameList);
                        aggregateData.put(source.useAs, aggregateResult);
                    }else{
                        // service level aggregate data, use each service to retrieve data
                        for(String serviceName: serviceNameList){
                            aggregateResult = retrieveAggregateData(source.aggregateDataComponent.context, source.aggregateDataComponent.property, new ArrayList<String>(Collections.singletonList(serviceName)));
                            addSpecificAggregateData(specificAggregateData, source.useAs, serviceName, aggregateResult);
                        }
                    }
                }catch (NoSessionFoundException e){
                    System.out.println("[WARNING][retrieve aggregate] aggregate data not found");
                    addAggregateMissingProperty("Aggregate", source.useAs, missingPropertyMap);
                }
            }else{
                // retrieve normal property
//                String currentServiceName;
                try {
                    // retrieve property from each service
                    for(Service service: serviceList){
//                        currentServiceName = service.name;
                        String property = retrieveConfig(service, source.context, source.from);
                        addAggregateNormalProperty(service.name, source.useAs, property, properties);
                    }
                }catch (NoSessionFoundException e){
                    System.out.println("[WARNING][retrieve aggregate] config not found");
                    addAggregateMissingProperty(source.context, source.from, missingPropertyMap);
                }
            }
        }
    }

    /**
     * add new property in specific aggregate data hash map<br>
     * result data format: propertyName - serviceName - propertyValue
     * @param specificAggregateData result specific aggregate data
     * @param propertyName target property name
     * @param serviceName applied service name
     * @param propertyValue target property value
     */
    private void addSpecificAggregateData(HashMap<String, HashMap<String, String>> specificAggregateData, String propertyName, String serviceName, String propertyValue){
        if(specificAggregateData.containsKey(propertyName)){
            var serviceMap = specificAggregateData.get(propertyName);
            serviceMap.put(serviceName, propertyValue);
            specificAggregateData.put(propertyName, serviceMap);
        }else{
            var tempMap = new HashMap<String, String>();
            tempMap.put(serviceName, propertyValue);
            specificAggregateData.put(propertyName, tempMap);
        }
    }

    /**
     * store new aggregate result in aggregate result hash map<br>
     * use sorted component as key
     * component order: service - context - propertyName
     * @param contextSet
     * @param propertySet
     * @param serviceList
     * @param aggregateResult
     */
    private void storeAggregateResult(HashSet<String> contextSet, HashSet<String> propertySet, ArrayList<String> serviceList, String aggregateResult){
        String dataKey = getAggregateResultKey(contextSet, propertySet, serviceList);
        aggregateDataMap.put(dataKey, aggregateResult);
    }

    /**
     * retrieve aggregate data with given aggregate component and applied service list
     * @param contextSet used context
     * @param propertySet used property
     * @param serviceList applied service
     * @return target aggregate result
     * @throws NoSessionFoundException if no aggregate result found
     */
    private String retrieveAggregateData(HashSet<String> contextSet, HashSet<String> propertySet, ArrayList<String> serviceList) throws NoSessionFoundException {
        String dataKey = getAggregateResultKey(contextSet, propertySet, serviceList);
        if(aggregateDataMap.containsKey(dataKey))
            return aggregateDataMap.get(dataKey);
        else
            throw new NoSessionFoundException("no aggregate result found");
    }

    /**
     * generate aggregate result key with given component<br>
     * sort all given service, context, property and concat every component with '.'
     * result component order: service - context - property
     * @param contextSet
     * @param propertySet
     * @param serviceList
     * @return
     */
    private String getAggregateResultKey(HashSet<String> contextSet, HashSet<String> propertySet, ArrayList<String> serviceList){
        StringBuilder fullContext = new StringBuilder();
        StringBuilder fullProperty = new StringBuilder();
        StringBuilder fullService = new StringBuilder();
        String fullKey = "";
        // sort context
        ArrayList<String> contextList = new ArrayList<>(contextSet);
        Collections.sort(contextList);
        for(String context: contextList) {
            if(contextList.indexOf(context) == 0)
                fullContext.append(context);
            else
                fullContext.append("+").append(context);
        }
        // sort property
        ArrayList<String> propertyList = new ArrayList<>(propertySet);
        Collections.sort(propertyList);
        for(String property: propertyList) {
            if(propertyList.indexOf(property) == 0)
                fullProperty.append(property);
            else
                fullProperty.append("+").append(property);
        }
        // sort service
        Collections.sort(serviceList);
        for(String service: serviceList) {
            if(serviceList.indexOf(service) == 0)
                fullService.append(service);
            else
                fullService.append("+").append(service);
        }
        // service - context - property
        fullKey += fullService.toString();
        fullKey += "/";
        fullKey += fullContext.toString();
        fullKey += "/";
        fullKey += fullProperty.toString();
        return fullKey;
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
            return report;
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
        CapabilityReport report = new CapabilityReport(capability.name, service.name);
        /* check stored data */
        if(storedData == null)
            return report;
        // check input stored data
        if(storedData.input != null) {
            for (DataLabel inputData : storedData.input) {
                addServiceSessionConfig(service.name, capabilityContext, inputData.to, inputConfig.get(inputData.from));
                if(inputData.addToGlobal)
                    addServiceSessionConfig(service.name, "general", inputData.to, inputConfig.get(inputData.from));
            }
        }
        // check output stored data
        if(storedData.output != null) {
            for (DataLabel outputData : storedData.output) {
                String outputType = capability.output.type;
                if (outputType.equals("plainText")) {
                    if (outputData.from.equals(capability.output.dataLabel))
                        addServiceSessionConfig(service.name, capabilityContext, outputData.to, output);
                    if(outputData.addToGlobal)
                        addServiceSessionConfig(service.name, "general", outputData.to, output);
                } else if (outputType.equals("json")) {
                    ArrayList<JsonInfo> jsonInfos = capability.output.jsonInfo;
                    JsonInfo targetInfo = jsonInfos.stream().filter(jsonInfo -> jsonInfo.dataLabel.equals(outputData.from)).findFirst().get();
                    String info = JsonPath.read(output, targetInfo.jsonPath).toString();
                    System.out.println(">>> [" + service.name + "]" + targetInfo.description + " : " + info);
                    addServiceSessionConfig(service.name, capabilityContext, outputData.to, info);
                    if(outputData.addToGlobal)
                        addServiceSessionConfig(service.name, "general", outputData.to, info);
                }
            }
        }
        /* collect execute output result */
        CapabilityOutput capabilityOutput = capability.output;
        if(capabilityOutput != null){
            if(capabilityOutput.type.equals("plainText")){
                report.addExecuteResult(capabilityOutput.dataLabel, output);
            }
            if(capabilityOutput.type.equals("json")){
                ArrayList<JsonInfo> outputInfo = capabilityOutput.jsonInfo;
                for(JsonInfo jsonInfo: outputInfo){
                    String info = JsonPath.read(output, jsonInfo.jsonPath).toString();
                    report.addExecuteResult(jsonInfo.dataLabel, info);
                }
            }
        }
        /* output message handle */
        System.out.println("[parse result] result map: " + gson.toJson(report));
        return report;
    }

    /**
     * expire all current service session config
     */
    private void expireAllSessionData(){
        Orchestrator.contextSessionData = new ConcurrentHashMap<>();
        Orchestrator.generalSessionData = new ConcurrentHashMap<>();
        Orchestrator.missingConfigMap = new ConcurrentHashMap<>();
        Orchestrator.aggregateDataMap = new ConcurrentHashMap<>();
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
