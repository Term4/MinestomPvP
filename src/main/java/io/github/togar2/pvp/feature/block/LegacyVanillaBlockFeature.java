package io.github.togar2.pvp.feature.block;

import io.github.togar2.pvp.feature.FeatureType;
import io.github.togar2.pvp.feature.RegistrableFeature;
import io.github.togar2.pvp.feature.config.DefinedFeature;
import io.github.togar2.pvp.feature.config.FeatureConfiguration;
import io.github.togar2.pvp.utils.CombatVersion;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.*;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerDiggingPacket;
import net.minestom.server.network.packet.client.play.ClientUseItemPacket;
import net.minestom.server.tag.Tag;


/**
 * Vanilla implementation of {@link LegacyBlockFeature}
 */
public class LegacyVanillaBlockFeature extends VanillaBlockFeature
		implements LegacyBlockFeature, RegistrableFeature {
	public static final DefinedFeature<LegacyVanillaBlockFeature> SHIELD = new DefinedFeature<>(
			FeatureType.LEGACY_BLOCK, configuration -> new LegacyVanillaBlockFeature(configuration, ItemStack.of(Material.SHIELD)),
			LegacyVanillaBlockFeature::initPlayer,
			FeatureType.ITEM_DAMAGE
	);

	public static final Tag<Boolean> BLOCKING_SWORD = Tag.Boolean("blockingSword");
	public static final Tag<ItemStack> BLOCK_REPLACEMENT_ITEM = Tag.ItemStack("blockReplacementItem");

	private final ItemStack blockingItem;

	public LegacyVanillaBlockFeature(FeatureConfiguration configuration, ItemStack blockingItem) {
		super(configuration.add(FeatureType.VERSION, CombatVersion.LEGACY));
		this.blockingItem = blockingItem;
	}

	public static void initPlayer(Player player, boolean firstInit) {
		player.setTag(BLOCKING_SWORD, false);
	}

	@Override
	public void init(EventNode<EntityInstanceEvent> node) {
		// Handle item swapping and slot changes
		node.addListener(PlayerSwapItemEvent.class, this::handleSwapItem);
		node.addListener(PlayerChangeHeldSlotEvent.class, this::handleChangeSlot);

		// Packet-based blocking detection without replacing default handlers
		node.addListener(PlayerPacketEvent.class, event -> {
			Player player = event.getPlayer();
			ClientPacket packet = event.getPacket();

			// Detect right-click with sword to start blocking
			if (packet instanceof ClientUseItemPacket) {
				ItemStack mainHand = player.getItemInMainHand();
				if (canBlockWith(player, mainHand) && !isBlocking(player)) {
					System.out.println("Blocking " + player.getUsername());
					block(player);
				}
				return;
			}

			// Detect release of right-click to stop blocking
			if (packet instanceof ClientPlayerDiggingPacket digging) {
				if (digging.status() == ClientPlayerDiggingPacket.Status.UPDATE_ITEM_STATE && isBlocking(player)) {
					System.out.println("Unblocking " + player.getUsername());
					unblock(player);
				}
			}
		});
	}

	@Override
	public boolean isBlocking(Player player) {
		return player.getTag(BLOCKING_SWORD);
	}

	@Override
	public void block(Player player) {
		if (!isBlocking(player)) {
			player.setTag(BLOCK_REPLACEMENT_ITEM, player.getItemInOffHand());
			player.setTag(BLOCKING_SWORD, true);

			player.setItemInOffHand(blockingItem);
			player.refreshActiveHand(true, true, false);
			player.sendPacketToViewersAndSelf(player.getMetadataPacket());
		}
	}

	@Override
	public void unblock(Player player) {
		if (isBlocking(player)) {
			player.setTag(BLOCKING_SWORD, false);
			player.setItemInOffHand(player.getTag(BLOCK_REPLACEMENT_ITEM));
			player.removeTag(BLOCK_REPLACEMENT_ITEM);

			// Add these lines to update the visual state for all viewers
			player.refreshActiveHand(false, false, false);
			player.sendPacketToViewersAndSelf(player.getMetadataPacket());
		}
	}

	protected void handleSwapItem(PlayerSwapItemEvent event) {
		Player player = event.getPlayer();
		if (player.getItemInOffHand().isSimilar(blockingItem) && isBlocking(player))
			event.setCancelled(true);
	}

	protected void handleChangeSlot(PlayerChangeHeldSlotEvent event) {
		Player player = event.getPlayer();
		if (player.getItemInOffHand().isSimilar(blockingItem) && isBlocking(player))
			unblock(player);
	}

	@Override
	public boolean canBlockWith(Player player, ItemStack stack) {
		return stack.material().registry().key().value().contains("sword");
	}
}
