package com.gream.mosaic.datastructures;

import com.gream.mosaic.domainobjects.MosaicTile;

public class MosaicNode extends BinaryTreeNode<MosaicTile> {

  private boolean stale;

  public MosaicNode(MosaicTile contents) {
    super(contents);
  }

  public MosaicNode(MosaicTile left, MosaicTile right) {
    super(merge(left, right), new MosaicNode(left), new MosaicNode(right));
  }

  public static MosaicTile merge(MosaicTile left, MosaicTile right) {
    // Merge RGB colors (averaging in RGB space is sufficient for tree structure)
    // The LAB color space is used for distance calculations, which is where it matters most
    int r = (left.getR() + right.getR()) / 2;
    int g = (left.getG() + right.getG()) / 2;
    int b = (left.getB() + right.getB()) / 2;
    MosaicTile mergedTile = new MosaicTile(r, g, b);
    mergedTile.setId(left.getId() + "," + right.getId());
    return mergedTile;
  }

  public static BinaryTreeNode<MosaicTile> min(BinaryTreeNode<MosaicTile> n1, BinaryTreeNode<MosaicTile> n2, MosaicTile obj) {
    double distance = MosaicTile.getDistance(n1.getContents(), obj);
    double distance2 = MosaicTile.getDistance(n2.getContents(), obj);
    return distance < distance2 ? n1 : n2;
  }

  public boolean isStale() {
    return stale;
  }

  public void setStale(boolean stale) {
    this.stale = stale;
  }

}
