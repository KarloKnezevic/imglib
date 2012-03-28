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

package net.imglib2.ops.example;

import net.imglib2.img.Img;
import net.imglib2.ops.Condition;
import net.imglib2.ops.Function;
import net.imglib2.ops.condition.AtKeyPointCondition;
import net.imglib2.ops.function.complex.ComplexImageFunction;
import net.imglib2.ops.function.general.GeneralUnaryFunction;
import net.imglib2.ops.image.ImageAssignment;
import net.imglib2.ops.operation.unary.real.RealSqr;
import net.imglib2.ops.operation.unary.real.RealUnaryOperation;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexDoubleType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * TODO
 *
 */
public class ExampleMisc {

	static void basicAssignmentSequence() {
		Img<UnsignedByteType> inputImg = null; // image 100x200
		Img<UnsignedByteType> outputImg = null; // image 100x200

		// sub region for assignment
		long[] origin = new long[] { 0, 0 };
		long[] span = new long[] { 50, 40 };

		RealUnaryOperation<DoubleType,DoubleType> op =
				new RealSqr<DoubleType, DoubleType>();

		Function<long[], DoubleType> imageFunc =
				new ComplexImageFunction<UnsignedByteType,DoubleType>(
						inputImg, new DoubleType());

		Function<long[], DoubleType> func =
			new GeneralUnaryFunction<long[],DoubleType,DoubleType>(
					imageFunc, op, new DoubleType());

		Condition<long[]> condition = new AtKeyPointCondition();

		ImageAssignment<UnsignedByteType,DoubleType> assigner =
				new ImageAssignment<UnsignedByteType,DoubleType>(
				outputImg, origin, span, func, condition);

		assigner.assign(); // processed in parallel

		assigner.abort(); // if desired
	}

	public static void main(String[] args) {

		RealType<?> r = new UnsignedByteType();
		ComplexType<?> c = new ComplexDoubleType();
		
		System.out.println(r.getClass()+" is a RealType : "+(r instanceof RealType<?>));
		System.out.println(r.getClass()+" is a ComplexType : "+(r instanceof ComplexType<?>));
		System.out.println(c.getClass()+" is a RealType : "+(c instanceof RealType<?>));
		System.out.println(c.getClass()+" is a ComplexType : "+(c instanceof ComplexType<?>));
	}
}
