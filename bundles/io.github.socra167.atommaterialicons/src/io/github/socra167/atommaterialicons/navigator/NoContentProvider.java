package io.github.socra167.atommaterialicons.navigator;

import org.eclipse.jface.viewers.ITreeContentProvider;

/**
 * The navigator content of this plug-in only provides labels, never children.
 */
public class NoContentProvider implements ITreeContentProvider {

	private static final Object[] NO_CHILDREN = new Object[0];

	@Override
	public Object[] getElements(Object inputElement) {
		return NO_CHILDREN;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return NO_CHILDREN;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return false;
	}
}
