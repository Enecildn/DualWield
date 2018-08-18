package com.enecildn.dualwield;

import java.util.EnumMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;

import net.minecraft.server.v1_12_R1.EnumParticle;
import net.minecraft.server.v1_12_R1.Packet;
import net.minecraft.server.v1_12_R1.PacketPlayOutAnimation;
import net.minecraft.server.v1_12_R1.PacketPlayOutWorldParticles;

public class DualWield extends JavaPlugin implements Listener
{
	@Override
	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if (event.getHand() != null)
		{
			if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
			{
				Player player = event.getPlayer();
				ItemStack itemStack = player.getInventory().getItemInOffHand();
				if (checkItem(itemStack))
				{
					sendPacket(new PacketPlayOutAnimation(((CraftPlayer) player).getHandle(), 3));
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
	{
		if (event.getRightClicked() instanceof LivingEntity)
		{
			Player player = event.getPlayer();
			LivingEntity livingEntity = (LivingEntity) event.getRightClicked();
			ItemStack itemStack = player.getInventory().getItemInOffHand();
			if (checkItem(itemStack))
			{
				if (isDamagedByEntity(player, livingEntity))
				{
					if (!event.isCancelled())
					{
						double damage = getDamage(itemStack);
						if (!player.isOnGround())
						{
							damage *= 1.5;
							sendPacket(new PacketPlayOutWorldParticles(EnumParticle.CRIT, false, (float) livingEntity.getLocation().getX(), (float) (livingEntity.getLocation().getY() + 1), (float) livingEntity.getLocation().getZ(), 0.5F, 0.5F, 0.5F, 0, 20, null));
						}
						if (player.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE))
						{
							for (PotionEffect potionEffect : player.getActivePotionEffects())
							{
								if (potionEffect.getType() == PotionEffectType.INCREASE_DAMAGE)
								{
									damage += 3 * Math.pow(2, potionEffect.getAmplifier());
								}
							}
						}
						if (itemStack.containsEnchantment(Enchantment.DAMAGE_ALL))
						{
							damage += 1 + (itemStack.getEnchantmentLevel(Enchantment.DAMAGE_ALL) - 1) / 2;
							sendPacket(new PacketPlayOutWorldParticles(EnumParticle.CRIT_MAGIC, false, (float) livingEntity.getLocation().getX(), (float) (livingEntity.getLocation().getY() + 1), (float) livingEntity.getLocation().getZ(), 0.5F, 0.5F, 0.5F, 0, 20, null));
						}
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 10, 1);
						livingEntity.damage(damage, player);
						sendPacket(new PacketPlayOutWorldParticles(EnumParticle.DAMAGE_INDICATOR, false, (float) livingEntity.getLocation().getX(), (float) (livingEntity.getLocation().getY() + 1), (float) livingEntity.getLocation().getZ(), 0.5F, 0.5F, 0.5F, 0, 3, null));
						if (player.getGameMode() != GameMode.CREATIVE && !itemStack.getItemMeta().isUnbreakable())
						{
							if (itemStack.getType().getMaxDurability() - itemStack.getDurability() >= 1)
							{
								itemStack.setDurability((short) (itemStack.getDurability() + 1));
							}
							else
							{
								player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 10, 1);
								player.getInventory().setItemInOffHand(null);
							}
						}
						sendPacket(new PacketPlayOutAnimation(((CraftPlayer) player).getHandle(), 3));
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	private boolean checkItem(ItemStack itemStack)
	{
		return itemStack != null && itemStack.getType() == Material.WOOD_SWORD || itemStack.getType() == Material.STONE_SWORD || itemStack.getType() == Material.IRON_SWORD || itemStack.getType() == Material.GOLD_SWORD || itemStack.getType() == Material.DIAMOND_SWORD;
	}
	
	private void sendPacket(Packet<?> packet)
	{
		for (Player player : Bukkit.getServer().getOnlinePlayers())
		{
			((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
		}
	}
	
	@SuppressWarnings("deprecation")
	private boolean isDamagedByEntity(Player player, Entity entity)
	{
		EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, entity, DamageCause.ENTITY_ATTACK, new EnumMap<EntityDamageEvent.DamageModifier, Double>(ImmutableMap.of(EntityDamageEvent.DamageModifier.BASE, 1D)), new EnumMap<EntityDamageEvent.DamageModifier, Function<Object, Double>>(ImmutableMap.of(EntityDamageEvent.DamageModifier.BASE, Functions.constant(0D))));
		Bukkit.getServer().getPluginManager().callEvent(event);
		return !event.isCancelled();
	}
	
	private double getDamage(ItemStack itemStack)
	{
		switch (itemStack.getType())
		{
			case WOOD_SWORD:
				return 4;
			case STONE_SWORD:
				return 5;
			case IRON_SWORD:
				return 6;
			case GOLD_SWORD:
				return 4;
			case DIAMOND_SWORD:
				return 7;
			default:
				return 1;
		}
	}
}
