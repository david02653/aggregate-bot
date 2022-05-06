package soselab.msdobot.aggregatebot.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * fake tool service
 */
@Service
public class PseudoService {

    private final String MEMBER = "Member";
    private final String GAME = "Game";
    private final String TOKEN_1 = "12345";
    private final String TOKEN_2 = "56789";

    private Gson gson;
    public PseudoService(){
        gson = new Gson();
    }

    public String getServiceDetailPartA(String request){
        JsonObject requestObj = gson.fromJson(request, JsonObject.class);
        String serviceName = requestObj.get("Api.serviceName").getAsString();
        String accessToken = requestObj.get("Api.accessToken").getAsString();
        if(accessToken.equals(TOKEN_1)){
            if(serviceName.equals(MEMBER))
                return "general member service API";
            if(serviceName.equals(GAME))
                return "simple game service API";
        }
        return "ERROR";
    }

    public void getServiceDetailPartB(){
    }

    public String getServiceApiData(String request){
        JsonObject requestObj = gson.fromJson(request, JsonObject.class);
        String serviceName = requestObj.get("Api.serviceName").getAsString();
        String accessToken = requestObj.get("Api.accessToken").getAsString();
        if(accessToken.equals(TOKEN_2)){
            if(serviceName.equals(MEMBER))
                return "5";
            if(serviceName.equals(GAME))
                return "27";
        }
        return "0";
    }

    public void analyzeServiceApiData(){
    }

    public String aggregateServiceDetail(String request){
        JsonObject requestObj = gson.fromJson(request, JsonObject.class);
        String rawServiceDetails = requestObj.get("serviceDetail").getAsString();
        String rawApiDetails = requestObj.get("apiDetail").getAsString();
        Type hashMapType = new TypeToken<HashMap<String, String>>(){}.getType();
        HashMap<String, String> serviceDetails = gson.fromJson(rawServiceDetails, hashMapType);
        HashMap<String, String> apiDetails = gson.fromJson(rawApiDetails, hashMapType);
        // expect {"Member": "member detail", "Game": "game detail"}
        // expect {"Member": "5", "Game": "27"}
        JsonArray resultKeyArray = new JsonArray();
        JsonArray resultValueArray = new JsonArray();
        for(java.util.Map.Entry<String, String> serviceDetail: serviceDetails.entrySet()){
            String serviceName = serviceDetail.getKey();
            String detail = serviceDetail.getValue();
            String apiDetail = apiDetails.get(serviceName);
            resultKeyArray.add(serviceName);
            resultValueArray.add(combinePseudoServiceDetail(detail, apiDetail));
        }
        JsonArray report = new JsonArray();
        report.add(resultKeyArray);
        report.add(resultValueArray);
        return gson.toJson(report);
    }

    private String combinePseudoServiceDetail(String detail, String apiDetail){
        return detail + " , has " + apiDetail + " kinds of api";
    }

    public void renderServiceDetail(){
    }
}
