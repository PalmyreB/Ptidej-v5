package mlssad.codesmells.test;

import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Document;

import junit.framework.TestCase;
import mlssad.codesmells.detection.ICodeSmellDetection;
import mlssad.kernel.impl.MLSCodeSmell;
import mlssad.utils.CodeToXml;

public abstract class AbstractCodeSmellTestCase extends TestCase {

	protected static ICodeSmellDetection detector;
	protected static Set<MLSCodeSmell> expectedSmells;
	protected static String codeSmell;
	protected static String aPathC;
	protected static String aPathJava;
	protected final static String PATH_C_NO_CODE_SMELL = "../MLS SAD Tests/rsc/CodeSmellsC/src/noCodeSmell/NoCodeSmell.c";
	protected final static String PATH_JAVA_NO_CODE_SMELL = "../MLS SAD Tests/rsc/CodeSmellsJNI/src/noCodeSmell/NoCodeSmell.java";
	protected final Document noCodeSmellXml = CodeToXml.parse(PATH_C_NO_CODE_SMELL, PATH_JAVA_NO_CODE_SMELL);

	protected void setUp() throws Exception {
		super.setUp();
		aPathC = null;
		aPathJava = null;
	}

	public void testNoCodeSmell() {
		detector.detect(noCodeSmellXml);

//		for (MLSCodeSmell cs : detector.getCodeSmells())
//			System.out.println(cs);

		assertEquals(0, detector.getCodeSmells().size());
		assertEquals(new HashSet<MLSCodeSmell>(), detector.getCodeSmells());
	}

	public void testCodeSmells() {
		detector.detect(CodeToXml.parse(aPathC, aPathJava));

//		for (MLSCodeSmell cs : detector.getCodeSmells())
//			System.out.println(cs);

		assertEquals(expectedSmells.size(), detector.getCodeSmells().size());
		assertEquals(expectedSmells, detector.getCodeSmells());
	}
}
