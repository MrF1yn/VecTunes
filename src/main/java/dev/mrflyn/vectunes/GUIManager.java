
package dev.mrflyn.vectunes;

import com.github.topisenpai.lavasrc.spotify.SpotifyAudioTrack;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.mrflyn.vectunes.Bot;
import dev.mrflyn.vectunes.FavouriteTrack;
import dev.mrflyn.vectunes.VecTunes;
import dev.mrflyn.vectunes.VecTunesTrackManager;
import dev.mrflyn.vectunes.searchmanagers.YouTubeSearchManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

public class GUIManager {
    public static HashMap<String, GUIManager> registeredManagers = new HashMap<>();
    private VecTunesTrackManager trackManager;
    private long channelID;
    private long guildID;
    private long embedID = 0L;
    private String uuid;
    private boolean first = true;

    public GUIManager(VecTunesTrackManager trackManager, long channelID, long guildID) {
        this.trackManager = trackManager;
        this.channelID = channelID;
        this.guildID = guildID;
        this.uuid = UUID.randomUUID().toString();
        registeredManagers.put(this.uuid, this);
    }

    public void update() {
        try {
            Guild guild = Bot.jda.getGuildById(this.guildID);
            if (guild == null) {
                return;
            }
            TextChannel channel = guild.getTextChannelById(this.channelID);
            if (channel == null) {
                return;
            }
            MessageEmbed embed = this.getEmbed("playerEmbed.json", this.trackManager.getPlayer().getPlayingTrack());
            if (embed == null) {
                return;
            }
            Button playPause = this.trackManager.getPlayer().isPaused() ? this.parseButton("play") : this.parseButton("pause");
            Button stop = this.parseButton("stop");
            Button autoplay = this.trackManager.isAutoPlay() ? this.parseButton("autoplay_on") : this.parseButton("autoplay_off");
            Button forward = this.parseButton("forward");
            Button rewind = this.parseButton("rewind");
            Button skip = this.parseButton("skip");
            Button queueLoop = this.trackManager.isQueueLoop() ? this.parseButton("queue_loop_on") : this.parseButton("queue_loop_off");
            Button songLoop = this.trackManager.isSongLoop() ? this.parseButton("song_loop_on") : this.parseButton("song_loop_off");
            Button volume = this.parseButton("volume");
            Button favourite = this.parseButton("favourite");
            if (this.embedID == 0L && this.first) {
                if (this.trackManager.getPlayer().getPlayingTrack() == null) {
                    return;
                }
                ((MessageCreateAction)channel.sendMessageEmbeds(embed, new MessageEmbed[0]).addComponents(ActionRow.of(skip, stop, queueLoop, songLoop, favourite), ActionRow.of(rewind, playPause, forward, autoplay, volume))).queue(message -> {
                    this.embedID = message.getIdLong();
                });
                this.first = false;
                return;
            }
            if (this.embedID == 0L) {
                return;
            }
            channel.editMessageComponentsById(this.embedID, ActionRow.of(skip, stop, queueLoop, songLoop), ActionRow.of(rewind, playPause, forward, autoplay, volume)).queue(success -> {}, t -> {
                if (t instanceof ErrorResponseException && ((ErrorResponseException)t).getErrorCode() == 10008) {
                    return;
                }
                t.printStackTrace();
            });
            channel.editMessageEmbedsById(this.embedID, embed).queue(success1 -> {}, t -> {
                if (t instanceof ErrorResponseException && ((ErrorResponseException)t).getErrorCode() == 10008) {
                    return;
                }
                t.printStackTrace();
            });
        }
        catch (Exception e) {
            if (e instanceof JsonSyntaxException) {
                return;
            }
            e.printStackTrace();
        }
    }

    public void sendSongEndEmbed(AudioTrack track) {
        MessageEmbed embed = this.getEmbed("songFinished.json", track);
        if (embed == null) {
            return;
        }
        Guild guild = Bot.jda.getGuildById(this.guildID);
        if (guild == null) {
            return;
        }
        TextChannel channel = guild.getTextChannelById(this.channelID);
        if (channel == null) {
            return;
        }
        channel.sendMessageEmbeds(embed, new MessageEmbed[0]).queue();
    }

    public void error(String reason) {
        VecTunes.log(reason);
    }

    public void destroy() {
        registeredManagers.remove(this.uuid);
        if (this.embedID == 0L) {
            return;
        }
        Guild guild = Bot.jda.getGuildById(this.guildID);
        if (guild == null) {
            return;
        }
        TextChannel channel = guild.getTextChannelById(this.channelID);
        if (channel == null) {
            return;
        }
        channel.deleteMessageById(this.embedID).queue();
    }

    public void onButtonClick(ButtonInteractionEvent event, String id) {
        switch (id) {
            case "play": 
            case "pause": {
                this.trackManager.togglePlayPause();
                break;
            }
            case "stop": {
                this.trackManager.destroy();
                return;
            }
            case "autoplay_off": 
            case "autoplay_on": {
                this.trackManager.toggleAutoPlay();
                break;
            }
            case "forward": {
                this.trackManager.forward(10);
                break;
            }
            case "rewind": {
                this.trackManager.rewind(10);
                break;
            }
            case "skip": {
                this.trackManager.skip();
                break;
            }
            case "queue_loop_on": 
            case "queue_loop_off": {
                this.trackManager.toggleQueueLoop();
                break;
            }
            case "song_loop_on": 
            case "song_loop_off": {
                this.trackManager.toggleSongLoop();
                break;
            }
            case "favourite": {
                if (this.trackManager.getPlayer().getPlayingTrack() == null) break;
                if (!YouTubeSearchManager.FAVOURITES.containsKey(event.getMember().getIdLong())) {
                    YouTubeSearchManager.FAVOURITES.put(event.getMember().getIdLong(), new ArrayList());
                }
                List<FavouriteTrack> tracks = YouTubeSearchManager.FAVOURITES.get(event.getMember().getIdLong());
                AudioTrack aTrack = this.trackManager.getPlayer().getPlayingTrack();
                FavouriteTrack track = new FavouriteTrack(event.getMember().getIdLong(), aTrack.getInfo().title, aTrack.getInfo().identifier, aTrack.getInfo().uri);
                if (tracks.contains(track)) {
                    tracks.remove(track);
                    event.reply("Removed: " + track.getTrackName() + " from your Favourites!").setEphemeral(true).queue();
                    return;
                }
                tracks.add(track);
                event.reply("Added: " + track.getTrackName() + " to your Favourites!").setEphemeral(true).queue();
                return;
            }
            case "volume": {
                TextInput subject = TextInput.create("volume_subject", "Volume", TextInputStyle.SHORT).setPlaceholder("0-100").setMinLength(1).setMaxLength(100).build();
                Modal modal = Modal.create(this.uuid + ":volume_modal", "Volume Change").addActionRows(ActionRow.of(subject)).build();
                event.replyModal(modal).queue();
                return;
            }
        }
        event.deferEdit().queue();
    }

    public VecTunesTrackManager getTrackManager() {
        return this.trackManager;
    }

    private String getVolumeBar() {
        int i;
        int currentVolume = this.trackManager.getPlayer().getVolume() / 10;
        int maxVolume = 10;
        int emptyBars = maxVolume - currentVolume;
        StringBuilder s = new StringBuilder();
        for (i = 1; i <= currentVolume; ++i) {
            s.append("\u25b0");
        }
        for (i = 1; i <= emptyBars; ++i) {
            s.append("\u25b1");
        }
        return s.toString();
    }

    private Button parseButton(String name) {
        String emoji = VecTunes.configManager.getButtonConfig().getString(name + ".emoji");
        String displayName = VecTunes.configManager.getButtonConfig().getString(name + ".display_name");
        String type = VecTunes.configManager.getButtonConfig().getString(name + ".type");
        return Button.of(ButtonStyle.valueOf(type), this.uuid + ":" + name, displayName, Emoji.fromFormatted(emoji));
    }

    private MessageEmbed getEmbed(String embedFile, AudioTrack currTrack) {
        try {
            String json = VecTunes.EMBED_JSONS.get(embedFile);
            String title = "NONE";
            Object thumbnail = "https://cdn.discordapp.com/icons/928525879087362050/d348f37279e01d75cbb7c09bdf39242a.webp?size=100";
            String artist = "NONE";
            String duration = "NONE";
            String requester = "NONE";
            String url = "https://www.vectlabs.xyz/";
            if (currTrack != null) {
                title = currTrack.getInfo().title;
                requester = Bot.jda.getUserById(this.trackManager.getTRACK_REQUESTER().get(currTrack.getIdentifier())).getAsMention();
                url = currTrack.getInfo().uri;
                artist = currTrack.getInfo().author;
                duration = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(currTrack.getDuration()) % 60L, TimeUnit.MILLISECONDS.toSeconds(currTrack.getDuration()) % 60L);
                if (currTrack.getSourceManager().getSourceName().equals("youtube")) {
                    thumbnail = "https://img.youtube.com/vi/" + currTrack.getInfo().identifier + "/0.jpg";
                } else if (currTrack instanceof SpotifyAudioTrack) {
                    thumbnail = ((SpotifyAudioTrack)currTrack).getArtworkURL();
                }
            }
            json = json.replace("%currSong%", title).replace("%songCoverImg%", (CharSequence)thumbnail).replace("%artist%", artist).replace("%duration%", duration).replace("%channel%", this.trackManager.getAudioChannelName()).replace("%requester%", requester).replace("%volume%", "" + this.trackManager.getPlayer().getVolume()).replace("%volumebar%", this.getVolumeBar()).replace("%currSongUrl%", url).replace("%queue%", this.trackManager.getRemainingQueueString());
            return this.jsonToEmbed(JsonParser.parseString(json).getAsJsonObject());
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private MessageEmbed jsonToEmbed(JsonObject json) {
        JsonObject footerObj;
        JsonObject imageObj;
        JsonObject thumbnailObj;
        JsonArray fieldsArray;
        JsonPrimitive colorObj;
        JsonPrimitive descObj;
        JsonObject authorObj;
        EmbedBuilder embedBuilder = new EmbedBuilder();
        JsonPrimitive titleObj = json.getAsJsonPrimitive("title");
        if (titleObj != null) {
            embedBuilder.setTitle(titleObj.getAsString());
        }
        if ((authorObj = json.getAsJsonObject("author")) != null) {
            String authorName = authorObj.get("name").getAsString();
            String authorIconUrl = authorObj.get("icon_url") == null ? null : authorObj.get("icon_url").getAsString();
            String authorUrl = authorObj.get("url") == null ? null : authorObj.get("url").getAsString();
            embedBuilder.setAuthor(authorName, authorUrl, authorIconUrl);
        }
        if ((descObj = json.getAsJsonPrimitive("description")) != null) {
            embedBuilder.setDescription(descObj.getAsString());
        }
        if ((colorObj = json.getAsJsonPrimitive("color")) != null) {
            Color color = new Color(colorObj.getAsInt());
            embedBuilder.setColor(color);
        }
        if ((fieldsArray = json.getAsJsonArray("fields")) != null) {
            fieldsArray.forEach(ele -> {
                String name = ele.getAsJsonObject().get("name").getAsString().length() > 900 ? ele.getAsJsonObject().get("name").getAsString().substring(0, 900) + "..." : ele.getAsJsonObject().get("name").getAsString();
                String content = ele.getAsJsonObject().get("value").getAsString().length() > 900 ? ele.getAsJsonObject().get("value").getAsString().substring(0, 900) + "..." : ele.getAsJsonObject().get("value").getAsString();
                boolean inline = ele.getAsJsonObject().get("inline").getAsBoolean();
                embedBuilder.addField(name, content, inline);
            });
        }
        if ((thumbnailObj = json.getAsJsonObject("thumbnail")) != null) {
            embedBuilder.setThumbnail(thumbnailObj.get("url").getAsString());
        }
        if ((imageObj = json.getAsJsonObject("image")) != null) {
            embedBuilder.setImage(imageObj.get("url").getAsString());
        }
        if ((footerObj = json.getAsJsonObject("footer")) != null) {
            String content = footerObj.get("text").getAsString();
            String footerIconUrl = footerObj.get("icon_url").getAsString();
            if (footerIconUrl != null) {
                embedBuilder.setFooter(content, footerIconUrl);
            } else {
                embedBuilder.setFooter(content);
            }
        }
        return embedBuilder.build();
    }
}

