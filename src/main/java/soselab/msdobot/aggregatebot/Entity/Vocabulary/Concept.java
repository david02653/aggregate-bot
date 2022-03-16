package soselab.msdobot.aggregatebot.Entity.Vocabulary;

import com.google.gson.Gson;

import java.util.ArrayList;

public class Concept {
    public String conceptName;
    public ArrayList<String> usedVocabulary;

    public Concept(){
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
