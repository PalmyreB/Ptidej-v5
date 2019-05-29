package mlssad.codesmells.detection.repository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import mlssad.codesmells.detection.AbstractCodeSmellDetection;
import mlssad.codesmells.detection.ICodeSmellDetection;

public class NotCachingObjectsElementsDetection extends AbstractCodeSmellDetection implements ICodeSmellDetection {

	public String getName() {
		return "NotCachingObjectsElementsDetection";
	}

	public void detect(final Document cXml, final Document javaXml) {
		/*
		 * "An abundance of GetFieldID() and GetMethodID() calls � in particular, if the
		 * calls are for the same fields and methods � indicates that the fields and
		 * method are not being cached."
		 * https://www.ibm.com/developerworks/library/j-jni/index.html
		 */

		Set<String> methods = new HashSet<>(Arrays.asList("GetFieldID", "GetMethodID", " GetStaticMethodID"));
		Set<String> notCachedSet = new HashSet<String>();
		XPath xPath = XPathFactory.newInstance().newXPath();

		try {
			/*
			 * FIRST CASE An ID is looked up in a function that is called several times
			 */

			// Functions that are potentially called several times in the host language
			String nativeDeclQuery = "//function_decl[specifier='native']/name";
			String hostCallQuery = "//call//name[last()]";
			NodeList nativeDeclList = (NodeList) xPath.evaluate(nativeDeclQuery, javaXml, XPathConstants.NODESET);
			NodeList hostCallList = (NodeList) xPath.evaluate(hostCallQuery, javaXml, XPathConstants.NODESET);
			// TODO Add case of a loop
			Set<String> nativeDeclSet = new HashSet<String>();
			Set<String> hostCallSet = new HashSet<String>();
			Set<String> severalCallSet = new HashSet<String>();
			int nativeDeclLength = nativeDeclList.getLength();
			int hostCallLength = hostCallList.getLength();
			for (int i = 0; i < nativeDeclLength; i++)
				nativeDeclSet.add(nativeDeclList.item(i).getTextContent());
			for (int i = 0; i < hostCallLength; i++) {
				String thisNode = hostCallList.item(i).getTextContent();
				if (!hostCallSet.add(thisNode))
					severalCallSet.add(thisNode);
			}
			nativeDeclSet.retainAll(severalCallSet);

			// Native functions that look up an ID
			List<String> nativeSelectorList = new LinkedList<>();
			List<String> IDSelectorList = new LinkedList<>();
			for (String method : methods) {
				nativeSelectorList.add(String.format(".//call/name/name = '%s'", method));
				IDSelectorList.add(String.format("name/name = '%s'", method));
			}
			String nativeSelector = String.join(" or ", nativeSelectorList);
			String IDSelector = String.join(" or ", IDSelectorList);
			String nativeQuery = String.format("//function[(%s) and name != 'JNI_OnLoad']", nativeSelector);
			String IDQuery = String.format(".//call[%s]//argument_list/argument[position() = 3]", IDSelector);

			NodeList nativeList = (NodeList) xPath.evaluate(nativeQuery, cXml, XPathConstants.NODESET);
			for (int i = 0; i < nativeList.getLength(); i++) {
				// WARNING: This only keeps the part of the function name after the last
				// underscore ("_") in respect to JNI syntax. Therefore, it does not work for
				// functions with _ in their names. This should not happen if names are written
				// in lowerCamelCase.
				String funcLongName = xPath.evaluate("./name", nativeList.item(i));
				String[] partsOfName = funcLongName.split("_");
				String funcName = partsOfName[partsOfName.length - 1];

				if (nativeDeclSet.contains(funcName)) {
					NodeList IDs = (NodeList) xPath.evaluate(IDQuery, nativeList.item(i), XPathConstants.NODESET);
					for (int j = 0; j < IDs.getLength(); j++)
						notCachedSet.add(IDs.item(j).getTextContent());
				}
			}

			/*
			 * SECOND CASE Inside a function, a same ID is looked up at least twice
			 */
			// We consider that the necessary fields are cached in the function JNI_OnLoad,
			// called only once
			String funcQuery = "//function[name != 'JNI_OnLoad']";
			String callTemplate = ".//call[name/name = '%s']";
			String argsQuery = ".//argument_list";
			String nameQuery = ".//argument_list/argument[position() = 3]";
			NodeList funcList = (NodeList) xPath.evaluate(funcQuery, cXml, XPathConstants.NODESET);
			int funcLength = funcList.getLength();
			// Analysis for each function
			for (int i = 0; i < funcLength; i++) {

				// Analysis for each Get<>ID
				for (String method : methods) {
					Set<NodeList> args = new HashSet<>();
					String callQuery = String.format(callTemplate, method);
					NodeList callList = (NodeList) xPath.evaluate(callQuery, funcList.item(i), XPathConstants.NODESET);
					int callLength = callList.getLength();
					for (int j = 0; j < callLength; j++) {
						// Arguments should be treated and compared as NodeLists and not Strings, in
						// case they do not respect the same conventions (e.g. concerning spaces)
						NodeList theseArgs = (NodeList) xPath.evaluate(argsQuery, callList.item(j),
								XPathConstants.NODESET);
						if (this.setContainsNodeList(args, theseArgs))
							notCachedSet.add(xPath.evaluate(nameQuery, callList.item(j)));
						else
							args.add(theseArgs);

					}
				}

			}
			this.setSetOfSmells(notCachedSet);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
	}

	private boolean argListsEqual(NodeList nl1, NodeList nl2) {
		int l1 = nl1.getLength();
		int l2 = nl2.getLength();

		if (l1 != l2)
			return false;

		if (l1 == 1 && nl1.item(0).hasChildNodes() && nl2.item(0).hasChildNodes())
			return this.argListsEqual(nl1.item(0).getChildNodes(), nl2.item(0).getChildNodes());

		for (int i = 0; i < l1; i++) {
			String nn1 = nl1.item(i).getNodeName();
			String nn2 = nl2.item(i).getNodeName();
			boolean differentTypes = !nn1.equals(nn2);
			boolean differentContents = !nn1.equals("#text")
					&& !nl1.item(i).getTextContent().equals(nl2.item(i).getTextContent());
			if (differentTypes || differentContents)
				return false;
		}
		return true;
	}

	private boolean setContainsNodeList(Set<NodeList> hs, NodeList nl) {
		for (NodeList cur_nl : hs)
			if (this.argListsEqual(cur_nl, nl))
				return true;
		return false;
	}

}
