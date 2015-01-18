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
import java.util.List;

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

		File imageCache = new File(directory + "/" + IMAGE_CACHE_CSV);
		if (!imageCache.exists() || clean) {
			buildImageCache();
		}

		TreeBuilder tb = new TreeBuilder(directory + "/" + IMAGE_CACHE_CSV, noise);
		try {
			MosaicBinaryTree tree = tb.build();

			File f = new File(in);
			BufferedImage img = ImageIO.read(f);

			int tileWidth = img.getWidth() / blocks;
			int tileHeight = img.getHeight() / blocks;
			MosaicTile[][] newImg = new MosaicTile[blocks][blocks];

			if (verbose) {
				System.out.println("\nFinding best matches..");
			}
			
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
					if (verbose) {
						printProgBar((progressBarCounter * 100) / (blocks * blocks));	
					}
				}
			}
			
			if (verbose) {
				System.out.println("\nBuilding new image..");
			}
			
			progressBarCounter = 0;
			BufferedImage toSave = new BufferedImage(tileWidth * blocks, tileHeight * blocks, BufferedImage.TYPE_INT_ARGB);

			Graphics2D g = img.createGraphics();
			g.setBackground(Color.white);
			g.clearRect(0, 0, toSave.getWidth(), toSave.getHeight());
			RenderingHints rh = g.getRenderingHints();	 
			rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHints (rh);
			Stroke s = new BasicStroke(border);
			g.setStroke(s);
			
			for (int i = 0; i < blocks; i++) {
				for (int j = 0; j < blocks; j++) {
					
					Rectangle rectangleWithoutPadding = new Rectangle((i * tileWidth) + padding, (j * tileHeight) + padding, tileWidth - padding * 2, tileHeight - padding * 2);
					Rectangle fullRectangle = new Rectangle(i * tileWidth, j * tileHeight, tileWidth, tileHeight);
					
					MosaicTile currentTile = newImg[i][j];
					
					if (tint_amount < 255) {
						
						// If the tint amount is not 255, then we should still draw
						// the image.
						
						File file = new File(currentTile.getPath());
						BufferedImage currentMosaicTileImage = ImageIO.read(file);
						
						if (circle) {
							Ellipse2D ellipse = new Ellipse2D.Float();
							ellipse.setFrame(rectangleWithoutPadding);
							g.setClip(ellipse);
						}
						
						g.drawImage(currentMosaicTileImage, rectangleWithoutPadding.x, rectangleWithoutPadding.y, rectangleWithoutPadding.width, rectangleWithoutPadding.height, null);
						
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
						g.drawOval(rectangleWithoutPadding.x, rectangleWithoutPadding.y, rectangleWithoutPadding.width, rectangleWithoutPadding.height);
					}
					else {
						g.drawRect(rectangleWithoutPadding.x, rectangleWithoutPadding.y, rectangleWithoutPadding.width, rectangleWithoutPadding.height);	
					}
					
					progressBarCounter++;
					
					if (verbose) {
						printProgBar((progressBarCounter * 100) / (blocks * blocks));	
					}
				}
			}

			if (verbose) {
				System.out.println("\nSaving new image to disk: " + out);
			}
			
			g.dispose();
			ImageIO.write(img, "JPEG", new File(out));

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private List<String> getHitList(MosaicTile[][] imgGrid, int row, int col) {
		List<String> hitList = new ArrayList<String>();
		
		int len = imgGrid.length-1;
		// a b c
		// d   f
		// g h i
		
		if (row != 0 && col != 0) {
			hitList.add(imgGrid[row-1][col-1].getId()); // a
		}
		
		if (row != 0) {
			hitList.add(imgGrid[row-1][col].getId()); // b
		}
		
		if (row != 0 && col != len) {
			hitList.add(imgGrid[row-1][col+1].getId()); // c
		}
		
		if (col != 0) {
			hitList.add(imgGrid[row][col-1].getId()); // d
		}
		
		return hitList;
	}

	static final String[] EXTENSIONS = new String[]{
        "jpg", "png", "bmp"
    };
	
    static final FilenameFilter IMAGE_FILTER = new FilenameFilter() {

        public boolean accept(final File dir, final String name) {
            for (final String ext : EXTENSIONS) {
                if (name.endsWith("." + ext)) {
                    return (true);
                }
            }
            return (false);
        }
    };
	
	private void buildImageCache() {
		if (verbose) {
			System.out.println("Analysing images in directory..");
		}
		
		List<MosaicTile> tiles = new ArrayList<MosaicTile>();
		StringBuffer sb = new StringBuffer();
		File[] listFiles = new File(directory).listFiles(IMAGE_FILTER);
		for (int i=0; i<listFiles.length; i++){
			File f = listFiles[i];
			if (f.isFile()) {
				try {
					MosaicTile m = new MosaicTile(Integer.toString(i), f);
					tiles.add(m);
					sb.append(m.toCSV());
					sb.append('\n');
				} catch (Exception e) {
					if (verbose) {
						System.out.println(e);
					}
				}
			}
			if (verbose) {
				printProgBar((i * 100) / (listFiles.length-1));
			}
		}
		try {
			String imageCacheFile = directory + "/" + IMAGE_CACHE_CSV;
			FileUtils.saveFile(imageCacheFile, sb.toString());
		} catch (IOException e) {
			if (verbose) {
				System.out.println("Error saving image cache file:");
				System.out.println(e);
			}
			System.exit(-1);
		}
	}
	
	public static void printProgBar(int percent){
	    StringBuilder bar = new StringBuilder("[");

	    for(int i = 0; i < 50; i++){
	        if (i <= (percent/2)){
	            bar.append("=");
	        }
	        else {
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
