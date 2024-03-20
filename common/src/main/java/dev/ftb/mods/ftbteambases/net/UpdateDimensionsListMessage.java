package dev.ftb.mods.ftbteambases.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteambases.client.FTBTeamBasesClient;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Set;

public class UpdateDimensionsListMessage extends BaseS2CMessage {
	private final boolean add;
	private final Set<ResourceKey<Level>> dimensions;

	public UpdateDimensionsListMessage(Collection<ResourceKey<Level>> dimensions, boolean add) {
		this.dimensions = Set.copyOf(dimensions);
		this.add = add;
	}

	public UpdateDimensionsListMessage(FriendlyByteBuf buf) {
		this.dimensions = Set.copyOf(buf.readList(buf1 -> ResourceKey.create(Registries.DIMENSION, buf1.readResourceLocation())));
		this.add = buf.readBoolean();
	}

	@Override
	public MessageType getType() {
		return FTBTeamBasesNet.UPDATE_DIMENSION_LIST;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeCollection(dimensions, (buf1, levelResourceKey) -> buf1.writeResourceLocation(levelResourceKey.location()));
		buf.writeBoolean(this.add);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		context.queue(() -> {
			Set<ResourceKey<Level>> levels = FTBTeamBasesClient.playerLevels(context.getPlayer());
			if (add) {
                levels.addAll(dimensions);
            } else {
                levels.removeAll(dimensions);
            }
		});
	}
}
