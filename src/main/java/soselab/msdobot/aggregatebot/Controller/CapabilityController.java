package soselab.msdobot.aggregatebot.Controller;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import soselab.msdobot.aggregatebot.Entity.Service.Config;
import soselab.msdobot.aggregatebot.Service.JenkinsService;

/**
 * declare all skills unit
 */
@RestController
@RequestMapping(value = "/capability")
public class CapabilityController {

    private final JenkinsService jenkinsService;

    @Autowired
    public CapabilityController(JenkinsService jenkinsService){
        this.jenkinsService = jenkinsService;
    }

    /**
     * skill endpoint of jenkins health report
     * expect orchestrator to call this endpoint when correspond intent detected
     * @param requestBody
     */
    @PostMapping(value = "/jenkins-health")
    public ResponseEntity<String> requestJenkinsHealthData(@RequestBody Config requestBody){
        System.out.println(new Gson().toJson(requestBody));
//        Config config = new Config(requestBody.username, requestBody.accessToken, requestBody.endpoint);
        return ResponseEntity.ok(jenkinsService.getJenkinsHealthReport(requestBody, requestBody.targetService));
    }

    /**
     * request job test report from jenkins endpoint, this version only need to assign target service name
     * @param requestBody config setting used to request jenkins endpoint
     * @return response from jenkins endpoint
     */
    @PostMapping(value = "/jenkins-testReport")
    public ResponseEntity<String> requestJenkinsTestReportData(@RequestBody Config requestBody){
        System.out.println(new Gson().toJson(requestBody));
//        Config config = new Config(requestBody.username, requestBody.accessToken, requestBody.endpoint);
        return ResponseEntity.ok(jenkinsService.getDirectJenkinsTestReport(requestBody, requestBody.targetService));
    }

    /**
     * request latest build number from jenkins endpoint
     * @param requestBody config setting used to request jenkins endpoint
     * @return response from jenkins endpoint
     */
    @PostMapping(value = "/jenkins-buildNumber")
    public ResponseEntity<String> requestJenkinsBuildNumber(@RequestBody Config requestBody){
        System.out.println(new Gson().toJson(requestBody));
//        Config config = new Config(requestBody.username, requestBody.accessToken, requestBody.endpoint);
        return ResponseEntity.ok(jenkinsService.getJenkinsLatestBuildNumber(requestBody, requestBody.targetService));
    }

    /**
     * request job test report from jenkins endpoint, require build number in parameter
     * @param requestBody config setting used to request jenkins endpoint
     * @return response from jenkins endpoint
     */
    @PostMapping(value = "/jenkins-testReport-semi")
    public ResponseEntity<String> requestJenkinsSemiTestReport(@RequestBody Config requestBody){
        System.out.println(new Gson().toJson(requestBody));
        return ResponseEntity.ok(jenkinsService.getJenkinsTestReport(requestBody, requestBody.targetService));
    }

    /**
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
     * fake get method skill, expect request url with parameter display in url
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
        return ResponseEntity.ok(jenkinsService.getJenkinsViewList(requestBody));
    }
}
