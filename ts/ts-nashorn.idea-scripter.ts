import {Parser} from "./ts-nashorn";
import IDEAScripter = require("nashorn/cz/goce/idea/scripter/IDEAScripter");
import ConsoleLogger = require("nashorn/cz/goce/idea/scripter/ConsoleLogger");
import PluginContext = require("nashorn/cz/goce/idea/scripter/PluginContext");
import VfsUtil = require("nashorn/com/intellij/openapi/vfs/VfsUtil");
import File = require("nashorn/java/io/File");

const OUTPUT = new File("C:/Sphinx/IDEAInspector/idea-sources/node_modules/nashorn");

interface RootPackage {
    packageName: string;
    includeSubPackages?: boolean;
}

type Root = RootPackage | string;

const ROOTS: Root[] = [
    /*{packageName: "com.intellij.psi", includeSubPackages: false},

    {packageName: "com.intellij.psi.search", includeSubPackages: false},
    {packageName: "com.intellij.lang.ecmascript6", includeSubPackages: true},
    {packageName: "com.intellij.lang.javascript", includeSubPackages: true},
    {packageName: "com.intellij.lang.typescript", includeSubPackages: true},
    "com.intellij.openapi.vfs.VirtualFileManager",
    "com.intellij.openapi.vfs.VfsUtil",
    "cz.goce.idea.scripter.PluginContext",
    "cz.goce.idea.scripter.PsiIterator",
    "java.io.FileOutputStream",
    "java.io.OutputStreamWriter",
    "java.io.StringWriter",
    "com.intellij.openapi.application.ApplicationManager",
    "cz.goce.idea.scripter.ClosureWriteCommandAction"*/
    "com.intellij.util.AdapterProcessor"
// {packageName: "com.intellij.find", includeSubPackages: true}

];

function execute(context: PluginContext, log: ConsoleLogger) {

	const parser = new Parser(OUTPUT, context, log);
	// parser.overwrite = true;

	for (let i = 0; i < ROOTS.length; i++) {
		const root = ROOTS[i];
		if (typeof root == "string") {
			const psiClass = parser.findClass(root);
			if (psiClass) {
				parser.parseClass(psiClass);
			} else {
				log.err.println("root class not found: " + root);
			}
		} else {
			const psiPackage = parser.findPackage(root.packageName);
			if (psiPackage) {
				parser.parsePackage(psiPackage, root.includeSubPackages);
			} else {
				log.err.println("root package not found: " + root);
			}
		}
	}

	const virtualOutput = VfsUtil.findFileByIoFile(OUTPUT, true);
	virtualOutput.refresh(true, true);
}

execute;