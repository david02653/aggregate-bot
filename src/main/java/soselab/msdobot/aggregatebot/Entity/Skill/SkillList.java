package soselab.msdobot.aggregatebot.Entity.Skill;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SkillList {
    public ArrayList<Skill> availableSkillList;

    public SkillList(){}

    public int size(){
        return availableSkillList.size();
    }

    /**
     * get available skill by correspond intent
     * @param correspondIntent
     * @return correspond skill array list, otherwise empty array list
     */
    public ArrayList<Skill> getSkill(String correspondIntent){
        for(Skill skill: availableSkillList){
            if(skill.correspondIntent.equals(correspondIntent))
                return new ArrayList<Skill>(Collections.singletonList(skill));
        }
        return new ArrayList<>();
    }

    @Override
    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(availableSkillList);
    }
}
