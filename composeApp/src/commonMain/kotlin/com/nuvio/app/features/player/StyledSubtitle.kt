package com.nuvio.app.features.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.features.subflow.SubFlowPreferences
import com.nuvio.app.features.subflow.SubFlowRepository
import com.nuvio.app.features.subflow.SubFlowText
import com.nuvio.app.features.subflow.SubFlowWordEntry
import com.nuvio.app.features.subflow.SubFlowWordStatus
import kotlin.math.sqrt

@Composable
internal fun StyledSubtitleOverlay(
    text: String,
    style: SubtitleStyleState,
    modifier: Modifier = Modifier,
    videoAspectRatio: Float = 0f,
    videoResizeMode: PlayerResizeMode = PlayerResizeMode.Fit,
) {
    SubFlowRepository.ensureLoaded()
    val subFlowPreferences by SubFlowRepository.preferences.collectAsState()
    val subFlowWords by SubFlowRepository.words.collectAsState()
    val subFlowSelection by SubFlowRepository.selection.collectAsState()
    LaunchedEffect(text) { SubFlowRepository.setLiveSubtitleText(text) }
    val wordTapHandler: ((String, String) -> Unit)? =
        if (subFlowPreferences.enabled) SubFlowRepository::selectWord else null
    val fontFamily = rememberSubtitleFontFamily(style.appearance.fontId)
    if (text.isBlank()) return
    if (subFlowSelection != null) return
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val fittedVideoBounds = subtitleVideoBoundsPx(
            containerWidthPx = containerWidthPx,
            containerHeightPx = containerHeightPx,
            videoAspectRatio = videoAspectRatio,
            resizeMode = videoResizeMode,
        )
        val layoutScale = (maxHeight / 360.dp).coerceAtMost(1f)
        Box(
            Modifier.fillMaxSize().padding(
                horizontal = 16.dp * layoutScale,
                vertical = maxHeight * (0.0533f + style.bottomOffset.coerceIn(0, 200) / 1000f),
            ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            StyledSubtitleText(
                text = text,
                style = style,
                scale = layoutScale,
                videoWidthPx = fittedVideoBounds.width,
                videoHeightPx = fittedVideoBounds.height,
                fontFamily = fontFamily,
                onSubFlowWordTap = wordTapHandler,
                subFlowPreferences = subFlowPreferences.takeIf { it.enabled },
                subFlowWords = subFlowWords,
                selectedWord = subFlowSelection?.word,
            )
        }
    }
}

@Composable
internal fun StyledSubtitleText(
    text: String,
    style: SubtitleStyleState,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    videoWidthPx: Float = SUBTITLE_BLUR_REFERENCE_WIDTH_PX,
    videoHeightPx: Float = SUBTITLE_BLUR_REFERENCE_HEIGHT_PX,
    fontFamily: FontFamily = rememberSubtitleFontFamily(style.appearance.fontId),
    onSubFlowWordTap: ((word: String, line: String) -> Unit)? = null,
    subFlowPreferences: SubFlowPreferences? = null,
    subFlowWords: Map<String, SubFlowWordEntry> = emptyMap(),
    selectedWord: String? = null,
    fillWidth: Boolean = true,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(if (fillWidth) modifier.fillMaxWidth() else modifier) {
        val paddingX = with(density) { (8.dp * scale).toPx() }
        val paddingY = with(density) { (4.dp * scale).toPx() }
        val availableWidth = constraints.maxWidth.toFloat()

        val outline = with(density) { (style.outlineWidth.dp * scale).toPx() }
        val outlineEnabled = style.outlineEnabled && outline > 0f
        val outlineBlur = style.appearance.outlineBlur.coerceIn(
            SUBTITLE_OUTLINE_BLUR_MIN,
            SUBTITLE_OUTLINE_BLUR_MAX,
        )
        val outlineBlurSigmaXPx = subtitleOutlineBlurSigmaXPx(outlineBlur, videoWidthPx)
        val outlineBlurSigmaYPx = subtitleOutlineBlurSigmaYPx(outlineBlur, videoHeightPx)
        val outlineBlurSigmaXDp = with(density) { outlineBlurSigmaXPx.toDp() }
        val outlineBlurSigmaYDp = with(density) { outlineBlurSigmaYPx.toDp() }
        val legacyBlurRadiusPx = sqrt(outlineBlurSigmaXPx * outlineBlurSigmaYPx)
        val useNativeBlur = supportsNativeSubtitleBlur()
        val blurBleedPx = if (outlineEnabled && outlineBlur > 0) {
            maxOf(outlineBlurSigmaXPx, outlineBlurSigmaYPx) * SUBTITLE_BLUR_OUTSET_SIGMAS
        } else {
            0f
        }
        val edgePaddingPx = if (outlineEnabled) outline + blurBleedPx else 0f
        val edgePadding = with(density) { edgePaddingPx.toDp() }

        val availableTextWidth = (availableWidth - paddingX * 2).coerceAtLeast(1f).toInt()
        val annotatedText = remember(text, subFlowPreferences, subFlowWords, selectedWord) {
            if (subFlowPreferences == null) {
                AnnotatedString(text)
            } else {
                val learningColor = parseSubFlowColor(subFlowPreferences.learningWordColor, Color(0xFF5CC8F5))
                val knownColor = parseSubFlowColor(subFlowPreferences.knownWordColor, Color(0xFF4DD18C))
                buildAnnotatedString {
                    append(text)
                    SubFlowText.wordRanges(text).forEach { range ->
                        val wordText = text.substring(range.first, range.last + 1)
                        val key = wordText.lowercase()
                        val status = subFlowWords[key]?.status
                        val wordColor = when (status) {
                            SubFlowWordStatus.Learning -> learningColor
                            SubFlowWordStatus.Known -> knownColor
                            else -> null
                        }
                        if (wordColor != null) {
                            addStyle(SpanStyle(color = wordColor), range.first, range.last + 1)
                        }
                        if (selectedWord != null && key == selectedWord.lowercase()) {
                            addStyle(
                                SpanStyle(background = Color.White.copy(alpha = 0.22f)),
                                range.first,
                                range.last + 1,
                            )
                        }
                    }
                }
            }
        }
        val layout = measurer.measure(
            text = annotatedText,
            style = TextStyle(
                color = style.textColor,
                fontFamily = fontFamily,
                fontSize = (style.fontSizeSp * scale).sp,
                fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                textDirection = TextDirection.ContentOrLtr,
            ),
            constraints = Constraints(maxWidth = availableTextWidth),
        )
        val contentHeight = with(density) { (layout.size.height + paddingY * 2).toDp() }
        val contentWidth = layout.size.width.toFloat() + paddingX * 2
        val boxWidthPx = contentWidth + edgePaddingPx * 2
        val boxWidth = with(density) { boxWidthPx.toDp() }
        val left = with(density) { ((availableWidth - boxWidthPx) / 2f).toDp() }
        val origin = Offset(
            edgePaddingPx + paddingX,
            paddingY + edgePaddingPx,
        )
        val radius = with(density) { (style.appearance.backgroundRadius.dp * scale).toPx() }
        val shadowSize = with(density) { (style.appearance.shadowSize.dp * scale).toPx() }

        val subFlowTapModifier = if (onSubFlowWordTap != null) {
            Modifier.pointerInput(text, layout, edgePaddingPx, paddingX, paddingY) {
                detectTapGestures { position ->
                    val textX = position.x - edgePaddingPx - paddingX
                    val textY = position.y - paddingY - edgePaddingPx
                    if (textX < 0f || textY < 0f) return@detectTapGestures
                    if (textX > layout.size.width.toFloat() || textY > layout.size.height.toFloat()) return@detectTapGestures
                    val textOffset = layout.getOffsetForPosition(Offset(textX, textY))
                    SubFlowText.wordAt(text, textOffset)?.let { hit ->
                        onSubFlowWordTap.invoke(hit.word, hit.line)
                    }
                }
            }
        } else {
            Modifier
        }

        Box(
            Modifier.requiredWidth(boxWidth)
                .height(contentHeight + edgePadding * 2)
                .offset(x = left, y = edgePadding)
                .then(subFlowTapModifier)
                .semantics { this.text = AnnotatedString(text) },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = style.backgroundColor,
                    topLeft = Offset(edgePaddingPx, edgePaddingPx),
                    size = androidx.compose.ui.geometry.Size(
                        contentWidth,
                        (size.height - edgePaddingPx * 2).coerceAtLeast(0f),
                    ),
                    cornerRadius = CornerRadius(radius),
                )
            }

            if (outlineEnabled) {
                val outlineColor = style.outlineColor.copy(
                    alpha = style.outlineColor.alpha * style.textColor.alpha,
                )
                if (outlineBlur > 0) {
                    if (useNativeBlur) {
                        Canvas(
                            Modifier.fillMaxSize().blur(
                                radiusX = outlineBlurSigmaXDp,
                                radiusY = outlineBlurSigmaYDp,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded,
                            ),
                        ) {
                            drawText(
                                layout,
                                color = outlineColor,
                                topLeft = origin,
                                drawStyle = Stroke(outline * 2f),
                                shadow = Shadow.None,
                            )
                        }
                    } else {
                        val maskShadowColor = outlineColor.copy(
                            alpha = outlineColor.alpha.coerceAtMost(0.999f),
                        )
                        Canvas(Modifier.fillMaxSize()) {
                            drawText(
                                layout,
                                color = Color.Transparent,
                                topLeft = origin,
                                drawStyle = Stroke(outline * 2f),
                                shadow = Shadow(
                                    color = maskShadowColor,
                                    offset = Offset.Zero,
                                    blurRadius = legacyBlurRadiusPx,
                                ),
                            )
                        }
                    }
                } else {
                    Canvas(Modifier.fillMaxSize()) {
                        drawText(
                            layout,
                            color = outlineColor,
                            topLeft = origin,
                            drawStyle = Stroke(outline * 2f),
                            shadow = Shadow.None,
                        )
                    }
                }
            }

            Canvas(Modifier.fillMaxSize()) {
                drawText(
                    layout,
                    color = Color.Unspecified,
                    topLeft = origin,
                    drawStyle = Fill,
                    shadow = if (style.appearance.shadowEnabled) {
                        Shadow(
                            color = Color.Black.copy(
                                alpha = style.appearance.shadowOpacity / 100f * style.textColor.alpha,
                            ),
                            offset = Offset(shadowSize, shadowSize),
                            blurRadius = shadowSize * 2,
                        )
                    } else {
                        Shadow.None
                    },
                )
            }
        }
    }
}
