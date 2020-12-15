package cc.quarkus.qcc.machine.llvm.impl;

import java.util.List;

import cc.quarkus.qcc.machine.llvm.LLStruct;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public final class LLVM {

    private LLVM() {}

    public static Module newModule() {
        return new ModuleImpl();
    }

    public static final LLValue i1 = new SingleWord("i1");
    public static final LLValue i8 = new SingleWord("i8");
    public static final LLValue i16 = new SingleWord("i16");
    public static final LLValue i24 = new SingleWord("i24");
    public static final LLValue i32 = new SingleWord("i32");
    public static final LLValue i64 = new SingleWord("i64");
    public static final LLValue i128 = new SingleWord("i128");

    public static final LLValue float16 = new SingleWord("half");
    public static final LLValue float32 = new SingleWord("float");
    public static final LLValue float64 = new SingleWord("double");
    public static final LLValue float128 = new SingleWord("fp128");

    public static final LLValue void_ = new SingleWord("void");

    public static final LLValue ZERO = new IntConstant(0);

    public static final LLValue FALSE = new SingleWord("false");
    public static final LLValue TRUE = new SingleWord("true");

    public static final LLValue NULL = new SingleWord("null");

    public static LLValue ptrTo(LLValue type, int addrSpace) {
        return new PointerTo((AbstractValue) type, addrSpace);
    }

    public static LLValue array(int dimension, LLValue elementType) {
        return new ArrayOf(dimension, (AbstractValue) elementType);
    }

    public static LLValue vector(final boolean vscale, final int dimension, final LLValue elementType) {
        return new VectorOf(dimension, (AbstractValue) elementType, vscale);
    }

    public static LLValue intConstant(int val) {
        return new IntConstant(val);
    }

    public static LLValue intConstant(long val) {
        return new LongConstant(val);
    }

    public static LLValue floatConstant(float val) {
        return new FloatConstant(val);
    }

    public static LLValue floatConstant(double val) {
        return new DoubleConstant(val);
    }

    public static LLValue global(final String name) {
        return new NamedGlobalValueOf(name);
    }

    public static LLValue function(final LLValue returnType, final List<LLValue> argTypes) {
        return new FunctionType(returnType, argTypes);
    }

    public static LLStruct struct() {
        return new StructType();
    }
}
