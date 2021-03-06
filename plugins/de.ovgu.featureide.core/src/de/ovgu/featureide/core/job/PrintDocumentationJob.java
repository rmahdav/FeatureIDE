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
package de.ovgu.featureide.core.job;

import static de.ovgu.featureide.fm.core.localization.StringTable.BUILT_DOCUMENTATION;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.function.Predicate;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.prop4j.Literal;
import org.prop4j.Node;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.builder.IComposerExtensionClass.Mechanism;
import de.ovgu.featureide.core.signature.ProjectSignatures;
import de.ovgu.featureide.core.signature.ProjectSignatures.SignatureIterator;
import de.ovgu.featureide.core.signature.ProjectStructure;
import de.ovgu.featureide.core.signature.base.AbstractClassFragment;
import de.ovgu.featureide.core.signature.documentation.ContextMerger;
import de.ovgu.featureide.core.signature.documentation.FeatureModuleMerger;
import de.ovgu.featureide.core.signature.documentation.VariantMerger;
import de.ovgu.featureide.core.signature.documentation.base.ADocumentationCommentMerger;
import de.ovgu.featureide.core.signature.documentation.base.BlockTag;
import de.ovgu.featureide.core.signature.documentation.base.DocumentationBuilder;
import de.ovgu.featureide.core.signature.filter.ConstraintFilter;
import de.ovgu.featureide.core.signature.filter.FeatureFilter;
import de.ovgu.featureide.fm.core.FMCorePlugin;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.configuration.SelectableFeature;
import de.ovgu.featureide.fm.core.configuration.Selection;
import de.ovgu.featureide.fm.core.editing.AdvancedNodeCreator;
import de.ovgu.featureide.fm.core.editing.NodeCreator;
import de.ovgu.featureide.fm.core.io.EclipseFileSystem;
import de.ovgu.featureide.fm.core.io.FileSystem;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

/**
 * This job generates Javadoc from the feature-aware comments in a selected location. Subsequently it uses the Javadoc parser to generate the documentation.
 *
 * @author Sebastian Krieter
 */
public class PrintDocumentationJob implements LongRunningMethod<Boolean> {

	private final String foldername, featureName;
	private final String[] options;
	private final IProject project;

	private final ADocumentationCommentMerger merger;
	private IMonitor<Boolean> workMonitor;

	public PrintDocumentationJob(String foldername, String[] options, ADocumentationCommentMerger merger, String featureName, IProject project) {
		this.foldername = foldername;
		this.options = options;
		this.merger = merger;
		this.featureName = featureName;
		this.project = project;
	}

	@Override
	public Boolean execute(IMonitor<Boolean> workMonitor) throws Exception {
		this.workMonitor = workMonitor;
		final IFeatureProject featureProject = CorePlugin.getFeatureProject(project);
		if (featureProject == null) {
			CorePlugin.getDefault().logWarning(project.getName() + " is no FeatureIDE Project!");
		}

		if (!deleteOldFolder()) {
			return false;
		}

		final ProjectSignatures projectSignatures = featureProject.getProjectSignatures();
		if (projectSignatures == null) {
			CorePlugin.getDefault().logWarning("No signatures available!");
			return false;
		}

		final Collection<Predicate<BlockTag>> commentFilters = new LinkedList<>();
		final Collection<Predicate<?>> signatureFilters = new LinkedList<>();

		final int[] featureIDs = projectSignatures.getFeatureIDs();
		if (merger instanceof VariantMerger) {
			// TODO !!! ignore abstract features (below)
			final Configuration conf = featureProject.loadCurrentConfiguration();
			if (conf != null) {
				final Collection<String> featureNames = conf.getSelectedFeatureNames();
				final int[] tempFeatureList = new int[featureNames.size()];
				int count = 0;
				for (final String featureName : featureNames) {
					final int id = projectSignatures.getFeatureID(featureName);
					if (id >= 0) {
						tempFeatureList[count++] = id;
					}
				}
				final int[] validFeatureIDs = new int[count];

				// sort feature list
				int c = 0;
				for (int j = 0; j < count; j++) {
					final int curId = tempFeatureList[j];
					for (int k = 0; k < featureIDs.length; k++) {
						if (curId == featureIDs[k]) {
							validFeatureIDs[c++] = curId;
							break;
						}
					}
				}

				final Node[] nodes = new Node[conf.getFeatures().size() + 1];
				nodes[0] = AdvancedNodeCreator.createCNF(conf.getFeatureModel());
				int i = 1;
				for (final SelectableFeature feature : conf.getFeatures()) {
					final Selection selection = feature.getSelection();
					nodes[i++] = selection == Selection.UNDEFINED ? new Literal(NodeCreator.varTrue)
						: new Literal(feature.getName(), feature.getSelection() == Selection.SELECTED);
				}
				signatureFilters.add(new ConstraintFilter(nodes));

				merger.setValidFeatureIDs(featureIDs.length, validFeatureIDs);
			}
		} else if (merger instanceof ContextMerger) {
			final Node[] nodes = new Node[2];
			nodes[0] = AdvancedNodeCreator.createCNF(projectSignatures.getFeatureModel());
			nodes[1] = new Literal(featureName, true);
			signatureFilters.add(new ConstraintFilter(nodes));

			merger.setValidFeatureIDs(featureIDs.length, featureIDs);
		} else if (merger instanceof FeatureModuleMerger) {
			final int index = projectSignatures.getFeatureID(featureName);
			merger.setValidFeatureIDs(featureIDs.length, new int[] { index });

			if (featureProject.getComposer().getGenerationMechanism() == Mechanism.PREPROCESSOR) {
				final Node[] nodes = new Node[2];
				nodes[0] = AdvancedNodeCreator.createCNF(projectSignatures.getFeatureModel());
				nodes[1] = new Literal(featureName, true);
				signatureFilters.add(new ConstraintFilter(nodes));

				commentFilters.add(new Predicate<BlockTag>() {

					@Override
					public boolean test(BlockTag object) {
						for (final String featureName : object.getConstraint().getContainedFeatures()) {
							if (featureName.equals(featureName)) {
								return true;
							}
						}
						;
						return false;
					}
				});
			} else {
				signatureFilters.add(new FeatureFilter(index));
			}
		}

		final DocumentationBuilder builder = new DocumentationBuilder(featureProject);
		builder.build(merger, commentFilters);

		buildJavaDoc(projectSignatures.iterator(signatureFilters));

		return true;
	}

	protected String folderPath = null;

	private boolean deleteOldFolder() {
		final IFolder folder = FMCorePlugin.createFolder(project, foldername);
		folderPath = folder.getLocation().toOSString();

		try {
			folder.delete(true, null);
		} catch (final CoreException e) {
			CorePlugin.getDefault().logError(e);
			return false;
		}
		return true;
	}

	private void buildJavaDoc(final SignatureIterator it) {
		final String extFoldername = foldername + "/src/";

		FMCorePlugin.createFolder(project, extFoldername);

		final HashSet<String> packageSet = new HashSet<String>();
		final LinkedList<String> classList = new LinkedList<String>();
		final ProjectStructure structure = new ProjectStructure(it);
		final String docOutput = folderPath + File.separator + "doc" + File.separator;
		final String srcOutput = folderPath + File.separator + "src" + File.separator;

		workMonitor.setRemainingWork(structure.getClasses().size() + 2);

		for (final AbstractClassFragment javaClass : structure.getClasses()) {
			final String packagename = javaClass.getSignature().getPackage();

			final String path = extFoldername + packagename.replace('.', '/');

			if (packagename.isEmpty()) {
				classList.add(srcOutput + javaClass.getSignature().getName() + ".java");
			} else {
				packageSet.add(packagename);
			}

			final IFolder folder = FMCorePlugin.createFolder(project, path);
			final IFile file = folder.getFile(javaClass.getSignature().getName() + ".java");
			try {
				FileSystem.write(EclipseFileSystem.getPath(file), javaClass.toString().getBytes(Charset.forName("UTF-8")));
			} catch (final IOException e) {
				CorePlugin.getDefault().logError(e);
			}
			workMonitor.worked();
		}
		final IFolder folder = FMCorePlugin.createFolder(project, foldername + "/doc/");

		final int defaultArguments = 4;
		int numDefaultArguments = defaultArguments;
		String[] args0 = null, args1 = null;

		if (!classList.isEmpty()) {
			args0 = new String[classList.size()];
			classList.toArray(args0);
			numDefaultArguments += args0.length;
		}
		if (!packageSet.isEmpty()) {
			if (args0 == null) {
				args0 = new String[packageSet.size()];
				packageSet.toArray(args0);
			} else {
				args1 = new String[packageSet.size()];
				packageSet.toArray(args1);
			}
			numDefaultArguments += packageSet.size();
		}

		final String[] javadocargs;
		if ((options != null) && (options.length > 0) && !options[0].isEmpty()) {
			javadocargs = new String[numDefaultArguments + options.length];
			System.arraycopy(options, 0, javadocargs, numDefaultArguments, options.length);
		} else {
			javadocargs = new String[numDefaultArguments];
		}
		javadocargs[0] = "-d";
		javadocargs[1] = docOutput;
		javadocargs[2] = "-sourcepath";
		javadocargs[3] = srcOutput;
		if (args0 != null) {
			System.arraycopy(args0, 0, javadocargs, defaultArguments, args0.length);
		}
		if (args1 != null) {
			System.arraycopy(args1, 0, javadocargs, defaultArguments + args0.length, args1.length);
		}

		com.sun.tools.javadoc.Main.execute(javadocargs);
		workMonitor.worked();

		try {
			folder.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (final CoreException e) {
			CorePlugin.getDefault().logError(e);
		}
		workMonitor.worked();

		CorePlugin.getDefault().logInfo(BUILT_DOCUMENTATION);
	}
}
