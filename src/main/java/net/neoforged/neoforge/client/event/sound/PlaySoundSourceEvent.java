package net.neoforged.neoforge.client.event.sound;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.neoforged.bus.api.Event;
public final class PlaySoundSourceEvent extends Event {
    private final SoundEngine engine; private final Channel channel; private final SoundInstance sound;
    public PlaySoundSourceEvent(SoundEngine engine,Channel channel,SoundInstance sound){this.engine=engine;this.channel=channel;this.sound=sound;}
    public SoundEngine getEngine(){return engine;} public Channel getChannel(){return channel;} public SoundInstance getSound(){return sound;}
}
