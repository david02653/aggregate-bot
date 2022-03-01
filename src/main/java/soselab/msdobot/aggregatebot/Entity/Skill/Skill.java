package soselab.msdobot.aggregatebot.Entity.Skill;

import java.util.ArrayList;

public class Skill {
    public String name;
    public String type;
    public String order;
    public String description;
    public String method;
    public String correspondIntent;
    public String endpoint;
    public ArrayList<String> input;
    public SkillOutput output;

    public Skill(){}
}
