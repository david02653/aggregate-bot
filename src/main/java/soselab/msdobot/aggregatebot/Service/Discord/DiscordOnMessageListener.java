package soselab.msdobot.aggregatebot.Service.Discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import soselab.msdobot.aggregatebot.Entity.CapabilityReport;
import soselab.msdobot.aggregatebot.Entity.RasaIntent;
import soselab.msdobot.aggregatebot.Service.Orchestrator;
import soselab.msdobot.aggregatebot.Service.RasaService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DiscordOnMessageListener extends ListenerAdapter {
    // todo: handle normal message input received from discord chatroom
    public ConcurrentHashMap<String, Boolean> configMissingList;
    private RasaService rasaService;
    private Orchestrator orchestrator;
    private String testChannelName = "test";

    @Autowired
    public DiscordOnMessageListener(Environment environment, RasaService rasa, Orchestrator orchestrator){
        this.configMissingList = new ConcurrentHashMap<>();
        this.rasaService = rasa;
        this.orchestrator = orchestrator;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event){
        // ignore bot message
        if(event.getAuthor().isBot()) return;
        // ignore message from other channel
        if(!event.getTextChannel().getName().equals(testChannelName)) return;
        // print received message
        printMessage(event);
        /*
        * check message status
        * normal message: rasa analyze
        * config filling message: check given message, fall back to previous message event if config filling process complete
        */
        if(isFillingConfig(event.getAuthor(), event.getMessage())){
            // todo: fill in missing config
        }else{
            // normal message workflow
            normalMessageHandle(event);
        }
    }

    /**
     * handle normal message, send message to rasa and use orchestrator
     * @param event message received event
     */
    private void normalMessageHandle(MessageReceivedEvent event){
        Message receivedMsg = event.getMessage();
        System.out.println("[DEBUG] trigger normal message handle");
        RasaIntent intent = rasaService.intentParsing(rasaService.analyze(receivedMsg.getContentDisplay()));

        // use orchestrator to decide what to do next
        // todo: update orchestrator workflow and change normal message handle workflow
//        HashMap<String, ArrayList<CapabilityReport>> report = orchestrator.capabilitySelector(intent);
//        createReportMessage(report, event);
    }

    /**
     * create report message from config missing report
     * @param finalReport
     */
    public Message createMissingReportMessage(HashMap<String, ArrayList<CapabilityReport>> finalReport){
        if(finalReport.isEmpty()) return null;
        EmbedBuilder embedBuilder = new EmbedBuilder();
        MessageBuilder messageBuilder = new MessageBuilder();
        for(Map.Entry<String, ArrayList<CapabilityReport>> capabilityReport: finalReport.entrySet()){
            String capabilityName = capabilityReport.getKey();
            ArrayList<CapabilityReport> serviceReport = capabilityReport.getValue();
            embedBuilder.setTitle("Missing Config");
            embedBuilder.setDescription("missing config while executing capability '" + capabilityName + "'");
            // todo : might have to many element in single message
            for(CapabilityReport report : serviceReport){
                String serviceName = report.service;
                HashMap<String, HashSet<String>> contextProperties = report.missingContextProperty;
                StringBuilder contextMsg = new StringBuilder();
                for(Map.Entry<String, HashSet<String>> properties: contextProperties.entrySet()){
                    contextMsg.append(properties.getKey() + "." + properties.getValue());
                }
                embedBuilder.addField(serviceName, contextMsg.toString(), false);
            }
        }
        return messageBuilder.setEmbeds(embedBuilder.build()).build();
    }

    /**
     * check if user is filling config
     * @param messageSender message sender
     * @return true if user is trying to fill in missing config, otherwise return false
     */
    private boolean isFillingConfig(User messageSender, Message message){
        // check message format
        // todo: add config update function and message format
        // check if lacking config
        // todo: check config missing list
        // always return false for now
        return false;
    }

    /**
     * print received message detail
     * @param event message received event
     */
    private void printMessage(MessageReceivedEvent event){
        System.out.println(" ================ ");
        System.out.println("[onMessage]: try to print received message.");
        System.out.println("> [author] " + event.getAuthor().getId());
        if(event.isFromGuild()) System.out.println("> [role] " + event.getMember().getRoles());
        else System.out.println("> [role] from private channel, no role found");
        System.out.println("> [message id] " + event.getMessage().getId());
        System.out.println("> [content raw] " + event.getMessage().getContentRaw());
        System.out.println("> [content display] " + event.getMessage().getContentDisplay());
        System.out.println("> [content strip] " + event.getMessage().getContentStripped());
        System.out.println("> [embed] " + event.getMessage().getEmbeds());
        System.out.println("> [attachment] " + event.getMessage().getAttachments());
        if(event.getMessage().getAttachments().size() > 0){
            for(Message.Attachment attachment: event.getMessage().getAttachments()){
                System.out.println("  > [attach id] " + attachment.getId());
                System.out.println("  > [attach type] " + attachment.getContentType());
                System.out.println("  > [attach url] " + attachment.getUrl());
                System.out.println("  > [attach proxy url] " + attachment.getProxyUrl());
            }
        }
    }
}
