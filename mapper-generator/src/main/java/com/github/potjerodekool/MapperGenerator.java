package com.github.potjerodekool;

import com.sun.source.tree.MethodTree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TreeScanner;

public class MapperGenerator {

    public void finished(TaskEvent e) {
        if (e.getKind() != TaskEvent.Kind.PARSE) {
            return;
        }
        e.getCompilationUnit().accept(new TreeScanner<Void, Void>() {

            @Override
            public Void visitMethod(MethodTree node, Void aVoid) {
                return super.visitMethod(node, aVoid);
            }
        }, null);
    }

}
