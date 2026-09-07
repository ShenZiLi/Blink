package com.wink.eye.ui.earclock

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wink.eye.R
import com.wink.eye.data.EarClockAlarm
import com.wink.eye.data.EarClockFrequency
import com.wink.eye.data.VibrationMode
import com.wink.eye.service.EarClockAlarmScheduler
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.UUID

/** 一周自定义顺序：周一..周日 */
private val CUSTOM_DAYS = listOf(
    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
    Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
)

/** 设置闹钟页：对齐 UI 设计文档（导航 + 倒计时 + 滚轮时间 + 频率 + 设置分组卡片） */
@Composable
fun EarClockEditScreen(
    existingAlarm: EarClockAlarm?,
    onSave: (EarClockAlarm) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(existingAlarm?.name ?: context.getString(R.string.earclock_default_name)) }
    var hour by remember { mutableIntStateOf(existingAlarm?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(existingAlarm?.minute ?: 0) }
    var frequency by remember { mutableStateOf(existingAlarm?.frequency ?: EarClockFrequency.ONCE) }
    var daysOfWeek by remember { mutableStateOf(existingAlarm?.daysOfWeek ?: emptySet()) }
    var ringtoneUri by remember { mutableStateOf(existingAlarm?.ringtoneUri) }
    var ringtoneName by remember { mutableStateOf<String?>(null) }
    var vibrationOn by remember { mutableStateOf(existingAlarm?.vibrationMode != VibrationMode.OFF) }
    var snoozeEnabled by remember { mutableStateOf(existingAlarm?.snoozeEnabled ?: true) }
    var snoozeMinutes by remember { mutableIntStateOf(existingAlarm?.snoozeMinutes ?: 5) }
    var snoozeRepeatLimit by remember { mutableIntStateOf(existingAlarm?.snoozeRepeatLimit ?: 3) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) {
            ringtoneUri = uri.toString()
            ringtoneName = RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.earclock_edit_cancel), color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = stringResource(R.string.earclock_edit_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                TextButton(
                    onClick = {
                        onSave(buildAlarm(existingAlarm, name, hour, minute, frequency,
                            daysOfWeek, ringtoneUri, vibrationOn, snoozeEnabled, snoozeMinutes, snoozeRepeatLimit))
                    },
                    enabled = isValid(frequency, daysOfWeek)
                ) {
                    Text(stringResource(R.string.earclock_edit_done), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            CountdownSection(hour, minute, frequency, daysOfWeek)

            Spacer(Modifier.height(16.dp))

            WheelTimePicker(
                hour = hour,
                minute = minute,
                onHourChange = { hour = it },
                onMinuteChange = { minute = it }
            )

            Spacer(Modifier.height(16.dp))

            // 频率切换
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = frequency == EarClockFrequency.ONCE,
                    onClick = { frequency = EarClockFrequency.ONCE },
                    label = { Text(stringResource(R.string.earclock_freq_once)) }
                )
                FilterChip(
                    selected = frequency == EarClockFrequency.WORKDAYS,
                    onClick = { frequency = EarClockFrequency.WORKDAYS },
                    label = { Text(stringResource(R.string.earclock_freq_workdays)) }
                )
                FilterChip(
                    selected = frequency == EarClockFrequency.CUSTOM,
                    onClick = { frequency = EarClockFrequency.CUSTOM },
                    label = { Text(stringResource(R.string.earclock_freq_custom)) }
                )
            }

            // 自定义频率：一周自选
            if (frequency == EarClockFrequency.CUSTOM) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.earclock_edit_days_label),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))
                val dayNames = listOf(
                    stringResource(R.string.day_mon), stringResource(R.string.day_tue),
                    stringResource(R.string.day_wed), stringResource(R.string.day_thu),
                    stringResource(R.string.day_fri), stringResource(R.string.day_sat),
                    stringResource(R.string.day_sun)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CUSTOM_DAYS.forEachIndexed { index, day ->
                        FilterChip(
                            selected = day in daysOfWeek,
                            onClick = {
                                daysOfWeek = if (day in daysOfWeek) daysOfWeek - day else daysOfWeek + day
                            },
                            label = { Text(dayNames[index]) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            SettingsCard(
                name = name,
                onNameChange = { name = it },
                workdayLabel = stringResource(R.string.earclock_edit_workday_weekdays),
                ringtoneName = ringtoneName,
                onPickRingtone = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri)
                    }
                    ringtoneLauncher.launch(intent)
                },
                vibrationOn = vibrationOn,
                onVibrationChange = { vibrationOn = it },
                snoozeEnabled = snoozeEnabled,
                onSnoozeEnabledChange = { snoozeEnabled = it },
                snoozeMinutes = snoozeMinutes,
                onSnoozeMinutesChange = { snoozeMinutes = it },
                snoozeRepeatLimit = snoozeRepeatLimit,
                onSnoozeRepeatLimitChange = { snoozeRepeatLimit = it },
                frequency = frequency
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 距离下次响铃倒计时：每秒刷新 */
@Composable
private fun CountdownSection(hour: Int, minute: Int, frequency: EarClockFrequency, daysOfWeek: Set<Int>) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    Text(
        text = stringResource(R.string.earclock_countdown_prefix),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = remainingLabel(now, hour, minute, frequency, daysOfWeek),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
    )
}

private fun remainingLabel(now: Long, hour: Int, minute: Int,
                           frequency: EarClockFrequency, daysOfWeek: Set<Int>): String {
    val dummy = EarClockAlarm(
        id = "dummy", name = "", enabled = true,
        hour = hour, minute = minute, frequency = frequency, daysOfWeek = daysOfWeek
    )
    val diffMs = (EarClockAlarmScheduler.nextTriggerMillis(dummy, now) - now).coerceAtLeast(0)
    val totalMin = diffMs / 60000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}小时${m}分钟" else "${m}分钟"
}

@Composable
private fun isValid(frequency: EarClockFrequency, daysOfWeek: Set<Int>): Boolean {
    return when (frequency) {
        EarClockFrequency.ONCE, EarClockFrequency.WORKDAYS -> true
        EarClockFrequency.CUSTOM -> daysOfWeek.isNotEmpty()
    }
}

private fun buildAlarm(
    existingAlarm: EarClockAlarm?,
    name: String,
    hour: Int,
    minute: Int,
    frequency: EarClockFrequency,
    daysOfWeek: Set<Int>,
    ringtoneUri: String?,
    vibrationOn: Boolean,
    snoozeEnabled: Boolean,
    snoozeMinutes: Int,
    snoozeRepeatLimit: Int
): EarClockAlarm = EarClockAlarm(
    id = existingAlarm?.id ?: UUID.randomUUID().toString(),
    name = name,
    enabled = existingAlarm?.enabled ?: true,
    hour = hour,
    minute = minute,
    frequency = frequency,
    daysOfWeek = daysOfWeek,
    ringtoneUri = ringtoneUri,
    vibrationMode = if (vibrationOn) VibrationMode.DEFAULT else VibrationMode.OFF,
    snoozeEnabled = snoozeEnabled,
    snoozeMinutes = snoozeMinutes,
    snoozeRepeatLimit = snoozeRepeatLimit
)

/** 双列滚轮时间选择器 */
@Composable
private fun WheelTimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WheelColumn(0..23, hour, onHourChange)
        Text(text = ":", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 4.dp))
        WheelColumn(0..59, minute, onMinuteChange)
    }
}

/** 单列滚轮：居中选中态高亮加粗 */
@Composable
private fun WheelColumn(range: IntRange, selected: Int, onSelect: (Int) -> Unit) {
    val itemHeight = 48.dp
    val listState: LazyListState = rememberLazyListState(initialFirstVisibleItemIndex = (selected - 1).coerceAtLeast(0))

    LaunchedEffect(selected) {
        listState.scrollToItem((selected - 1).coerceAtLeast(0))
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { idx ->
                val v = (idx + 1).coerceIn(range.first, range.last)
                if (v != selected) onSelect(v)
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .height(itemHeight * 3)
            .width(96.dp)
            .alpha(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(range.toList()) { index, value ->
            val isSel = value == selected
            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    style = if (isSel) MaterialTheme.typography.headlineMedium
                    else MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSel) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(if (isSel) 1f else 0.45f)
                )
            }
        }
    }
}

/** 设置分组卡片 */
@Composable
private fun SettingsCard(
    name: String,
    onNameChange: (String) -> Unit,
    workdayLabel: String,
    ringtoneName: String?,
    onPickRingtone: () -> Unit,
    vibrationOn: Boolean,
    onVibrationChange: (Boolean) -> Unit,
    snoozeEnabled: Boolean,
    onSnoozeEnabledChange: (Boolean) -> Unit,
    snoozeMinutes: Int,
    onSnoozeMinutesChange: (Int) -> Unit,
    snoozeRepeatLimit: Int,
    onSnoozeRepeatLimitChange: (Int) -> Unit,
    frequency: EarClockFrequency
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            // 工作日类型（仅 WORKDAYS 可见）
            if (frequency == EarClockFrequency.WORKDAYS) {
                SettingRow(title = stringResource(R.string.earclock_edit_workday_type), value = workdayLabel, chevron = false)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // 闹钟名称
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.earclock_edit_name_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 铃声
            ClickableSettingRow(
                title = stringResource(R.string.earclock_edit_ringtone),
                value = ringtoneName ?: stringResource(R.string.earclock_edit_ringtone_default),
                onClick = onPickRingtone
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 振动
            SwitchRow(
                title = stringResource(R.string.earclock_edit_vibration),
                checked = vibrationOn,
                onCheckedChange = onVibrationChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 稍后提醒
            SwitchRow(
                title = stringResource(R.string.earclock_edit_snooze),
                checked = snoozeEnabled,
                onCheckedChange = onSnoozeEnabledChange
            )
            if (snoozeEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.earclock_edit_snooze_interval),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(5, 10, 15).forEach { v ->
                            FilterChip(
                                selected = snoozeMinutes == v,
                                onClick = { onSnoozeMinutesChange(v) },
                                label = { Text("${v}${stringResource(R.string.unit_minutes)}") }
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.earclock_edit_snooze_limit),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1, 3, 5).forEach { v ->
                            FilterChip(
                                selected = snoozeRepeatLimit == v,
                                onClick = { onSnoozeRepeatLimitChange(v) },
                                label = { Text(stringResource(R.string.earclock_edit_snooze_times_label, v)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String, chevron: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (chevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClickableSettingRow(title: String, value: String, onClick: () -> Unit) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}