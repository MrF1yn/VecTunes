
package dev.mrflyn.vectunes.commandlisteners;

import dev.mrflyn.vectunes.VecTunes;
import dev.mrflyn.vectunes.searchmanagers.YouTubeSearchManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class PremiumCommandListener
extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            return;
        }
        if (!event.getName().equals("premium")) {
            return;
        }
        if (event.getUser().getIdLong()!=847383537497800714L)return;
        if(YouTubeSearchManager.PREMIUM_GUILDS.contains(event.getGuild().getIdLong())){
            event.reply("Already Premium!").queue();
            return;
        }
        YouTubeSearchManager.PREMIUM_GUILDS.add(event.getGuild().getIdLong());
        StringBuilder s = new StringBuilder();
        s.append("Successfully enabled VecTunes Premium Mode on this guild. Please Invite the following bots to the server to enjoy the seamless multi-bot feature.\n");
        for(String link : VecTunes.configManager.getMainConfig().getStringList("premium_bot_links")){
            s.append(link).append("\n");
        }
        event.reply(s.toString()).queue();
    }
}

