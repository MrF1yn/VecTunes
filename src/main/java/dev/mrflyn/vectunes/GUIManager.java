
package dev.mrflyn.vectunes;

import com.github.topi314.lavasrc.ExtendedAudioTrack;
import com.github.topi314.lavasrc.spotify.SpotifyAudioTrack;
import com.google.gson.*;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
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
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.apache.commons.text.StringEscapeUtils;

public class GUIManager {
    public static HashMap<String, GUIManager> registeredManagers = new HashMap<>();
    private VecTunesTrackManager trackManager;
    private long channelID;
    private long guildID;
    private long embedID = 0L;
    private String uuid;

    private long lastUpdated;
    Gson gson = new GsonBuilder().setLenient().create();

    public GUIManager(VecTunesTrackManager trackManager, long channelID, long guildID) {
        this.lastUpdated = System.currentTimeMillis();
        this.trackManager = trackManager;
        this.channelID = channelID;
        this.guildID = guildID;
        this.uuid = UUID.randomUUID().toString();
        registeredManagers.put(this.uuid, this);
    }

    private boolean msgExists(TextChannel channel, long id){
        try {
            channel.retrieveMessageById(id).complete();
        }catch (Exception e){
            return false;
        }
        return true;
    }


    public void update() {
        try {
            Guild guild = trackManager.getBot().jda.getGuildById(this.guildID);
            if (guild == null) {
                return;
            }
            GuildMessageChannel channel = guild.getChannelById(GuildMessageChannel.class, this.channelID);

            if (channel == null) {
                return;
            }
            MessageEmbed embed = this.getEmbed("playerEmbed.json", this.trackManager.getPlayer().getPlayingTrack());
            if (embed == null) {
                return;
            }
            if(System.currentTimeMillis() - lastUpdated < 500)return;
            Button playPause = this.trackManager.getPlayer().isPaused() ? this.parseButton("play") : this.parseButton("pause");
            Button stop = this.parseButton("stop");
            Button autoplay = this.trackManager.isAutoPlay() ? this.parseButton("autoplay_on") : this.parseButton("autoplay_off");
            Button shuffle = this.trackManager.isShuffle() ? this.parseButton("shuffle_on") : this.parseButton("shuffle_off");
            Button forward = this.parseButton("forward");
            Button rewind = this.parseButton("rewind");
            Button skip = this.parseButton("skip");
            Button queueLoop = this.trackManager.isQueueLoop() ? this.parseButton("queue_loop_on") : this.parseButton("queue_loop_off");
            Button songLoop = this.trackManager.isSongLoop() ? this.parseButton("song_loop_on") : this.parseButton("song_loop_off");
            Button volume = this.parseButton("volume");
            Button favourite = this.parseButton("favourite");
            Button jump = this.parseButton("jump_track");
            if(this.embedID!=0L){
                channel.editMessageEmbedsById(this.embedID, embed).queue(s->{
                    channel.editMessageComponentsById(this.embedID,
                            ActionRow.of(skip, stop, queueLoop, songLoop, favourite),
                            ActionRow.of(rewind, playPause, forward, volume),
                            ActionRow.of(shuffle, jump, autoplay)).queue(s1->{},err1->{
//                                err1.printStackTrace();
                    });

                },err->{
//                    err.printStackTrace();
                });

                return;
            }
            if (this.trackManager.getPlayer().getPlayingTrack() == null) {
                return;
            }
            channel.sendMessageEmbeds(embed).addComponents(
                    ActionRow.of(skip, stop, queueLoop, songLoop, favourite),
                    ActionRow.of(rewind, playPause, forward, volume),
                    ActionRow.of(shuffle, jump, autoplay)).queue(message -> {
                this.embedID = message.getIdLong();
                update();
            }, err -> {
                err.printStackTrace();
                update();
            });

        } catch (Exception e) {
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
        Guild guild = trackManager.getBot().jda.getGuildById(this.guildID);
        if (guild == null) {
            return;
        }
        GuildMessageChannel channel = guild.getChannelById(GuildMessageChannel.class,this.channelID);
        if (channel == null) {
            return;
        }
        channel.sendMessageEmbeds(embed).queue();
        if (this.embedID != 0L)
            channel.deleteMessageById(this.embedID).queue(success -> {
                this.embedID = 0L;
                update();
            }, Throwable::printStackTrace);

    }

    public void error(String reason) {
        VecTunes.log(reason);
    }



    public void destroy() {
        registeredManagers.remove(this.uuid);
        if (this.embedID == 0L) {
            return;
        }
        Guild guild = trackManager.getBot().jda.getGuildById(this.guildID);
        if (guild == null) {
            return;
        }
        GuildMessageChannel channel = guild.getChannelById(GuildMessageChannel.class,this.channelID);
        if (channel == null) {
            return;
        }
//        channel.deleteMessageById(this.embedID).queue();
        this.embedID = 0L;
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
            case "shuffle_off":
            case "shuffle_on": {
                this.trackManager.toggleShuffle();
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
                TextInput subject = TextInput.create("volume_subject", "Volume", TextInputStyle.SHORT).setPlaceholder("1-100").setMinLength(1).setMaxLength(100).build();
                Modal modal = Modal.create(this.uuid + ":volume_modal", "Volume Change").
                        addComponents(ActionRow.of(subject)).build();
                event.replyModal(modal).queue();
                return;
            }
            case "jump_track":{
                TextInput subject = TextInput.create("jump_track_subject", "Jump Track", TextInputStyle.SHORT)
                        .setPlaceholder("0-"+trackManager.getRemainingTrackNumber()).setMinLength(1).setMaxLength(100).build();
                Modal modal = Modal.create(this.uuid + ":jump_track_modal", "Jump Track").
                        addComponents(ActionRow.of(subject)).build();
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
        String displayName = VecTunes.configManager.getButtonConfig().getString(name + ".display_name")
                .replace("%volume%", this.trackManager.getPlayer().getVolume() + "");
        String type = VecTunes.configManager.getButtonConfig().getString(name + ".type");
        return Button.of(ButtonStyle.valueOf(type), this.uuid + ":" + name, displayName, Emoji.fromFormatted(emoji));
    }

    private MessageEmbed getEmbed(String embedFile, AudioTrack currTrack) {
        try {
            String json = VecTunes.EMBED_JSONS.get(embedFile);
            String title = "NONE";
            String thumbnail = null;
            String artist = "NONE";
            String duration = "NONE";
            String requester = "NONE";
            String url = "https://www.vectlabs.xyz/";
            if (currTrack != null) {
                title = currTrack.getInfo().title;
                requester = VecTunes.mainBot.jda.getUserById(this.trackManager.getTRACK_REQUESTER().get(currTrack.getIdentifier())).getAsMention();
                url = currTrack.getInfo().uri;
                artist = currTrack.getInfo().author;
                duration = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(currTrack.getDuration()) % 60L, TimeUnit.MILLISECONDS.toSeconds(currTrack.getDuration()) % 60L);
                if (currTrack.getSourceManager().getSourceName().equals("youtube")) {
                    thumbnail = "https://img.youtube.com/vi/" + currTrack.getInfo().identifier + "/0.jpg";
                } else if (currTrack instanceof SpotifyAudioTrack) {
                    thumbnail = ((SpotifyAudioTrack) currTrack).getArtistArtworkUrl();
                }
            }
            if (thumbnail == null)
                thumbnail = "https://cdn.discordapp.com/icons/928525879087362050/d348f37279e01d75cbb7c09bdf39242a.webp?size=100";
            String queue = " " + StringEscapeUtils.escapeJson(this.trackManager.getRemainingQueueString().trim());
            queue = queue.length() > 900 ? queue.substring(0, 900) + "..." : queue;
            json = json.replace("%currSong%", StringEscapeUtils.escapeJson(title.trim()))
                    .replace("%songCoverImg%", StringEscapeUtils.escapeJson(thumbnail.trim()))
                    .replace("%artist%", StringEscapeUtils.escapeJson(artist.trim()))
                    .replace("%duration%", StringEscapeUtils.escapeJson(duration.trim()))
                    .replace("%channel%", StringEscapeUtils.escapeJson(this.trackManager.getAudioChannelName().trim()))
                    .replace("%requester%", StringEscapeUtils.escapeJson(requester.trim()))
                    .replace("%volume%", StringEscapeUtils.escapeJson(("" + this.trackManager.getPlayer().getVolume()).trim()))
                    .replace("%volumebar%", StringEscapeUtils.escapeJson(this.getVolumeBar().trim()))
                    .replace("%currSongUrl%", StringEscapeUtils.escapeJson(url.trim()));
            try {
                return jsonToEmbed(gson.fromJson(json.replace("%queue%", queue), JsonObject.class));
            }catch (Exception e){
                return jsonToEmbed(gson.fromJson(json.replace("%queue%", "Loading..."), JsonObject.class));
            }
        } catch (Exception e) {
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
            if(trackManager.getBot().isPremium()){
                color = new Color(Integer.parseInt(VecTunes.configManager.getMainConfig().getStringList("premium_bots").get(
                        VecTunes.premiumBots.indexOf(trackManager.getBot())).split(":")[1]
                ));
            }

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

