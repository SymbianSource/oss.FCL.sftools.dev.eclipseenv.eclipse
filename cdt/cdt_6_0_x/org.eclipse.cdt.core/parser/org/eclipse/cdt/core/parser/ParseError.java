/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John Camelon (IBM Rational Software) - Initial API and implementation
 *     Mike Kucera (IBM)- convert to Java 5 enum
 *******************************************************************************/
package org.eclipse.cdt.core.parser;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ParseError extends Error {

	private static final long serialVersionUID= -3626877473345356953L;
	
	private final ParseErrorKind errorKind;

	public enum ParseErrorKind
	{
		// the method called is not implemented in this particular implementation
		METHOD_NOT_IMPLEMENTED,
		
		// offset specified is within a section of code #if'd out by the preprocessor 
		// semantic context cannot be provided in this case
		OFFSETDUPLE_UNREACHABLE,
		
		// offset range specified is not a valid identifier or qualified name
		// semantic context cannot be provided in this case
		OFFSET_RANGE_NOT_NAME,

		TIMEOUT_OR_CANCELLED,
	}

	public ParseErrorKind getErrorKind()
	{
		return errorKind;
	}
	
	public ParseError( ParseErrorKind kind )
	{
		errorKind = kind;
	}
}
