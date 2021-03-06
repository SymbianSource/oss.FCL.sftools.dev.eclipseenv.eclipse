/*******************************************************************************
 * Copyright (c) 2004, 2007 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 * Anton Leherbauer (Wind River Systems) - bug 183291
 *******************************************************************************/
package org.eclipse.cdt.debug.internal.ui.actions;

import org.eclipse.cdt.debug.internal.ui.ICDebugHelpContextIds;
import org.eclipse.cdt.debug.internal.ui.IInternalCDebugUIConstants;
import org.eclipse.cdt.debug.internal.ui.views.disassembly.DisassemblyView;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class ToggleBreakpointRulerAction extends Action {

	static class EmptySelection implements ISelection {

		public boolean isEmpty() {
			return true;
		}		
	}
	
	private IVerticalRulerInfo fRuler;
	private IWorkbenchPart fTargetPart;
	private IToggleBreakpointsTarget fTargetAdapter;
	private static final ISelection EMPTY_SELECTION = new EmptySelection();  

	/**
	 * Constructor for ToggleBreakpointRulerAction.
	 * 
	 * @param ruler
	 * @param editor
	 */
	public ToggleBreakpointRulerAction( IWorkbenchPart part, IVerticalRulerInfo ruler ) {
		super( ActionMessages.getString( "ToggleBreakpointRulerAction.Toggle_Breakpoint_1" ) ); //$NON-NLS-1$
		fRuler = ruler;
		setTargetPart( part );
		part.getSite().getWorkbenchWindow().getWorkbench().getHelpSystem().setHelp( this, ICDebugHelpContextIds.TOGGLE_BREAKPOINT_ACTION );
		setId( IInternalCDebugUIConstants.ACTION_TOGGLE_BREAKPOINT );
	}

	/**
	 * Disposes this action
	 */
	public void dispose() {
		setTargetPart( null );
		fRuler = null;
	}

	/**
	 * @see Action#run()
	 */
	public void run() {
		try {
				fTargetAdapter.toggleLineBreakpoints( getTargetPart(), getTargetSelection() );
		}
		catch( CoreException e ) {
			ErrorDialog.openError( getTargetPart().getSite().getShell(), 
								   ActionMessages.getString( "ToggleBreakpointRulerAction.Error_1" ), //$NON-NLS-1$
								   ActionMessages.getString( "ToggleBreakpointRulerAction.Operation_failed_1" ), //$NON-NLS-1$
								   e.getStatus() );
		}
	}

	/**
	 * Returns this action's vertical ruler info.
	 *
	 * @return this action's vertical ruler
	 */
	protected IVerticalRulerInfo getVerticalRulerInfo() {
		return fRuler;
	}

	private IWorkbenchPart getTargetPart() {
		return this.fTargetPart;
	}

	private void setTargetPart( IWorkbenchPart targetPart ) {
		this.fTargetPart = targetPart;
		if (fTargetPart != null) {
			IResource resource = (IResource) fTargetPart.getAdapter(IResource.class);
			if (resource == null && fTargetPart instanceof IEditorPart) {
				resource = (IResource) ((IEditorPart)fTargetPart).getEditorInput().getAdapter(IResource.class);
			}
			if (resource != null) {
				fTargetAdapter = getAdapter(resource);
			}
			if (fTargetAdapter == null) {
				fTargetAdapter = getAdapter(fTargetPart);
			}
		}
		if (fTargetAdapter == null) {
			fTargetAdapter = new ToggleBreakpointAdapter();
		}
	}

	private IToggleBreakpointsTarget getAdapter(IAdaptable adaptable) {
		IToggleBreakpointsTarget adapter= (IToggleBreakpointsTarget)adaptable.getAdapter(IToggleBreakpointsTarget.class);
		if (adapter == null) {
			IAdapterManager adapterManager= Platform.getAdapterManager();
			if (adapterManager.hasAdapter(adaptable, IToggleBreakpointsTarget.class.getName())) { 
				adapter= (IToggleBreakpointsTarget)adapterManager.loadAdapter(adaptable, IToggleBreakpointsTarget.class.getName()); 
			}
		}
		return adapter;
	}

	/**
	 * Returns the current selection in the active part, possibly
	 * and empty selection, but never <code>null</code>.
	 * 
	 * @return the selection in the active part, possibly empty
	 */
	private ISelection getTargetSelection() {
		IDocument doc = getDocument();
		if ( doc != null ) {
			int line = getVerticalRulerInfo().getLineOfLastMouseButtonActivity();
			try {
				IRegion region = doc.getLineInformation( line );
				return new TextSelection( doc, region.getOffset(), region.getLength() );
			}
			catch( BadLocationException e ) {
				DebugPlugin.log( e );
			} 
		}
		return EMPTY_SELECTION;
	}

	private IDocument getDocument() {
		IWorkbenchPart targetPart = getTargetPart();
		if ( targetPart instanceof ITextEditor ) {
			ITextEditor textEditor = (ITextEditor)targetPart; 
			IDocumentProvider provider = textEditor.getDocumentProvider();
			if ( provider != null )
				return provider.getDocument( textEditor.getEditorInput() );
		}
		else if ( targetPart instanceof DisassemblyView ) {
			DisassemblyView dv = (DisassemblyView)targetPart;
			IDocumentProvider provider = dv.getDocumentProvider();
			if ( provider != null )
				return provider.getDocument( dv.getInput() );
		}
		return null;
	}
}
