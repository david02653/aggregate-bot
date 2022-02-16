package soselab.msdobot.aggregatebot.Entity;

import com.google.gson.Gson;

public class RasaIntent {
    public String intent;
    public String jobName;

    public RasaIntent(){}
    public RasaIntent(String intent, String jobName){
        this.intent = intent;
        this.jobName = jobName;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getIntent() {
        return intent;
    }

    public String getJobName() {
        return jobName;
    }

    @Override
    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
