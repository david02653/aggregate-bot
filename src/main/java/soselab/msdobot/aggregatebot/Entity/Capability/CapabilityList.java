package soselab.msdobot.aggregatebot.Entity.Capability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;

public class CapabilityList {
    public ArrayList<Capability> availableCapabilityList;

    public CapabilityList(){}

    public int size(){
        return availableCapabilityList.size();
    }

//    /**
//     * create hashmap of currently available capability list
//     */
//    public void createCapabilityMap(){
//        HashMap<String, Capability> map = new HashMap<>();
//        for(Capability capability: availableCapabilityList){
//            map.put(capability.name, capability);
//        }
//        this.capabilityHashMap = map;
//    }

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

    /**
     * get complete capability data list in upper intent capability list
     * @param semiCapabilityList
     * @return
     */
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
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(availableCapabilityList);
    }
}
