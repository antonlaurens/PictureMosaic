package com.gream;

import static org.kohsuke.args4j.ExampleMode.REQUIRED;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
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
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.spi.OptionHandler;

import com.gream.mosaic.TreeBuilder;
import com.gream.mosaic.datastructures.MosaicBinaryTree;
import com.gream.mosaic.datastructures.MosaicNode;
import com.gream.mosaic.domainobjects.MosaicTile;
import com.gream.mosaic.utils.FileUtils;
import com.gream.mosaic.utils.ImageUtils;

public class Entry {

	private static final String IMAGE_CACHE_CSV = "imageCache.csv";

	@Option(name = "-d", required = true, usage = "the directory in which the images are located")
	private String directory;

	@Option(name = "-i", required = true, usage = "Input filename")
	private String in;

	@Option(name = "-o", required = true, usage = "Output filename")
	private String out;
	
	@Option(name = "-n", usage = "Adds a chance of noise to the mosaic: [0, 1.0]")
	private double noise = 0;
	
	@Option(name = "-b", required = true, usage = "Amount of mosaic tiles per column and row.")
	private int blocks = 50;
	
	@Option(name = "-t", usage = "Indicated the alpha of the color to tint the blocks with: [0, 255]")
	private int tint = 0;
	
	@Option(name = "-c", usage = "Force a rebuild of the image cache")
	private boolean clean;
	
	@Option(name = "-s", usage = "If set, then an image can only be used once as a tile in the mosaic. Risk of running out of photos.")
	private boolean consume;
	
	@Option(name = "-a", usage = "If set, no two adjacent images can be the same")
	private boolean adjacenyBan;
	
	@Option(name = "-v", usage = "Enables verbose output")
	private boolean verbose;

	public void doMain(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);

		try {
			parser.parseArgument(args);
			noise = Math.min(1, noise);
			tint = Math.min(255, tint);
			
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

			int xChunk = img.getWidth() / blocks;
			int yChunk = img.getHeight() / blocks;
			MosaicTile[][] newImg = new MosaicTile[blocks][blocks];

			if (verbose) {
				System.out.println("\nFinding best matches..");
			}
			
			int counter = 0;
			for (int i = 0; i < xChunk * blocks; i += xChunk) {

				for (int j = 0; j < yChunk * blocks; j += yChunk) {
					if (adjacenyBan) {
						tree.unstale();
					}
					int[] rgbs = new int[xChunk * yChunk];
					img.getRGB(i, j, xChunk, yChunk, rgbs, 0, xChunk);
					
					MosaicNode consumeClosest = tree.findClosest(new MosaicTile("", "", ImageUtils.getAverageRGB(rgbs)));
					
					if (adjacenyBan) {
						List<String> hitlist = getHitList(newImg, i / xChunk, j / yChunk);
						while (hitlist.contains(consumeClosest.getContents().getId())) {
							consumeClosest.setStale(true);
							consumeClosest = tree.findClosest(new MosaicTile("", "", ImageUtils.getAverageRGB(rgbs)));
						}
					}
					
					consumeClosest.setStale(consume);
					newImg[i / xChunk][j / yChunk] = consumeClosest.getContents();
					
					counter++;
					if (verbose) {
						printProgBar((counter * 100) / (blocks * blocks));	
					}
				}
			}
			
			if (verbose) {
				System.out.println("\nBuilding new image..");
			}
			
			counter = 0;
			BufferedImage toSave = new BufferedImage(xChunk * blocks, yChunk * blocks, BufferedImage.TYPE_INT_ARGB);

			Graphics2D g = img.createGraphics();
			g.setBackground(Color.white);
			g.clearRect(0, 0, toSave.getWidth(), toSave.getHeight());
			RenderingHints rh = g.getRenderingHints();	 
			rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHints (rh);
			Stroke s = new BasicStroke(5);
			g.setStroke(s);
			for (int i = 0; i < blocks; i++) {
				for (int j = 0; j < blocks; j++) {
					int padding = 5;
					if (tint < 255) {
						File file = new File(newImg[i][j].getPath());
						BufferedImage currentMosaicTileImage = ImageIO.read(file);
						Ellipse2D ellipse = new Ellipse2D.Float();
						ellipse.setFrame((i * xChunk) + padding, (j * yChunk) + padding, xChunk - padding *2, yChunk - padding *2);
						g.setClip(ellipse);
						g.drawImage(currentMosaicTileImage, i * xChunk, j * yChunk, xChunk, yChunk, null);	
					}
					
					if (tint > 0) {
						Color c = newImg[i][j].getAverageColors();
						Color newColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), tint);
						g.setColor(newColor);
						g.fillRect(i * xChunk, j * yChunk, xChunk, yChunk);
					}
					
					Rectangle2D rect = new Rectangle2D.Float();
					rect.setRect(i * xChunk, j * yChunk, xChunk, yChunk);
					g.setClip(rect);
					
					Color c = newImg[i][j].getAverageColors();
					Color newColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);
					g.setColor(newColor);
					g.drawOval((i * xChunk) + padding, (j * yChunk) + padding, xChunk - padding *2, yChunk - padding *2);
					counter++;
					
					if (verbose) {
						printProgBar((counter * 100) / (blocks * blocks));	
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
//					sb.append('\n');
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
