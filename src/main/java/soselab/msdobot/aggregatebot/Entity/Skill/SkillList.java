package soselab.msdobot.aggregatebot.Entity.Skill;

import com.google.gson.Gson;

import java.util.ArrayList;

public class SkillList {
    public ArrayList<Skill> availableSkillList;

    public SkillList(){}

    public int size(){
        return availableSkillList.size();
    }

    /**
     * get available skill by correspond intent
     * @param correspondIntent
     * @return correspond skill, otherwise null
     */
    public Skill getSkill(String correspondIntent){
        for(Skill skill: availableSkillList){
            if(skill.correspondIntent.equals(correspondIntent))
                return skill;
        }
        return null;
    }

    @Override
    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(availableSkillList);
    }
}
