package soselab.msdobot.aggregatebot.Exception;

public class JenkinsRequestException extends Exception {
    public JenkinsRequestException(String errorMsg){
        super(errorMsg);
    }
    public JenkinsRequestException(){}
}
