package soselab.msdobot.aggregatebot.Service.Discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;
import java.util.List;

/**
 * JDA instance, define what discord bot could access
 */
@Service
public class JDAConnect {

    public static JDA JDA;
    private DiscordOnMessageListener onMessageListener;
//    private DiscordOnButtonClickListener buttonListener;
    private DiscordGeneralEventListener generalEventListener;
    private DiscordSlashCommandListener slashCommandListener;
    private final String appToken;

    @Autowired
    public JDAConnect(Environment env, DiscordOnMessageListener onMessageListener, DiscordGeneralEventListener generalEventListener, DiscordSlashCommandListener slashCommandListener){
        this.onMessageListener = onMessageListener;
//        this.buttonListener = buttonEvt;
        this.generalEventListener = generalEventListener;
        this.slashCommandListener = slashCommandListener;
        this.appToken = env.getProperty("discord.bot.token");
    }

    /**
     * connect to discord when this class instance is created
     * this should be triggered by spring itself when this application startupConsumer
     */
    @PostConstruct
    private void init(){
        try{
            createJDAConnect(appToken);
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("[JDA] initialize failed !");
        }
    }

    /**
     * create connect to discord by using server token
     * @param token server token
     * @throws LoginException if discord login failed
     */
    public void createJDAConnect(String token) throws LoginException {
        JDABuilder builder = JDABuilder.createDefault(token);

        configure(builder);
        // add customized Event Listener
        builder.addEventListeners(generalEventListener);
        // add customized MessageListener
        builder.addEventListeners(onMessageListener);
        // add customized Button onClick listener
//        builder.addEventListeners(buttonListener);
        // add customized slash command listener
        builder.addEventListeners(slashCommandListener);
        JDA = builder.build();
    }

    /**
     * discord bot setup
     * @param builder discord bot builder
     */
    public void configure(JDABuilder builder){
        // disable member activities (streaming / games / spotify)
//        builder.disableCache(CacheFlag.ACTIVITY);
        // disable member chunking on startup
//        builder.setChunkingFilter(ChunkingFilter.NONE);

        builder.enableIntents(GatewayIntent.GUILD_MEMBERS).setMemberCachePolicy(MemberCachePolicy.ALL);
    }

    /**
     * only for testing purpose, send message
     * @param msg
     */
    public void send(Message msg){
        System.out.println("[DEBUG] send msg");
        JDA.getGuildById("737233839709225001").getTextChannelById("966378622560665610").sendMessage(msg).queue();
//        List<TextChannel> channels = JDA.getTextChannelsByName(channel, true);
//        for(TextChannel ch: channels){
//            ch.sendMessage(msg).queue();
//        }
    }
}
