package com.gream.mosaic;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;

import com.gream.mosaic.datastructures.MosaicBinaryTree;
import com.gream.mosaic.datastructures.MosaicNode;
import com.gream.mosaic.datastructures.Queue;
import com.gream.mosaic.domainobjects.MosaicTile;

public class TreeBuilder {

  private Queue<MosaicTile> objs;
  private MosaicNode root;
  private String imageCachePath;
  private double noiseFactor;

  public TreeBuilder(String imageCachePath, double noiseFactor) {
    this.objs = new Queue<MosaicTile>();
    this.imageCachePath = imageCachePath;
    this.noiseFactor = noiseFactor;
  }

  public MosaicBinaryTree build() throws Exception {

    BufferedReader br = new BufferedReader(new FileReader(this.imageCachePath));
    String line;
    while ((line = br.readLine()) != null) {
      String[] l = line.split(",");
      int r = Integer.parseInt(l[1]);
      int g = Integer.parseInt(l[2]);
      int b = Integer.parseInt(l[3]);

      Color averageColors = new Color(r, g, b);
      objs.enqueue(new MosaicTile(l[0], l[4], averageColors));
    }
    br.close();

    root = new MosaicNode(objs.dequeue(), objs.dequeue());

    MosaicTile next = objs.dequeue();
    while (next != null) {
      MosaicNode closer = (MosaicNode) MosaicNode.min(root.getLeft(), root.getRight(), next);
      while (!closer.isLeaf()) {
        closer = (MosaicNode) MosaicNode.min(closer.getLeft(), closer.getRight(), next);
      }
      closer.setLeft(new MosaicNode(closer.getContents()));
      closer.setRight(new MosaicNode(next));
      closer.setContents(MosaicNode.merge(closer.getContents(), next));

      next = objs.dequeue();
    }
    return new MosaicBinaryTree(root, noiseFactor);
  }

}
