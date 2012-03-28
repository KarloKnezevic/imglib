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


package net.imglib2.ops.image;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.ops.Condition;
import net.imglib2.ops.DiscreteNeigh;
import net.imglib2.ops.Function;
import net.imglib2.ops.RegionIndexIterator;
import net.imglib2.type.numeric.ComplexType;

// In old AssignOperation could do many things
// - set conditions on each input and output image
//     Now this can be done by creating a complex Condition
// - set regions of input and output
//     Now this can be done by creating a complex Condition
// - interrupt from another thread
//     Done via abort()
// - observe the iteration
//     still to do
// regions in same image could be handled by a translation function that
//   transforms from one space to another
// regions in different images can also be handled this way
//   a translation function takes a function and a coord transform
// now also these regions, if shape compatible, can be composed into a N+1
//   dimensional space and handled as one dataset
// TODO
// - add listeners in assign (like progress indicators, stat collectors, etc.)


/**
 * A multithreaded implementation that assigns the values of a region of
 * an Img<A> to values from a Function<long[],B>. A and B extend ComplexType<?>.
 *  
 * @author Barry DeZonia
 */
public class ImageAssignment<A extends ComplexType<A>,B extends ComplexType<B>> {

	// -- instance variables --

	private ExecutorService executor;
	private boolean assigning;
	private List<Runnable> tasks;
	
	// -- constructor --
	
	/**
	 * Constructor. A working neighborhood is built using negOffs and posOffs. If
	 * they are zero in extent the working neighborhood is a single pixel. This
	 * neighborhood is moved point by point over the Img<?> and passed to the
	 * function for evaluation. Pixels are assigned in the Img<?> if the given
	 * condition is satisfied at that point.
	 * 
	 * @param img - the Img<A extends ComplexType<A>> to assign data values to
	 * @param origin - the origin of the region to assign within the Img<A>
	 * @param span - the extents of the region to assign within the Img<A>
	 * @param function - the Function<long[],B> to evaluate at each point of the region
	 * @param condition - the condition that must be satisfied
	 * @param negOffs - the extents in the negative direction of the working neighborhood
	 * @param posOffs - the extents in the positive direction of the working neighborhood
	 * 
	 */
	public ImageAssignment(
		Img<A> img,
		long[] origin,
		long[] span,
		Function<long[],B> function,
		Condition<long[]> condition,
		long[] negOffs,
		long[] posOffs)
	{
		this.assigning = false;
		this.executor = null;
		this.tasks = null;
		setupTasks(img, origin, span, function, condition, negOffs, posOffs);
	}
	
	/**
	 * Constructor. A working neighborhood is assumed to be a single pixel. This
	 * neighborhood is moved point by point over the Img<A> and passed to the
	 * function for evaluation. Pixels are assigned in the Img<A> if the given
	 * condition is satisfied at that point.
	 *
	 * @param img - the Img<A extends ComplexType<A>> to assign data values to
	 * @param origin - the origin of the region to assign within the Img<A>
	 * @param span - the extents of the region to assign within the Img<A>
	 * @param function - the Function<long[],B> to evaluate at each point of the region
	 * @param condition - the condition that must be satisfied
	 * 
	 */
	public ImageAssignment(
		Img<A> img,
		long[] origin,
		long[] span,
		Function<long[],B> function,
		Condition<long[]> condition)
	{
		this(img, origin, span, function, condition, new long[origin.length], new long[origin.length]);
	}
		
	// -- public interface --

	/**
	 * Assign pixels using input variables specified in constructor. Can be
	 * aborted using abort().
	 */
	public void assign() {
		synchronized(this) {
			assigning = true;
			executor = Executors.newFixedThreadPool(tasks.size());
			for (Runnable task : tasks)
				executor.submit(task);
		}
		boolean terminated = true;
		synchronized (this) {
			// TODO - does this shutdown() call return immediately or wait until
			// everything is complete. If it waits then this synchronized block will
			// keep abort() from being able to work.
			executor.shutdown();
			terminated = executor.isTerminated();
			if (terminated) executor = null;
		}
		while (!terminated) {
			try { Thread.sleep(100); } catch (Exception e) { /* do nothing */ }
			synchronized (this) {
				terminated = executor.isTerminated();
				if (terminated) executor = null;
			}
		}
		synchronized (this) {
			assigning = false;
		}
	}

	/**
	 * Aborts an in progress assignment. Has no effect if not currently
	 * running an assign() operation.
	 */
	public void abort() {
		// TODO - this method maybe ineffective. See TODO note in assign().
		boolean terminated = true;
		synchronized (this) {
			if (!assigning) return;
			if (executor != null) {
				executor.shutdownNow();
				terminated = executor.isTerminated();
			}
		}
		while (!terminated) {
			try { Thread.sleep(100); } catch (Exception e) { /* do nothing */ }
			synchronized (this) {
				if (executor == null)
					terminated = true;
				else
					terminated = executor.isTerminated();
			}
		}
	}

	// -- private helpers --

	private void setupTasks(
		Img<A> img,
		long[] origin,
		long[] span,
		Function<long[],B> func,
		Condition<long[]> cond,
		long[] negOffs,
		long[] posOffs)
	{
		tasks = new ArrayList<Runnable>();
		int axis = chooseBestAxis(span);
		int numThreads = chooseNumThreads(span,axis);
		long length = span[axis] / numThreads;
		if (span[axis] % numThreads > 0) length++;
		long startOffset = 0;
		while (startOffset < span[axis]) {
			if (startOffset + length > span[axis]) length = span[axis] - startOffset;
			Runnable task =
					task(img, origin, span, axis, origin[axis] + startOffset, length, func, cond, negOffs, posOffs);
			tasks.add(task);
			startOffset += length;
		}
	}

	/**
	 * Determines best axis to divide along. Currently chooses biggest axis.
	 */
	private int chooseBestAxis(long[] span) {
		int bestAxis = 0;
		long bestAxisSize = span[bestAxis];
		for (int i = 1; i < span.length; i++) {
			long axisSize = span[i]; 
			if (axisSize > bestAxisSize) {
				bestAxis = i;
				bestAxisSize = axisSize;
			}
		}
		return bestAxis;
	}

	/**
	 * Determines how many threads to use
	 */
	private int chooseNumThreads(long[] span, int axis) {
		int maxThreads = Runtime.getRuntime().availableProcessors();
		if (maxThreads == 1) return 1;
		long numElements = numElements(span);
		if (numElements < 10000L) return 1;
		long axisSize = span[axis];
		if (axisSize < maxThreads)
			return (int) axisSize;
		return maxThreads;
	}

	/**
	 * Calculates the number of elements in the output region span
	 */
	private long numElements(long[] span) {
		if (span.length == 0) return 0;
		long numElems = span[0];
		for (int i = 1; i < span.length; i++)
			numElems *= span[i];
		return numElems;
	}

	/** Creates a Runnable task that can be submitted to the thread executor.
	 * The task assigns values to a subset of the output region.
	 */
	private Runnable task(
		Img<A> img,
		long[] imageOrigin,
		long[] imageSpan,
		int axis,
		long startIndex,
		long length,
		Function<long[],B> fn,
		Condition<long[]> cnd,
		long[] nOffsets,
		long[] pOffsets)
	{
		//System.out.println("axis "+axis+" start "+startIndex+" len "+length);
		final long[] regOrigin = imageOrigin.clone();
		regOrigin[axis] = startIndex;
		final long[] regSpan = imageSpan.clone();
		regSpan[axis] = length;
		
		// FIXME - warning unavoidable at moment. We don't have the type. If
		// we remove typing from RegionRunner it won't compile.

		return
			new RegionRunner<A,B>(
				img,
				regOrigin,
				regSpan,
				fn.copy(),
				(cnd == null ? null : cnd.copy()),
				nOffsets.clone(),
				pOffsets.clone());
	}

	/**
	 * RegionRunner is the workhorse for assigning output values from the
	 * evaluation of the input function across a subset of the output region.
	 */
	private class RegionRunner<U extends ComplexType<U>, V extends ComplexType<V>>
		implements Runnable
	{
		private final Img<U> img;
		private final Function<long[], V> function;
		private final Condition<long[]> condition;
		private final DiscreteNeigh region;
		private final DiscreteNeigh neighborhood;

		/**
		 * Constructor
		 */
		public RegionRunner(
			Img<U> img,
			long[] origin,
			long[] span,
			Function<long[], V> func,
			Condition<long[]> cond,
			long[] negOffs,
			long[] posOffs)
		{
			this.img = img;
			this.function = func;
			this.condition = cond;
 			this.region = buildRegion(origin, span);
			this.neighborhood = new DiscreteNeigh(new long[negOffs.length], negOffs, posOffs);
		}

		/**
		 * Conditionally assigns pixels in the output region.
		 */
		@Override
		public void run() {
			final RandomAccess<U> accessor = img.randomAccess();
			final V output = function.createOutput();
			final RegionIndexIterator iter = new RegionIndexIterator(region);
			while (iter.hasNext()) {
				iter.fwd();
				neighborhood.moveTo(iter.getPosition());
				boolean proceed =
						(condition == null) ||
						(condition.isTrue(neighborhood,iter.getPosition()));
				if (proceed) {
					function.evaluate(neighborhood, iter.getPosition(), output);
					accessor.setPosition(iter.getPosition());
					accessor.get().setReal(output.getRealDouble());
					accessor.get().setImaginary(output.getImaginaryDouble());
					// FIXME
					// Note - for real datasets this imaginary assignment may waste cpu
					// cycles. Perhaps it can get optimized away by the JIT. But maybe not
					// since the type is not really known because this class is really
					// constructed from a raw type. We'd need to test how the JIT handles
					// this situation. Note that in past incarnations this class used
					// assigner classes. The complex version set R & I but the real
					// version just set R. We could adopt that approach once again.
				}
			}
		}
		
		/**
		 * Builds a DiscreteNeigh region from an origin and span. The
		 * DiscreteNeigh is needed for use with a RegionIndexIterator.
		 */
		private DiscreteNeigh buildRegion(long[] org, long[] spn) {
			long[] nOffsets = new long[org.length];
			long[] pOffsets = new long[org.length];
			for (int i = 0; i < org.length; i++)
				pOffsets[i] = spn[i] - 1;
			return new DiscreteNeigh(org, nOffsets, pOffsets);
		}
	}
}
