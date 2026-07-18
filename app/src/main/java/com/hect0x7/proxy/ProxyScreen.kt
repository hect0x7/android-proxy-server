package com.hect0x7.proxy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class ProxyKind(val title: String) { HTTP("HTTP Proxy"), SOCKS("SOCKS Proxy") }

@Composable
fun ProxyScreen(
    settings: ProxySettings,
    runtime: RuntimeSnapshot,
    addresses: List<String>,
    onSettingsChanged: (ProxySettings) -> Unit,
) {
    var editing by remember { mutableStateOf<ProxyKind?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(runtime.running)
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                ProxyToggleRow(
                    title = ProxyKind.HTTP.title,
                    port = settings.httpPort,
                    enabled = settings.httpEnabled,
                    running = runtime.httpRunning,
                    onEdit = { editing = ProxyKind.HTTP },
                    onToggle = { onSettingsChanged(settings.copy(httpEnabled = it)) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ProxyToggleRow(
                    title = ProxyKind.SOCKS.title,
                    port = settings.socksPort,
                    enabled = settings.socksEnabled,
                    running = runtime.socksRunning,
                    onEdit = { editing = ProxyKind.SOCKS },
                    onToggle = { onSettingsChanged(settings.copy(socksEnabled = it)) },
                )

                SectionTitle("Proxy Details")
                DetailRow("HTTP", if (settings.httpEnabled) "0.0.0.0:${settings.httpPort}" else "Stopped")
                DetailRow("SOCKS", if (settings.socksEnabled) "0.0.0.0:${settings.socksPort}" else "Stopped")

                SectionTitle("IP Addresses")
                if (addresses.isEmpty()) DetailRow("Local network", "No reachable IPv4 address")
                else addresses.forEachIndexed { index, address ->
                    DetailRow(if (index == 0) "Local network" else "", address, monospace = true)
                }

                SectionTitle("Traffic")
                Row(modifier = Modifier.fillMaxWidth()) {
                    Metric("Download", formatBytes(runtime.bytesReceived), Modifier.weight(1f))
                    Metric("Upload", formatBytes(runtime.bytesSent), Modifier.weight(1f))
                }

                SectionTitle("Connections")
                Row(modifier = Modifier.fillMaxWidth()) {
                    Metric("Active", runtime.activeConnections.toString(), Modifier.weight(1f))
                    Metric("Session total", runtime.totalConnections.toString(), Modifier.weight(1f))
                }

                SectionTitle("Session Log")
                SessionLog(runtime.log)
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    editing?.let { kind ->
        PortDialog(
            title = kind.title,
            initialPort = if (kind == ProxyKind.HTTP) settings.httpPort else settings.socksPort,
            reservedPort = if (kind == ProxyKind.HTTP) settings.socksPort else settings.httpPort,
            onDismiss = { editing = null },
            onConfirm = { port ->
                editing = null
                onSettingsChanged(if (kind == ProxyKind.HTTP) settings.copy(httpPort = port) else settings.copy(socksPort = port))
            },
        )
    }
}

@Composable
private fun TopBar(running: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).background(Color(0xFF008577)).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Every Proxy",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier.size(8.dp).background(
                color = if (running) Color(0xFFB9F6CA) else Color.White.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.extraSmall,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (running) "RUNNING" else "IDLE",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ProxyToggleRow(
    title: String,
    port: Int,
    enabled: Boolean,
    running: Boolean,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp).clickable(onClick = onEdit)
            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                if (running) Text(
                    text = "ACTIVE",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = "Port $port",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = "Edit $title port",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun DetailRow(label: String, value: String, monospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.width(116.dp),
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = value, fontSize = 21.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun SessionLog(log: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp, max = 240.dp)
            .background(MaterialTheme.colorScheme.surface).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (log.isEmpty()) {
            Text("No session activity", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        } else {
            log.takeLast(10).forEach { line ->
                Text(
                    text = line,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PortDialog(
    title: String,
    initialPort: Int,
    reservedPort: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember(initialPort) { mutableStateOf(initialPort.toString()) }
    val port = text.toIntOrNull()
    val valid = port != null && port in 1..65535 && port != reservedPort

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title port") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value -> if (value.length <= 5 && value.all(Char::isDigit)) text = value },
                label = { Text("Port") },
                supportingText = {
                    if (port == reservedPort) Text("HTTP and SOCKS must use different ports")
                    else if (!valid) Text("Enter a value from 1 to 65535")
                },
                isError = !valid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onConfirm(port!!) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = MaterialTheme.shapes.small,
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return if (value >= 100) "%.0f %s".format(value, units[unit]) else "%.1f %s".format(value, units[unit])
}
