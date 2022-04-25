package soselab.msdobot.aggregatebot.Service.Discord;

import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class DiscordGeneralEventListener extends ListenerAdapter {

    private final String SERVER_ID;

    @Autowired
    public DiscordGeneralEventListener(Environment env){
        this.SERVER_ID = env.getProperty("discord.server.id");
    }

    @Override
    public void onReady(@NotNull ReadyEvent event){
        System.out.println("> JDA onReady.");
        System.out.println("> start to update available command.");
        event.getJDA().getGuildById(SERVER_ID).upsertCommand("update_config", "update service config")
                .addOption(OptionType.STRING, "service", "target service name", true)
                .addOption(OptionType.STRING, "property", "config name", true)
                .addOption(OptionType.STRING, "value", "config value", true)
                .addOption(OptionType.STRING, "context", "config context, default: general", false)
                .queue();
        System.out.println("> done.");
        /* print current slash command */
        event.getJDA().getGuildById(SERVER_ID).retrieveCommands().queue(commands -> {
            System.out.println("[onReady] available command: " + commands);
        });
    }
}
