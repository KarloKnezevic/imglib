package script.imglib.test;

import static org.junit.Assert.assertTrue;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.RankFilters;
import ij.process.ImageProcessor;
import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.io.ImgOpener;
import net.imglib2.script.ImgLib;
import net.imglib2.script.algorithm.integral.histogram.IntegralHistogramCursor;
import net.imglib2.script.algorithm.integral.histogram.IntegralHistogram;
import net.imglib2.script.algorithm.integral.histogram.LinearHistogram;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;

import org.junit.Test;

public class TestHistograms {

	static public final void main(String[] arg) {
		new ImageJ();
		TestHistograms t = new TestHistograms();
		//t.testFeatures();
		//t.testHistogramOf3x3Img();
		t.comparePerformance();
	}
	
	public void testCorners() {
		long[] window = new long[]{10, 10, 10};
		int numDimensions = window.length;
		Point[] offsets = new Point[(int)Math.pow(2, numDimensions)];
		for (int i=0; i<offsets.length; ++i) offsets[i] = new Point(numDimensions);
		int d = 0;
		while (d < numDimensions) {
			final int flip = (int)Math.pow(2, d);
			int sign = -1;
			for (int i=0; i<offsets.length;) {
				offsets[i].setPosition(sign * window[d] / 2, d);
				++i;
				if (0 == i % flip) sign *= -1;
			}
			++d;
		}
		for (int i=0; i<offsets.length; ++i) {
			System.out.println(offsets[i].toString());
		}
	}
	
	public void testFeatures() {
		try {
			Img<UnsignedByteType> img = new ImgOpener().openImg("/home/albert/Desktop/t2/bridge-crop-streched-smoothed.tif");
			ImgLib.wrap(img, "Original").show();
			long[] radius = new long[]{10, 10}; // radius=1 is equivalent to ImageJ's radius=1 in RankFilters
			LinearHistogram<UnsignedByteType> lh = new LinearHistogram<UnsignedByteType>(256, img.numDimensions(), new UnsignedByteType(0), new UnsignedByteType(255));
			Img<UnsignedShortType> integralHistogram = IntegralHistogram.create(img, lh, new UnsignedShortType());
			HistogramFeatures<UnsignedByteType, UnsignedShortType> features = new HistogramFeatures<UnsignedByteType, UnsignedShortType>(img, integralHistogram, lh, radius);
			ImgLib.wrap(features, "Features for " + radius[0] + "x" + radius[1]).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void comparePerformance() {
		try {
			ij.Prefs.setThreads(1);
			Img<UnsignedByteType> img = new ImgOpener().openImg("/home/albert/Desktop/t2/bridge.tif");
			ImgLib.wrap(img, "Original").show();
			
			long t0 = System.currentTimeMillis();
			final UnsignedByteType min = new UnsignedByteType(0);
			final UnsignedByteType max = new UnsignedByteType(255);
			final int nBins = 128; // 32 delivers speeds close to ImageJ's when not using median
			final LinearHistogram<UnsignedByteType> lh = new LinearHistogram<UnsignedByteType>(nBins, img.numDimensions(), min, max);
			final Img<IntType> integralHistogram = IntegralHistogram.create(img, lh, new IntType());
			System.out.println("Creating integral histogram took " + (System.currentTimeMillis() - t0) + " ms");
			long[] radius = new long[]{25, 25}; // radius=1 is equivalent to ImageJ's radius=1 in RankFilters
			HistogramFeatures<UnsignedByteType, IntType> features =
					new HistogramFeatures<UnsignedByteType, IntType>(img, integralHistogram, lh, radius);
			long t1 = System.currentTimeMillis();
			
			ImagePlus imp = ImgLib.wrap(img);
			
			long t2 = System.currentTimeMillis();
			RankFilters rf = new RankFilters();
			ImageProcessor ip1 = imp.getProcessor().duplicate();
			rf.rank(ip1, radius[0], RankFilters.MIN);
			ImageProcessor ip2 = imp.getProcessor().duplicate();
			rf.rank(ip2, radius[0], RankFilters.MAX);
			ImageProcessor ip3 = imp.getProcessor().duplicate();
			rf.rank(ip3, radius[0], RankFilters.MEAN);
			ImageProcessor ip4 = imp.getProcessor().duplicate();
			rf.rank(ip4, radius[0], RankFilters.MEDIAN);
			long t3 = System.currentTimeMillis();
			
			System.out.println("Integral features: " + (t1 - t0) + " ms");
			System.out.println("Regular features: " + (t3 - t2) + " ms");
			
			// Show them both
			ImgLib.wrap(features, "Integral Histogram Features for " + radius[0] + "x" + radius[1]).show();
			ImageStack stack = new ImageStack(imp.getWidth(), imp.getHeight());
			stack.addSlice("min", ip1);
			stack.addSlice("max", ip2);
			stack.addSlice("mean", ip3);
			stack.addSlice("median", ip4);
			new ImagePlus("Regular features for " + radius[0] + "x" + radius[1], stack).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * 1 2 3
	 * 2 4 6
	 * 3 6 9
	 * 
	 * In the histogram, the bottom right should have:
	 * {1:1, 2:2, 3:2, 4:1, 5:0, 6:2, 7:0, 8:0, 9:0}
	 * 
	 */
	@Test
	public <T extends IntegerType<T> & NativeType<T> >void testHistogramOf3x3Img() {
		Img<UnsignedByteType> img =
				new UnsignedByteType().createSuitableNativeImg(
						new ArrayImgFactory<UnsignedByteType>(),
						new long[]{3, 3});
		Cursor<UnsignedByteType> c = img.cursor();
		long[] position = new long[3];
		int sum = 0;
		while (c.hasNext()) {
			c.fwd();
			c.localize(position);
			int v = 1;
			for (int i=0; i<position.length; ++i) v *= position[i] + 1;
			c.get().set(v);
			sum += v;
		}
		assertTrue("Test image isn't right", sum == 36);
		//
		try {
			ImgLib.wrap(img, "image").show();
		} catch (ImgLibException e) {
			e.printStackTrace();
		}
		// Histogram
		UnsignedByteType min = new UnsignedByteType(1);
		UnsignedByteType max = new UnsignedByteType(9);
		LinearHistogram<UnsignedByteType> lh = new LinearHistogram<UnsignedByteType>(9, img.numDimensions(), min, max);
		Img<T> h = IntegralHistogram.create(img, lh);
		
		// Expected cummulative:
		final int[] expected = new int[]{1, 2, 2, 1, 0, 2, 0, 0, 1};
		
		RandomAccess<T> ra = h.randomAccess();
		StringBuilder sb = new StringBuilder("{");
		ra.setPosition(3, 0);
		ra.setPosition(3, 1);
		for (int i=0; i<9; ++i) {
			ra.setPosition(i, 2);
			if (i > 0) sb.append(',').append(' ');
			sb.append(i+1).append(':').append(ra.get().getInteger());
			assertTrue("Cummulative histogram isn't right", expected[i] == ra.get().getInteger());
		}
		sb.append('}');
		System.out.println("Cummulative Histogram: " + sb.toString());
		
		try {
			ImgLib.wrap((Img)h, "histogram").show();
		} catch (ImgLibException e) {
			e.printStackTrace();
		}
		
		// Test extracting the histogram for the last pixel
		long[] px = new long[]{1, 2, 1, 2};
		long[] py = new long[]{1, 1, 2, 2};
		int[] sign = new int[]{1, -1, -1, 1};
		long[] hist = new long[9];
		for (int i=0; i<4; ++i) {
			ra.setPosition(px[i] + 1, 0);
			ra.setPosition(py[i] + 1, 1);
			for (int bin=0; bin<9; ++bin) {
				ra.setPosition(bin, 2);
				hist[bin] += sign[i] * ra.get().getIntegerLong();
			}
		}
		for (int bin=0; bin<9; ++bin) {
			if (8 == bin) assertTrue(1 == hist[bin]);
			else assertTrue(0 == hist[bin]);
		}
		System.out.println(Util.printCoordinates(hist));
		
		// Test extracting the histogram for the lower right 2x2 area
		px = new long[]{0, 2, 0, 2};
		py = new long[]{0, 0, 2, 2};
		sign = new int[]{1, -1, -1, 1};
		hist = new long[9];
		for (int i=0; i<4; ++i) {
			ra.setPosition(px[i] + 1, 0);
			ra.setPosition(py[i] + 1, 1);
			for (int bin=0; bin<9; ++bin) {
				ra.setPosition(bin, 2);
				hist[bin] += sign[i] * ra.get().getIntegerLong();
			}
		}
		for (int bin=0; bin<9; ++bin) {
			switch (bin) {
			case 3:
			case 8:
				assertTrue(1 == hist[bin]);
				break;
			case 5:
				assertTrue(2 == hist[bin]);
				break;
			default:
				assertTrue(0 == hist[bin]);
				break;
			}
		}
		System.out.println(Util.printCoordinates(hist));
		
		// Histograms
		long[] radius = new long[]{0, 0};
		lh = new LinearHistogram<UnsignedByteType>(9, 2, min, max);
		IntegralHistogramCursor<T, UnsignedByteType> hs = new IntegralHistogramCursor<T, UnsignedByteType>(h, lh, radius);
		hs.setPosition(2, 0);
		hs.setPosition(2, 1);
		hist = hs.get().bins;
		System.out.println("From Histograms, 0x0: " + Util.printCoordinates(hist));
		for (int bin=0; bin<9; ++bin) {
			if (8 == bin) assertTrue(1 == hist[bin]);
			else assertTrue(0 == hist[bin]);
		}
		
		radius = new long[]{1, 1}; // means 3x3 centered on the pixel
		hs = new IntegralHistogramCursor<T, UnsignedByteType>(h, lh, radius);
		hs.setPosition(2, 0);
		hs.setPosition(2, 1);
		hist = hs.get().bins;
		System.out.println("From Histograms, 3x3: " + Util.printCoordinates(hist));
		for (int bin=0; bin<9; ++bin) {
			switch (bin) {
			case 3:
			case 8:
				assertTrue(1 == hist[bin]);
				break;
			case 5:
				assertTrue(2 == hist[bin]);
				break;
			default:
				assertTrue(0 == hist[bin]);
				break;
			}
		}
		
		radius = new long[]{0, 0};
		lh = new LinearHistogram<UnsignedByteType>(9, 2, new UnsignedByteType(1), new UnsignedByteType(9));
		Img<UnsignedByteType> integralHistogram = IntegralHistogram.create(img, lh, new UnsignedByteType());
		HistogramFeatures<UnsignedByteType, UnsignedByteType> features =
				new HistogramFeatures<UnsignedByteType, UnsignedByteType>(img, integralHistogram, lh, radius);
		try {
			ImgLib.wrap(features, "features for 0x0").show();
		} catch (ImgLibException e) {
			e.printStackTrace();
		}
		
	}
}