package mlssad.codesmells.detection.repository;

import java.util.HashSet;
import java.util.Set;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mlssad.codesmells.detection.AbstractCodeSmellDetection;
import mlssad.codesmells.detection.ICodeSmellDetection;
import mlssad.kernel.impl.MLSCodeSmell;

public class NotSecuringLibrariesDetection extends AbstractCodeSmellDetection implements ICodeSmellDetection {

	public void detect(final Document xml) {
		Set<MLSCodeSmell> notSecureLibraries = new HashSet<>();

		// TODO System.load and System.loadLibrary: only way to load a library?
		String loadQuery = "call[name = 'System.loadLibrary' or name = 'System.load']//argument";
		String secureQuery = "descendant::call[name = 'AccessController.doPrivileged']//" + loadQuery;

		try {
			final XPathExpression loadExpr = xPath.compile("descendant::" + loadQuery);
			final XPathExpression secureExpr = xPath.compile(secureQuery);

			NodeList javaList = (NodeList) JAVA_FILES_EXP.evaluate(xml, XPathConstants.NODESET);
			final int javaLength = javaList.getLength();

			for (int i = 0; i < javaLength; i++) {
				Node javaXml = javaList.item(i);
				final String javaFilePath = FILEPATH_EXP.evaluate(javaXml);

				NodeList loadList = (NodeList) loadExpr.evaluate(javaXml, XPathConstants.NODESET);
				NodeList secureList = (NodeList) secureExpr.evaluate(javaXml, XPathConstants.NODESET);
				final int loadLength = loadList.getLength();
				final int secureLength = secureList.getLength();

				// TODO Refactor the loops
				for (int j = 0; j < loadLength; j++) {
					Node thisNode = loadList.item(j);
					final String thisLibrary = thisNode.getTextContent();
					final String thisMethod = FUNC_EXP.evaluate(thisNode);
					final String thisClass = CLASS_EXP.evaluate(thisNode);
					final String thisPackage = PACKAGE_EXP.evaluate(thisNode);
					notSecureLibraries.add(new MLSCodeSmell(this.getCodeSmellName(), thisLibrary, thisMethod, thisClass,
							thisPackage, javaFilePath));
				}
				for (int j = 0; j < secureLength; j++) {
					Node thisNode = secureList.item(j);
					final String thisLibrary = thisNode.getTextContent();
					final String thisMethod = FUNC_EXP.evaluate(thisNode);
					final String thisClass = CLASS_EXP.evaluate(thisNode);
					final String thisPackage = PACKAGE_EXP.evaluate(thisNode);
					notSecureLibraries.remove(new MLSCodeSmell(this.getCodeSmellName(), thisLibrary, thisMethod,
							thisClass, thisPackage, javaFilePath));
				}
			}
			this.setSetOfSmells(notSecureLibraries);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
	}

}
