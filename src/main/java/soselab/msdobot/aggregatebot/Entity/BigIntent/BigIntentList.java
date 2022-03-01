package soselab.msdobot.aggregatebot.Entity.BigIntent;

import com.google.gson.Gson;
import soselab.msdobot.aggregatebot.Entity.Skill.Skill;

import java.util.ArrayList;

public class BigIntentList {

    public ArrayList<BigIntent> crossSkillList;

    public BigIntentList(){
    }

    public ArrayList<Skill> getSkillList(String intent){
        for(BigIntent bigIntent: crossSkillList){
            if(bigIntent.correspondIntent.equals(intent)) {
                return bigIntent.getSequencedSkillList();
            }
        }
        return new ArrayList<>();
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
