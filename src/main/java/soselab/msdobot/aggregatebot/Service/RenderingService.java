package soselab.msdobot.aggregatebot.Service;

import com.google.gson.*;
import com.jakewharton.fliptables.FlipTable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import soselab.msdobot.aggregatebot.Entity.CapabilityReport;
import soselab.msdobot.aggregatebot.Entity.DiscordEmbedFieldTemplate;
import soselab.msdobot.aggregatebot.Entity.DiscordEmbedTemplate;
import soselab.msdobot.aggregatebot.Entity.DiscordMessageTemplate;
import soselab.msdobot.aggregatebot.Entity.Service.Service;

import java.sql.Array;
import java.util.*;

public class RenderingService implements RenderingTemplate {

    HashMap<String, String> aggregateResult;
    ArrayList<Service> serviceList;

    public RenderingService(){}
    public RenderingService(ArrayList<Service> serviceList, JsonArray aggregateResult){
        this.aggregateResult = new HashMap<>();
        this.serviceList = serviceList;
        parseAggregateReport(aggregateResult);
    }

    @Override
    public void parseAggregateReport(JsonArray aggregateReport) {
        JsonArray keyArray = aggregateReport.get(AGGREGATE_RESULT_KEY).getAsJsonArray();
        JsonArray valueArray = aggregateReport.get(AGGREGATE_RESULT_VALUE).getAsJsonArray();
        int count = 0;
        for(JsonElement key: keyArray){
            aggregateResult.put(key.getAsString(), valueArray.get(count).getAsString());
            count++;
        }
    }

    @Override
    public DiscordMessageTemplate defaultRendering(HashMap<String, String> aggregateData, HashMap<String, HashMap<String, String>> specificAggregateData, HashMap<String, HashMap<String, String>> properties) {
        var resultTemplate = new DiscordMessageTemplate();
        var embedList = new ArrayList<DiscordEmbedTemplate>();
        // handle aggregate data
        var aggregateDataTemplate = new DiscordEmbedTemplate();
        aggregateDataTemplate.setTitle("Aggregate Data");
        var aggregateFieldList = new ArrayList<DiscordEmbedFieldTemplate>();
        for(Map.Entry<String, String> data: aggregateData.entrySet()){
            var field = new DiscordEmbedFieldTemplate(data.getKey(), data.getValue());
            aggregateFieldList.add(field);
        }
        aggregateDataTemplate.setFieldList(aggregateFieldList);
        embedList.add(aggregateDataTemplate);
        // handle specific aggregate data
        for(Map.Entry<String, HashMap<String, String>> specificData: specificAggregateData.entrySet()){
            var fieldList = new ArrayList<DiscordEmbedFieldTemplate>();
            var specificAggregateDataTemplate = new DiscordEmbedTemplate();
            specificAggregateDataTemplate.setTitle(specificData.getKey());
            for(Map.Entry<String, String> serviceData: specificData.getValue().entrySet()){
                var field = new DiscordEmbedFieldTemplate(serviceData.getKey(), serviceData.getValue());
                fieldList.add(field);
            }
            specificAggregateDataTemplate.setFieldList(fieldList);
            embedList.add(specificAggregateDataTemplate);
        }
        // handle normal properties
        for(Map.Entry<String, HashMap<String, String>> property: properties.entrySet()){
            var fieldList = new ArrayList<DiscordEmbedFieldTemplate>();
            var propertyTemplate = new DiscordEmbedTemplate();
            propertyTemplate.setTitle(property.getKey());
            for(Map.Entry<String, String> serviceData: property.getValue().entrySet()){
                var field = new DiscordEmbedFieldTemplate(serviceData.getKey(), serviceData.getValue());
                fieldList.add(field);
            }
            propertyTemplate.setFieldList(fieldList);
            embedList.add(propertyTemplate);
        }
        // return result message template
        resultTemplate.setEmbedList(embedList);
        return resultTemplate;
    }

    /**
     * parse aggregate report to simple ascii art table
     * @return simple ascii art table
     */
    public String parseToSimpleAsciiArtTable(){
//        ArrayList<String> header = new ArrayList<>();
//        ArrayList<ArrayList<String>> body;
        String[] header = {"key", "value"};
        String[][] body = new String[aggregateResult.size()][2];
        int count = 0;
        for(Map.Entry<String, String> data: aggregateResult.entrySet()){
            String key = data.getKey();
            String value = data.getValue();
            body[count][0] = key;
            if(value.length() > 150)
                body[count][1] = value.substring(0, 100);
            else
                body[count][1] = value;
            count++;
        }
        String resultTable = FlipTable.of(header, body);
        System.out.println(resultTable);
        return resultTable;
    }

    /**
     * check if given template string has legal discord template message format
     * @param rawTemplate rendering result
     * @return true if given rendering result has legal format, otherwise return false
     */
    public static boolean templateFormatCheck(String rawTemplate){
        Gson gson = new Gson();
        try{
            var msg = gson.fromJson(rawTemplate, DiscordMessageTemplate.class);
            return true;
        }catch (JsonParseException e){
            System.out.println("[DEBUG][rendering result] failed to parse rendering result");
            return false;
        }
    }

    /**
     * parse rendering capability request result into Discord Message format
     * @param rawTemplate request result
     * @return discord message of rendering result
     */
    public static DiscordMessageTemplate parseRenderingResult(String rawTemplate){
        // parse rendering result to discord message template
        Gson gson = new Gson();
        DiscordMessageTemplate msg;
        msg = gson.fromJson(rawTemplate, DiscordMessageTemplate.class);
        return msg;
    }

    /**
     * create discord message from message template
     * @param messageTemplate message template
     * @return discord message
     */
    public static Message createDiscordMessage(DiscordMessageTemplate messageTemplate){
        // create discord message from message template
        var messageBuilder = new MessageBuilder();
        messageBuilder.append(messageTemplate.getMainMessage());
        var embedList = new ArrayList<MessageEmbed>();
        for(DiscordEmbedTemplate embed: messageTemplate.getEmbedList()){
            var embedBuilder = new EmbedBuilder();
            if(embed.getTitle() != null)
                embedBuilder.setTitle(embed.getTitle());
            if(embed.getDescription() != null)
                embedBuilder.setDescription(embed.getDescription());
            if(embed.getImageLink() != null)
                embedBuilder.setImage(embed.getImageLink());
            for(DiscordEmbedFieldTemplate field: embed.getFieldList()){
                if(field.getName() != null && field.getValue() != null)
                    embedBuilder.addField(field.getName(), field.getValue(), false);
            }
            embedList.add(embedBuilder.build());
        }
        messageBuilder.setEmbeds(embedList);
        return messageBuilder.build();
    }

    /**
     * create discord message with simple string message
     * @param message message content
     * @return discord message of given content
     */
    public static Message createSimpleMessage(String message){
        // create discord message with simple string message
        var messageBuilder = new MessageBuilder();
        if(message.length() <= 2000)
            messageBuilder.appendCodeBlock(message, "");
        else{
            // todo: handle over size message
            messageBuilder.appendCodeBlock(message.substring(0, 1999), "");
        }
        return messageBuilder.build();
    }

    /**
     * create missing report message from capability reports<br>
     * get missing property report from each capability report<br>
     * format: context - property[]
     * @param capabilityReports
     * @return
     */
    public Message createMissingReportMessage(ArrayList<CapabilityReport> capabilityReports){
        var template = new DiscordMessageTemplate();
        var embedList = new ArrayList<DiscordEmbedTemplate>();
        for(CapabilityReport capabilityReport: capabilityReports){
            HashMap<String, HashSet<String>> tempReport = capabilityReport.missingContextProperty;
            // add all missing config report in result message
            var embed = new DiscordEmbedTemplate();
            var fieldList = new ArrayList<DiscordEmbedFieldTemplate>();
            for(Map.Entry<String, HashSet<String>> report: tempReport.entrySet()){
                var field = new DiscordEmbedFieldTemplate();
                field.setName(report.getKey());
                field.setValue(getMissingProperties(report.getValue()));
                fieldList.add(field);
            }
            embed.setFieldList(fieldList);
            embedList.add(embed);
        }
        template.setMainMessage("Missing Config Detected");
        template.setEmbedList(embedList);
        return createDiscordMessage(template);
    }

    /**
     * concat all missing properties will '\n'
     * @param properties
     * @return
     */
    private String getMissingProperties(HashSet<String> properties){
        var start = true;
        var builder = new StringBuilder();
        for(String property: properties){
            if(start){
                builder.append(property);
                start = false;
            }else
                builder.append("\n").append(property);
        }
        return builder.toString();
    }

    /**
     * create error message from missing properties<br>
     * format: context - property[]
     * @param missingProperties missing properties
     * @return missing context property message
     */
    public static Message createMissingReportMessage(HashMap<String, HashSet<String>> missingProperties){
        String[] header = {"Context", "Missing Property"};
        String[][] body = new String[missingProperties.size()][2];
        var index = 0;
        for(Map.Entry<String, HashSet<String>> contextProperties: missingProperties.entrySet()){
            String context = contextProperties.getKey();
            HashSet<String> properties = contextProperties.getValue();
            var propertyList = new StringBuilder();
            var start = true;
            for(String property: properties){
                if(start) {
                    propertyList = new StringBuilder(property);
                    start = false;
                }else
                    propertyList.append("\n").append(property);
            }
            body[index][0] = context;
            body[index][1] = propertyList.toString();
            index++;
        }
        var builder = new MessageBuilder();
        builder.appendCodeBlock(FlipTable.of(header, body), "");
        return builder.build();
    }

    public Message createReportMessage(CapabilityReport report){
        // todo: create discord message with capability report
        return null;
    }
    /**
     * DEPRECATED<br>
     * create report message from config missing report<br>
     * assume given missing report contains multiple capability execute report
     * @param missingReport missing properties report
     */
//    public static Message createMissingReportMessage(HashMap<String, ArrayList<CapabilityReport>> missingReport){
//        if(missingReport.isEmpty()) return null;
//        EmbedBuilder embedBuilder = new EmbedBuilder();
//        MessageBuilder messageBuilder = new MessageBuilder();
//        for(Map.Entry<String, ArrayList<CapabilityReport>> capabilityReport: missingReport.entrySet()){
//            String capabilityName = capabilityReport.getKey();
//            ArrayList<CapabilityReport> serviceReport = capabilityReport.getValue();
//            embedBuilder.setTitle("Missing Config");
//            embedBuilder.setDescription("missing config while executing capability '" + capabilityName + "'");
//            // might have to many element in single message
//            for(CapabilityReport report : serviceReport){
//                String serviceName = report.service;
//                HashMap<String, HashSet<String>> contextProperties = report.missingContextProperty;
//                StringBuilder contextMsg = new StringBuilder();
//                for(Map.Entry<String, HashSet<String>> properties: contextProperties.entrySet()){
//                    contextMsg.append(properties.getKey() + "." + properties.getValue());
//                }
//                embedBuilder.addField(serviceName, contextMsg.toString(), false);
//            }
//        }
//        return messageBuilder.setEmbeds(embedBuilder.build()).build();

//    }
}