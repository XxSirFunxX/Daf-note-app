package com.example.model

enum class SymbolType(
    val defaultTitleFa: String,
    val defaultCategory: SymbolCategory,
    val isRingSound: Boolean = false,
    val defaultScale: Float = 1.0f
) {
    // 1. Notes (صداها)
    NOTE_WHOLE("نت گرد", SymbolCategory.NOTES),
    NOTE_HALF("نت سفید", SymbolCategory.NOTES),
    NOTE_QUARTER("نت سیاه", SymbolCategory.NOTES),
    NOTE_EIGHTH("نت چنگ", SymbolCategory.NOTES),
    NOTE_SIXTEENTH("نت دولاچنگ", SymbolCategory.NOTES),
    NOTE_THIRTYSECOND("نت سه‌لاچنگ", SymbolCategory.NOTES),
    NOTE_SIXTYFOURTH("نت چهارلاچنگ", SymbolCategory.NOTES),

    // 2. Rests (سکوت‌ها)
    REST_WHOLE("سکوت گرد", SymbolCategory.RESTS),
    REST_HALF("سکوت سفید", SymbolCategory.RESTS),
    REST_QUARTER("سکوت سیاه", SymbolCategory.RESTS),
    REST_EIGHTH("سکوت چنگ", SymbolCategory.RESTS),
    REST_SIXTEENTH("سکوت دولاچنگ", SymbolCategory.RESTS),
    REST_THIRTYSECOND("سکوت سه‌لاچنگ", SymbolCategory.RESTS),
    REST_SIXTYFOURTH("سکوت چهارلاچنگ", SymbolCategory.RESTS),

    // 3. Ring Sounds (صدای حلقه‌ها - خط چهارم فقط)
    RING_SINGLE("حلقه منفرد", SymbolCategory.RING_SOUNDS, isRingSound = true),
    RING_TREMOLO("ریز حلقه", SymbolCategory.RING_SOUNDS, isRingSound = true),
    RING_ACCENT("اشاره حلقه", SymbolCategory.RING_SOUNDS, isRingSound = true),
    RING_DOUBLE("حلقه جفتی", SymbolCategory.RING_SOUNDS, isRingSound = true),
    RING_THUMB_ROLL("شستی / پرتاب حلقه", SymbolCategory.RING_SOUNDS, isRingSound = true),

    // 4. Techniques & Strokes (تکنیک‌ها و مضراب)
    TECH_RAST("مضراب راست (∨)", SymbolCategory.TECHNIQUES),
    TECH_CHAP("مضراب چپ (∧)", SymbolCategory.TECHNIQUES),
    TECH_TOM("صدای تُم (بم)", SymbolCategory.TECHNIQUES),
    TECH_BAK("صدای بَک (زیر)", SymbolCategory.TECHNIQUES),
    TECH_MIANEH("صدای میانه", SymbolCategory.TECHNIQUES),
    TECH_RIZ("ریز (غلتک)", SymbolCategory.TECHNIQUES),
    TECH_ESHAREH("اشاره", SymbolCategory.TECHNIQUES),
    TECH_PALANG("پلنگ", SymbolCategory.TECHNIQUES),
    TECH_SEVEN("شماره ۷", SymbolCategory.TECHNIQUES),
    TECH_EIGHT("شماره ۸", SymbolCategory.TECHNIQUES),

    // 5. Bar Lines (خطوط میزان)
    BAR_SINGLE("خط میزان ساده", SymbolCategory.BAR_LINES),
    BAR_DOUBLE("دولاخط میزان", SymbolCategory.BAR_LINES),
    BAR_FINAL("خط پایان (دوبل ضخیم)", SymbolCategory.BAR_LINES),
    BAR_REPEAT_START("شروع تکرار (:|)", SymbolCategory.BAR_LINES),
    BAR_REPEAT_END("پایان تکرار (|:)", SymbolCategory.BAR_LINES),

    // 6. Time Signatures (میزان‌نما)
    TIME_2_4("میزان‌نما ۲/۴", SymbolCategory.TIME_SIGNATURES),
    TIME_3_4("میزان‌نما ۳/۴", SymbolCategory.TIME_SIGNATURES),
    TIME_4_4("میزان‌نما ۴/۴", SymbolCategory.TIME_SIGNATURES),
    TIME_6_8("میزان‌نما ۶/۸", SymbolCategory.TIME_SIGNATURES),
    TIME_7_8("میزان‌نما ۷/۸", SymbolCategory.TIME_SIGNATURES),
    TIME_CUSTOM("میزان‌نما سفارشی", SymbolCategory.TIME_SIGNATURES),

    // 7. Tempo & Dynamic text
    TEXT_TEMPO_ANDANTE("Andante ♩=80", SymbolCategory.TEMPO_TEXT),
    TEXT_DYNAMIC_F("دینامیک forte (f)", SymbolCategory.TEMPO_TEXT),
    TEXT_DYNAMIC_P("دینامیک piano (p)", SymbolCategory.TEMPO_TEXT),
    TEXT_DYNAMIC_MF("دینامیک mezzo forte (mf)", SymbolCategory.TEMPO_TEXT),
    TEXT_LESSON_BADGE("شماره درس", SymbolCategory.TEMPO_TEXT),
    TEXT_ANNOTATION("متن و یادداشت", SymbolCategory.TEMPO_TEXT),

    // 8. Custom user symbol
    CUSTOM_SYMBOL("نماد سفارشی", SymbolCategory.CUSTOM)
}
