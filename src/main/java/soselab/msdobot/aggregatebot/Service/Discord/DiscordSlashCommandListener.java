package soselab.msdobot.aggregatebot.Service.Discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import soselab.msdobot.aggregatebot.Service.Orchestrator;

@Service
public class DiscordSlashCommandListener extends ListenerAdapter {

    private final String ACTIVE_CHANNEL_ID;
    private final Orchestrator orchestrator;

    @Autowired
    public DiscordSlashCommandListener(Environment env, Orchestrator orchestrator){
        this.ACTIVE_CHANNEL_ID = env.getProperty("discord.testChannel.id");
        this.orchestrator = orchestrator;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event){
        System.out.println("[slash command] config update command triggered.");
        event.deferReply().queue();
        /* check channel */
        if(!event.getChannel().getId().equals(ACTIVE_CHANNEL_ID)) {
            event.getHook().sendMessage("permission denied.").setEphemeral(true).queue();
        }
        if(event.getName().equals("update_config")){
            String serviceName, context, property, value;
            serviceName = event.getOption("service").getAsString();
            property = event.getOption("property").getAsString();
            value = event.getOption("value").getAsString();
            if(event.getOption("context") == null)
                context = "general";
            else
                context = event.getOption("context").getAsString();
            printConfigUpdateInfo(serviceName, context, property, value);
            orchestrator.addServiceSessionConfig(serviceName, context, property, value);
            orchestrator.removeMissingConfig(serviceName, context, property);
            event.getHook().sendMessage(createConfigInfoMessage(serviceName, context, property, value)).setEphemeral(false).queue();
        }
    }

    /**
     * print received config detail
     * @param service service name
     * @param context config context
     * @param property config property name
     * @param value config property value
     */
    private void printConfigUpdateInfo(String service, String context, String property, String value){
        System.out.println("===");
        System.out.println("[DEBUG] try to update config");
        System.out.println("service: " + service);
        System.out.println("context: " + context);
        System.out.println("property name: " + property);
        System.out.println("property value: " + value);
        System.out.println("===");
    }

    /**
     * build config detail message
     * @param service service name
     * @param context config context
     * @param property config property name
     * @param value config property value
     * @return config detail message
     */
    private Message createConfigInfoMessage(String service, String context, String property, String value){
        MessageBuilder builder = new MessageBuilder();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setDescription("request received, update config with data below.");
        embedBuilder.addField("Service Name", service, true);
        embedBuilder.addField("Context Name", context, true);
        embedBuilder.addField("Property Name", property, false);
        embedBuilder.addField("Property Value", value, false);
        builder.setEmbeds(embedBuilder.build());
        return builder.build();
    }
}
