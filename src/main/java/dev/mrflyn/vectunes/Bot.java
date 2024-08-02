
package dev.mrflyn.vectunes;

import dev.mrflyn.vectunes.PresenceListener;
import dev.mrflyn.vectunes.VecTunesTrackManager;
import dev.mrflyn.vectunes.commandlisteners.ButtonListener;
import dev.mrflyn.vectunes.commandlisteners.PlayCommandListener;
import java.util.HashMap;

import dev.mrflyn.vectunes.commandlisteners.PremiumCommandListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class Bot {
    public static HashMap<JDA, Bot> JDA_TO_BOT = new HashMap<>();
    public HashMap<Long, VecTunesTrackManager> CHANNEL_TO_TUNES = new HashMap<>();
    public JDA jda;//TODO: fix this
    private String botToken;
    private boolean premium;

    public Bot(String token) {
        if (token == null) {
            return;
        }
        this.botToken = token;
    }

    public Bot(String token, boolean isPremium) {
        if (token == null) {
            return;
        }
        this.botToken = token;
        this.premium = isPremium;
    }

    public boolean isPremium() {
        return premium;
    }


    public void enable() {
        long millis = System.currentTimeMillis();
        try {
            JDABuilder builder = JDABuilder.createLight(this.botToken,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_VOICE_STATES)
                    .addEventListeners(
                            new ButtonListener(),
                            new PresenceListener()
                            )
                    .setActivity(Activity.playing("/play Tunes"))
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableCache(CacheFlag.VOICE_STATE, new CacheFlag[0]);
            if (!premium){
                builder.addEventListeners(
                        new PlayCommandListener(),
                        new PremiumCommandListener()
                );
            }
            jda = builder.build();
            JDA_TO_BOT.put(jda, this);
            jda.awaitReady();

        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        VecTunes.log("Done ("+(System.currentTimeMillis()-millis)/1000.0+"s)! For help, type \"help\" or \"?\"");

        if(premium)
            return;

        jda.upsertCommand("play", "play a song with the link.")
                .addOptions(
                        new OptionData(OptionType.STRING, "link-or-name", "Link or Name of the song.", true).setAutoComplete(true),
                        new OptionData(OptionType.BOOLEAN, "forceplay", "whether to skip the queue and force play the song.",false))
                .queue();

        jda.upsertCommand("premium", "enable premium mode on the server. currently on available to the developer.")
                .queue();

        jda.upsertCommand("spotify_setup", "setup spotify api")
                .addOptions(
                        new OptionData(OptionType.STRING, "client-id", "client-id", true),
                        new OptionData(OptionType.STRING, "client-secret", "client-secret", true),
                        new OptionData(OptionType.STRING, "country-code", "country-code", true))
                .queue();

    }
}

