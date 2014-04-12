package com.google.minijoe.compiler.ast;

import com.google.minijoe.compiler.CompilerException;
import com.google.minijoe.compiler.visitor.Visitor;

public class AnnotationLiteral extends Expression {

    public Identifier anno;

    public AnnotationLiteral(Identifier anno) {
        this.anno = anno;
    }

    @Override
    public Expression visitExpression(Visitor visitor) throws CompilerException {
        return visitor.visit(this);
    }

}
