/*******************************************************************************
 * Copyright (c) 2008 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 *  
 * Contributors: 
 * Institute for Software - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.ui.tests.refactoring.utils;

import junit.framework.TestCase;

import org.eclipse.cdt.internal.ui.refactoring.utils.IdentifierHelper;
import org.eclipse.cdt.internal.ui.refactoring.utils.IdentifierResult;

/**
 * @author Thomas Corbat
 *
 */
public class CorrectCaseTest extends TestCase {

	public CorrectCaseTest(){
		super("Check Correct Identifier"); //$NON-NLS-1$
	}
	
	@Override
	public void runTest() {
		IdentifierResult result;
		
		result = IdentifierHelper.checkIdentifierName("A"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("Z"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("a"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("z"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("_"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("_A"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("_Z"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("_a"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("_z"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("__"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("_0"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("_9"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("Aaaa"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("Zaaa"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("aaaa"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("zaaa"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());
		
		result = IdentifierHelper.checkIdentifierName("_aaa"); //$NON-NLS-1$
		assertTrue(result.getMessage(), IdentifierResult.VALID == result.getResult());

	}

}
