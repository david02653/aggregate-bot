package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;

public class CapabilityList {
    public ArrayList<Capability> availableCapabilityList;

    public CapabilityList(){}

    public int size(){
        return availableCapabilityList.size();
    }

    /**
     * get available capability by correspond intent
     * @param correspondIntent correspond atomic intent
     * @return correspond capability array list, otherwise empty array list
     */
    public ArrayList<Capability> getCapability(String correspondIntent){
        for(Capability capability : availableCapabilityList){
            if(capability.atomicIntent.equals(correspondIntent))
                return new ArrayList<Capability>(Collections.singletonList(capability));
        }
        return new ArrayList<>();
    }

    public ArrayList<Capability> getCompleteCapability(ArrayList<Capability> semiCapabilityList){
        ArrayList<Capability> resultList = new ArrayList<>();
        for(Capability semiCapability : semiCapabilityList){
            for(Capability capability : availableCapabilityList){
                if(capability.name.equals(semiCapability.name)){
                    resultList.add(capability);
                    break;
                }
            }
        }
        return resultList;
    }

    @Override
    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(availableCapabilityList);
    }
}
