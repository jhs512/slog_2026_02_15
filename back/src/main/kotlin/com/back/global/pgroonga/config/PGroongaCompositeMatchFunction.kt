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
 * PGroonga 복합 인덱스를 사용하는 `text[] &@~ pgroonga_full_text_search_condition` 방식의 Hibernate function.
 *
 * 렌더링 결과:
 *   ARRAY[col1, col2] &@~ (?, NULL, 'indexName')::pgroonga_full_text_search_condition
 *
 * `(col1, col2)` ROW 형식은 PGroonga가 record 타입 연산자를 등록하지 않아 동작하지 않음.
 * `ARRAY[col1, col2]` 형식은 text[] &@~ pgroonga_full_text_search_condition 연산자를 사용하여 정상 동작.
 *
 * 이 방식의 장점:
 * - 단일 인덱스 스캔 (BitmapOr 없이)
 * - PGroonga 네이티브 쿼리 문법 완전 지원 (AND/OR/NOT/prefix/구문검색 등)
 * - NOT(-) 이 title·content 전체에 전역 적용됨
 *
 * indexName 은 등록 시점에 함수명으로 식별되므로 인스턴스별로 하드코딩됨.
 * CustomPostgreSQLDialect 에서 함수명과 인덱스명을 매핑하여 등록한다.
 *
 * QueryDSL에서:
 *   Expressions.booleanTemplate("function('pgroonga_post_match', {0}, {1}, {2}) = true", col1, col2, kw)
 */
class PGroongaCompositeMatchFunction(
    functionName: String,
    private val indexName: String,
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
        sqlAstArguments[0].accept(walker)   // col1
        sqlAppender.appendSql(", ")
        sqlAstArguments[1].accept(walker)   // col2
        sqlAppender.appendSql("] &@~ (")
        sqlAstArguments[2].accept(walker)   // kw (바인딩 파라미터로 렌더링됨)
        sqlAppender.appendSql(", NULL, '")
        sqlAppender.appendSql(indexName)
        sqlAppender.appendSql("')::pgroonga_full_text_search_condition")
    }
}
