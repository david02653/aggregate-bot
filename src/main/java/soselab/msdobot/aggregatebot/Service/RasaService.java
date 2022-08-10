package soselab.msdobot.aggregatebot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

    /**
     * use regex to extract rasa analyze result<br>
     * note that this method can only recognize 'ask_job_health_report' and 'ask_job_test_report'
     * @param analyzeResult rasa analyze result
     * @return rasa intent
     */
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

    /**
     * use regex to extract rasa analyze result<br>
     * note that this method can ONLY extract 'intent' and 'jobName' for now
     * @param analyzeResult rasa analyze result
     * @return rasa intent
     */
    public RasaIntent intentParsing(String analyzeResult){
        RasaIntent intentSet = new RasaIntent();
        Pattern jobNameExtractor = Pattern.compile("'jobName': '(.*?)'");
        Matcher matcher = jobNameExtractor.matcher(analyzeResult);
        if(matcher.find())
            intentSet.setJobName(matcher.group(1));
        Pattern intentExtractor = Pattern.compile("'intent': '(.*?)'");
        matcher = intentExtractor.matcher(analyzeResult);
        if(matcher.find())
            intentSet.setIntent(matcher.group(1));
        return intentSet;
    }

    /**
     * send message to rasa endpoint to analyze intent
     * @param utterance
     * @return
     */
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

    /**
     * parse analyzed rasa intent
     * @param raw
     * @return
     */
    public RasaIntent directParse(String raw){
//        System.out.println("[dirPar]");
//        System.out.println(raw);
//        System.out.println("---");
        var gson = new Gson();
        JsonArray obj = gson.fromJson(raw, JsonArray.class);
//        System.out.println(gson.toJson(obj));
//        System.out.println("---");
        RasaIntent result = new RasaIntent();
        JsonObject custom = obj.get(0).getAsJsonObject().get("custom").getAsJsonObject();
//        System.out.println(gson.toJson(custom));
//        System.out.println("---");
        result.setIntent(custom.get("intent").getAsString());
        result.setJobName(custom.get("jobName").getAsString());
        return result;
    }
}
