package soselab.msdobot.aggregatebot.Entity;

import com.google.gson.Gson;

import java.util.HashMap;

public class SkillConfig {

    public HashMap<String, String> content;
    // todo: add session timeout

    public SkillConfig(){
        content = new HashMap<>();
    }

    public void addData(String key, String value){
        if(key.equals("targetService"))
            return;
        this.content.put(key, value);
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
