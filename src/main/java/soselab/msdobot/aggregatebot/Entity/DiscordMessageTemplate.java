package soselab.msdobot.aggregatebot.Entity;

import com.google.gson.Gson;

import java.util.ArrayList;

public class DiscordMessageTemplate {

    private String mainMessage;
    private ArrayList<DiscordEmbedTemplate> embedList;

    public DiscordMessageTemplate(){
        this.embedList = new ArrayList<>();
    }

    /**
     * create new discord message with simple string message
     * @param message message context
     */
    public DiscordMessageTemplate(String message){
        this.embedList = new ArrayList<>();
        this.mainMessage = message;
    }

    public String getMainMessage() {
        return mainMessage;
    }

    public ArrayList<DiscordEmbedTemplate> getEmbedList() {
        return embedList;
    }

    public void setMainMessage(String message){
        this.mainMessage = mainMessage;
    }

    /**
     * set embed object in discord message<br>
     * note that due to discord API limitation, if given template size is bigger than 10<br>
     * element with index higher than 10 will be ignored
     * @param templates
     */
    public void setEmbedList(ArrayList<DiscordEmbedTemplate> templates){
        if(templates.size() <= 10)
            this.embedList = templates;
        else{
            for(int i = 0; i < 10; i++){
                this.embedList.add(templates.get(i));
            }
        }
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
