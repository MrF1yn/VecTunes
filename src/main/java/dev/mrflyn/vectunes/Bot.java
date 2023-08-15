
package dev.mrflyn.vectunes;

import dev.mrflyn.vectunes.PresenceListener;
import dev.mrflyn.vectunes.VecTunesTrackManager;
import dev.mrflyn.vectunes.commandlisteners.ButtonListener;
import dev.mrflyn.vectunes.commandlisteners.PlayCommandListener;
import java.util.HashMap;

import dev.mrflyn.vectunes.commandlisteners.SpotifySetupCommandListener;
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
    public HashMap<Long, VecTunesTrackManager> CHANNEL_TO_TUNES = new HashMap<>();
    public static JDA jda;
    private String botToken;

    public Bot(String token) {
        if (token == null) {
            return;
        }
        this.botToken = token;
    }

    public void enable() {
        long millis = System.currentTimeMillis();
        try {
            jda = JDABuilder.createLight(this.botToken,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_VOICE_STATES)
                    .addEventListeners(
                            new PlayCommandListener(),
                            new ButtonListener(),
                            new PresenceListener(),
                            new SpotifySetupCommandListener()
                            )
                    .setActivity(Activity.playing("/play Tunes"))
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableCache(CacheFlag.VOICE_STATE, new CacheFlag[0])
                    .build();
            jda.awaitReady();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        jda.upsertCommand("play", "play a song with the link.")
                .addOptions(
                        new OptionData(OptionType.STRING, "link-or-name", "Link or Name of the song.", true).setAutoComplete(true),
                        new OptionData(OptionType.BOOLEAN, "forceplay", "whether to skip the queue and force play the song.",false))
                .queue();

        jda.upsertCommand("spotify_setup", "setup spotify api")
                .addOptions(
                        new OptionData(OptionType.STRING, "client-id", "client-id", true),
                        new OptionData(OptionType.STRING, "client-secret", "client-secret", true),
                        new OptionData(OptionType.STRING, "country-code", "country-code", true))
                .queue();
        VecTunes.log("Done ("+(System.currentTimeMillis()-millis)/1000.0+"s)! For help, type \"help\" or \"?\"");
    }
}

