package com.butingbe.domain.reward.dto.response;

import java.util.List;

/**
 * 유저 보유 전체 보상 요약 응답.
 *
 * @param pointBalance 현재 보유 포인트 잔액
 * @param badges 6대 구역별 배지 월 목록
 * @param coupons 보유 쿠폰 목록
 */
public record UserRewardsResDto(
    long pointBalance, List<ZoneBadgeGroupResDto> badges, List<UserCouponResDto> coupons) {}
