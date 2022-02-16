package soselab.msdobot.aggregatebot.Service;

import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import soselab.msdobot.aggregatebot.Entity.RasaIntent;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * natural language processing component
 * evaluate user utterance's intent
 */
@Service
public class RasaService {

    private final String rasaEndpoint;

    @Autowired
    public RasaService(Environment env){
        rasaEndpoint = env.getProperty("rasa.endpoint");
    }

    /**
     * fake method to determine user intent
     * only detect health report and test report intent for now via hardcoded code block
     */
    public void fakeNLPComponent(String utterance){
        System.out.println(">>> fake test");
        Pattern healthPattern = Pattern.compile("^health.*$");
        Pattern testPattern = Pattern.compile("^test report.*$");
    }

    public RasaIntent restrictedIntentParsing(String analyzeResult){
        RasaIntent intentSet = new RasaIntent();
        if(analyzeResult.contains("ask_job_health_report") || analyzeResult.contains("ask_job_test_report")) {
            Pattern jobNameExtractor = Pattern.compile("'jobName': '(.*?)'");
            Matcher matcher = jobNameExtractor.matcher(analyzeResult);
            if (matcher.find())
                intentSet.setJobName(matcher.group(1));
            if(analyzeResult.contains("health"))
                intentSet.setIntent("ask_job_health_report");
            else
                intentSet.setIntent("ask_job_test_report");
            return intentSet;
        }
        return null;
    }

    public String analyze(String utterance){
        RestTemplate template = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("sender", "test");
        requestBody.addProperty("message", utterance);
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        ResponseEntity<String> resp = template.exchange(rasaEndpoint, HttpMethod.POST, entity, String.class);
        System.out.println("[DEBUG] rasa analyze result: " + resp.getBody());
        return resp.getBody();
    }
}
