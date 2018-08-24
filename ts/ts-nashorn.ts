/// <reference path="../node_modules/nashorn/nashorn-builtins.d.ts" />

import VirtualFileManager = require( "nashorn/com/intellij/openapi/vfs/VirtualFileManager" );
import VfsUtil = require( "nashorn/com/intellij/openapi/vfs/VfsUtil" );
import JavaPsiFacade = require( "nashorn/com/intellij/psi/JavaPsiFacade" );
import GlobalSearchScope = require( "nashorn/com/intellij/psi/search/GlobalSearchScope" );
import ProjectAndLibrariesScope = require( "nashorn/com/intellij/psi/search/ProjectAndLibrariesScope" );
import PsiClassType = require( "nashorn/com/intellij/psi/PsiClassType" );
import PsiArrayType = require( "nashorn/com/intellij/psi/PsiArrayType" );
import PsiPrimitiveType = require( "nashorn/com/intellij/psi/PsiPrimitiveType" );
import PsiTypeParameter = require( "nashorn/com/intellij/psi/PsiTypeParameter" );
import PsiWildcardType = require( "nashorn/com/intellij/psi/PsiWildcardType" );

import File = require( "nashorn/java/io/File" );
import FileOutputStream = require( "nashorn/java/io/FileOutputStream" );
import OutputStreamWriter = require( "nashorn/java/io/OutputStreamWriter" );
import PrintWriter = require( "nashorn/java/io/PrintWriter" );
import StringWriter = require( "nashorn/java/io/StringWriter" );
import PsiClass = require("nashorn/com/intellij/psi/PsiClass");
import PluginContext = require("nashorn/cz/goce/idea/scripter/PluginContext");
import PsiFile = require("nashorn/com/intellij/psi/PsiFile");
import PsiParameter = require("nashorn/com/intellij/psi/PsiParameter");
import PsiModifierList = require("nashorn/com/intellij/psi/PsiModifierList");
import PsiType = require("nashorn/com/intellij/psi/PsiType");
import ConsoleLogger = require("nashorn/cz/goce/idea/scripter/ConsoleLogger");
import IDEAScripter = require("nashorn/cz/goce/idea/scripter/IDEAScripter");
import PsiPackage = require("nashorn/com/intellij/psi/PsiPackage");
import PsiMethod = require("nashorn/com/intellij/psi/PsiMethod");


interface ImportItem {
    alias: string;
    psiClass: PsiClass;
    alreadyImported?: boolean;
}

const PROPERTY_BLACKLIST = [
    "class", "private", "protected", "public", "import", "true", "false", "boolean", "number", "object", "string",
    "this", "return", "enum", "export", "const", "var", "let", "super", "if", "while", "for", "of", "in", "static"
];

class Imports {
    imports: { [qualifiedName: string]: ImportItem } = {};
    aliases: { [alias: string]: ImportItem } = {};

    constructor(currentPsiClass: PsiClass) {
        let currentClassImport = {
            alias: currentPsiClass.name,
            psiClass: currentPsiClass,
            alreadyImported: true
        };
        this.imports[currentPsiClass.qualifiedName] = currentClassImport;
        this.aliases[currentClassImport.alias] = currentClassImport;
    };

    add(psiClass: PsiClass): string {

        const containingClass = psiClass.containingClass;
        if (containingClass) {
            return this.add(containingClass) + "." + psiClass.name;
        }

        let imp = this.imports[psiClass.qualifiedName];
        if (imp) {
            return imp.alias;
        } else {
            let aliasCandidate = psiClass.name;
            let counter = 0;
            while (this.aliases[aliasCandidate]) {
                aliasCandidate = psiClass.name + "_" + ( counter++);
            }
            imp = {
                alias: aliasCandidate,
                psiClass: psiClass
            };
            this.imports[psiClass.qualifiedName] = imp;
            this.aliases[aliasCandidate] = imp;
            return aliasCandidate;
        }
    }

    forEach(callback: (ImportItem) => void) {
        for (let qualifiedName in this.imports) {
            if (this.imports.hasOwnProperty(qualifiedName)) {
                let imp = this.imports[qualifiedName];
                if (!imp.alreadyImported) {
                    callback(imp);
                }
            }
        }
    };

    print(printer: PrintWriter) {
        let hasImport = false;
        this.forEach(function (imp) {
            hasImport = true;
            printer.println("import " + imp.alias + " = require('nashorn/" + String(imp.psiClass.qualifiedName).replace(/\./g, "/") + "');");
        });

        if (hasImport) {
            printer.println();
        }
    }
}

export class Parser {
    private getterPattern = /^(get|is)([A-Z_].*)/;
    private cachedTypes: { [simpleName: string]: PsiClass };
    private searchScope: ProjectAndLibrariesScope;
    private javaPsiFacade: JavaPsiFacade;
    public overwrite: boolean = false;
    private processed: { [qualifiedName: string]: PsiClass } = {};

    constructor(private output: File, private context: PluginContext, private log: ConsoleLogger) {

        this.javaPsiFacade = JavaPsiFacade.getInstance(context.project);
        this.searchScope = new ProjectAndLibrariesScope(context.project);
        this.cachedTypes = {
            "Number": this.findClass("java.lang.Number"),
            "String": this.findClass("java.lang.String"),
            "Character": this.findClass("java.lang.Character"),
            "Boolean": this.findClass("java.lang.Boolean"),
            "Object": this.findClass("java.lang.Object"),
        };
    }

    findClass(qualifiedName: string): PsiClass {
        return this.javaPsiFacade.findClass(qualifiedName, this.searchScope);
    }

    findPackage(packageName: string): PsiPackage {
        return this.javaPsiFacade.findPackage(packageName);
    }

    private printToFile(file: File, callback: (PrintWriter) => void) {
        const dir = file.getParentFile();
        dir.mkdirs();
        const outputStream = new FileOutputStream(file);
        try {
            const writer = new OutputStreamWriter(outputStream, "UTF-8");
            try {
                const printer = new PrintWriter(writer);
                try {
                    callback(printer);
                } finally {
                    printer.close();
                }
            } finally {
                writer.close();
            }
        } finally {
            outputStream.close();
        }
    }

    private printToBuffer(callback: (PrintWriter) => void) {
        const writer = new StringWriter();
        const printer = new PrintWriter(writer);
        try {
            callback(printer);
        } finally {
            printer.close();
        }
        return writer.toString();
    }

    private printType(psiType: PsiType, printer: PrintWriter, imports: Imports) {
        if (psiType === null) {
            printer.print("any");
        } else if (psiType instanceof PsiWildcardType) {
            if (psiType.extends) {
                this.printType(psiType.getExtendsBound(), printer, imports);
            } else {
                printer.print("any");
            }

        } else if (psiType instanceof PsiClassType) {
            const psiClass = psiType.resolve();
            if (psiClass === null) {
                this.log.err.println("failed to resolve type: " + psiType);
                printer.print("any");
            } else if (psiClass === this.cachedTypes["String"] || psiClass === this.cachedTypes["Character"]) {
                printer.print("string");
            } else if (psiClass === this.cachedTypes["Number"] || psiClass.isInheritor(this.cachedTypes["Number"], true)) {
                printer.print("number");
            } else if (psiClass === this.cachedTypes["Boolean"]) {
                printer.print("boolean");
            } else if (psiClass instanceof PsiTypeParameter) {
                printer.print(psiClass.name);
            } else {
                this.printClassRef(psiClass, printer, imports);
                const typeParameters = psiType.parameters;
                if (typeParameters && typeParameters.length) {
                    printer.print("<");
                    this.printTypeList(typeParameters, printer, imports);
                    printer.print(">");
                }
            }
        } else if (psiType instanceof PsiArrayType) {
            this.printType(psiType.componentType, printer, imports);
            printer.print("[]");
        } else if (psiType instanceof PsiPrimitiveType) {
            const typeText = psiType.presentableText;
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
        } else {
            printer.print("any /* " + psiType.presentableText + " *" + "/");
            this.log.err.println("unsupported type found: " + psiType + " (" + psiType.getClass() + ")");
        }
    }

    private printTypeList(psiTypes: PsiType[], printer: PrintWriter, imports: Imports) {
        for (let i = 0; i < psiTypes.length; i++) {
            const psiType = psiTypes[i];
            if (i > 0) {
                printer.print(", ");
            }
            this.printType(psiType, printer, imports);
        }
    }

    private printClassRef(psiClass: PsiClass, printer: PrintWriter, imports: Imports) {
        const alias = imports.add(psiClass);
        printer.print(alias);
    }


    private transferModifiers(modifierList: PsiModifierList, printer: PrintWriter, allowedModifiers: string[]) {
        allowedModifiers.forEach(function (modifier) {
            if (modifierList.hasExplicitModifier(modifier)) {
                printer.print(modifier + " ");
            }
        });
    }

    private printTypeParameters(typeParameters: PsiTypeParameter[], printer: PrintWriter, imports: Imports) {
        printer.print("<");
        for (let i = 0; i < typeParameters.length; i++) {
            if (i > 0) {
                printer.print(", ");
            }

            const param = typeParameters[i];
            printer.print(param.name);

            const extendsListTypes = param.extendsListTypes;
            if (extendsListTypes && extendsListTypes.length) {
                printer.print(" extends ");
                this.printType(extendsListTypes[0], printer, imports);
            }

        }
        printer.print(">");
    }

    private printParameterList(psiParameters: PsiParameter[], printer: PrintWriter, imports: Imports) {
        for (let i = 0; i < psiParameters.length; i++) {
            if (i > 0) {
                printer.print(", ");
            }
            const parameter = psiParameters[i];
            const nameIdentifier = parameter.nameIdentifier;
            if (nameIdentifier) {
                printer.print(nameIdentifier.text + " : ");
            } else {
                printer.print("arg" + (i + 1) + " : ");
            }

            this.printType(parameter.type, printer, imports);
        }
    }

    private printClass(psiClass: PsiClass, printer: PrintWriter, imports: Imports, indent: string) {

        let functionalInterface: PsiMethod = null;
        if (psiClass.isInterface()) {
            for (let annotation of psiClass.modifierList.annotations) {
                if (annotation.qualifiedName == "java.lang.FunctionalInterface") {

                    for (let method of psiClass.methods) {
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
        } else {
            printer.print(indent);
        }

        printer.print("class ");

        printer.print(psiClass.name);

        if (functionalInterface) {
            printer.print("_interface");
        }

        if (psiClass.hasTypeParameters()) {
            this.printTypeParameters(psiClass.typeParameters, printer, imports)
        }

        const extendsList = Java.from(psiClass.extendsListTypes).concat(Java.from(psiClass.implementsListTypes));
        if (extendsList.length == 0 && psiClass !== this.cachedTypes["Object"]) {
            printer.print(" extends " + imports.add(this.cachedTypes["Object"]));
        }

        printer.println(" {");

        const existingFields = {
            staticFields: {},
            instanceFields: {},
            add: function (name: string, modifierList: PsiModifierList) {
                let container;
                if (modifierList.hasModifierProperty("static")) {
                    container = this.staticFields;
                } else {
                    container = this.instanceFields;
                }
                if (container[name]) {
                    return false;
                } else {
                    container[name] = true;
                    return true;
                }
            }
        };

        const fields = psiClass.fields;
        for (let i = 0; i < fields.length; i++) {
            const field = fields[i];
            if (field.modifierList.hasModifierProperty("public")) {
                const name = field.nameIdentifier.text;
                if (existingFields.add(name, field.modifierList)) {
                    printer.print(indent + "\t");
                    this.transferModifiers(field.modifierList, printer, ["static"]);
                    printer.print(name + " : ");
                    this.printType(field.type, printer, imports);
                    printer.println(";");
                }
            }
        }

        const methods = psiClass.methods;
        for (let i = 0; i < methods.length; i++) {
            const getter = methods[i];
            if (!getter.constructor
                && getter.modifierList.hasModifierProperty("public")
                && getter.parameterList.parametersCount == 0
            ) {
                const matches = this.getterPattern.exec(getter.name);
                if (matches) {
                    let name = matches[2];
                    let decapitalize = true;
                    if (name.length > 1) {
                        const secondLetter = name.charAt(1);
                        decapitalize = secondLetter != secondLetter.toUpperCase();
                    }
                    if (decapitalize) {
                        name = name.charAt(0).toLowerCase() + name.substr(1);
                    }

                    if (PROPERTY_BLACKLIST.indexOf(name) < 0
                        && existingFields.add(name, getter.modifierList)) {

                        printer.print(indent + "\t");
                        this.transferModifiers(getter.modifierList, printer, ["static"]);
                        printer.print(name + " : ");
                        this.printType(getter.returnType, printer, imports);
                        printer.println(";");
                    }
                }
            }
        }

        const constructors = psiClass.constructors;
        for (let i = 0; i < constructors.length; i++) {
            const constructor = constructors[i];
            if (constructor.modifierList.hasModifierProperty("public")) {
                printer.print(indent + "\t");
                printer.print("constructor");
                printer.print("(");
                this.printParameterList(constructor.parameterList.parameters, printer, imports);
                printer.println(");");
            }
        }

        for (let i = 0; i < methods.length; i++) {
            const method = methods[i];
            if (!method.constructor && method.findDeepestSuperMethod() == null) {
                printer.print(indent + "\t");
                this.transferModifiers(method.modifierList, printer, ["static"]);
                printer.print(method.name);
                if (method.hasTypeParameters()) {
                    this.printTypeParameters(method.typeParameters, printer, imports)
                }
                printer.print("(");
                this.printParameterList(method.parameterList.parameters, printer, imports);
                printer.print(") : ");
                this.printType(method.returnType, printer, imports);
                printer.println(";")
            }
        }

        printer.println(indent + "}");


        if (extendsList.length) {
            if (indent === "") {
                printer.print("\n\ndeclare ");
            } else {
                printer.print("\n\n" + indent);
            }
            printer.print("interface ");

            printer.print(psiClass.name);

            if (functionalInterface) {
                printer.print("_interface");
            }

            if (psiClass.hasTypeParameters()) {
                this.printTypeParameters(psiClass.typeParameters, printer, imports)
            }
            printer.print(" extends ");

            this.printTypeList(extendsList, printer, imports);
            printer.print(" {}");

        }

        if (functionalInterface) {
            if (indent === "") {
                printer.print("\n\ndeclare ");
            } else {
                printer.print("\n\n" + indent);
            }
            printer.print("type " + psiClass.name);
            if (psiClass.hasTypeParameters()) {
                this.printTypeParameters(psiClass.typeParameters, printer, imports)
            }
            printer.print(" = " + psiClass.name + "_interface");
            if (psiClass.hasTypeParameters()) {
                this.printTypeParameters(psiClass.typeParameters, printer, imports)
            }
            printer.print(" | ((");
            this.printParameterList(functionalInterface.parameterList.parameters, printer, imports);
            printer.print(")=>");
            this.printType(functionalInterface.returnType, printer, imports);
            printer.print(");");
        }

        const innerClasses = psiClass.innerClasses;
        if (innerClasses.length) {

            if (indent === "") {
                printer.print("\n\ndeclare ");
            } else {
                printer.print("\n\n" + indent);
            }
            printer.println("module " + psiClass.name + " {");
            for (let innerClass of innerClasses) {
                this.printClass(innerClass, printer, imports, "\t" + indent);
            }
            printer.println(indent + "}");

        }
    }

    parseClass(psiClass: PsiClass) {
        if (this.processed[psiClass.qualifiedName]) {
            return;
        }
        this.processed[psiClass.qualifiedName] = psiClass;

        let file = new File(this.output, String(psiClass.qualifiedName).replace(/\./g, "/") + ".d.ts");

        if (!this.overwrite && file.exists()) {
            return false;
        }
        file.hashCode();

        let imports = new Imports(psiClass);
        let self = this;
        let buffer = this.printToBuffer(function (printer) {
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

    }

    parsePackage(psiPackage: PsiPackage, includeSubPackages: boolean) {
        for (let psiClass of psiPackage.classes) {
            this.parseClass(psiClass);
        }
        if (includeSubPackages) {
            for (let subPackage of psiPackage.subPackages) {
                this.parsePackage(subPackage, includeSubPackages);
            }
        }
    }
}