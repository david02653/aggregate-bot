package soselab.msdobot.aggregatebot.Entity;

import com.google.gson.Gson;

import java.util.ArrayList;

public class DiscordEmbedTemplate {

    private String title;
    private String titleLink;
    private String imageLink;
    private String description;
    private ArrayList<DiscordEmbedFieldTemplate> fieldList;

    public DiscordEmbedTemplate(){
        this.fieldList = new ArrayList<>();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    public void setTitleLink(String titleLink) {
        this.titleLink = titleLink;
    }

    /**
     * set embed message field<br>
     * note that due to Discord API limitation, only 25 field is allowed<br>
     * element has bigger index than 25 will be ignored
     * @param fieldList
     */
    public void setFieldList(ArrayList<DiscordEmbedFieldTemplate> fieldList) {
        if(fieldList.size() <= 25)
            this.fieldList = fieldList;
        else{
            for(int i=0; i<25; i++)
                this.fieldList.add(fieldList.get(i));
        }
    }

    public String getTitle() {
        return title;
    }

    public String getTitleLink() {
        return titleLink;
    }

    public String getImageLink() {
        return imageLink;
    }

    public String getDescription() {
        return description;
    }

    public ArrayList<DiscordEmbedFieldTemplate> getFieldList() {
        return fieldList;
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
