package soselab.msdobot.aggregatebot.Entity.Service;

import com.google.gson.GsonBuilder;

public class ContextConfig {

    public String name;
    public String value;

    public ContextConfig(){
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
