
package dev.mrflyn.vectunes.commandlisteners;

import com.github.topi314.lavasrc.spotify.SpotifyCredentials;
import dev.mrflyn.vectunes.VecTunes;
import dev.mrflyn.vectunes.searchmanagers.YouTubeSearchManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class SpotifySetupCommandListener
extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            return;
        }
        if (event.getMember() == null) {
            return;
        }
        if (!event.getName().equals("spotify_setup")) {
            return;
        }
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR))return;
        if (event.getOptions().size()<3)return;
        String clientID = event.getOptions().get(0).getAsString();
        String clientSecret = event.getOptions().get(1).getAsString();
        String countryUS = event.getOptions().get(2).getAsString();
        VecTunes.spotifySearchManager.getSpotifySourceManager().registerSpotifyCredentials(clientID, clientSecret, countryUS, event.getGuild().getIdLong());
        YouTubeSearchManager.GUILD_SPOTIFY_CREDENTIALS.put(event.getGuild().getIdLong(), new SpotifyCredentials(
                clientID,
                clientSecret,
                countryUS
        ));
        event.reply("DONE!").setEphemeral(true).queue();
    }
}

