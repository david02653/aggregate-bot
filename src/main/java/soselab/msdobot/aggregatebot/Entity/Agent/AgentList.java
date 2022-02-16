package soselab.msdobot.aggregatebot.Entity.Agent;

import com.google.gson.Gson;
import soselab.msdobot.aggregatebot.Entity.Agent.Agent;

import java.util.ArrayList;

public class AgentList {
    public ArrayList<Agent> agentList;

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(agentList);
    }
}
