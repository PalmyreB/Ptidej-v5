package mlssad.codesmells.detection.repository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import mlssad.codesmells.detection.AbstractCodeSmellDetection;
import mlssad.codesmells.detection.ICodeSmellDetection;
import mlssad.kernel.impl.MLSCodeSmell;
import mlssad.utils.PropertyGetter;

public class PassingExcessiveObjectsDetection extends AbstractCodeSmellDetection implements ICodeSmellDetection {

	public String getCodeSmellName() {
		return "PassingExcessiveObjectsDetection";
	}

	public void detect(final Document cXml, final Document javaXml) {
		// TODO If object used for something else than get, it is not a code smell to
		// pass it as a parameter

		int maxNbOfFields = PropertyGetter.getIntProp("PassingExcessiveObjects.MaxNbOfFields", 5);

		Set<String> types = new HashSet<>(Arrays.asList("ObjectField", "BooleanField", "ByteField", "CharField",
				"ShortField", "IntField", "LongField", "FloatField", "DoubleField"));

		String staticTemplate = "Static%s";
		String getTemplate = "Get%s";
		String setTemplate = "Set%s";

		Set<MLSCodeSmell> excessiveObjectsSet = new HashSet<>();
		XPath xPath = XPathFactory.newInstance().newXPath();
		String callTemplate = "//call/name/name[. = '%s']";
		String paramQuery = "parameter_list/parameter[position()>2]/decl[type/name = 'jobject']/name";
		String funcQuery = "//function";

		try {
			final XPathExpression FILEPATH_EXP = xPath.compile(FILEPATH_QUERY);

			String cFilePath = FILEPATH_EXP.evaluate(cXml);

			NodeList funcList = (NodeList) xPath.evaluate(funcQuery, cXml, XPathConstants.NODESET);
			int funcLength = funcList.getLength();
			// Analysis for each function
			for (int i = 0; i < funcLength; i++) {
				String funcName = xPath.evaluate("./name", funcList.item(i));
				NodeList paramList = (NodeList) xPath.evaluate(paramQuery, funcList.item(i), XPathConstants.NODESET);
				int paramLength = paramList.getLength();
				// Analysis for each parameter that is an object
				for (int j = 0; j < paramLength; j++) {
					int nbGet = 0;
					Iterator<String> it = types.iterator();
					while (it.hasNext()) {
						// TODO Refactor

						// If the function sets a field, then passing the object as an argument was
						// necessary and not a code smell
						String thisType = it.next();
						String setQuery = String.format(callTemplate, String.format(setTemplate, thisType));
						NodeList setList = (NodeList) xPath.evaluate(setQuery, cXml, XPathConstants.NODESET);
						if (setList.getLength() > 0) {
							break;
						}
						String setStaticQuery = String.format(callTemplate,
								String.format(setTemplate, String.format(staticTemplate, thisType)));
						NodeList setStaticList = (NodeList) xPath.evaluate(setStaticQuery, cXml,
								XPathConstants.NODESET);
						if (setStaticList.getLength() > 0) {
							break;
						}

						// Accesses to fields of the current object
						String getQuery = String.format(callTemplate, String.format(getTemplate, thisType));
						NodeList getList = (NodeList) xPath.evaluate(getQuery, cXml, XPathConstants.NODESET);
						nbGet += getList.getLength();
						String getStaticQuery = String.format(callTemplate,
								String.format(getTemplate, String.format(staticTemplate, thisType)));
						NodeList getStaticList = (NodeList) xPath.evaluate(getStaticQuery, cXml,
								XPathConstants.NODESET);
						nbGet += getStaticList.getLength();
					}

					// If there are many accesses, the code smell is justified:
					// better pass the object as a parameter than pass too many fields
					if (nbGet > 0 && nbGet < maxNbOfFields) {
						excessiveObjectsSet.add(new MLSCodeSmell(this.getCodeSmellName(),
								paramList.item(j).getTextContent(), funcName, "", "", cFilePath));
					}
				}
			}

			this.setSetOfSmells(excessiveObjectsSet);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
	}

}
