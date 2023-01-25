
package dev.mrflyn.vectunes;

import com.github.topisenpai.lavasrc.applemusic.AppleMusicSourceManager;
import com.github.topisenpai.lavasrc.spotify.SpotifyCredentials;
import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import com.google.gson.JsonParser;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import dev.mrflyn.vectunes.Bot;
import dev.mrflyn.vectunes.ConfigManager;
import dev.mrflyn.vectunes.searchmanagers.SpotifySearchManager;
import dev.mrflyn.vectunes.searchmanagers.YouTubeSearchManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Scanner;
import org.simpleyaml.configuration.file.YamlFile;

public class VecTunes {
    private static DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    public static ConfigManager configManager;
    public static Bot bot;
    public static AudioPlayerManager playerManager;
    public static HashMap<String, String> EMBED_JSONS;
    public static YouTubeSearchManager youTubeSearchManager;
    public static SpotifySearchManager spotifySearchManager;

    public static void main(String[] args) throws FileNotFoundException {
        VecTunes.saveResource("embeds", "playerEmbed.json", false);
        VecTunes.saveResource("embeds", "songFinished.json", false);
        configManager = new ConfigManager(new YamlFile("config.yml"), new YamlFile("buttons.yml"));
        configManager.init();
        YouTubeSearchManager.readAutoCompletes();
        YouTubeSearchManager.readFavourites();
        YouTubeSearchManager.readSpotifyCredentials();
        youTubeSearchManager = new YouTubeSearchManager();
        youTubeSearchManager.validateAutoCompletes();
        EMBED_JSONS = new HashMap<>();
        EMBED_JSONS.put("playerEmbed.json", JsonParser.parseReader(new FileReader("embeds/playerEmbed.json")).toString());
        EMBED_JSONS.put("songFinished.json", JsonParser.parseReader(new FileReader("embeds/songFinished.json")).toString());
        Runtime.getRuntime().addShutdownHook(new Thread(){

            @Override
            public void run() {
                YouTubeSearchManager.saveAutoCompletes();
            }
        });
        new Thread(){
            Scanner sc = new Scanner(System.in);

            @Override
            public void run() {
                while (true) {
                    String[] fullCmd = this.sc.nextLine().split("#");
                    switch (fullCmd[0]) {
                        case "stop":
                            System.exit(0);
                            break;
                        case "del_spotify":
                            if (fullCmd.length<2)return;
                            try{
                                long guildID = Long.parseLong(fullCmd[1]);
                                VecTunes.spotifySearchManager.getSpotifySourceManager().unregisterSpotifyCredentials(guildID);
                                if(YouTubeSearchManager.GUILD_SPOTIFY_CREDENTIALS.containsKey(guildID)) {
                                    YouTubeSearchManager.GUILD_SPOTIFY_CREDENTIALS.remove(guildID);
                                    VecTunes.log("Removed SpotifyCreds for guild: " + guildID);
                                }
                                VecTunes.log("Not Found!: "+ guildID);

                            }catch (Exception e){
                                VecTunes.log("Incorrect format!");
                                return;
                            }
                            break;
                        case "registered_spotify_guilds":
                            for(long key : YouTubeSearchManager.GUILD_SPOTIFY_CREDENTIALS.keySet()){
                                VecTunes.log(key);
                            }
                            break;
                    }
                }
            }
        }.start();
        playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        if (configManager.getMainConfig().getBoolean("spotify.enabled")) {
            SpotifySourceManager spotifySourceManager = new SpotifySourceManager(null, playerManager);
            spotifySourceManager.registerSpotifyCredentials(configManager.getMainConfig().getString("spotify.client_id"),
                    configManager.getMainConfig().getString("spotify.client_secret"),
                    configManager.getMainConfig().getString("spotify.country_code"),
                    -1L
            );
            playerManager.registerSourceManager(spotifySourceManager);

            spotifySearchManager = new SpotifySearchManager(spotifySourceManager);
        }
        playerManager.registerSourceManager(new AppleMusicSourceManager(null, "us", playerManager));
        playerManager.registerSourceManager(new HttpAudioSourceManager());
        playerManager.registerSourceManager(new LocalAudioSourceManager());
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        bot = new Bot(configManager.getMainConfig().getString("token"));
        bot.enable();
    }

    public static <T> void log(T msg) {
        System.out.println("[" + LocalDateTime.now().format(myFormatObj) + "] " + msg.toString());
    }

    public static InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }
        try {
            URL url = VecTunes.class.getClassLoader().getResource(filename);
            if (url == null) {
                return null;
            }
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        }
        catch (IOException var4) {
            return null;
        }
    }

    public static void saveResource(String dest, String resourcePath, boolean replace) {
        block8: {
            if (resourcePath != null && !resourcePath.equals("")) {
                InputStream in = VecTunes.getResource(resourcePath = resourcePath.replace('\\', '/'));
                if (in == null) {
                    throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in ");
                }
                File outFile = new File(dest, resourcePath);
                int lastIndex = resourcePath.lastIndexOf(47);
                File outDir = new File(dest, resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));
                if (!outDir.exists()) {
                    outDir.mkdirs();
                }
                try {
                    int len;
                    if (outFile.exists() && !replace) {
                        VecTunes.log("Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
                        break block8;
                    }
                    FileOutputStream out = new FileOutputStream(outFile);
                    byte[] buf = new byte[1024];
                    while ((len = in.read(buf)) > 0) {
                        ((OutputStream)out).write(buf, 0, len);
                    }
                    ((OutputStream)out).close();
                    in.close();
                }
                catch (IOException var10) {
                    VecTunes.log("Could not save " + outFile.getName() + " to " + outFile);
                    var10.printStackTrace();
                }
            } else {
                throw new IllegalArgumentException("ResourcePath cannot be null or empty");
            }
        }
    }
}

