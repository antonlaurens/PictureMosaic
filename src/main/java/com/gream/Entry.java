package com.gream;

import static org.kohsuke.args4j.ExampleMode.REQUIRED;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.gream.mosaic.TreeBuilder;
import com.gream.mosaic.datastructures.MosaicBinaryTree;
import com.gream.mosaic.datastructures.MosaicNode;
import com.gream.mosaic.domainobjects.MosaicTile;
import com.gream.mosaic.utils.FileUtils;
import com.gream.mosaic.utils.ImageUtils;

public class Entry {

  private static final String IMAGE_CACHE_CSV = "imageCache.csv";

  @Option(name = "-dir", aliases = "-d", required = true, usage = "The directory in which the source images are located. These images will be used to build the PictureMosaic from, the smaller these images are the better.")
  private String directory;

  @Option(name = "-input", aliases = "-i", required = true, usage = "Input filename.")
  private String in;

  @Option(name = "-output", aliases = "-o", required = true, usage = "Output filename.")
  private String out;

  @Option(name = "-noise", aliases = "-n", usage = "Adds a chance of noise to the mosaic: [0, 1.0]. Defaults to zero.")
  private double noise = 0;

  @Option(name = "-blocks", aliases = "-b", required = true, usage = "The number of tiles per row/ column of the PictureMosaic. Defaults to 50.")
  private int blocks = 50;

  @Option(name = "-tint", aliases = "-t", usage = "Indicates the alpha of the color to tint the blocks with: [0, 255]. Defaults to 0.")
  private int tint_amount = 0;

  @Option(name = "-cache_rebuild", aliases = "-cr", usage = "When the source images directory is read for the first time, a cache file is created to speed up consequent read. To force a rebuild of the image cache, specify this argument.")
  private boolean clean;

  @Option(name = "-padding", aliases = "-p", usage = "The amount of padding in pixels between tiles.")
  private int padding = 0;

  @Option(name = "-stroke", aliases = "-s", usage = "The stroke width on a tile. The colour of the stroke is the average RGB values inside the image.")
  private int border = 0;

  @Option(name = "-circle", aliases = "-cir", usage = "If this is set, then tiles will be drawn as circles, not rectangles.")
  private boolean circle = false;

  @Option(name = "-consume", aliases = "-c", usage = "If set, then a source image can only be used once as a tile in the PictureMosaic. Please note that you run the risk of running out of photos.")
  private boolean consume;

  @Option(name = "-adjacency_ban", aliases = "-ab", usage = "If set, no two adjacent images can be the same.")
  private boolean adjacencyBan;

  @Option(name = "-verbose", aliases = "-v", usage = "Enables verbose output.")
  private boolean verbose;

  @SuppressWarnings("deprecation")
  public void doMain(String[] args) {
    CmdLineParser parser = new CmdLineParser(this);

    try {
      parser.parseArgument(args);
      noise = Math.min(1, noise);
      tint_amount = Math.min(255, tint_amount);

      if (args.length == 0) {
        throw new CmdLineException(parser, "No arguments found.");
      }

    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      System.err.println("java -jar PictureMosaic.jar [options...] arguments...");
      parser.printUsage(System.err);
      System.err.println();
      System.err.println("  Example: java -jar PictureMosaic.jar" + parser.printExample(REQUIRED));
      return;
    }

    System.out.println("[DEBUG] Command line arguments parsed successfully");
    System.out.println("[DEBUG] Directory: " + directory);
    System.out.println("[DEBUG] Input file: " + in);
    System.out.println("[DEBUG] Output file: " + out);
    System.out.println("[DEBUG] Blocks: " + blocks);
    System.out.println("[DEBUG] Clean cache: " + clean);

    File imageCacheFile = new File(directory + "/" + IMAGE_CACHE_CSV);
    if (!imageCacheFile.exists() || clean) {
      buildImageCache();
    }

    TreeBuilder tb = new TreeBuilder(directory + "/" + IMAGE_CACHE_CSV, noise);
    try {
      MosaicBinaryTree tree = tb.build();

      File f = new File(in);
      System.out.println("[DEBUG] Reading input image file: " + f.getAbsolutePath());
      System.out.println("[DEBUG] Input file exists: " + f.exists());
      System.out.println("[DEBUG] Input file is file: " + f.isFile());
      System.out.println("[DEBUG] Input file can read: " + f.canRead());

      if (!f.exists()) {
        System.err.println("[ERROR] Input file does not exist: " + f.getAbsolutePath());
        System.err.println(
            "[ERROR] Please check that the file path is correct and includes the file extension (e.g., .png, .jpg)");
        System.exit(-1);
      }

      if (!f.isFile()) {
        System.err.println("[ERROR] Input path is not a file: " + f.getAbsolutePath());
        System.exit(-1);
      }

      if (!f.canRead()) {
        System.err.println("[ERROR] Cannot read input file (permission denied): " + f.getAbsolutePath());
        System.exit(-1);
      }

      BufferedImage img = ImageIO.read(f);

      if (img == null) {
        System.err.println("[ERROR] Failed to read image file. The file may not be a valid image format.");
        System.err.println("[ERROR] Supported formats: JPG, PNG, BMP");
        System.err.println("[ERROR] File: " + f.getAbsolutePath());
        System.exit(-1);
      }

      System.out
          .println("[DEBUG] Successfully loaded input image: " + img.getWidth() + "x" + img.getHeight() + " pixels");

      int tileWidth = img.getWidth() / blocks;
      int tileHeight = img.getHeight() / blocks;
      System.out.println("[DEBUG] Tile dimensions: " + tileWidth + "x" + tileHeight + " pixels per tile");
      System.out.println("[DEBUG] Total tiles: " + blocks + "x" + blocks + " = " + (blocks * blocks) + " tiles");

      // Pre-load and pre-scale all source images to tile size for performance
      System.out.println("\n[INFO] Pre-loading source images into memory cache...");
      Map<String, BufferedImage> imageCache = loadImagesIntoMemoryCache(directory + "/" + IMAGE_CACHE_CSV, tileWidth,
          tileHeight);
      System.out.println("[INFO] Loaded " + imageCache.size() + " images into memory cache");

      MosaicTile[][] newImg = new MosaicTile[blocks][blocks];

      System.out.println("\n[INFO] Finding best matches for " + (blocks * blocks) + " tiles...");

      int progressBarCounter = 0;
      for (int i = 0; i < tileWidth * blocks; i += tileWidth) {

        for (int j = 0; j < tileHeight * blocks; j += tileHeight) {
          if (adjacencyBan) {
            tree.unstale();
          }
          int[] rgbs = new int[tileWidth * tileHeight];
          img.getRGB(i, j, tileWidth, tileHeight, rgbs, 0, tileWidth);

          MosaicNode consumeClosest = tree.findClosest(new MosaicTile("", "", ImageUtils.getAverageRGB(rgbs)));

          if (adjacencyBan) {
            List<String> hitlist = getHitList(newImg, i / tileWidth, j / tileHeight);
            while (hitlist.contains(consumeClosest.getContents().getId())) {
              consumeClosest.setStale(true);
              consumeClosest = tree.findClosest(new MosaicTile("", "", ImageUtils.getAverageRGB(rgbs)));
            }
          }

          consumeClosest.setStale(consume);
          newImg[i / tileWidth][j / tileHeight] = consumeClosest.getContents();

          progressBarCounter++;
          int percent = (progressBarCounter * 100) / (blocks * blocks);
          if (verbose) {
            printProgBar(percent);
          } else if (progressBarCounter % 10 == 0 || progressBarCounter == blocks * blocks) {
            // Show progress every 10 tiles or at completion
            System.out.print("\r[INFO] Finding matches: " + progressBarCounter + "/" + (blocks * blocks) + " tiles ("
                + percent + "%)");
          }
        }
      }
      System.out.println(); // New line after progress

      System.out.println("\n[INFO] Building mosaic image...");

      progressBarCounter = 0;
      BufferedImage toSave = new BufferedImage(tileWidth * blocks, tileHeight * blocks, BufferedImage.TYPE_INT_ARGB);

      Graphics2D g = toSave.createGraphics();
      g.setBackground(Color.white);
      g.clearRect(0, 0, toSave.getWidth(), toSave.getHeight());
      RenderingHints rh = g.getRenderingHints();
      rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHints(rh);
      Stroke s = new BasicStroke(border);
      g.setStroke(s);

      for (int i = 0; i < blocks; i++) {
        for (int j = 0; j < blocks; j++) {

          Rectangle rectangleWithoutPadding = new Rectangle((i * tileWidth) + padding, (j * tileHeight) + padding,
              tileWidth - padding * 2, tileHeight - padding * 2);
          Rectangle fullRectangle = new Rectangle(i * tileWidth, j * tileHeight, tileWidth, tileHeight);

          MosaicTile currentTile = newImg[i][j];

          if (tint_amount < 255) {

            // If the tint amount is not 255, then we should still draw
            // the image.

            // Use cached image instead of reading from disk
            BufferedImage currentMosaicTileImage = imageCache.get(currentTile.getPath());
            if (currentMosaicTileImage == null) {
              // Fallback to disk read if not in cache (shouldn't happen)
              System.err.println("[WARNING] Image not found in cache: " + currentTile.getPath());
              File file = new File(currentTile.getPath());
              currentMosaicTileImage = ImageIO.read(file);
              // Scale to tile size if needed
              if (currentMosaicTileImage.getWidth() != tileWidth - padding * 2 ||
                  currentMosaicTileImage.getHeight() != tileHeight - padding * 2) {
                BufferedImage scaled = new BufferedImage(tileWidth - padding * 2, tileHeight - padding * 2,
                    BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = scaled.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(currentMosaicTileImage, 0, 0, tileWidth - padding * 2, tileHeight - padding * 2, null);
                g2.dispose();
                currentMosaicTileImage = scaled;
              }
            }

            if (circle) {
              Ellipse2D ellipse = new Ellipse2D.Float();
              ellipse.setFrame(rectangleWithoutPadding);
              g.setClip(ellipse);
            }

            g.drawImage(currentMosaicTileImage, rectangleWithoutPadding.x, rectangleWithoutPadding.y,
                rectangleWithoutPadding.width, rectangleWithoutPadding.height, null);

          }

          if (tint_amount > 0) {

            // Render the tint

            Color c = currentTile.getAverageColors();
            Color newColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), tint_amount);
            g.setColor(newColor);
            g.fillRect(fullRectangle.x, fullRectangle.y, fullRectangle.width, fullRectangle.height);

          }

          g.setColor(currentTile.getAverageColors());

          if (circle) {
            Rectangle2D rect = new Rectangle2D.Float();
            rect.setRect(fullRectangle.x, fullRectangle.y, fullRectangle.width, fullRectangle.height);
            g.setClip(rect);
            g.drawOval(rectangleWithoutPadding.x, rectangleWithoutPadding.y, rectangleWithoutPadding.width,
                rectangleWithoutPadding.height);
          } else {
            g.drawRect(rectangleWithoutPadding.x, rectangleWithoutPadding.y, rectangleWithoutPadding.width,
                rectangleWithoutPadding.height);
          }

          progressBarCounter++;
          int percent = (progressBarCounter * 100) / (blocks * blocks);
          if (verbose) {
            printProgBar(percent);
          } else if (progressBarCounter % 10 == 0 || progressBarCounter == blocks * blocks) {
            // Show progress every 10 tiles or at completion
            System.out.print("\r[INFO] Building mosaic: " + progressBarCounter + "/" + (blocks * blocks) + " tiles ("
                + percent + "%)");
          }
        }
      }
      System.out.println(); // New line after progress

      File outputFile = new File(out);
      System.out.println("\n[INFO] Saving output image to: " + outputFile.getAbsolutePath());
      System.out.println(
          "[DEBUG] Output image dimensions: " + (tileWidth * blocks) + "x" + (tileHeight * blocks) + " pixels");

      g.dispose();

      // Determine format from file extension
      String fileName = outputFile.getName().toLowerCase();
      String format = "png"; // default
      if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
        format = "jpeg";
      } else if (fileName.endsWith(".png")) {
        format = "png";
      } else if (fileName.endsWith(".bmp")) {
        format = "bmp";
      }

      System.out.println("[DEBUG] Detected output format: " + format.toUpperCase());

      // Check if writer is available
      javax.imageio.ImageWriter writer = null;
      java.util.Iterator<javax.imageio.ImageWriter> writers = javax.imageio.ImageIO.getImageWritersByFormatName(format);
      if (writers.hasNext()) {
        writer = writers.next();
      }

      if (writer == null) {
        System.err.println("[ERROR] No " + format.toUpperCase() + " writer available.");
        System.err.println(
            "[ERROR] Available writers: " + java.util.Arrays.toString(javax.imageio.ImageIO.getWriterFormatNames()));
        System.exit(-1);
      }

      System.out.println("[DEBUG] Using ImageWriter: " + writer.getClass().getName());

      // Convert image if needed for format compatibility (JPEG doesn't support alpha)
      BufferedImage imageToSave = toSave;
      if (format.equals("jpeg") && toSave.getType() != BufferedImage.TYPE_INT_RGB) {
        System.out
            .println(
                "[DEBUG] Converting image from type " + toSave.getType() + " to TYPE_INT_RGB for JPEG compatibility");
        imageToSave = new BufferedImage(toSave.getWidth(), toSave.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = imageToSave.createGraphics();
        g2.drawImage(toSave, 0, 0, null);
        g2.dispose();
      }

      boolean saved = ImageIO.write(imageToSave, format, outputFile);
      if (!saved) {
        System.err.println("[ERROR] Failed to save image. ImageIO.write returned false.");
        System.err.println("[ERROR] Attempted format: " + format);
        System.err.println("[ERROR] Image type: " + imageToSave.getType());
        System.err.println("[ERROR] Available writers for " + format + ": " +
            java.util.Arrays.toString(javax.imageio.ImageIO.getWriterFormatNames()));
        System.exit(-1);
      }

      if (outputFile.exists()) {
        long fileSize = outputFile.length();
        System.out.println("[INFO] Successfully saved output image!");
        System.out.println("[INFO] File size: " + (fileSize / 1024) + " KB (" + fileSize + " bytes)");
        System.out.println("[INFO] Output file: " + outputFile.getAbsolutePath());
      } else {
        System.err.println("[ERROR] Output file was not created: " + outputFile.getAbsolutePath());
        System.exit(-1);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private List<String> getHitList(MosaicTile[][] imgGrid, int row, int col) {
    List<String> hitList = new ArrayList<String>();

    int len = imgGrid.length - 1;
    // a b c
    // d f
    // g h i

    if (row != 0 && col != 0) {
      hitList.add(imgGrid[row - 1][col - 1].getId()); // a
    }

    if (row != 0) {
      hitList.add(imgGrid[row - 1][col].getId()); // b
    }

    if (row != 0 && col != len) {
      hitList.add(imgGrid[row - 1][col + 1].getId()); // c
    }

    if (col != 0) {
      hitList.add(imgGrid[row][col - 1].getId()); // d
    }

    return hitList;
  }

  static final String[] EXTENSIONS = new String[] {
      "jpg", "png", "bmp"
  };

  static final FilenameFilter IMAGE_FILTER = new FilenameFilter() {

    public boolean accept(final File dir, final String name) {
      return (true);
    }
  };

  private void buildImageCache() {
    if (verbose) {
      System.out.println("Analysing images in directory..");
    }

    System.out.println("[DEBUG] Building image cache for directory: " + directory);

    File dirFile = new File(directory);
    System.out.println("[DEBUG] Directory path: " + dirFile.getAbsolutePath());
    System.out.println("[DEBUG] Directory exists: " + dirFile.exists());
    System.out.println("[DEBUG] Is directory: " + dirFile.isDirectory());
    System.out.println("[DEBUG] Can read: " + dirFile.canRead());

    List<MosaicTile> tiles = new ArrayList<MosaicTile>();
    StringBuffer sb = new StringBuffer();
    File[] listFiles = dirFile.listFiles(IMAGE_FILTER);

    if (listFiles == null) {
      System.err.println("[ERROR] Failed to list files in directory: " + directory);
      System.err.println("[ERROR] Directory exists: " + dirFile.exists() + ", Is directory: " + dirFile.isDirectory()
          + ", Can read: " + dirFile.canRead());
      if (dirFile.exists() && dirFile.isDirectory() && !dirFile.canRead()) {
        System.err.println("[ERROR] Permission denied: The directory exists but you don't have read permissions.");
        System.err.println("[ERROR] On macOS, this may be due to privacy/security restrictions.");
        System.err.println("[ERROR] Solutions:");
        System.err.println(
            "[ERROR]   1. Grant 'Full Disk Access' or 'Files and Folders' permission to Terminal/Java in System Preferences > Security & Privacy");
        System.err.println(
            "[ERROR]   2. Move the directory to a location you have full access to (e.g., ~/Desktop or ~/Documents)");
        System.err.println("[ERROR]   3. Check directory permissions: chmod +r \"" + directory + "\"");
      } else {
        System.err.println("[ERROR] Directory may not exist, may not be accessible, or may not be a directory.");
      }
      System.exit(-1);
    }

    System.out.println("[DEBUG] Found " + listFiles.length + " files in directory");

    for (int i = 0; i < listFiles.length; i++) {
      File f = listFiles[i];
      System.out.println("[DEBUG] Processing file " + (i + 1) + "/" + listFiles.length + ": " + f.getName()
          + " (isFile: " + f.isFile() + ")");
      if (f.isFile()) {
        try {
          MosaicTile m = new MosaicTile(Integer.toString(i), f);
          tiles.add(m);
          sb.append(m.toCSV());
          sb.append('\n');
          System.out.println("[DEBUG] Successfully processed image: " + f.getName());
        } catch (Exception e) {
          System.err.println("[WARNING] Failed to process file " + f.getName() + ": " + e.getMessage());
          if (verbose) {
            System.out.println(e);
          }
        }
      }
      if (verbose) {
        printProgBar((i * 100) / (listFiles.length - 1));
      }
    }
    try {
      String imageCacheFile = directory + "/" + IMAGE_CACHE_CSV;
      System.out.println("[DEBUG] Saving image cache to: " + imageCacheFile);
      FileUtils.saveFile(imageCacheFile, sb.toString());
      System.out.println("[DEBUG] Successfully saved image cache with " + tiles.size() + " tiles");
    } catch (IOException e) {
      System.err.println("[ERROR] Error saving image cache file: " + e.getMessage());
      if (verbose) {
        System.out.println("Error saving image cache file:");
        System.out.println(e);
      }
      System.exit(-1);
    }
  }

  /**
   * Loads source images into an in-memory cache, pre-scaled to tile size for
   * performance.
   * This avoids reading images from disk for each tile during rendering.
   */
  private Map<String, BufferedImage> loadImagesIntoMemoryCache(String imageCachePath, int tileWidth, int tileHeight) {
    Map<String, BufferedImage> cache = new HashMap<String, BufferedImage>();

    try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(imageCachePath))) {
      String line;
      int loaded = 0;
      int failed = 0;

      while ((line = br.readLine()) != null && !line.trim().isEmpty()) {
        String[] parts = line.split(",");
        if (parts.length >= 5) {
          String imagePath = parts[4]; // Path is the 5th column (index 4)

          try {
            File imageFile = new File(imagePath);
            if (imageFile.exists() && imageFile.isFile()) {
              BufferedImage original = ImageIO.read(imageFile);
              if (original != null) {
                // Pre-scale image to tile size (accounting for padding)
                int targetWidth = Math.max(1, tileWidth - padding * 2);
                int targetHeight = Math.max(1, tileHeight - padding * 2);

                BufferedImage scaled;
                if (original.getWidth() == targetWidth && original.getHeight() == targetHeight) {
                  scaled = original;
                } else {
                  scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
                  Graphics2D g2 = scaled.createGraphics();
                  g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                  g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                  g2.drawImage(original, 0, 0, targetWidth, targetHeight, null);
                  g2.dispose();
                }

                cache.put(imagePath, scaled);
                loaded++;

                if (verbose && loaded % 100 == 0) {
                  System.out.print("\r[INFO] Loaded " + loaded + " images...");
                }
              } else {
                failed++;
                if (verbose) {
                  System.err.println("[WARNING] Failed to read image: " + imagePath);
                }
              }
            } else {
              failed++;
              if (verbose) {
                System.err.println("[WARNING] Image file not found: " + imagePath);
              }
            }
          } catch (Exception e) {
            failed++;
            if (verbose) {
              System.err.println("[WARNING] Error loading image " + imagePath + ": " + e.getMessage());
            }
          }
        }
      }

      if (verbose) {
        System.out.println(); // New line after progress
      }

      if (failed > 0) {
        System.out.println("[WARNING] Failed to load " + failed + " images (they will be skipped)");
      }

    } catch (IOException e) {
      System.err.println("[ERROR] Failed to read image cache file: " + e.getMessage());
      if (verbose) {
        e.printStackTrace();
      }
    }

    return cache;
  }

  public static void printProgBar(int percent) {
    StringBuilder bar = new StringBuilder("[");

    for (int i = 0; i < 50; i++) {
      if (i <= (percent / 2)) {
        bar.append("=");
      } else {
        bar.append(" ");
      }
    }

    bar.append("]   " + percent + "%     ");
    System.out.print("\r" + bar.toString());
  }

  public static void main(String[] args) {
    new Entry().doMain(args);
  }

}
