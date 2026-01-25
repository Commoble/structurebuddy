package net.commoble.structurebuddy.api;

import java.util.EnumSet;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * Context for baking a {@link BoxElement} into a {@link BoundingBox} of a known size
 * (as opposed to using jigsaw connections).
 * @param generationContext GenerationContext
 * @param box BoundingBox which the element is allowed to generate into.
 * The entire cuboid should be considered available space.
 * Coordinates may be in any arbitrary reference frame.
 * @param boundingSurfaces Directions of walls/floor/ceiling which bound this box.
 * Elements which are smaller than the available box may wish to "snap" to one of these edges.
 * @param data JigsawDataReader providing read access to data shared by other pieces selected to be placed into the tree.
 * Data retrieved from the reader should not be modified or mutated as your element has not yet been selected for placing.
 * @param rotation Rotation
 * @param liquidSettings LiquidSettings
 */
public record BoxBakeContext(
	GenerationContext generationContext,
	BoundingBox box,
	EnumSet<Direction> boundingSurfaces,
	JigsawDataReader data,
	Rotation rotation,
	LiquidSettings liquidSettings)
{

}
