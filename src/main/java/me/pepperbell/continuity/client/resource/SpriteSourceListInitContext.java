package me.pepperbell.continuity.client.resource;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;

public interface SpriteSourceListInitContext {
	ThreadLocal<SpriteSourceListInitContext> THREAD_LOCAL = new ThreadLocal<>();

	@Nullable
	Set<Identifier> getExtraIds();
}
