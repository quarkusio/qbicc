package cc.quarkus.qcc.graph.type;

public class StartValue implements Value<StartType>, IOSource, MemorySource {

    public StartValue(StartType type, Value<?>...arguments) {
        this.type = type;
        this.arguments = arguments;
        this.io = new IOValue();
        this.memory = new MemoryValue();
    }

    @Override
    public StartType getType() {
        return this.type;
    }

    public Value<?> getArgument(int index) {
        return this.arguments[index];
    }

    @Override
    public IOValue getIO() {
        return this.io;
    }

    @Override
    public MemoryValue getMemory() {
        return this.memory;
    }

    private final StartType type;
    private final Value<?>[] arguments;
    private final IOValue io;
    private final MemoryValue memory;
}
