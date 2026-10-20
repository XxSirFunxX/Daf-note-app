package com.example.model

enum class PaperSize(
    val titleFa: String,
    val widthPt: Float,
    val heightPt: Float,
    val pdfWidthPt: Int = 595,
    val pdfHeightPt: Int = 842
) {
    A4("A4 (۲۱۰ × ۲۹۷ میلی‌متر)", 1000f, 1414.28f, 595, 842),
    A3("A3 (۲۹۷ × ۴۲۰ میلی‌متر)", 1414.28f, 2000f, 842, 1191)
}
