package com.gream.mosaic.domainobjects;

import java.awt.Color;
import java.io.File;

import com.gream.mosaic.utils.ColorSpaceUtils;
import com.gream.mosaic.utils.ImageUtils;

public class MosaicTile {

  private Color averageColors;
  private double[] labColor; // LAB color space values for perceptual matching
  private String id;
  private String path;

  public MosaicTile(String id, File f) throws Exception {
    this.id = id;
    this.path = f.getPath();
    this.averageColors = ImageUtils.getAverageRGB(f);
    this.labColor = ColorSpaceUtils.rgbToLab(this.averageColors);
  }

  public MosaicTile(String id, String path, Color averageColors) throws Exception {
    this.id = id;
    this.path = path;
    this.averageColors = averageColors;
    this.labColor = ColorSpaceUtils.rgbToLab(averageColors);
  }

  public MosaicTile(int r, int g, int b) {
    this.averageColors = new Color(r, g, b);
    this.labColor = ColorSpaceUtils.rgbToLab(r, g, b);
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

  /**
   * Calculates the squared Euclidean distance between two tiles in LAB color
   * space.
   * LAB color space is perceptually uniform, providing better color matching that
   * aligns with human vision compared to RGB.
   * Using squared distance avoids the expensive sqrt operation while maintaining
   * the same relative ordering for comparison purposes.
   */
  public static double getDistance(MosaicTile obj1, MosaicTile obj2) {
    return ColorSpaceUtils.getLabDistanceSquared(obj1.labColor, obj2.labColor);
  }

  /**
   * Legacy RGB distance method (kept for backward compatibility if needed).
   * Use getDistance() for better perceptual matching.
   */
  @Deprecated
  public static int getRgbDistance(MosaicTile obj1, MosaicTile obj2) {
    int r = (obj1.getR() - obj2.getR()) * (obj1.getR() - obj2.getR());
    int g = (obj1.getG() - obj2.getG()) * (obj1.getG() - obj2.getG());
    int b = (obj1.getB() - obj2.getB()) * (obj1.getB() - obj2.getB());
    return r + g + b; // Return squared distance (no sqrt needed for comparison)
  }

  /**
   * Gets the LAB color values for this tile.
   * 
   * @return LAB color as [L, a, b] array
   */
  public double[] getLabColor() {
    return labColor;
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
