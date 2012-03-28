/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2012 Stephan Preibisch, Stephan Saalfeld, Tobias
 * Pietzsch, Albert Cardona, Barry DeZonia, Curtis Rueden, Lee Kamentsky, Larry
 * Lindsey, Johannes Schindelin, Christian Dietz, Grant Harris, Jean-Yves
 * Tinevez, Steffen Jaensch, Mark Longair, Nick Perry, and Jan Funke.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */


package net.imglib2.io.img.virtual;

import net.imglib2.AbstractCursor;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;


/**
 * This class manages read only nonspatial access to a virtual image. Data
 * returned from get() can be written to but any changes are never saved
 * to disk.
 *  
 * @author Barry DeZonia
 */
public class VirtualCursor<T extends NativeType<T> & RealType<T>>
	extends AbstractCursor<T>
{
	private VirtualImg<T> virtImage;
	private IntervalIterator iter;
	private long[] position;
	private VirtualAccessor<T> accessor;
	
	public VirtualCursor(VirtualImg<T> image) {
		super(image.numDimensions());
		this.virtImage = image;
		long[] fullDimensions = new long[image.numDimensions()];
		image.dimensions(fullDimensions);
		this.iter = new IntervalIterator(fullDimensions);
		this.position = new long[fullDimensions.length];
		this.accessor = new VirtualAccessor<T>(virtImage);
	}
	
	@Override
	public T get() {
		iter.localize(position);
		return accessor.get(position);
	}

	@Override
	public void fwd() {
		iter.fwd();
	}

	@Override
	public void reset() {
		iter.reset();
	}

	@Override
	public boolean hasNext() {
		return iter.hasNext();
	}

	@Override
	public void localize(long[] pos) {
		iter.localize(pos);
	}

	@Override
	public long getLongPosition(int d) {
		return iter.getLongPosition(d);
	}

	@Override
	public VirtualCursor<T> copy() {
		return new VirtualCursor<T>(virtImage);
	}

	@Override
	public VirtualCursor<T> copyCursor() {
		return new VirtualCursor<T>(virtImage);
	}
	
	public Object getCurrentPlane() {
		return accessor.getCurrentPlane();
	}
}
