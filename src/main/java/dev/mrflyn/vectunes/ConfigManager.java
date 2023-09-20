
package dev.mrflyn.vectunes;

import dev.mrflyn.vectunes.VecTunes;
import org.simpleyaml.configuration.file.YamlFile;

import java.util.Arrays;

public class ConfigManager {
    private YamlFile mainConfig;
    private YamlFile buttonConfig;

    public ConfigManager(YamlFile mainFile, YamlFile buttonFile) {
        this.mainConfig = mainFile;
        this.buttonConfig = buttonFile;
    }

    public void init() {
        try {
            if (!this.mainConfig.exists()) {
                VecTunes.log("New file has been created: " + this.mainConfig.getFilePath() + "\n");
                this.mainConfig.createNewFile(true);
                this.mainConfig.addDefault("token", "<botToken>");
                this.mainConfig.addDefault("premium_bots", Arrays.asList("1234:15701557", "1235:3141152",
                        "1234:8934895", "12356:13836756"));
                this.mainConfig.addDefault("premium_bot_links", Arrays.asList(
                        "https://discord.com/api/oauth2/authorize?client_id=1153609192234553354&permissions=556235091264&scope=bot%20applications.commands",
                        "https://discord.com/api/oauth2/authorize?client_id=1153702608805056522&permissions=556235091264&scope=applications.commands%20bot",
                        "https://discord.com/api/oauth2/authorize?client_id=1153703545816744037&permissions=556235091264&scope=bot%20applications.commands",
                        "https://discord.com/api/oauth2/authorize?client_id=1153704213910655137&permissions=556235091264&scope=applications.commands%20bot"));
                this.mainConfig.addDefault("debug", true);
//                spotify:
//                enabled: true
//                client_id: 1613db80ba9f4790bba00ad65841cb93
//                client_secret: 561dbd04c99a430891926de9d2a3c007
//                country_code: US
                this.mainConfig.addDefault("spotify.enabled", true);
                this.mainConfig.addDefault("spotify.client_id", "1613db80ba9f4790bba00ad65841cb93");
                this.mainConfig.addDefault("spotify.client_secret", "561dbd04c99a430891926de9d2a3c007");
                this.mainConfig.addDefault("spotify.country_code", "US");
                this.mainConfig.save();
            } else {
                VecTunes.log(this.mainConfig.getFilePath() + " already exists, loading configurations...\n");
            }
            this.mainConfig.load();
            if (!this.buttonConfig.exists()) {
                VecTunes.log("New file has been created: " + this.buttonConfig.getFilePath() + "\n");
                this.buttonConfig.createNewFile(true);
                this.buttonConfig.addDefault("play.emoji", "<:Play:1028291915852042300>");
                this.buttonConfig.addDefault("pause.emoji", "<:Pause:1028291907291463710>");
                this.buttonConfig.addDefault("stop.emoji", "<:Stop:1028291878589825145>");
                this.buttonConfig.addDefault("autoplay_off.emoji", "<:AutoPlay_OFF:1028291889436303430>");
                this.buttonConfig.addDefault("autoplay_on.emoji", "<:AutoPlay_ON:1028291891575402537>");
                this.buttonConfig.addDefault("forward.emoji", "<:Forward:1028291900618317915>");
                this.buttonConfig.addDefault("rewind.emoji", "<:Rewind:1028291920440602685>");
                this.buttonConfig.addDefault("skip.emoji", "<:Skip:1028291922349011005>");
                this.buttonConfig.addDefault("queue_loop_off.emoji", "<:LoopOff:1028291880364019733>");
                this.buttonConfig.addDefault("queue_loop_on.emoji", "<:LoopOn:1028291911687078008>");
                this.buttonConfig.addDefault("song_loop_off.emoji", "<:LoopQueue:1028291913813606420>");
                this.buttonConfig.addDefault("song_loop_on.emoji", "<:LoopQueue:1028291913813606420>");
                this.buttonConfig.addDefault("volume.emoji", "<:Volume:1028291884692553810>");
                this.buttonConfig.addDefault("jump_track.emoji", "<:Jump:1084877137619734578>");
                this.buttonConfig.addDefault("favourite.emoji", "<:FavoriteSong:1028291898483425300>");
                this.buttonConfig.addDefault("shuffle_on.emoji", "<:Shuffle:1034499896507641956>");
                this.buttonConfig.addDefault("shuffle_off.emoji", "<:Shuffle:1034499896507641956>");


                this.buttonConfig.addDefault("play.display_name", "");
                this.buttonConfig.addDefault("pause.display_name", "");
                this.buttonConfig.addDefault("stop.display_name", "");
                this.buttonConfig.addDefault("autoplay_off.display_name", "Autoplay: Off");
                this.buttonConfig.addDefault("autoplay_on.display_name", "Autoplay: On");
                this.buttonConfig.addDefault("forward.display_name", "+10s");
                this.buttonConfig.addDefault("rewind.display_name", "-10s");
                this.buttonConfig.addDefault("skip.display_name", "");
                this.buttonConfig.addDefault("queue_loop_off.display_name", "QueueLoop: Off");
                this.buttonConfig.addDefault("queue_loop_on.display_name", "QueueLoop: On");
                this.buttonConfig.addDefault("song_loop_off.display_name", "TrackLoop: Off");
                this.buttonConfig.addDefault("song_loop_on.display_name", "TrackLoop: On");
                this.buttonConfig.addDefault("volume.display_name", "%volume%%");
                this.buttonConfig.addDefault("jump_track.display_name", "Jump Track");
                this.buttonConfig.addDefault("favourite.display_name", "");
                this.buttonConfig.addDefault("shuffle_on.display_name", "Shuffle: On");
                this.buttonConfig.addDefault("shuffle_off.display_name", "Shuffle: Off");


                this.buttonConfig.addDefault("play.type", "SECONDARY");
                this.buttonConfig.addDefault("pause.type", "SECONDARY");
                this.buttonConfig.addDefault("stop.type", "SECONDARY");
                this.buttonConfig.addDefault("autoplay_off.type", "SECONDARY");
                this.buttonConfig.addDefault("autoplay_on.type", "SUCCESS");
                this.buttonConfig.addDefault("forward.type", "SECONDARY");
                this.buttonConfig.addDefault("rewind.type", "SECONDARY");
                this.buttonConfig.addDefault("skip.type", "SECONDARY");
                this.buttonConfig.addDefault("queue_loop_off.type", "SECONDARY");
                this.buttonConfig.addDefault("queue_loop_on.type", "SUCCESS");
                this.buttonConfig.addDefault("song_loop_off.type", "SECONDARY");
                this.buttonConfig.addDefault("song_loop_on.type", "SUCCESS");
                this.buttonConfig.addDefault("volume.type", "SECONDARY");
                this.buttonConfig.addDefault("jump_track.type", "SECONDARY");
                this.buttonConfig.addDefault("favourite.type", "SECONDARY");
                this.buttonConfig.addDefault("shuffle_on.type", "SUCCESS");
                this.buttonConfig.addDefault("shuffle_off.type", "SECONDARY");
                this.buttonConfig.save();
            } else {
                VecTunes.log(this.buttonConfig.getFilePath() + " already exists, loading configurations...\n");
            }
            this.buttonConfig.load();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public YamlFile getMainConfig() {
        return this.mainConfig;
    }

    public YamlFile getButtonConfig() {
        return this.buttonConfig;
    }
}

