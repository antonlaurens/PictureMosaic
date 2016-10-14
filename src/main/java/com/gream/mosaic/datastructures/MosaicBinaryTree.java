package com.gream.mosaic.datastructures;

import java.util.Random;

import com.gream.mosaic.domainobjects.MosaicTile;

public class MosaicBinaryTree extends BinaryTree<MosaicNode> {

  private Random rnd = new Random();

  private double noiseFactor;

  public MosaicBinaryTree(MosaicNode root, double noiseFactor) {
    super(root);
    this.noiseFactor = noiseFactor;
  }

  public MosaicNode findClosest(MosaicTile toFind) {
    MosaicNode closer = this.root;
    while (!closer.isLeaf()) {

      boolean isLeftStale = ((MosaicNode) closer.getLeft()).isStale();
      boolean isRightStale = ((MosaicNode) closer.getRight()).isStale();

      if ((isLeftStale && isRightStale)) {
        if (closer == this.root) {
          System.err.println("We ran out of images! Dang!");
          System.exit(-1);
        }
        closer.setStale(true);
        closer = (MosaicNode) closer.getParent();
      } else if (isRightStale) {
        closer = (MosaicNode) closer.getLeft();
      } else if (isLeftStale) {
        closer = (MosaicNode) closer.getRight();
      } else if (rnd.nextDouble() <= this.noiseFactor) {
        closer = (MosaicNode) closer.getLeft();
      } else {
        closer = (MosaicNode) MosaicNode.min(closer.getLeft(), closer.getRight(), toFind);
      }
    }
    return closer;
  }

  public void unstale() {
    unstaleNode(this.root);
  }

  private void unstaleNode(MosaicNode node) {
    if (node == null) return;
    node.setStale(false);
    unstaleNode((MosaicNode) node.getLeft());
    unstaleNode((MosaicNode) node.getRight());
  }

}
