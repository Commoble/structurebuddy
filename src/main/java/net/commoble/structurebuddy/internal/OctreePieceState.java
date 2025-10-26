package net.commoble.structurebuddy.internal;

import java.util.List;

import org.jetbrains.annotations.ApiStatus;

import net.commoble.structurebuddy.api.JigsawConnectionToChild;
import net.commoble.structurebuddy.api.content.DynamicJigsawStructurePiece;

/**
 * Holds a jisgaw piece and its possible connections to potential child pieces
 * @param piece DynamicJigsawStructurePiece
 * @param shuffledConnectionsToChildren List of jigsaw connections in this parent piece pointing to potential child pieces
 * @param octree SubtractiveOctree representing the remaining space that child pieces can generate in
 * @param depth iteration depth of this structure piece
 * @param placementPriority int indicating processing priority for this piece state relative to other unprocessed piece states (higher = sooner)
 * @param placementCounter int indicating when this piece was added to the queue (0, 1, 2, 3, etc). Used to break ties in the priority queue correctly for pieces with the same placement priority.
 */
@ApiStatus.Internal
public record OctreePieceState(DynamicJigsawStructurePiece piece, List<JigsawConnectionToChild> shuffledConnectionsToChildren, SubtractiveOctree octree, int depth, int placementPriority, int placementCounter) implements Comparable<OctreePieceState>
{
	@Override
	public int compareTo(OctreePieceState that)
	{
		// placement priority : higher => earlier in list
		// if same, check placement counter: lower -> earlier in list
		// not using SequencedPriorityIterator because we need to pass the list of jigsaws from the queue to apis
		return this.placementPriority < that.placementPriority ? 1
			: this.placementPriority > that.placementPriority ? -1
			: this.placementCounter < that.placementCounter ? -1
			: this.placementCounter > that.placementCounter ? 1
			: 0;
	}
}
