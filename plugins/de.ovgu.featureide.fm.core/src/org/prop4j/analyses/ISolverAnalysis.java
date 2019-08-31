/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2019  FeatureIDE team, University of Magdeburg, Germany
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
package org.prop4j.analyses;

import org.prop4j.solver.ISolver;

import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

/**
 * Interface given to specify a class to be an analysis. The generic type <T> indicates the object hat should be returned after the analysis is done. So the
 * generic type represents the result for an analysis.
 *
 * @author Joshua Sprey
 */
public interface ISolverAnalysis<T> {

	/**
	 * Determine what should happen in the analysis. Also gives back the result.
	 *
	 * @return Result of the analysis.
	 */
	T analyze(IMonitor monitor);

	/**
	 * Returns the solver that is used to solve the analysis.
	 *
	 * @return Solver
	 */
	ISolver getSolver();
}