
package dev.mrflyn.vectunes.searchmanagers;

import com.github.topi314.lavasrc.spotify.SpotifyAudioTrack;
import com.github.topi314.lavasrc.spotify.SpotifySourceManager;
import com.github.topi314.lavasrc.spotify.data.SpotifyArtistInfo;
import com.github.topi314.lavasrc.spotify.data.SpotifyTrackFeatures;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;

public class SpotifySearchManager {

    private OkHttpClient client;
    private SpotifySourceManager spotifySourceManager;

    public SpotifySearchManager(SpotifySourceManager spotifySourceManager) {
        this.client = new OkHttpClient();
        this.spotifySourceManager = spotifySourceManager;
    }

    public SpotifySourceManager getSpotifySourceManager() {
        return spotifySourceManager;
    }

    public BasicAudioPlaylist getPlaylistFromSearch(String query, long guildID) {
        try {
            AudioItem item = this.spotifySourceManager.getSearch(query,false, guildID);
            if (item == AudioReference.NO_TRACK) {
                return null;
            }
            if (!(item instanceof BasicAudioPlaylist)) {
                return null;
            }
            return (BasicAudioPlaylist)item;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<SpotifyAudioTrack> getAutoPlayList(AudioTrack audioTrack, long guildID) {
        try {
        SpotifyAudioTrack track;
        if (!(audioTrack instanceof SpotifyAudioTrack)){
            BasicAudioPlaylist pl = this.getPlaylistFromSearch(audioTrack.getInfo().title, guildID);
            if (pl==null)return new ArrayList<>();
            track = (SpotifyAudioTrack) pl.getTracks().get(0);
        }else{
            track = (SpotifyAudioTrack) audioTrack;
        }

        SpotifyArtistInfo artistInfo = this.spotifySourceManager.getArtistInfo(track.getTrackInfo().author, guildID);
        if (artistInfo==null)return new ArrayList<>();
        SpotifyTrackFeatures trackFeatures = this.spotifySourceManager.getTrackFeatures(track.getInfo().identifier, guildID);
        if (trackFeatures==null) return new ArrayList<>();
            String genre = StringUtils.join(artistInfo.getGenres(), "%2C");
            genre = URLEncoder.encode(genre, StandardCharsets.UTF_8);
        String market = "US";
        if (YouTubeSearchManager.GUILD_SPOTIFY_CREDENTIALS.containsKey(guildID))
            market = YouTubeSearchManager.GUILD_SPOTIFY_CREDENTIALS.get(guildID).getCountryCode();

        String query = "seed_artists=" + artistInfo.getId()+"&"+
                "seed_genres="+genre+"&"+
                "seed_tracks="+track.getInfo().identifier+"&"+
                "market="+market;
//                "max_acousticness="+trackFeatures.getAcousticness()+"&"+
//                "max_danceability="+trackFeatures.getDanceability()+"&"+
//                "max_duration_ms="+trackFeatures.getDuration_ms()+"&"+
//                "max_energy="+trackFeatures.getEnergy()+"&"+
//                "max_instrumentalness="+trackFeatures.getInstrumentalness()+"&"+
//                "max_key="+trackFeatures.getKey()+"&"+
//                "max_liveness="+trackFeatures.getLiveness()+"&"+
//                "max_loudness="+trackFeatures.getLoudness()+"&"+
//                "max_mode="+trackFeatures.getMode()+"&"+
//                "max_popularity="+artistInfo.getPopularity()+"&"+
//                "max_speechiness="+trackFeatures.getSpeechiness()+"&"+
//                "max_tempo="+trackFeatures.getTempo()+"&"+
//                "max_time_signature="+trackFeatures.getTime_signature()+"&"+
//                "max_valence="+trackFeatures.getValence()
                ;

            AudioItem item = this.spotifySourceManager.getRecommendations(query, false, guildID);
            if (item == AudioReference.NO_TRACK) {
                return new ArrayList<SpotifyAudioTrack>();
            }
            if (!(item instanceof BasicAudioPlaylist)) {
                return new ArrayList<SpotifyAudioTrack>();
            }
            return ((BasicAudioPlaylist)item).getTracks().stream().map(t -> (SpotifyAudioTrack)t).collect(Collectors.toList());
        }
        catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<SpotifyAudioTrack>();
        }
    }

    public List<SpotifyAudioTrack> getAutoPlayListFromName(String name, long guildID) {
        BasicAudioPlaylist list = this.getPlaylistFromSearch(name, guildID);
        if (list == null) {
            return new ArrayList<SpotifyAudioTrack>();
        }
        return this.getAutoPlayList((SpotifyAudioTrack)list.getTracks().get(0), guildID);
    }
}

