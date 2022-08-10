package soselab.msdobot.aggregatebot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import soselab.msdobot.aggregatebot.Exception.RequestException;

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

    /**
     * request data from jenkins endpoint, require jenkins-username and jenkins-accessToken
     * @param url jenkins endpoint
     * @param user jenkins username
     * @param token jenkins access token
     * @return response from jenkins endpoint
     * @throws RequestException
     */
    private ResponseEntity<String> basicJenkinsRequest(String url, String user, String token) throws RequestException {
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
            throw new RequestException();
        }
        return resp;
    }

    /**
     * get jenkins health report
     * @param config required info to access jenkins endpoint
     * @param targetService target job
     */
    public String getJenkinsHealthReport(JsonObject config, String targetService){
        String queryUrl = config.get("Api.endpoint").getAsString() + "/job/" + targetService + "/api/json?depth=2&tree=healthReport[*]";
        System.out.println("> [url] " + queryUrl);
        try {
            ResponseEntity<String> resp = basicJenkinsRequest(queryUrl, config.get("User.username").getAsString(), config.get("Api.accessToken").getAsString());
//            System.out.println(resp.getBody());
            Gson gson = new Gson();
            JsonObject body = gson.fromJson(resp.getBody(), JsonObject.class);
            JsonArray healthReport = body.getAsJsonArray("healthReport");
            System.out.println(gson.toJson(healthReport));
            return gson.toJson(healthReport);
        }catch (RequestException je){
            je.printStackTrace();
            System.out.println("[DEBUG] failed requesting jenkins health data.");
            return "error occurred while requesting health data";
        }
    }

    /**
     * get latest jenkins job build number
     * @param config jenkins config
     * @param targetService service name
     * @return latest job build number
     */
    public String getJenkinsLatestBuildNumber(JsonObject config, String targetService){
        String buildStatusRequestUrl = config.get("Api.endpoint").getAsString() + "/job/" + targetService + "/api/json?depth=2&tree=lastBuild[number]";
        System.out.println("[DEBUG][build number] received config : " + config);
        System.out.println("[DEBUG][build number] try to request latest build data from " + buildStatusRequestUrl);
        try{
            ResponseEntity<String> resp = basicJenkinsRequest(buildStatusRequestUrl, config.get("User.username").getAsString(), config.get("Api.accessToken").getAsString());
            Gson gson  = new Gson();
            JsonObject json = gson.fromJson(resp.getBody(), JsonObject.class);
            return json.getAsJsonObject("lastBuild").get("number").getAsString();
        }catch (RequestException je){
            je.printStackTrace();
            System.out.println("[DEBUG] failed requesting jenkins build status.");
            return "error occurred while requesting build data / test report data";
        }
    }

    /**
     * get latest jenkins job test report
     * @param config jenkins config, expect build number and jenkins account data
     * @param targetService service name
     * @return latest jenkins job test report
     */
    public String getJenkinsTestReport(JsonObject config, String targetService){
        String requestUrl = config.get("Api.endpoint").getAsString() + "/job/" + targetService + "/" + config.get("Api.buildNumber").getAsString() + "/testReport/api/json";
        System.out.println("[DEBUG][test report] received config : " + config);
        System.out.println("[DEBUG][test report] try to request test report data from " + requestUrl);
        try{
            ResponseEntity<String> resp = basicJenkinsRequest(requestUrl, config.get("User.username").getAsString(), config.get("Api.accessToken").getAsString());
//            return new Gson().toJson(resp.getBody());
            return testReportInfoExtractor(resp.getBody());
        }catch (RequestException je){
            je.printStackTrace();
            System.out.println("[DEBUG] failed requesting jenkins test report");
            return "error occurred while requesting test report data";
        }
    }

    /**
     * get job git information from jenkins of given build number
     * @param config jenkins config, expect build number and jenkins account data
     * @param targetService service name
     * @return target jenkins job git information
     */
    public String getJenkinsGitInfo(JsonObject config, String targetService){
        String requestUrl = config.get("Api.endpoint").getAsString() + "/job/" + targetService + "/" + config.get("Api.buildNumber").getAsString() + "/git/api/json";
        System.out.println("[DEBUG][git info] received config : " + config);
        System.out.println("[DEBUG][git info] try to request git info data from " + requestUrl);
        try{
            ResponseEntity<String> resp = basicJenkinsRequest(requestUrl, config.get("User.username").getAsString(), config.get("Api.accessToken").getAsString());
            Gson gson = new Gson();
            JsonObject body = gson.fromJson(resp.getBody(), JsonObject.class);
            System.out.println(gson.toJson(body));
            return gson.toJson(body);
        }catch (RequestException je){
            je.printStackTrace();
            System.out.println("[DEBUG] failed requesting jenkins git info");
            return "error occurred while requesting git info data";
        }
    }

    /**
     * get jenkins test report<br>
     * this function contains getting the latest build number and retrieve correspond report
     * @param config
     * @param targetService
     * @return
     */
    public String getDirectJenkinsTestReport(JsonObject config, String targetService){
        // retrieve the latest build number
        String buildStatusRequestUrl = config.get("Api.endpoint").getAsString() + "/job/" + targetService + "/api/json?depth=2&tree=lastBuild[number]";
        System.out.println("[DEBUG] received config : " + config);
        System.out.println("[DEBUG] try to request latest build data from " + buildStatusRequestUrl);
        try {
            ResponseEntity<String> buildResp = basicJenkinsRequest(buildStatusRequestUrl, config.get("User.username").getAsString(), config.get("Api.accessToken").getAsString());
            // extract build data
            Gson gson = new Gson();
            JsonObject statusObj = gson.fromJson(buildResp.getBody(), JsonObject.class);
            String buildNumber = statusObj.getAsJsonObject("lastBuild").get("number").getAsString();
            // declare test report request url
            String testReportRequestUrl = config.get("Api.endpoint").getAsString() + "/job/" + targetService + "/" + buildNumber + "/testReport/api/json";
            System.out.println("[DEBUG] try to request test report data from " + testReportRequestUrl);
            ResponseEntity<String> testResp = basicJenkinsRequest(testReportRequestUrl, config.get("User.username").getAsString(), config.get("Api.accessToken").getAsString());
            System.out.println(">>> raw test report of " + targetService + ": " + gson.toJson(testResp.getBody()));
            System.out.println("-----");
//            return gson.toJson(testResp.getBody());
            return testReportInfoExtractor(testResp.getBody());
        }catch (RequestException je){
            je.printStackTrace();
            System.out.println("[DEBUG] failed requesting jenkins build status.");
            return "error occurred while requesting build data / test report data";
        }
    }

    /**
     * extract key information from jenkins test report<br>
     * test duration, failed count, passed count and skipped count will be extracted
     * @param rawReport raw report string
     * @return result report string
     */
    private String testReportInfoExtractor(String rawReport){
        JsonObject testReport = new Gson().fromJson(rawReport, JsonObject.class);
        var duration = testReport.get("duration").getAsFloat();
        var failCount = testReport.get("failCount").getAsInt();
        var passCount = testReport.get("passCount").getAsInt();
        var skipCount = testReport.get("skipCount").getAsInt();
        var resultReport = String.format("Test Report: %d test case found, %d passed, %d failed, %d skipped, cost %.3f ms", failCount+passCount+skipCount, passCount, failCount, skipCount, duration);
        return resultReport;
    }

    /**
     * get jenkins view list
     * @param config
     * @return
     */
    public String getJenkinsViewList(String config){
        System.out.println("[DEBUG] received config " + config);
        Gson gson = new Gson();
        JsonObject rawObj = gson.fromJson(config, JsonObject.class);
        String rawString = rawObj.get("jenkinsInfo").getAsString();
        JsonObject configObj = gson.fromJson(rawString, JsonObject.class);
//        JSONObject rawObj = new JSONObject(config);
//        String rawString = rawObj.getString("Jenkins-info");
//        JSONObject configObj = new JSONObject(rawString);
//        System.out.println("[DEBUG] config " + configObj);
        String requestUrl = configObj.get("url").getAsString() +  "/api/json?tree=views[name,jobs[name,url]]";
        System.out.println("[DEBUG] request url " + requestUrl);
        try{
            ResponseEntity<String> resp = basicJenkinsRequest(requestUrl, configObj.get("name").getAsString(), configObj.get("token").getAsString());
            return resp.getBody();
        }catch (RequestException je){
            je.printStackTrace();
            System.out.println("[DEBUG] failed requesting jenkins view list.");
            return  "error occurred requesting view list";
        }
    }
}
