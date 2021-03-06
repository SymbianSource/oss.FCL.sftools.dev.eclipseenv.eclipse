/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     QnX Software System
 *     Anton Leherbauer (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.internal.corext.template.c;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateTranslator;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.PreferenceConstants;


/**
 * A context for C/C++
 */
public class CContext extends TranslationUnitContext {	

	/**
	 * Creates a C/C++ code template context.
	 * 
	 * @param type the context type
	 * @param document the document
	 * @param completionOffset the completion position within the document
	 * @param completionLength the length of the context
	 * @param translationUnit the translation unit represented by the document
	 */
	public CContext(TemplateContextType type, IDocument document, int completionOffset, int completionLength,
		ITranslationUnit translationUnit) {
		super(type, document, completionOffset, completionLength, translationUnit);
	}

	/**
	 * Creates a C/C++ code template context.
	 * 
	 * @param type the context type.
	 * @param document the document.
	 * @param completionPosition the completion position within the document
	 * @param translationUnit the translation unit (may be <code>null</code>).
	 */
	public CContext(TemplateContextType type, IDocument document,
			Position completionPosition, ITranslationUnit translationUnit) {
		super(type, document, completionPosition, translationUnit);
	}

	/*
	 * @see DocumentTemplateContext#getStart()
	 */ 
	@Override
	public int getStart() {
		if (fIsManaged && getCompletionLength() > 0)
			return super.getStart();
		
		try {
			IDocument document= getDocument();

			int start= getCompletionOffset();
			int end= getCompletionOffset() + getCompletionLength();
			
			while (start != 0 && !Character.isWhitespace(document.getChar(start - 1)))
				start--;
			
			while (start != end && Character.isWhitespace(document.getChar(start)))
				start++;
			
			if (start == end)
				start= getCompletionOffset();	
			
				return start;	

		} catch (BadLocationException e) {
			return super.getStart();	
		}
	}

	@Override
	public int getEnd() {
		if (fIsManaged || getCompletionLength() == 0)
			return super.getEnd();

		try {
			IDocument document= getDocument();

			int start= getCompletionOffset();
			int end= getCompletionOffset() + getCompletionLength();
			
			while (start != end && Character.isWhitespace(document.getChar(end - 1)))
				end--;
			
			return end;	

		} catch (BadLocationException e) {
			return super.getEnd();
		}		
	}
	
	/*
	 * @see TemplateContext#evaluate(Template)
	 */
	@Override
	public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {
		if (!canEvaluate(template))
			return null;
			
		TemplateTranslator translator= new TemplateTranslator();
		TemplateBuffer buffer= translator.translate(template.getPattern());

		getContextType().resolve(buffer, this);

		IPreferenceStore prefs= CUIPlugin.getDefault().getPreferenceStore();
		boolean useCodeFormatter= prefs.getBoolean(PreferenceConstants.TEMPLATES_USE_CODEFORMATTER);			

		ICProject project= getCProject();
		int indentationLevel = isReadOnly() ? 0 : getIndentationLevel();
		CFormatter formatter= new CFormatter(TextUtilities.getDefaultLineDelimiter(getDocument()), indentationLevel, useCodeFormatter, project);
		formatter.format(buffer, this);
		
		return buffer;
	}

}
