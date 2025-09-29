package sh.adelessfox.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.CheckUtil;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;

import javax.lang.model.SourceVersion;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class NullabilityAnnotationsCheck extends AbstractCheck {
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
    public void visitToken(DetailAST ast) {
        switch (ast.getType()) {
            case TokenTypes.CTOR_DEF -> visitConstructorDef(ast);
            case TokenTypes.METHOD_DEF -> visitMethodDef(ast);
            case TokenTypes.VARIABLE_DEF -> visitVariableDef(ast);
            default -> throw new IllegalArgumentException("Unexpected token: " + ast.getType());
        }
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
        var annotations = getChildren(modifiers)
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
                log(origin, "A primitive type ''{0}'' is annotated with a nullability annotation.", type);
            }
        } else if (!annotated) {
            log(origin, "A reference type ''{0}'' is missing a nullability annotation.", type);
        }

        // TODO Check for mixed NotNull and Nullable annotations
        // TODO Log the actual annotation used on a primitive
        // TODO Make (qualified) annotation names configurable
    }

    private static boolean isAnnotated(List<String> annotations) {
        return annotations.contains("NotNull") || annotations.contains("Nullable");
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
