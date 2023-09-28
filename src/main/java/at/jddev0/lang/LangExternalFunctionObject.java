package at.jddev0.lang;

import java.util.List;

/**
 * Lang-Module<br>
 * Helper class for external Lang functions
 * 
 * @author JDDev0
 * @version v1.0.0
 */
@FunctionalInterface
@Deprecated
public interface LangExternalFunctionObject {
	DataObject callFunc(LangInterpreter interpreter, List<DataObject> argumentList, final int SCOPE_ID);
}