package soselab.msdobot.aggregatebot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.fliptables.FlipTable;
import com.jayway.jsonpath.JsonPath;
import net.bytebuddy.description.method.MethodDescription;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import soselab.msdobot.aggregatebot.Entity.Capability.Capability;
import soselab.msdobot.aggregatebot.Entity.DiscordEmbedFieldTemplate;
import soselab.msdobot.aggregatebot.Entity.DiscordEmbedTemplate;
import soselab.msdobot.aggregatebot.Entity.DiscordMessageTemplate;
import soselab.msdobot.aggregatebot.Entity.RasaIntent;
import soselab.msdobot.aggregatebot.Service.*;
import soselab.msdobot.aggregatebot.Service.Discord.DiscordOnMessageListener;
import soselab.msdobot.aggregatebot.Service.Discord.JDAConnect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
@TestPropertySource("classpath:application-dev.properties")
class AggregateBotApplicationTest {

    @Autowired
    private JenkinsService jenkinsService;
    @Autowired
    private ConfigLoader configLoader;
    @Autowired
    private RasaService rasaService;
    @Autowired
    private Orchestrator orchestrator;
    @Autowired
    private DiscordOnMessageListener onMsgListener;
    @Autowired
    private JDAConnect jdaConnect;

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
        configLoader.loadCapabilityConfig();
    }

    /**
     * test if big intent yaml setting file load successfully
     */
    @Test
    void loadBigIntentConfigTest(){
        configLoader.loadUpperIntentConfig();
    }

    /**
     * test if keyword yaml setting file load successfully
     */
    @Test
    void loadVocabularyConfigTest(){
        configLoader.loadVocabularyConfig();
    }

    /**
     * test if jenkins health report code block works correctly
     */
    @Test
    void testRequestJenkinsHealthReport(){
        // todo: update jenkins health report test
//        Config testConfig = new Config("linux", "11eb26ac2812dda2527594fc7a423a98fd", "http://soselab.asuscomm.com:10000");
//        jenkinsService.getJenkinsHealthReport(testConfig, "k8s-pdas-ContractService");
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
        RasaIntent intent = new RasaIntent("test-jenkins-health", "Cinema");
        var msg = orchestrator.capabilitySelector(intent);
        jdaConnect.send(msg);
    }

    @Test
    void
    testSkillSelector2(){
        RasaIntent intent = new RasaIntent("ask_jenkins_job_build_number", "Cinema");
        var msg = orchestrator.capabilitySelector(intent);
        jdaConnect.send(msg);
    }

    /**
     * test user input works find
     * include rasa analyze, rasa intent extraction and orchestrator skill picking
     */
    @Test
    void testUserInput(){
        String userUtterance = "test report of Ordering";
        RasaIntent intent = rasaService.restrictedIntentParsing(rasaService.analyze(userUtterance));
        var msg = orchestrator.capabilitySelector(intent);
        jdaConnect.send(msg);
    }

    /**
     * test if skill with get method works fine
     */
    @Test
    void testGetMethod(){
        RasaIntent intent = new RasaIntent("get_this", "FakeOne");
        orchestrator.capabilitySelector(intent);
    }

    @Test
    void testCustomMappingRequest(){
        RasaIntent intent = new RasaIntent("ask_job_view_list", "Cinema");
        var msg = orchestrator.capabilitySelector(intent);
        jdaConnect.send(msg);
    }

    @Test
    void testSendReportMsg(){
        // temporarily disable this testing method, try to update report workflow
        RasaIntent intent = new RasaIntent("ask_job_view_list", "Cinema");
//        jdaConnect.send(onMsgListener.createReportMessage(orchestrator.capabilitySelector(intent)));
    }

    /**
     * test if jenkins test report code block works correctly
     */
    @Test
    void testTestReport(){
        // todo: update test report test
//        Config testConfig = new Config("linux", "11eb26ac2812dda2527594fc7a423a98fd", "http://soselab.asuscomm.com:10000");
//        jenkinsService.getDirectJenkinsTestReport(testConfig, "k8s-pdas-ContractService");
    }

    /**
     * test if skill set with illegal input keyword will be removed correctly
     */
    @Test
    void autoRemoveIllegalSkillTest(){
        // todo: update illegal capability remove
//        configLoader.loadCapabilityConfig();
//        configLoader.verifyCapabilityInputVocabulary();
    }

    @Test
    void testJsonPath(){
        Gson gson = new Gson();
        configLoader.loadCapabilityConfig();
//        String dataPath = "$.availableSkillList[*].name";
        String dataPath = "$.availableSkillList[0]";
        System.out.println(JsonPath.read(gson.toJson(configLoader.capabilityList), dataPath).toString());
    }

    @Test
    void testRegexReplace(){
        String temp = "username";
        String pattern = "{" + temp + "}";
        String normalizedPattern = pattern.replace("{", "\\{").replace("}", "\\}");
        String testPattern = "\\{" + temp + "}";
        String testExample = "regex replace test [{username}]";
        System.out.println(testExample.replaceAll(testPattern, "content"));
    }

    @Test
    void testRegexIterate(){
        String test = "[ta][eb][sc][td][rf]e[777]";
        Pattern pattern = Pattern.compile("\\[([a-z]+)\\]");
        Matcher matcher = pattern.matcher(test);
        while(matcher.find()){
            System.out.println(matcher.group(1));
        }
    }

    @Test
    void testGsonJsonObjectAddProperty(){
        JsonObject obj = new JsonObject();
        obj.addProperty("test", "raw content");
        obj.addProperty("test", "fixed content");
        System.out.println(obj);
    }

    @Test
    void testFlipTable(){
        String[] header = {"init", ""};
        String[][] body = {
                {"row1", "row11"},
                {"row2", "row21"}
        };
        System.out.println(FlipTable.of(header, body));
    }

    @Test
    void testToArray(){
        ArrayList<ArrayList<String>> test = new ArrayList<>();
        ArrayList<String> body = new ArrayList<>();
        ArrayList<String> body1 = new ArrayList<>();
        body.add("1");
        body.add("2");
        body.add("3");
        test.add(body);
        body1.add("a");
        body1.add("b");
        body1.add("c");
        test.add(body1);
        // print
//        ArrayList<String>[] half = (ArrayList<String>[]) test.toArray();
//        String[][] comp = (String[][]) Arrays.stream(half).toArray();
        Gson gson = new Gson();
        System.out.println(gson.toJson(test));
        String[][] comp = new String[2][3];
        for(ArrayList<String> first: test){
            for(String second: first){
                comp[test.indexOf(first)][first.indexOf(second)] = second;
            }
        }
        System.out.println(gson.toJson(comp));
    }

    @Test
    void testDefaultRendering(){
        JsonArray keyArray = new JsonArray();
        keyArray.add("1");
        keyArray.add("2");
        keyArray.add("3");
        JsonArray valueArray = new JsonArray();
        valueArray.add("a");
        valueArray.add("b");
        valueArray.add("c");
        JsonArray resultArray = new JsonArray();
        resultArray.add(keyArray);
        resultArray.add(valueArray);
        RenderingService rendering = new RenderingService(null, resultArray);
        System.out.println(rendering.parseToSimpleAsciiArtTable());
    }

    @Test
    void testHashCode(){
//        ArrayList<String> a = new ArrayList<>();
//        a.add("A");
//        a.add("B");
//        ArrayList<String> b = new ArrayList<>();
//        b.add("A");
//        b.add("B");
        String a = "AB";
        String b = "A";
        b = b + "B";
        if(a.hashCode() == b.hashCode())
            System.out.println("equal");
        else
            System.out.println("non");
    }

    @Test
    void testGsonConvert(){
        Gson gson = new Gson();
        HashMap<String, String> test = new HashMap<>();
        test.put("a", "b");
        test.put("c", "d");
        System.out.println(gson.toJson(test));
        String raw = gson.toJson(test);
        java.lang.reflect.Type type = new TypeToken<HashMap<String, String>>(){}.getType();
        HashMap<String, String> reform = gson.fromJson(raw, type);
        System.out.println(reform.size());
        System.out.println(reform.get("a"));
        System.out.println(gson.toJson(reform));
    }

    @Test
    void testPseudoIntents(){
        RasaIntent intent = new RasaIntent("pseudo-service-detail-go", "Pseudo");
        var msg = orchestrator.capabilitySelector(intent);
        jdaConnect.send(msg);
    }

    @Test
    void testLastElement(){
        ArrayList<String> list = new ArrayList<>();
        list.add("element 1");
        list.add("element 2");
        String test = "element 2";
        for(String ele: list){
            if(list.indexOf(ele) == list.size() - 1)
                System.out.println(ele);
        }
    }

    @Test
    void testDiscordMessageTemplate(){
        var template = new DiscordMessageTemplate("testing content");
        jdaConnect.send(RenderingService.createDiscordMessage(template));
    }

    @Test
    void testDiscordMessageEmbedTemplate(){
        var template = new DiscordMessageTemplate("top");
        var embed = new DiscordEmbedTemplate();
        embed.setTitle("embed title");
        embed.setDescription("embed description");
        var field = new DiscordEmbedFieldTemplate("field name", "field value");
        embed.setFieldList(new ArrayList<DiscordEmbedFieldTemplate>(Collections.singletonList(field)));
        template.setEmbedList(new ArrayList<DiscordEmbedTemplate>(Collections.singletonList(embed)));
        jdaConnect.send(RenderingService.createDiscordMessage(template));
    }

    @Test
    void testInput(){
//        Capability capability, HashMap<String, String> aggregateData, HashMap<String, HashMap<String, String>> specificAggregateData, HashMap<String, HashMap<String, String>> properties
        var aggregateDate = new HashMap<String, String>();
        var specificData = new HashMap<String, HashMap<String, String>>();
        var properties = new HashMap<String, HashMap<String, String>>();
        aggregateDate.put("systemDataKey", "systemData");
        var tempMap = new HashMap<String, String>();
        tempMap.put("serviceA", "content A");
        specificData.put("specific", tempMap);
        tempMap = new HashMap<String, String>();
        tempMap.put("property key", "property value");
        properties.put("property context", tempMap);
    }

    @Test
    void testMap(){
        var map = new HashMap<String, String>();
        map.put("head", "body");
        System.out.println(new Gson().toJson(map));
    }
}