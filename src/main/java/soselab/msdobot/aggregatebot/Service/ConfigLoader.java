package soselab.msdobot.aggregatebot.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import soselab.msdobot.aggregatebot.Entity.Agent.AgentList;
import soselab.msdobot.aggregatebot.Entity.UpperIntent.UpperIntent;
import soselab.msdobot.aggregatebot.Entity.UpperIntent.UpperIntentList;
import soselab.msdobot.aggregatebot.Entity.Vocabulary.Concept;
import soselab.msdobot.aggregatebot.Entity.Vocabulary.CustomMapping;
import soselab.msdobot.aggregatebot.Entity.Vocabulary.Vocabulary;
import soselab.msdobot.aggregatebot.Entity.Service.ServiceList;
import soselab.msdobot.aggregatebot.Entity.Service.ServiceSystem;
import soselab.msdobot.aggregatebot.Entity.Service.SubService;
import soselab.msdobot.aggregatebot.Entity.Capability.Capability;
import soselab.msdobot.aggregatebot.Entity.Capability.CapabilityList;
import soselab.msdobot.aggregatebot.Exception.IllegalConceptException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * load config files
 */
@Service
public class ConfigLoader {

    private final YAMLFactory yamlFactory;
    private final ObjectMapper mapper;
    private YAMLParser parser;
    private final Gson gson;
    private final String agentConfigPath;
    private final String serviceConfigPath;
    private final String capabilityConfigPath;
    private final String upperIntentConfigPath;
    private final String vocabularyConfigPath;

    public static AgentList agentList;
    public static ServiceList serviceList;
    public static CapabilityList capabilityList;
    public static UpperIntentList upperIntentList;
    public static Vocabulary vocabularyList;

    /**
     * load all config file and verify loaded config, vocabulary config must be loaded before service/capability/upperIntent config
     * @param env file path
     */
    @Autowired
    public ConfigLoader(Environment env){
        yamlFactory = new YAMLFactory();
        mapper = new ObjectMapper();
        gson = new Gson();
        agentConfigPath = env.getProperty("bot.config.agent");
        serviceConfigPath = env.getProperty("bot.config.service");
        capabilityConfigPath = env.getProperty("bot.config.capability");
        upperIntentConfigPath = env.getProperty("bot.config.upperIntent");
        vocabularyConfigPath = env.getProperty("bot.config.vocabulary");

        loadVocabularyConfig();
        verifyConceptVocabulary();
        verifyCustomMappingVocabulary();
        loadAgentConfig();
        loadCapabilityConfig();
        verifyCapabilityInputVocabulary();
        loadUpperIntentConfig();
        verifyUpperIntent();
        loadServiceConfig();
    }

    /**
     * load agent config from config file
     */
    public void loadAgentConfig(){
        try {
            System.out.println("> try to load agent config from " + agentConfigPath);
            parser = yamlFactory.createParser(new File(agentConfigPath));
            agentList = mapper.readValue(parser, AgentList.class);
            System.out.println(">>> " + agentList);
            System.out.println("---");
        }catch (IOException ioe){
            ioe.printStackTrace();
            System.out.println("> [DEBUG] agent config file load failed.");
        }
    }

    /**
     * load capability config from config file
     */
    public void loadCapabilityConfig(){
        try{
            System.out.println("> try to load skill config from " + capabilityConfigPath);
            parser = yamlFactory.createParser(new File(capabilityConfigPath));
            capabilityList = mapper.readValue(parser, CapabilityList.class);
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
                if(capabilityList.availableCapabilityList.stream().noneMatch(capability -> capability.name.equals(step.name))){
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
        System.out.println("[Vocabulary] " + vocabularyList.general.size() + " vocabulary found in general concept.");
        System.out.println("[Vocabulary] " + vocabularyList.customMappingList.size() + " custom mapping found.");
        System.out.println("[Vocabulary] " + gson.toJson(vocabularyList));
    }

    /**
     * generate a service list in hashmap form
     * this map could be used as a quick lookup table when checking service level
     */
    public void generateServiceMap(){
        System.out.println("[DEBUG] start to generate service map");
        HashMap<String, SubService> tempServiceMap = new HashMap<>();
        SubService tempService;
        for(ServiceSystem system: serviceList.serviceList){
            tempService = new SubService(system.name, system.type, system.description, system.config);
            tempServiceMap.put(system.name, tempService.overrideJenkinsConfig(null));
            for(SubService service: system.getSubService()){
                tempService.setName(service.name);
                tempService.setType(service.type);
                tempService.setDescription(service.description);
                // override system jenkins config if service has individual config
//                if(service.jenkinsConfig != null)
//                    tempService.setJenkinsConfig(service.jenkinsConfig);
                tempServiceMap.put(service.name, tempService.overrideJenkinsConfig(service.config));
            }
        }
        serviceList.setServiceMap(tempServiceMap);
        System.out.println(">>> " + gson.toJson(serviceList));
    }

    /**
     * verify all listed vocabulary in capability specification file is legal
     */
    public void verifyCapabilityInputVocabulary(){
        System.out.println("> start to verify capability config");
        Iterator<Capability> capabilityIterator = capabilityList.availableCapabilityList.iterator();
        while(capabilityIterator.hasNext()){
            Capability currentCapability = capabilityIterator.next();
            System.out.println("[DEBUG] checking capability '" + currentCapability.name + "'");
            // check input type
            if(currentCapability.input.stream().anyMatch(this::isVocabularyIllegal)){
                System.out.println("[WARNING] illegal input vocabulary found in Capability '" + currentCapability.name + "'.");
                System.out.println("[WARNING] system will ignore Capability '" + currentCapability.name + "' from now on.");
                capabilityIterator.remove();
                continue; // move on to next capability if current one get removed
            }
            // check output type
            if(!vocabularyList.output.contains(currentCapability.output.type)){
                System.out.println("[WARNING] illegal output found in Capability '" + currentCapability.name + "'.");
                System.out.println("[WARNING] system will ignore Capability '" + currentCapability.name + "' from now on.");
                capabilityIterator.remove();
            }
        }
        System.out.println(">>> " + gson.toJson(capabilityList));
        System.out.println("---");
    }

    /**
     * verify all used vocabulary in each concept, all vocabulary are expected existing in general concept<br>
     * illegal vocabulary usage will be ignored
     */
    public void verifyConceptVocabulary(){
        System.out.println("> start to verify vocabulary used in concept");
        for(Concept concept: vocabularyList.conceptList){
            System.out.println("[DEBUG] checking concept '" + concept.conceptName + "'");
            Iterator<String> iterator = concept.usedVocabulary.iterator();
            while(iterator.hasNext()){
                String vocabulary = iterator.next();
                if(isVocabularyIllegal(vocabulary)){
                    System.out.println("[WARNING] vocabulary '" + vocabulary + "' used in concept '" + concept.conceptName + "' does not exist in general concept or any other concept !");
                    System.out.println("[WARNING] system will ignore this vocabulary from now on.");
                    iterator.remove();
                }
            }
        }
        System.out.println(">>> " + gson.toJson(vocabularyList.conceptList));
        System.out.println("---");
    }

    /**
     * verify if loaded vocabulary config file has illegal format and remove illegal entities
     */
    public void verifyCustomMappingVocabulary(){
        // check custom mapping binding vocabulary and schema
        System.out.println("> start to verify vocabulary custom mapping.");
        Iterator<CustomMapping> mappingsIterator = vocabularyList.customMappingList.iterator();
        while(mappingsIterator.hasNext()){
            CustomMapping mapping = mappingsIterator.next();
            String mappingSchema = mapping.schema;
            String resultSchema = mappingSchema;
            Pattern vocabularyPattern = Pattern.compile("%\\{([a-zA-Z0-9-/]+)}");
            Matcher vocabularyMatcher = vocabularyPattern.matcher(mappingSchema);
            // create vocabulary verify list from schema data, format schema
            ArrayList<String> verifyList = new ArrayList<>();
            while (vocabularyMatcher.find()){
                String fullVocabulary = vocabularyMatcher.group(1);
                resultSchema = resultSchema.replaceAll("%\\{" + fullVocabulary + "\\}", "\"" + fullVocabulary + "\"");
                System.out.println("[DEBUG] vocabulary '" + fullVocabulary + "' detected in custom mapping schema '" + mapping.mappingName + "'");
                verifyList.add(fullVocabulary);
            }
            // check schema
            if(!isValidJsonString(resultSchema)){
                System.out.println("[WARNING] schema of mapping '" + mapping.mappingName + "' is not a json string.");
                System.out.println("[WARNING] system will ignore mapping '" + mapping.mappingName + "' from now on.");
                mappingsIterator.remove();
                continue;
            }
            // check vocabulary verify list
            for(String currentRawVocabulary: verifyList){
                if(isVocabularyIllegal(currentRawVocabulary)){
                    System.out.println("[WARNING] system will ignore mapping '" + mapping.mappingName + "' from now on.");
                    mappingsIterator.remove();
                    break;
                }
            }
        }
        vocabularyList.createCustomMappingHashMap();
        System.out.println(">>> " + gson.toJson(vocabularyList));
        System.out.println("---");
    }

    /**
     * check if vocabulary available in assigned concept, concept name and vocabulary value are expected to be separated by slash character<br>
     * use default concept 'general' if there is no slash character found in input vocabulary
     * @param input vocabulary
     * @return true if illegal, otherwise false
     */
    private boolean isVocabularyIllegal(String input){
        if(input.contains("/")){
            String conceptType = input.split("/")[0];
            String vocabulary = input.split("/", 2)[1];
            try{
                Concept concept = vocabularyList.getConcept(conceptType);
                /* no matched vocabulary found in target concept */
                if(!concept.usedVocabulary.contains(vocabulary)){
                    System.out.println("[WARNING] vocabulary '" + vocabulary + "' is not available in concept '" + conceptType + "' !");
                    return true;
                }
            }catch (IllegalConceptException e){
                /* no concept found */
                System.out.println("[WARNING] concept '" + conceptType + "' does not exist.");
                return true;
            }
        }else{
            /* no matched vocabulary found in general concept */
            if(!vocabularyList.general.contains(input)){
                System.out.println("[WARNING] vocabulary '" + input + "' is not available in general concept !");
                return true;
            }
        }
        return false;
    }

    /**
     * verify if input json string is legal json format
     * @param raw json string
     * @return true if legal, otherwise false
     */
    private boolean isValidJsonString(String raw){
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
