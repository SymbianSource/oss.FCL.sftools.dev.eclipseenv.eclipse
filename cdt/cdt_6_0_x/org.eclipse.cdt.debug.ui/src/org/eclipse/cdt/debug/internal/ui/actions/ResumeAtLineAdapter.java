/*******************************************************************************
 * Copyright (c) 2004, 2007 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 * Freescale - https://bugs.eclipse.org/bugs/show_bug.cgi?id=186929
 *******************************************************************************/
package org.eclipse.cdt.debug.internal.ui.actions; 

import org.eclipse.cdt.core.IAddress;
import org.eclipse.cdt.debug.core.CDIDebugModel;
import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.cdt.debug.core.model.IResumeAtAddress;
import org.eclipse.cdt.debug.core.model.IResumeAtLine;
import org.eclipse.cdt.debug.internal.core.ICDebugInternalConstants;
import org.eclipse.cdt.debug.internal.core.model.CDebugElement;
import org.eclipse.cdt.debug.internal.core.sourcelookup.CSourceLookupDirector;
import org.eclipse.cdt.debug.internal.ui.CDebugUIUtils;
import org.eclipse.cdt.debug.internal.ui.IInternalCDebugUIConstants;
import org.eclipse.cdt.debug.internal.ui.views.disassembly.DisassemblyEditorInput;
import org.eclipse.cdt.debug.internal.ui.views.disassembly.DisassemblyView;
import org.eclipse.cdt.debug.ui.CDebugUIPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;
 
/**
 * Resume at line target adapter for the CDI debugger
 */
public class ResumeAtLineAdapter implements IResumeAtLineTarget {

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.internal.ui.actions.IResumeAtLineTarget#resumeAtLine(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection, org.eclipse.debug.core.model.ISuspendResume)
	 */
	public void resumeAtLine( IWorkbenchPart part, ISelection selection, ISuspendResume target ) throws CoreException {
		String errorMessage = null;
		if ( part instanceof ITextEditor ) {
			ITextEditor textEditor = (ITextEditor)part;
			IEditorInput input = textEditor.getEditorInput();
			if ( input == null ) {
				errorMessage = ActionMessages.getString( "ResumeAtLineAdapter.0" ); //$NON-NLS-1$
			}
			else {
				IDocument document = textEditor.getDocumentProvider().getDocument( input );
				if ( document == null ) {
					errorMessage = ActionMessages.getString( "ResumeAtLineAdapter.1" ); //$NON-NLS-1$
				}
				else {
					final String fileName = getFileName( input ); // actually, absolute path, not just file name
					IDebugTarget debugTarget = null;
					if ( target instanceof CDebugElement ) { // should always be, but just in case
						debugTarget = ((CDebugElement)target).getDebugTarget();
					}
					final IPath path = convertPath( fileName, debugTarget );					
					ITextSelection textSelection = (ITextSelection)selection;
					final int lineNumber = textSelection.getStartLine() + 1;
					if ( target instanceof IAdaptable ) {
						final IResumeAtLine resumeAtLine = (IResumeAtLine)((IAdaptable)target).getAdapter( IResumeAtLine.class );
						if ( resumeAtLine != null && resumeAtLine.canResumeAtLine( path.toPortableString(), lineNumber ) ) {
							Runnable r = new Runnable() {
								public void run() {
									try {
										resumeAtLine.resumeAtLine( path.toPortableString(), lineNumber );
									}
									catch( DebugException e ) {
										failed( e );
									}								
								}
							};
							runInBackground( r );
						}
					}
					return;
				}
			}
		}
		else if ( part instanceof DisassemblyView ) {
			IEditorInput input = ((DisassemblyView)part).getInput();
			if ( !(input instanceof DisassemblyEditorInput) ) {
				errorMessage = ActionMessages.getString( "ResumeAtLineAdapter.2" ); //$NON-NLS-1$
			}
			else {
				ITextSelection textSelection = (ITextSelection)selection;
				int lineNumber = textSelection.getStartLine() + 1;
				final IAddress address = ((DisassemblyEditorInput)input).getAddress( lineNumber );
				if ( address != null && target instanceof IAdaptable ) {
					final IResumeAtAddress resumeAtAddress = (IResumeAtAddress)((IAdaptable)target).getAdapter( IResumeAtAddress.class );
					if ( resumeAtAddress != null && resumeAtAddress.canResumeAtAddress( address ) ) {
						Runnable r = new Runnable() {
							
							public void run() {
								try {
									resumeAtAddress.resumeAtAddress( address );
								}
								catch( DebugException e ) {
									failed( e );
								}								
							}
						};
						runInBackground( r );
					}
				}
				return;
			}
		}
		else {
			errorMessage = ActionMessages.getString( "ResumeAtLineAdapter.3" ); //$NON-NLS-1$
		}
		throw new CoreException( new Status( IStatus.ERROR, CDebugUIPlugin.getUniqueIdentifier(), IInternalCDebugUIConstants.INTERNAL_ERROR, errorMessage, null ) );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.internal.ui.actions.IResumeAtLineTarget#canResumeAtLine(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection, org.eclipse.debug.core.model.ISuspendResume)
	 */
	public boolean canResumeAtLine( IWorkbenchPart part, ISelection selection, ISuspendResume target ) {
		if ( target instanceof IAdaptable ) {			
			if ( part instanceof IEditorPart ) {
				IResumeAtLine resumeAtLine = (IResumeAtLine)((IAdaptable)target).getAdapter( IResumeAtLine.class );
				if ( resumeAtLine == null)
					return false;
				IEditorPart editorPart = (IEditorPart)part;
				IEditorInput input = editorPart.getEditorInput();
				if ( input == null ) {
					return false;
				}
				if ( !(editorPart instanceof ITextEditor) ) {
					return false;
				}
				ITextEditor textEditor = (ITextEditor)editorPart;
				IDocument document = textEditor.getDocumentProvider().getDocument( input );
				if ( document == null ) {
					return false;
				}
				String fileName = null; // actually, absolute path, not just file name
				try {
					fileName = getFileName( input );
				}
				catch( CoreException e ) {
				}
				if (fileName == null) {
					return false;
				}
				IDebugTarget debugTarget = null;
				if ( target instanceof CDebugElement ) { // should always be, but just in case
					debugTarget = ((CDebugElement)target).getDebugTarget();
				}
				if (debugTarget == null) {
					return false;
				}
				final IPath path = convertPath( fileName, debugTarget );									
				ITextSelection textSelection = (ITextSelection)selection;
				int lineNumber = textSelection.getStartLine() + 1;
				return resumeAtLine.canResumeAtLine( path.toPortableString(), lineNumber );
			}
			if ( part instanceof DisassemblyView ) {
				IResumeAtAddress resumeAtAddress = (IResumeAtAddress)((IAdaptable)target).getAdapter( IResumeAtAddress.class );
				if ( resumeAtAddress == null )
					return false;
				IEditorInput input = ((DisassemblyView)part).getInput();
				if ( !(input instanceof DisassemblyEditorInput) ) {
					return false;
				}
				ITextSelection textSelection = (ITextSelection)selection;
				int lineNumber = textSelection.getStartLine() + 1;
				IAddress address = ((DisassemblyEditorInput)input).getAddress( lineNumber );
				return resumeAtAddress.canResumeAtAddress( address );
			}
		}
		return false;
	}

	private String getFileName( IEditorInput input ) throws CoreException {
		return CDebugUIUtils.getEditorFilePath(input);
	}

	private void runInBackground( Runnable r ) {
		DebugPlugin.getDefault().asyncExec( r );
	}

	protected void failed( Throwable e ) {
		MultiStatus ms = new MultiStatus( CDIDebugModel.getPluginIdentifier(), ICDebugInternalConstants.STATUS_CODE_ERROR, ActionMessages.getString( "ResumeAtLineAdapter.4" ), null ); //$NON-NLS-1$
		ms.add( new Status( IStatus.ERROR, CDIDebugModel.getPluginIdentifier(), ICDebugInternalConstants.STATUS_CODE_ERROR, e.getMessage(), e ) );
		CDebugUtils.error( ms, this );
	}
	
	private IPath convertPath( String sourceHandle, IDebugTarget debugTarget ) {
		IPath path = null;
		if ( Path.EMPTY.isValidPath( sourceHandle ) ) {
			if ( debugTarget != null ) {
				ISourceLocator sl = debugTarget.getLaunch().getSourceLocator();
				if ( sl instanceof CSourceLookupDirector ) {
					path = ((CSourceLookupDirector)sl).getCompilationPath( sourceHandle );
				}
			}
			if ( path == null ) {
				path = new Path( sourceHandle );
			}
		}
		return path;
	}
}
