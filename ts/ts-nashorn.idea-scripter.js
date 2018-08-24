"use strict";
exports.__esModule = true;
var ts_nashorn_1 = require("./ts-nashorn");
var VfsUtil = require("nashorn/com/intellij/openapi/vfs/VfsUtil");
var File = require("nashorn/java/io/File");
var OUTPUT = new File("C:/Sphinx/IDEAInspector/idea-sources/node_modules/nashorn");
var ROOTS = [
    "com.intellij.util.AdapterProcessor"
];
function execute(context, log) {
    var parser = new ts_nashorn_1.Parser(OUTPUT, context, log);
    for (var i = 0; i < ROOTS.length; i++) {
        var root = ROOTS[i];
        if (typeof root == "string") {
            var psiClass = parser.findClass(root);
            if (psiClass) {
                parser.parseClass(psiClass);
            }
            else {
                log.err.println("root class not found: " + root);
            }
        }
        else {
            var psiPackage = parser.findPackage(root.packageName);
            if (psiPackage) {
                parser.parsePackage(psiPackage, root.includeSubPackages);
            }
            else {
                log.err.println("root package not found: " + root);
            }
        }
    }
    var virtualOutput = VfsUtil.findFileByIoFile(OUTPUT, true);
    virtualOutput.refresh(true, true);
}
execute;
