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


package net.imglib2.ops.function.real;

import net.imglib2.ops.Function;
import net.imglib2.ops.Neighborhood;
import net.imglib2.ops.RegionIndexIterator;
import net.imglib2.type.numeric.RealType;

// NOTE : convolution and correlation are similar operations whose output is
//   rotated by 180 degrees for the same kernel. You can get one or the other
//   from the same function by rotating the input kernel by 180 degrees. As
//   implemented below this function is really a Correlation.

// TODO
//   A convolution is really a GeneralBinaryOperation between an input function
//   and a kernel function. For efficiency this class exists. For generality a
//   kernel function could calculate in evaluate(neigh,point,output) the relation
//   of neigh and point and choose the correct index into the kernel. By doing
//   this we could have more flexibility in the definitions of kernels (rather
//   than just an array of user supplied reals).

/**
 * 
 * @author Barry DeZonia
 */
public class RealConvolutionFunction<T extends RealType<T>> implements Function<long[],T> {

	private final Function<long[],T> otherFunc;
	private final T variable;
	private final double[] kernel;
	private RegionIndexIterator iter;
	
	public RealConvolutionFunction(Function<long[],T> otherFunc, double[] kernel) {
		this.otherFunc = otherFunc;
		this.variable = createOutput();
		this.kernel = kernel;
		this.iter = null;
	}
	
	@Override
	public void evaluate(Neighborhood<long[]> region, long[] point, T output) {
		if (iter == null)
			iter = new RegionIndexIterator(region);
		else
			iter.relocate(region.getKeyPoint());
		iter.reset();
		int cell = 0;
		double sum = 0;
		while (iter.hasNext()) {
			iter.fwd();
			otherFunc.evaluate(region, iter.getPosition(), variable);
			sum += variable.getRealDouble() * kernel[cell++];
		}
		output.setReal(sum);
	}

	@Override
	public RealConvolutionFunction<T> copy() {
		return new RealConvolutionFunction<T>(otherFunc.copy(), kernel.clone());
	}

	@Override
	public T createOutput() {
		return otherFunc.createOutput();
	}
}
