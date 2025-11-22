package wtf.dettex.api.other.command.datatypes;

import wtf.dettex.api.other.command.exception.CommandException;

public interface IDatatypePost<T, O> extends IDatatype {
    T apply(IDatatypeContext datatypeContext, O original) throws CommandException;
}

