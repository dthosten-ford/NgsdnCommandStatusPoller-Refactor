package com.ford.ngsdnvehicle.commands;

import com.google.common.base.Optional;

public class SingleValueField<VALUE> {

//    @SerializedName("value")
    private VALUE value;

    public SingleValueField(VALUE value) {
        this.value = value;
    }

    public Optional<VALUE> getValue() {
        return Optional.fromNullable(value);
    }
}
