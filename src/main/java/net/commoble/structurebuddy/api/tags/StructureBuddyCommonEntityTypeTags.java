package net.commoble.structurebuddy.api.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/// entity_type c tags added by StructureBuddy
public final class StructureBuddyCommonEntityTypeTags
{
	private StructureBuddyCommonEntityTypeTags() {} // utility class
	
	private static final Identifier c(String path)
	{
		return Identifier.fromNamespaceAndPath("c", path);
	}
	
	/// #c:item_frames
	public static final TagKey<EntityType<?>> ITEM_FRAMES = TagKey.create(Registries.ENTITY_TYPE, c("item_frames"));}
