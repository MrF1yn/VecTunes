
package dev.mrflyn.vectunes;



import dev.mrflyn.vectunes.searchmanagers.YouTubeSearchManager;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class PresenceListener
extends ListenerAdapter {

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        event.getJDA().getPresence().setActivity(Activity.playing("/play Tunes in " + event.getGuildTotalCount() + " Servers."));

    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        if(Bot.JDA_TO_BOT.get(event.getJDA()).isPremium() && !YouTubeSearchManager.PREMIUM_GUILDS.contains(event.getGuild().getIdLong())){
            event.getGuild().leave().queue();
        }

    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        if(Bot.JDA_TO_BOT.get(event.getJDA()).isPremium() && !YouTubeSearchManager.PREMIUM_GUILDS.contains(event.getGuild().getIdLong())){
            event.getGuild().leave().queue();
        }
        event.getJDA().getPresence().setActivity(Activity.playing("/play Tunes in " + event.getJDA().getGuilds().size() + " Servers."));
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        event.getJDA().getPresence().setActivity(Activity.playing("Tunes in " + event.getJDA().getGuilds().size() + " Servers."));
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event){
        if (event.getChannelLeft()!=null){
            Bot bot = Bot.JDA_TO_BOT.get(event.getJDA());
            if (bot==null)return;
            if (event.getMember().getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
                if (bot.CHANNEL_TO_TUNES.containsKey(event.getChannelLeft().getIdLong())) {
                    bot.CHANNEL_TO_TUNES.get(event.getChannelLeft().getIdLong()).destroy();
                }
                return;
            }
            if (bot.CHANNEL_TO_TUNES.containsKey(event.getChannelLeft().getIdLong())) {
                if (event.getChannelLeft().getMembers().size()-1<1) {
                    bot.CHANNEL_TO_TUNES.get(event.getChannelLeft().getIdLong()).destroy();
                }
                return;
            }
        }
    }

}

