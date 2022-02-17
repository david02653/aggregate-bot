package soselab.msdobot.aggregatebot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import soselab.msdobot.aggregatebot.Entity.Service.JenkinsConfig;
import soselab.msdobot.aggregatebot.Entity.Skill.SkillOutput;
import soselab.msdobot.aggregatebot.Exception.JenkinsRequestException;

import java.util.Collections;
import java.util.HashMap;

/**
 * declaration of jenkins skill
 */
@Service
public class JenkinsService {

    public JenkinsService(){
        templateBuilder = new RestTemplateBuilder();
    }

    private RestTemplate template;
    private RestTemplateBuilder templateBuilder;

    private ResponseEntity<String> basicJenkinsRequest(String url, String user, String token) throws JenkinsRequestException {
        template = templateBuilder.basicAuthentication(user, token).build();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = null;
        try{
            resp = template.exchange(url, HttpMethod.GET, entity, String.class);
//            System.out.println(resp);
        }catch (Exception e){
            e.printStackTrace();
            throw new JenkinsRequestException();
        }
        return resp;
    }

    /**
     * get jenkins health report
     * @param jenkinsConfig required info to access jenkins endpoint
     * @param targetService target job
     */
    public String getJenkinsHealthReport(JenkinsConfig jenkinsConfig, String targetService){
        String queryUrl = jenkinsConfig.endpoint + "/job/" + targetService + "/api/json?depth=2&tree=healthReport[*]";
        System.out.println("> [url] " + queryUrl);
        try {
            ResponseEntity<String> resp = basicJenkinsRequest(queryUrl, jenkinsConfig.username, jenkinsConfig.accessToken);
//            System.out.println(resp.getBody());
            Gson gson = new Gson();
            JsonObject body = gson.fromJson(resp.getBody(), JsonObject.class);
            JsonArray healthReport = body.getAsJsonArray("healthReport");
            System.out.println(gson.toJson(healthReport));
            return gson.toJson(healthReport);
        }catch (JenkinsRequestException je){
            je.printStackTrace();
            System.out.println("[DEBUG] failed requesting jenkins health data.");
            return "error occurred while requesting health data";
        }
    }

    public String getJenkinsTestReport(JenkinsConfig config, String targetService){
        // retrieve the latest build number
        String buildStatusRequestUrl = config.endpoint + "/job/" + targetService + "/api/json?depth=2&tree=lastBuild[number]";
        System.out.println("[DEBUG] try to request latest build data from " + buildStatusRequestUrl);
        try {
            ResponseEntity<String> buildResp = basicJenkinsRequest(buildStatusRequestUrl, config.username, config.accessToken);
            // extract build data
            Gson gson = new Gson();
            JsonObject statusObj = gson.fromJson(buildResp.getBody(), JsonObject.class);
            String buildNumber = statusObj.getAsJsonObject("lastBuild").get("number").getAsString();
            // declare test report request url
            String testReportRequestUrl = config.endpoint + "/job/" + targetService + "/" + buildNumber + "/testReport/api/json";
            System.out.println("[DEBUG] try to request test report data from " + testReportRequestUrl);
            ResponseEntity<String> testResp = basicJenkinsRequest(testReportRequestUrl, config.username, config.accessToken);
            System.out.println(">>> raw test report of " + targetService + ": " + gson.toJson(testResp.getBody()));
            System.out.println("-----");
            return gson.toJson(testResp.getBody());
        }catch (JenkinsRequestException je){
            je.printStackTrace();
            System.out.println("[DEBUG] failed requesting jenkins build status.");
            return "error occurred while requesting build data / test report data";
        }
    }
}
