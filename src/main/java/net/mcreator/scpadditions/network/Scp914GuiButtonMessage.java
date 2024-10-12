
package net.mcreator.scpadditions.network;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import net.mcreator.scpadditions.world.inventory.Scp914GuiMenu;
import net.mcreator.scpadditions.procedures.Scp914toVeryFineProcedure;
import net.mcreator.scpadditions.procedures.Scp914toRoughProcedure;
import net.mcreator.scpadditions.procedures.Scp914toFineProcedure;
import net.mcreator.scpadditions.procedures.Scp914toCoarseProcedure;
import net.mcreator.scpadditions.procedures.Scp914to1to1Procedure;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.function.Supplier;
import java.util.HashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class Scp914GuiButtonMessage {
	private final int buttonID, x, y, z;

	public Scp914GuiButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public Scp914GuiButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(Scp914GuiButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(Scp914GuiButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			int buttonID = message.buttonID;
			int x = message.x;
			int y = message.y;
			int z = message.z;
			handleButtonAction(entity, buttonID, x, y, z);
		});
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		Level world = entity.level();
		HashMap guistate = Scp914GuiMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			Scp914toRoughProcedure.execute(world, x, y, z);
		}
		if (buttonID == 1) {

			Scp914toFineProcedure.execute(world, x, y, z);
		}
		if (buttonID == 2) {

			Scp914toCoarseProcedure.execute(world, x, y, z);
		}
		if (buttonID == 3) {

			Scp914toVeryFineProcedure.execute(world, x, y, z);
		}
		if (buttonID == 4) {

			Scp914to1to1Procedure.execute(world, x, y, z);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		ScpAdditionsMod.addNetworkMessage(Scp914GuiButtonMessage.class, Scp914GuiButtonMessage::buffer, Scp914GuiButtonMessage::new, Scp914GuiButtonMessage::handler);
	}
}
