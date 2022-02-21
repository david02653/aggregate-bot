package soselab.msdobot.aggregatebot;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import soselab.msdobot.aggregatebot.Entity.RasaIntent;
import soselab.msdobot.aggregatebot.Entity.Service.Config;
import soselab.msdobot.aggregatebot.Service.*;

@SpringBootTest
@TestPropertySource("classpath:application-dev.properties")
class AggregateBotApplicationTest {

    @Autowired
    private LoadService service;
    @Autowired
    private JenkinsService jenkinsService;
    @Autowired
    private ConfigLoader configLoader;
    @Autowired
    private RasaService rasaService;
    @Autowired
    private Orchestrator orchestrator;

    /**
     * test if properties file load correctly
     */
    @Test
    void propertiesTest(){
        System.out.println(service.loadEnv());
        Assertions.assertEquals(service.loadEnv(), "test stuff");
    }

    /**
     * test if agent yaml setting file load successfully
     */
    @Test
    void loadAgentConfigTest(){
        configLoader.loadAgentConfig();
    }

    /**
     * test if service yaml setting file load successfully
     */
    @Test
    void loadServiceConfigTest(){
        configLoader.loadServiceConfig();
    }

    /**
     * test if skill yaml setting file load successfully
     */
    @Test
    void loadSkillConfigTest(){
        configLoader.loadSkillConfig();
    }

    /**
     * test if keyword yaml setting file load successfully
     */
    @Test
    void loadKeywordConfigTest(){
        configLoader.loadKeywordConfig();
    }

    /**
     * test if jenkins health report code block works correctly
     */
    @Test
    void testRequestJenkinsHealthReport(){
        Config testConfig = new Config("linux", "11eb26ac2812dda2527594fc7a423a98fd", "http://soselab.asuscomm.com:10000");
        jenkinsService.getJenkinsHealthReport(testConfig, "k8s-pdas-ContractService");
    }

    /**
     * test rasa intent analyze function
     */
    @Test
    void testRasaAnalyze(){
        Assertions.assertTrue(rasaService.analyze("health report of Payment").contains("intent"));
    }

    /**
     * test intent extraction works correctly
     */
    @Test
    void testRasaIntentExtract(){
        System.out.println(rasaService.restrictedIntentParsing("[{\"recipient_id\":\"test\",\"text\":\"{'intent': 'ask_job_health_report', 'jobName': 'Payment', 'lostName': False}\"}]"));
    }

    /**
     * test if service hash map can be generated successfully
     */
    @Test
    void testGenerateServiceMap(){
        configLoader.generateServiceMap();
    }

    /**
     * test if orchestrator can correctly pick correspond skill from skill list
     */
    @Test
    void testSkillSelector(){
        RasaIntent intent = new RasaIntent("ask_job_health_report", "Cinema");
        orchestrator.skillSelector(intent);
    }

    /**
     * test user input works find
     * include rasa analyze, rasa intent extraction and orchestrator skill picking
     */
    @Test
    void testUserInput(){
        String userUtterance = "test report of Ordering";
        RasaIntent intent = rasaService.restrictedIntentParsing(rasaService.analyze(userUtterance));
        orchestrator.skillSelector(intent);
    }

    /**
     * test if skill with get method works fine
     */
    @Test
    void testGetMethod(){
        RasaIntent intent = new RasaIntent("get_this", "FakeOne");
        orchestrator.skillSelector(intent);
    }

    /**
     * test if jenkins test report code block works correctly
     */
    @Test
    void testTestReport(){
        Config testConfig = new Config("linux", "11eb26ac2812dda2527594fc7a423a98fd", "http://soselab.asuscomm.com:10000");
        jenkinsService.getJenkinsTestReport(testConfig, "k8s-pdas-ContractService");
    }

    /**
     * test if skill set with illegal input keyword will be removed correctly
     */
    @Test
    void autoRemoveIllegalSkillTest(){
        configLoader.loadSkillConfig();
        configLoader.verifySkillInputKeyword();
    }

    @Test
    void testJsonPath(){
        Gson gson = new Gson();
        configLoader.loadSkillConfig();
//        String dataPath = "$.availableSkillList[*].name";
        String dataPath = "$.availableSkillList[0]";
        System.out.println(JsonPath.read(gson.toJson(configLoader.skillList), dataPath).toString());
    }
}