package sh.adelessfox.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.CheckUtil;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;

import javax.lang.model.SourceVersion;
import java.util.*;
import java.util.stream.Stream;

public final class NullabilityAnnotationsCheck extends AbstractCheck {
    private final Set<String> notNullNames = new HashSet<>();
    private final Set<String> nullableNames = new HashSet<>();

    @Override
    public int[] getDefaultTokens() {
        return new int[]{
            TokenTypes.CTOR_DEF,
            TokenTypes.METHOD_DEF
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[]{
            TokenTypes.CTOR_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.VARIABLE_DEF,
        };
    }

    @Override
    public int[] getRequiredTokens() {
        return CommonUtil.EMPTY_INT_ARRAY;
    }

    @Override
    public void init() {
        if (notNullNames.isEmpty()) {
            throw new IllegalArgumentException("Please specify 'notNullNames' with supported notnull annotations");
        }
        if (nullableNames.isEmpty()) {
            throw new IllegalArgumentException("Please specify 'nullableNames' with supported nullable annotations");
        }
    }

    @Override
    public void visitToken(DetailAST ast) {
        switch (ast.getType()) {
            case TokenTypes.CTOR_DEF -> visitConstructorDef(ast);
            case TokenTypes.METHOD_DEF -> visitMethodDef(ast);
            case TokenTypes.VARIABLE_DEF -> visitVariableDef(ast);
            default -> throw new IllegalArgumentException("Unexpected token: " + ast.getType());
        }
    }

    public void setNotNullNames(String[] notNullNames) {
        this.notNullNames.clear();
        Collections.addAll(this.notNullNames, notNullNames);
    }

    public void setNullableNames(String[] nullableNames) {
        this.nullableNames.clear();
        Collections.addAll(this.nullableNames, nullableNames);
    }

    private void visitConstructorDef(DetailAST ast) {
        visitParameters(getFirstToken(ast, TokenTypes.PARAMETERS));
    }

    private void visitMethodDef(DetailAST ast) {
        visitParameters(getFirstToken(ast, TokenTypes.PARAMETERS));
    }

    private void visitParameters(DetailAST parameters) {
        getChildren(parameters, TokenTypes.PARAMETER_DEF).forEach(this::validate);
    }

    private void visitVariableDef(DetailAST ast) {
        validate(ast);
    }

    private void validate(DetailAST ast) {
        var modifiers = getFirstToken(ast, TokenTypes.MODIFIERS);
        var type = getFirstToken(ast, TokenTypes.TYPE);
        var ident = getFirstToken(ast, TokenTypes.IDENT);
        validate(ast, modifiers, type, ident);
    }

    private void validate(DetailAST origin, DetailAST modifiers, DetailAST type, DetailAST identifier) {
        var annotations = getChildren(modifiers, TokenTypes.ANNOTATION)
            .map(child -> getFirstToken(child, TokenTypes.IDENT))
            .map(CheckUtil::extractQualifiedName)
            .toList();

        var qualifiedType = CheckUtil.extractQualifiedName(type.getFirstChild());
        var qualifiedName = identifier.getText();

        validate(origin, annotations, qualifiedType, qualifiedName);
    }

    private void validate(DetailAST origin, List<String> annotations, String type, String identifier) {
        boolean annotated = isAnnotated(annotations);
        if (SourceVersion.isKeyword(type)) {
            if (annotated) {
                log(origin, "nullability.annotated.primitive", type);
            }
        } else if (!annotated) {
            log(origin, "nullability.non.annotated.reference", type);
        }

        // TODO Check for mixed NotNull and Nullable annotations
        // TODO Log the actual annotation used on a primitive
        // TODO Make (qualified) annotation names configurable
    }

    private boolean isAnnotated(List<String> annotations) {
        for (String annotation : annotations) {
            if (notNullNames.contains(annotation) || nullableNames.contains(annotation)) {
                return true;
            }
        }
        return false;
    }

    private Stream<DetailAST> getChildren(DetailAST ast, int type) {
        return getChildren(ast).filter(child -> child.getType() == type);
    }

    private Stream<DetailAST> getChildren(DetailAST ast) {
        return Stream.iterate(ast.getFirstChild(), Objects::nonNull, DetailAST::getNextSibling);
    }

    private DetailAST getFirstToken(DetailAST ast, int type) {
        return Objects.requireNonNull(ast.findFirstToken(type));
    }
}
