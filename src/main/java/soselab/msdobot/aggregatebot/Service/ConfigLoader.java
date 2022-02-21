package soselab.msdobot.aggregatebot.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import soselab.msdobot.aggregatebot.Entity.Agent.AgentList;
import soselab.msdobot.aggregatebot.Entity.Keyword.Keyword;
import soselab.msdobot.aggregatebot.Entity.Service.ServiceList;
import soselab.msdobot.aggregatebot.Entity.Service.ServiceSystem;
import soselab.msdobot.aggregatebot.Entity.Service.SubService;
import soselab.msdobot.aggregatebot.Entity.Skill.Skill;
import soselab.msdobot.aggregatebot.Entity.Skill.SkillList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

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
    private final String skillConfigPath;
    private final String keywordConfigPath;

    public static AgentList agentList;
    public static ServiceList serviceList;
    public static SkillList skillList;
    public static Keyword keywordList;

    @Autowired
    public ConfigLoader(Environment env){
        yamlFactory = new YAMLFactory();
        mapper = new ObjectMapper();
        agentConfigPath = env.getProperty("bot.config.agent");
        serviceConfigPath = env.getProperty("bot.config.service");
        skillConfigPath = env.getProperty("bot.config.skill");
        keywordConfigPath = env.getProperty("bot.config.keyword");

        loadKeywordConfig();
        loadAgentConfig();
        loadSkillConfig();
        loadServiceConfig();
        verifySkillInputKeyword();
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

    public void loadSkillConfig(){
        try{
            System.out.println("> try to parse skill config from " + skillConfigPath);
            parser = yamlFactory.createParser(new File(skillConfigPath));
            skillList = mapper.readValue(parser, SkillList.class);
            System.out.println(skillList);
        }catch (IOException ioe){
            ioe.printStackTrace();
            System.out.println("> [DEBUG] skill config file load failed.");
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

    public void loadKeywordConfig(){
        try{
            System.out.println("> try to parse keyword config from ");
            parser = yamlFactory.createParser(new File(keywordConfigPath));
            keywordList = mapper.readValue(parser,  Keyword.class);
            System.out.println(new Gson().toJson(keywordList));
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

    /**
     * verify all listed keyword in skill specification file is legal
     */
    public void verifySkillInputKeyword(){
        Iterator<Skill> skillIterator = skillList.availableSkillList.iterator();
        while(skillIterator.hasNext()){
            Skill currentSkill = skillIterator.next();
            // check input type
            if(currentSkill.input.stream().anyMatch(input -> !keywordList.input.contains(input))){
                System.out.println("[DEBUG] illegal input found in Skill '" + currentSkill.name + "'.");
                System.out.println("[DEBUG] auto remove illegal Skill '" + currentSkill.name + "'.");
                skillIterator.remove();
                continue; // move on to next skill if current one get removed
            }
            // check output type
            if(!keywordList.output.contains(currentSkill.output.type)){
                System.out.println("[DEBUG] illegal output found in Skill '" + currentSkill.name + "'.");
                System.out.println("[DEBUG] auto remove illegal Skill '" + currentSkill.name + "'.");
                skillIterator.remove();
            }
        }
        System.out.println(new Gson().toJson(skillList));
//        skillList.availableSkillList.removeIf(skill ->
//            skill.input.stream().anyMatch(input -> !keywordList.keyword.contains(input)));
    }

}
