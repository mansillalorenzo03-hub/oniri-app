package com.kanso.focus.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

enum class ReaderAccessibilityMode(val label: String) {
    DYSLEXIA("Solo Dislexia"),
    LOW_VISION("Poca Visión"),
    BOTH("Ambas")
}

object PdfExportHelper {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 44

    fun exportAndShareAdaptedPdf(
        context: Context,
        title: String,
        blocks: List<String>,
        mode: ReaderAccessibilityMode,
        baseFontSizeSp: Float,
        highContrastYellow: Boolean,
        limitToThreePages: Boolean
    ) {
        if (blocks.isEmpty()) return

        val contentWidth = PAGE_WIDTH - (MARGIN * 2)
        val contentHeight = PAGE_HEIGHT - (MARGIN * 2)

        val formattedText = buildFormattedSpannable(blocks, mode)

        val defaultTargetSize = when (mode) {
            ReaderAccessibilityMode.DYSLEXIA -> baseFontSizeSp.coerceAtLeast(14f)
            ReaderAccessibilityMode.LOW_VISION -> (baseFontSizeSp * 1.25f).coerceAtLeast(20f)
            ReaderAccessibilityMode.BOTH -> (baseFontSizeSp * 1.25f).coerceAtLeast(20f)
        }

        val finalFontSize = if (limitToThreePages) {
            findMaxFontSizeForPageLimit(
                text = formattedText,
                contentWidth = contentWidth,
                pageContentHeight = contentHeight,
                maxPages = 3,
                initialMaxSize = defaultTargetSize
            )
        } else {
            defaultTargetSize
        }

        val textPaint = createTextPaint(finalFontSize, highContrastYellow)
        val staticLayout = buildStaticLayout(formattedText, textPaint, contentWidth)

        val pageSlices = paginateLayout(
            layout = staticLayout,
            pageContentHeight = contentHeight,
            maxPages = if (limitToThreePages) 3 else Int.MAX_VALUE
        )

        val pdfDocument = PdfDocument()
        try {
            pageSlices.forEachIndexed { index, (startTop, endBottom) ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                if (highContrastYellow) {
                    canvas.drawColor(Color.BLACK)
                } else {
                    canvas.drawColor(Color.WHITE)
                }

                // Encabezado sutil de página
                val headerPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                    color = if (highContrastYellow) Color.parseColor("#FFD600") else Color.DKGRAY
                    textSize = 9f
                    typeface = Typeface.MONOSPACE
                }
                canvas.drawText(
                    "Kanso · ${mode.label} · Pág. ${index + 1}/${pageSlices.size}",
                    MARGIN.toFloat(),
                    (MARGIN - 16).toFloat(),
                    headerPaint
                )

                canvas.save()
                canvas.translate(MARGIN.toFloat(), MARGIN.toFloat())
                canvas.clipRect(0, 0, contentWidth, endBottom - startTop)
                canvas.translate(0f, -startTop.toFloat())
                staticLayout.draw(canvas)
                canvas.restore()

                pdfDocument.finishPage(page)
            }

            val pdfDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val safeName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "kanso_adaptado" }
            val outFile = File(pdfDir, "${safeName}_kanso.pdf")
            FileOutputStream(outFile).use { output ->
                pdfDocument.writeTo(output)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Documento adaptado Kanso ($title)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Exportar PDF adaptado").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } finally {
            pdfDocument.close()
        }
    }

    private fun findMaxFontSizeForPageLimit(
        text: CharSequence,
        contentWidth: Int,
        pageContentHeight: Int,
        maxPages: Int,
        initialMaxSize: Float
    ): Float {
        var low = 6f
        var high = initialMaxSize.coerceAtLeast(8f)
        var best = low

        repeat(14) {
            val mid = (low + high) / 2f
            val paint = createTextPaint(mid, false)
            val layout = buildStaticLayout(text, paint, contentWidth)
            val pages = paginateLayout(layout, pageContentHeight, Int.MAX_VALUE).size
            if (pages <= maxPages) {
                best = mid
                low = mid
            } else {
                high = mid
            }
        }
        return best
    }

    private fun createTextPaint(textSizePx: Float, highContrastYellow: Boolean): TextPaint {
        return TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
            color = if (highContrastYellow) Color.parseColor("#FFD600") else Color.BLACK
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
    }

    private fun buildStaticLayout(
        text: CharSequence,
        paint: TextPaint,
        width: Int
    ): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1.25f)
            .setIncludePad(false)
            .build()
    }

    private fun paginateLayout(
        layout: StaticLayout,
        pageContentHeight: Int,
        maxPages: Int
    ): List<Pair<Int, Int>> {
        val pages = mutableListOf<Pair<Int, Int>>()
        val lineCount = layout.lineCount
        if (lineCount == 0) {
            pages.add(0 to pageContentHeight)
            return pages
        }

        var startLine = 0
        while (startLine < lineCount && pages.size < maxPages) {
            val startTop = layout.getLineTop(startLine)
            var endLine = startLine
            while (endLine + 1 < lineCount &&
                layout.getLineBottom(endLine + 1) - startTop <= pageContentHeight
            ) {
                endLine++
            }
            val endBottom = layout.getLineBottom(endLine)
            pages.add(startTop to endBottom)
            startLine = endLine + 1
        }
        return pages
    }

    private fun buildFormattedSpannable(
        blocks: List<String>,
        mode: ReaderAccessibilityMode
    ): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        val applyBionic = mode == ReaderAccessibilityMode.DYSLEXIA || mode == ReaderAccessibilityMode.BOTH
        val applyLowVisionNumbers = mode == ReaderAccessibilityMode.LOW_VISION || mode == ReaderAccessibilityMode.BOTH

        blocks.forEachIndexed { index, block ->
            val prefix = "[${String.format("%02d", index + 1)}] "
            val prefixStart = builder.length
            builder.append(prefix)
            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                prefixStart,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            appendFormattedSegment(
                builder = builder,
                text = block,
                applyBionic = applyBionic,
                applyLowVisionNumbers = applyLowVisionNumbers
            )

            if (index < blocks.lastIndex) {
                builder.append("\n\n")
            }
        }
        return builder
    }

    private fun appendFormattedSegment(
        builder: SpannableStringBuilder,
        text: String,
        applyBionic: Boolean,
        applyLowVisionNumbers: Boolean
    ) {
        val tokenRegex = Regex("\\S+|\\s+")
        tokenRegex.findAll(text).forEach { match ->
            val token = match.value
            if (token.isBlank()) {
                builder.append(token)
            } else {
                val start = builder.length
                builder.append(token)
                val end = builder.length

                val hasDigits = token.any { it.isDigit() }
                if (applyLowVisionNumbers && hasDigits) {
                    val numberMatcher = Regex("\\d+([.,:/-]\\d+)*")
                    numberMatcher.findAll(token).forEach { numMatch ->
                        val numStart = start + numMatch.range.first
                        val numEnd = start + numMatch.range.last + 1
                        builder.setSpan(
                            StyleSpan(Typeface.BOLD),
                            numStart,
                            numEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        builder.setSpan(
                            RelativeSizeSpan(1.15f),
                            numStart,
                            numEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }

                if (applyBionic) {
                    val letterCount = token.count { it.isLetter() }
                    if (letterCount > 0) {
                        val boldCount = ceil(token.length * 0.45).toInt().coerceIn(1, token.length)
                        builder.setSpan(
                            StyleSpan(Typeface.BOLD),
                            start,
                            start + boldCount,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
        }
    }
}
