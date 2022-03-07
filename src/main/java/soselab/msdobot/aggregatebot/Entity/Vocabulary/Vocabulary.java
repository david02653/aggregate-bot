package soselab.msdobot.aggregatebot.Entity.Vocabulary;

import com.google.gson.Gson;

import java.util.ArrayList;

public class Vocabulary {
    public ArrayList<String> input;
    public ArrayList<String> output;

    public Vocabulary(){
    }

    @Override
    public String toString(){
        return new Gson().toJson(this.input);
    }
}
