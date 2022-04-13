package soselab.msdobot.aggregatebot.Exception;

public class NoSessionFoundException extends Exception {
    public NoSessionFoundException(String errMsg){
        super(errMsg);
    }

    public NoSessionFoundException(){
    }
}
