package soselab.msdobot.aggregatebot.Entity.Vocabulary;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;

public class Context {

    public String contextName;
    public ArrayList<String> properties;

    public Context(){
    }

    @Override
    public String toString(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
