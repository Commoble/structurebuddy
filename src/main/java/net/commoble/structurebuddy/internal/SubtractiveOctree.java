package net.commoble.structurebuddy.internal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.commoble.structurebuddy.api.AvailableSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Representation of a Point-Region Octree in 3D integer space, designed for use with jigsaw structures.
 * Jigsaw Structures start with one large AABB and carve out negative space from it as pieces are assembled.
 * We use this octree to model this instead of vanilla voxelshapes as it's more efficient.
 * 
 * The octree represents all integer-space points within its bounding box, inclusive on both min and max edges.
 * A 2x2x2 octree has points at all eight combinations of x=[0,1], y=[0,1], z=[0,1], and its min and max corners
 * exist at [0,0,0] and [1,1,1].
 * 
 * If an octree has size 1 along a dimension, its min and max bounds for that dimension are equal.
 *  
 * Suppose a region of points overlaps the octree at exactly one point (a corner). Do we consider it to
 * subdivide the octree or not?
 * 
 * Let's say it overlaps at [0,0,0] of the 2x2x2 octree
 * We *should* subdivide the 2x2x2 octree, because there is one octant that overlaps and seven that do not.
 * We can say the same for overlaps of the other seven corners as well.
 * 
 * How do we define the overlapping point? *Do* we define the overlapping point?
 * if A is the initial bounding box and P is the overlapping point,
 * Then along each dimension, we set new bounds at [A.min, P] and [P+1, A.max]
 * 
 * In the 2x2x2 octree, if P is a maximum corner and P.x = 0, then we divide the x-axis into [0,0] and [1,1]
 * if P.x = 1, then we divide the x-axis into [0,1] and [2,1]?
 * this is obviously wrong, so if P is a maximum corner and == A.maxCorner, then do not subdivide
 * 
 * if P is a minimum corner, we do not subdivide if P = A.minCorner
 * 
 * 
 * Okay, now what about our states
 * For a negative-space octree, we have three states
 * 
 * 1) not subtracted and not subdivided
 * 2) subdivided but not subtracted
 * 3) subtracted (if octree has been removed then we don't care about subdivisions
 * 
 * how do we best normalize this three-state situation?
 * 1) nullable optional (ugly)
 * 2) multiple subclasses of the octree (doesn't work because we would need the parent to declare it empty)
 * 3) multiple subclasses of the subdivision holder
 * 4) redundant fields
 */
public interface SubtractiveOctree extends AvailableSpace
{
	/**
	 * {@return True if this octree has been entirely subtracted after this operation (whether this
	 * operation was responsible for the subtraction or if it was already subtracted), false otherwise}
	 * @param subtractionBounds The bounding box to attempt to subtract from this octree
	 */
	public abstract boolean subtract(BoundingBox subtractionBounds);
	
	/**
	 * Represents an octree which did not start empty (but may have been subtracted sufficiently to be come completely empty afterward)
	 */
	public static class NonEmpty implements SubtractiveOctree
	{
		private final int depth;
		private boolean empty = false;
		private @Nonnull BoundingBox bounds;
		private @Nullable SubdivisionHolder subdivisionHolder = null;
		
		/**
		 * Constructs a NonEmpty at depth 0
		 * @param bounds Non-null bounds with size >= 1 on all axes (use Empty to represent volume=0)
		 */
		public NonEmpty(@Nonnull BoundingBox bounds)
		{
			this(bounds, 0);
		}
		/**
		 * Constructor
		 * @param bounds Non-null bounds with size >= 1 on all axes (use Empty to represent volume=0)
		 * @param depth how deep within the octree we are
		 */
		public NonEmpty(@Nonnull BoundingBox bounds, int depth)
		{
			this.bounds = bounds;
			this.depth = depth;
		}
		
		/**
		 * Subtracts the given BoundingBox from this octree
		 * @param subtractionBounds BoundingBox to subtract from this octree
		 * @return true if this octree is empty after the subtraction
		 */
		public boolean subtract(@Nonnull BoundingBox subtractionBounds)
		{
			// if we've already been totally subtracted, can't subtract further
			if (this.empty)
				return true;
			
			// get the intersection of our base box and the subtraction box
			@Nullable BoundingBox clampedSubtractionBounds = intersection(this.bounds, subtractionBounds);
			// if subtraction bounds is entirely outside our octree, ignore it
			if (clampedSubtractionBounds == null)
				return false;
			
			// if our octree is entirely within the subtraction bounds, mark us as not present
			if (doesBoxEncapsulate(subtractionBounds, this.bounds))
			{
				this.empty = true;
				return true;
			}
			
			// (if our AABB volume is 1, we will not reach this point --
			// -- all other boxes either completely overlap us or do not overlap us)
			
			// now we know the subtraction region partially overlaps this octree and we need to subtract something
			// if we're not already subdivided, subdivide it first
			if (this.subdivisionHolder == null)
			{
				this.subdivisionHolder = this.makeSubdivisions(clampedSubtractionBounds);
			}
			
			// subtract each subdivision
			boolean subdivisionsRemaining = false;
			SubtractiveOctree[] subdivisions = this.subdivisionHolder.subdivisions();
			for (int i=0; i< subdivisions.length; i++)
			{
				if (subdivisions[i].subtract(subtractionBounds))
				{
					subdivisions[i] = Empty.INSTANCE;
				}
				else
				{
					subdivisionsRemaining = true;
				}
			}
			boolean allSubdivisionsSubtracted = !subdivisionsRemaining;
			if (allSubdivisionsSubtracted)
				this.empty = true;
			
			return this.empty;
		}
		
		/**
		 * {@return An array of 8 octrees, each of whom has a bounding box no larger than
		 * this octree's bounding box, + the dividing point at the minimal corner of the maximal subdivision}
		 * @param subtractionBounds Clamped subtraction bounds that consists of a strict
		 * subset of our octree's bounds (resides entirely within but does not entirely overlap it)
		 */
		private SubdivisionHolder makeSubdivisions(BoundingBox subtractionBounds)
		{
			// how do we decide where to make the cut
			// we only really need to subdivide once here, the subtracter will subdivide further if necessary
			// 1) naive implementation: just divide at the center point (not necessarily the most efficient)
			// 2) heuristic implementation: divide at whichever corner of the subtraction bounds is closest to the center
			// using a corner of the subtraction bounds for the subdivision point reduces the number of octrees
			// that need to be further subdivided
			// using the closest corner to the center means we avoid corners that are already on the edge of the outer bounds
			// (there will be at least one corner that's not on the outer bounds)
			SubtractiveOctree[] subdivisions = new SubtractiveOctree[8];
			BlockPos center = this.bounds.getCenter(); // the minimal corner of the maximal subdivision
			Corner[] corners = Corner.values();
			// find best corner to divide at
			// okay the heuristic subdivision has a flaw where it can choose to subdivide at the minimal corner
			// of the octree bounds, which is illegal
			// just subdividing at the center will always be legal for bounds.volume > 1
//			BlockPos center = null;
//			int bestDistance = Integer.MAX_VALUE;
//			for (int i=0; i<8; i++)
//			{
//				// the minimal point of the maximal subdivision 
//				BlockPos minMax = corners[i].getMinMax(subtractionBounds);
//				int dist = minMax.distManhattan(center);
//				if (dist < bestDistance)
//				{
//					bestDistance = dist;
//					bestCorner = minMax;
//				}
//			}
//			if (bestCorner == null)
//				bestCorner = center;
			
			for (int i=0; i<8; i++)
			{
				Corner corner = corners[i];
				BoundingBox subdivisionBox = corner.getSubdivision(this.bounds, center);
				
				subdivisions[i] = subdivisionBox == null
					? Empty.INSTANCE
					: new NonEmpty(subdivisionBox, this.depth+1);
			}
			return new SubdivisionHolder(center, subdivisions);
		}

		@Override
		public boolean contains(BoundingBox newBox)
		{
			if (this.empty)
				return false;
			
			// first, check if the outer box contains the new box
			if (!doesBoxEncapsulate(this.bounds, newBox))
				return false;
			// then, check whether any space has been subtracted from this box
			if (this.subdivisionHolder == null)
				return true; // no subdivisions = no subtraction done
			
			// otherwise, check if each subdivision that intersects with newBox contains that intersection
			BlockPos divider = this.subdivisionHolder.divider();
			SubtractiveOctree[] subdivisions = this.subdivisionHolder.subdivisions();
			Corner[] corners = Corner.values();
			for (int i=0; i<8; i++)
			{
				BoundingBox querySubdivision = corners[i].getSubdivision(newBox, divider);
				if (querySubdivision != null && !subdivisions[i].contains(querySubdivision))
				{
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean containsPos(Vec3i pos)
		{
			if (this.empty)
				return false;
			
			// first, check if the outer box contains the pos
			if (!this.bounds.isInside(pos))
				return false;
			// then, check whether any space has been subtracted from this box
			if (this.subdivisionHolder == null)
				return true; // no subdivisions = no subtraction done
			
			// otherwise, get the octant which would contain the position and check that
			int octantIndex = Corner.getOctantIndex(this.subdivisionHolder.divider(), pos);
			return this.subdivisionHolder.subdivisions[octantIndex].containsPos(pos);
		}
		
		/**
		 * Holds the sub-trees of some octree and the point they are divided at
		 * @param divider The minimal corner of the maximal subdivision,
		 * retained for contains checking
		 * @param subdivisions Array of eight sub-octrees
		 */
		private static record SubdivisionHolder(BlockPos divider, SubtractiveOctree[] subdivisions)
		{
		}
		
		private static enum Corner
		{
			DNW(false, false, false),
			DNE(true, false, false),
			DSW(false, false, true),
			DSE(true, false, true),
			UNW(false, true, false),
			UNE(true, true, false),
			USW(false, true, true),
			USE(true, true, true);
			
			private final boolean x;
			private final boolean y;
			private final boolean z;
			
			Corner(boolean x, boolean y, boolean z)
			{
				this.x = x;
				this.y = y;
				this.z = z;
			}
			
			/**
			 * Returns the corner where the given octantPosition is relative to a divider position
			 * @param divider Minimal position of the maximal corner
			 * @param octantPosition Position of the corner octant we want to get relative to the divider
			 * @return index of the Corner which contains the octantPosition
			 */
			public static int getOctantIndex(Vec3i divider, Vec3i octantPosition)
			{
				boolean x = octantPosition.getX() >= divider.getX();
				boolean y = octantPosition.getY() >= divider.getY();
				boolean z = octantPosition.getZ() >= divider.getZ();
				return (x ? 1 : 0) + (y ? 4 : 0) + (z ? 2 : 0);
			}
			
			/**
			 * Gets the minimal corner of the maximal subdivision of the outer box,
			 * if this corner of the inner box were to be used to subdivide the outer box
			 * @param box
			 * @return BlockPos at the minimal corner of the maximal subdivision of the outer box
			 */
			@SuppressWarnings("unused")
			public BlockPos getMinMax(BoundingBox box)
			{
				// if this is the maximal corner (true,true,true), return cornerPos + (1,1,1)
				// if this is the minimal corner (false,false,false), return cornerPos
				return new BlockPos(
					this.x ? box.maxX() + 1 : box.minX(),
					this.y ? box.maxY() + 1 : box.minY(),
					this.z ? box.maxZ() + 1 : box.minZ());
			}
			
			/**
			 * {@return This corner's subdivision of the bounding box, or null if the box would be invalid/interted
			 * or have volume < 1}
			 * @param box BoundingBox to divide
			 * @param divider The blockpos at the minimal corner of the maximal subdivision
			 */
			public @Nullable BoundingBox getSubdivision(BoundingBox box, BlockPos divider)
			{
				/*
					let's say we have
						O xxx
						xxxOxxx
						 xxx O
					where O is divider, x is box, on the x-axis
					minimal axis is box.minX to min(box.maxX or divider.x - 1)
					maximal axis is max(divider.x or box.maxX) to box.maxX
					
					so we only Math.min maxAxis for minimal corners
					and we only Math.max minAxis for maximal corners
				 */
				int dividerX = divider.getX();
				int dividerY = divider.getY();
				int dividerZ = divider.getZ();
				int minX = this.x ? Math.max(box.minX(), dividerX) : box.minX();
				int maxX = this.x ? box.maxX() : Math.min(box.maxX(), dividerX - 1);
				if (maxX < minX)
					return null;
				
				int minY = this.y ? Math.max(box.minY(), dividerY) : box.minY();
				int maxY = this.y ? box.maxY() : Math.min(box.maxY(), dividerY - 1);
				if (maxY < minY)
					return null;
				
				int minZ = this.z ? Math.max(box.minZ(), dividerZ) : box.minZ();
				int maxZ = this.z ? box.maxZ() : Math.min(box.maxZ(), dividerZ - 1);
				if (maxZ < minZ)
					return null;
				
				return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
			}
		}
	}
	
	/**
	 * SubtractiveOctree which started empty
	 */
	public static final class Empty implements SubtractiveOctree
	{
		/** singleton Empty instance */
		public static final Empty INSTANCE = new Empty();
		
		private Empty() {}

		@Override
		public boolean subtract(BoundingBox subtractionBounds)
		{
			return true;
		}

		@Override
		public boolean contains(BoundingBox newBox)
		{
			return false;
		}

		@Override
		public boolean containsPos(Vec3i pos)
		{
			return false;
		}
	}
	
	/**
	 * Returns the intersection of two bounding boxes
	 * @param a A bounding box
	 * @param b Another bounding box
	 * @return The intersection of the boxes, or null if the resulting box would be inverted or have a volume of less than 1
	 */
	public static @Nullable BoundingBox intersection(BoundingBox a, BoundingBox b)
	{
		int minX = Math.max(a.minX(), b.minX());
		int maxX = Math.min(a.maxX(), b.maxX());
		if (maxX < minX)
			return null;
		int minY = Math.max(a.minY(), b.minY());
		int maxY = Math.min(a.maxY(), b.maxY());
		if (maxY < minY)
			return null;
		int minZ = Math.max(a.minZ(), b.minZ());
		int maxZ = Math.min(a.maxZ(), b.maxZ());
		if (maxZ < minZ)
			return null;
		
		return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	/**
	 * {@return true if the inner BoundingBox is entirely contained within the outer bounding box, false otherwise}
	 * @param outer A bounding box in normal form {@code (min dimensions <= max dimensions)}
	 * @param inner Another bounding box in normal form
	 */
	public static boolean doesBoxEncapsulate(BoundingBox outer, BoundingBox inner)
	{
		return outer.minX() <= inner.minX()
			&& outer.minY() <= inner.minY()
			&& outer.minZ() <= inner.minZ()
			&& outer.maxX() >= inner.maxX()
			&& outer.maxY() >= inner.maxY()
			&& outer.maxZ() >= inner.maxZ();
	}
}
