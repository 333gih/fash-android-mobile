package com.pc.fash_android_mobile.ui.orders

/** Which order-detail mutation is in flight — drives per-button loading and global blocking. */
enum class OrderDetailBusyAction {
    None,
    CheckIn,
    AcknowledgeCash,
    ReportNoShow,
    ConfirmHandoff,
    Ship,
    ConfirmReceipt,
    SubmitReview,
    OpenDispute,
    SubmitEvidence,
    CancelOrder,
}
