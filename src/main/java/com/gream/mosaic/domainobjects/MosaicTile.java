package com.gream.mosaic.domainobjects;

import java.awt.Color;
import java.io.File;

import com.gream.mosaic.utils.ImageUtils;

public class MosaicTile {

  private Color averageColors;
  private String id;
  private String path;

  public MosaicTile(String id, File f) throws Exception {
    this.id = id;
    this.path = f.getPath();
    this.averageColors = ImageUtils.getAverageRGB(f);
  }

  public MosaicTile(String id, String path, Color averageColors) throws Exception {
    this.id = id;
    this.path = path;
    this.averageColors = averageColors;
  }

  public MosaicTile(int r, int g, int b) {
    this.averageColors = new Color(r, g, b);
  }

  public Color getAverageColors() {
    return averageColors;
  }

  public void setAverageColors(Color averageColors) {
    this.averageColors = averageColors;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String toCSV() {
    return this.id + ","
      + this.averageColors.getRed() + ","
      + this.averageColors.getGreen() + ","
      + this.averageColors.getBlue() + ","
      + this.path;
  }

  public int getR() {
    return this.averageColors.getRed();
  }

  public int getG() {
    return this.averageColors.getGreen();
  }

  public int getB() {
    return this.averageColors.getBlue();
  }

  public static int getDistance(MosaicTile obj1, MosaicTile obj2) {
    int r = (obj1.getR() - obj2.getR()) * (obj1.getR() - obj2.getR());
    int g = (obj1.getG() - obj2.getG()) * (obj1.getG() - obj2.getG());
    int b = (obj1.getB() - obj2.getB()) * (obj1.getB() - obj2.getB());
    return (int) Math.sqrt(r + g + b);
  }

  @Override
  public String toString() {
    return "{ id: " + this.id + ", color: { r: "
      + this.averageColors.getRed() + ", g: "
      + this.averageColors.getGreen() + ", b: "
      + this.averageColors.getBlue() + " } }";
  }

  public String getPath() {
    return path;
  }

}
