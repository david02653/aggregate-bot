package soselab.msdobot.aggregatebot.Entity.BigIntent;

import com.google.gson.Gson;
import soselab.msdobot.aggregatebot.Entity.Skill.Skill;
import soselab.msdobot.aggregatebot.Entity.Skill.SkillList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class BigIntent {

    public String name;
    public String correspondIntent;
    public ArrayList<Skill> sequencedSkillList;

    public BigIntent(){
    }

    public ArrayList<Skill> getSequencedSkillList(){
        sortSequencedSkillList();
        return new ArrayList<>(sequencedSkillList);
    }

    public void sortSequencedSkillList(){
        this.sequencedSkillList.sort(Comparator.comparingInt((Skill skill) -> Integer.parseInt(skill.order)));
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
