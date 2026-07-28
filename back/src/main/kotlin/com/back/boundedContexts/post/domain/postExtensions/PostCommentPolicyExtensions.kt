package com.back.boundedContexts.post.domain.postExtensions

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.PostComment
import com.back.global.dto.RsData
import com.back.global.exception.app.BusinessException

// ================================
// 수정 권한
// ================================

/**
 * 수정 권한 체크 (RsData 반환)
 */
fun PostComment.getCheckActorCanModifyRs(actor: Member?): RsData<Void> {
    if (actor == null) return RsData.fail("401-1", "로그인 후 이용해주세요.")
    if (actor == author) return RsData.OK
    return RsData.fail("403-1", "작성자만 댓글을 수정할 수 있습니다.")
}

fun PostComment.checkActorCanModify(actor: Member?) {
    val rs = getCheckActorCanModifyRs(actor)
    if (rs.isFail) throw BusinessException(rs.resultCode, rs.msg)
}

// ================================
// 삭제 권한
// ================================

/**
 * 삭제 권한 체크 (RsData 반환) - 관리자도 삭제 가능
 */
fun PostComment.getCheckActorCanDeleteRs(actor: Member?): RsData<Void> {
    if (actor == null) return RsData.fail("401-1", "로그인 후 이용해주세요.")
    if (actor.isAdmin) return RsData.OK
    if (actor == author) return RsData.OK
    return RsData.fail("403-2", "작성자만 댓글을 삭제할 수 있습니다.")
}

fun PostComment.checkActorCanDelete(actor: Member?) {
    val rs = getCheckActorCanDeleteRs(actor)
    if (rs.isFail) throw BusinessException(rs.resultCode, rs.msg)
}
