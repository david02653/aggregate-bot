package soselab.msdobot.aggregatebot.Entity.Capability;

import java.util.ArrayList;

/**
 * declare output data format and details<br>
 * note that only 'plainText' and 'json' format are valid for now
 */
public class CapabilityOutput {
    public String type; // what is the output data format
    public String dataLabel; // if it is 'plainText', what is the data label
    public ArrayList<JsonInfo> jsonInfo; // json data detail

    public CapabilityOutput(){}
}
