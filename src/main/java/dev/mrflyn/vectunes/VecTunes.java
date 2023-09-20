
package dev.mrflyn.vectunes;



import com.github.topi314.lavasrc.spotify.SpotifySourceManager;
import com.google.gson.JsonObject;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.simpleyaml.configuration.file.YamlFile;

public class VecTunes {
    //TODO: When premium bot in vc and main bot not in vc, on /play main bot joins but technically the premium bot
    // which is already there in the vc should queue the song.
    //TODO: GUI interaction of the premium bots dont work.
    private static DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    public static ConfigManager configManager;
    public static Bot mainBot;
    public static ArrayList<Bot> premiumBots = new ArrayList<>();
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
        YouTubeSearchManager.readPremiumGuilds();
        youTubeSearchManager = new YouTubeSearchManager();
        youTubeSearchManager.validateAutoCompletes();
        EMBED_JSONS = new HashMap<>();
        EMBED_JSONS.put("playerEmbed.json", JsonParser.parseReader(new FileReader("embeds/playerEmbed.json")).toString());
        EMBED_JSONS.put("songFinished.json", JsonParser.parseReader(new FileReader("embeds/songFinished.json")).toString());
        Runtime.getRuntime().addShutdownHook(new Thread(){

            @Override
            public void run() {
                YouTubeSearchManager.savePersistentData();
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
//        playerManager.registerSourceManager(new AppleMusicSourceManager(null, "us", playerManager));
        playerManager.registerSourceManager(new HttpAudioSourceManager());
        playerManager.registerSourceManager(new LocalAudioSourceManager());
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        mainBot = new Bot(configManager.getMainConfig().getString("token"));
        mainBot.enable();
        for(String token : configManager.getMainConfig().getStringList("premium_bots")){
            Bot bot = new Bot(token.split(":")[0], true);
            bot.enable();
            premiumBots.add(bot);
        }
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

    public static long skyNodeRemainingTimeSeconds(){
        final HttpGet request = new HttpGet("https://panel.skynode.pro/api/client/servers/3f576c30");
        final List<Header> headers = Arrays.asList(
                new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
                new BasicHeader(HttpHeaders.ACCEPT, "application/json"),
                new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer ptlc_NjwZpOvcrVY1i5KcEDZ2pueG6Vhnn0d3aVZNsKuxKHR"));

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultHeaders(headers)
                .build()) {

            String response = client.execute(request, new BasicResponseHandler());
            JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
            String expiresAT = obj.get("attributes").getAsJsonObject().get("expires_at").getAsString();

            LocalDateTime dateTimeex = LocalDateTime.parse(expiresAT.substring(0, expiresAT.length()-1));
            LocalDateTime dateTime = LocalDateTime.now(ZoneOffset.UTC);
            return Duration.between(dateTime, dateTimeex).toSeconds();
        }catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }
}

