package com.back.global.pGroonga.config

import org.hibernate.metamodel.model.domain.ReturnableType
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor
import org.hibernate.query.sqm.function.FunctionKind
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers
import org.hibernate.sql.ast.SqlAstTranslator
import org.hibernate.sql.ast.spi.SqlAppender
import org.hibernate.sql.ast.tree.SqlAstNode
import org.hibernate.type.BasicType

/**
 * PGroonga 복합 표현식 인덱스를 사용하는 `ARRAY[col1::text, col2::text] &@~ kw` 방식의 Hibernate function.
 *
 * 렌더링 결과:
 *   ARRAY[col1::text, col2::text] &@~ ?
 *
 * 대응 인덱스: CREATE INDEX ... USING pgroonga ((ARRAY[col1::text, col2::text]))
 *
 * QueryDSL에서:
 *   Expressions.booleanTemplate("function('pgroonga_post_match', {0}, {1}, {2}) = true", col1, col2, kw)
 */
class PGroongaCompositeMatchFunction(
    functionName: String,
    booleanType: BasicType<Boolean>,
) : AbstractSqmSelfRenderingFunctionDescriptor(
    functionName,
    FunctionKind.NORMAL,
    StandardArgumentsValidators.exactly(3),
    StandardFunctionReturnTypeResolvers.invariant(booleanType),
    null,
) {

    override fun render(
        sqlAppender: SqlAppender,
        sqlAstArguments: List<SqlAstNode>,
        returnType: ReturnableType<*>?,
        walker: SqlAstTranslator<*>,
    ) {
        sqlAppender.appendSql("ARRAY[")
        sqlAstArguments[0].accept(walker)
        sqlAppender.appendSql("::text, ")
        sqlAstArguments[1].accept(walker)
        sqlAppender.appendSql("::text] &@~ ")
        sqlAstArguments[2].accept(walker)
    }
}
