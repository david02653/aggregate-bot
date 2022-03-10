package soselab.msdobot.aggregatebot.Entity.Vocabulary;

import com.google.gson.Gson;

import java.util.ArrayList;

public class CustomMapping {
    public String mappingName;
    public String description;
    public String schema;
    public ArrayList<String> usedVocabulary;

    public CustomMapping(){
        this.usedVocabulary = new ArrayList<>();
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
