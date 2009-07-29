/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * 	   Sergey Prigogin (Google)
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.text.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.text.ICCompletionProposal;

import org.eclipse.cdt.internal.ui.CPluginImages;

public class MarkerResolutionProposal implements ICCompletionProposal {
	private IMarkerResolution fResolution;
	private IMarker fMarker;

	/**
	 * Constructor for MarkerResolutionProposal.
	 */
	public MarkerResolutionProposal(IMarkerResolution resolution, IMarker marker) {
		fResolution= resolution;
		fMarker= marker;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
	 */
	public void apply(IDocument document) {
		fResolution.run(fMarker);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		if (fResolution instanceof IMarkerResolution2) {
			return ((IMarkerResolution2) fResolution).getDescription();
		}
		if (fResolution instanceof ICCompletionProposal) {
			return ((ICCompletionProposal) fResolution).getAdditionalProposalInfo();
		}
		try {
			String problemDesc= (String) fMarker.getAttribute(IMarker.MESSAGE);
			return CorrectionMessages.bind(CorrectionMessages.MarkerResolutionProposal_additionaldesc,
					problemDesc);
		} catch (CoreException e) {
			CUIPlugin.log(e);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return fResolution.getLabel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
	 */
	public Image getImage() {
		if (fResolution instanceof IMarkerResolution2) {
			return ((IMarkerResolution2) fResolution).getImage();
		}
		if (fResolution instanceof ICCompletionProposal) {
			return ((ICCompletionProposal) fResolution).getImage();
		}
		return CPluginImages.get(CPluginImages.IMG_CORRECTION_CHANGE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.java.ICCompletionProposal#getRelevance()
	 */
	public int getRelevance() {
		if (fResolution instanceof ICCompletionProposal) {
			return ((ICCompletionProposal) fResolution).getRelevance();
		}
		return 10;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
	 */
	public Point getSelection(IDocument document) {
		if (fResolution instanceof ICCompletionProposal) {
			return ((ICCompletionProposal) fResolution).getSelection(document);
		}
		return null;
	}

	public String getIdString() {
		return getDisplayString();
	}
}
