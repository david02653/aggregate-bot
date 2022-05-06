package soselab.msdobot.aggregatebot.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
    public static ConcurrentHashMap<Integer, String> aggregateDataMap;
    private final String expireTrigger;
    private final ConfigLoader configLoader;

    public Orchestrator(Environment env, ConfigLoader configLoader){
        contextSessionData = new ConcurrentHashMap<>();
        generalSessionData = new ConcurrentHashMap<>();
        missingConfigMap = new ConcurrentHashMap<>();
        aggregateDataMap = new ConcurrentHashMap<>();
        expireTrigger = env.getProperty("bot.session.expire.trigger");
        this.configLoader = configLoader;
    }

    public void capabilitySelector(RasaIntent intent){
//        HashMap<String, CapabilityReport> finalReport = new HashMap<>();
        HashMap<String, ArrayList<CapabilityReport>> finalReport = new HashMap<>();
        String intentName = intent.getIntent();
        String jobName = intent.getJobName();
        // check if try to expire session
        if(intentName.equals(expireTrigger)){
            expireAllSessionData();
            checkCapabilityExecuteResult(finalReport);
            return;
//            return finalReport;
        }
        Gson gson = new Gson();
        final ExecutorService executor = Executors.newFixedThreadPool(5);
        final List<Future<CapabilityReport>> futures = new ArrayList<>();
        Future<CapabilityReport> future;

        // get correspond capability by intent name
        ArrayList<Capability> capabilityList = getCorrespondCapabilityList(intentName);
        if(capabilityList == null || capabilityList.isEmpty()) {
            System.out.println(">> [DEBUG] No available skill found.");
            checkCapabilityExecuteResult(finalReport);
            return;
//            return finalReport;
        }
        System.out.println("[DEBUG] available skill detected : " + gson.toJson(capabilityList));

        // get service list
        // try to extract service name from general config if service name is not available
        if(jobName == null || jobName.isEmpty())
            jobName = generalSessionData.getOrDefault("Api.serviceName", "");
        ArrayList<Service> serviceList = ConfigLoader.serviceList.getSubServiceList(jobName);
        System.out.println("[DEBUG] todo subService list: " + gson.toJson(serviceList));
        if(serviceList.isEmpty()) {
            System.out.println("[DEBUG] target service not exist.");
            checkCapabilityExecuteResult(finalReport);
            return;
//            return finalReport;
        }

        boolean singleTurn = false;
        singleTurn = capabilityList.size() == 1;
        /* execute sequenced capability list */
        for(Capability capability : capabilityList){
            // fire skill request for every sub-service
            if(capability.isAggregateMethod || capability.isRenderingMethod){
                // todo: handle aggregate and rendering capabilities
                future = executor.submit(() -> handleAggregateCapability(capability, serviceList));
                futures.add(future);
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
                    checkCapabilityExecuteResult(finalReport);
                    return;
//                    return finalReport;
                }
                // if selected capabilities only contain single capability, run default aggregate and rendering
                if(singleTurn || checkLastCapability(capabilityList, capability)){
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
                }
                futures.clear();
            }catch (InterruptedException | ExecutionException e){
                e.printStackTrace();
            }
        }
        checkCapabilityExecuteResult(finalReport);
//        return finalReport;
    }

    /**
     * check if given capability is the last element of capability list and whether it is an aggregate capability or not
     * @param capabilityList capability list
     * @param currentCapability target capability
     * @return true if given capability is the last one and is an aggregate capability, otherwise return false
     */
    private boolean checkLastCapability(ArrayList<Capability> capabilityList, Capability currentCapability){
        // check if current capability is the last capability
        if((capabilityList.size() -1) == capabilityList.indexOf(currentCapability)){
            // todo: change aggregate capability check to rendering capability check
            return !currentCapability.isAggregateMethod;
        }
        return false;
    }

    private void checkCapabilityExecuteResult(HashMap<String, ArrayList<CapabilityReport>> report){
        // todo: handle capability final report
        // report contains error, return error message
    }

    /**
     * add new missing config in previous report map<br>
     * @param reportMap previous report map, use capability name as key, service report list as value
     * @param report new report
     */
    private void mergeMissingReport(HashMap<String, ArrayList<CapabilityReport>> reportMap, CapabilityReport report){
        // todo: merge report
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
        // collect required config
        AggregateDetail aggregateDetail = capability.aggregateDetail;
        ArrayList<AggregateSource> dataSources = aggregateDetail.dataSource;
        HashMap<String, String> aggregateData = new HashMap<>();
        HashMap<String, HashMap<String, String>> properties = new HashMap<>();
        HashMap<String, HashSet<String>> missingPropertyMap = new HashMap<>();
        collectRequiredAggregateConfig(dataSources, serviceList, aggregateData, properties, missingPropertyMap);
        // check if any required data is missing
        if(missingPropertyMap.size() > 0){
            report.setMissingContextProperty(missingPropertyMap);
            System.out.println("[WARNING][handle aggregate] missing config");
            return report;
        }
        // todo: request aggregate endpoint
        String requestMethod = capability.method;
        String requestEndpoint = capability.apiEndpoint;
        String rawAggregateReport = "";
        if(requestMethod.equals("POST"))
            rawAggregateReport = postRequestAggregateEndpoint(capability, aggregateData, properties);
        else {
            // todo: complete get method request to aggregate endpoint
            if(!hasPathVariable(requestEndpoint))
                getRequestAggregateEndpoint();
            else
                getRequestAggregateEndpointViaPathVariable();
        }
        // todo: parse and store aggregate result
        JsonArray aggregateReport = new Gson().fromJson(rawAggregateReport, JsonArray.class);
        AggregateDataMaterial usedMaterial = aggregateDetail.usedMaterial;
        storeAggregateResult(usedMaterial.context, usedMaterial.property, collectServiceName(serviceList), rawAggregateReport);
        report.addResultFromAggregateReport(aggregateReport);
        return report;
    }

    private String postRequestAggregateEndpoint(Capability capability, HashMap<String, String> aggregateData, HashMap<String, HashMap<String, String>> properties){
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonObject requestBody = new JsonObject();
        Gson gson = new Gson();
        for(AggregateSource dataSource: capability.aggregateDetail.dataSource){
            if(dataSource.isAggregationData){
                requestBody.addProperty(dataSource.useAs, aggregateData.get(dataSource.useAs));
            }else{
                requestBody.addProperty(dataSource.useAs, gson.toJson(properties.get(dataSource.useAs)));
            }
        }
        System.out.println("[DEBUG][orchestrator][POST aggregate] requestBody: " + requestBody);
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        System.out.println("[DEBUG] try to request aggregate capability with body " + new Gson().toJson(requestBody));
        System.out.println("[DEBUG] try to request aggregate capability from " + capability.apiEndpoint);
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
     * collect used data from aggregate capability
     * @param dataSources target aggregate capability data source
     * @param serviceList target service list
     * @param aggregateData result aggregate data set
     * @param properties result property data set
     * @param missingPropertyMap missing properties
     */
    private void collectRequiredAggregateConfig(ArrayList<AggregateSource> dataSources, ArrayList<Service> serviceList, HashMap<String, String> aggregateData, HashMap<String, HashMap<String, String>> properties, HashMap<String, HashSet<String>> missingPropertyMap){
        ArrayList<String> serviceNameList = collectServiceName(serviceList);
        // todo: collect required config in aggregate capability
        for(AggregateSource source: dataSources){
            if(source.isAggregationData){
                // todo: retrieve aggregate data
                try{
                    String aggregateResult = retrieveAggregateData(source.aggregateDataMaterial.context, source.aggregateDataMaterial.property, serviceNameList);
                    aggregateData.put(source.useAs, aggregateResult);
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
     * store new aggregate result in aggregate result hash map<br>
     * use sorted material hashcode as key
     * @param contextSet
     * @param propertySet
     * @param serviceList
     * @param aggregateResult
     */
    private void storeAggregateResult(HashSet<String> contextSet, HashSet<String> propertySet, ArrayList<String> serviceList, String aggregateResult){
        // todo: check aggregate result could belongs to property map
        int materialHash = getAggregateResultKey(contextSet, propertySet, serviceList);
        aggregateDataMap.put(materialHash, aggregateResult);
    }

    /**
     * retrieve aggregate data with given aggregate material
     * @param contextSet
     * @param propertySet
     * @param serviceList
     * @return target aggregate result
     * @throws NoSessionFoundException if no aggregate result found
     */
    private String retrieveAggregateData(HashSet<String> contextSet, HashSet<String> propertySet, ArrayList<String> serviceList) throws NoSessionFoundException {
        int materialHash = getAggregateResultKey(contextSet, propertySet, serviceList);
        if(aggregateDataMap.containsKey(materialHash))
            return aggregateDataMap.get(materialHash);
        else
            throw new NoSessionFoundException("no aggregate result found");
    }

    /**
     * generate aggregate result key with given material<br>
     * sort all given service, context, property and concat every material before hashcode
     * @param contextSet
     * @param propertySet
     * @param serviceList
     * @return sorted and concatenated material hash code
     */
    private int getAggregateResultKey(HashSet<String> contextSet, HashSet<String> propertySet, ArrayList<String> serviceList){
        StringBuilder fullContext = new StringBuilder();
        StringBuilder fullProperty = new StringBuilder();
        StringBuilder fullService = new StringBuilder();
        String fullMaterial = "";
        // sort context
        ArrayList<String> contextList = new ArrayList<>(contextSet);
        Collections.sort(contextList);
        for(String context: contextList)
            fullContext.append(context);
        // sort property
        ArrayList<String> propertyList = new ArrayList<>(propertySet);
        Collections.sort(propertyList);
        for(String property: propertyList)
            fullProperty.append(property);
        // sort service
        Collections.sort(serviceList);
        for(String service: serviceList)
            fullService.append(service);
        fullMaterial += fullService.toString();
        fullMaterial += fullContext.toString();
        fullMaterial += fullProperty.toString();
        return fullMaterial.hashCode();
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
