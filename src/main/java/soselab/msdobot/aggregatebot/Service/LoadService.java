package soselab.msdobot.aggregatebot.Service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;


/**
 * this is a testing class for personal junit testing
 */
@Service
public class LoadService {

    public String testData;

    public LoadService(Environment env){
        testData = env.getProperty("testData");
    }

    public String loadEnv(){
        return testData;
    }
}
