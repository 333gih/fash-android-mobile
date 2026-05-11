"""
Rebuild values-en/strings.xml from values/strings.xml order, using existing EN
where present and TRANSLATIONS for gaps.
"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_PATH = ROOT / "app/src/main/res/values/strings.xml"
EN_PATH = ROOT / "app/src/main/res/values-en/strings.xml"

TRANSLATIONS: dict[str, str] = {
    "notification_channel_chat_name": "Messages",
    "notification_channel_chat_desc": "Chat notifications",
    "notification_channel_orders_name": "Orders",
    "notification_channel_orders_desc": "Order updates",
    "notification_channel_general_name": "Fash",
    "notification_channel_general_desc": "General notifications",
    "following_button": "Following",
    "chat_inbox_preview_offer_pending_seller": "Offer pending · %1$s",
    "chat_inbox_preview_offer_you": "You · offer %1$s",
    "chat_inbox_preview_offer_from_seller": "Seller · %1$s",
    "chat_inbox_preview_offer_from_buyer": "Buyer offered %1$s",
    "chat_inbox_preview_offer_generic": "Price offer · %1$s",
    "chat_offer_dialog_helper": "Fair offers help you close deals faster.",
    "chat_delete_message_title": "Delete message",
    "chat_delete_message_body": "This message will be deleted. You can only delete within 5 minutes.",
    "chat_delete_message_confirm": "Delete",
    "chat_offer_accepted_label": "Offer accepted",
    "chat_offer_checkout": "Checkout now",
    "chat_set_price": "Make offer",
    "chat_input_hint": "Type a message…",
    "chat_send": "Send",
    "chat_send_error_gateway": (
        "The gateway temporarily returned an error (code %1$d). Try again. "
        "Text that looks like shell or SQL commands is sometimes blocked by edge security — rephrase or contact support."
    ),
    "address_catalog_load_failed": (
        "Could not load the administrative catalog (province / district / ward). "
        "Check your connection and common-service auth (JWT or INTERNAL_SECRET must match the server), then try again."
    ),
    "chat_offer_label": "Price offer",
    "chat_offer_accept": "Accept",
    "chat_offer_decline": "Decline",
    "chat_offer_dialog_listed_price": "Listed price: %s",
    "chat_offer_dialog_placeholder": "Enter amount",
    "chat_deal_banner_buyer_pending": "Deal confirmed! Tap to pay",
    "chat_deal_banner_seller_pending": "Waiting for buyer to pay",
    "chat_deal_banner_in_progress": "Order in progress",
    "chat_deal_banner_done": "Deal completed",
    "chat_deal_banner_cancelled": "Order cancelled — tap for details",
    "chat_deal_banner_disputed": "Order disputed — tap for details",
    "chat_deal_banner_delivered": "Order delivered and confirmed",
    "chat_deal_banner_status_pending": "Updating order status — tap to view",
    "chat_deal_banner_view_order": "View order →",
    "chat_deal_pay_now": "Pay now",
    "chat_deal_payment_deadline_warning_buyer": (
        "Important: Pay soon to keep this order. Unpaid orders may be cancelled under policy."
    ),
    "chat_deal_payment_deadline_warning_seller": (
        "The buyer must pay soon — unpaid orders may be cancelled."
    ),
    "chat_offer_status_waiting": "Waiting…",
    "chat_offer_status_accepted": "Accepted ✓",
    "chat_offer_status_declined": "Declined",
    "chat_offer_status_expired": "Expired",
    "chat_offer_status_cancelled": "Cancelled",
    "chat_offer_waiting_seller": "Waiting for seller response…",
    "chat_pending_offer_button_waiting": "Waiting…",
    "chat_listing_sold_label": "SOLD",
    "chat_error_pending_offer": "Please wait for the seller to respond to your current offer",
    "chat_error_order_exists": "A transaction already exists for this item",
    "chat_error_forbidden": "You don’t have access",
    "chat_error_not_found": "Conversation not found",
    "chat_error_offer_limit": "Offer limit reached (max %1$d).",
    "chat_offer_limit_tooltip": "Offer limit reached (%1$d)",
    "chat_offer_policy_intro": "This product thread allows up to %1$d price offers.",
    "chat_offer_policy_progress": "Used %1$d/%2$d offers (max %2$d per thread for this product).",
    "chat_offer_policy_last": "You have 1 offer left (max %1$d per thread).",
    "chat_offer_policy_at_limit": "Reached %1$d/%2$d offers — cannot send more.",
    "chat_offer_limit_reset_banner": "Listing price changed — %1$s",
    "chat_reopened_snackbar": "This item is available again — you can keep chatting.",
    "chat_conversation_ended_readonly": "This conversation has ended",
    "chat_conversation_sold_readonly": "This item has been sold",
    "chat_inbox_by_product": "By product",
    "product_reserved_other": "Someone else is checking out",
    "product_reserved_buyer": "Your order is being processed",
    "product_listing_sold_bar": "Sold",
    "product_listing_available_snackbar": "This item is available again",
    "checkout_subtitle": "Confirm your order & pay securely",
    "checkout_order_overview": "Order overview",
    "checkout_section_product": "Product",
    "checkout_section_order": "Order",
    "checkout_section_parties": "Participants",
    "checkout_order_id_label": "Order ID",
    "checkout_price_deal": "Agreed price",
    "checkout_price_listed": "Listed price",
    "checkout_platform_fee_from_order": "Platform fee",
    "checkout_platform_fee_estimate": "Platform fee (est. 10%%)",
    "checkout_seller_receives": "Seller receives (after fees)",
    "checkout_value_title": "Secure checkout",
    "checkout_value_escrow": "Funds are held until you confirm delivery.",
    "checkout_value_support": "Dispute support if the item doesn’t match the description.",
    "checkout_editorial_badge": "EDITORIAL SELECTION",
    "checkout_shipping_fee": "Shipping",
    "checkout_discount": "Discount",
    "checkout_total_payment": "Total to pay",
    "checkout_awaiting_gateway": "Waiting for payment gateway… The app will update when payment completes.",
    "checkout_secured_fash_pay": "SECURED BY FASH PAY",
    "checkout_payment_method_label": "Choose payment method",
    "checkout_order_summary": "Payment summary",
    "checkout_confirm_pay": "Confirm & pay",
    "checkout_payment_init_failed": "Could not start payment. Check core API (orders/…/payments/initiate) or try again.",
    "checkout_payment_poll_timeout": "No payment confirmation yet. Check Orders or try again.",
    "checkout_payment_cancelled_or_dispute": "Order cancelled or in dispute.",
    "orders_empty_buying": "No purchases yet",
    "orders_empty_buying_sub": "Orders appear when you buy an item",
    "orders_empty_selling": "No sales yet",
    "orders_empty_selling_sub": "Orders appear when someone buys from you",
    "avatar_default_cd": "Profile photo",
    "order_detail_loading": "Loading order…",
    "order_detail_title": "Order details",
    "order_detail_status_caption": "Order status",
    "order_detail_load_error": "Could not load order.",
    "order_detail_pay": "Pay",
    "order_detail_party_buyer": "Buyer",
    "order_detail_party_seller": "Seller",
    "order_detail_product": "Product",
    "order_detail_amount": "Subtotal",
    "order_detail_platform_fee": "Platform fee",
    "order_detail_seller_payout": "Seller payout",
    "order_detail_tracking": "Shipping",
    "order_detail_tracking_empty": "No tracking number yet",
    "order_detail_review_sent": "Review submitted.",
    "order_detail_review_error": "Could not submit review.",
    "order_detail_review_title": "Rate this order",
    "order_detail_review_hint": "Choose a star rating (1–5)",
    "order_detail_review_submit": "Submit review",
    "order_status_payment_pending": "Awaiting payment",
    "order_status_payment_held": "Paid — awaiting shipment",
    "order_status_in_transit": "In transit",
    "order_status_delivered_confirmed": "Delivered",
    "order_status_cancelled": "Cancelled",
    "order_status_disputed": "In dispute",
    "order_status_unknown": "—",
    "order_hero_buyer_payment_pending_title": "Awaiting payment",
    "order_hero_buyer_payment_pending_sub": "Complete payment so the seller can prepare your item.",
    "order_hero_seller_wait_payment_title": "Waiting for buyer to pay",
    "order_hero_seller_wait_payment_sub": "You’ll be notified when payment completes.",
    "order_hero_seller_prepare_title": "Prepare shipment",
    "order_hero_seller_prepare_sub": "Pack and ship on time to avoid cancellation.",
    "order_hero_seller_prepare_sub_deadline": "Ship before %1$s (per policy).",
    "order_hero_buyer_payment_held_title": "Paid — awaiting delivery",
    "order_hero_buyer_payment_held_sub": "Funds are held safely until you confirm receipt.",
    "order_hero_buyer_escrow_sub": "Estimated payout to seller: %1$s (if applicable).",
    "order_hero_eta_line": "Estimated delivery: %1$s",
    "order_hero_in_transit_sub": "Your order is on the way.",
    "order_hero_completed_sub": "Completed on %1$s",
    "order_hero_cancelled_sub": "Cancelled on %1$s",
    "order_hero_cancelled_sub_generic": "This order was cancelled.",
    "order_hero_disputed_sub": "Please watch for updates or contact support.",
    "order_timeline_title": "Order history",
    "order_timeline_placed": "Order placed",
    "order_timeline_waiting_payment": "Awaiting payment",
    "order_timeline_paid": "Payment received",
    "order_timeline_preparing": "Preparing shipment",
    "order_timeline_preparing_sub": "Seller is packing your order.",
    "order_timeline_shipped": "Shipped",
    "order_timeline_in_transit": "Out for delivery",
    "order_timeline_delivered": "Delivered",
    "order_timeline_cancelled": "Cancelled",
    "order_timeline_dispute_open": "Dispute",
    "order_detail_shipping_address_title": "Shipping address",
    "order_detail_payment_breakdown_title": "Payment breakdown",
    "order_detail_buyer_product_price": "Item price",
    "order_detail_buyer_shipping_fee": "Shipping fee",
    "order_detail_buyer_total": "Total",
    "order_detail_seller_revenue_title": "Payment breakdown",
    "order_detail_seller_listing_price": "Sale price",
    "order_detail_commission_percent": "Commission (%1$d%%)",
    "order_detail_seller_net": "Net earnings",
    "order_detail_buyer_protection_title": "Buyer protection",
    "order_detail_buyer_protection_body": "Fash helps with refunds if the item doesn’t match the description or you don’t receive it (per policy).",
    "order_detail_counterparty_buyer": "Buyer details",
    "order_detail_counterparty_seller": "Seller",
    "order_detail_variant_line": "Variant: %1$s",
    "order_detail_action_ship": "Mark shipped",
    "order_detail_chat_buyer": "Chat with buyer",
    "order_detail_chat_seller": "Contact seller",
    "order_detail_help_cd": "Help",
    "order_detail_help_body": "Order status: awaiting payment, paid (escrow), in transit, delivered, cancelled, or disputed. For more help, contact customer support.",
    "order_detail_help_close": "Close",
    "order_detail_chat_unavailable": "No chat thread for this order yet.",
    "order_detail_ship_dialog_title": "Shipment details",
    "order_detail_ship_tracking_label": "Tracking number",
    "order_detail_ship_carrier_label": "Carrier",
    "order_detail_ship_confirm": "Confirm shipment",
    "order_detail_ship_success": "Shipment updated.",
    "order_detail_ship_error": "Could not update shipment. Try again.",
    "order_detail_ship_tracking_required": "Enter a tracking number.",
    "order_detail_dispute_open": "Open dispute",
    "order_detail_dispute_evidence": "Update evidence",
    "order_detail_dispute_dialog_title_open": "Open dispute",
    "order_detail_dispute_dialog_title_evidence": "Submit evidence",
    "order_detail_dispute_description_label": "Description",
    "order_detail_dispute_description_required": "Please enter a description.",
    "order_detail_dispute_add_photo": "Add photo (%1$d/10)",
    "order_detail_dispute_photo_limit": "Maximum 10 photos.",
    "order_detail_dispute_submit": "Submit",
    "order_detail_dispute_open_success": "Dispute opened.",
    "order_detail_dispute_evidence_success": "Evidence updated.",
    "order_detail_dispute_error": "Action failed. Try again.",
    "order_detail_shipping_title": "Shipping address",
    "order_detail_shipping_change": "Change",
    "order_detail_shipping_choose": "Choose address",
    "order_detail_shipping_none": "No shipping address selected",
    "address_list_title": "Shipping address",
    "address_list_header": "Delivery",
    "address_list_subtitle": "Choose where your order will be sent.",
    "address_badge_default": "DEFAULT",
    "address_add_new": "Add new address",
    "address_confirm": "Confirm",
    "address_add_title": "Add new address",
    "address_section_shipping": "SHIPPING",
    "address_form_header": "Delivery details",
    "address_field_full_name": "FULL NAME",
    "address_field_full_name_hint": "Recipient’s full name",
    "address_field_phone": "PHONE",
    "address_field_phone_hint": "e.g. 0912 345 678",
    "address_field_city": "PROVINCE / CITY",
    "address_field_city_hint": "Type or select province / city",
    "address_field_district": "DISTRICT",
    "address_field_district_hint": "Type or select district",
    "address_field_ward": "WARD / COMMUNE",
    "address_field_ward_hint": "Type or select ward / commune",
    "address_field_line1": "STREET, BUILDING",
    "address_field_line1_hint": "Street address, building, unit",
    "address_default_toggle": "Set as default address",
    "address_default_toggle_sub": "Use this address for future orders",
    "address_save": "Save address",
    "address_saved_success": "Address saved.",
    "address_validation_required": "Please fill in all required fields.",
    "address_empty_alert_title": "No address yet",
    "address_empty_alert_body": (
        "Add at least one shipping address. You can set one as default for faster checkout."
    ),
    "address_empty_alert_create": "Add address",
}


def parse_string_elements(text: str) -> list[tuple[str, str, str]]:
    """Return list of (name, attributes_after_name, inner_xml)."""
    out: list[tuple[str, str, str]] = []
    for m in re.finditer(
        r'<string\s+name="([^"]+)"([^>]*)>([\s\S]*?)</string>',
        text,
    ):
        out.append((m.group(1), m.group(2), m.group(3)))
    return out


def parse_en_map(text: str) -> dict[str, str]:
    d: dict[str, str] = {}
    for name, attrs, inner in parse_string_elements(text):
        d[name] = inner
    return d


def main() -> None:
    default_text = DEFAULT_PATH.read_text(encoding="utf-8")
    en_old = EN_PATH.read_text(encoding="utf-8")
    en_map = parse_en_map(en_old)

    default_entries = parse_string_elements(default_text)
    lines: list[str] = ["<resources>"]

    for name, attrs, vi_inner in default_entries:
        if name in TRANSLATIONS:
            body = TRANSLATIONS[name].replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        elif name in en_map:
            body = en_map[name]
        else:
            body = vi_inner
            print("WARN: fallback to default (vi) for:", name)

        lines.append(f'    <string name="{name}"{attrs}>{body}</string>')

    lines.append("</resources>")
    EN_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {len(default_entries)} strings to {EN_PATH}")


if __name__ == "__main__":
    main()
