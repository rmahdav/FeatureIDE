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
package de.ovgu.featureide.fm.core.analysis.mig;

public class MarkVisitor implements Visitor<byte[]> {
	final byte[] markerArray;

	public MarkVisitor(int numberOfariables) {
		markerArray = new byte[numberOfariables];
	}

	@Override
	public VisitResult visitStrong(int curLiteral) {
		markerArray[Math.abs(curLiteral) - 1] = curLiteral < 0 ? ModalImplicationGraph.VALUE_0 : ModalImplicationGraph.VALUE_1;
		return VisitResult.Continue;
	}

	@Override
	public VisitResult visitWeak(int curLiteral) {
		markerArray[Math.abs(curLiteral) - 1] |= (curLiteral < 0 ? ModalImplicationGraph.VALUE_0Q : ModalImplicationGraph.VALUE_1Q);
		return VisitResult.Continue;
	}

	@Override
	public byte[] getResult() {
		return markerArray;
	}

}
