package soselab.msdobot.aggregatebot.Controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import soselab.msdobot.aggregatebot.Service.JenkinsService;
import soselab.msdobot.aggregatebot.Service.PseudoService;

/**
 * declare all capability endpoint
 */
@RestController
@RequestMapping(value = "/capability")
public class CapabilityController {

    private final JenkinsService jenkinsService;
    private final PseudoService pseudoService;
    private final Gson gson;

    @Autowired
    public CapabilityController(JenkinsService jenkinsService, PseudoService pseudoService){
        this.jenkinsService = jenkinsService;
        this.pseudoService = pseudoService;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * skill endpoint of jenkins health report
     * expect orchestrator to call this endpoint when correspond intent detected
     * @param requestBody
     */
    @PostMapping(value = "/jenkins-health")
    public ResponseEntity<String> requestJenkinsHealthData(@RequestBody String requestBody){
        JsonObject requestObj = gson.fromJson(requestBody, JsonObject.class);
        System.out.println(gson.toJson(requestBody));
        System.out.println("obj: " + gson.toJson(requestObj));
//        Config config = new Config(requestBody.username, requestBody.accessToken, requestBody.endpoint);
        return ResponseEntity.ok(jenkinsService.getJenkinsHealthReport(requestObj, requestObj.get("Api.serviceName").getAsString()));
    }

    /**
     * request job test report from jenkins endpoint, this version only need to assign target service name
     * @param requestBody config setting used to request jenkins endpoint
     * @return response from jenkins endpoint
     */
    @PostMapping(value = "/jenkins-testReport")
    public ResponseEntity<String> requestJenkinsTestReportData(@RequestBody String requestBody){
        JsonObject requestObject = gson.fromJson(requestBody, JsonObject.class);
        System.out.println(gson.toJson(requestBody));
//        Config config = new Config(requestBody.username, requestBody.accessToken, requestBody.endpoint);
        return ResponseEntity.ok(jenkinsService.getDirectJenkinsTestReport(requestObject, requestObject.get("Api.serviceName").getAsString()));
    }

    /**
     * request latest build number from jenkins endpoint
     * @param requestBody config setting used to request jenkins endpoint
     * @return response from jenkins endpoint
     */
    @PostMapping(value = "/jenkins-buildNumber")
    public ResponseEntity<String> requestJenkinsBuildNumber(@RequestBody String requestBody){
        JsonObject requestObj = gson.fromJson(requestBody, JsonObject.class);
        System.out.println(gson.toJson(requestBody));
//        Config config = new Config(requestBody.username, requestBody.accessToken, requestBody.endpoint);
        return ResponseEntity.ok(jenkinsService.getJenkinsLatestBuildNumber(requestObj, requestObj.get("Api.serviceName").getAsString()));
    }

    /**
     * request job test report from jenkins endpoint, require build number in parameter
     * @param requestBody config setting used to request jenkins endpoint
     * @return response from jenkins endpoint
     */
    @PostMapping(value = "/jenkins-testReport-semi")
    public ResponseEntity<String> requestJenkinsSemiTestReport(@RequestBody String requestBody){
        JsonObject requestObj = gson.fromJson(requestBody, JsonObject.class);
        System.out.println(gson.toJson(requestBody));
        return ResponseEntity.ok(jenkinsService.getJenkinsTestReport(requestObj, requestObj.get("Api.serviceName").getAsString()));
    }

    /**
     * request jenkins git information
     * @param requestBody
     * @return
     */
    @PostMapping(value = "/jenkins-git-semi")
    public ResponseEntity<String> requestJenkinsSemiGitInfo(@RequestBody String requestBody){
        JsonObject requestObj = gson.fromJson(requestBody, JsonObject.class);
        System.out.println(gson.toJson(requestBody));
        return ResponseEntity.ok(jenkinsService.getJenkinsGitInfo(requestObj, requestObj.get("Api.serviceName").getAsString()));
    }

    /**
     * DEPRECATED<br>
     * fake get method skill, expect request url with parameter concat by '?'
     * @param username
     * @param accessToken
     * @return
     */
    @GetMapping(value = "/fake")
    public ResponseEntity<String> requestFakeGetSkill(@RequestParam(required = false) String username, @RequestParam(required = false) String accessToken){
        return ResponseEntity.ok("[GET Method][RequestParam] username=" + username + ", token=" + accessToken);
    }

    /**
     * DEPRECATED<br>
     * fake get method capability, expect request url with parameter display in url
     * @param username
     * @return
     */
    @GetMapping(value = "/fake-variable/{username}")
    public ResponseEntity<String> requestFakeGetSkillByPathVariable(@PathVariable String username){
        return ResponseEntity.ok("[Get Method][PathVariable] username=" + username);
    }

    /**
     * request jenkins view list
     * @param requestBody
     * @return
     */
    @PostMapping(value = "/jenkins-view-list")
    public ResponseEntity<String> requestJenkinsViewList(@RequestBody String requestBody){
        System.out.println(requestBody);
        return ResponseEntity.ok(jenkinsService.getJenkinsViewList(requestBody));
    }

    /**
     * DEPRECATED<br>
     * fake api, testing only, get fake service detail
     * @param requestBody
     * @return
     */
    @PostMapping(value = "/pseudo-detail")
    public ResponseEntity<String> requestPseudoServiceDetail(@RequestBody String requestBody){
        System.out.println("[DEBUG][controller] pseudo-detail triggered");
        return ResponseEntity.ok(pseudoService.getServiceDetailPartA(requestBody));
    }

    /**
     * DEPRECATED<br>
     * fake api, testing only, get fake service api detail
     * @param requestBody
     * @return
     */
    @PostMapping(value = "/pseudo-api")
    public ResponseEntity<String> requestPseudoServiceAPI(@RequestBody String requestBody){
        System.out.println("[DEBUG][controller] pseudo-api triggered");
        return ResponseEntity.ok(pseudoService.getServiceApiData(requestBody));
    }

    /**
     * DEPRECATED<br>
     * fake api, testing only, aggregate fake service detail
     * @param requestBody
     * @return
     */
    @PostMapping(value = "/pseudo-aggregate")
    public ResponseEntity<String> aggregatePseudoServiceDetail(@RequestBody String requestBody){
        System.out.println("[DEBUG][controller] pseudo-aggregate triggered");
        return ResponseEntity.ok(pseudoService.aggregateServiceDetail(requestBody));
    }

    /**
     * DEPRECATED<br>
     * testing method to print rendering endpoint request body
     * @param requestBody
     * @return
     */
    @PostMapping(value = "/test-input")
    public ResponseEntity<String> testRenderingInput(@RequestBody String requestBody){
        System.out.println("[DEBUG][pseudo] test input raw");
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(gson.fromJson(requestBody, JsonObject.class)));
        System.out.println("-----");
        System.out.println("[DEBUG][controller] test rendering api input");
        return ResponseEntity.ok(pseudoService.printInput(requestBody));
    }

    /**
     * check restler simple test result
     * @param requestBody
     * @return
     */
    @PostMapping(value = "/restler")
    public ResponseEntity<String> restler(@RequestBody String requestBody){
        System.out.println("[DEBUG][restler] restler method triggered");
        return ResponseEntity.ok(pseudoService.checkRESTlerInfo(requestBody));
    }
}
