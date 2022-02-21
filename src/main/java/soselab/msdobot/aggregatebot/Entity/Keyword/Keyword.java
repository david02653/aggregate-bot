package soselab.msdobot.aggregatebot.Entity.Keyword;

import com.google.gson.Gson;

import java.util.ArrayList;

public class Keyword {
    public ArrayList<String> input;
    public ArrayList<String> output;

    public Keyword(){
    }

    @Override
    public String toString(){
        return new Gson().toJson(this.input);
    }
}
