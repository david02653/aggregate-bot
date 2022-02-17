package soselab.msdobot.aggregatebot.Controller;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import soselab.msdobot.aggregatebot.Entity.JenkinsRequest;
import soselab.msdobot.aggregatebot.Entity.Service.JenkinsConfig;
import soselab.msdobot.aggregatebot.Service.JenkinsService;

import javax.websocket.server.PathParam;

/**
 * declare all skills unit
 */
@RestController
@RequestMapping(value = "/skill")
public class SkillController {

    private final JenkinsService jenkinsService;

    @Autowired
    public SkillController(JenkinsService jenkinsService){
        this.jenkinsService = jenkinsService;
    }

    /**
     * skill endpoint of jenkins health report
     * expect orchestrator to call this endpoint when correspond intent detected
     * @param requestBody
     */
    @PostMapping(value = "/jenkins-health")
    public ResponseEntity<String> requestJenkinsHealthData(@RequestBody JenkinsRequest requestBody){
        System.out.println(new Gson().toJson(requestBody));
        JenkinsConfig config = new JenkinsConfig(requestBody.username, requestBody.accessToken, requestBody.endpoint);
        return ResponseEntity.ok(jenkinsService.getJenkinsHealthReport(config, requestBody.targetService));
    }

    @PostMapping(value = "/jenkins-testReport")
    public ResponseEntity<String> requestJenkinsTestReportData(@RequestBody JenkinsRequest requestBody){
        System.out.println(new Gson().toJson(requestBody));
        JenkinsConfig config = new JenkinsConfig(requestBody.username, requestBody.accessToken, requestBody.endpoint);
        return ResponseEntity.ok(jenkinsService.getJenkinsTestReport(config, requestBody.targetService));
    }

    @GetMapping(value = "/fake")
    public ResponseEntity<String> requestFakeGetSkill(@RequestParam(required = false) String username, @RequestParam(required = false) String accessToken){
        return ResponseEntity.ok("[GET Method] username=" + username + ", token=" + accessToken);
    }
}
