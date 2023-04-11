
package dev.mrflyn.vectunes.commandlisteners;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import dev.mrflyn.vectunes.Bot;
import dev.mrflyn.vectunes.VecTunes;
import dev.mrflyn.vectunes.VecTunesAudioSendHandler;
import dev.mrflyn.vectunes.VecTunesTrackManager;
import java.util.ArrayList;


import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;

public class PlayCommandListener
extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            return;
        }
        if (event.getMember() == null) {
            return;
        }
        if (!event.getName().equals("play")) {
            return;
        }
        PlayCommandListener.startPlaying(event);
    }

    public static void startPlaying(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        GuildVoiceState vcState = member.getVoiceState();
        try {
            if (vcState == null) {
                return;
            }
            if (!vcState.inAudioChannel()) {
                return;
            }
            String link = event.getOptions().get(0).getAsString();
            if (!link.startsWith("https://") && !link.startsWith("http://")) {
                link = VecTunes.youTubeSearchManager.getLinkFromQuery(link);
            }
            Guild guild = event.getGuild();
            if (link.toLowerCase().contains("spotify.")) {
                link = guild.getIdLong() + " " + link;
            }
            VecTunes.log(link);
            VoiceChannel vc = (VoiceChannel) (vcState.getChannel());
            VecTunes.log(vc.getName());
            event.reply("Searching for your song...").setEphemeral(true).queue();
            AudioManager manager = guild.getAudioManager();
            if (event.getGuild().getMember(Bot.jda.getSelfUser()).hasPermission(Permission.MANAGE_CHANNEL))
                vc.getManager().setBitrate(guild.getMaxBitrate()).queue();
            if (vc.getMembers().contains(guild.getMember(Bot.jda.getSelfUser())) && VecTunes.bot.CHANNEL_TO_TUNES.containsKey(vc.getIdLong())) {
                VecTunes.bot.CHANNEL_TO_TUNES.get(vc.getIdLong()).queue(link, member.getIdLong(), null);
                return;
            }
            AudioPlayer player = VecTunes.playerManager.createPlayer();
            manager.setSendingHandler(new VecTunesAudioSendHandler(player));
            manager.openAudioConnection(vc);
            VecTunesTrackManager tunesTrackManager = new VecTunesTrackManager(player, vc.getIdLong(), event.getGuild().getIdLong(), event.getChannel().getIdLong());
            player.addListener(tunesTrackManager);
            tunesTrackManager.queue(link, member.getIdLong(), event.getChannel().asTextChannel());
        }catch (PermissionException e){
            event.getChannel().asTextChannel().sendMessage("No permission to connect to vc!").queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getGuild() == null) {
            return;
        }
        if (event.getMember() == null) {
            return;
        }
        if (!event.getName().equals("play")) {
            return;
        }
        Member member = event.getMember();
        GuildVoiceState vcState = member.getVoiceState();
        if (vcState == null) {
            return;
        }
        if (!vcState.inAudioChannel()) {
            return;
        }
        ArrayList<Command.Choice> choices = new ArrayList<Command.Choice>();
        String query = event.getFocusedOption().getValue();
        if (query.startsWith("https://") || query.startsWith("http://") || query.isEmpty() || query.equals(" ")) {
            return;
        }
        for (String autoCompletes : VecTunes.youTubeSearchManager.getAutoCompletes(query)) {
            if (autoCompletes.length() > 100) {
                autoCompletes = autoCompletes.substring(0, 100);
            }
            choices.add(new Command.Choice(autoCompletes, autoCompletes));
        }
        event.replyChoices(choices).queue();
    }
}

