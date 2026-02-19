package com.back.global.pgroonga.config

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
 * PGroonga `&@~` 연산자를 Hibernate function으로 등록.
 * QueryDSL에서: Expressions.booleanTemplate("function('pgroonga_match', {0}, {1}) = true", col, kw)
 * 렌더링 결과: (cast(col as text) &@~ ?)
 */
class PGroongaMatchFunction(booleanType: BasicType<Boolean>) :
    AbstractSqmSelfRenderingFunctionDescriptor(
        "pgroonga_match",
        FunctionKind.NORMAL,
        StandardArgumentsValidators.exactly(2),
        StandardFunctionReturnTypeResolvers.invariant(booleanType),
        null,
    ) {

    override fun render(
        sqlAppender: SqlAppender,
        sqlAstArguments: List<SqlAstNode>,
        returnType: ReturnableType<*>?,
        walker: SqlAstTranslator<*>,
    ) {
        sqlAppender.appendSql("(cast(")
        sqlAstArguments[0].accept(walker)
        sqlAppender.appendSql(" as text) &@~ ")
        sqlAstArguments[1].accept(walker)
        sqlAppender.appendSql(")")
    }
}
