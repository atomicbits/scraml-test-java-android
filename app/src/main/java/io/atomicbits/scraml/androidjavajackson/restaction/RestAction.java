package io.atomicbits.scraml.androidjavajackson.restaction;

import io.atomicbits.raml10.dsl.androidjavajackson.Callback;

/**
 * Created by peter on 8/12/17.
 */

public abstract class RestAction {

    private Boolean succesful = null;

    public abstract <T> void call(Callback<T> callback);

    public abstract String getName();

    public void setSuccessful(Boolean successful) {
        this.succesful = successful;
    }

    public Boolean isSuccessful() {
        return this.succesful;
    }

}
