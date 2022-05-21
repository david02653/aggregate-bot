package soselab.msdobot.aggregatebot.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import soselab.msdobot.aggregatebot.Entity.Capability.*;
import soselab.msdobot.aggregatebot.Entity.Service.*;
import soselab.msdobot.aggregatebot.Entity.UpperIntent.UpperIntent;
import soselab.msdobot.aggregatebot.Entity.UpperIntent.UpperIntentList;
import soselab.msdobot.aggregatebot.Entity.Vocabulary.Context;
import soselab.msdobot.aggregatebot.Entity.Vocabulary.Vocabulary;
import soselab.msdobot.aggregatebot.Exception.IllegalConceptException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * load config files and handle simple config data control
 */
@org.springframework.stereotype.Service
public class ConfigLoader {

    private final YAMLFactory yamlFactory;
    private final ObjectMapper mapper;
    private YAMLParser parser;
    private final Gson gson;
    private final String serviceConfigPath;
    private final String capabilityConfigPath;
    private final String upperIntentConfigPath;
    private final String vocabularyConfigPath;

    public static ServiceList serviceList;
    public static ArrayList<Capability> capabilityList;
    public static UpperIntentList upperIntentList;
    //    public static ArrayList<UpperIntent> upperIntentList;
    public static Vocabulary vocabularyList;

    /**
     * load all config file and verify loaded config, vocabulary config must be loaded before service/capability/upperIntent config
     * @param env file path
     */
    @Autowired
    public ConfigLoader(Environment env){
        yamlFactory = new YAMLFactory();
        mapper = new ObjectMapper();
//        gson = new Gson();
        gson = new GsonBuilder().setPrettyPrinting().create();
        serviceConfigPath = env.getProperty("bot.config.service");
        capabilityConfigPath = env.getProperty("bot.config.capability");
        upperIntentConfigPath = env.getProperty("bot.config.upperIntent");
        vocabularyConfigPath = env.getProperty("bot.config.vocabulary");

        loadVocabularyConfig();
        verifyContextProperties();
        loadCapabilityConfig();
        verifyCapabilityConfig();
        loadUpperIntentConfig();
        verifyUpperIntent();
        loadServiceConfig();
        verifyServiceConfig();
    }

    /**
     * load capability config from config file
     */
    public void loadCapabilityConfig(){
        try{
            System.out.println("> try to load skill config from " + capabilityConfigPath);
            parser = yamlFactory.createParser(new File(capabilityConfigPath));
            capabilityList = mapper.readValue(parser, new TypeReference<ArrayList<Capability>>(){});
            System.out.println(">>> " + capabilityList);
            System.out.println("---");
        }catch (IOException ioe){
            ioe.printStackTrace();
            System.out.println("> [DEBUG] capability config file load failed.");
        }
    }

    /**
     * load upper intent config from config file
     */
    public void loadUpperIntentConfig(){
        try{
            System.out.println("> try to load upper intent config from " + upperIntentConfigPath);
            parser = yamlFactory.createParser(new File(upperIntentConfigPath));
            upperIntentList = mapper.readValue(parser, UpperIntentList.class);
            System.out.println(">>> " + upperIntentList);
        }catch (IOException ioe){
            ioe.printStackTrace();
            System.out.println("> [DEBUG] upper intent config file load failed.");
        }
        System.out.println("---");
    }

    /**
     * verify upper intent config, illegal upper intent will be ignored
     */
    private void verifyUpperIntent(){
        System.out.println("> start to verify upper intent config");
        Iterator<UpperIntent> intentIterator = upperIntentList.crossCapabilityList.iterator();
        while(intentIterator.hasNext()){
            UpperIntent currentIntent = intentIterator.next();
            System.out.println("[DEBUG] checking upper intent '" + currentIntent.name + "'");
            for(Capability step: currentIntent.sequencedCapabilityList){
                if(capabilityList.stream().noneMatch(capability -> capability.name.equals(step.name))){
                    System.out.println("[Error] Error code: U01");
                    System.out.println("[WARNING] capability '" + step.name + "' at order " + step.order + " from upper intent '" + currentIntent.name + "' is not available !");
                    System.out.println("[WARNING] system will ignore upperIntent '" + currentIntent.name + "' from now on.");
                    intentIterator.remove();
                    break;
                }
            }
        }
        System.out.println(">>> " + upperIntentList);
        System.out.println("---");
    }

    /**
     * load service config from config file
     */
    public void loadServiceConfig(){
        try{
            System.out.println("> try to load service config from " + serviceConfigPath);
            parser = yamlFactory.createParser(new File(serviceConfigPath));
            serviceList = mapper.readValue(parser, ServiceList.class);
            System.out.println(">>> " + serviceList);
            generateServiceMap();
            System.out.println("---");
        }catch (IOException ioe){
            ioe.printStackTrace();
            System.out.println("> [DEBUG] service config file load failed.");
        }
    }

    /**
     * load vocabulary config from config file
     */
    public void loadVocabularyConfig(){
        try{
            System.out.println("> try to load vocabulary config from " + vocabularyConfigPath);
            parser = yamlFactory.createParser(new File(vocabularyConfigPath));
            vocabularyList = mapper.readValue(parser,  Vocabulary.class);
            vocabularyList.createConceptHashMap();
            printVocabularyConfig();
            System.out.println("---");
        }catch (IOException ioe){
            ioe.printStackTrace();
            System.out.println("> [DEBUG] vocabulary config file load failed.");
        }
    }

    /**
     * print summary of loaded vocabulary config
     */
    public void printVocabularyConfig(){
        System.out.println(gson.toJson(vocabularyList));
    }

    /**
     * generate a service list in hashmap form<br>
     * this map could be used as a quick lookup table when checking service level
     */
    public void generateServiceMap(){
        System.out.println("[DEBUG] start to generate service map");
        HashMap<String, Service> tempServiceMap = new HashMap<>();
        for(ServiceSystem system: serviceList.serviceList){
            // system
            tempServiceMap.put(system.name, new Service(system.name, system.type, system.description, system.config));
            // sub service
            for(Service service: system.service){
                tempServiceMap.put(service.name, new Service(service.name, service.type, service.description, mergeConfig(system.config, service.config)));
            }
        }
        serviceList.setServiceMap(tempServiceMap);
        System.out.println(">>> " + gson.toJson(serviceList));
    }

    /**
     * merge system config and service config and return new instance of config array list
     * @param generalConfig system config
     * @param config service config
     * @return merged config
     */
    private ArrayList<ServiceConfig> mergeConfig(ArrayList<ServiceConfig> generalConfig, ArrayList<ServiceConfig> config){
        ArrayList<ServiceConfig> result;
        // todo: merge service config
        if(generalConfig == null){
            return Objects.requireNonNullElseGet(config, ArrayList::new);
        }else{
            if(config == null)
                return generalConfig;
            else{
                // merge config
                result = new ArrayList<>(generalConfig);
                // foreach new subService config
                for(ServiceConfig currentConfig: config){
                    // get previous context config if exist
                    if(result.stream().anyMatch(previous -> previous.context.equals(currentConfig.context))){
                        ServiceConfig temp = result.stream().filter(current -> current.context.equals(currentConfig.context)).findFirst().get();
                        for(ContextConfig property: currentConfig.properties){
                            if(temp.properties.stream().anyMatch(previous -> previous.name.equals(property.name))){
                                temp.properties.stream().filter(previous -> previous.name.equals(property.name)).findFirst().get().setValue(property.value);
                            }else{
                                temp.properties.add(property);
                            }
                        }
                        // update config
                        result.removeIf(previous -> previous.context.equals(currentConfig.context));
                        result.add(temp);
                    }else{
                        result.add(currentConfig);
                    }
                }
                return result;
            }
        }
    }

    /**
     * verify all listed vocabulary in capability specification file is legal
     */
    public void verifyCapabilityConfig(){
        System.out.println("> start to verify capability config");
        Iterator<Capability> capabilityIterator = capabilityList.iterator();
        while(capabilityIterator.hasNext()){
            Capability currentCapability = capabilityIterator.next();
            System.out.println("[DEBUG] checking capability '" + currentCapability.name + "'");
            if(currentCapability.isRenderingMethod){
                // todo: verify rendering method
                continue;
            }
            if(!currentCapability.isAggregateMethod){
                /* check context */
                String context = currentCapability.context;
                if (!vocabularyList.isAvailableContext(context)) {
                    System.out.println("[Error] Error code: C01");
                    System.out.println("[WARNING] context '" + context + "' found in capability '" + currentCapability.name + "' is illegal");
                    System.out.println("[WARNING] this capability will be ignored by system from now on.");
                    capabilityIterator.remove();
                    continue;
                }
                /* check used mapping */
                final ArrayList<String> legalMappingList;
                try {
                    if (currentCapability.usedMappingList != null)
                        legalMappingList = getLegalCustomMapping(currentCapability.usedMappingList);
                    else
                        legalMappingList = new ArrayList<>();
                } catch (IllegalConceptException ic) {
                    System.out.println("[WARNING] verification failed when processing custom mapping, this capability will be ignored from now on.");
                    capabilityIterator.remove();
                    continue;
                }
                /* check input */
                if (currentCapability.input.stream().anyMatch(input -> isPropertyIllegal(input, legalMappingList))) {
                    System.out.println("[WARNING] verification failed when processing input properties, this capability will be ignored from now on.");
                    capabilityIterator.remove();
                    continue;
                }
                /* check output */
                if (missingDataLabel(currentCapability.output)) {
                    System.out.println("[WARNING] verification failed when processing output config, this capability will be ignored from now on.");
                    capabilityIterator.remove();
                    continue;
                }
                final ArrayList<String> outputStoredDataLabelList;
                try {
                    outputStoredDataLabelList = getOutputStoredDataList(currentCapability.output);
                } catch (IllegalConceptException ic) {
                    System.out.println("[Error] Error code: C05");
                    System.out.println("[WARNING] verification failed when processing output config, this capability will be ignored from now on.");
                    capabilityIterator.remove();
                    continue;
                }
                /* check stored data */
                if (currentCapability.storedData != null) {
                    // input
                    if (!isStoredDataInputLegal(currentCapability.storedData.input, currentCapability.input)) {
                        System.out.println("[WARNING] verification failed when processing storedData input config, this capability will be ignored from now on.");
                        capabilityIterator.remove();
                        continue;
                    }
                    // output
                    if (!isStoredDataOutputLegal(currentCapability.storedData.output, outputStoredDataLabelList)) {
                        System.out.println("[WARNING] illegal output label found in storedData, this capability will be ignored from now on.");
                        capabilityIterator.remove();
                    }
                }
            } else {
                // verify aggregate capability
                AggregateDetail aggregateDetail = currentCapability.aggregateDetail;
                /* check if aggregate detail is even exist */
                if(aggregateDetail == null){
                    System.out.println("[Error] Error code: C16");
                    System.out.println("[WARNING] aggregate capability found but missing aggregate detail, this capability will be ignored from now on.");
                    capabilityIterator.remove();
                    continue;
                }
                /* check used material */
                if(aggregateDetail.storeResult){
                    if(aggregateDetail.usedComponent == null){
                        System.out.println("[Error] Error code: C17.");
                        System.out.println("[WARNING] missing stored data about aggregate result, this capability will be ignored from now on.");
                        capabilityIterator.remove();
                        continue;
                    }
                    if(isUsedMaterialIllegal(aggregateDetail.usedComponent, currentCapability.accessLevel)){
                        System.out.println("[WARNING] illegal config found in used material config, this capability will be ignored from now on.");
                        capabilityIterator.remove();
                        continue;
                    }
                }
                /* check aggregate capability data source */
                if(aggregateDetail.dataSource == null){
                    // check if aggregate data source do exist
                    System.out.println("[Error] Error code: C20");
                    System.out.println("[WARNING] no input data source found in aggregate capability '" + currentCapability.name + "', this capability will be ignored from now on.");
                    capabilityIterator.remove();
                    continue;
                }
                if(isAggregateDataSourceIllegal(currentCapability)){
                    System.out.println("[WARNING] error occurs when verifying aggregate data source, this capability will be ignored from now on");
                    capabilityIterator.remove();
                }
            }
        }
        System.out.println(">>> " + gson.toJson(capabilityList));
        System.out.println("---");
    }

    /**
     * check if used material in aggregate detail contains illegal config
     * @param usedMaterial used material in aggregate detail
     * @param accessLevel capability access level
     * @return true if any config goes wrong, otherwise return false
     */
    private boolean isUsedMaterialIllegal(AggregateDataComponent usedMaterial, String accessLevel){
        // check context in used material
        for(String context: usedMaterial.context){
            if(!vocabularyList.isAvailableContext(context)){
                System.out.println("[Error] Error code: C18");
                System.out.println("[WARNING] context '" + context + "' found in used material is not available.");
                return true;
            }
        }
        // check property in used material (is property exist and legal ?)
        for(String property: usedMaterial.property){
            if(isPropertyIllegal(property)){
                System.out.println("[Error] Error code: C19");
                System.out.println("[WARNING] property '" + property + "' found in used material is not available.");
                return true;
            }
        }
        return false;
    }

    /**
     * verify capability aggregate data source config
     * @param capability target capability
     * @return true if any aggregate data source is illegal, otherwise false
     */
    private boolean isAggregateDataSourceIllegal(Capability capability){
        AggregateDetail aggregateDetail = capability.aggregateDetail;
        for(AggregateSource dataSource: aggregateDetail.dataSource){
            if(dataSource.isAggregationData){
                AggregateDataComponent materials = dataSource.aggregateDataComponent;
                // check aggregate data input
                for(String context: materials.context){
                    if(!vocabularyList.isAvailableContext(context)){
                        System.out.println("[Error] Error code: C20");
                        System.out.println("[WARNING] assigned context '" + context + "' in aggregate data source is not available");
                        return true;
                    }
                }
                for(String property: materials.property){
                    if(isPropertyIllegal(property)){
                        System.out.println("[Error] Error code: C21");
                        System.out.println("[WARNING] assign property '" + property + "' in aggregate data source is not available");
                        return true;
                    }
                }
            }else{
                // check normal property input
                if(!vocabularyList.isAvailableContext(dataSource.context)){
                    System.out.println("[Error] Error code: C22");
                    System.out.println("[WARNING] context '" + dataSource.context + "' found in data source is illegal");
                    return true;
                }
                if(isPropertyIllegal(dataSource.from)){
                    System.out.println("[Error] Error code: C23");
                    System.out.println("[WARNING] property '" + dataSource.from + "' found in data source is illegal");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * check if stored data input config legal
     * @param dataLabelList input label of capability stored data
     * @param inputList capability input labels
     * @return true if all stored data input label are legal, otherwise return false
     */
    private boolean isStoredDataInputLegal(ArrayList<DataLabel> dataLabelList, ArrayList<String> inputList){
        if(dataLabelList == null)
            return true;
        for(DataLabel dataSet: dataLabelList){
            if(isPropertyIllegal(dataSet.to)) {
                System.out.println("[Error] Error code: C06");
                System.out.println("[WARNING] data destination '" + dataSet.to + "' found in storedData input is illegal.");
                return false;
            }
            if(!inputList.contains(dataSet.from)){
                System.out.println("[Error] Error code: C07");
                System.out.println("[WARNING] data source '" + dataSet.from + "' found in storedData input is illegal.");
                return false;
            }
        }
        return true;
    }

    /**
     * check if stored data output config legal
     * @param dataLabels output label of capability stored data
     * @param outputDataLabelList capability output labels
     * @return true if all stored data output label are legal, otherwise return false
     */
    private boolean isStoredDataOutputLegal(ArrayList<DataLabel> dataLabels, ArrayList<String> outputDataLabelList){
        if(dataLabels == null)
            return true;
        for(DataLabel dataSet: dataLabels){
            if(isPropertyIllegal(dataSet.to)){
                System.out.println("[Error] Error code: C08");
                System.out.println("[WARNING] data destination '" + dataSet.to + "' found in storedData output is illegal.");
                return false;
            }
            if(!outputDataLabelList.contains(dataSet.from)){
                System.out.println("[Error] Error code: C09");
                System.out.println("[WARNING] data source '" + dataSet.from + "' found in storedData output is illegal.");
                return false;
            }
        }
        return true;
    }

    /**
     * verify loaded service config
     */
    private void verifyServiceConfig(){
        System.out.println("> start to verify service config.");
        for(ServiceSystem system: serviceList.serviceList){
            System.out.println("[DEBUG] start checking system '" + system.name + "'");
            /* check system-wide config */
            // context
            if(system.config != null){
                if(system.config.removeIf(config -> !vocabularyList.isAvailableContext(config.context))) {
                    System.out.println("[Error] Error code: S01");
                    System.out.println("[WARNING] illegal context found in system '" + system.name + "'.");
                    System.out.println("[WARNING] this context config setting will be ignored by system from now on.");
                }
                // context property
                for(ServiceConfig serviceConfig: system.config){
                    System.out.println("[DEBUG] checking system config setting of context '" + serviceConfig.context + "'");
                    if(serviceConfig.properties.removeIf(config -> isPropertyIllegal(config.name))){
                        System.out.println("[Error] Error code: S02");
                        System.out.println("[WARNING] illegal property found in context '" + serviceConfig.context + "' config.");
                        System.out.println("[WARNING] illegal property config will be ignored by system from now on.");
                    }
                }
            }
            /* check service config */
            System.out.println("[DEBUG] start checking subService settings of system '" + system.name + "'");
            for(Service service: system.service){
                System.out.println("[DEBUG] start checking service '" + service.name + "'");
                // context
                if(service.config != null){
                    if(service.config.removeIf(config -> !vocabularyList.isAvailableContext(config.context))){
                        System.out.println("[Error] Error code: S03");
                        System.out.println("[WARNING] illegal context found in service '" + service.name + "'.");
                        System.out.println("[WARNING] this context config setting will be ignored by system from now on.");
                    }
                    // context property
                    for(ServiceConfig serviceConfig: service.config){
                        if(serviceConfig.properties.removeIf(config -> isPropertyIllegal(config.name))){
                            System.out.println("[Error] Error code: S04");
                            System.out.println("[WARNING] illegal property found in context '" + serviceConfig.context + "' config.");
                            System.out.println("[WARNING] illegal property config will be ignored by system from now on.");
                        }
                    }
                }
            }
        }
        System.out.println(">>> " + gson.toJson(serviceList));
    }

    /**
     * check if given custom mapping list contains any illegal property
     * @param mappingList given custom mapping list
     * @return legal mapping name list, otherwise empty list
     * @throws IllegalConceptException if any schema or property is illegal
     */
    public ArrayList<String> getLegalCustomMapping(ArrayList<CustomMapping> mappingList) throws IllegalConceptException {
        ArrayList<String> legalMappingList = new ArrayList<>();
        Pattern propertyPattern = Pattern.compile("%\\{([a-zA-Z0-9-/.]+)}");
        Matcher propertyMatcher;
        for(CustomMapping mapping: mappingList){
            // check schema
            if(!isValidJsonString(mapping.schema.replaceAll("%\\{[a-zA-Z0-9-/.]+}", "\"test\""))) {
                System.out.println("[Error] Error code: C02");
                System.out.println("[WARNING] given schema is not a legal json string.");
                throw new IllegalConceptException("illegal schema format");
            }
            propertyMatcher = propertyPattern.matcher(mapping.schema);
            while(propertyMatcher.find()){
                String property = propertyMatcher.group(1);
                // check extracted property
                if(isPropertyIllegal(property)) {
                    System.out.println("[Error] Error code: C03");
                    System.out.println("[WARNING] property '" + property + "' found in mapping '" + mapping.mappingName + "' is not a legal property.");
                    throw new IllegalConceptException(property + " is illegal.");
                }
            }
            legalMappingList.add(mapping.mappingName);
        }
        return legalMappingList;
    }

    /**
     * check if output config missing any data label<br>
     * check direct data label if output type is plainText<br>
     * check json info data label if output type is json<br>
     * return true if any label is missing
     * @param output target capability output
     * @return true if any data label is missing, otherwise return false
     */
    private boolean missingDataLabel(CapabilityOutput output) {
        if(output.type.equals("plainText")){
            // check data label
            if(output.dataLabel == null || output.dataLabel.isEmpty()) {
                System.out.println("[Error] Error code: C14");
                System.out.println("[WARNING] output type is 'plainText' but no data label found.");
                return true;
            }
        }else if(output.type.equals("json")){
            // check json info
            ArrayList<JsonInfo> infos = output.jsonInfo;
            for(JsonInfo info: infos){
                if(info.dataLabel == null || info.dataLabel.isEmpty()){
                    System.out.println("[Error code: C15]");
                    System.out.println("[WARNING] found json info with no data label.");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * get all dataLabel in output config
     * @param output output config
     * @return output stored data label list
     * @throws IllegalConceptException if illegal output type found
     */
    public ArrayList<String> getOutputStoredDataList(CapabilityOutput output) throws IllegalConceptException {
        // check output type
        if(!vocabularyList.getOutputConcept().contains(output.type))
            throw new IllegalConceptException("[WARNING] illegal output type detected.");
        ArrayList<String> storedDataLabelList = new ArrayList<>();
        if(output.dataLabel != null && !output.dataLabel.isEmpty())
            storedDataLabelList.add(output.dataLabel);
        // check json info
        if(output.jsonInfo != null)
            for(JsonInfo jsonInfo: output.jsonInfo)
                if(jsonInfo.dataLabel != null && !jsonInfo.dataLabel.isEmpty())
                    storedDataLabelList.add(jsonInfo.dataLabel);
        return storedDataLabelList;
    }

    /**
     * get correspond capabilities by target intent
     * @param intent target intent
     * @return correspond capability
     */
    public ArrayList<Capability> getCorrespondCapabilityByIntent(String intent){
        // check normal capability
        if(capabilityList.stream().anyMatch(capability -> capability.atomicIntent.equals(intent))) {
            return new ArrayList<>(List.of(capabilityList.stream().filter(capability -> capability.atomicIntent.equals(intent)).findFirst().get()));
        } else
            return new ArrayList<>();
    }

    /**
     * get complete capabilities list by target upper intent
     * @param upperIntent target upper intent
     * @return capability list
     */
    public ArrayList<Capability> getUpperIntentCapabilityListByIntent(String upperIntent){
        System.out.println("[DEBUG] start to check upper intent");
        System.out.println(new Gson().toJson(upperIntentList));
        ArrayList<Capability> semiCapabilityList = upperIntentList.getSemiCapabilityList(upperIntent);
        ArrayList<Capability> resultList = new ArrayList<>();
        for(Capability semiCapability: semiCapabilityList){
            resultList.add(capabilityList.stream().filter(capability -> capability.name.equals(semiCapability.name)).findFirst().get());
        }
        return resultList;
    }

    /**
     * verify all used properties in each context, each property is expected existing in assigned concept<br>
     * illegal property usage will be ignored
     */
    public void verifyContextProperties(){
        System.out.println("> start to verify properties used in context");
        for(Context context: vocabularyList.ContextList){
            System.out.println("[DEBUG] checking context '" + context.contextName + "'");
            Iterator<String> iterator = context.properties.iterator();
            while(iterator.hasNext()){
                String property = iterator.next();
                System.out.println("[DEBUG] checking property '" + property + "'");
                if(isPropertyIllegal(property)){
                    System.out.println("[Error] Error code: V01");
                    System.out.println("[WARNING] system will ignore this property from now on.");
                    iterator.remove();
                }
            }
        }
        System.out.println(">>> " + gson.toJson(vocabularyList.ContextList));
        vocabularyList.createContextHashMap();
        System.out.println("---");
    }

    /**
     * check if given property is available in assigned concept, concept name and property are expected to be separated by hyphen character<br>example: conceptA-propertyA
     * @param property input property
     * @return true if illegal, otherwise false
     */
    private boolean isPropertyIllegal(String property){
        if(!property.contains("."))
            return true;
        String[] token = property.split("\\.", 2);
        String conceptName = token[0];
        String value = token[1];
        return vocabularyList.isIllegalConceptProperty(conceptName, value);
    }

    /**
     * check if given property is available in assigned concept, concept name and property are expected to be separated by hyphen character<br>example: conceptA-propertyA<br>if given property does not match general property format (contains hyphen), check if available in given exception list
     * @param property input property
     * @param exceptionList exception property list
     * @return true if illegal, otherwise false
     */
    private boolean isPropertyIllegal(String property, ArrayList<String> exceptionList){
        if(property.contains(".")){
            String[] token = property.split("\\.", 2);
            String conceptName = token[0];
            String value = token[1];
            return vocabularyList.isIllegalConceptProperty(conceptName, value);
        }else {
            if (!exceptionList.contains(property)) {
                System.out.println("[Error] Error code: C04");
                System.out.println("[WARNING] property '" + property + "' does not exist in exception list.");
                return true;
            }
            return false;
        }
    }

    /**
     * verify if input json string is legal json format
     * @param raw json string
     * @return true if legal, otherwise false
     */
    private static boolean isValidJsonString(String raw){
        Gson gson = new Gson();
        try{
            gson.fromJson(raw, JsonObject.class);
        }catch (Exception e){
            try{
                gson.fromJson(raw, JsonArray.class);
            }catch (Exception ae){
                return false;
            }
        }
        return true;
    }
}
