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
package org.eclipse.cdt.internal.ui.refactoring.implementmethod;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * @author Mirko Stocker
 *
 */
public class ImplementMethodRefactoringWizard extends RefactoringWizard {

       private final ImplementMethodRefactoring refactoring;

	public ImplementMethodRefactoringWizard(ImplementMethodRefactoring refactoring) {
    	   super(refactoring, WIZARD_BASED_USER_INTERFACE);
    	   this.refactoring = refactoring;
       }

       @Override
       protected void addUserInputPages() {
    	   if(refactoring.getParameterHandler().needsAdditionalArgumentNames()) {
    		   addPage(new ParameterNamesInputPage(refactoring.getParameterHandler()));
    	   }
       }
}
