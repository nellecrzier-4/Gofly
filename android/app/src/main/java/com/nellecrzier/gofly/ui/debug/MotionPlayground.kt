@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.nellecrzier.gofly.ui.debug

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofly.AnimPref
import com.nellecrzier.gofly.ui.components.AppLoadingIndicator
import com.nellecrzier.gofly.ui.components.animatedConnectedShape
import com.nellecrzier.gofly.ui.components.connectedShape
import com.nellecrzier.gofly.ui.motion.motionScheme
import com.nellecrzier.gofly.ui.motion.pressScale
import com.nellecrzier.gofly.ui.motion.rememberReducedMotion
import com.nellecrzier.gofly.ui.motion.staggeredEnter
import kotlinx.coroutines.delay

/**
 * Laboratorium gerak Material 3 Expressive — hanya dipakai di build debug dari
 * pintu "Motion playground" di Setelan.
 *
 * Semua contoh di sini memakai `MaterialTheme.motionScheme` (spesifikasi spatial
 * untuk posisi/ukuran/bentuk/skala, spesifikasi effects untuk alfa), dan semua
 * menerima sakelar gerak redup layar ini. Saat gerak diredupkan, setiap contoh
 * memakai `tween(0)` atau membuang animasinya sama sekali, bukan sekadar
 * dipercepat.
 *
 * Tidak ada logika bisnis: status tombol/daftar hanya state lokal layar ini.
 */

/** Spesifikasi "langsung jadi" untuk mode gerak redup. */
private val SnapFloat: AnimationSpec<Float> = tween(durationMillis = 0)
private val SnapDp: AnimationSpec<Dp> = tween(durationMillis = 0)

@Composable
internal fun MotionPlayground(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // AnimPref lokal layar ini: tidak menyentuh preferensi aplikasi.
    var pref by rememberSaveable { mutableStateOf(AnimPref.SYSTEM) }
    val deviceReduced = rememberReducedMotion()
    // Sama seperti PrefsState.motionReduced: apa pun selain SYSTEM = redup.
    val reducedMotion = pref != AnimPref.SYSTEM || deviceReduced

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            PlaygroundHeader(onClose = onClose)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                MotionPreferenceSection(
                    pref = pref,
                    deviceReduced = deviceReduced,
                    reducedMotion = reducedMotion,
                    onChange = { pref = it },
                )
                LoadingIndicatorsSection(reducedMotion)
                StatefulButtonSection(reducedMotion)
                ConnectedGroupSection(reducedMotion)
                WavyProgressSection(reducedMotion)
                ConnectedListSection(reducedMotion)
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

/* ------------------------------- kerangka ---------------------------- */

@Composable
private fun PlaygroundHeader(onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Tutup laboratorium gerak",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                text = "Motion playground",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Debug · Material 3 Expressive",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Judul + penjelasan + kartu tempat contoh diletakkan. */
@Composable
private fun PlaygroundSection(
    title: String,
    caption: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

/* ------------------------------ preferensi --------------------------- */

/**
 * Segmen AnimPref khusus layar ini. SYSTEM mengikuti skala animator perangkat,
 * REDUCED memaksa redup, dan OFF diperlakukan sama seperti REDUCED karena
 * aplikasi memetakan semua nilai selain SYSTEM ke `motionReduced`.
 */
@Composable
private fun MotionPreferenceSection(
    pref: AnimPref,
    deviceReduced: Boolean,
    reducedMotion: Boolean,
    onChange: (AnimPref) -> Unit,
) {
    PlaygroundSection(
        title = "AnimPref",
        caption = "Sakelar gerak untuk semua contoh di bawah. Tidak mengubah Setelan aplikasi.",
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            AnimPref.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = pref == option,
                    onClick = { onChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = AnimPref.entries.size,
                    ),
                    label = {
                        Text(
                            text = option.name,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = when {
                pref == AnimPref.SYSTEM && deviceReduced ->
                    "SYSTEM → skala animator perangkat 0, jadi gerak redup."
                pref == AnimPref.SYSTEM ->
                    "SYSTEM → skala animator perangkat normal, gerak penuh."
                pref == AnimPref.REDUCED -> "REDUCED → gerak redup dipaksa."
                else -> "OFF → gerak redup (di aplikasi, apa pun selain SYSTEM = redup)."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (reducedMotion) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/* ------------------------------- indikator --------------------------- */

/** AppLoadingIndicator 24/48/96 dp + ContainedLoadingIndicator di wadah tonal. */
@Composable
private fun LoadingIndicatorsSection(reducedMotion: Boolean) {
    PlaygroundSection(
        title = "Indikator muat",
        caption = "AppLoadingIndicator (pembungkus LoadingIndicator M3) pada tiga ukuran, " +
            "lalu ContainedLoadingIndicator asli M3 di dalam wadah tonal.",
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            listOf(24, 48, 96).forEach { size ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(96.dp), contentAlignment = Alignment.BottomCenter) {
                        AppLoadingIndicator(
                            size = size.dp,
                            color = MaterialTheme.colorScheme.primary,
                            reduceMotion = reducedMotion,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "$size dp",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                // Wadah tonal milik indikator sendiri tetap dipakai (warna bawaan).
                if (reducedMotion) {
                    // Varian determinate tidak berputar/bermorf: pengganti saat redup.
                    ContainedLoadingIndicator(
                        progress = { 0.6f },
                        modifier = Modifier.size(64.dp),
                    )
                } else {
                    ContainedLoadingIndicator(modifier = Modifier.size(64.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "ContainedLoadingIndicator",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = if (reducedMotion) {
                            "Gerak redup: varian determinate, diam di 60%."
                        } else {
                            "Indeterminate: bentuk bermorf di dalam pil."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

/* ------------------------------ tombol tiga fase --------------------- */

private enum class ActionPhase { Idle, Loading, Success }

/**
 * Tombol idle → loading → sukses: label memudar (effects), lebar menyusut jadi
 * lingkaran (spatial), centang membesar masuk (spatial), lalu kembali ke idle
 * saat disentuh lagi. Fase loading berjalan sendiri supaya tidak butuh logika.
 */
@Composable
private fun StatefulButtonSection(reducedMotion: Boolean) {
    var phase by remember { mutableStateOf(ActionPhase.Idle) }
    val spatial = motionScheme.fastSpatialSpec<Float>()
    val effects = motionScheme.fastEffectsSpec<Float>()

    val width by animateDpAsState(
        targetValue = if (phase == ActionPhase.Idle) 196.dp else 56.dp,
        animationSpec = if (reducedMotion) SnapDp else motionScheme.fastSpatialSpec(),
        label = "actionWidth",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (phase == ActionPhase.Idle) 1f else 0f,
        animationSpec = if (reducedMotion) SnapFloat else effects,
        label = "actionLabelAlpha",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (phase == ActionPhase.Success) 1f else 0.6f,
        animationSpec = if (reducedMotion) SnapFloat else spatial,
        label = "actionCheckScale",
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (phase == ActionPhase.Success) 1f else 0f,
        animationSpec = if (reducedMotion) SnapFloat else effects,
        label = "actionCheckAlpha",
    )

    // Fase loading selesai sendiri; tidak ada pekerjaan nyata di baliknya.
    LaunchedEffect(phase) {
        if (phase == ActionPhase.Loading) {
            delay(1500)
            phase = ActionPhase.Success
        }
    }

    PlaygroundSection(
        title = "Tombol tiga fase",
        caption = "Idle → memuat → sukses. Label memudar dengan spesifikasi effects, " +
            "lebar dan centang dengan spesifikasi spatial.",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    phase = when (phase) {
                        ActionPhase.Idle -> ActionPhase.Loading
                        ActionPhase.Success -> ActionPhase.Idle
                        // Ketukan saat memuat diabaikan, bukan dinonaktifkan,
                        // supaya warna tombol tidak berubah jadi disabled.
                        ActionPhase.Loading -> ActionPhase.Loading
                    }
                },
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.width(width).height(56.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Unggah berkas",
                        modifier = Modifier.alpha(labelAlpha),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                    if (phase == ActionPhase.Loading) {
                        AppLoadingIndicator(
                            size = 24.dp,
                            color = LocalContentColor.current,
                            reduceMotion = reducedMotion,
                        )
                    }
                    if (phase == ActionPhase.Success) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Selesai",
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = checkScale
                                    scaleY = checkScale
                                    alpha = checkAlpha
                                },
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = { phase = ActionPhase.Idle }) {
                Text("Reset")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (phase) {
                ActionPhase.Idle -> "Fase: idle · ketuk untuk mulai"
                ActionPhase.Loading -> "Fase: memuat · selesai otomatis 1,5 dtk"
                ActionPhase.Success -> "Fase: sukses · ketuk untuk kembali ke idle"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* --------------------------- grup tombol bersambung ------------------ */

/**
 * Deretan ToggleButton bergaya connected button group: bentuk sudut dari
 * `ButtonGroupDefaults.connected*ButtonShapes()` dengan jarak
 * `ConnectedSpaceBetween`, jadi pilihan yang aktif mekar jadi pil.
 * Saat redup, ketiga bentuk disamakan supaya tidak ada morf.
 */
@Composable
private fun ConnectedGroupSection(reducedMotion: Boolean) {
    var selected by remember { mutableIntStateOf(0) }
    val items = listOf(
        "Ringkas" to Icons.Filled.GridView,
        "Daftar" to Icons.AutoMirrored.Filled.List,
        "Rapat" to Icons.Filled.Tune,
    )

    PlaygroundSection(
        title = "Grup tombol bersambung",
        caption = "ToggleButton dengan bentuk connected: ujung membulat penuh, tengah " +
            "bersudut kecil. Pilihan bergerak dengan spesifikasi spatial tercepat.",
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            items.forEachIndexed { index, (label, icon) ->
                val shapes = toggleShapesFor(index, items.size, reducedMotion)
                ToggleButton(
                    checked = selected == index,
                    onCheckedChange = { checked -> if (checked) selected = index },
                    shapes = shapes,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Terpilih: ${items[selected].first}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Bentuk connected untuk tiap posisi; satu bentuk statis saat gerak redup. */
@Composable
private fun toggleShapesFor(index: Int, count: Int, reducedMotion: Boolean): ToggleButtonShapes {
    if (reducedMotion) {
        val flat = connectedShape(index = index, count = count)
        return ToggleButtonShapes(shape = flat, pressedShape = flat, checkedShape = flat)
    }
    return when {
        count <= 1 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    }
}

/* ---------------------------- progres bergelombang ------------------- */

/**
 * LinearWavyProgressIndicator dengan sakelar amplitudo: 0 = datar, 1 = penuh
 * bergelombang. Ketebalan dinaikkan supaya bedanya terlihat; amplitudo tetap
 * memakai API `amplitude` milik komponen. Saat redup, amplitudo dipaksa 0 dan
 * progres langsung jadi.
 */
@Composable
private fun WavyProgressSection(reducedMotion: Boolean) {
    var wavy by rememberSaveable { mutableStateOf(true) }
    var target by remember { mutableFloatStateOf(0.24f) }
    val density = LocalDensity.current
    val stroke = remember(density) {
        Stroke(width = with(density) { 6.dp.toPx() }, cap = StrokeCap.Round)
    }
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = if (reducedMotion) SnapFloat else motionScheme.slowSpatialSpec(),
        label = "wavyProgress",
    )

    LaunchedEffect(Unit) {
        delay(160)
        target = 0.72f
    }

    PlaygroundSection(
        title = "Progres bergelombang",
        caption = "Amplitude 0 = datar, 1 = bergelombang penuh. Perubahan amplitudo " +
            "dianimasikan oleh komponennya sendiri.",
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Amplitudo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (wavy && !reducedMotion) "bergelombang" else "datar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = wavy && !reducedMotion,
                onCheckedChange = { wavy = it },
                enabled = !reducedMotion,
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearWavyProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(40.dp),
            stroke = stroke,
            trackStroke = stroke,
            amplitude = { if (wavy && !reducedMotion) 1f else 0f },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "progres ${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* ---------------------------- daftar bersambung ---------------------- */

private data class PlaygroundRow(val id: Int, val title: String)

/**
 * Daftar bersambung: tiap baris adalah Surface sendiri dengan jarak 2 dp,
 * sudut dari `animatedConnectedShape`, kunci stabil dan `animateItem()`.
 * Menambah/menghapus baris memperlihatkan perubahan sudut + perpindahan.
 */
@Composable
private fun ConnectedListSection(reducedMotion: Boolean) {
    val rows = remember {
        mutableStateListOf(
            PlaygroundRow(0, "Sudut bersambung"),
            PlaygroundRow(1, "Kunci stabil"),
            PlaygroundRow(2, "animateItem"),
        )
    }
    var nextId by remember { mutableIntStateOf(3) }
    val addSource = remember { MutableInteractionSource() }
    val listState = rememberLazyListState()

    // Baris baru langsung terlihat: geser ke ujung, atau lompat saat redup.
    LaunchedEffect(rows.size) {
        if (rows.isNotEmpty()) {
            if (reducedMotion) {
                listState.scrollToItem(rows.lastIndex)
            } else {
                listState.animateScrollToItem(rows.lastIndex)
            }
        }
    }

    PlaygroundSection(
        title = "Daftar bersambung",
        caption = "Baris terpisah 2 dp; sudut luar 26 dp, sudut dalam 6 dp, dianimasikan " +
            "saat baris disisipkan atau dibuang.",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${rows.size} baris",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(
                onClick = {
                    rows.add(PlaygroundRow(nextId, "Baris ${nextId + 1}"))
                    nextId += 1
                },
                enabled = rows.size < 8,
                interactionSource = addSource,
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.pressScale(addSource, reducedMotion),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tambah", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { if (rows.isNotEmpty()) rows.removeAt(rows.lastIndex) },
                enabled = rows.isNotEmpty(),
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
                Text("Buang", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(12.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().height(272.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
                Surface(
                    shape = animatedConnectedShape(
                        index = index,
                        count = rows.size,
                        reduceMotion = reducedMotion,
                    ),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEnter(index = index, reduceMotion = reducedMotion)
                        // Saat redup: tidak ada fade maupun geser sama sekali.
                        .animateItem(
                            fadeInSpec = if (reducedMotion) null else motionScheme.fastEffectsSpec(),
                            placementSpec = if (reducedMotion) null else motionScheme.defaultSpatialSpec(),
                            fadeOutSpec = if (reducedMotion) null else motionScheme.fastEffectsSpec(),
                        ),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(start = 16.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { rows.remove(row) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Buang ${row.title}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
