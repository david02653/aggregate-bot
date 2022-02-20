package soselab.msdobot.aggregatebot.Entity.Service;

public class Config {
    public String username;
    public String accessToken;
    public String endpoint;

    public Config(){}

    public Config(String username, String accessToken, String endpoint){
        this.username = username;
        this.accessToken = accessToken;
        this.endpoint = endpoint;
    }
}
