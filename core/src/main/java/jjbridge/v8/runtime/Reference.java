package jjbridge.v8.runtime;

import jjbridge.common.runtime.JSReference;
import jjbridge.common.value.JSType;

public class Reference implements JSReference {
    public final long handle;
    private final JSType nominalType;
    private final ReferenceTypeGetter typeGetter;
    private final EqualityChecker equalityChecker;

    protected Reference(long handle, JSType nominalType, ReferenceTypeGetter typeGetter, EqualityChecker equalityChecker) {
        this.handle = handle;
        this.nominalType = nominalType;
        this.typeGetter = typeGetter;
        this.equalityChecker = equalityChecker;
    }

    @Override
    public JSType getNominalType() {
        return this.nominalType;
    }

    @Override
    public JSType getActualType() {
        return this.typeGetter.getType(handle);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Reference)) return false;
        Reference other = (Reference) obj;
        return this.equalityChecker.checkAreEqual(this.handle, other.handle);
    }
}
