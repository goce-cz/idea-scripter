"use strict";
exports.__esModule = true;
var JavaPsiFacade = require("nashorn/com/intellij/psi/JavaPsiFacade");
var ProjectAndLibrariesScope = require("nashorn/com/intellij/psi/search/ProjectAndLibrariesScope");
var PsiClassType = require("nashorn/com/intellij/psi/PsiClassType");
var PsiArrayType = require("nashorn/com/intellij/psi/PsiArrayType");
var PsiPrimitiveType = require("nashorn/com/intellij/psi/PsiPrimitiveType");
var PsiTypeParameter = require("nashorn/com/intellij/psi/PsiTypeParameter");
var PsiWildcardType = require("nashorn/com/intellij/psi/PsiWildcardType");
var File = require("nashorn/java/io/File");
var FileOutputStream = require("nashorn/java/io/FileOutputStream");
var OutputStreamWriter = require("nashorn/java/io/OutputStreamWriter");
var PrintWriter = require("nashorn/java/io/PrintWriter");
var StringWriter = require("nashorn/java/io/StringWriter");
var PROPERTY_BLACKLIST = [
    "class", "private", "protected", "public", "import", "true", "false", "boolean", "number", "object", "string",
    "this", "return", "enum", "export", "const", "var", "let", "super", "if", "while", "for", "of", "in", "static"
];
var Imports = (function () {
    function Imports(currentPsiClass) {
        this.imports = {};
        this.aliases = {};
        var currentClassImport = {
            alias: currentPsiClass.name,
            psiClass: currentPsiClass,
            alreadyImported: true
        };
        this.imports[currentPsiClass.qualifiedName] = currentClassImport;
        this.aliases[currentClassImport.alias] = currentClassImport;
    }
    ;
    Imports.prototype.add = function (psiClass) {
        var containingClass = psiClass.containingClass;
        if (containingClass) {
            return this.add(containingClass) + "." + psiClass.name;
        }
        var imp = this.imports[psiClass.qualifiedName];
        if (imp) {
            return imp.alias;
        }
        else {
            var aliasCandidate = psiClass.name;
            var counter = 0;
            while (this.aliases[aliasCandidate]) {
                aliasCandidate = psiClass.name + "_" + (counter++);
            }
            imp = {
                alias: aliasCandidate,
                psiClass: psiClass
            };
            this.imports[psiClass.qualifiedName] = imp;
            this.aliases[aliasCandidate] = imp;
            return aliasCandidate;
        }
    };
    Imports.prototype.forEach = function (callback) {
        for (var qualifiedName in this.imports) {
            if (this.imports.hasOwnProperty(qualifiedName)) {
                var imp = this.imports[qualifiedName];
                if (!imp.alreadyImported) {
                    callback(imp);
                }
            }
        }
    };
    ;
    Imports.prototype.print = function (printer) {
        var hasImport = false;
        this.forEach(function (imp) {
            hasImport = true;
            printer.println("import " + imp.alias + " = require('nashorn/" + String(imp.psiClass.qualifiedName).replace(/\./g, "/") + "');");
        });
        if (hasImport) {
            printer.println();
        }
    };
    return Imports;
}());
var Parser = (function () {
    function Parser(output, context, log) {
        this.output = output;
        this.context = context;
        this.log = log;
        this.getterPattern = /^(get|is)([A-Z_].*)/;
        this.overwrite = false;
        this.processed = {};
        this.javaPsiFacade = JavaPsiFacade.getInstance(context.project);
        this.searchScope = new ProjectAndLibrariesScope(context.project);
        this.cachedTypes = {
            "Number": this.findClass("java.lang.Number"),
            "String": this.findClass("java.lang.String"),
            "Character": this.findClass("java.lang.Character"),
            "Boolean": this.findClass("java.lang.Boolean"),
            "Object": this.findClass("java.lang.Object")
        };
    }
    Parser.prototype.findClass = function (qualifiedName) {
        return this.javaPsiFacade.findClass(qualifiedName, this.searchScope);
    };
    Parser.prototype.findPackage = function (packageName) {
        return this.javaPsiFacade.findPackage(packageName);
    };
    Parser.prototype.printToFile = function (file, callback) {
        var dir = file.getParentFile();
        dir.mkdirs();
        var outputStream = new FileOutputStream(file);
        try {
            var writer = new OutputStreamWriter(outputStream, "UTF-8");
            try {
                var printer = new PrintWriter(writer);
                try {
                    callback(printer);
                }
                finally {
                    printer.close();
                }
            }
            finally {
                writer.close();
            }
        }
        finally {
            outputStream.close();
        }
    };
    Parser.prototype.printToBuffer = function (callback) {
        var writer = new StringWriter();
        var printer = new PrintWriter(writer);
        try {
            callback(printer);
        }
        finally {
            printer.close();
        }
        return writer.toString();
    };
    Parser.prototype.printType = function (psiType, printer, imports) {
        if (psiType === null) {
            printer.print("any");
        }
        else if (psiType instanceof PsiWildcardType) {
            if (psiType["extends"]) {
                this.printType(psiType.getExtendsBound(), printer, imports);
            }
            else {
                printer.print("any");
            }
        }
        else if (psiType instanceof PsiClassType) {
            var psiClass = psiType.resolve();
            if (psiClass === null) {
                this.log.err.println("failed to resolve type: " + psiType);
                printer.print("any");
            }
            else if (psiClass === this.cachedTypes["String"] || psiClass === this.cachedTypes["Character"]) {
                printer.print("string");
            }
            else if (psiClass === this.cachedTypes["Number"] || psiClass.isInheritor(this.cachedTypes["Number"], true)) {
                printer.print("number");
            }
            else if (psiClass === this.cachedTypes["Boolean"]) {
                printer.print("boolean");
            }
            else if (psiClass instanceof PsiTypeParameter) {
                printer.print(psiClass.name);
            }
            else {
                this.printClassRef(psiClass, printer, imports);
                var typeParameters = psiType.parameters;
                if (typeParameters && typeParameters.length) {
                    printer.print("<");
                    this.printTypeList(typeParameters, printer, imports);
                    printer.print(">");
                }
            }
        }
        else if (psiType instanceof PsiArrayType) {
            this.printType(psiType.componentType, printer, imports);
            printer.print("[]");
        }
        else if (psiType instanceof PsiPrimitiveType) {
            var typeText = psiType.presentableText;
            switch (typeText) {
                case "int":
                case "byte":
                case "short":
                case "long":
                case "float":
                case "double":
                    printer.print("number");
                    break;
                case "char":
                    printer.print("string");
                    break;
                default:
                    printer.print(typeText);
            }
        }
        else {
            printer.print("any /* " + psiType.presentableText + " *" + "/");
            this.log.err.println("unsupported type found: " + psiType + " (" + psiType.getClass() + ")");
        }
    };
    Parser.prototype.printTypeList = function (psiTypes, printer, imports) {
        for (var i = 0; i < psiTypes.length; i++) {
            var psiType = psiTypes[i];
            if (i > 0) {
                printer.print(", ");
            }
            this.printType(psiType, printer, imports);
        }
    };
    Parser.prototype.printClassRef = function (psiClass, printer, imports) {
        var alias = imports.add(psiClass);
        printer.print(alias);
    };
    Parser.prototype.transferModifiers = function (modifierList, printer, allowedModifiers) {
        allowedModifiers.forEach(function (modifier) {
            if (modifierList.hasExplicitModifier(modifier)) {
                printer.print(modifier + " ");
            }
        });
    };
    Parser.prototype.printTypeParameters = function (typeParameters, printer, imports) {
        printer.print("<");
        for (var i = 0; i < typeParameters.length; i++) {
            if (i > 0) {
                printer.print(", ");
            }
            var param = typeParameters[i];
            printer.print(param.name);
            var extendsListTypes = param.extendsListTypes;
            if (extendsListTypes && extendsListTypes.length) {
                printer.print(" extends ");
                this.printType(extendsListTypes[0], printer, imports);
            }
        }
        printer.print(">");
    };
    Parser.prototype.printParameterList = function (psiParameters, printer, imports) {
        for (var i = 0; i < psiParameters.length; i++) {
            if (i > 0) {
                printer.print(", ");
            }
            var parameter = psiParameters[i];
            var nameIdentifier = parameter.nameIdentifier;
            if (nameIdentifier) {
                printer.print(nameIdentifier.text + " : ");
            }
            else {
                printer.print("arg" + (i + 1) + " : ");
            }
            this.printType(parameter.type, printer, imports);
        }
    };
    Parser.prototype.printClass = function (psiClass, printer, imports, indent) {
        var functionalInterface = null;
        if (psiClass.isInterface()) {
            for (var _i = 0, _a = psiClass.modifierList.annotations; _i < _a.length; _i++) {
                var annotation = _a[_i];
                if (annotation.qualifiedName == "java.lang.FunctionalInterface") {
                    for (var _b = 0, _c = psiClass.methods; _b < _c.length; _b++) {
                        var method = _c[_b];
                        if (!method.modifierList.hasModifierProperty("static")) {
                            functionalInterface = method;
                            break;
                        }
                    }
                }
            }
        }
        if (indent === "") {
            printer.print("declare ");
        }
        else {
            printer.print(indent);
        }
        printer.print("class ");
        printer.print(psiClass.name);
        if (functionalInterface) {
            printer.print("_interface");
        }
        if (psiClass.hasTypeParameters()) {
            this.printTypeParameters(psiClass.typeParameters, printer, imports);
        }
        var extendsList = Java.from(psiClass.extendsListTypes).concat(Java.from(psiClass.implementsListTypes));
        if (extendsList.length == 0 && psiClass !== this.cachedTypes["Object"]) {
            printer.print(" extends " + imports.add(this.cachedTypes["Object"]));
        }
        printer.println(" {");
        var existingFields = {
            staticFields: {},
            instanceFields: {},
            add: function (name, modifierList) {
                var container;
                if (modifierList.hasModifierProperty("static")) {
                    container = this.staticFields;
                }
                else {
                    container = this.instanceFields;
                }
                if (container[name]) {
                    return false;
                }
                else {
                    container[name] = true;
                    return true;
                }
            }
        };
        var fields = psiClass.fields;
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            if (field.modifierList.hasModifierProperty("public")) {
                var name_1 = field.nameIdentifier.text;
                if (existingFields.add(name_1, field.modifierList)) {
                    printer.print(indent + "\t");
                    this.transferModifiers(field.modifierList, printer, ["static"]);
                    printer.print(name_1 + " : ");
                    this.printType(field.type, printer, imports);
                    printer.println(";");
                }
            }
        }
        var methods = psiClass.methods;
        for (var i = 0; i < methods.length; i++) {
            var getter = methods[i];
            if (!getter.constructor
                && getter.modifierList.hasModifierProperty("public")
                && getter.parameterList.parametersCount == 0) {
                var matches = this.getterPattern.exec(getter.name);
                if (matches) {
                    var name_2 = matches[2];
                    var decapitalize = true;
                    if (name_2.length > 1) {
                        var secondLetter = name_2.charAt(1);
                        decapitalize = secondLetter != secondLetter.toUpperCase();
                    }
                    if (decapitalize) {
                        name_2 = name_2.charAt(0).toLowerCase() + name_2.substr(1);
                    }
                    if (PROPERTY_BLACKLIST.indexOf(name_2) < 0
                        && existingFields.add(name_2, getter.modifierList)) {
                        printer.print(indent + "\t");
                        this.transferModifiers(getter.modifierList, printer, ["static"]);
                        printer.print(name_2 + " : ");
                        this.printType(getter.returnType, printer, imports);
                        printer.println(";");
                    }
                }
            }
        }
        var constructors = psiClass.constructors;
        for (var i = 0; i < constructors.length; i++) {
            var constructor = constructors[i];
            if (constructor.modifierList.hasModifierProperty("public")) {
                printer.print(indent + "\t");
                printer.print("constructor");
                printer.print("(");
                this.printParameterList(constructor.parameterList.parameters, printer, imports);
                printer.println(");");
            }
        }
        for (var i = 0; i < methods.length; i++) {
            var method = methods[i];
            if (!method.constructor && method.findDeepestSuperMethod() == null) {
                printer.print(indent + "\t");
                this.transferModifiers(method.modifierList, printer, ["static"]);
                printer.print(method.name);
                if (method.hasTypeParameters()) {
                    this.printTypeParameters(method.typeParameters, printer, imports);
                }
                printer.print("(");
                this.printParameterList(method.parameterList.parameters, printer, imports);
                printer.print(") : ");
                this.printType(method.returnType, printer, imports);
                printer.println(";");
            }
        }
        printer.println(indent + "}");
        if (extendsList.length) {
            if (indent === "") {
                printer.print("\n\ndeclare ");
            }
            else {
                printer.print("\n\n" + indent);
            }
            printer.print("interface ");
            printer.print(psiClass.name);
            if (functionalInterface) {
                printer.print("_interface");
            }
            if (psiClass.hasTypeParameters()) {
                this.printTypeParameters(psiClass.typeParameters, printer, imports);
            }
            printer.print(" extends ");
            this.printTypeList(extendsList, printer, imports);
            printer.print(" {}");
        }
        if (functionalInterface) {
            if (indent === "") {
                printer.print("\n\ndeclare ");
            }
            else {
                printer.print("\n\n" + indent);
            }
            printer.print("type " + psiClass.name);
            if (psiClass.hasTypeParameters()) {
                this.printTypeParameters(psiClass.typeParameters, printer, imports);
            }
            printer.print(" = " + psiClass.name + "_interface");
            if (psiClass.hasTypeParameters()) {
                this.printTypeParameters(psiClass.typeParameters, printer, imports);
            }
            printer.print(" | ((");
            this.printParameterList(functionalInterface.parameterList.parameters, printer, imports);
            printer.print(")=>");
            this.printType(functionalInterface.returnType, printer, imports);
            printer.print(");");
        }
        var innerClasses = psiClass.innerClasses;
        if (innerClasses.length) {
            if (indent === "") {
                printer.print("\n\ndeclare ");
            }
            else {
                printer.print("\n\n" + indent);
            }
            printer.println("module " + psiClass.name + " {");
            for (var _d = 0, innerClasses_1 = innerClasses; _d < innerClasses_1.length; _d++) {
                var innerClass = innerClasses_1[_d];
                this.printClass(innerClass, printer, imports, "\t" + indent);
            }
            printer.println(indent + "}");
        }
    };
    Parser.prototype.parseClass = function (psiClass) {
        if (this.processed[psiClass.qualifiedName]) {
            return;
        }
        this.processed[psiClass.qualifiedName] = psiClass;
        var file = new File(this.output, String(psiClass.qualifiedName).replace(/\./g, "/") + ".d.ts");
        if (!this.overwrite && file.exists()) {
            return false;
        }
        file.hashCode();
        var imports = new Imports(psiClass);
        var self = this;
        var buffer = this.printToBuffer(function (printer) {
            self.printClass(psiClass, printer, imports, "");
            printer.println();
            printer.println("export = " + psiClass.name);
        });
        this.printToFile(file, function (printer) {
            imports.print(printer);
            printer.print(buffer);
        });
        this.log.out.println(file);
        imports.forEach(function (imp) {
            self.parseClass(imp.psiClass);
        });
    };
    Parser.prototype.parsePackage = function (psiPackage, includeSubPackages) {
        for (var _i = 0, _a = psiPackage.classes; _i < _a.length; _i++) {
            var psiClass = _a[_i];
            this.parseClass(psiClass);
        }
        if (includeSubPackages) {
            for (var _b = 0, _c = psiPackage.subPackages; _b < _c.length; _b++) {
                var subPackage = _c[_b];
                this.parsePackage(subPackage, includeSubPackages);
            }
        }
    };
    return Parser;
}());
exports.Parser = Parser;
