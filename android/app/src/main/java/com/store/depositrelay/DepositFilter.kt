package com.store.depositrelay

/**
 * 어떤 문자/알림을 "입금 알림"으로 볼지 판단한다.
 * 개인 문자를 서버로 보내지 않도록, 기본적으로 입금성 문구가 있을 때만 전달한다.
 */
object DepositFilter {

    /** 문자 없이 앱 알림만 오는 은행 등, 알림 접근으로 감시할 패키지 */
    val BANK_PACKAGES = setOf(
        "com.kakaobank.channel",  // 카카오뱅크
        "viva.republica.toss",    // 토스
        "com.kbstar.kbbank",      // KB스타뱅킹
        "com.kbstar.reboot",
        "com.shinhan.sbanking",   // 신한 SOL
        "com.wooribank.smart.npib",
        "com.kebhana.hanapush",   // 하나
        "nh.smart.banking",       // 농협
        "com.ibk.android.ionebank",
        "com.sc.danb.scbankapp",
        "com.kftc.bankledger",    // 오픈뱅킹류
    )

    private val POSITIVE = listOf("입금", "받았어요", "받았습니다", "이체받", "입금액")
    private val NEGATIVE = listOf("출금", "결제", "승인취소", "해외승인", "체크카드", "신용카드", "광고", "(광고)")

    fun looksLikeDeposit(text: String): Boolean {
        if (text.isBlank()) return false
        val hasPos = POSITIVE.any { text.contains(it) }
        if (!hasPos) return false
        for (n in NEGATIVE) {
            if (text.contains(n) && n != "출금") return false
        }
        // 금액(숫자+원)이 있어야 함
        return Regex("[0-9][0-9,]{2,}\\s*원?").containsMatchIn(text)
    }

    fun isBankPackage(pkg: String?): Boolean =
        pkg != null && BANK_PACKAGES.contains(pkg)
}
