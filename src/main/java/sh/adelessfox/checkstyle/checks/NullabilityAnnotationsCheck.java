package sh.adelessfox.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.*;
import org.jspecify.annotations.*;

import javax.lang.model.*;
import java.util.*;
import java.util.stream.*;

@NullMarked
public final class NullabilityAnnotationsCheck extends AbstractCheck {
    /** Specify classes that should be treated as "Nonnull" annotations. */
    private final Set<String> nonnullClassNames = new HashSet<>();
    private final Set<String> nonnullShortClassNames = new HashSet<>();

    /** Specify classes that should be treated as "Nullable" annotations. */
    private final Set<String> nullableClassNames = new HashSet<>();
    private final Set<String> nullableShortClassNames = new HashSet<>();

    /** Specify whether local variables should be checked. */
    private boolean checkLocalVariables = true;

    @Override
    public int[] getDefaultTokens() {
        return getAcceptableTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[]{
            TokenTypes.IMPORT,
            TokenTypes.CTOR_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.VARIABLE_DEF,
        };
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[]{
            TokenTypes.IMPORT
        };
    }

    @Override
    public void init() {
        if (nonnullClassNames.isEmpty()) {
            throw new IllegalArgumentException(
                "Please specify 'nonnullClassNames' with supported notnull annotations");
        }
        if (nullableClassNames.isEmpty()) {
            throw new IllegalArgumentException(
                "Please specify 'nullableClassNames' with supported nullable annotations");
        }

        HashSet<String> retainedSet = new HashSet<>(nonnullClassNames.size());
        retainedSet.addAll(nonnullClassNames);
        retainedSet.retainAll(nullableClassNames);
        if (!retainedSet.isEmpty()) {
            throw new IllegalArgumentException("Conflicting nonnull and nullable class names: "
                + String.join(", ", retainedSet));
        }
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        nonnullShortClassNames.clear();
        nonnullShortClassNames.addAll(nonnullClassNames);

        nullableShortClassNames.clear();
        nullableShortClassNames.addAll(nullableClassNames);
    }

    @Override
    public void visitToken(DetailAST ast) {
        switch (ast.getType()) {
            case TokenTypes.CTOR_DEF -> visitConstructorDef(ast);
            case TokenTypes.METHOD_DEF -> visitMethodDef(ast);
            case TokenTypes.VARIABLE_DEF -> visitVariableDef(ast);
            case TokenTypes.IMPORT -> visitImport(ast);
            default -> throw new IllegalArgumentException("Unexpected token: " + ast.getType());
        }
    }

    public void setNonnullClassNames(String[] nonnullClassNames) {
        this.nonnullClassNames.clear();
        Collections.addAll(this.nonnullClassNames, nonnullClassNames);
    }

    public void setNullableClassNames(String[] nullableClassNames) {
        this.nullableClassNames.clear();
        Collections.addAll(this.nullableClassNames, nullableClassNames);
    }

    public void setCheckLocalVariables(boolean checkLocalVariables) {
        this.checkLocalVariables = checkLocalVariables;
    }

    private void visitConstructorDef(DetailAST ast) {
        visitParameters(getFirstToken(ast, TokenTypes.PARAMETERS));
    }

    private void visitMethodDef(DetailAST ast) {
        visitParameters(getFirstToken(ast, TokenTypes.PARAMETERS));
        validate(ast);
    }

    private void visitVariableDef(DetailAST ast) {
        if (!checkLocalVariables && isChildOf(ast, TokenTypes.METHOD_DEF)) {
            return;
        }
        validate(ast);
    }

    private void visitImport(DetailAST ast) {
        String name = getImportedTypeCanonicalName(ast);
        if (isStarImport(ast)) {
            extendClassNamesFromPackage(name, nonnullClassNames, nonnullShortClassNames);
            extendClassNamesFromPackage(name, nullableClassNames, nullableShortClassNames);
        } else {
            extendClassNamesFromCanonicalName(name, nonnullClassNames, nonnullShortClassNames);
            extendClassNamesFromCanonicalName(name, nullableClassNames, nullableShortClassNames);
        }
    }

    private void visitParameters(DetailAST parameters) {
        getChildren(parameters, TokenTypes.PARAMETER_DEF).forEach(this::validate);
    }

    private void validate(DetailAST ast) {
        var type = getFirstToken(ast, TokenTypes.TYPE);
        var modifiers = getFirstToken(ast, TokenTypes.MODIFIERS);
        validate(type, modifiers);
    }

    private void validate(DetailAST type, DetailAST modifiers) {
        var typeIdentifier = FullIdent.createFullIdent(type.getFirstChild()).getText();
        var annotationIdentifiers = getChildren(modifiers, TokenTypes.ANNOTATION)
            .map(child -> getFirstToken(child, TokenTypes.AT).getNextSibling())
            .map(ident -> FullIdent.createFullIdent(ident).getText())
            .toList();
        validate(type, typeIdentifier, annotationIdentifiers);
    }

    private void validate(DetailAST origin, String type, List<String> annotations) {
        Optional<Match> match = matchingClassName(annotations);
        if (SourceVersion.isKeyword(type)) {
            if (match.isPresent()) {
                log(origin, "nullability.annotated.primitive", type);
            }
        } else if (match.isEmpty()) {
            log(origin, "nullability.non.annotated.reference", type);
        }

        // TODO Check for mixed NotNull and Nullable annotations
        // TODO Log the actual annotation used on a primitive
    }

    private Optional<Match> matchingClassName(List<String> classNames) {
        for (String className : classNames) {
            Optional<Match> result = matchingClassName(className);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    private Optional<Match> matchingClassName(String className) {
        String shortName = className.substring(className.lastIndexOf('.') + 1);
        if (nonnullClassNames.contains(className) || nonnullShortClassNames.contains(shortName)) {
            return Optional.of(Match.NONNULL);
        } else if (nullableClassNames.contains(className) || nullableShortClassNames.contains(shortName)) {
            return Optional.of(Match.NULLABLE);
        } else {
            return Optional.empty();
        }
    }

    private static void extendClassNamesFromCanonicalName(
        String canonicalName,
        Set<String> classNames,
        Set<String> shortClassNames
    ) {
        if (classNames.contains(canonicalName)) {
            shortClassNames.add(canonicalName.substring(canonicalName.lastIndexOf('.') + 1));
        }
    }

    private static void extendClassNamesFromPackage(
        String packageName,
        Set<String> classNames,
        Set<String> shortClassNames
    ) {
        for (String name : classNames) {
            int index = name.lastIndexOf('.');
            if (name.substring(0, index).equals(packageName)) {
                shortClassNames.add(name.substring(index + 1));
            }
        }
    }

    private static boolean isStarImport(DetailAST importAst) {
        for (DetailAST toVisit = importAst; toVisit != null; ) {
            toVisit = getNextSubTreeNode(toVisit, importAst);
            if (toVisit != null && toVisit.getType() == TokenTypes.STAR) {
                return true;
            }
        }
        return false;
    }

    private static String getImportedTypeCanonicalName(DetailAST importAst) {
        StringBuilder canonicalNameBuilder = new StringBuilder(256);
        for (DetailAST toVisit = importAst; toVisit != null; ) {
            toVisit = getNextSubTreeNode(toVisit, importAst);
            if (toVisit != null && toVisit.getType() == TokenTypes.IDENT) {
                if (!canonicalNameBuilder.isEmpty()) {
                    canonicalNameBuilder.append('.');
                }
                canonicalNameBuilder.append(toVisit.getText());
            }
        }
        return canonicalNameBuilder.toString();
    }

    @Nullable
    private static DetailAST getNextSubTreeNode(DetailAST currentNodeAst, DetailAST subTreeRootAst) {
        DetailAST currentNode = currentNodeAst;
        DetailAST toVisitAst = currentNode.getFirstChild();
        while (toVisitAst == null) {
            toVisitAst = currentNode.getNextSibling();
            if (currentNode.getParent().equals(subTreeRootAst)) {
                break;
            }
            currentNode = currentNode.getParent();
        }
        return toVisitAst;
    }


    private Stream<DetailAST> getChildren(DetailAST ast, int type) {
        return getChildren(ast).filter(child -> child.getType() == type);
    }

    @NullUnmarked
    private Stream<DetailAST> getChildren(DetailAST ast) {
        return Stream.iterate(ast.getFirstChild(), Objects::nonNull, DetailAST::getNextSibling);
    }

    private DetailAST getFirstToken(DetailAST ast, int type) {
        return Objects.requireNonNull(ast.findFirstToken(type));
    }

    private boolean isChildOf(DetailAST ast, int type) {
        for (DetailAST c = ast.getParent(); c != null; c = c.getParent()) {
            if (c.getType() == type) {
                return true;
            }
        }
        return false;
    }

    private enum Match {
        NONNULL,
        NULLABLE
    }
}
