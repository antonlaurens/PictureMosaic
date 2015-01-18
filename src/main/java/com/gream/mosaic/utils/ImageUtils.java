package com.gream.mosaic.utils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import javax.imageio.ImageIO;

public class ImageUtils {

	public static Color getAverageRGB(File f) throws Exception {
		BufferedImage img = ImageIO.read(f);
		return getAverageRGB(img);
	}
	
	public static Color getAverageRGB(URL url) throws Exception {
		BufferedImage img = ImageIO.read(url);
		return getAverageRGB(img);
	}
	
	public static Color getAverageRGB(BufferedImage img) {
		float red = 0.0f, green = 0.0f, blue = 0.0f;
		for (int i = 0; i < img.getWidth(); i++) {
			for (int j = 0; j < img.getHeight(); j++) {
				Color c = new Color(img.getRGB(i, j));
				red += c.getRed();
				green += c.getGreen();
				blue += c.getBlue();
			}
		}
		float pixels = img.getHeight() * img.getWidth();
		return new Color(red / pixels / 255, green / pixels / 255, blue / pixels / 255);	
	}

	public static Color getAverageRGB(int[] arr) {
		float red = 0.0f, green = 0.0f, blue = 0.0f;
		for (int i = 0; i < arr.length; i++) {
			Color c = new Color(arr[i]);
			red += c.getRed();
			green += c.getGreen();
			blue += c.getBlue();	
		}
		
		float pixels = arr.length;
		return new Color(red / pixels / 255, green / pixels / 255, blue / pixels / 255);
	}
	
}
