package com.back.global.pGroonga.config

import org.hibernate.metamodel.model.domain.ReturnableType
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor
import org.hibernate.query.sqm.function.FunctionKind
import org.hibernate.query.sqm.produce.function.ArgumentsValidator
import org.hibernate.query.sqm.produce.function.FunctionArgumentException
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers
import org.hibernate.query.sqm.tree.SqmTypedNode
import org.hibernate.sql.ast.SqlAstTranslator
import org.hibernate.sql.ast.spi.SqlAppender
import org.hibernate.sql.ast.tree.SqlAstNode
import org.hibernate.type.BasicType
import org.hibernate.type.BindingContext

/**
 * PGroonga 복합 표현식 인덱스를 사용하는 `ARRAY[col1::text, col2::text, ...] &@~ kw` 방식의 Hibernate function.
 *
 * 가변 인자: 마지막 인자가 검색 키워드, 나머지 앞 인자들이 대상 컬럼이다 (컬럼 1개 이상 + 키워드 = 최소 2개).
 *
 * 렌더링 결과:
 *   ARRAY[col1::text, col2::text, ...] &@~ ?
 *
 * 대응 인덱스: CREATE INDEX ... USING pgroonga ((ARRAY[col1::text, col2::text, ...]))
 *
 * QueryDSL에서:
 *   Expressions.booleanTemplate("function('pgroonga_match', {0}, {1}, {2}) = true", col1, col2, kw)
 */
class PGroongaCompositeMatchFunction(
    functionName: String,
    booleanType: BasicType<Boolean>,
) : AbstractSqmSelfRenderingFunctionDescriptor(
    functionName,
    FunctionKind.NORMAL,
    MinArgumentCountValidator(2),
    StandardFunctionReturnTypeResolvers.invariant(booleanType),
    null,
) {

    override fun render(
        sqlAppender: SqlAppender,
        sqlAstArguments: List<SqlAstNode>,
        returnType: ReturnableType<*>?,
        walker: SqlAstTranslator<*>,
    ) {
        val keywordArgIndex = sqlAstArguments.lastIndex

        sqlAppender.appendSql("ARRAY[")
        sqlAstArguments.dropLast(1).forEachIndexed { index, argument ->
            if (index > 0) {
                sqlAppender.appendSql(", ")
            }
            argument.accept(walker)
            sqlAppender.appendSql("::text")
        }
        sqlAppender.appendSql("] &@~ ")
        sqlAstArguments[keywordArgIndex].accept(walker)
    }

    private class MinArgumentCountValidator(
        private val minCount: Int,
    ) : ArgumentsValidator {
        override fun validate(
            arguments: List<SqmTypedNode<*>>,
            functionName: String,
            bindingContext: BindingContext,
        ) {
            if (arguments.size >= minCount) return

            throw FunctionArgumentException(
                "Function $functionName() requires at least $minCount arguments, but ${arguments.size} arguments given",
            )
        }
    }
}
