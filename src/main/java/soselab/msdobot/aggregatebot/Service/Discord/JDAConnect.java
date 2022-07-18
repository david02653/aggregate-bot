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
import java.util.ArrayList;
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
    private final String serverId;
    private final String testChannelId;

    @Autowired
    public JDAConnect(Environment env, DiscordOnMessageListener onMessageListener, DiscordGeneralEventListener generalEventListener, DiscordSlashCommandListener slashCommandListener){
        this.onMessageListener = onMessageListener;
//        this.buttonListener = buttonEvt;
        this.generalEventListener = generalEventListener;
        this.slashCommandListener = slashCommandListener;
        this.appToken = env.getProperty("discord.bot.token");
        this.serverId = env.getProperty("discord.server.id");
        this.testChannelId = env.getProperty("discord.testChannel.id");
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
     * @param msg message content
     */
    public void send(Message msg){
        System.out.println("[DEBUG] send msg");
        JDA.getGuildById(serverId).getTextChannelById(testChannelId).sendMessage(msg).queue();
//        }
    }

    public void send(ArrayList<Message> msgList){
        System.out.println("[DEBUG] send msg");
        msgList.forEach(msg -> {
            JDA.getGuildById(serverId).getTextChannelById(testChannelId).sendMessage(msg).queue();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * send message to target text channel<br>
     * should only be used by rabbitMQ message handler<br>
     * note that given channel id might not be found in guild
     * @param msg message content
     * @param channelId target channel id
     */
    public void send(Message msg, String channelId){
        System.out.println("[DEBUG] send msg to channel '" + channelId + "'");
        JDA.getGuildById(serverId).getTextChannelById(channelId).sendMessage(msg).queue();
    }
    public void send(ArrayList<Message> msgList, String channelId){
        System.out.println("[DEBUG] send msg to channel '" + channelId + "'");
        msgList.forEach(msg -> {
            JDA.getGuildById(serverId).getTextChannelById(channelId).sendMessage(msg).queue();

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
