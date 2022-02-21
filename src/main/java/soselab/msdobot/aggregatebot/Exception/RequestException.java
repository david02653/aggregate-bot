package soselab.msdobot.aggregatebot.Exception;

public class RequestException extends Exception {
    public RequestException(String errorMsg){
        super(errorMsg);
    }
    public RequestException(){}
}
