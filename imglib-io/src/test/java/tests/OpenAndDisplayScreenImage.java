package tests;

import ij.ImageJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;

import java.io.IOException;

import loci.formats.FormatException;
import mpicbg.imglib.display.ARGBScreenImage;
import mpicbg.imglib.display.RealARGBConverter;
import mpicbg.imglib.display.XYProjector;
import mpicbg.imglib.img.Img;
import mpicbg.imglib.img.array.ArrayImgFactory;
import mpicbg.imglib.io.ImgOpener;
import mpicbg.imglib.type.numeric.ARGBType;
import mpicbg.imglib.type.numeric.real.FloatType;

public class OpenAndDisplayScreenImage
{	
	final static public void main( final String[] args )
		throws FormatException, IOException
	{
		new ImageJ();
		
		final ImgOpener io = new ImgOpener();
		Img< FloatType > img = io.openImg( "/home/saalfeld/Desktop/73.tif", new ArrayImgFactory<FloatType>(), new FloatType()).getImg();
		
		final ARGBScreenImage screenImage = new ARGBScreenImage( ( int )img.dimension( 0 ), ( int )img.dimension( 1 ) );
		final XYProjector< FloatType, ARGBType > projector = new XYProjector< FloatType, ARGBType >( img, screenImage, new RealARGBConverter< FloatType >( 0, 127 ) );
		
		final ColorProcessor cp = new ColorProcessor( screenImage.image() );
		final ImagePlus imp = new ImagePlus( "argbScreenProjection", cp );
		imp.show();

		for ( int k = 0; k < 3; ++k ) 
			for ( int i = 0; i < img.dimension( 2 ); ++i )
			{
				projector.setPosition( i, 2 );
				projector.map();
				final ColorProcessor cpa = new ColorProcessor( screenImage.image() );
				imp.setProcessor( cpa );
				imp.updateAndDraw();
			}
		
		projector.map();
		
		projector.setPosition( 40, 2 );
		projector.map();
	}
}
