
package dev.mrflyn.vectunes;

import dev.mrflyn.vectunes.PresenceListener;
import dev.mrflyn.vectunes.VecTunesTrackManager;
import dev.mrflyn.vectunes.commandlisteners.ButtonListener;
import dev.mrflyn.vectunes.commandlisteners.PlayCommandListener;
import java.util.HashMap;
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
        try {
            jda = JDABuilder.createLight(this.botToken,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_VOICE_STATES)
                    .addEventListeners(
                            new PlayCommandListener(),
                            new ButtonListener(),
                            new PresenceListener())
                    .setActivity(Activity.playing("/play Tunes"))
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableCache(CacheFlag.VOICE_STATE, new CacheFlag[0])
                    .build();
            jda.awaitReady();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        jda.upsertCommand("play", "play a song with the link.").addOptions(new OptionData(OptionType.STRING, "link-or-name", "Link or Name of the song.", true).setAutoComplete(true)).queue();
    }
}

