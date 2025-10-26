package net.commoble.structurebuddy.api;

/**
 * JigsawConnectionToParent with selection priority.
 * Mostly useful for converting vanilla JigsawBlockInfos (which have selection priority)
 * from structure templates into shuffled lists of dynamic jigsaw connections
 * @param connection JigsawConnectionToParent in this child jigsaw piece to a parent jigsaw piece
 * @param selectionPriority int indicating prioritization in shuffled lists of jigsaw connections (higher = earlier in list)
 */
public record SelectableJigsawConnectionToParent(JigsawConnectionToParent connection, int selectionPriority)
{
}