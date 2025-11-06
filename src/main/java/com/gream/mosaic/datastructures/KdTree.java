package com.gream.mosaic.datastructures;

import java.util.ArrayList;
import java.util.List;

import com.gream.mosaic.domainobjects.MosaicTile;

/**
 * KD-tree implementation for efficient nearest neighbor search in 3D color
 * space (LAB).
 * A KD-tree partitions space along alternating dimensions, making it ideal for
 * nearest neighbor queries in multi-dimensional spaces like LAB color space.
 * 
 * This provides better performance than a simple binary tree for color
 * matching,
 * especially with large numbers of source images.
 */
public class KdTree {

    private KdNode root;
    private int dimension = 3; // LAB has 3 dimensions: L, a, b

    public KdTree(List<MosaicTile> tiles) {
        if (tiles == null || tiles.isEmpty()) {
            throw new IllegalArgumentException("Cannot build KD-tree from empty tile list");
        }
        root = buildTree(tiles, 0);
    }

    /**
     * Recursively builds the KD-tree by partitioning tiles along alternating
     * dimensions.
     */
    private KdNode buildTree(List<MosaicTile> tiles, int depth) {
        if (tiles.isEmpty()) {
            return null;
        }

        if (tiles.size() == 1) {
            return new KdNode(tiles.get(0));
        }

        // Select dimension to split on (alternate between L, a, b)
        int axis = depth % dimension;

        // Find median along the selected axis
        tiles.sort((t1, t2) -> {
            double[] lab1 = t1.getLabColor();
            double[] lab2 = t2.getLabColor();
            return Double.compare(lab1[axis], lab2[axis]);
        });

        int median = tiles.size() / 2;
        KdNode node = new KdNode(tiles.get(median));

        // Recursively build left and right subtrees
        List<MosaicTile> leftTiles = new ArrayList<>(tiles.subList(0, median));
        List<MosaicTile> rightTiles = new ArrayList<>(tiles.subList(median + 1, tiles.size()));

        node.left = buildTree(leftTiles, depth + 1);
        node.right = buildTree(rightTiles, depth + 1);

        return node;
    }

    /**
     * Finds the nearest neighbor to the target tile in LAB color space.
     * Uses a branch-and-bound algorithm to efficiently prune the search space.
     */
    public MosaicTile findNearest(MosaicTile target) {
        if (root == null) {
            return null;
        }

        NearestNeighborSearch search = new NearestNeighborSearch();
        search(target, root, 0, search);
        return search.nearest != null ? search.nearest.tile : null;
    }

    /**
     * Recursive nearest neighbor search with branch-and-bound pruning.
     */
    private void search(MosaicTile target, KdNode node, int depth, NearestNeighborSearch search) {
        if (node == null) {
            return;
        }

        // Calculate distance to current node
        double distance = MosaicTile.getDistance(target, node.tile);
        if (distance < search.bestDistance) {
            search.bestDistance = distance;
            search.nearest = node;
        }

        int axis = depth % dimension;
        double[] targetLab = target.getLabColor();
        double[] nodeLab = node.tile.getLabColor();
        double axisDistance = targetLab[axis] - nodeLab[axis];

        // Determine which side to search first (closer side)
        KdNode nearChild = axisDistance < 0 ? node.left : node.right;
        KdNode farChild = axisDistance < 0 ? node.right : node.left;

        // Recursively search the closer side
        if (nearChild != null) {
            search(target, nearChild, depth + 1, search);
        }

        // Check if we need to search the farther side
        // Only search if the hyperplane distance is less than current best distance
        if (farChild != null && axisDistance * axisDistance < search.bestDistance) {
            search(target, farChild, depth + 1, search);
        }
    }

    /**
     * Helper class to track the nearest neighbor during search.
     */
    private static class NearestNeighborSearch {
        KdNode nearest = null;
        double bestDistance = Double.MAX_VALUE;
    }

    /**
     * KD-tree node containing a tile and its children.
     */
    private static class KdNode {
        MosaicTile tile;
        KdNode left;
        KdNode right;

        KdNode(MosaicTile tile) {
            this.tile = tile;
        }
    }

    /**
     * Marks all nodes as not stale (resets the stale flag).
     * This is used when adjacency ban is enabled.
     */
    public void unstale() {
        unstaleNode(root);
    }

    private void unstaleNode(KdNode node) {
        if (node == null)
            return;
        // Note: KD-tree nodes don't have a stale flag, but we can add one if needed
        // For now, this is a placeholder for compatibility with the existing interface
        unstaleNode(node.left);
        unstaleNode(node.right);
    }
}
