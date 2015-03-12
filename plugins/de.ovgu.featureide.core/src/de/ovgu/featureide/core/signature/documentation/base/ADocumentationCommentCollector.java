/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2015  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.core.signature.documentation.base;

import java.util.Collections;
import java.util.List;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.signature.base.AbstractSignature;
import de.ovgu.featureide.core.signature.filter.IFilter;

/**
 * Abstract merger for modul-comment.
 * 
 * @author Sebastian Krieter
 */
public abstract class ADocumentationCommentCollector {
	
	protected final IFeatureProject featureProject;
	protected int[] featureList;
	
	public ADocumentationCommentCollector(IFeatureProject featureProject) {
		this.featureProject = featureProject;
	}

	public abstract List<SignatureCommentPair> collect(List<IFilter<AbstractSignature>> filter);
	
	public List<SignatureCommentPair> collect() {
		return collect(Collections.<IFilter<AbstractSignature>>emptyList());
	}
	
}