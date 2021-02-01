package cc.quarkus.qcc.plugin.llvm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.Linkage;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.object.Data;
import cc.quarkus.qcc.object.DataDeclaration;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.object.FunctionDeclaration;
import cc.quarkus.qcc.object.ProgramModule;
import cc.quarkus.qcc.object.ProgramObject;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public class LLVMGenerator implements Consumer<CompilationContext>, ValueVisitor<CompilationContext, LLValue> {

    public void accept(final CompilationContext ctxt) {
        for (ProgramModule programModule : ctxt.getAllProgramModules()) {
            DefinedTypeDefinition def = programModule.getTypeDefinition();
            Path outputFile = ctxt.getOutputFile(def, "ll");
            final Module module = Module.newModule();
            final LLVMModuleNodeVisitor moduleVisitor = new LLVMModuleNodeVisitor(module, ctxt);
            final LLVMModuleDebugInfo debugInfo = new LLVMModuleDebugInfo(module, ctxt);

            for (Section section : programModule.sections()) {
                String sectionName = section.getName();
                for (ProgramObject item : section.contents()) {
                    String name = item.getName();
                    Linkage linkage = map(item.getLinkage());
                    if (item instanceof Function) {
                        ExecutableElement element = ((Function) item).getOriginalElement();
                        MethodBody body = ((Function) item).getBody();
                        boolean isExact = item == ctxt.getExactFunction(element);
                        if (body == null) {
                            ctxt.error("Function `%s` has no body", name);
                            continue;
                        }

                        BasicBlock entryBlock = body.getEntryBlock();
                        FunctionDefinition functionDefinition = module.define(name).linkage(linkage);
                        LLValue topSubprogram = null;

                        if (isExact) {
                            functionDefinition.meta("dbg", debugInfo.getDebugInfoForFunction(element).getSubprogram());
                        } else {
                            topSubprogram = debugInfo.createThunkSubprogram((Function) item).asRef();
                            functionDefinition.meta("dbg", topSubprogram);
                        }

                        LLVMNodeVisitor nodeVisitor = new LLVMNodeVisitor(ctxt, module, debugInfo, topSubprogram, moduleVisitor, Schedule.forMethod(entryBlock), ((Function) item), functionDefinition);
                        if (! sectionName.equals(CompilationContext.IMPLICIT_SECTION_NAME)) {
                            functionDefinition.section(sectionName);
                        }

                        nodeVisitor.execute();
                    } else if (item instanceof FunctionDeclaration) {
                        FunctionDeclaration fn = (FunctionDeclaration) item;
                        cc.quarkus.qcc.machine.llvm.Function decl = module.declare(name).linkage(linkage);
                        FunctionType fnType = fn.getType();
                        decl.returns(moduleVisitor.map(fnType.getReturnType()));
                        int cnt = fnType.getParameterCount();
                        for (int i = 0; i < cnt; i++) {
                            decl.param(moduleVisitor.map(fnType.getParameterType(i)));
                        }
                    } else if (item instanceof DataDeclaration) {
                        module.global(moduleVisitor.map(item.getType())).external().linkage(linkage).asGlobal(item.getName());
                    } else {
                        assert item instanceof Data;
                        Literal value = (Literal) ((Data) item).getValue();
                        module.global(moduleVisitor.map(item.getType())).value(moduleVisitor.map(value)).linkage(linkage).asGlobal(item.getName());
                    }
                }
            }
            try {
                Files.createDirectories(outputFile.getParent());
                try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                    module.writeTo(writer);
                }
            } catch (IOException e) {
                ctxt.error("Failed to write \"%s\": %s", outputFile, e.getMessage());
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException e2) {
                    ctxt.warning("Failed to clean \"%s\": %s", outputFile, e.getMessage());
                }
            }
            LLVMState llvmState = ctxt.computeAttachmentIfAbsent(LLVMState.KEY, LLVMState::new);
            llvmState.addModulePath(outputFile);
        }
    }

    Linkage map(cc.quarkus.qcc.object.Linkage linkage) {
        switch (linkage) {
            case COMMON: return Linkage.COMMON;
            case INTERNAL: return Linkage.INTERNAL;
            case PRIVATE: return Linkage.PRIVATE;
            case WEAK: return Linkage.EXTERN_WEAK;
            case EXTERNAL: return Linkage.EXTERNAL;
            default: throw Assert.impossibleSwitchCase(linkage);
        }
    }
}
