package org.qbicc.graph;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A parameter to a basic block.
 */
public final class BlockParameter extends AbstractValue implements PinnedNode {
    private final ValueType type;
    private final boolean nullable;
    private final BlockLabel blockLabel;
    private final Slot slot;

    BlockParameter(Node callSite, ExecutableElement element, ValueType type, boolean nullable, BlockLabel blockLabel, Slot slot) {
        super(callSite, element, 0, -1);
        this.type = type;
        this.nullable = nullable;
        this.blockLabel = blockLabel;
        this.slot = slot;
    }

    @Override
    int calcHashCode() {
        return System.identityHashCode(this);
    }

    @Override
    String getNodeName() {
        return "BlockParameter";
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    @Override
    public ValueType getType() {
        return type;
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    @Override
    public BlockLabel getPinnedBlockLabel() {
        return blockLabel;
    }

    public Slot getSlot() {
        return slot;
    }

    /**
     * Get all of the possible non-parameter argument values for this parameter.
     *
     * @return the set of possible values (not {@code null})
     */
    public Set<Value> getPossibleValues() {
        LinkedHashSet<Value> possibleValues = new LinkedHashSet<>();
        getPossibleValues(possibleValues, new HashSet<>(), false);
        return possibleValues;
    }

    public Set<Value> getPossibleValuesIncludingParameters() {
        LinkedHashSet<Value> possibleValues = new LinkedHashSet<>();
        getPossibleValues(possibleValues, new HashSet<>(), true);
        return possibleValues;
    }

    private void getPossibleValues(Set<Value> current, Set<BlockParameter> visited, boolean includeParams) {
        if (visited.add(this)) {
            BasicBlock pinnedBlock = getPinnedBlock();
            Set<BasicBlock> incoming = pinnedBlock.getIncoming();
            if (incoming.isEmpty()) {
                // initial block; the parameter is an input to the function
                current.add(this);
            } else for (BasicBlock basicBlock : incoming) {
                if (basicBlock.isReachable()) {
                    Value value = basicBlock.getTerminator().getOutboundArgument(slot);
                    if (includeParams) {
                        current.add(value);
                    }
                    if (value instanceof BlockParameter bp) {
                        bp.getPossibleValues(current, visited, includeParams);
                    }
                }
            }
        }
    }

    public boolean possibleValuesAreNullable() {
        if (! nullable) {
            // avoid creating set
            return false;
        }
        return possibleValuesAreNullable(new HashSet<>());
    }

    private boolean possibleValuesAreNullable(final HashSet<BlockParameter> visited) {
        if (visited.add(this) && nullable) {
            BasicBlock pinnedBlock = getPinnedBlock();
            Set<BasicBlock> incoming = pinnedBlock.getIncoming();
            if (incoming.isEmpty()) {
                // initial block; the test above proves this value is nullable
                return true;
            } else for (BasicBlock basicBlock : incoming) {
                if (basicBlock.isReachable()) {
                    Value value = basicBlock.getTerminator().getOutboundArgument(slot);
                    if (value instanceof BlockParameter bp) {
                        if (bp.possibleValuesAreNullable(visited)) {
                            return true;
                        }
                        // else continue
                    } else if (value.isNullable()) {
                        return true;
                    }
                    // else continue
                }
            }
        }
        return false;
    }

    public StringBuilder appendQualifiedName(StringBuilder b) {
        return getPinnedBlock().toString(b).append('.').append(slot);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return appendQualifiedName(b.append('%'));
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public <T> long accept(ValueVisitorLong<T> visitor, T param) {
        return visitor.visit(param, this);
    }
}
