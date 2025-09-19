package io.github.togar2.pvp.feature.projectile;

import io.github.togar2.pvp.entity.projectile.*;
import io.github.togar2.pvp.feature.FeatureType;
import io.github.togar2.pvp.feature.RegistrableFeature;
import io.github.togar2.pvp.feature.config.DefinedFeature;
import io.github.togar2.pvp.feature.config.FeatureConfiguration;
import io.github.togar2.pvp.feature.cooldown.ItemCooldownFeature;
import io.github.togar2.pvp.feature.fall.FallFeature;
import io.github.togar2.pvp.utils.ViewUtil;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Vanilla implementation of {@link MiscProjectileFeature}
 */
public class VanillaMiscProjectileFeature implements MiscProjectileFeature, RegistrableFeature {
	public static final DefinedFeature<VanillaMiscProjectileFeature> DEFINED = new DefinedFeature<>(
			FeatureType.MISC_PROJECTILE, VanillaMiscProjectileFeature::new,
			FeatureType.ITEM_COOLDOWN, FeatureType.FALL
	);

	private static int snowballCounter = 0;

	private final FeatureConfiguration configuration;
	
	private ItemCooldownFeature itemCooldownFeature;
	private FallFeature fallFeature;
	
	public VanillaMiscProjectileFeature(FeatureConfiguration configuration) {
		this.configuration = configuration;
	}
	
	@Override
	public void initDependencies() {
		this.itemCooldownFeature = configuration.get(FeatureType.ITEM_COOLDOWN);
		this.fallFeature = configuration.get(FeatureType.FALL);
	}
	
	@Override
	public void init(EventNode<EntityInstanceEvent> node) {
		node.addListener(PlayerUseItemEvent.class, event -> {
			if (event.getItemStack().material() != Material.SNOWBALL
					&& event.getItemStack().material() != Material.EGG
					&& event.getItemStack().material() != Material.ENDER_PEARL)
				return;

			Player player = event.getPlayer();
			ItemStack stack = event.getItemStack();
			
			boolean snowball = stack.material() == Material.SNOWBALL;
			boolean enderpearl = stack.material() == Material.ENDER_PEARL;
			
			SoundEvent soundEvent;
			CustomEntityProjectile projectile;
			if (snowball) {
				soundEvent = SoundEvent.ENTITY_SNOWBALL_THROW;
				projectile = new Snowball(player);
			} else if (enderpearl) {
				soundEvent = SoundEvent.ENTITY_ENDER_PEARL_THROW;
				projectile = new ThrownEnderpearl(player, fallFeature);
			} else {
				soundEvent = SoundEvent.ENTITY_EGG_THROW;
				projectile = new ThrownEgg(player);
			}
			
			((ItemHoldingProjectile) projectile).setItem(stack);
			
			ThreadLocalRandom random = ThreadLocalRandom.current();
			ViewUtil.viewersAndSelf(player).playSound(Sound.sound(
					soundEvent,
					snowball || enderpearl ? Sound.Source.NEUTRAL : Sound.Source.PLAYER,
					0.5f, 0.4f / (random.nextFloat() * 0.4f + 0.8f)
			), player);
			
			if (enderpearl) {
				itemCooldownFeature.setCooldown(player, Material.ENDER_PEARL, 20);
			}

			System.out.println("Creating projectile entity #" + (++snowballCounter));
			Pos position = player.getPosition().add(0, player.getEyeHeight(), 0);

			//System.out.println("Before shootFromRotation");
			projectile.shootFromRotation(position.pitch(), position.yaw(), 0, 1.5, 1.0);

			Vec direction = position.direction();
			Vec velocity = direction.mul(1.5);
			projectile.setVelocity(velocity);

			System.out.println("Velocity set to: " + projectile.getVelocity());

			// Add player velocity BEFORE spawning
			Vec playerVel = player.getVelocity();
			projectile.setVelocity(projectile.getVelocity().add(playerVel.x(),
					player.isOnGround() ? 0.0D : playerVel.y(), playerVel.z()));

			System.out.println("About to spawn snowball #" + snowballCounter + " with ID: " + projectile.getEntityId());
			// NOW spawn it with the correct velocity already set
			projectile.setInstance(Objects.requireNonNull(player.getInstance()), position.withView(projectile.getPosition()));

			System.out.println("Spawned snowball #" + snowballCounter);

			if (player.getGameMode() != GameMode.CREATIVE) {
				player.setItemInHand(event.getHand(), stack.withAmount(stack.amount() - 1));
			}
		});
	}
}
