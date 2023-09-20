
package dev.mrflyn.vectunes.commandlisteners;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import dev.mrflyn.vectunes.Bot;
import dev.mrflyn.vectunes.VecTunes;
import dev.mrflyn.vectunes.VecTunesAudioSendHandler;
import dev.mrflyn.vectunes.VecTunesTrackManager;
import java.util.ArrayList;


import dev.mrflyn.vectunes.searchmanagers.YouTubeSearchManager;
import net.dv8tion.jda.api.JDA;
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
        startPlaying(event, event.getJDA(), null, false);
    }

    public static void startPlaying(SlashCommandInteractionEvent event, JDA jda, String link, boolean redirected) {
        Member member = event.getMember();
        GuildVoiceState vcState = member.getVoiceState();
        Guild guild = event.getGuild();
        boolean isPremiumGuild = YouTubeSearchManager.PREMIUM_GUILDS.contains(guild.getIdLong());
        try {

            if (vcState == null) {
                return;
            }
            if (!vcState.inAudioChannel()) {
                return;
            }
            if(link==null) {
                link = event.getOptions().get(0).getAsString();
                if (!link.startsWith("https://") && !link.startsWith("http://")) {
                    link = VecTunes.youTubeSearchManager.getLinkFromQuery(link);
                }
                if (link.toLowerCase().contains("spotify.")) {
                    link = guild.getIdLong() + " " + link;
                }
            }
            boolean forcePlay = event.getOptions().size() >= 2 && event.getOptions().get(1).getAsBoolean();
            VecTunes.log(link);
            VoiceChannel vc = (VoiceChannel) (vcState.getChannel());
            GuildVoiceState botVCState = guild.getMember(jda.getSelfUser()).getVoiceState();

            if(!redirected && isPremiumGuild) {
                for (Bot bot : VecTunes.premiumBots) {
                    if (vc.getMembers().contains(guild.getMember(bot.jda.getSelfUser()))) {
                        startPlaying(event, bot.jda, link, true);
                        return;
                    }
                }
            }

            if(!VecTunes.premiumBots.isEmpty() && botVCState.inAudioChannel() && botVCState.getChannel()!= vcState.getChannel() && !redirected && isPremiumGuild){
                PlayCommandListener.redirectPlayer(event, link);
                return;
            }

            VecTunes.log(vc.getName());
            event.reply("Searching for your song...").setEphemeral(true).queue();
            //re-retriving the guild cause we want it linked with the new bot's jda.
            guild = jda.getGuildById(guild.getIdLong());
            AudioManager manager = guild.getAudioManager();


            if (event.getGuild().getMember(VecTunes.mainBot.jda.getSelfUser()).hasPermission(Permission.MANAGE_CHANNEL))
                vc.getManager().setBitrate(guild.getMaxBitrate()).queue();



            if (vc.getMembers().contains(guild.getMember(jda.getSelfUser())) && Bot.JDA_TO_BOT.get(jda).CHANNEL_TO_TUNES.containsKey(vc.getIdLong())) {
                VecTunes.log("EXISTING QUEUE");
                Bot.JDA_TO_BOT.get(jda).CHANNEL_TO_TUNES.get(vc.getIdLong()).queue(link, member.getIdLong(), event.getChannel().asTextChannel(), forcePlay);
                return;
            }
            AudioPlayer player = VecTunes.playerManager.createPlayer();
            manager.setSendingHandler(new VecTunesAudioSendHandler(player));
            manager.openAudioConnection(vc);

            VecTunesTrackManager tunesTrackManager = new VecTunesTrackManager(player, vc.getIdLong(), event.getGuild().getIdLong(),
                    Bot.JDA_TO_BOT.get(jda).isPremium()?vc.getIdLong():event.getChannel().getIdLong(),
                    Bot.JDA_TO_BOT.get(jda)
            );

            player.addListener(tunesTrackManager);

            tunesTrackManager.queue(link, member.getIdLong(),
                    event.getChannel().asGuildMessageChannel(),
                    forcePlay);
        }catch (PermissionException e){
            event.getChannel().asTextChannel().sendMessage("No permission to connect to vc!").queue();
        }
    }

    public static void redirectPlayer(SlashCommandInteractionEvent event, String link){
        for(Bot bot : VecTunes.premiumBots){
            JDA jda = bot.jda;
            if (!event.getGuild().isMember(jda.getSelfUser()))continue;
            if (!event.getGuild().getMember(jda.getSelfUser()).getVoiceState().inAudioChannel()){
                startPlaying(event, jda, link, true);
                return;
            }
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

