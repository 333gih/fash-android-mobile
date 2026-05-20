# Home & Explore — Chiến lược UI/UX (guest / logged-in / cold start)

**Phạm vi giai đoạn 1:** Cải thiện bố cục, copy, và insight hiển thị **trên app hiện tại** (vẫn vào app qua login).  
**Chưa làm giai đoạn 2:** Mở browse không đăng nhập + API public (cần core-service).

**Liên quan code:** `HomeFeedContent.kt`, `ExploreScreen.kt`, `GET /listings/home`, `GET /search/listings` (explore heat).

---

## 1. Chẩn đoán hiện trạng

### 1.1 Vai trò thực tế của từng màn (sau login)

| Màn | API chính | Ý nghĩa thực |
|-----|-----------|--------------|
| **Home → “Dành cho bạn”** | `GET /listings/home` | Feed **chỉ từ seller đã follow** — không phải recommendation |
| **Explore → Listings** | `GET /search/listings` (không `q`) | **Cold start thật** — heat score (like, save, recency) |
| **Home → Shop nên ghé** | `GET /search/featured-sellers` | Leaderboard seller (verified, followers) |
| **Home → Bạn vừa xem** | `GET /listings/recently-viewed` | Resume session — **ẩn hẳn nếu rỗng** |

### 1.2 Vấn đề UX (gây confused)

1. **Nhãn “Dành cho bạn”** gợi TikTok/Pinterest (personalized) nhưng data là **social graph** (follow-only) → user mới **luôn thấy empty card** dù Explore đầy đồ.
2. **Home xếp discovery phía dưới empty state** — Editorial + Featured sellers + Recently viewed **sau** khối “Chưa có món” → scroll mới thấy nội dung sống.
3. **Explore và Home trùng chức năng nhận thức** — cả hai đều có grid/listing + featured sellers; user không biết “vào tab nào để săn”.
4. **Journey row (Đang giao / Đã lưu / Tin nhắn)** trên Home — đúng với buyer active, **nhiễu với user mới** chưa có đơn.
5. **Guest browse:** App **chưa có** shell guest (`MainNavScreen` chỉ sau login). Research cold start **áp dụng sau** khi mở API; giai đoạn 1 vẫn fix cho **user mới đã login**.

### 1.3 Core value Fash (neo business — filter mọi đề xuất)

| Core value | Implication cho Home/Explore |
|------------|------------------------------|
| **C2C second-hand, gặp mặt / giao gần** | Ưu tiên **ảnh đẹp, giá, khoảng cách / meetup**, không dashboard đơn hàng |
| **Tin cậy seller + chat trước khi chốt** | CTA “Nhắn seller”, profile seller nổi trên card — không chỉ “mua ngay” |
| **Social (follow shop)** | “Dành cho bạn” **hợp lý khi đã follow** — cần đổi tên + vị trí khi chưa follow |
| **Không phải Shopee logistics** | Home không nên giống trang quản lý đơn; journey row thu nhỏ khi không có activity |

**KPI phù hợp giai đoạn đầu:** session duration, scroll depth trên Explore, save rate, **click → chat**, repeat open — khớp research bạn đưa.

---

## 2. Thói quen user VN — phần apply được

| Insight (VN / Gen Z–Millennial) | Apply cho Fash |
|----------------------------------|----------------|
| **Lướt trước, quyết định sau** (TikTok Shop, Shopee browse) | Feed **không trống**; ưu tiên visual grid, không wall “đăng nhập” |
| **Giá + deal + “mới đăng”** kích hoạt săn đồ second-hand | Chip **“Mới hôm nay”**, badge thời gian đăng trên card (Explore đã có heat recency backend) |
| **Tin người bán / chat** trước giao dịch | Login gate đặt ở **save / chat / đặt lịch gặp** — không chặn **xem** listing |
| **Local / gần tôi** (meetup model) | Copy “Gần bạn” khi có location; Phase 2 API filter `country` / geo |
| **Style tribe** (streetwear, vintage, Y2K) | **1-tap interest chips** (local cache, không account) — UI Phase 1 có thể là chip → mở Explore filtered |
| **Gen Z: entertainment + trend; Millennial: tin cậy** | Home: editorial + shop nổi bật; Explore: grid săn đồ |

**Không over-apply ngay:** AI recommendation, nearby GPS chính xác, curated DB riêng — backend đã có **explore heat** + **featured sellers** đủ cho Phase 1.

---

## 3. Định vị lại hai tab (mental model)

```text
HOME = "Của tôi + những gì liên quan tôi"
EXPLORE = "Săn đồ trên toàn sàn (luôn có nội dung)"
```

| | **Home** | **Explore** |
|---|----------|-------------|
| **Mục tiêu** | Retention: đơn đang chạy, follow feed, tiếp tục xem | Acquisition & discovery: scroll sâu, filter, search |
| **Primary content** | Follow feed (khi có) + **preview săn đồ** khi chưa follow | Full heat-ranked grid |
| **Secondary** | Journey (conditional), promo, editorial, featured sellers | Sellers tab, filters, search overlay |
| **Empty state** | **Không được** chỉ empty — phải có **fallback grid** (preview Explore) | Chỉ empty khi filter quá chặt |

---

## 4. Thiết kế đề xuất — Logged-in (Phase 1 UI/UX, không đổi auth flow)

### 4.1 Cấu trúc Home mới (thứ tự block)

```text
1. Buyer journey row     → CHỈ hiện khi có ≥1: delivering | saved | unread messages
2. Promo slider          → giữ
3. Quick actions         → Explore | Bán | Đơn (giữ)
4. ★ Săn đồ hôm nay      → NEW: 6–8 listing từ logic Explore (reuse VM/API explore)
   subtitle: "Đang hot trên Fash"
5. Từ shop bạn theo dõi → ĐỔI TÊN từ "Dành cho bạn"
   - Có follow + có tin → grid 2 cột như hiện tại
   - Không follow / rỗng → KHÔNG dùng empty card lớn; 1 dòng copy + CTA "Theo shop" → Featured sellers
6. Bạn vừa xem          → CHỈ khi ≥2 item; đưa LÊN ngay dưới block 5 (returning user)
7. Shop nên ghé         → featured sellers rail
8. Cẩm nang             → editorial
9. Footer brand
```

**Lý do:** User mới scroll 2–3 màn hình đã thấy **ảnh sản phẩm thật** (block 4), không bị chặn bởi empty “Dành cho bạn”.

### 4.2 Đổi copy (tránh confused)

| Hiện tại | Đề xuất (VI) | Đề xuất (EN) |
|----------|--------------|--------------|
| Dành cho bạn | **Từ shop bạn theo dõi** | **From shops you follow** |
| subtitle: Tin mới từ shop... | **Theo dõi shop để thấy tin mới ở đây** | Same intent, shorter |
| Chưa có món nào ở đây (empty lớn) | **Bỏ empty lớn** — thay bằng block “Săn đồ hôm nay” + 1 dòng nhỏ dưới section follow | |
| Khám phá (CTA) | **Lướt thêm** / **Xem tất cả** → tab Explore | |

### 4.3 Explore — tinh chỉnh nhẹ (Phase 1)

- Subtitle rõ: *“Toàn bộ đồ đang bán — sắp theo độ hot & mới đăng”*.
- **Style chips hàng đầu** (UI): Streetwear | Vintage | Y2K | Local brand → `onFilter` aesthetic tag (đã có filter Explore).
- Giữ Sellers tab — phù hợp social marketplace.
- **Không** nhân đôi “Recently viewed” trên Explore (tránh trùng Home).

### 4.4 Recently viewed — bỏ khỏi Home có hợp lý không?

| Option | Đánh giá |
|--------|----------|
| **Bỏ hẳn** | ❌ Mất resume path; Explore không có thay thế |
| **Giữ cuối Home** | ⚠️ Returning user phải scroll qua editorial + sellers |
| **✅ Khuyến nghị** | Giữ nhưng **conditional + ưu tiên cao** (≥2 items, đặt sau follow feed / trước featured sellers) |
| **Phase 2** | Thêm entry trên Profile: “Đã xem gần đây” |

---

## 5. Thiết kế đề xuất — Guest (chuẩn bị UI, implement sau khi mở API)

> **Lưu ý:** Hiện core **không** cho guest gọi `GET /search/listings`. Phase 2 cần `OptionalAuthenticate` hoặc public browse + rate limit.

### 5.1 Cùng shell tab, khác nội dung

| Block | Guest | Logged-in |
|-------|-------|-----------|
| Journey row | Ẩn | Conditional |
| Promo | ✅ Public slides (`/app/advertising/slides`) | ✅ |
| Quick actions | Explore ✅ / Bán → login / Đơn → login | ✅ |
| Primary feed | **Săn đồ hôm nay** (trending/explore) | Follow feed + fallback săn đồ |
| Save / Like / Chat | Bottom sheet **“Đăng nhập để lưu & nhắn seller”** | Bình thường |
| Featured sellers | ✅ (sau API public) | ✅ |
| Recently viewed | Local cache listing IDs (Room) tối đa 20 | Server + local |

### 5.2 Onboarding interest (không cần account)

```text
Lần đầu mở Explore (hoặc sau splash optional):
  "Bạn thích style nào?" 
  [Streetwear] [Vintage] [Y2K] [Minimal] [Bỏ qua]
→ SharedPreferences → pre-select filter Explore
```

Rẻ, khớp research, không cần AI.

### 5.3 Insight guest vs logged-in (visual)

- **Guest:** badge nhẹ trên header Explore *“Đang xem với tư cách khách”* + nút **Đăng nhập** góc phải (không modal chặn feed).
- **Logged-in:** không badge; bell inbox như hiện tại.

---

## 6. Cold start — map research → backend hiện có

| Chiến lược research | Backend Fash hiện tại | Phase |
|---------------------|----------------------|-------|
| Trending / hot | `GET /search/listings` heat score | ✅ Đã có (cần auth) |
| New listings | Heat ưu tiên recency | ✅ Một phần |
| Nearby | Chưa có distance sort public | Phase 2 |
| Curated collections | Editorial public + promo slides | ✅ Một phần |
| Featured sellers | `GET /search/featured-sellers` | ✅ (cần auth) |
| Follow feed | `GET /listings/home` | ✅ Chỉ khi có follow |
| Interest tags | Filter `aesthetic_tag_ids` Explore | ✅ UI chips Phase 1 |
| Scoring đơn giản | Đã implement trong `GetExploreFeed` | ✅ Không cần ML |

**Sai lầm tránh:** Để `listings/home` làm discovery chính — **sai domain**; home API trả `[]` khi không follow là **đúng**, UI phải compensate.

---

## 7. Wireframe logic (logged-in, user mới chưa follow ai)

```text
┌─────────────────────────────────────┐
│ [Promo]                             │
│ [Khám phá] [Bán] [Đơn]              │
├─────────────────────────────────────┤
│ Săn đồ hôm nay          [Xem tất cả]│
│ ┌────┐ ┌────┐                       │
│ │img │ │img │  ... (6-8)            │
│ └────┘ └────┘                       │
├─────────────────────────────────────┤
│ Từ shop bạn theo dõi                │
│ Theo vài shop để thấy tin mới ở đây │
│ [Khám phá shop nổi bật →]           │
├─────────────────────────────────────┤
│ Shop nên ghé (horizontal)           │
│ Cẩm nang                            │
└─────────────────────────────────────┘
```

**User đã follow + có listing:** block 4 thu nhỏ hoặc đẩy xuống; block 5 full grid.

---

## 8. Login gate — đặt đúng chỗ (chuẩn marketplace VN)

| Hành động | Guest (tương lai) | Hiện tại |
|-----------|-------------------|----------|
| Xem feed / PDP | Cho phép | Cần login (toàn app) |
| Search | Cho phép | Login |
| Save / Like | Login | Login |
| Chat seller | Login | Login |
| Đặt lịch gặp / checkout | Login | Login |
| Đăng bán | Login | Login |

Phase 1 (vẫn login app): giảm **cảm giác** dashboard bằng layout §4, không đổi gate.

---

## 9. Roadmap đề xuất

### Phase 1 — UI/UX only (ưu tiên, khớp yêu cầu hiện tại) — **Done**

- [x] Đổi tên section + copy (`home_top_section_*`, explore subtitle)
- [x] Block **“Săn đồ hôm nay”** — `HomeHuntTodaySection` + `GET /search/listings` (`sort=popular`, limit 8)
- [x] Journey row conditional (`BuyerHomeStats.hasJourneyActivity()`)
- [x] Thay `HomePersonalizedFeedEmptyCard` bằng `HomeFollowFeedEmptyHint` (gọn)
- [x] `HomeRecentlyViewedSection` — min 2 items, đặt trước editorial/sellers
- [x] `ExploreStyleQuickChipsRow` trên Explore (toggle aesthetic filter)
- [ ] (Deferred) `HomeTrendingCategoriesSection` — repository vẫn trả empty categories

### Phase 2 — Product + API — **In progress (guest browse shipped)**

- [x] Guest shell: `GuestMainShell` + `MainNavScreen(isGuestMode)` — Home/Explore/PDP không login
- [x] core: `GET /api/v1/public/browse/*` + client attestation (`PUBLIC_BROWSE_CLIENT_SECRETS`) — see `core-service/docs/public-browse-api.md`
- [x] Kong: `api-subdomain-core-public`, `api-core-service-public-path`, RBAC skip `/api/v1/public/`
- [x] Android: `PublicBrowseHttp`, `PUBLIC_BROWSE_CLIENT_*` BuildConfig, login “Tiếp tục xem không đăng nhập”
- [x] UX gates: `GuestLoginSheet` + tab placeholders (Profile/Chat/Post, orders, notifications)
- [ ] Local recent views guest (device-only)
- [ ] Interest onboarding → SharedPreferences
- [ ] Admin portal preview headers (`fash-admin-portal`)

### Phase 3 — Growth

- [ ] Nearby filter
- [ ] Curated collections CMS
- [ ] A/B KPI: scroll depth, chat CTR

---

## 10. Tóm tắt trả lời câu hỏi

1. **Home guest vs logged-in hiển thị thế nào?**  
   - Guest (sau này): Home ≈ Explore lite (hot grid + promo), không journey, login mềm trên hành động.  
   - Logged-in: Home = activity (optional) + **săn đồ fallback** + **follow feed** (đổi tên) + resume + social proof sellers.

2. **Cold start apply gì?**  
   - Ngay: explore heat + featured sellers + editorial + style chips UI.  
   - Sau: public API, local interest, nearby.

3. **Neo business?**  
   - Ưu tiên scroll săn đồ + chat + meetup, không biến Home thành Shopee dashboard.

4. **Recent view bỏ Home?**  
   - **Không bỏ** — **đổi vị trí + conditional**; Phase 2 thêm Profile entry.

5. **Phase hiện tại:**  
   - **Chỉ UI/UX** trên user đã login; fix “For you” misleading và empty feed trước khi mở guest browse.

---

*Tài liệu dùng cho design review và ticket breakdown Android; backend guest cần ticket riêng trên core-service.*
