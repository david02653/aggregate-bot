package soselab.msdobot.aggregatebot.Entity.Service;

public class JenkinsConfig {
    public String username;
    public String accessToken;
    public String endpoint;

    public JenkinsConfig(){}

    public JenkinsConfig(String username, String accessToken, String endpoint){
        this.username = username;
        this.accessToken = accessToken;
        this.endpoint = endpoint;
    }
}
