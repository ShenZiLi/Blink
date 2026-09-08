package com.wink.eye.ui.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wink.eye.R
import com.wink.eye.data.IntervalUnit
import com.wink.eye.data.ReminderMode
import com.wink.eye.data.Rule
import com.wink.eye.data.RuleType
import com.wink.eye.data.ScreenTimeUnit
import java.util.UUID

/** Debug 开关：允许亮屏时长/暗屏重置使用秒级单位，正式上线时设为 false */
private const val DEBUG_SECONDS_ENABLED = true

@Composable
fun EditScreen(
    existingRule: Rule?,
    onSave: (Rule) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existingRule?.name ?: "护眼规则") }
    var ruleTypeIndex by remember { mutableStateOf(if (existingRule?.type is RuleType.ScreenTime) 1 else 0) }

    // 间隔时间设置
    val existingInterval = existingRule?.type as? RuleType.Interval
    var intervalValue by remember { mutableIntStateOf(existingInterval?.value ?: 30) }
    var intervalValueText by remember { mutableStateOf((existingInterval?.value ?: 30).toString()) }
    var intervalUnit by remember { mutableStateOf(existingInterval?.unit ?: IntervalUnit.MINUTES) }

    // 亮屏时长设置
    val existingScreenTime = existingRule?.type as? RuleType.ScreenTime
    var screenOnDuration by remember { mutableFloatStateOf(existingScreenTime?.effectiveScreenOnDuration?.toFloat() ?: 30f) }
    var screenOffResetDuration by remember { mutableFloatStateOf(existingScreenTime?.effectiveScreenOffResetDuration?.toFloat() ?: 5f) }
    var screenOnUnit by remember { mutableStateOf(existingScreenTime?.screenOnUnit ?: ScreenTimeUnit.MINUTES) }
    var screenOffResetUnit by remember { mutableStateOf(existingScreenTime?.screenOffResetUnit ?: ScreenTimeUnit.MINUTES) }
    var reminderMode by remember { mutableStateOf(existingRule?.reminderMode ?: ReminderMode.NOTIFICATION) }

    val isEditing = existingRule != null

    // 预设选中状态
    val isPreset15 = ruleTypeIndex == 0 && intervalUnit == IntervalUnit.MINUTES && intervalValue == 15
    val isPreset30 = ruleTypeIndex == 0 && intervalUnit == IntervalUnit.MINUTES && intervalValue == 30
    val isPreset1h = ruleTypeIndex == 0 && intervalUnit == IntervalUnit.MINUTES && intervalValue == 60

    val isFormValid = name.isNotBlank() && (ruleTypeIndex == 1 || intervalValue > 0)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.edit_back), color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = if (isEditing) stringResource(R.string.edit_title_edit)
                    else stringResource(R.string.edit_title_new),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                TextButton(
                    onClick = {
                        val ruleType = if (ruleTypeIndex == 0) {
                            RuleType.Interval(value = intervalValue, unit = intervalUnit)
                        } else {
                            RuleType.ScreenTime(
                                screenOnDuration = screenOnDuration.toInt(),
                                screenOffResetDuration = screenOffResetDuration.toInt(),
                                screenOnUnit = screenOnUnit,
                                screenOffResetUnit = screenOffResetUnit
                            )
                        }
                        val rule = Rule(
                            id = existingRule?.id ?: UUID.randomUUID().toString(),
                            name = name.ifBlank { context.getString(R.string.default_rule_name) },
                            type = ruleType,
                            reminderMode = reminderMode,
                            enabled = existingRule?.enabled ?: true
                        )
                        onSave(rule)
                    },
                    enabled = isFormValid
                ) {
                    Text(stringResource(R.string.edit_save), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── 规则设置卡片 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column {
                    // 规则名称
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.edit_name_label)) },
                        placeholder = { Text(stringResource(R.string.edit_name_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 规则类型
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.edit_type_label),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Spacer(Modifier.weight(1f))
                        FilterChip(
                            selected = ruleTypeIndex == 0,
                            onClick = { ruleTypeIndex = 0 },
                            label = { Text(stringResource(R.string.rule_type_interval)) }
                        )
                        FilterChip(
                            selected = ruleTypeIndex == 1,
                            onClick = { ruleTypeIndex = 1 },
                            label = { Text(stringResource(R.string.rule_type_screen)) }
                        )
                    }
                }
            }

            // ── 间隔时间卡片 ──
            if (ruleTypeIndex == 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column {
                        // 自定义间隔
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                stringResource(R.string.edit_interval_custom_label),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = intervalValueText,
                                onValueChange = { text ->
                                    intervalValueText = text.filter { it.isDigit() }
                                    intervalValue = text.filter { it.isDigit() }.toIntOrNull() ?: 0
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.width(72.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = intervalUnit == IntervalUnit.MINUTES,
                                    onClick = { intervalUnit = IntervalUnit.MINUTES },
                                    label = { Text(stringResource(R.string.unit_minutes)) }
                                )
                                FilterChip(
                                    selected = intervalUnit == IntervalUnit.SECONDS,
                                    onClick = { intervalUnit = IntervalUnit.SECONDS },
                                    label = { Text(stringResource(R.string.unit_seconds)) }
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 快捷预设
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.edit_interval_preset_label),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.weight(1f))
                            FilterChip(
                                selected = isPreset15,
                                onClick = {
                                    intervalValue = 15; intervalValueText = "15"; intervalUnit = IntervalUnit.MINUTES
                                },
                                label = { Text(stringResource(R.string.preset_15min)) }
                            )
                            FilterChip(
                                selected = isPreset30,
                                onClick = {
                                    intervalValue = 30; intervalValueText = "30"; intervalUnit = IntervalUnit.MINUTES
                                },
                                label = { Text(stringResource(R.string.preset_30min)) }
                            )
                            FilterChip(
                                selected = isPreset1h,
                                onClick = {
                                    intervalValue = 60; intervalValueText = "60"; intervalUnit = IntervalUnit.MINUTES
                                },
                                label = { Text(stringResource(R.string.preset_1hour)) }
                            )
                        }
                    }
                }
            }

            // ── 亮屏时长卡片 ──
            if (ruleTypeIndex == 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column {
                        val screenOnUnitLabel = if (screenOnUnit == ScreenTimeUnit.MINUTES)
                            stringResource(R.string.unit_minutes) else stringResource(R.string.unit_seconds)
                        val screenOffResetUnitLabel = if (screenOffResetUnit == ScreenTimeUnit.MINUTES)
                            stringResource(R.string.unit_minutes) else stringResource(R.string.unit_seconds)

                        // 亮屏时长
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${stringResource(R.string.edit_screen_on_label)}: ${screenOnDuration.toInt()} $screenOnUnitLabel",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = screenOnDuration,
                                onValueChange = { screenOnDuration = it },
                                valueRange = if (screenOnUnit == ScreenTimeUnit.MINUTES) 5f..120f else 5f..300f,
                                steps = if (screenOnUnit == ScreenTimeUnit.MINUTES) 22 else 58,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (DEBUG_SECONDS_ENABLED) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilterChip(
                                        selected = screenOnUnit == ScreenTimeUnit.MINUTES,
                                        onClick = { screenOnUnit = ScreenTimeUnit.MINUTES; screenOnDuration = 30f },
                                        label = { Text(stringResource(R.string.unit_minutes)) }
                                    )
                                    FilterChip(
                                        selected = screenOnUnit == ScreenTimeUnit.SECONDS,
                                        onClick = { screenOnUnit = ScreenTimeUnit.SECONDS; screenOnDuration = 30f },
                                        label = { Text(stringResource(R.string.unit_seconds)) }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 暗屏重置
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${stringResource(R.string.edit_screen_off_reset_label)}: ${screenOffResetDuration.toInt()} $screenOffResetUnitLabel",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = screenOffResetDuration,
                                onValueChange = { screenOffResetDuration = it },
                                valueRange = if (screenOffResetUnit == ScreenTimeUnit.MINUTES) 1f..30f else 5f..300f,
                                steps = if (screenOffResetUnit == ScreenTimeUnit.MINUTES) 28 else 58,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (DEBUG_SECONDS_ENABLED) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilterChip(
                                        selected = screenOffResetUnit == ScreenTimeUnit.MINUTES,
                                        onClick = { screenOffResetUnit = ScreenTimeUnit.MINUTES; screenOffResetDuration = 5f },
                                        label = { Text(stringResource(R.string.unit_minutes)) }
                                    )
                                    FilterChip(
                                        selected = screenOffResetUnit == ScreenTimeUnit.SECONDS,
                                        onClick = { screenOffResetUnit = ScreenTimeUnit.SECONDS; screenOffResetDuration = 30f },
                                        label = { Text(stringResource(R.string.unit_seconds)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 提醒方式卡片 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.edit_reminder_label),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.weight(1f))
                    FilterChip(
                        selected = reminderMode == ReminderMode.ALARM,
                        onClick = { reminderMode = ReminderMode.ALARM },
                        label = { Text(stringResource(R.string.edit_reminder_alarm)) }
                    )
                    FilterChip(
                        selected = reminderMode == ReminderMode.NOTIFICATION,
                        onClick = { reminderMode = ReminderMode.NOTIFICATION },
                        label = { Text(stringResource(R.string.edit_reminder_notification)) }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // 保存按钮
            Button(
                onClick = {
                    val ruleType = if (ruleTypeIndex == 0) {
                        RuleType.Interval(value = intervalValue, unit = intervalUnit)
                    } else {
                        RuleType.ScreenTime(
                            screenOnDuration = screenOnDuration.toInt(),
                            screenOffResetDuration = screenOffResetDuration.toInt(),
                            screenOnUnit = screenOnUnit,
                            screenOffResetUnit = screenOffResetUnit
                        )
                    }
                    val rule = Rule(
                        id = existingRule?.id ?: UUID.randomUUID().toString(),
                        name = name.ifBlank { context.getString(R.string.default_rule_name) },
                        type = ruleType,
                        reminderMode = reminderMode,
                        enabled = existingRule?.enabled ?: true
                    )
                    onSave(rule)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                enabled = isFormValid,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    stringResource(R.string.edit_save),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}