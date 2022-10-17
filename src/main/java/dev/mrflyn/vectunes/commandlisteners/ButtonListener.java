
package dev.mrflyn.vectunes.commandlisteners;

import dev.mrflyn.vectunes.Bot;
import dev.mrflyn.vectunes.GUIManager;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ButtonListener
extends ListenerAdapter {
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getGuild() == null) {
            return;
        }
        if (event.getMember() == null) {
            return;
        }
        if (event.getMember().getVoiceState() == null) {
            return;
        }
        if (!event.getMember().getVoiceState().inAudioChannel()) {
            return;
        }
        if (event.getMember().getVoiceState().getChannel() == null) {
            return;
        }
        if (!event.getMember().getVoiceState().getChannel().getMembers().contains(event.getGuild().getMember(Bot.jda.getSelfUser()))) {
            return;
        }
        String[] ids = event.getComponentId().split(":");
        if (ids.length < 2) {
            return;
        }
        GUIManager guiManager = GUIManager.registeredManagers.get(ids[0]);
        if (guiManager == null) {
            return;
        }
        guiManager.onButtonClick(event, ids[1]);
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getGuild() == null) {
            return;
        }
        if (event.getMember() == null) {
            return;
        }
        if (event.getMember().getVoiceState() == null) {
            return;
        }
        if (!event.getMember().getVoiceState().inAudioChannel()) {
            return;
        }
        if (event.getMember().getVoiceState().getChannel() == null) {
            return;
        }
        if (!event.getMember().getVoiceState().getChannel().getMembers().contains(event.getGuild().getMember(Bot.jda.getSelfUser()))) {
            return;
        }
        String[] ids = event.getModalId().split(":");
        if (ids.length < 2) {
            return;
        }
        if (!ids[1].equals("volume_modal")) {
            return;
        }
        try {
            Integer.parseInt(event.getValue("volume_subject").getAsString());
        }
        catch (Exception e) {
            return;
        }
        int volume = Integer.parseInt(event.getValue("volume_subject").getAsString());
        if (volume < 0 || volume > 100) {
            return;
        }
        GUIManager guiManager = GUIManager.registeredManagers.get(ids[0]);
        if (guiManager == null) {
            return;
        }
        guiManager.getTrackManager().getPlayer().setVolume(volume);
        guiManager.update();
        event.deferEdit().queue();
    }
}

