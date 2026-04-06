package me.pepperbell.continuity.client.resource;

import java.util.Set;

import net.minecraft.resources.Identifier;

public interface SpriteSourceListInitContext {
	ThreadLocal<SpriteSourceListInitContext> THREAD_LOCAL = new ThreadLocal<>();

	Set<Identifier> getExtraIds();
}
