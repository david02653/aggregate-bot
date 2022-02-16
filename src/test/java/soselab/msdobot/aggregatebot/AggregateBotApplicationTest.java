package soselab.msdobot.aggregatebot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import soselab.msdobot.aggregatebot.Entity.RasaIntent;
import soselab.msdobot.aggregatebot.Entity.Service.JenkinsConfig;
import soselab.msdobot.aggregatebot.Service.*;

import java.io.ObjectInputFilter;

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

    @Test
    void propertiesTest(){
        System.out.println(service.loadEnv());
        Assertions.assertEquals(service.loadEnv(), "test stuff");
    }

    @Test
    void loadAgentConfigTest(){
        configLoader.loadAgentConfig();
    }

    @Test
    void loadServiceConfigTest(){
        configLoader.loadServiceConfig();
    }

    @Test
    void loadSkillConfigTest(){
        configLoader.loadSkillConfig();
    }

    @Test
    void testRequestJenkinsHealthReport(){
        JenkinsConfig testConfig = new JenkinsConfig("linux", "11eb26ac2812dda2527594fc7a423a98fd", "http://soselab.asuscomm.com:10000");
        jenkinsService.getJenkinsHealthReport(testConfig, "k8s-pdas-ContractService");
    }

    @Test
    void testRasaAnalyze(){
        Assertions.assertTrue(rasaService.analyze("health report of Payment").contains("intent"));
    }

    @Test
    void testRasaIntentExtract(){
        System.out.println(rasaService.restrictedIntentParsing("[{\"recipient_id\":\"test\",\"text\":\"{'intent': 'ask_job_health_report', 'jobName': 'Payment', 'lostName': False}\"}]"));
    }

    @Test
    void testGenerateServiceMap(){
        configLoader.generateServiceMap();
    }

    @Test
    void testSkillSelector(){
        RasaIntent intent = new RasaIntent("ask_job_health_report", "Payment");
        orchestrator.skillSelector(intent);
    }

    @Test
    void testUserInput(){
        String userUtterance = "health report of Cinema";
        RasaIntent intent = rasaService.restrictedIntentParsing(rasaService.analyze(userUtterance));
        orchestrator.skillSelector(intent);
    }

    @Test
    void testGetMethod(){
        RasaIntent intent = new RasaIntent("get_this", "FakeOne");
        orchestrator.skillSelector(intent);
    }
}