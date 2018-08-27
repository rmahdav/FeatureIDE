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
package de.ovgu.featureide.ui.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.ui.utils.AntennaEnum;
import de.ovgu.featureide.fm.ui.utils.MungeEnum;
import de.ovgu.featureide.ui.UIPlugin;

/**
 * Syntax completion for preprocessors
 *
 * @author Reimar Schr�ter
 * @author Marcus Pinnecke
 * @author Mohammed Khaled
 * @author Iris-Maria Banciu
 */
@SuppressWarnings("restriction")
public class AnnoCompletion implements IJavaCompletionProposalComputer {

	private static final Image FEATURE_ICON = UIPlugin.getImage("FeatureIconSmall.ico");

	private JavaPreprocessor currentPreprocessor = JavaPreprocessor.Unknown;
	private Status status = Status.ShowNothing;

	List<CompletionProposal> directivesCompletionProposalList = Collections.emptyList();

	private IFile file;
	private IFeatureProject featureProject;

	public AnnoCompletion() {

	}

	private void init() {
		file = ((IFileEditorInput) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getEditorInput()).getFile();
		featureProject = CorePlugin.getFeatureProject(file);
		status = Status.ShowNothing;
	}

	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext arg0, IProgressMonitor arg1) {
		return Collections.emptyList();
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public void sessionEnded() {}

	@Override
	public void sessionStarted() {
		init();
	}

	public List<CompletionProposal> getFeatureList(final IFeatureProject featureProject, final CharSequence prefix) {
		final Iterable<String> featureNames = FeatureUtils.getConcreteFeatureNames(featureProject.getFeatureModel());

		final LinkedList<CompletionProposal> completionProposalsList = createListOfCompletionProposals(prefix, featureNames);
		return completionProposalsList;
	}

	private LinkedList<CompletionProposal> createListOfCompletionProposals(final CharSequence prefix, final Iterable<String> list) {
		final LinkedList<CompletionProposal> completionProposalList = new LinkedList<CompletionProposal>();
		for (String string : list) {
			CompletionProposal proposal = null;
			proposal = CompletionProposal.create(CompletionProposal.LABEL_REF, prefix.length());
			proposal.setName((string).toCharArray());
			if (currentPreprocessor == JavaPreprocessor.Antenna) {
				string += " ";
			}
			proposal.setCompletion((string).toCharArray());

			if (string.startsWith(prefix.toString())) {
				completionProposalList.add(proposal);
			}
		}
		return completionProposalList;
	}

	private JavaContentAssistInvocationContext setCurrentContext(ContentAssistInvocationContext arg0) {

		JavaContentAssistInvocationContext context = null;
		if (arg0 instanceof JavaContentAssistInvocationContext) {
			context = (JavaContentAssistInvocationContext) arg0;
		}

		return context;
	}

	private String getFeatureNameFromCondition(String conditionLine) {
		final String featurename = conditionLine.trim().substring(conditionLine.indexOf('[') - 1, conditionLine.indexOf(']') - 2);

		return featurename;
	}

	private String getSuggestedFeatureName(JavaContentAssistInvocationContext context) throws BadLocationException {

		int counter = 0;
		final int line = context.getDocument().getLineOfOffset(context.getInvocationOffset());
		final int lineIndex = line;
		for (int i = lineIndex; i >= 0; i--) {
			final int offsetOfLine = context.getDocument().getLineOffset(i);
			final int lineLength = context.getDocument().getLineLength(i);
			final String lineContent = context.getDocument().get(offsetOfLine, lineLength);
			if ((i == lineIndex) && !lineContent.contains("/*end[")) {
				return null;

			}
			if (lineContent.contains("/*end[")) {
				counter++;
			}
			if (lineContent.contains("/*if[")) {
				if (counter > 1) {
					counter--;
					continue;
				}
				return getFeatureNameFromCondition(lineContent);

			}
		}
		return null;

	}

	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext arg0, IProgressMonitor arg1) {

		currentPreprocessor = JavaPreprocessor.getPreprocessor(featureProject);

		final JavaContentAssistInvocationContext context = setCurrentContext(arg0);

		if ((context == null) || (file == null) || (featureProject == null)) {
			return Collections.emptyList();
		}

		final Status status = computeList(context);

		if (status == Status.ShowNothing) {
			return Collections.emptyList();
		}

		CharSequence prefix = "";
		try {
			prefix = context.computeIdentifierPrefix();
		} catch (final BadLocationException e) {
			e.printStackTrace();
		}

		final List<CompletionProposal> featureCompletionProposalList = getFeatureList(featureProject, prefix);
		switch (currentPreprocessor) {
		case Antenna:
			directivesCompletionProposalList = getAntennaCompletionProposals(prefix);
			break;
		case Munge:
			directivesCompletionProposalList = getMungeCompletionProposals(prefix);
			break;
		default:
			break;
		}

		final ArrayList<ICompletionProposal> list = new ArrayList<ICompletionProposal>();
		if (status == Status.ShowFeatures) {
			final List<CompletionProposal> newFeatureCompletionProposalList = sortMungeProposalList(context, featureCompletionProposalList);

			for (final CompletionProposal proposal : newFeatureCompletionProposalList) {

				final LazyJavaCompletionProposal feature = createLazyJavaCompletionProposal(context, prefix, proposal);

				feature.setImage(FEATURE_ICON);

				setCursorPostionForMunge(proposal, feature);
				list.add(feature);

			}

		} else if (status == Status.ShowDirectives) {

			for (final CompletionProposal proposal : directivesCompletionProposalList) {
				final LazyJavaCompletionProposal syntax = createLazyJavaCompletionProposal(context, prefix, proposal);

				spawnCursorInbetweenSyntaxForMunge(syntax);

				list.add(syntax);

			}
		}

		return list;
	}

	/**
	 * @param context
	 * @param featureCompletionProposalList
	 * @return
	 */
	private List<CompletionProposal> sortMungeProposalList(final JavaContentAssistInvocationContext context,
			List<CompletionProposal> featureCompletionProposalList) {
		String suggestedFeatureName = null;
		final List<CompletionProposal> newfeatureCompletionProposalList = featureCompletionProposalList;
		if (currentPreprocessor == JavaPreprocessor.Munge) {

			try {
				suggestedFeatureName = getSuggestedFeatureName(context);
			} catch (final BadLocationException e) {
				// TODO Auto-generated catch block
				suggestedFeatureName = null;
			}

			if (suggestedFeatureName != null) {
				for (final CompletionProposal proposal : featureCompletionProposalList) {
					if (Arrays.equals(proposal.getName(), suggestedFeatureName.toCharArray())) {
						newfeatureCompletionProposalList.removeAll(newfeatureCompletionProposalList);
						newfeatureCompletionProposalList.add(proposal);
						Collections.swap(featureCompletionProposalList, 0, featureCompletionProposalList.indexOf(proposal));

						break;
					}

				}

			}

		}
		return newfeatureCompletionProposalList;

	}

	/**
	 * @param proposal
	 * @param feature
	 */
	private void setCursorPostionForMunge(final CompletionProposal proposal, final LazyJavaCompletionProposal feature) {
		if (currentPreprocessor == JavaPreprocessor.Munge) {

			feature.setCursorPosition(proposal.getName().length + 3);

		}
	}

	/**
	 * @param context
	 * @param prefix
	 * @param proposal
	 * @return
	 */
	private LazyJavaCompletionProposal createLazyJavaCompletionProposal(final JavaContentAssistInvocationContext context, CharSequence prefix,
			final CompletionProposal proposal) {
		final LazyJavaCompletionProposal lazyJavaCompletionProposal = new LazyJavaCompletionProposal(proposal, context);

		lazyJavaCompletionProposal.setReplacementString(new String(proposal.getCompletion()).replace(prefix, ""));
		lazyJavaCompletionProposal.setReplacementOffset(context.getInvocationOffset());

		return lazyJavaCompletionProposal;
	}

	/**
	 * @param syntax
	 */
	private void spawnCursorInbetweenSyntaxForMunge(final LazyJavaCompletionProposal syntax) {
		if (currentPreprocessor == JavaPreprocessor.Munge) {
			syntax.setCursorPosition(syntax.toString().length() - 3);
		}
	}

	private boolean lineContainsElements(String lineContent, List<String> list) {

		for (final String div : list) {
			if (lineContent.contains(div)) {
				return true;
			}
		}
		return false;
	}

	private String findLastKeyword(String context) {
		final String text = context.trim();
		final int indexofKeyword = text.lastIndexOf(" ");
		if (indexofKeyword < 0) {
			return text;
		}
		return text.substring(indexofKeyword).trim();
	}

	private Status computeList(JavaContentAssistInvocationContext context) {
		try {
			final int line = context.getDocument().getLineOfOffset(context.getInvocationOffset());
			final int offsetOfLine = context.getDocument().getLineOffset(line);
			final int lineLength = context.getDocument().getLineLength(line);
			final String lineContent = context.getDocument().get(offsetOfLine, lineLength);
			final String lastKeyword = findLastKeyword(lineContent);

			getStatusAccordingToPreprocessor(lineContent, lastKeyword);
		}

		catch (final BadLocationException e1) {
			e1.printStackTrace();
		}
		return status;
	}

	private void getStatusAccordingToPreprocessor(final String lineContent, final String lastKeyword) {
		switch (currentPreprocessor) {
		case Antenna:
			status = getStatusForAntenna(lineContent, lastKeyword);
			break;
		case Munge:
			status = getStatusForMunge(lineContent, lastKeyword);
			break;
		default:
			break;
		}
	}

	private Status getStatusForAntenna(final String lineContent, final String lastKeyword) {
		final boolean triggerAutocomplete = lineContent.trim().substring(2).trim().contains("#");
		final boolean hasDirective = lineContainsElements(lineContent, AntennaEnum.getAllDirectives());
		final boolean directiveHasCondition = lineContainsElements(lineContent, Arrays.asList("#if", "#elif", "#condition"));
		final boolean hasFeature = lineContainsElements(lineContent, (List<String>) FeatureUtils.getConcreteFeatureNames(featureProject.getFeatureModel()));
		final boolean acceptsNewDirective = (lastKeyword.contains("&&") || lastKeyword.contains("||"));

		if (triggerAutocomplete && !hasDirective) {
			status = Status.ShowDirectives;
		} else {
			status = Status.ShowNothing;
		}
		if ((directiveHasCondition && !hasFeature) || (hasFeature && hasDirective && acceptsNewDirective)) {
			status = Status.ShowFeatures;
		}
		return status;
	}

	private Status getStatusForMunge(String lineContent, String lastKeyword) {
		final boolean hasOpeningSyntax = lineContent.trim().contains("/*");
		final boolean hasFeature = lineContainsElements(lineContent, (List<String>) FeatureUtils.getConcreteFeatureNames(featureProject.getFeatureModel()));
		final boolean hasDirective = lineContainsElements(lineContent, Arrays.asList("if", "if_not", "else", "end"));
		if ((hasOpeningSyntax && !hasDirective)) {
			status = Status.ShowDirectives;
		}
		if (hasDirective && !hasFeature) {
			status = Status.ShowFeatures;
		}
		return status;
	}

	private List<CompletionProposal> getAntennaCompletionProposals(final CharSequence prefix) {
		final LinkedList<CompletionProposal> completionProposalList = createListOfCompletionProposals(prefix, AntennaEnum.getAllDirectives());
		return completionProposalList;
	}

	private List<CompletionProposal> getMungeCompletionProposals(final CharSequence prefix) {
		final LinkedList<CompletionProposal> completionProposalList = createListOfCompletionProposals(prefix, MungeEnum.getAllDirectives());

		return completionProposalList;
	}

	private enum Status {
		ShowFeatures, ShowDirectives, ShowNothing
	}

	private enum JavaPreprocessor {
		Antenna, Munge, Unknown;

		public static JavaPreprocessor getPreprocessor(final IFeatureProject featureProject) {
			switch (featureProject.getComposer().getName()) {
			case "Antenna":
				return JavaPreprocessor.Antenna;
			case "Munge":
				return JavaPreprocessor.Munge;
			default:
				return JavaPreprocessor.Unknown;
			}
		}
	}
}
