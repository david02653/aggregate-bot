package soselab.msdobot.aggregatebot.Entity.Service;

import com.google.gson.Gson;

public class Config {
    public String username;
    public String accessToken;
    public String endpoint;
    public String buildNumber;
    public String targetService;

    public Config(){}

    /**
     * simple config constructor for jenkins data access
     * @param username jenkins username
     * @param accessToken jenkins access token
     * @param endpoint jenkins endpoint
     */
    public Config(String username, String accessToken, String endpoint){
        this.username = username;
        this.accessToken = accessToken;
        this.endpoint = endpoint;
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
