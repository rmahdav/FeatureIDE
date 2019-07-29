/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
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
package org.prop4j.analyses.impl;

import java.util.HashMap;

import org.prop4j.analyses.AbstractSolverAnalysisFactory;
import org.prop4j.analyses.ISolverAnalysis;
import org.prop4j.analyses.impl.general.ConstraintsUnsatisfiableAnalysis;
import org.prop4j.analyses.impl.general.CoreDeadAnalysis;
import org.prop4j.analyses.impl.general.ImplicationAnalysis;
import org.prop4j.analyses.impl.general.IndeterminedAnalysis;
import org.prop4j.analyses.impl.general.RedundantConstraintAnalysis;
import org.prop4j.analyses.impl.general.TautologicalConstraintAnalysis;
import org.prop4j.analyses.impl.general.ValidAnalysis;
import org.prop4j.solver.AbstractSatSolver;
import org.prop4j.solver.AbstractSolverFactory;
import org.prop4j.solver.ISatProblem;
import org.prop4j.solver.ISolver;
import org.prop4j.solver.ISolverProblem;
import org.prop4j.solver.impl.SolverManager;

import de.ovgu.featureide.fm.core.FMCorePlugin;

/**
 * Default factory used to create analyses with the solver currently selected in the FeatureIDE preferences
 *
 * @author Joshua Sprey
 */
public class GeneralSolverAnalysisFactory extends AbstractSolverAnalysisFactory {

	private HashMap<String, Object> defaultConfiguration = new HashMap<String, Object>();
	private final AbstractSolverFactory factory;

	/**
	 *
	 */
	public GeneralSolverAnalysisFactory() {
		defaultConfiguration.put(AbstractSatSolver.CONFIG_TIMEOUT, 1000);
		defaultConfiguration.put(AbstractSatSolver.CONFIG_DB_SIMPLIFICATION_ALLOWED, true);
		defaultConfiguration.put(AbstractSatSolver.CONFIG_VERBOSE, false);
		factory = SolverManager.getSelectedFeatureAttributeSolverFactory();
		FMCorePlugin.getDefault().logInfo("Habe Factory: " + factory.getId());
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.analyses.AbstractSolverAnalysisFactory#getDefaultConfiguration()
	 */
	@Override
	public HashMap<String, Object> getDefaultConfiguration() {
		return defaultConfiguration;
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.analyses.AbstractSolverAnalysisFactory#setDefaultConfiguration(java.util.HashMap)
	 */
	@Override
	public void setDefaultConfiguration(HashMap<String, Object> map) {
		defaultConfiguration = map;
	}

	/*
	 * (non-Javadoc)
	 * @see org.prop4j.analyses.ISolverAnalysisFactory#getAnalysis(java.lang.Object, org.prop4j.solver.ISolverProblem)
	 */
	@Override
	public ISolverAnalysis<?> getAnalysis(Class<?> analysisClass, ISolverProblem problem) {
		if (analysisClass.equals(ValidAnalysis.class)) {
			return getValidAnalyis(problem);
		} else if (analysisClass.equals(CoreDeadAnalysis.class)) {
			return getCoreDeadAnalysis(problem);
		} else if (analysisClass.equals(ImplicationAnalysis.class)) {
			return getImplicationAnalysis(problem);
		} else if (analysisClass.equals(IndeterminedAnalysis.class)) {
			return getIndeterminedAnalysis(problem);
		} else if (analysisClass.equals(RedundantConstraintAnalysis.class)) {
			return getRedundantConstraintAnalysis(problem);
		} else if (analysisClass.equals(ConstraintsUnsatisfiableAnalysis.class)) {
			return getConstraintsUnsatisfiableAnaylsis(problem);// Start AAAAAAAAAAAAAAAAAAAAAAS
		} else if (analysisClass.equals(TautologicalConstraintAnalysis.class)) {
			return getConstraintsTautologyAnaylsis(problem);
		}
		return null;
	}

	private CoreDeadAnalysis getCoreDeadAnalysis(ISolverProblem problem) {
		if (problem instanceof ISatProblem) {
			final ISolver solver = factory.getSolver((ISatProblem) problem);
			solver.setConfiguration(defaultConfiguration);
			return new CoreDeadAnalysis(solver);
		} else {
			return null;
		}
	}

	private ValidAnalysis getValidAnalyis(ISolverProblem problem) {
		if (problem instanceof ISatProblem) {
			final ISolver solver = factory.getSolver((ISatProblem) problem);
			solver.setConfiguration(defaultConfiguration);
			return new ValidAnalysis(solver);
		} else {
			return null;
		}
	}

	private ImplicationAnalysis getImplicationAnalysis(ISolverProblem problem) {
		if (problem instanceof ISatProblem) {
			final ISolver solver = factory.getSolver((ISatProblem) problem);
			solver.setConfiguration(defaultConfiguration);
			return new ImplicationAnalysis(solver);
		} else {
			return null;
		}
	}

	private IndeterminedAnalysis getIndeterminedAnalysis(ISolverProblem problem) {
		if (problem instanceof ISatProblem) {
			final ISolver solver = factory.getSolver((ISatProblem) problem);
			solver.setConfiguration(defaultConfiguration);
			return new IndeterminedAnalysis(solver, null);
		} else {
			return null;
		}
	}

	private RedundantConstraintAnalysis getRedundantConstraintAnalysis(ISolverProblem problem) {
		if (problem instanceof ISatProblem) {
			final ISolver solver = factory.getSolver((ISatProblem) problem);
			solver.setConfiguration(defaultConfiguration);
			return new RedundantConstraintAnalysis(solver, this);
		} else {
			return null;
		}
	}

	private ConstraintsUnsatisfiableAnalysis getConstraintsUnsatisfiableAnaylsis(ISolverProblem problem) {
		if (problem instanceof ISatProblem) {
			final ISolver solver = factory.getSolver((ISatProblem) problem);
			solver.setConfiguration(defaultConfiguration);
			return new ConstraintsUnsatisfiableAnalysis(solver, this);
		} else {
			return null;
		}
	}

	private TautologicalConstraintAnalysis getConstraintsTautologyAnaylsis(ISolverProblem problem) {
		if (problem instanceof ISatProblem) {
			final ISolver solver = factory.getSolver((ISatProblem) problem);
			solver.setConfiguration(defaultConfiguration);
			return new TautologicalConstraintAnalysis(solver, this);
		} else {
			return null;
		}
	}
}