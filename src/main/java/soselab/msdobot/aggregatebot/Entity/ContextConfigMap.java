package soselab.msdobot.aggregatebot.Entity;

import com.google.gson.Gson;

import java.util.HashMap;

public class ContextConfigMap {

    public HashMap<String, HashMap<String, String>> context;
    // todo: add session timeout

    public ContextConfigMap(){
        context = new HashMap<>();
    }

    /**
     * add new properties in context map, if target context does not exist, create new context
     * @param contextName target context name
     * @param propertyKey property key
     * @param propertyValue property value
     */
    public void addContextProperty(String contextName, String propertyKey, String propertyValue){
        if(context.containsKey(contextName)){
            context.get(contextName).put(propertyKey, propertyValue);
        }else{
            HashMap<String, String> temp = new HashMap<>();
            temp.put(propertyKey, propertyValue);
            context.put(contextName, temp);
        }
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
