package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.compat.SimpleVoiceChatPresence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Reflection-only client bridge so Simple Voice Chat remains an optional dependency. */
public final class SimpleVoiceChatMicrophoneClient {
    private static final String VOICECHAT_CLIENT =
            "de.maxhenkel.voicechat.VoicechatClient";
    private static final String MICROPHONE_MANAGER =
            "de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager";
    private static final String SOUND_MANAGER =
            "de.maxhenkel.voicechat.voice.client.SoundManager";
    private static final String CLIENT_MANAGER =
            "de.maxhenkel.voicechat.voice.client.ClientManager";

    private SimpleVoiceChatMicrophoneClient() {
    }

    public static List<String> devices() {
        if (!SimpleVoiceChatPresence.installed()) return List.of("");
        try {
            Class<?> manager = Class.forName(MICROPHONE_MANAGER);
            Object result = manager.getMethod("deviceNames").invoke(null);
            List<String> devices = new ArrayList<>();
            devices.add("");
            if (result instanceof Iterable<?> iterable) {
                for (Object value : iterable) {
                    if (value instanceof String device && !device.isBlank()
                            && !devices.contains(device)) {
                        devices.add(device);
                    }
                }
            }
            return List.copyOf(devices);
        } catch (ReflectiveOperationException | LinkageError exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not enumerate Simple Voice Chat microphones",
                    exception);
            return List.of("");
        }
    }

    public static String selectedDevice() {
        if (!SimpleVoiceChatPresence.installed()) return "";
        try {
            Object entry = microphoneConfigEntry();
            if (entry == null) return "";
            Method get = entry.getClass().getMethod("get");
            Object selected = get.invoke(entry);
            return selected instanceof String value ? value : "";
        } catch (ReflectiveOperationException | LinkageError exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not read the Simple Voice Chat microphone setting",
                    exception);
            return "";
        }
    }

    public static boolean selectDevice(String rawDevice) {
        if (!SimpleVoiceChatPresence.installed()) return false;
        String device = rawDevice == null ? "" : rawDevice;
        try {
            Object entry = microphoneConfigEntry();
            if (entry == null) return false;

            Method setter = null;
            for (Method method : entry.getClass().getMethods()) {
                if ("set".equals(method.getName())
                        && method.getParameterCount() == 1) {
                    setter = method;
                    break;
                }
            }
            if (setter == null) return false;
            setter.invoke(entry, device);
            entry.getClass().getMethod("save").invoke(entry);
            reloadAudio();
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not change the Simple Voice Chat microphone",
                    exception);
            return false;
        }
    }

    public static String displayName(String rawDevice) {
        if (rawDevice == null || rawDevice.isBlank()) return "Default microphone";
        try {
            Class<?> soundManager = Class.forName(SOUND_MANAGER);
            Object cleaned = soundManager.getMethod("cleanDeviceName", String.class)
                    .invoke(null, rawDevice);
            if (cleaned instanceof String value && !value.isBlank()) return value;
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return rawDevice;
    }

    private static Object microphoneConfigEntry()
            throws ReflectiveOperationException {
        Class<?> voicechatClient = Class.forName(VOICECHAT_CLIENT);
        Field configField = voicechatClient.getField("CLIENT_CONFIG");
        Object config = configField.get(null);
        if (config == null) return null;
        Field microphone = config.getClass().getField("microphone");
        return microphone.get(config);
    }

    private static void reloadAudio() {
        try {
            Class<?> manager = Class.forName(CLIENT_MANAGER);
            Object client = manager.getMethod("getClient").invoke(null);
            if (client != null) client.getClass().getMethod("reloadAudio").invoke(client);
        } catch (ReflectiveOperationException | LinkageError exception) {
            ScpClassifiedDirectiveMod.LOGGER.debug(
                    "Simple Voice Chat audio will use the new microphone on its next reload",
                    exception);
        }
    }
}
