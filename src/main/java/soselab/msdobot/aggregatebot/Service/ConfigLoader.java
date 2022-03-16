package soselab.msdobot.aggregatebot.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import soselab.msdobot.aggregatebot.Entity.Agent.AgentList;
import soselab.msdobot.aggregatebot.Entity.UpperIntent.UpperIntentList;
import soselab.msdobot.aggregatebot.Entity.Vocabulary.Concept;
import soselab.msdobot.aggregatebot.Entity.Vocabulary.CustomMapping;
import soselab.msdobot.aggregatebot.Entity.Vocabulary.Vocabulary;
import soselab.msdobot.aggregatebot.Entity.Service.ServiceList;
import soselab.msdobot.aggregatebot.Entity.Service.ServiceSystem;
import soselab.msdobot.aggregatebot.Entity.Service.SubService;
import soselab.msdobot.aggregatebot.Entity.Capability.Capability;
import soselab.msdobot.aggregatebot.Entity.Capability.CapabilityList;

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

    @Autowired
    public ConfigLoader(Environment env){
        yamlFactory = new YAMLFactory();
        mapper = new ObjectMapper();
        agentConfigPath = env.getProperty("bot.config.agent");
        serviceConfigPath = env.getProperty("bot.config.service");
        capabilityConfigPath = env.getProperty("bot.config.capability");
        upperIntentConfigPath = env.getProperty("bot.config.upperIntent");
        vocabularyConfigPath = env.getProperty("bot.config.vocabulary");

        loadVocabularyConfig();
        verifyCustomMappingVocabulary();
        loadAgentConfig();
        loadCapabilityConfig();
        loadUpperIntentConfig();
        loadServiceConfig();
        verifyCapabilityInputVocabulary();
    }

    public void loadAgentConfig(){
        try {
            System.out.println("> try to parse agent config from " + agentConfigPath);
            parser = yamlFactory.createParser(new File(agentConfigPath));
            agentList = mapper.readValue(parser, AgentList.class);
            System.out.println(agentList);
        }catch (IOException ioe){
            ioe.printStackTrace();
            System.out.println("> [DEBUG] agent config file load failed.");
        }
    }

    public void loadCapabilityConfig(){
        try{
            System.out.println("> try to parse skill config from " + capabilityConfigPath);
            parser = yamlFactory.createParser(new File(capabilityConfigPath));
            capabilityList = mapper.readValue(parser, CapabilityList.class);
            System.out.println(capabilityList);
        }catch (IOException ioe){
            ioe.printStackTrace();
            System.out.println("> [DEBUG] capability config file load failed.");
        }
    }

    public void loadUpperIntentConfig(){
        try{
            System.out.println("> try to parse upper intent config from " + upperIntentConfigPath);
            parser = yamlFactory.createParser(new File(upperIntentConfigPath));
            upperIntentList = mapper.readValue(parser, UpperIntentList.class);
            System.out.println(upperIntentList);
        }catch (IOException ioe){
            ioe.printStackTrace();
            System.out.println("> [DEBUG] upper intent config file load failed.");
        }
    }

    public void loadServiceConfig(){
        try{
            System.out.println("> try to parse service config from " + serviceConfigPath);
            parser = yamlFactory.createParser(new File(serviceConfigPath));
            serviceList = mapper.readValue(parser, ServiceList.class);
            System.out.println(serviceList);
            generateServiceMap();
        }catch (IOException ioe){
            ioe.printStackTrace();
            System.out.println("> [DEBUG] service config file load failed.");
        }
    }

    public void loadVocabularyConfig(){
        try{
            System.out.println("> try to parse keyword config from ");
            parser = yamlFactory.createParser(new File(vocabularyConfigPath));
            vocabularyList = mapper.readValue(parser,  Vocabulary.class);
            System.out.println(new Gson().toJson(vocabularyList));
        }catch (IOException ioe){
            ioe.printStackTrace();
            System.out.println("> [DEBUG] keyword config file load failed.");
        }
    }

    /**
     * generate a service list in hashmap form
     * this map could be used as a quick lookup table when checking service level
     */
    public void generateServiceMap(){
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
        System.out.println("---");
        System.out.println("> [DEBUG] complete service map " + new Gson().toJson(serviceList));
        System.out.println("---");
    }

    private boolean isInputVocabularyLegal(String input){
        return vocabularyList.general.contains(input) || vocabularyList.customMappingHashMap.containsKey(input);
    }

    /**
     * verify all listed vocabulary in capability specification file is legal
     */
    public void verifyCapabilityInputVocabulary(){
        Iterator<Capability> capabilityIterator = capabilityList.availableCapabilityList.iterator();
        while(capabilityIterator.hasNext()){
            Capability currentCapability = capabilityIterator.next();
            System.out.println(currentCapability);
            // check input type
            if(currentCapability.input.stream().anyMatch(input -> !isInputVocabularyLegal(input))){
                System.out.println("[DEBUG] illegal input found in Capability '" + currentCapability.name + "'.");
                System.out.println("[DEBUG] auto remove illegal Capability '" + currentCapability.name + "'.");
                capabilityIterator.remove();
                continue; // move on to next skill if current one get removed
            }
            // check output type
            if(!vocabularyList.output.contains(currentCapability.output.type)){
                System.out.println("[DEBUG] illegal output found in Capability '" + currentCapability.name + "'.");
                System.out.println("[DEBUG] auto remove illegal Capability '" + currentCapability.name + "'.");
                capabilityIterator.remove();
            }
        }
        System.out.println(new Gson().toJson(capabilityList));
//        skillList.availableSkillList.removeIf(skill ->
//            skill.input.stream().anyMatch(input -> !keywordList.keyword.contains(input)));
    }

    public void verifyCustomMappingVocabulary(){
        // check custom mapping binding vocabulary and schema
        Iterator<CustomMapping> mappingsIterator = vocabularyList.customMappingList.iterator();
        while(mappingsIterator.hasNext()){
            CustomMapping mapping = mappingsIterator.next();
            ArrayList<Concept> bindingVocabularyList = mapping.usedConcept;
            String mappingSchema = mapping.schema;
            Pattern vocabularyPattern = Pattern.compile("%\\{([a-zA-Z0-9-]+)\\}");
            Matcher vocabularyMatcher = vocabularyPattern.matcher(mappingSchema);
            // check schema
            while (vocabularyMatcher.find()){
                System.out.println("[DEBUG] vocabulary '" + vocabularyMatcher.group(1) + "' detected in schema " + mapping.mappingName);
                // retrieve vocabulary concept type
            }
            /*
            // check binding vocabulary
            if(!vocabularyList.input.containsAll(bindingVocabularyList)){
                System.out.println("[DEBUG] illegal vocabulary found in mapping '" + mapping.mappingName + "'");
                mappingsIterator.remove();
                continue;
            }
            // check schema
            String mappingSchema = mapping.schema;
            for(String input: bindingVocabularyList)
                mappingSchema = mappingSchema.replaceAll("\\$" + input, input);
            if(!isValidJsonString(mappingSchema)){
                System.out.println("[DEBUG] schema of mapping '" + mapping.mappingName + "' is not a json string");
                mappingsIterator.remove();
            }
             */
        }
        vocabularyList.createCustomMappingHashMap();
        System.out.println(new Gson().toJson(vocabularyList));
    }

    private boolean isValidJsonString(String raw){
        try{
            new JSONObject(raw);
        }catch (JSONException e){
            try{
                new JSONArray(raw);
            }catch (JSONException je){
                return false;
            }
        }
        return true;
    }

}
