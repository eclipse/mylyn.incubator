/*******************************************************************************
 * Copyright (c) 2011 Tasktop Technologies.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.internal.sandbox.search.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylyn.sandbox.search.ui.SearchResult;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * @author David Green
 */
public class DesktopSearchWorkbenchAdapter implements IWorkbenchAdapter {

	private static DesktopSearchWorkbenchAdapter instance = new DesktopSearchWorkbenchAdapter();

	public ImageDescriptor getImageDescriptor(Object o) {
		if (o instanceof SearchResult) {
			SearchResult item = (SearchResult) o;

			IResource resource = item.getResource();

			final IEditorRegistry editorRegistry = PlatformUI.getWorkbench().getEditorRegistry();

			IContentType contentType = null;
			if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				try {
					final IContentDescription contentDescription = file.getContentDescription();
					if (contentDescription != null) {
						contentType = contentDescription.getContentType();
					}
				} catch (CoreException e) {
					// ignore
				}
			}

			ImageDescriptor image = editorRegistry.getImageDescriptor(item.getFile().getName(), contentType);
			if (image == null) {
				image = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE);
			}
			return image;
		}
		return null;
	}

	public String getLabel(Object o) {
		if (o instanceof SearchResult) {
			SearchResult item = (SearchResult) o;
			return item.getFile().getName();
		}
		return null;
	}

	public Object[] getChildren(Object o) {
		return new Object[0];
	}

	public Object getParent(Object o) {
		return null;
	}

	public static DesktopSearchWorkbenchAdapter instance() {
		return instance;
	}

}
