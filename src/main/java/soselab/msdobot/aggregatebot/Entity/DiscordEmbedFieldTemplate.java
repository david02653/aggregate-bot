package soselab.msdobot.aggregatebot.Entity;

import com.google.gson.Gson;

public class DiscordEmbedFieldTemplate {

    private String name;
    private String value;

    public DiscordEmbedFieldTemplate(){
    }
    public DiscordEmbedFieldTemplate(String name, String value){
        setName(name);
        setValue(value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
