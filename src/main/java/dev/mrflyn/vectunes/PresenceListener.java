
package dev.mrflyn.vectunes;

import dev.mrflyn.vectunes.Bot;
import dev.mrflyn.vectunes.VecTunes;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class PresenceListener
extends ListenerAdapter {
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        event.getJDA().getPresence().setActivity(Activity.playing("/play Tunes in " + event.getGuildTotalCount() + " Servers."));
    }

    @Override
    public void onGuildJoin(@Nonnull GuildJoinEvent event) {
        event.getJDA().getPresence().setActivity(Activity.playing("/play Tunes in " + event.getJDA().getGuilds().size() + " Servers."));
    }

    @Override
    public void onGuildLeave(@Nonnull GuildLeaveEvent event) {
        event.getJDA().getPresence().setActivity(Activity.playing("Tunes in " + event.getJDA().getGuilds().size() + " Servers."));
    }

    @Override
    public void onGuildVoiceMove(@Nonnull GuildVoiceMoveEvent event) {
        if (event.getMember().getIdLong() == Bot.jda.getSelfUser().getIdLong()) {
            if (VecTunes.bot.CHANNEL_TO_TUNES.containsKey(event.getChannelLeft().getIdLong())) {
                VecTunes.bot.CHANNEL_TO_TUNES.get(event.getChannelLeft().getIdLong()).destroy();
            }
            return;
        }
    }

    @Override
    public void onGuildVoiceLeave(@Nonnull GuildVoiceLeaveEvent event) {
        if (event.getMember().getIdLong() == Bot.jda.getSelfUser().getIdLong()) {
            if (VecTunes.bot.CHANNEL_TO_TUNES.containsKey(event.getChannelLeft().getIdLong())) {
                VecTunes.bot.CHANNEL_TO_TUNES.get(event.getChannelLeft().getIdLong()).destroy();
            }
            return;
        }
        if (VecTunes.bot.CHANNEL_TO_TUNES.containsKey(event.getChannelLeft().getIdLong())) {
            if (event.getChannelLeft().getMembers().size() < 1) {
                VecTunes.bot.CHANNEL_TO_TUNES.get(event.getChannelLeft().getIdLong()).destroy();
            }
            return;
        }
    }
}

