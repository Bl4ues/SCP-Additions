package net.mcreator.scpadditions.facility.elevator;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * Canonical resource, animation, bone and locator names for the Core Room
 * elevator assets authored in Blockbench.
 */
public final class ElevatorAssets {
    public static final ResourceLocation CARRIAGE_MODEL = resource(
            "geo/entity/core_room_elevator_carriage.geo.json");
    public static final ResourceLocation CARRIAGE_TEXTURE = resource(
            "textures/entities/core_room_elevator_carriage.png");
    public static final ResourceLocation CARRIAGE_ANIMATION = resource(
            "animations/entity/core_room_elevator_carriage.animation.json");

    public static final ResourceLocation FLOOR_STATION_MODEL = resource(
            "geo/block/core_room_elevator_floor_station.geo.json");
    public static final ResourceLocation FLOOR_STATION_TEXTURE = resource(
            "textures/entities/core_room_elevator_floor_station.png");
    public static final ResourceLocation FLOOR_STATION_ANIMATION = resource(
            "animations/block/core_room_elevator_floor_station.animation.json");

    public static final ResourceLocation PULLEY_MODEL = resource(
            "geo/block/core_room_elevator_pulley.geo.json");
    public static final ResourceLocation PULLEY_TEXTURE = resource(
            "textures/block/elevator_pulley.png");

    public static final String CARRIAGE_CLOSED =
            "animation.core_room_elevator_carriage.closed";
    public static final String CARRIAGE_OPENING =
            "animation.core_room_elevator_carriage.opening";
    public static final String CARRIAGE_OPEN =
            "animation.core_room_elevator_carriage.open";
    public static final String CARRIAGE_CLOSING =
            "animation.core_room_elevator_carriage.closing";

    public static final String STATION_CLOSED =
            "animation.core_room_elevator_floor_station.closed";
    public static final String STATION_OPENING =
            "animation.core_room_elevator_floor_station.opening";
    public static final String STATION_OPEN =
            "animation.core_room_elevator_floor_station.open";
    public static final String STATION_CLOSING =
            "animation.core_room_elevator_floor_station.closing";

    public static final String CABIN = "cabin";
    public static final String STATIC_BODY = "static_body";
    public static final String WINDOW = "window";
    public static final String BEAM = "beam";
    public static final String DOOR_LEFT = "door_left";
    public static final String DOOR_RIGHT = "door_right";
    public static final String BUTTONS = "buttons";
    public static final String BUTTON_UP = "button_up";
    public static final String BUTTON_DOWN = "button_down";

    public static final String STATION = "station";
    public static final String FRONT_PLATE = "front_plate";
    public static final String DOOR_PLATE = "door_plate";
    public static final String BACK_DOOR_PLATE = "back_door_plate";
    public static final String PULLEY = "pulley";

    public static final String CABLE_ATTACHMENT_FRONT =
            "cable_attachment_front";
    public static final String CABLE_ATTACHMENT_REAR =
            "cable_attachment_rear";
    public static final String CABLE_ORIGIN_FRONT = "cable_origin_front";
    public static final String CABLE_ORIGIN_REAR = "cable_origin_rear";

    private ElevatorAssets() {
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(ScpAdditionsMod.MODID, path);
    }
}
